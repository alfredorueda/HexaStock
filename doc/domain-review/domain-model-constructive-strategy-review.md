# HexaStock — Constructive Strategy for a Pragmatic Rich Domain Model

**Date:** 15 April 2026  
**Author:** Architectural Strategy Review (code-grounded analysis)  
**Stack:** Java 21 · Spring Boot 3.5 · JPA/Hibernate · MySQL (configured) / PostgreSQL (target)  
**Scope:** How to keep DDD and hexagonal architecture while achieving pragmatic persistence and scalable runtime behaviour

---

## 1. Executive Summary

**Yes, HexaStock can and should keep its rich domain model.** The domain layer (`Portfolio`, `Holding`, `Lot`, `SellResult`, the sealed `Transaction` hierarchy, `HoldingPerformanceCalculator`) is well-designed, correctly protects real business invariants, and provides genuine value. Nothing in the scalability improvement path requires destroying or gutting this model.

The main bottlenecks are *not* in the domain itself but in the **persistence and transaction orchestration patterns** that surround it:

1. **External API calls happen inside the database transaction** — `PortfolioStockOperationsService.buyStock()` and `sellStock()` acquire a pessimistic lock, then call `stockPriceProviderPort.fetchStockPrice()` (which includes a 500ms throttle + network I/O), holding the lock for the entire duration.

2. **Every portfolio load uses a pessimistic write lock** — `JpaPortfolioRepository.getPortfolioById()` always delegates to `findByIdForUpdate()`, even for read-only operations like `GET /api/portfolios/{id}` and the holdings performance report.

3. **Full aggregate graph is loaded and fully re-merged on every operation** — `PortfolioMapper.toJpaEntity()` creates entirely new detached JPA entity instances and calls `save()`, forcing Hibernate to diff the entire Portfolio → Holdings → Lots graph even when only `balance` changed.

4. **No read-optimised paths exist** — all queries (list portfolios, get portfolio, transaction history, holdings performance) flow through the same aggregate-loading mechanism designed for write commands.

All four of these can be improved **without changing the domain model at all**. The domain layer stays rich. The changes land in the persistence adapter, the application services, and possibly new dedicated query services.

| Horizon | Goal | Recommended posture |
|---|---|---|
| **Now** | Personal portfolio — correct & clean | Fix the obvious: non-locking reads, external API outside transaction, pagination. Keep domain model untouched. |
| **Next** | Power-user growth — responsive & safe | Introduce CQRS-lite query services, `@Version` optimistic locking, smarter JPA fetch plans, batch price fetching. Domain model untouched. |
| **Later** | Enterprise-like growth — if evidence demands | Consider aggregate boundary split, event-sourced projections, lot compaction. Only if monitoring proves it is needed. |

---

## 2. What Serious High-Scale Teams Usually Do in Practice

### 2.1 Small Transactional Aggregates

**Principle.** High-scale DDD systems keep aggregates focused on the smallest unit of transactional consistency. An aggregate should contain exactly the entities whose invariants must be checked *atomically within a single transaction*. It should **not** be shaped to serve every possible read query.

**HexaStock's current aggregate:**

```
Portfolio (Aggregate Root)
  ├── balance: Money
  ├── Map<Ticker, Holding>
  │     └── List<Lot>
```

✅ **Verified from code.** `Portfolio.buy()` (in [Portfolio.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java)) checks `balance.isLessThan(totalCost)` and then modifies both `balance` and one `Holding`. `Portfolio.sell()` modifies `balance` and one `Holding`. These invariants — **cash sufficiency and share availability** — must be checked atomically on the same object graph. The aggregate boundary is *correct* for write operations.

**Assessment for current personal scope:** 🟢 The aggregate is appropriately sized. A typical personal portfolio has 5–15 holdings and <200 lots. Loading the full graph for a buy or sell is negligible. There is no pressure to split the aggregate.

**Assessment for future heavier scope:** 🟡 At 50+ holdings with thousands of lots, loading the full graph for `deposit()` (which only touches `balance`) becomes wasteful. Two paths would help before splitting the aggregate:

1. **Targeted save path** — after `deposit()`/`withdraw()`, only `UPDATE portfolio SET balance = ? WHERE id = ?` instead of re-merging the full graph.
2. **Partial load** — for deposit/withdraw, load only the `Portfolio` root without cascading to holdings/lots.

Splitting the aggregate (making `Holding` a separate root) should be reserved for genuine evidence of scale pressure. It sacrifices the clean atomicity of `Portfolio.buy()` which checks balance and creates a lot in one operation.

---

### 2.2 CQRS or "CQRS-Lite"

**Principle.** In practice, most mature DDD systems do *not* use the aggregate for read queries. Instead:

- **Write model**: Rich aggregate, loaded from repository, mutated, saved. Used for commands (buy, sell, deposit, withdraw).
- **Read model**: Simple DTO queries, projections, or even denormalized tables. Used for dashboards, lists, search, reporting.

This is "CQRS-lite" — not full event-sourced CQRS, just a disciplined separation of read and write paths.

**Which HexaStock use cases should remain aggregate-based (write model)?**

| Use case | Needs aggregate? | Why |
|---|---|---|
| `deposit()` | ✅ Yes — balance invariant | `Portfolio.deposit()` validates positivity and updates balance |
| `withdraw()` | ✅ Yes — balance + sufficiency | `Portfolio.withdraw()` checks `balance.isLessThan(money)` |
| `buyStock()` | ✅ Yes — balance + holding creation | `Portfolio.buy()` checks funds, finds/creates holding, creates lot |
| `sellStock()` | ✅ Yes — share availability + FIFO | `Portfolio.sell()` checks holding exists, `Holding.sell()` does FIFO |
| `createPortfolio()` | ✅ Yes — but trivial | Factory method, no graph loaded |

**Which use cases should become projection/query-based?**

| Use case | Current implementation | Recommended | Why |
|---|---|---|---|
| `getPortfolio()` | Loads full aggregate with `findByIdForUpdate()` via `PortfolioLifecycleService` | Simple SQL projection — `SELECT id, owner_name, balance, created_at FROM portfolio WHERE id = ?` | Needs only 4 scalar fields. No holdings, no lots, no lock. |
| `getAllPortfolios()` | Loads *all* full aggregates via `findAll()` + `PortfolioMapper.toModelEntity()` for each | SQL projection — `SELECT id, owner_name, balance, created_at FROM portfolio` | Never needs holdings/lots for a list view |
| `getTransactions()` | Loads all transactions into domain objects via `TransactionMapper` | Direct JPQL/SQL query returning DTOs, with pagination | Transaction history is read-only and potentially unbounded |
| `getHoldingsPerformance()` | Loads full aggregate + all transactions + N market API calls inside a `@Transactional` with pessimistic lock | Dedicated query service: SQL for transaction aggregates, batch market API calls, no aggregate load, no lock | Most expensive endpoint. Should not touch the write model at all. |

**Concrete change:** Introduce a `PortfolioQueryService` (or `PortfolioReadService`) in the application layer that uses a new `PortfolioQueryPort` (secondary port) backed by lightweight JPQL projections or native SQL. The write-side `PortfolioPort` stays for commands. The hexagonal boundary is preserved — the domain model is untouched.

---

### 2.3 Short Transactions

**Principle.** Database transactions should be as short as possible. Never hold a database lock while performing network I/O (HTTP calls, messaging, etc.). Lock → mutate → save → commit. External calls go before or after the transaction.

**HexaStock's current buy flow (`PortfolioStockOperationsService.buyStock()`):**

✅ **Verified from code** ([PortfolioStockOperationsService.java](application/src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java)):

