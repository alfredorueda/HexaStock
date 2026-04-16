# HexaStock — Constructive Strategy for a Pragmatic Rich Domain Model

**Date:** 15 April 2026 (revised and expanded)
**Author:** Architectural Strategy Review (code-grounded analysis)
**Stack:** Java 21 · Spring Boot 3.5 · JPA/Hibernate · MySQL (configured) / PostgreSQL (target)
**Scope:** How to keep DDD and hexagonal architecture while achieving pragmatic persistence, scalable runtime behaviour, and honest cost analysis

---

## 1. Executive Summary

**HexaStock can and should keep its rich domain model.** The domain layer — `Portfolio`, `Holding`, `Lot`, `SellResult`, the sealed `Transaction` hierarchy, `HoldingPerformanceCalculator`, and the full suite of value objects — is well-designed, correctly protects real business invariants, and provides genuine pedagogical and structural value. The improvement path does not require replacing or hollowing out the model.

The analysis identifies **four categories** of findings:

| Category | Finding |
|---|---|
| **Preserve unconditionally** | Domain model, FIFO sell algorithm, value objects, sealed Transaction hierarchy, hexagonal port/adapter structure, interface segregation, Caffeine cache strategy |
| **Improve with low risk** | Move external API calls outside DB transactions; add non-locking read path; paginate transaction history; add `@BatchSize` to JPA collections |
| **Evolve when evidence warrants** | CQRS-lite query services; `@Version` optimistic locking; smarter JPA save path; batch/parallel price fetching; SQL-based reporting |
| **Redesign only if scale proves necessary** | Aggregate boundary split; event sourcing; dedicated projection tables; shared cache infrastructure |

Several current design choices deserve nuance rather than blanket criticism:

- **The 500 ms API throttle** is not an architectural defect — it is a deliberate constraint imposed by free-tier API rate limits (Finnhub, Alpha Vantage) and is explicitly documented as such in the codebase. The genuine improvement is not removing the throttle but ensuring it does not run *inside* a database transaction holding a lock.
- **Pessimistic locking** is a defensible pedagogical choice: it is simpler to reason about, eliminates retry logic, and is correct for all concurrency levels. Optimistic locking is a worthwhile evolution when contention is measured to be low, but the current approach is not broken — it is conservative.
- **Full aggregate loading** is proportional to the domain's actual size. A detailed memory analysis (Section 4) shows that even a power-user portfolio with 30 holdings and 750 lots consumes ~550 KB per request — well within comfortable bounds for any JVM deployment. The concern is not memory but lock duration and connection pool utilisation.

| Horizon | Goal | Recommended posture |
|---|---|---|
| **Now** | Personal portfolio — correct & clean | Fix the obvious: non-locking reads, external API outside transaction, pagination. Domain model untouched. |
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

**Assessment for current personal scope:** 🟢 The aggregate is appropriately sized. A typical personal portfolio has 5–15 holdings and <200 lots. Loading the full graph for a buy or sell is negligible (see Section 4 for detailed memory estimates). There is no pressure to split the aggregate.

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
| `createPortfolio()` | ✅ Yes (trivial — `Portfolio.create()`) | Factory method, no graph loaded |

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

**Context:** The 500 ms `Thread.sleep()` throttle in both `FinhubStockPriceAdapter` and `AlphaVantageStockPriceAdapter` exists because free-tier API keys (Finnhub free: 60 calls/minute; Alpha Vantage free: 5 calls/minute) enforce strict rate limits. The adapters' Javadoc explicitly documents this: *"Throttles outbound API calls to avoid hitting free-tier rate limits."* Additionally, the `@Cacheable(sync = true)` on both adapters means that repeated requests for the same ticker within the 5-minute cache TTL skip the throttle entirely.

**The improvement opportunity:** The throttle is justified for the API tier. The issue is that it runs *inside* the `@Transactional` method, so the pessimistic lock (step 1) is held for the entire duration of step 2. With the cache cold: 500 ms throttle + network latency (up to 5 s timeout). With the cache warm: near-zero. The refactoring is not "remove the throttle" but "fetch the price before opening the transaction":

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

**Trade-off:** The price may have changed between the fetch and the buy execution. For a personal portfolio context, this is entirely acceptable — it mirrors how real market orders work (the quoted price is never guaranteed). The Caffeine cache with 5-minute TTL means most buy/sell operations within a short interaction window use the same cached price anyway. For a professional trading system, a price-staleness check could be added inside `executeBuy()`.

**Impact:** Pessimistic lock duration drops from ~600 ms–5.1 s to ~50–200 ms (just DB I/O and domain logic). This is a **high-value, low-risk change** that does not touch the domain model at all.

---

### 2.4 Optimistic Concurrency Before Pessimistic Locking

**Principle.** Pessimistic locking (`SELECT ... FOR UPDATE`) acquires a database-level exclusive lock that blocks all other transactions touching the same row. Optimistic locking (`@Version`) allows concurrent reads, detects conflicts at commit time, and throws an exception that the caller retries. Each is appropriate in different contexts.

**HexaStock's current locking strategy:**

✅ **Verified from code:** `JpaPortfolioSpringDataRepository` ([JpaPortfolioSpringDataRepository.java](adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java)) has one query method: `findByIdForUpdate()` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This is the **only** way `JpaPortfolioRepository.getPortfolioById()` loads a portfolio.

✅ **Verified:** No `@Version` field exists on any JPA entity (`PortfolioJpaEntity`, `HoldingJpaEntity`, `LotJpaEntity`). There is no optimistic locking.

**Analysis for a personal financial portfolio:**

Pessimistic locking is a defensible choice for a pedagogical project. It has important advantages:

- **Simplicity**: no retry logic needed, no version-carrying through mappers, no risk of silent data loss from unhandled `OptimisticLockException`.
- **Correctness by construction**: if the lock is acquired, the transaction will succeed (no conflict-based failures).
- **Pedagogical clarity**: students can reason about "lock → mutate → save → release" without understanding optimistic concurrency control.

The main cost is that concurrent requests to the same portfolio are serialised at the database level. For a personal portfolio application where concurrent conflicts are extremely rare, this cost is low in practice. A single user is unlikely to submit two buy orders simultaneously.

**When to evolve toward optimistic locking:**

The trigger is not "pessimistic locking is wrong" but "pessimistic locking combined with long-held transactions causes measurable contention." After applying the refactoring in Section 2.3 (API calls outside the transaction), pessimistic lock duration drops to ~50–200 ms, which is already quite short. At that point, the benefits of switching to optimistic locking are modest for a single-user scenario.

If the system evolves toward multiple concurrent users on the same portfolio (e.g., a shared family portfolio), optimistic locking becomes more valuable:

| Scenario | Recommended lock | Why |
|---|---|---|
| **Read-only queries** (get portfolio, list portfolios, get transactions, get holdings) | **No lock at all** | Read paths should never block writes. Use the standard `findById()` or DTO projections. |
| **Cash operations** (deposit, withdraw) | **Optimistic locking** (`@Version`) | Conflicts are extremely rare. If one occurs, a simple retry resolves it. |
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

✅ **Verified:** Loads full aggregate graph with pessimistic lock. `PortfolioResponseDTO` only uses `id, ownerName, balance, createdAt`.

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

✅ **Verified:** `ReportingService.getHoldingsPerformance()` loads the full portfolio aggregate (with pessimistic lock), loads all transactions, then calls `stockPriceProviderPort.fetchStockPrice(tickers)` which calls the external API sequentially for each ticker — with the 500 ms throttle on cache-miss only (the Caffeine `@Cacheable` layer means repeated calls within 5 minutes are instant).

**Worst-case cost** (cold cache, 15 holdings): pessimistic lock held during 1 portfolio load + full transaction load + 15 × (500 ms throttle + network) ≈ 8 seconds under lock. **Typical-case cost** (warm cache): lock held for portfolio load + transaction load + 15 × ~1 ms = well under 1 second.

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

This pushes the aggregation to the database, which is optimised for it, and returns only the summary rows.

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
| **Caffeine cache for stock prices** | `@Cacheable("stockPrices")` on both adapters, 5-min TTL | Prevents redundant API calls. Correctly keyed by `ticker.value()`. `sync = true` prevents thundering herd. |
| **500 ms API throttle** | `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter` | Correctly rate-limits calls to free-tier APIs. Documented as pedagogical/infrastructure constraint. Not an architectural defect. |

### 3.2 Improve Now with Low Risk

#### 3.2.1 Add a non-locking read path for `getPortfolioById()`

- **Code areas:** `JpaPortfolioSpringDataRepository`, `JpaPortfolioRepository`, `PortfolioPort` interface
- **Why it helps:** Currently, `GET /api/portfolios/{id}` acquires a `PESSIMISTIC_WRITE` lock — blocking concurrent writes. Read-only queries should never lock.
- **Change:** Add a standard `findById()` call (already inherited from `JpaRepository`). Add a new method `getPortfolioByIdForRead()` to `PortfolioPort` that uses the non-locking path. Or better: use a DTO projection that doesn't load the aggregate at all (see 3.2.5).
- **Expected impact:** Reads no longer block writes. Immediate concurrency improvement.
- **Difficulty:** Low

#### 3.2.2 Move external API call outside the database transaction

- **Code areas:** `PortfolioStockOperationsService.buyStock()`, `sellStock()`
- **Why it helps:** Reduces lock duration from the throttle + network time to just DB I/O and domain logic, regardless of whether the lock is pessimistic or optimistic.
- **Change:** Fetch `StockPrice` before the `@Transactional` block. See detailed refactoring in Section 2.3.
- **Expected impact:** Lock duration drops from ~600 ms–5.1 s (cold cache) to ~50–200 ms. With warm cache the improvement is smaller but the change is still structurally correct.
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
- **Why it helps:** The holdings endpoint currently loads the full aggregate with a pessimistic lock, loads all transactions, and makes sequential API calls — all inside a single transaction. It is the most expensive operation, particularly on cache-cold scenarios.
- **Change:** Create a dedicated query that:
  1. Aggregates transactions via SQL (GROUP BY ticker)
  2. Gets remaining shares from holdings table directly (no domain model)
  3. Fetches stock prices in batch **outside** any DB transaction
  4. Computes metrics in memory from these three inputs
- **Expected impact:** Eliminates aggregate loading, pessimistic lock, and N+1 for the heaviest endpoint. Response time drops from N×500 ms (cold) to ~500 ms (single batch price fetch, cached).
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
- **Note:** The current pessimistic locking is not broken — it is conservative. This evolution is a refinement, not a bug fix.

#### 3.3.3 Smarter save path — avoid full graph re-merge

- **What:** Instead of `PortfolioMapper.toJpaEntity()` + `save()`, either:
  - (a) Keep the loaded JPA entity in the persistence context, mutate it directly after domain logic, and let Hibernate's dirty checking handle the UPDATE. Or:
  - (b) For simple operations (deposit/withdraw), issue a targeted JPQL `UPDATE portfolio SET balance = ?, version = ? WHERE id = ? AND version = ?`.
- **Why soon:** The current save pattern creates new detached instances for the entire aggregate and forces Hibernate to diff the whole graph. For a deposit that only changes `balance`, this is disproportionate.
- **Effort:** Medium–High — requires rethinking the mapper / adapter interaction. Option (a) couples the persistence adapter more tightly to JPA but is pragmatic. Option (b) bypasses the aggregate for specific commands but needs guard rails.
- **What remains DDD-rich:** The domain model is unchanged. Only the persistence adapter's save strategy changes.

#### 3.3.4 Batch/parallel stock price fetching

- **What:** The `StockPriceProviderPort.fetchStockPrice(Set<Ticker>)` default implementation calls `fetchStockPrice(Ticker)` sequentially. Replace with a parallel implementation using `CompletableFuture.supplyAsync()` with a bounded executor, or batch API calls if the provider supports it.
- **Why soon:** The holdings performance endpoint calls one API per ticker with a 500 ms throttle (on cache miss). Parallel fetching with the Caffeine cache (most tickers will be cached within the 5-minute TTL) dramatically reduces worst-case response time.
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
- **Evidence required:** p95 buy/sell latency consistently >1 s; profiling confirming aggregate hydration as the dominant cost.
- **Trade-off:** `Portfolio.buy()` can no longer atomically check balance and create a lot. Requires either a saga, a two-phase lock, or accepting eventual consistency on balance checks.
- **At HexaStock's current scale, this would add significant complexity without measurable benefit.** The memory analysis in Section 4 confirms that even a 750-lot aggregate is well under 1 MB.