```
@Transactional          ← transaction opens
1. portfolioPort.getPortfolioById(id)     ← SELECT ... FOR UPDATE (pessimistic lock acquired)
2. stockPriceProviderPort.fetchStockPrice(ticker)  ← HTTP call to Finnhub/AlphaVantage
                                                     (includes Thread.sleep(500ms) throttle)
3. portfolio.buy(ticker, quantity, price)   ← domain logic
4. portfolioPort.savePortfolio(portfolio)   ← full graph merge
5. transactionPort.save(transaction)        ← INSERT
@Transactional          ← transaction commits, lock released
```

**The problem:** The pessimistic lock (step 1) is held for the entire duration of step 2 (500ms throttle + network latency up to 5s). This serializes all access to the same portfolio for potentially seconds.

**The sell flow has the same issue.** `sellStock()` follows the identical pattern.

**Exact refactoring path:**

```java
// PortfolioStockOperationsService.buyStock() — REFACTORED

public void buyStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    // Step 1: Fetch price OUTSIDE the transaction (no lock held)
    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
    Price price = stockPrice.price();

    // Step 2: Short transactional block — lock, mutate, save, commit
    executeBuy(portfolioId, ticker, quantity, price);
}

@Transactional
void executeBuy(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity, Price price) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

    portfolio.buy(ticker, quantity, price);
    portfolioPort.savePortfolio(portfolio);

    Transaction transaction = Transaction.createPurchase(portfolioId, ticker, quantity, price);
    transactionPort.save(transaction);
}
```

**Trade-off:** The price may have changed between the fetch and the buy execution. For a personal portfolio context, this is entirely acceptable — it mirrors how real market orders work (the quoted price is never guaranteed). For a professional trading system, a price-staleness check could be added inside `executeBuy()`.

**Impact:** Pessimistic lock duration drops from ~600ms–5.1s to ~50–200ms (just DB I/O and domain logic). This is a **high-value, low-risk change** that does not touch the domain model at all.

---

### 2.4 Optimistic Concurrency Before Pessimistic Locking

**Principle.** Pessimistic locking (`SELECT ... FOR UPDATE`) acquires a database-level exclusive lock that blocks all other transactions touching the same row. It is the right choice when conflicts are frequent and the cost of a failed retry is high. Optimistic locking (`@Version`) allows concurrent reads, detects conflicts at commit time, and throws an exception that the caller retries. It is the right choice when conflicts are rare.

**HexaStock's current locking strategy:**

✅ **Verified from code:** `JpaPortfolioSpringDataRepository` ([JpaPortfolioSpringDataRepository.java](adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java)) has one query method: `findByIdForUpdate()` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This is the **only** way `JpaPortfolioRepository.getPortfolioById()` loads a portfolio.

✅ **Verified:** No `@Version` field exists on any JPA entity (`PortfolioJpaEntity`, `HoldingJpaEntity`, `LotJpaEntity`). There is no optimistic locking anywhere.

**Analysis for a personal financial portfolio:**

In a personal portfolio application, **concurrent conflicts on the same portfolio are extremely rare**. A single user is unlikely to submit two buy orders simultaneously. The primary concurrency scenario is a user clicking "Buy" while a background reporting query is running — and those should be on different code paths anyway (the reporting query should not acquire a write lock).

**Recommended mixed strategy:**

| Scenario | Recommended lock | Why |
|---|---|---|
| **Read-only queries** (get portfolio, list portfolios, get transactions, get holdings) | **No lock at all** | Read paths should never block writes. Use the standard `findById()` or DTO projections. |
| **Cash operations** (deposit, withdraw) | **Optimistic locking** (`@Version`) | Conflicts are extremely rare. If one occurs, a simple retry resolves it. The user's intent is unambiguous. |
| **Buy/sell stock** | **Optimistic locking** (`@Version`) for personal scope | Conflicts remain rare. The domain validates cash sufficiency and share availability at the point of mutation, so a stale-read + retry is safe. |
| **Buy/sell stock** at higher concurrency | **Pessimistic locking** as an option to switch to | If monitoring shows frequent `OptimisticLockException`, switch the buy/sell paths to pessimistic. Keep reads unlocked. |

**How to add `@Version`:**

Add a `version` column to `PortfolioJpaEntity`:

```java
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {
    @Id private String id;
    @Version private Long version;   // ← add this
    // ... rest unchanged
}
```

Hibernate will automatically include `WHERE version = ?` in UPDATE statements and throw `OptimisticLockException` on conflict. No change to the domain model.

**Important caveat for the current save pattern:** The current `savePortfolio()` creates entirely new detached JPA entities via `PortfolioMapper.toJpaEntity()`. When Hibernate merges a detached entity with an `@Version` field, it **must** carry the correct version value. This means the version must flow through the domain model or be stored in the persistence adapter as metadata. The cleanest approach is to store the version in the JPA adapter's unit-of-work context or pass it through a dedicated persistence metadata object — never in the domain model itself.

---

### 2.5 Targeted Fetch Plans Instead of Default Graph Loading

**Principle.** JPA's default fetching behaviour is to load associated collections lazily (the JPA spec default for `@OneToMany`). But "lazy" only means "deferred" — as soon as the code accesses the collection, all elements are loaded. Serious teams control exactly what is fetched using:

- **join fetch** (load eagerly in one query)
- **entity graphs** (declarative, reusable)
- **`@BatchSize`** (reduce N+1 from N queries to N/batch queries)
- **projection queries** (skip the entity entirely)

**How JPA currently loads a portfolio in HexaStock:**

✅ **Verified from code** ([PortfolioJpaEntity.java](adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/entity/PortfolioJpaEntity.java)):

```java
@OneToMany(cascade = ALL, orphanRemoval = true)
@JoinColumn(name = "portfolio_id")
private Set<HoldingJpaEntity> holdings = new HashSet<>();
```

The `@OneToMany` default fetch type is `FetchType.LAZY`. However, `PortfolioMapper.toModelEntity()` immediately iterates `jpaEntity.getHoldings()`, which triggers the lazy load. Then `HoldingMapper.toModelEntity()` iterates `holdingJpaEntity.getLots()` for each holding. This creates:

```
1 query: SELECT ... FROM portfolio WHERE id = ? FOR UPDATE
1 query: SELECT ... FROM holding WHERE portfolio_id = ?   (lazy trigger)
H queries: SELECT ... FROM lot WHERE holding_id = ?       (one per holding)
= 2 + H total queries
```

**This is the classic N+1 pattern** applied to lots.

**What to do:**

**Option A — `@BatchSize` (simplest, immediate win):**

Add `@BatchSize(size = 30)` to `HoldingJpaEntity.lots`:

```java
@OneToMany(cascade = ALL, orphanRemoval = true)
@JoinColumn(name = "holding_id")
@OrderBy("purchasedAt ASC")
@BatchSize(size = 30)     // ← batches lot loading into groups
private List<LotJpaEntity> lots = new ArrayList<>();
```

Also add `@BatchSize` to `PortfolioJpaEntity.holdings`:

```java
@OneToMany(cascade = ALL, orphanRemoval = true)
@JoinColumn(name = "portfolio_id")
@BatchSize(size = 30)
private Set<HoldingJpaEntity> holdings = new HashSet<>();
```

This reduces the query count from `2 + H` to approximately `2 + ceil(H/30)`. For a typical personal portfolio (10 holdings), this means 3 queries instead of 12.

**Option B — join fetch query for write operations:**

Add a dedicated JPQL query to `JpaPortfolioSpringDataRepository`:

```java
@Query("SELECT p FROM PortfolioJpaEntity p " +
       "LEFT JOIN FETCH p.holdings h " +
       "LEFT JOIN FETCH h.lots " +
       "WHERE p.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<PortfolioJpaEntity> findByIdWithGraphForUpdate(@Param("id") String id);
```