#### 3.4.2 Dedicated ledger / event-sourced approach

- **What:** Replace the aggregate-based write model with an event-sourced ledger where each mutation emits an event (CashDeposited, StockPurchased, etc.) and the current state is derived from events (or from periodic snapshots + recent events).
- **When justified:** Only if the system becomes a multi-user, multi-portfolio platform with 10,000+ concurrent users and needs audit-grade event replay, temporal queries ("what was the portfolio state on March 15?"), or cross-system event propagation.
- **Evidence required:** Current architecture measurably failing under sustained load + real business need for temporal queries or event replay.
- **At HexaStock's current pedagogical scope, this would be overengineering.** The append-only `portfolio_transaction` table already provides a transaction history without the complexity of full event sourcing.

#### 3.4.3 Deeper domain decomposition — bounded contexts

- **What:** Split the monolith into separate bounded contexts: Portfolio Management (commands), Reporting & Analytics (queries), Market Data (price caching/fetching), Transaction Ledger (append-only records).
- **When justified:** Only if different parts of the system have genuinely different scaling requirements, deployment cadences, or ownership teams.
- **Evidence required:** Organizational scaling (multiple teams), SLA differentiation (99.99% for trading, 99.9% for reporting), or data residency requirements.
- **At HexaStock's current single-team scope, this adds accidental complexity without organizational justification.**

---

## 4. Memory Footprint per Request and Concurrency Envelope

This section provides a detailed, object-level memory analysis of the Portfolio aggregate to answer a concrete question: **at what portfolio size does memory become a genuine concern?**

### 4.1 JVM Assumptions

All estimates use the following assumptions, which apply to HexaStock's stated runtime:

| Parameter | Value | Notes |
|---|---|---|
| JVM | HotSpot 64-bit, Java 21 | As declared in `pom.xml` |
| Compressed oops | Enabled (default for heaps < 32 GB) | Reduces reference size from 8 B to 4 B |
| Object header | 12 bytes | 8 B mark word + 4 B compressed klass pointer |
| Padding | To 8-byte boundary | JVM specification requirement |
| Compressed reference | 4 bytes | Object pointers under compressed oops |
| Array header | 16 bytes | 12 B object header + 4 B length field |

These are standard HotSpot values; exact figures may vary slightly across JDK vendors but not materially.

### 4.2 Domain Object Footprint

Each value is derived from the actual fields declared in the source code.

#### Primitive value objects

| Type | Record fields | Shallow bytes | Nested objects | Deep bytes | Source |
|---|---|---|---|---|---|
| `ShareQuantity` | `int value` | 16 B | — | **~16 B** | [ShareQuantity.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/money/ShareQuantity.java) |
| `Money` | `BigDecimal amount` | 16 B | 1 × BigDecimal (~40 B compact) | **~56 B** | [Money.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Money.java) |
| `Price` | `BigDecimal value` | 16 B | 1 × BigDecimal (~40 B) | **~56 B** | [Price.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Price.java) |
| `Ticker` | `String value` | 16 B | 1 × String (4 chars → 48 B) | **~64 B** | [Ticker.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/market/Ticker.java) |
| `PortfolioId` | `String value` | 16 B | 1 × String (UUID 36 chars → 80 B) | **~96 B** | [PortfolioId.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/PortfolioId.java) |
| `HoldingId` | `String value` | 16 B | 1 × String (UUID → 80 B) | **~96 B** | [HoldingId.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/HoldingId.java) |
| `LotId` | `String value` | 16 B | 1 × String (UUID → 80 B) | **~96 B** | [LotId.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/LotId.java) |
| `LocalDateTime` | (nested `LocalDate` + `LocalTime`) | 24 B | 2 nested objects (24 B each) | **~72 B** | JDK built-in |

*BigDecimal "compact" means `intCompact` fits in a `long` (true for typical stock prices like 150.25 → stored as 15025 with scale 2). Object: 12 B header + 8 B long + 4 B scale + 4 B precision + 4 B intVal ref + 4 B stringCache ref = 36 → padded to 40 B.*

*String memory: 24 B (12 B header + 4 B byte[] ref + 1 B coder + 4 B hash + 1 B hashIsZero → padded to 24 B) + byte[] array (16 B header + length → padded to 8-byte boundary). UUID "550e8400-e29b-41d4-a716-446655440000" (36 Latin-1 chars): 24 B + 56 B = 80 B. Ticker "AAPL" (4 chars): 24 B + 24 B = 48 B.*

#### Composite domain objects

| Type | Fields (from source) | Shallow bytes | Deep bytes (excluding children) | Source |
|---|---|---|---|---|
| `Lot` | LotId id, ShareQuantity initialShares, ShareQuantity remainingShares, Price unitPrice, LocalDateTime purchasedAt | 32 B | **~288 B** (96 + 16 + 16 + 56 + 72 + 32 shell) | [Lot.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Lot.java) |
| `Holding` | HoldingId id, Ticker ticker, ArrayList<Lot> lots | 24 B | **~264 B** base + lots (96 + 64 + 80 ArrayList shell + 24 self) | [Holding.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java) |
| `Portfolio` | PortfolioId id, String ownerName, Money balance, LocalDateTime createdAt, HashMap<Ticker, Holding> holdings | 32 B | **~424 B** base + holdings (96 + 48 + 56 + 72 + 120 HashMap shell + 32 self) | [Portfolio.java](domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java) |

*ArrayList shell: 24 B (object) + 56 B (Object[10] default array) = 80 B. HashMap shell: 40 B (object) + 80 B (Node[16] default table) = 120 B. Each HashMap.Node entry: 32 B.*

### 4.3 JPA + Mapping Overhead

When a portfolio is loaded via `JpaPortfolioRepository.getPortfolioById()`, three layers of objects exist simultaneously during a write request:

1. **JPA managed entities** — loaded by Hibernate from the database, tracked in the persistence context.
2. **Domain model** — created by `PortfolioMapper.toModelEntity()` from the JPA entities.
3. **JPA detached entities** — created by `PortfolioMapper.toJpaEntity()` during `savePortfolio()`, used for the merge back.

The mapper extracts primitive values and references (e.g., `entity.getId().value()` returns the String from `PortfolioId`), so many underlying `String` and `BigDecimal` instances are **shared** between the domain model and both JPA entity graphs. The newly allocated objects per layer are primarily the entity shells and their collection wrappers.

**JPA entity shell sizes (new allocations only, Strings/BigDecimals shared):**

| JPA Entity | Shell size | Collection overhead |
|---|---|---|
| `PortfolioJpaEntity` | ~32 B | ~120 B (HashSet internal HashMap) |
| `HoldingJpaEntity` | ~24 B | ~80 B (ArrayList) |
| `LotJpaEntity` | ~32 B | — |

**Hibernate persistence context overhead per managed entity:**

Hibernate maintains per-entity metadata including an `EntityEntry` (status, lock mode, loaded state snapshot for dirty checking), plus an `EntityKey` in the identity map. Conservative estimate: **~400 B per managed entity** (200–300 B for `EntityEntry` + ~64 B for `EntityKey` + snapshot of scalar fields).

### 4.4 Scenario Table — Table B

The following table estimates total request-scoped memory for a buy/sell operation (which loads the full aggregate, creates the domain model, mutates it, and creates a new JPA entity graph for save):

| Scenario | Holdings | Lots / holding | Total lots | Domain graph | JPA mirror (new allocs) | Hibernate PC | **Total / request** | Comfort |
|---|---|---|---|---|---|---|---|---|
| **Tiny portfolio** | 1 | 1 | 1 | ~1.0 KB | ~0.3 KB | ~1.2 KB | **~2.5 KB** | 🟢 Negligible |
| **Average retail** | 5 | 3 | 15 | ~6 KB | ~1.1 KB | ~8.4 KB | **~16 KB** | 🟢 Comfortable |
| **Active trader** | 15 | 10 | 150 | ~47 KB | ~6.5 KB | ~66 KB | **~120 KB** | 🟢 Comfortable |
| **Power user** | 30 | 25 | 750 | ~220 KB | ~27 KB | ~312 KB | **~560 KB** | 🟡 Fine; watch GC |
| **Stress / edge case** | 50 | 50 | 2,500 | ~718 KB | ~85 KB | ~1.0 MB | **~1.8 MB** | 🟡 Investigate lock time, not memory |

**Derivation notes:**
- Domain graph: `Portfolio base (424 B) + H × (Holding base 264 B + HashMap.Node 32 B) + L × Lot (288 B)`
- JPA mirror: `PortfolioJpa shell (152 B) + H × HoldingJpa shell (104 B) + L × LotJpa shell (32 B)`
- Hibernate PC: `(1 + H + L) × 400 B`
- "Lots / holding" is an average; real distributions are uneven (some tickers traded more frequently)

**Key observation:** Even the extreme stress scenario (2,500 lots — far beyond any personal portfolio) uses under 2 MB per request. **Memory is not the constraining factor for HexaStock at any realistic scale.** A JVM with the default 256 MB heap has over 150 MB usable for application data, which can accommodate hundreds of concurrent extreme-case requests.

### 4.5 Concurrency Envelope

Memory per request is small. The real constraints on concurrent throughput are elsewhere:

| Resource | Default | Practical limit | Bottleneck? |
|---|---|---|---|
| **Tomcat thread pool** | 200 threads | 200 concurrent HTTP requests | Rarely — threads waiting on DB connections before threads are exhausted |
| **HikariCP connection pool** | 10 connections | 10 concurrent DB transactions | **Yes** — with 500 ms avg TX duration (cold cache): max ~20 TX/sec. After API-outside-TX refactoring (~50 ms TX): ~200 TX/sec |
| **JVM heap (512 MB)** | ~350 MB usable | ~350 MB / 120 KB per active-trader request ≈ 2,900 concurrent | No — connection pool is exhausted long before heap |
| **Pessimistic lock (per portfolio)** | 1 writer at a time | 1 mutating operation per portfolio simultaneously | **Yes for same-portfolio** — serialises all writes to the same portfolio. With pessimistic lock + API inside TX: one user's buy blocks everyone else's buy on that portfolio for 500 ms+ |
| **External API rate limit** | 60 calls/min (Finnhub free tier) | ~1 call/sec sustained | **Yes for cold-cache** — but Caffeine cache (5-min TTL) absorbs most repeat queries |

**Throughput modelling:**

| Scenario | TX duration | Max TX / sec (10 connections) | Max concurrent same-portfolio writes |
|---|---|---|---|
| **Current** (API inside TX, cold cache) | ~600 ms | ~17 | 1 (serialised by pessimistic lock) |
| **Current** (API inside TX, warm cache) | ~60 ms | ~167 | 1 |
| **Refactored** (API outside TX) | ~50 ms | ~200 | 1 (still pessimistic lock per portfolio, but lock held very briefly) |
| **Refactored + optimistic** | ~50 ms | ~200 | Multiple (concurrent, retry on conflict) |

### 4.6 Where the Real Bottleneck Lives

The analysis is clear: **HexaStock's performance constraints are not in the domain model's memory footprint, but in the transaction orchestration layer:**

1. **Lock duration** — the pessimistic lock on `findByIdForUpdate()` is held while the external API is called. This serialises same-portfolio access for hundreds of milliseconds to seconds. **Fix: move API call outside the transaction (Section 2.3).**

2. **Lock scope** — read-only operations (`getPortfolio`, `getHoldingsPerformance`) acquire a write lock via the same `findByIdForUpdate()` path. They do not need any lock at all. **Fix: add a non-locking read path (Section 3.2.1).**

3. **Connection pool utilisation** — with 10 default connections and long-held transactions, the pool saturates at modest concurrency. **Fix: shorten transactions (items 1 and 2 above) and increase pool size if needed (`spring.datasource.hikari.maximum-pool-size`).**

4. **External API rate limit** — the 500 ms throttle exists because the free-tier API requires it. The Caffeine cache mitigates most repeated calls. The remaining exposure is on cold caches (first call for a ticker within 5 minutes). **This is an infrastructure constraint, not an application defect.**

The domain model — `Portfolio`, `Holding`, `Lot` with their value objects — contributes negligible overhead. Converting it to a thinner model (e.g., flat DTOs, IDs without wrappers) would save kilobytes while the transaction orchestration issues cost seconds.

---

## 5. From Reference Architecture to Production-Ready Pragmatism

HexaStock is explicitly a pedagogical project. Several design choices serve a teaching purpose — they demonstrate a concept clearly even if a production system might choose differently. This section acknowledges those choices honestly and maps the path from "reference architecture" to "production-ready" for each.