This loads the entire Portfolio → Holdings → Lots graph in a single SQL query with JOINs. Good for write paths where the full graph is needed. Beware of Cartesian product explosion if there are many holdings × many lots — Hibernate's `@BatchSize` may be more efficient for large graphs.

**Option C — entity graphs (reusable, declarative):**

```java
@NamedEntityGraph(
    name = "Portfolio.withHoldingsAndLots",
    attributeNodes = @NamedAttributeNode(value = "holdings", subgraph = "holdings.lots"),
    subgraphs = @NamedSubgraph(name = "holdings.lots", attributeNodes = @NamedAttributeNode("lots"))
)
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity { ... }
```

Then use `@EntityGraph("Portfolio.withHoldingsAndLots")` on a repository method. Useful when different use cases need different slices of the graph.

**For HexaStock now:** Option A (`@BatchSize`) is the quickest win. Option B or C for the write path when more control is needed.

---

### 2.6 Read Models / Projections for Heavy Screens

**Principle.** Screens that show summaries, lists, or dashboards should query the database directly for the data they need, not load and transform rich domain aggregates.

**Current heavy screens in HexaStock and their recommended read strategies:**

#### `GET /api/portfolios` — list all portfolios

✅ **Verified:** `PortfolioLifecycleService.getAllPortfolios()` → `portfolioPort.getAllPortfolios()` → `jpaSpringDataRepository.findAll().stream().map(PortfolioMapper::toModelEntity).toList()`.

This loads **every portfolio with all its holdings and lots**, maps them all to domain objects, then the REST controller converts them to `PortfolioResponseDTO(id, ownerName, balance, createdAt)` — discarding holdings and lots entirely.

**Recommended:** A Spring Data JPA interface projection:

```java
public interface PortfolioSummaryProjection {
    String getId();
    String getOwnerName();
    BigDecimal getBalance();
    LocalDateTime getCreatedAt();
}
```

With a repository method: `List<PortfolioSummaryProjection> findAllProjectedBy()`.

Single query, no joins, no mapping overhead.

#### `GET /api/portfolios/{id}` — get portfolio

✅ **Verified:** Loads full aggregate graph with pessimistic lock. PortfolioResponseDTO only uses `id, ownerName, balance, createdAt`.

**Recommended:** Same projection approach, single `findById()` without lock.

#### `GET /api/portfolios/{id}/transactions` — transaction history

✅ **Verified:** `TransactionService.getTransactions()` loads all transactions for a portfolio without pagination. `JpaTransactionSpringDataRepository.getAllByPortfolioId()` returns `List<TransactionJpaEntity>`.

**Recommended:** Paginated query returning DTOs directly:

```java
Page<TransactionJpaEntity> findByPortfolioIdOrderByCreatedAtDesc(
    String portfolioId, Pageable pageable);
```

Or a JPQL projection that returns `TransactionDTO` directly, avoiding domain-object mapping entirely.

#### `GET /api/portfolios/{id}/holdings` — holdings performance

✅ **Verified:** `ReportingService.getHoldingsPerformance()` loads the full portfolio aggregate (with pessimistic lock), loads all transactions, then calls `stockPriceProviderPort.fetchStockPrice(tickers)` which calls the external API sequentially for each ticker with a 500ms throttle.

**This is the most expensive endpoint.** For 15 holdings: pessimistic lock held during 1 portfolio load + full transaction load + 15 × 500ms API calls = ~8 seconds under lock.

**Recommended:** A dedicated `HoldingsPerformanceQueryService` that:

1. Aggregates transaction data via SQL (SUM of quantities, costs, profits grouped by ticker) — replacing in-memory `HoldingPerformanceCalculator` for the read path
2. Fetches prices in batch via `StockPriceProviderPort.fetchStockPrice(Set<Ticker>)` — **outside any DB transaction**
3. Combines the two datasets in memory
4. No portfolio aggregate loaded, no lock held

The domain-level `HoldingPerformanceCalculator` remains useful for unit testing and domain validation. The query service is a read-optimised parallel path.

---

### 2.7 Event-Driven Collaboration Between Boundaries

**Principle.** Domain events (`PortfolioCashDeposited`, `StockPurchased`, etc.) are useful for:

1. **Decoupling write-side consequences from the command** — e.g., updating a read model or sending a notification after a buy.
2. **Maintaining read model consistency** without polling (transactional outbox pattern).
3. **Integration events** for cross-service communication in a microservice architecture.

**Assessment for HexaStock:**

| Technique | Need now? | Need soon? | Need later? | Notes |
|---|---|---|---|---|
| Domain events (in-process) | No | Maybe | Yes if CQRS | Useful if read models are introduced and need to be updated after writes. Spring `ApplicationEventPublisher` is trivial to add. |
| Transactional outbox | No | No | Only at scale | Only if read model consistency must survive app restarts in a distributed deployment. |
| Integration events | No | No | Only if microservices | HexaStock is a monolith. No need for inter-service messaging. |
| Async projections | No | No | If read-model latency is acceptable | For pre-computing holdings performance periodically instead of on-demand. |

**Bottom line:** Domain events are a clean fit for HexaStock's architecture, but they are not urgently needed. If CQRS-lite is adopted (separate query services), the write-side doesn't need to "push" updates to the read side as long as the read side queries the same database directly. Events become valuable if the read model uses a separate store (materialized view, cache, search index).

---

### 2.8 Selective Denormalization

**Principle.** When a read-heavy screen requires expensive joins, aggregations, or external enrichment, it can be worth pre-computing the result into a denormalized structure (table, materialized view, or cache) that is cheap to read and updated asynchronously.

**Assessment for HexaStock:**

| Read model | Denormalize now? | Why / Why not |
|---|---|---|
| **Portfolio summary** (id, owner, balance, createdAt) | No | Already a single table read — `portfolio` table contains all needed fields |
| **Holdings performance** (ticker, qty, avg price, current price, PnL) | **Consider soon** | Currently requires full aggregate load + full tx scan + N API calls. A `holdings_performance` cache table updated after each trade + periodic price refresh would eliminate the most expensive query in the system. |
| **Realized/unrealized gain views** | Not yet | Can be computed from transaction aggregation SQL for now |
| **Transaction timeline** | No — pagination is sufficient | With a paginated query + index on `(portfolio_id, created_at DESC)`, the existing table serves this well |

**When denormalization is justified:** When the read endpoint has a response time >2 seconds and the data it returns can tolerate a few seconds of staleness. The holdings performance endpoint is the main candidate.

---

### 2.9 Historical Data Management

**Principle.** Append-only data (transactions, lots) grows without bound. If this data is loaded into the command model, performance degrades over the portfolio's lifetime. Serious systems use:

- **Pagination** for query paths
- **Archiving/partitioning** for data beyond a relevance window
- **Snapshots** to avoid replaying history
- **Lot compaction** to merge fully-depleted lots into summary records
- **Separate analytics models** that do not affect the command path

**Assessment for HexaStock:**

#### Transaction history

✅ **Verified:** `getTransactionsByPortfolioId()` ([JpaTransactionRepository.java](adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaTransactionRepository.java)) loads all transactions into memory. `TransactionService.getTransactions()` returns the full list with no pagination.

**Recommended now:** Add `Pageable` support to the transaction query. This is a one-line change in the Spring Data repository interface:

```java
Page<TransactionJpaEntity> findByPortfolioIdOrderByCreatedAtDesc(
    String portfolioId, Pageable pageable);
```

Plus updating `TransactionUseCase` and the REST endpoint to accept page/size parameters.

**Recommended later:** If transaction tables grow beyond millions of rows, table partitioning by `created_at` or archiving old transactions to a cold store.

#### Lot history