### Table A — Pedagogical Choices vs Production Alternatives

| Design choice | Pedagogical rationale | Production alternative | When to switch | What to preserve |
|---|---|---|---|---|
| **500 ms API throttle** (`Thread.sleep` in adapter) | Demonstrates rate-limit awareness for free-tier APIs. Visible in code as explicit constraint. Makes the external dependency cost tangible for learners. | Non-blocking rate limiter (e.g., Resilience4j `RateLimiter`, token-bucket). Paid API tier with higher limits. Background price ingestion decoupled from user requests. | When the project moves to a paid API tier, or when the throttle measurably impacts user-perceived latency (it currently does for cold-cache multi-ticker scenarios). | The *concept* of rate limiting at the adapter boundary. The adapter's `@Cacheable` strategy. |
| **Pessimistic locking on all reads and writes** | Simplicity: no retry logic, no version tracking, no silent data loss from unhandled conflicts. Easy for students to trace "lock acquired → work → lock released". | Optimistic locking (`@Version`) for writes; no lock at all for reads. Retry logic for `OptimisticLockException`. | When contention is measured (not assumed) and retries are acceptable. After API calls are moved outside the TX (Section 2.3), so lock duration is already short. | The principle of explicit concurrency control. The domain model's invariant protection. |
| **Full aggregate load for every operation** | Demonstrates the DDD aggregate pattern: load entire consistency boundary, mutate, save. Clear and correct. Students see the full lifecycle. | Partial loads for operations that touch only `balance`. DTO projections for read-only queries. Targeted UPDATEs for scalar changes. | When profiling shows aggregate hydration as a measurable cost (Section 4 shows this is unlikely before 750+ lots). For reads, the improvement is immediate and low-risk (projections). | Aggregate-based writes for operations that check cross-entity invariants (`buy`, `sell`). |
| **Full aggregate re-creation on save** (`toJpaEntity()` creates new detached graph) | Clean separation: domain model has zero JPA annotations. Mapper is stateless. Easy to understand the mapping lifecycle. | Keep loaded JPA entity managed; update in place. Let Hibernate dirty-checking handle UPDATEs. Or use targeted JPQL for scalar changes. | When the merge overhead becomes measurable (unlikely for portfolios <1,000 lots) or when `@Version` optimistic locking makes version-carrying necessary. | The principle that domain objects do not depend on JPA. |
| **Sequential price fetching for reporting** | Documented in `ReportingService` Javadoc: *"intentional — free-tier API enforces strict rate limits"*. Demonstrates explicit trade-off reasoning. | Parallel fetching with bounded executor + rate limiter. Batch API endpoint if provider supports it. Wider cache window for reporting. | When the holdings endpoint has >5 tickers and cache-cold response time exceeds user tolerance (~2 s). | The port/adapter separation that allows swapping the fetching strategy without touching domain logic. |

### What This Table Shows

The current codebase is not making these choices out of ignorance — the Javadoc and code comments demonstrate awareness of the trade-offs. The codebase is a **reference implementation** that prioritises clarity and correctness over raw throughput. The path to production readiness is incremental and does not require a rewrite:

1. **First:** structural fixes that are correct at every scale (API outside TX, non-locking reads, pagination) — these should be applied regardless of whether the project remains pedagogical.
2. **Next:** production refinements that trade simplicity for throughput (optimistic locking, managed entities, parallel fetching) — apply when scale data justifies the added complexity.
3. **Preserve always:** the principles each pedagogical choice illustrates (rate-limit awareness, explicit concurrency control, aggregate invariant protection, clean persistence separation).

---

## 6. Concrete Strategy by Use Case — Table C

| Use case | Rich aggregate? | Current pattern | Near-term recommendation | Longer-term evolution | Notes |
|---|---|---|---|---|---|
| **Create portfolio** | ✅ Yes (trivial) | `Portfolio.create()` → JPA save | No change needed | No change needed | INSERT only, no aggregate load |
| **Get portfolio summary** | ❌ No | Full aggregate load + pessimistic lock | DTO projection, no lock | Stay as DTO projection | Needs only 4 scalar fields |
| **List portfolios** | ❌ No | Loads *all* aggregates | DTO projection | Paginate if portfolio count grows | Currently loads all holdings and lots to return 4 fields per portfolio |
| **Deposit cash** | ✅ Yes | Full aggregate load + pessimistic lock + full save | Same aggregate, API already not involved | Optimistic lock + targeted UPDATE (balance only) | Only `balance` changes — full graph merge is disproportionate |
| **Withdraw cash** | ✅ Yes | Full aggregate load + pessimistic lock + full save | Same aggregate | Optimistic lock + targeted UPDATE | Same as deposit |
| **Buy stock** | ✅ Yes | Full aggregate + API **inside** TX + pessimistic lock | Fetch price outside TX (Section 2.3) | Optimistic lock + retry | Most impactful single refactoring |
| **Sell stock** | ✅ Yes | Full aggregate + API **inside** TX + pessimistic lock | Fetch price outside TX | Optimistic lock + retry | FIFO requires holdings/lots loaded |
| **Get transaction history** | ❌ No | Full list, no pagination | Add `Pageable` | DTO projection, skip domain mapping | Potentially unbounded |
| **Get holdings performance** | ❌ No | Full aggregate + all TXs + N sequential API calls inside locked TX | SQL aggregation + batch price fetch outside TX | Projection table if >5,000 TXs | Most expensive endpoint |
| **Dashboard (future)** | ❌ No | Does not exist yet | Design as pure read-model from start | DTO projections, optional pre-computed summary | Avoid aggregate path entirely |

---

## 7. Concrete Concurrency Strategy

### Current vs Recommended

| Operation | Current style | Recommended near-term | Why |
|---|---|---|---|
| `getPortfolio()` | `PESSIMISTIC_WRITE` via `findByIdForUpdate()` | **No lock** — simple `findById()` or DTO projection | Read-only. Should never block writes. |
| `getAllPortfolios()` | No lock (uses `findAll()`) but loads full aggregates | **No lock** — DTO projection | Already correct (no lock), but should not load aggregates. |
| `deposit()` | `PESSIMISTIC_WRITE` | **Keep pessimistic for now**; evolve to optimistic when `@Version` is added | Acceptable at current scale. Pessimistic is simpler and conflict-free for single-user. |
| `withdraw()` | `PESSIMISTIC_WRITE` | Same as deposit | Same rationale. |
| `buyStock()` | `PESSIMISTIC_WRITE` + external API **inside** TX | **Price outside TX** + keep pessimistic lock (short TX) | Lock duration is the primary issue; fixing that is more impactful than changing lock type. |
| `sellStock()` | `PESSIMISTIC_WRITE` + external API **inside** TX | Same refactoring as buy | Same rationale. |
| `getTransactions()` | `@Transactional` read-only, no explicit lock | **No change needed** (but add pagination) | Already reasonable. |
| `getHoldingsPerformance()` | `PESSIMISTIC_WRITE` + N sequential API calls **inside** TX | **No aggregate load, no lock, no TX** — SQL + batch price fetch | Complete restructuring of the read path. |

### Concurrency Questions Answered

**Which operations should remain serialised?**
None need to be strictly serialised at the personal portfolio scale. With pessimistic locking and short transactions (after API-outside-TX refactoring), serialisation per portfolio is brief (~50–200 ms) and causes no practical contention for a single user.

**Which reads should never acquire write locks?**
All of them: `getPortfolio()`, `getAllPortfolios()`, `getTransactions()`, `getHoldingsPerformance()`, any future dashboard queries.

**Where is current transaction scope too wide?**
- `buyStock()` and `sellStock()` — external API call inside the TX.
- `getHoldingsPerformance()` — holds a TX with pessimistic lock while making N sequential API calls (on cache miss).

**Where should retries be used?**
If/when optimistic locking is adopted: `deposit()`, `withdraw()`, `buyStock()`, `sellStock()`. A simple retry wrapper (max 3 attempts with exponential backoff) handles `OptimisticLockException`. Spring Retry (`@Retryable`) is suitable.

**Where should idempotency be considered?**
- `deposit()` and `withdraw()` — if the client retries a timeout, a duplicate deposit could occur. Adding an idempotency key (client-provided or request-scoped UUID) prevents double-processing.
- `buyStock()` — same concern. An idempotency key on the transaction record (stored as a unique constraint) protects against duplicates.
- This is a **medium-term** concern, not urgent for personal use.

**Is the current design horizontally scalable enough for personal scope?**
Yes — with one caveat. Multiple app instances can run simultaneously because all state is in the database. However:
- The Caffeine cache is local per JVM. Two instances may have different cached prices. This is acceptable for a personal app; at higher scale, a shared cache (Redis) would be needed.
- Pessimistic locks are database-level, so they correctly serialise across instances. Optimistic locking (`@Version`) is also database-level and works across instances.

---

## 8. Concrete JPA/Hibernate Strategy

### 8.1 `@Version` — Currently Missing

✅ **Verified:** No `@Version` field on `PortfolioJpaEntity`, `HoldingJpaEntity`, or `LotJpaEntity`.

**Recommendation:** Add `@Version private Long version;` to `PortfolioJpaEntity` as a medium-term improvement. Since the aggregate root is the consistency boundary, versioning only the root entity is sufficient — child entity changes are always saved through the root.

**Complication with current save pattern:** `PortfolioMapper.toJpaEntity()` creates a **new** `PortfolioJpaEntity` instance. This detached instance will have `version = null`. When `save()` is called, Hibernate's `SimpleJpaRepository.save()` checks `entityInformation.isNew(entity)` — since `id` is set but `version` is null, the behaviour depends on ID strategy. With a String `@Id` (not generated), Hibernate cannot distinguish "new" from "existing" when version is null.

**Solution options:**
- **(a)** Add `version` to the `Portfolio` domain model as an opaque persistence-aware field (pragmatic but impure).
- **(b)** Store the loaded JPA entity in a request-scoped context within `JpaPortfolioRepository`, and update it in-place during `savePortfolio()` instead of creating new instances.
- **(c)** Convert `PortfolioJpaEntity` to implement `Persistable<String>` with a transient `isNew` flag, and carry `version` through the mapper.

Option **(b)** is the cleanest: the loaded entity stays managed in the persistence context, the mapper applies domain changes to it, and Hibernate's dirty checking handles the UPDATE with version check automatically.

### 8.2 Detached Graph Merge — Current Pattern

✅ **Verified:** `JpaPortfolioRepository.savePortfolio()` → `PortfolioMapper.toJpaEntity(portfolio)` → `jpaSpringDataRepository.save(jpaEntity)`.

`toJpaEntity()` creates entirely new instances:
```java
PortfolioJpaEntity portfolioJpaEntity = new PortfolioJpaEntity(
        entity.getId().value(), ...);
portfolioJpaEntity.setHoldings(entity.getHoldings().stream()
        .map(HoldingMapper::toJpaEntity).collect(Collectors.toSet())); // new set of new entities
```

**How this works:** When `save()` is called with this detached graph, Spring Data's `SimpleJpaRepository.save()` sees a non-null ID and calls `entityManager.merge()`. Hibernate then:
1. Loads the existing entity from the persistence context (or issues a SELECT)
2. Copies all field values from detached to managed entity
3. Diffs the holdings collection to detect additions, removals, and modifications
4. For each holding, diffs its lots collection
5. Issues INSERT/UPDATE/DELETE as needed

**Assessment for current scale:** For a personal portfolio (5–15 holdings, <200 lots), this works correctly and the overhead is dominated by DB I/O, not by in-memory diffing. The pattern is pedagogically clean: mapper is stateless, domain model has no JPA dependency.

**Assessment for future scale:** For portfolios with 500+ lots, the full-graph merge involves hundreds of comparisons and potentially hundreds of SELECT statements (if entities are not in the persistence context). At that point, consider switching to managed-entity updates.

### 8.3 Batch Fetching

**Recommended now:** Add `@BatchSize(size = 30)` annotations:

- `PortfolioJpaEntity.holdings`
- `HoldingJpaEntity.lots`