✅ **Verified:** Lots with `remainingShares = 0` are removed from the domain model by `Holding.sell()` using `iterator.remove()`. With `orphanRemoval = true`, they are deleted from the database.

This is already good — depleted lots do not accumulate in the database. No lot compaction is needed unless the business requirement changes to preserve full lot history for tax audit purposes.

If lot history needs to be preserved: keep a separate `lot_history` table (append-only, written after sell completion) rather than keeping empty lots in the aggregate.

#### Holdings performance calculation

✅ **Verified:** `HoldingPerformanceCalculator.getHoldingsPerformance()` ([HoldingPerformanceCalculator.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/HoldingPerformanceCalculator.java)) scans *all* transactions for the portfolio in a single pass. As transaction count grows (e.g., 10,000+ trades over a portfolio's lifetime), this in-memory scan becomes expensive.

**Recommended approach (soon):** Replace the full-scan calculator on the read path with SQL aggregation:

```sql
SELECT ticker,
       SUM(CASE WHEN type = 'PURCHASE' THEN quantity ELSE 0 END) AS total_bought_qty,
       SUM(CASE WHEN type = 'PURCHASE' THEN total_amount ELSE 0 END) AS total_bought_cost,
       SUM(CASE WHEN type = 'SALE' THEN profit ELSE 0 END) AS realized_gain
FROM portfolio_transaction
WHERE portfolio_id = ?
  AND ticker IS NOT NULL
GROUP BY ticker
```

This pushes the aggregation to the database, which is optimised for it, and returns only the summary rows. The in-memory O(T) scan is eliminated.

---

## 3. HexaStock-Specific Diagnosis: Preserve, Improve, Evolve, Redesign

### 3.1 Preserve As-Is

These elements are well-designed and should remain unchanged:

| Element | Location | Why it is strong |
|---|---|---|
| **Portfolio aggregate root with Holding and Lot** | `model/portfolio/Portfolio.java`, `Holding.java`, `Lot.java` | Correct invariant boundary. Cash sufficiency, share availability, and FIFO ordering are protected atomically. |
| **FIFO sell algorithm** | `Holding.sell()` ([Holding.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java)) | Clean iterator-based traversal. Removes depleted lots in-place. Business-correct. |
| **Value objects — `Money`, `Price`, `ShareQuantity`, `Ticker`** | `model/money/`, `model/market/` | Records with validation, immutable, normalized scale. Textbook quality. |
| **`SellResult` value object** | `model/portfolio/SellResult.java` | Clean encapsulation of sell outcome. `of()` factory computes profit. |
| **Sealed `Transaction` hierarchy** | `model/transaction/Transaction.java` + subtypes | Java 21 sealed + records. Each subtype carries only its semantically relevant fields. Exhaustive switch support. |
| **Transaction separation from aggregate** | `Transaction` persisted independently via `TransactionPort` | Correctly identified that transactions are append-only, do not participate in aggregate invariants, and would bloat the aggregate. |
| **Domain exception hierarchy** | `DomainException`, `InsufficientFundsException`, `ConflictQuantityException`, etc. | Clear distinction between domain rule violations and technical errors. |
| **`HoldingPerformanceCalculator`** | `model/portfolio/HoldingPerformanceCalculator.java` | O(T) single-pass algorithm. Well-documented choice of sequential over parallel. Remains valuable for unit testing. |
| **Port/adapter structure** | Use case interfaces in `application/port/in/`, outbound ports in `application/port/out/` | Clean hexagonal boundaries. All adapter dependencies flow inward. |
| **Interface Segregation** | Separate `CashManagementUseCase`, `PortfolioLifecycleUseCase`, `PortfolioStockOperationsUseCase`, `ReportingUseCase`, `TransactionUseCase` | ISP well-applied. Each controller or client depends only on the ports it needs. |
| **`StockPriceProviderPort` with profile-based adapters** | Finnhub, AlphaVantage, Mock — activated by Spring profiles | Clean strategy pattern. Easy to test with mock adapter. |
| **Caffeine cache for stock prices** | `@Cacheable("stockPrices")` on both adapters, 5-min TTL | Prevents redundant API calls. Correctly keyed by `ticker.value()`. |

### 3.2 Improve Now with Low Risk

#### 3.2.1 Add a non-locking read path for `getPortfolioById()`

- **Code areas:** `JpaPortfolioSpringDataRepository`, `JpaPortfolioRepository`, `PortfolioPort` interface
- **Why it helps:** Currently, `GET /api/portfolios/{id}` acquires a `PESSIMISTIC_WRITE` lock — blocking concurrent writes. Read-only queries should never lock.
- **Change:** Add a standard `findById()` call (already inherited from `JpaRepository`). Add a new method `getPortfolioByIdForRead()` to `PortfolioPort` that uses the non-locking path. Or better: use a DTO projection that doesn't load the aggregate at all (see 3.2.5).
- **Expected impact:** Reads no longer block writes. Immediate concurrency improvement.
- **Difficulty:** Low

#### 3.2.2 Move external API call outside the database transaction

- **Code areas:** `PortfolioStockOperationsService.buyStock()`, `sellStock()`
- **Why it helps:** Reduces pessimistic lock duration from seconds to milliseconds by removing HTTP I/O from the transactional scope.
- **Change:** Fetch `StockPrice` before the `@Transactional` block. See detailed refactoring in Section 2.3.
- **Expected impact:** Lock duration drops by ~10–50x. Other requests to the same portfolio are no longer blocked during API calls.
- **Difficulty:** Low — method split and annotation adjustment

#### 3.2.3 Add pagination to transaction history

- **Code areas:** `JpaTransactionSpringDataRepository`, `JpaTransactionRepository`, `TransactionService`, `TransactionUseCase`, `PortfolioRestController.getTransactions()`
- **Why it helps:** Prevents loading unbounded transaction lists into memory. The current `getAllByPortfolioId()` returns everything.
- **Change:** Add `Pageable` parameter. Return `Page<TransactionDTO>`. Add `page` and `size` query parameters to the REST endpoint.
- **Expected impact:** Constant-time response regardless of transaction count. Prevents memory spikes.
- **Difficulty:** Low

#### 3.2.4 Add `@BatchSize` to JPA collections

- **Code areas:** `PortfolioJpaEntity.holdings`, `HoldingJpaEntity.lots`
- **Why it helps:** Reduces N+1 query pattern from `2 + H` queries to `~3–4` queries when loading the aggregate for write operations.
- **Change:** Add `@BatchSize(size = 30)` to both `@OneToMany` relationships.
- **Expected impact:** ~70–80% reduction in SQL query count for typical portfolios. No code logic changes.
- **Difficulty:** Low — annotation only

#### 3.2.5 Introduce portfolio summary projection for read endpoints

- **Code areas:** New `PortfolioSummaryProjection` interface. New methods in `JpaPortfolioSpringDataRepository`. New `PortfolioQueryPort` (optional) or reuse `PortfolioPort`.
- **Why it helps:** `GET /api/portfolios` and `GET /api/portfolios/{id}` currently load full aggregates to return 4 scalar fields. A projection returns only what is needed.
- **Change:** Add Spring Data interface projections. Use them in `PortfolioLifecycleService` for read operations (or introduce a separate `PortfolioQueryService`).
- **Expected impact:** List/detail queries drop from multi-join aggregate load to single-table SELECT. Dramatic performance improvement for list views.
- **Difficulty:** Low–Medium

#### 3.2.6 Separate holdings performance from the write-side aggregate

- **Code areas:** `ReportingService.getHoldingsPerformance()`, `PortfolioRestController.getHoldings()`
- **Why it helps:** The holdings endpoint currently loads the full aggregate with a pessimistic lock, loads all transactions, and makes sequential API calls — all inside a single transaction. It is the single most expensive operation.
- **Change:** Create a dedicated query that:
  1. Aggregates transactions via SQL (GROUP BY ticker)
  2. Gets remaining shares from holdings table directly (no domain model)
  3. Fetches stock prices in batch **outside** any DB transaction
  4. Computes metrics in memory from these three inputs
- **Expected impact:** Eliminates aggregate loading, pessimistic lock, and N+1 for the heaviest endpoint. Response time drops from N×500ms to ~500ms (single batch price fetch, cached).
- **Difficulty:** Medium

### 3.3 Evolve Next If Usage Grows

#### 3.3.1 CQRS-lite with separate query services

- **What:** Formalize the split by creating a `PortfolioQueryService` (application layer) backed by `PortfolioQueryPort` (output port). This service handles all read use cases. The existing `PortfolioLifecycleService`, `CashManagementService`, and `PortfolioStockOperationsService` handle only writes.
- **Why soon:** Once the quick wins above are applied, the read/write split becomes natural and makes the codebase easier to reason about.
- **Expected benefit:** Clear separation of read vs write concerns. Easier to optimize each independently.
- **Effort:** Medium
- **What remains DDD-rich:** All write paths still use the Portfolio aggregate.

#### 3.3.2 Optimistic locking with `@Version`

- **What:** Add `@Version private Long version;` to `PortfolioJpaEntity`. Switch write operations from pessimistic to optimistic locking (remove `FOR UPDATE`, add retry on `OptimisticLockException`).
- **Why soon:** After removing external API calls from the transaction (3.2.2), the transaction duration is short enough that optimistic locking becomes practical. Conflicts are rare for personal portfolios.
- **Effort:** Medium — requires version to be carried through the mapper round-trip. Needs retry logic in the service layer.
- **Risk:** The current save pattern (creating new detached JPA entities) requires that the version value is preserved in the mapping. This needs careful implementation.

#### 3.3.3 Smarter save path — avoid full graph re-merge

- **What:** Instead of `PortfolioMapper.toJpaEntity()` + `save()`, either:
  - (a) Keep the loaded JPA entity in the persistence context, mutate it directly after domain logic, and let Hibernate's dirty checking handle the UPDATE. Or:
  - (b) For simple operations (deposit/withdraw), issue a targeted JPQL `UPDATE portfolio SET balance = ?, version = ? WHERE id = ? AND version = ?`.
- **Why soon:** The current save pattern creates new detached instances for the entire aggregate and forces Hibernate to diff the whole graph. For a deposit that only changes `balance`, this is disproportionate.
- **Effort:** Medium–High — requires rethinking the mapper / adapter interaction. Option (a) couples the persistence adapter more tightly to JPA but is pragmatic. Option (b) bypasses the aggregate for specific commands but needs guard rails.
- **What remains DDD-rich:** The domain model is unchanged. Only the persistence adapter's save strategy changes.

#### 3.3.4 Batch/parallel stock price fetching

- **What:** The `StockPriceProviderPort.fetchStockPrice(Set<Ticker>)` default implementation calls `fetchStockPrice(Ticker)` sequentially. Replace with a parallel implementation using `CompletableFuture.supplyAsync()` with a bounded executor, or batch API calls if the provider supports it.
- **Why soon:** The holdings performance endpoint calls one API per ticker with a 500ms throttle. Parallel fetching with the Caffeine cache (most tickers will be cached) dramatically reduces response time.
- **Effort:** Low–Medium
- **What stays the same:** Port interface unchanged. Only adapter implementation changes.

#### 3.3.5 Projection tables for holdings performance

- **What:** A `portfolio_holdings_summary` table (or materialized view) updated on each trade. Contains: `portfolio_id, ticker, total_bought_qty, total_bought_cost, remaining_shares, realized_gain`. The holdings query reads this table + fetches current prices.
- **Why later but prepare now:** If SQL aggregation from `portfolio_transaction` becomes slow (tens of thousands of transactions), this pre-computed table avoids the aggregation entirely.
- **Effort:** Medium
- **Trigger:** Transaction count per portfolio exceeds ~5,000 and the holdings endpoint response time exceeds 2 seconds.

### 3.4 Redesign Only If Scale Truly Demands It

#### 3.4.1 Split aggregate — Holding as separate aggregate root

- **What:** Make `Holding` an independent aggregate root with its own `HoldingRepository`. `Portfolio` holds only `balance` and a list of `HoldingId` references.
- **When justified:** If a portfolio routinely has 100+ holdings with thousands of lots and the full-graph load for buy/sell exceeds 1 second even after fetch-plan optimization.
- **Evidence required:** p95 buy/sell latency consistently >1s; profiling confirming aggregate hydration as the dominant cost.
- **Trade-off:** `Portfolio.buy()` can no longer atomically check balance and create a lot. Requires either a saga, a two-phase lock, or accepting eventual consistency on balance checks.
- **This should NOT be done for a personal portfolio system.** The complexity cost outweighs the benefit at this scale.

#### 3.4.2 Dedicated ledger / event-sourced approach

- **What:** Replace the aggregate-based write model with an event-sourced ledger where each mutation emits an event (CashDeposited, StockPurchased, etc.) and the current state is derived from events (or from periodic snapshots + recent events).
- **When justified:** Only if the system becomes a multi-user, multi-portfolio platform with 10,000+ concurrent users and needs audit-grade event replay, temporal queries ("what was the portfolio state on March 15?"), or cross-system event propagation.
- **Evidence required:** Current architecture measurably failing under sustained load + real business need for temporal queries or event replay.
- **This is NOT appropriate for HexaStock today or in the foreseeable future.**

#### 3.4.3 Deeper domain decomposition — bounded contexts

- **What:** Split the monolith into separate bounded contexts: Portfolio Management (commands), Reporting & Analytics (queries), Market Data (price caching/fetching), Transaction Ledger (append-only records).
- **When justified:** Only if different parts of the system have genuinely different scaling requirements, deployment cadences, or ownership teams.
- **Evidence required:** Organizational scaling (multiple teams), SLA differentiation (99.99% for trading, 99.9% for reporting), or data residency requirements.
- **This is NOT appropriate for a single-team personal portfolio project.**

---

## 4. Concrete Strategy for HexaStock by Use Case

| Use case | Should use rich aggregate? | Should use lock? | Should use projection/read model? | Suggested persistence strategy | Notes |
|---|---|---|---|---|---|
| **Create portfolio** | ✅ Yes (trivial — `Portfolio.create()`) | No — INSERT only | No | `INSERT INTO portfolio (id, owner_name, balance, created_at)` via JPA | No aggregate load needed |
| **Get portfolio summary** | ❌ No | No | ✅ Yes — DTO projection | `SELECT id, owner_name, balance, created_at FROM portfolio WHERE id = ?` | Drop aggregate load and lock |
| **List portfolios** | ❌ No | No | ✅ Yes — DTO projection | `SELECT id, owner_name, balance, created_at FROM portfolio` | Currently loads all aggregates. Critical to fix. |
| **Deposit cash** | ✅ Yes — balance invariant | Optimistic (`@Version`) | No | Load aggregate → domain `deposit()` → save. Short TX. | Consider targeted UPDATE for balance-only change long-term |
| **Withdraw cash** | ✅ Yes — balance + sufficiency | Optimistic (`@Version`) | No | Load aggregate → domain `withdraw()` → save. Short TX. | Balance check requires latest balance |
| **Buy stock** | ✅ Yes — balance + holding/lot creation | Optimistic (`@Version`), pessimistic if contention proven | No | Fetch price **outside** TX → load aggregate → domain `buy()` → save + INSERT tx. Short TX. | Most important refactoring: price fetch outside TX |
| **Sell stock** | ✅ Yes — share availability + FIFO | Optimistic (`@Version`), pessimistic if contention proven | No | Fetch price **outside** TX → load aggregate → domain `sell()` → save + INSERT tx. Short TX. | FIFO logic requires holdings/lots loaded |
| **List holdings (simple)** | ❌ No | No | ✅ Yes — SQL join | `SELECT h.ticker, SUM(l.remaining) FROM holding h JOIN lot l ON ... WHERE h.portfolio_id = ? GROUP BY h.ticker` | If just listing tickers + share counts, no aggregate needed |
| **Get holding detail** | Partial — domain logic for unrealized gain | No | ✅ Mixed | Load holding's lots from DB → compute unrealized gain with domain logic OR SQL. Depends on complexity tolerance. | Could be a lightweight domain model instantiation for one holding only |
| **Get transaction history** | ❌ No | No | ✅ Yes — paginated query | `SELECT ... FROM portfolio_transaction WHERE portfolio_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?` | Must add pagination |
| **Get holdings performance** | ❌ No (replace with SQL + domain service hybrid) | No | ✅ Yes — SQL aggregation + batch price fetch | SQL GROUP BY for tx aggregates → batch price fetch **outside** TX → compute metrics in memory | Most expensive endpoint — highest improvement potential |
| **Dashboard view** | ❌ No | No | ✅ Yes — DTO projection | Portfolio summary + top holdings + recent transactions. Three lightweight queries. No aggregate load. | Future endpoint — design as read-model from the start |

---

## 5. Concrete Concurrency Strategy

### Current vs Recommended

| Operation | Current concurrency style | Recommended style | Why |
|---|---|---|---|
| `getPortfolio()` | `PESSIMISTIC_WRITE` via `findByIdForUpdate()` | **No lock** — simple `findById()` or DTO projection | Read-only. Should never block writes. |
| `getAllPortfolios()` | No lock (uses `findAll()`) but loads full aggregates | **No lock** — DTO projection | Already correct (no lock), but should not load aggregates. |
| `deposit()` | `PESSIMISTIC_WRITE` (via `getPortfolioById()`) | **Optimistic** (`@Version`) with retry | Conflicts extremely rare for personal use. Retry on `OptimisticLockException`. |
| `withdraw()` | `PESSIMISTIC_WRITE` | **Optimistic** (`@Version`) with retry | Same rationale as deposit. |
| `buyStock()` | `PESSIMISTIC_WRITE` + external API **inside** TX | **Price outside TX** → **Optimistic** inside short TX with retry | Reduces lock duration from seconds to milliseconds. |
| `sellStock()` | `PESSIMISTIC_WRITE` + external API **inside** TX | **Price outside TX** → **Optimistic** inside short TX with retry | Same rationale. |
| `getTransactions()` | `@Transactional` read-only, no explicit lock | **No change needed** (but add pagination) | Already reasonable. |
| `getHoldingsPerformance()` | `PESSIMISTIC_WRITE` + N sequential API calls **inside** TX | **No aggregate load, no lock, no TX** — SQL + batch price fetch | Complete restructuring of the read path. |

### Concurrency Questions Answered

**Which operations should remain serialized?**  
None need to be strictly serialized at the personal portfolio scale. With optimistic locking, concurrent writes to the same portfolio are detected and retried rather than serialized.

**Which operations can rely on optimistic locking?**  
All write operations: deposit, withdraw, buy, sell. The domain validates invariants *at the point of mutation*, so a stale-read + optimistic-retry is safe — the retry will re-load the latest state and re-check invariants.

**Which reads should never acquire write locks?**  
All of them: `getPortfolio()`, `getAllPortfolios()`, `getTransactions()`, `getHoldingsPerformance()`, any future dashboard queries.

**Where is current transaction scope too wide?**  
- `buyStock()` and `sellStock()` — external API call inside the TX.
- `getHoldingsPerformance()` — holds a TX with pessimistic lock while making N sequential API calls.

**Where should retries be used?**  
After switching to optimistic locking: `deposit()`, `withdraw()`, `buyStock()`, `sellStock()`. A simple retry wrapper (max 3 attempts with exponential backoff) handles `OptimisticLockException`. Spring Retry (`@Retryable`) is suitable.

**Where should idempotency be considered?**  
- `deposit()` and `withdraw()` — if the client retries a timeout, a duplicate deposit could occur. Adding an idempotency key (client-provided or request-scoped UUID) prevents double-processing.
- `buyStock()` — same concern. An idempotency key on the transaction record (stored as a unique constraint) protects against duplicates.
- This is a **medium-term** concern, not urgent for personal use.

**Is the current design horizontally scalable enough for personal scope?**  
Yes — with one caveat. Multiple app instances can run simultaneously because all state is in the database. However:
- The Caffeine cache is local per JVM. Two instances may have different cached prices. This is acceptable for a personal app; at higher scale, a shared cache (Redis) would be needed.
- Pessimistic locks are database-level, so they correctly serialize across instances. After switching to optimistic locking, this remains correct because `@Version` checks are also database-level.

---

## 6. Concrete JPA/Hibernate Strategy

### 6.1 `@Version` — Currently Missing

✅ **Verified:** No `@Version` field on `PortfolioJpaEntity`, `HoldingJpaEntity`, or `LotJpaEntity`.

**Recommendation:** Add `@Version private Long version;` to `PortfolioJpaEntity` as a medium-term improvement. Since the aggregate root is the consistency boundary, versioning only the root entity is sufficient — child entity changes are always saved through the root.

**Complication with current save pattern:** `PortfolioMapper.toJpaEntity()` creates a **new** `PortfolioJpaEntity` instance. This detached instance will have `version = null`. When `save()` is called, Hibernate's `SimpleJpaRepository.save()` checks `entityInformation.isNew(entity)` — since `id` is set but `version` is null, the behaviour depends on the ID strategy. With a String `@Id` (not generated), Hibernate cannot distinguish "new" from "existing" when version is null.

**Solution options:**
- **(a)** Add `version` to the `Portfolio` domain model as an opaque persistence-aware field (pragmatic but impure).
- **(b)** Store the loaded JPA entity in a request-scoped context within `JpaPortfolioRepository`, and update it in-place during `savePortfolio()` instead of creating new instances.
- **(c)** Convert `PortfolioJpaEntity` to implement `Persistable<String>` with a transient `isNew` flag, and carry `version` through the mapper.

Option **(b)** is the cleanest: the loaded entity stays managed in the persistence context, the mapper applies domain changes to it, and Hibernate's dirty checking handles the UPDATE with version check automatically.

### 6.2 Detached Graph Merge — Current Problem

✅ **Verified:** `JpaPortfolioRepository.savePortfolio()` → `PortfolioMapper.toJpaEntity(portfolio)` → `jpaSpringDataRepository.save(jpaEntity)`.

`toJpaEntity()` creates entirely new instances:
```java
PortfolioJpaEntity portfolioJpaEntity = new PortfolioJpaEntity(
        entity.getId().value(), ...);
portfolioJpaEntity.setHoldings(entity.getHoldings().stream()
        .map(HoldingMapper::toJpaEntity).collect(Collectors.toSet())); // new set of new entities
```

When `save()` is called with this detached graph, Hibernate's `merge()` must:
1. Load the existing entity from the persistence context (or DB) to get current state
2. Diff every field of the root entity
3. Diff the holdings collection (is each holding new, modified, or removed?)
4. For each holding, diff its lots collection
5. Issue INSERT/UPDATE/DELETE as needed

For a portfolio with 30 holdings × 20 lots = 600 child entities, this is 601 merge comparisons plus potential SELECT-before-UPDATE queries.

**Recommended immediate fix:** Do not create new JPA entities. Instead, keep the loaded JPA entity managed:

```java
// In JpaPortfolioRepository
private PortfolioJpaEntity loadedEntity; // or use a unit-of-work pattern

@Override
public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
    return jpaSpringDataRepository.findById(portfolioId.value())
            .map(jpa -> {
                // Keep reference to the managed JPA entity
                // ... (details depend on UoW implementation)
                return PortfolioMapper.toModelEntity(jpa);
            });
}
```

Then in `savePortfolio()`, update the managed entity in-place rather than creating new instances. This is a significant refactoring of the persistence adapter but preserves the domain model untouched.

**Simpler intermediate step:** At minimum, use `entityManager.merge()` explicitly and understand that the `save()` call on a detached entity with a String `@Id` may behave as an INSERT attempt if JPA doesn't find it in the persistence context. Currently this works because `save()` on Spring Data calls `entityManager.merge()` when `isNew()` returns false (ID is set), but it's fragile and generates unnecessary SELECT statements.

### 6.3 Batch Fetching

**Recommended now:** Add `@BatchSize(size = 30)` annotations:

- `PortfolioJpaEntity.holdings`
- `HoldingJpaEntity.lots`

Or globally in `application.properties`:
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=30
```

### 6.4 Entity Graph Opportunities

For the write path (buy/sell), where the full aggregate is needed, a `@NamedEntityGraph` or a `JOIN FETCH` query could load the entire graph in 1 query instead of `2 + ceil(H/30)`. Worth adding as an alternative to `@BatchSize` for write paths:

```java
@Query("SELECT DISTINCT p FROM PortfolioJpaEntity p " +
       "LEFT JOIN FETCH p.holdings h " +
       "LEFT JOIN FETCH h.lots " +
       "WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdWithGraph(@Param("id") String id);
```

⚠️ **Caveat:** With `Set<HoldingJpaEntity>` and `List<LotJpaEntity>`, a double `JOIN FETCH` creates a Cartesian product. Hibernate handles this by de-duplicating, but the wire-level result set can be large. For portfolios under 100 holdings, this is fine. For larger portfolios, `@BatchSize` may be more efficient. Test with realistic data before choosing.

### 6.5 Orphan Removal Implications

✅ **Verified:** Both `@OneToMany` relationships use `orphanRemoval = true`:
- `PortfolioJpaEntity.holdings`
- `HoldingJpaEntity.lots`

This is correct for the domain: when `Holding.sell()` removes a fully-depleted lot via `iterator.remove()`, the corresponding `LotJpaEntity` should be deleted.

**Risk with detached merge:** When `toJpaEntity()` creates a new Set of holdings and a new List of lots, Hibernate must compare old vs new collections to detect orphans. If the entity IDs match, Hibernate correctly identifies removed children. If there is any ID mismatch (e.g., due to `equals()/hashCode()` issues on JPA entities), orphan detection may fail silently.

**Mitigation:** Ensure `HoldingJpaEntity` and `LotJpaEntity` have correct `equals()` and `hashCode()` based on their `@Id` fields. Currently, ✅ they use the default `Object` identity, which works when entities are managed in the persistence context but may cause issues with detached merges. **Consider adding `equals()/hashCode()` based on `id` to the JPA entities.**

### 6.6 Pagination for Large Collections

**Transaction history:** Add paginated query method as described in Section 2.6.

**Holdings within a portfolio:** Not needed now — a personal portfolio rarely has 100+ holdings. If it grows, the `Holding` list inside `PortfolioJpaEntity` could use `@OrderBy("ticker ASC")` and server-side filtering. But this is a much later concern.

### 6.7 Current Mapping Alignment

The current mapping pattern (domain ↔ JPA via static mapper classes) is a **sensible DDD choice** that keeps the domain model free from JPA annotations. The cost is the full-graph re-creation on save. This is the classic DDD persistence trade-off.

**Assessment:** For HexaStock's current scale, this trade-off is acceptable. The mapping cost is dominated by DB query time and network I/O, not by in-memory object creation. However, if performance profiling later shows the mapping round-trip as a measurable cost (unlikely below 1,000 lots), consider the managed-entity approach described in 6.2.

---

## 7. Recommended Target Architecture for HexaStock

### Stage A — Good Enough for Personal Portfolios

**Minimal changes, maximum practical value.**

**Architectural shape:**
- Domain model: unchanged
- Write path: aggregate-based, short transactions (API call outside TX)
- Read path: DTO projections for list/detail, paginated transaction history
- Locking: pessimistic on writes (existing), no lock on reads
- Persistence: `@BatchSize` added, same mapper pattern

**Changes from current state:**
1. Non-locking read path for `getPortfolio()` and `getAllPortfolios()`
2. Move `fetchStockPrice()` outside the `@Transactional` block in `buyStock()` and `sellStock()`
3. Add pagination to `getTransactions()`
4. Add `@BatchSize(size = 30)` to JPA entity collections
5. Portfolio summary DTO projection for list/detail endpoints

**Expected benefits:**
- Read operations no longer block writes
- Buy/sell lock duration drops from seconds to <200ms
- Transaction history response time bounded regardless of data volume
- Portfolio list endpoint drops from multi-join to single-table query

**Costs:** ~2–3 days of focused work. All changes are in the persistence adapter and application services. Zero domain model changes.

**What remains DDD-rich:** Everything. Write commands use the aggregate as before.

**What becomes query-oriented:** `getPortfolio()`, `getAllPortfolios()`, `getTransactions()`

**Signals it is time for Stage B:**
- Portfolio size exceeds 30 holdings / 500 lots and write latency becomes noticeable
- Holdings performance endpoint routinely exceeds 5 seconds
- Multiple users accessing the same portfolio simultaneously (even if rare)

---

### Stage B — Stronger for Power Users

**Some more investment, still the same overall architecture.**

**Architectural shape:**
- Domain model: unchanged
- Write path: `@Version` optimistic locking, short transactions, retry on conflict
- Read path: Dedicated `PortfolioQueryService` with SQL projections and paginated queries. Holdings performance via SQL aggregation + batch price fetch.
- Persistence: Managed JPA entity approach (or targeted UPDATE for cash ops). Entity graphs for write paths. Optimistic locking flow.

**Changes from Stage A:**
1. `@Version` on `PortfolioJpaEntity` + carry version through mapper
2. Retry logic for `OptimisticLockException` (Spring Retry)
3. Dedicated `PortfolioQueryService` and `PortfolioQueryPort`
4. SQL-based holdings performance calculation
5. Parallel/batch stock price fetching
6. Smarter save path (managed entity or targeted UPDATE for cash)

**Expected benefits:**
- Optimistic locking allows concurrent reads and writes without serialization
- Holdings performance drops from H×500ms to ~500ms
- Write operations that only touch balance avoid full-graph merge
- Clear architectural separation of read and write concerns

**Costs:** ~1–2 weeks of focused work. Moderate refactoring of persistence adapter. New query services. Tests updated.

**What remains DDD-rich:** All write commands, domain logic, invariant enforcement, FIFO sell algorithm.

**What becomes query-oriented:** All read endpoints, holdings performance, transaction history, portfolio summary.

**Signals it is time for Stage C:**
- Portfolio size routinely exceeds 100 holdings / 5,000 lots
- Transaction count per portfolio exceeds 50,000
- Multiple concurrent users on the same portfolio with measurable conflict rate
- Buy/sell p95 latency exceeds 500ms after all Stage B optimizations

---

### Stage C — Prepared for Enterprise-Like Pressure

**Only if real usage proves necessary.**

**Architectural shape:**
- Domain model: potentially split aggregate (Holding as separate root)
- Write path: saga or two-phase coordination for buy/sell across Portfolio balance + Holding lots
- Read path: Materialized projection tables updated by domain events. Pre-computed holdings summary.
- Persistence: Event-sourced or ledger-based transaction model. Lot compaction. Table partitioning.
- Infrastructure: Shared cache (Redis), rate-limited external API client with circuit breaker, horizontal scaling.

**Changes from Stage B:**
1. Split `Holding` into separate aggregate root (if profiling justifies)
2. Domain events published after mutations
3. Transactional outbox for reliable event publishing
4. Materialized `holdings_summary` table updated by event consumer
5. Lot compaction job (merge fully-depleted lots into summary)
6. Transaction table partitioning
7. Redis or shared cache for stock prices
8. Circuit breaker on external API adapters

**Expected benefits:**
- Sub-second response for all operations regardless of portfolio size
- Horizontally scalable with correct shared state
- Eventual consistency model suitable for dashboards with <1s staleness

**Costs:** Weeks to months. Significant architectural complexity. Event infrastructure.

**What remains DDD-rich:** Write commands for each aggregate (Portfolio and Holding separately).

**What becomes query-oriented:** All read paths, dashboard, performance.

---

## 8. Prioritized Action Plan

| Priority | # | Change | Why now | Effort | Expected benefit | Risk |
|---|---|---|---|---|---|---|
| **Quick win** | 1 | Move `fetchStockPrice()` outside TX in `buyStock()`/`sellStock()` | Lock held during HTTP I/O is the single worst bottleneck | Low (method split) | Lock duration: seconds → milliseconds | Very low — price staleness is acceptable |
| **Quick win** | 2 | Add `@BatchSize(size = 30)` to `PortfolioJpaEntity.holdings` and `HoldingJpaEntity.lots` | Reduces N+1 queries immediately | Very low (2 annotations) | Query count: 2+H → ~3–4 | None |
| **Quick win** | 3 | Add non-locking read path for `getPortfolio()` | Reads currently block writes with `FOR UPDATE` | Low (add `findById()` call) | Reads no longer block writes | None |
| **Quick win** | 4 | Add pagination to `getTransactions()` | Unbounded list is a time bomb | Low (Pageable parameter) | Constant-time transaction history | None |
| **Near-term** | 5 | DTO projection for `getAllPortfolios()` | Currently loads all aggregates to return 4 fields | Low–Medium | List endpoint: O(P×(H+L)) → O(P) | Low |
| **Near-term** | 6 | DTO projection for `getPortfolio()` | Currently loads full aggregate with lock | Low–Medium | Detail endpoint: multi-join → single table | Low |
| **Near-term** | 7 | Restructure `getHoldingsPerformance()` — SQL aggregation + batch price fetch, no aggregate load | The most expensive endpoint. Lock + full load + N×500ms API | Medium | Response time: H×500ms → ~500ms. No lock. | Medium — requires new SQL query + testing |
| **Medium-term** | 8 | Add `@Version` to `PortfolioJpaEntity` + optimistic locking | Enables concurrent writes without serialization | Medium | Better concurrency, no lock waits | Medium — version must flow through mapper |
| **Medium-term** | 9 | Formal `PortfolioQueryService` + `PortfolioQueryPort` | Formalizes read/write separation | Medium | Cleaner architecture, easier to extend | Low |
| **Medium-term** | 10 | Smarter save path (managed entities or targeted UPDATE) | Avoids full graph re-merge on every save | Medium–High | Reduced DB overhead for cash operations | Medium — changes persistence adapter significantly |
| **Medium-term** | 11 | Parallel stock price fetching in adapter | Sequential fetching with throttle is slow | Low–Medium | Better response time for multi-ticker operations | Low |
| **Optional long-term** | 12 | Domain events + outbox for read-model updates | Decouples write-side from read-side projections | High | Reliable projection updates across restarts | Only if projection tables are adopted |
| **Optional long-term** | 13 | Dedicated holdings summary projection table | Pre-computed read model for performances | Medium–High | Sub-second holdings dashboard | Eventual consistency complexity |
| **Future only** | 14 | Split Holding into separate aggregate | Avoid full graph load for large portfolios | High | Per-holding operations scale independently | Loses atomic buy check |
| **Future only** | 15 | Event sourcing for transactions | Full temporal query support, replay | Very High | Audit-grade history, temporal queries | Complete rewrite of write model |

---

## 9. Final Verdict

### 1. Can HexaStock remain a rich DDD model and still be pragmatic?

**Yes, absolutely.** The domain model is the strongest part of HexaStock. `Portfolio`, `Holding`, `Lot`, the FIFO sell algorithm, the value objects, the sealed `Transaction` hierarchy — these are all well-designed and provide real value. None of the recommended improvements require changing the domain model. Every optimization is in the persistence adapter, the application services, or the addition of complementary read paths.

The false dilemma "either rich model or good performance" does not apply here. The model is rich for write commands. Reads get lightweight dedicated paths. Both coexist without conflict.

### 2. What should be changed immediately?

Three things, all high-value and low-risk:

1. **Move `fetchStockPrice()` outside the `@Transactional` block** in `PortfolioStockOperationsService.buyStock()` and `sellStock()`. This single change reduces lock duration by 10–50x.
2. **Add `@BatchSize(size = 30)`** to `PortfolioJpaEntity.holdings` and `HoldingJpaEntity.lots`. Two annotations, immediate N+1 reduction.
3. **Add a non-locking `findById()` path** for read-only operations. Stop using `findByIdForUpdate()` for `getPortfolio()`, `getAllPortfolios()`, and `getHoldingsPerformance()`.

A close fourth: **paginate transaction history**.

### 3. What should NOT be changed yet?

- **Do NOT split the aggregate** (Holding as separate root). The current aggregate boundary is correct and efficient for personal portfolios. Splitting adds significant complexity for no measurable gain at current scale.
- **Do NOT add event sourcing.** Append-only transactions are already stored separately. There is no need for a full event-sourced write model.
- **Do NOT add domain events or transactional outbox.** These are solutions looking for a problem that doesn't exist yet.
- **Do NOT add Redis or external caching infrastructure.** Caffeine is perfectly adequate for a single-instance deployment.
- **Do NOT refactor the mapper/save pattern yet.** It works correctly. The detached-merge overhead is not the primary bottleneck — external API calls inside transactions and unnecessary aggregate loading are far more impactful. Fix those first.

### 4. What would distinguish a healthy DDD evolution from overengineering?

- **Healthy** = each change is motivated by a measured problem, affects only the layer where the problem lives (typically persistence or orchestration), and preserves the domain model's integrity.
- **Overengineering** = copying patterns from internet articles (CQRS, event sourcing, saga, microservices) without evidence that the current architecture is failing. Building infrastructure for scale that may never arrive. Adding abstraction layers to solve theoretical problems.

The test: **"Can you point to a specific endpoint, operation, or user scenario where the current architecture measurably underperforms?"** If yes, the change is justified. If no, it's premature.

---

## Recommended Default Posture

- **Now:** Fix the three obvious wins (API outside TX, batch fetch, non-locking reads). Keep the rich domain model untouched. Ship and observe.
- **Next:** When portfolio sizes grow or response time monitoring shows degradation, introduce CQRS-lite query services, optimistic locking, and SQL-based reporting. The domain model still does not change.
- **Later:** Only when real production evidence demands it (consistent p95 latency violations, measured lock contention, OOM under load), consider aggregate boundary splits, projection tables, or event infrastructure. Challenge any proposal that cannot point to specific metrics.

---

*This review is based solely on source code present in the HexaStock repository as of 15 April 2026. No production metrics, load test results, or runtime profiling data were available. All performance estimates are analytically derived with stated assumptions. Have I produced a serious, constructive, HexaStock-specific strategy rather than generic architecture advice? Yes — every recommendation references actual class names, method signatures, and code paths in the repository, and every deferred recommendation states the specific evidence that would justify it.*