Or globally in `application.properties`:
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=30
```

### 8.4 Entity Graph Opportunities

For the write path (buy/sell), where the full aggregate is needed, a `@NamedEntityGraph` or a `JOIN FETCH` query could load the entire graph in 1 query instead of `2 + ceil(H/30)`:

```java
@Query("SELECT DISTINCT p FROM PortfolioJpaEntity p " +
       "LEFT JOIN FETCH p.holdings h " +
       "LEFT JOIN FETCH h.lots " +
       "WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdWithGraph(@Param("id") String id);
```

⚠️ **Caveat:** With `Set<HoldingJpaEntity>` and `List<LotJpaEntity>`, a double `JOIN FETCH` creates a Cartesian product. Hibernate handles this by de-duplicating, but the wire-level result set can be large. For portfolios under 100 holdings, this is fine. For larger portfolios, `@BatchSize` may be more efficient. Test with realistic data before choosing.

### 8.5 Orphan Removal

✅ **Verified:** Both `@OneToMany` relationships use `orphanRemoval = true`. This is correct: when `Holding.sell()` removes a fully-depleted lot via `iterator.remove()`, the corresponding `LotJpaEntity` should be deleted.

**Risk with detached merge:** When `toJpaEntity()` creates a new Set of holdings and a new List of lots, Hibernate must compare old vs new collections to detect orphans. If entity IDs match correctly, this works. If there is any ID mismatch (e.g., due to `equals()/hashCode()` issues on JPA entities), orphan detection may fail silently.

**Mitigation:** Ensure `HoldingJpaEntity` and `LotJpaEntity` have correct `equals()` and `hashCode()` based on their `@Id` fields. Currently they use the default `Object` identity, which works when entities are managed in the persistence context but may cause issues with detached merges. **Consider adding `equals()/hashCode()` based on `id` to the JPA entities.**

### 8.6 Current Mapping Alignment

The current mapping pattern (domain ↔ JPA via static mapper classes) is a **sensible DDD choice** that keeps the domain model free from JPA annotations. The cost is the full-graph re-creation on save. This is the classic DDD persistence trade-off.

**Assessment:** For HexaStock's current scale, this trade-off is acceptable. The mapping cost is dominated by DB query time and network I/O, not by in-memory object creation. However, if performance profiling later shows the mapping round-trip as a measurable cost (unlikely below 1,000 lots), consider the managed-entity approach described in 8.2.

---

## 9. Recommended Target Architecture for HexaStock

### Stage A — Good Enough for Personal Portfolios

**Minimal changes, maximum practical value.**

**Architectural shape:**
- Domain model: unchanged
- Write path: aggregate-based, short transactions (API call outside TX)
- Read path: DTO projections for list/detail, paginated transaction history
- Locking: pessimistic on writes (existing, now with short TX), no lock on reads
- Persistence: `@BatchSize` added, same mapper pattern

**Changes from current state:**
1. Non-locking read path for `getPortfolio()` and `getAllPortfolios()`
2. Move `fetchStockPrice()` outside the `@Transactional` block in `buyStock()` and `sellStock()`
3. Add pagination to `getTransactions()`
4. Add `@BatchSize(size = 30)` to JPA entity collections
5. Portfolio summary DTO projection for list/detail endpoints

**Expected benefits:**
- Read operations no longer block writes
- Buy/sell lock duration drops from hundreds of milliseconds / seconds (cache dependent) to <200 ms
- Transaction history response time bounded regardless of data volume
- Portfolio list endpoint drops from multi-join to single-table query

**What remains DDD-rich:** Everything. Write commands use the aggregate as before.

**Signals it is time for Stage B:**
- Portfolio size exceeds 30 holdings / 500 lots and write latency becomes noticeable
- Holdings performance endpoint routinely exceeds 3 seconds
- Multiple users accessing the same portfolio simultaneously (even if rare)

---

### Stage B — Stronger for Power Users

**More investment, same overall architecture.**

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
- Optimistic locking allows concurrent reads and writes without serialisation
- Holdings performance drops from H×500 ms (worst case) to ~500 ms
- Write operations that only touch balance avoid full-graph merge
- Clear architectural separation of read and write concerns

**What remains DDD-rich:** All write commands, domain logic, invariant enforcement, FIFO sell algorithm.

**Signals it is time for Stage C:**
- Portfolio size routinely exceeds 100 holdings / 5,000 lots
- Transaction count per portfolio exceeds 50,000
- Multiple concurrent users on the same portfolio with measurable conflict rate
- Buy/sell p95 latency exceeds 500 ms after all Stage B optimisations

---

### Stage C — Prepared for Enterprise-Like Pressure

**Only if real usage proves necessary.**

**Architectural shape:**
- Domain model: potentially split aggregate (Holding as separate root)
- Write path: saga or two-phase coordination for buy/sell across Portfolio balance + Holding lots
- Read path: Materialised projection tables updated by domain events. Pre-computed holdings summary.
- Persistence: Event-sourced or ledger-based transaction model. Lot compaction. Table partitioning.
- Infrastructure: Shared cache (Redis), rate-limited external API client with circuit breaker, horizontal scaling.

The changes are significant (split aggregate, domain events, outbox, materialised views, shared cache, circuit breaker). This level of investment is justified only when monitoring data shows the system is measurably failing under real usage — not by anticipation.

---

## 10. Prioritized Action Plan

| Priority | # | Change | Why now | Effort | Expected benefit | Risk |
|---|---|---|---|---|---|---|
| **Quick win** | 1 | Move `fetchStockPrice()` outside TX in `buyStock()`/`sellStock()` | Lock held during HTTP I/O is the primary latency driver for cold-cache scenarios | Low (method split) | Lock duration: cold-cache seconds → ~50–200 ms | Very low — price staleness is acceptable |
| **Quick win** | 2 | Add `@BatchSize(size = 30)` to `PortfolioJpaEntity.holdings` and `HoldingJpaEntity.lots` | Reduces N+1 queries immediately | Very low (2 annotations) | Query count: 2+H → ~3–4 | None |
| **Quick win** | 3 | Add non-locking read path for `getPortfolio()` | Reads currently block writes with `FOR UPDATE` | Low (add `findById()` call) | Reads no longer block writes | None |
| **Quick win** | 4 | Add pagination to `getTransactions()` | Unbounded list grows with portfolio lifetime | Low (Pageable parameter) | Constant-time transaction history | None |
| **Near-term** | 5 | DTO projection for `getAllPortfolios()` | Currently loads all aggregates to return 4 fields | Low–Medium | List endpoint: O(P×(H+L)) → O(P) | Low |
| **Near-term** | 6 | DTO projection for `getPortfolio()` | Currently loads full aggregate with lock | Low–Medium | Detail endpoint: multi-join → single table | Low |
| **Near-term** | 7 | Restructure `getHoldingsPerformance()` — SQL aggregation + batch price fetch, no aggregate load | The most expensive endpoint, especially on cold cache | Medium | Response time: worst-case H×500 ms → ~500 ms. No lock. | Medium — requires new SQL query + testing |
| **Medium-term** | 8 | Add `@Version` to `PortfolioJpaEntity` + optimistic locking | Enables concurrent writes without serialisation | Medium | Better concurrency, no lock waits | Medium — version must flow through mapper |
| **Medium-term** | 9 | Formal `PortfolioQueryService` + `PortfolioQueryPort` | Formalises read/write separation | Medium | Cleaner architecture, easier to extend | Low |
| **Medium-term** | 10 | Smarter save path (managed entities or targeted UPDATE) | Avoids full graph re-merge on every save | Medium–High | Reduced DB overhead for cash operations | Medium — changes persistence adapter significantly |
| **Medium-term** | 11 | Parallel stock price fetching in adapter | Sequential fetching with throttle is slow on cache miss | Low–Medium | Better response time for multi-ticker operations | Low |
| **Optional long-term** | 12 | Domain events + outbox for read-model updates | Decouples write-side from read-side projections | High | Reliable projection updates across restarts | Only if projection tables are adopted |
| **Optional long-term** | 13 | Dedicated holdings summary projection table | Pre-computed read model for performance dashboard | Medium–High | Sub-second holdings dashboard regardless of TX count | Eventual consistency complexity |
| **Future only** | 14 | Split Holding into separate aggregate | Avoid full graph load for large portfolios | High | Per-holding operations scale independently | Loses atomic buy check |
| **Future only** | 15 | Event sourcing for transactions | Full temporal query support, replay | Very High | Audit-grade history, temporal queries | Complete rewrite of write model |

---

## 11. Final Verdict

### 1. Can HexaStock remain a rich DDD model and still be pragmatic?

**Yes.** The domain model is the strongest part of HexaStock. `Portfolio`, `Holding`, `Lot`, the FIFO sell algorithm, the value objects, the sealed `Transaction` hierarchy — these are well-designed and provide real structural and pedagogical value. None of the recommended improvements require changing the domain model. Every optimisation lands in the persistence adapter, the application services, or the addition of complementary read paths.

The premise "either rich model or good performance" does not apply here. The model is rich for write commands. Reads get lightweight dedicated paths. Both coexist without conflict.

### 2. What to change immediately (3 refinements)

These are structurally correct at every scale, low-risk, and high-value:

1. **Move `fetchStockPrice()` outside the `@Transactional` block** in `buyStock()` and `sellStock()`. The external API call (with its free-tier throttle) does not need to hold a database lock. This change is correct regardless of locking strategy.
2. **Add a non-locking `findById()` path** for read-only operations. Read-only queries (`getPortfolio()`, `getAllPortfolios()`, `getHoldingsPerformance()`) should never acquire a `PESSIMISTIC_WRITE` lock.
3. **Paginate transaction history.** Unbounded `List<Transaction>` grows with the portfolio's lifetime.

A close fourth: **`@BatchSize(size = 30)`** on both JPA collections — two annotations, immediate N+1 reduction.

### 3. What to evolve at medium term (3 refinements)

Apply these when scale data or user feedback justifies the added complexity:

1. **`@Version` optimistic locking** — replace pessimistic locking on write paths. The current pessimistic approach is not wrong, but optimistic locking enables better concurrency when lock duration is already short (after item 2.1 above).
2. **CQRS-lite query services** — formal separation of read and write paths. Dedicated `PortfolioQueryService` with SQL projections replaces aggregate loading for all read endpoints.
3. **SQL-based holdings performance** — replace in-memory `HoldingPerformanceCalculator` on the read path with database aggregation + batch price fetching outside any transaction.

### 4. What to not change yet

These are working correctly and adding complexity now would be premature:

- **Do not split the aggregate.** The memory analysis (Section 4) confirms that even a 750-lot portfolio uses ~560 KB. The aggregate boundary is correct for the invariants it protects. Splitting adds saga complexity for no measurable gain.
- **Do not add event sourcing.** The `portfolio_transaction` table already provides transaction history. Full event sourcing adds significant infrastructure for capabilities HexaStock does not need.
- **Do not add shared cache infrastructure (Redis).** Caffeine is appropriate for single-instance deployment. Redis is justified only for multi-instance horizontal scaling.
- **Do not refactor the mapper/save pattern yet.** The detached-graph merge works correctly. Its cost is dwarfed by the transaction orchestration issues (API inside TX, unnecessary locking). Fix the bigger problems first; the mapper pattern may never need changing at realistic scale.

### 5. How to distinguish healthy evolution from overengineering

- **Healthy** = each change is motivated by a measured problem, affects only the layer where the problem lives (typically persistence or orchestration), and preserves the domain model's integrity.
- **Overengineering** = copying patterns from architecture articles (CQRS, event sourcing, saga, microservices) without evidence that the current architecture is failing. Building infrastructure for scale that may never arrive.

The test: **"Can you point to a specific endpoint, operation, or user scenario where the current architecture measurably underperforms?"** If yes, the change is justified. If no, it is premature.

---

## Recommended Default Posture

- **Now:** Fix the three obvious structural wins (API outside TX, non-locking reads, pagination). Add `@BatchSize`. Keep the rich domain model untouched. Ship and observe.
- **Next:** When portfolio sizes grow or response time monitoring shows degradation, introduce CQRS-lite query services, optimistic locking, and SQL-based reporting. The domain model still does not change.
- **Later:** Only when real production evidence demands it (consistent p95 latency violations, measured lock contention, heap pressure under load), consider aggregate boundary splits, projection tables, or event infrastructure. Challenge any proposal that cannot point to specific metrics.

---

*This review is based solely on source code present in the HexaStock repository as of 15 April 2026. No production metrics, load test results, or runtime profiling data were available. All performance estimates are analytically derived from the JVM memory model and stated assumptions. Memory footprint figures are estimates accurate to ±20% — exact values depend on JDK vendor, GC implementation, and runtime conditions. Every recommendation references actual class names, method signatures, and code paths in the repository, and every deferred recommendation states the specific evidence that would justify it.*
