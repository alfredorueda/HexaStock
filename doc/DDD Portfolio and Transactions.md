# Portfolio and Transaction Aggregates in DDD
## A Production-Grade Architecture Guide

---

## 1. Introduction

This document formalizes the design decision for structuring the **Portfolio** domain and its related concepts (**Holdings**, **Lots**, and **Transactions**) in alignment with **Domain-Driven Design (DDD)** principles, as introduced by **Eric Evans** in *Domain-Driven Design: Tackling Complexity in the Heart of Software* (2003) and further developed by **Vaughn Vernon** (*Implementing Domain-Driven Design*, 2013) and **Jimmy Nilsson** (*Applying Domain-Driven Design and Patterns*, 2006).

This guide aims to illustrate how those principles can be applied to a concrete portfolio domain implemented using a hexagonal architecture, where persistence technologies can be swapped through adapters.

In doing so, it also discusses a number of practical considerations that often arise in real systems, including:

- **Aggregate size** and memory usage
- **Dirty checking** and write amplification in Hibernate
- **Optimistic locking contention** under concurrent operations
- **Transaction history queries** — filtering, pagination, sorting
- **Indexing** strategies for relational databases
- **Serialization overhead** in caches, APIs, and event systems

The goal is to maintain a **clean, invariant-focused domain model** while ensuring **high performance and practical feasibility** in a production system.

---

## 2. Domain Context

The system manages investment portfolios. Each portfolio tracks:

- A **cash balance** — funds available for trading
- **Holdings** — current positions in specific stock tickers
- **Lots** — individual purchase batches within a holding (used for FIFO cost basis calculations)
- **Transactions** — an append-only history of all operations: deposits, withdrawals, purchases, and sales

Operations such as `deposit`, `withdraw`, `buy`, and `sell` mutate the portfolio state (cash balance, holdings, lots) and then record a Transaction **after successful completion** of the operation.

---

## 3. Current Model

Following Evans' definition of **aggregate roots** and Vernon's recommendations for **transactional consistency boundaries**, the domain model is structured as follows:

### Portfolio as Aggregate Root

- **Role**: Central point for enforcing business invariants related to investment portfolios.
- **Contains**:
  - **Holdings** — represent positions in specific assets, keyed by `TickerSymbol`.
  - **Lots** — represent individual batches of an asset acquired at a certain time and price, owned by a Holding.
- **Reasoning**:
  - Holdings and Lots are part of the same **consistency boundary** and must be updated atomically to preserve business rules (e.g., cash balance can never go negative, sell quantity cannot exceed held shares).
  - Their size remains **bounded** in practice — a portfolio typically holds tens to low hundreds of distinct tickers, each with a manageable number of lots.

### Transactions as a Separate Aggregate

Aligned with Vernon's principle that aggregates should remain **small and focused on invariants**:

- **Role**: Immutable record of all operations that have affected the Portfolio (deposits, withdrawals, purchases, sales).
- **Reasoning**:
  - Transaction history grows **without bound** and is not needed for enforcing Portfolio invariants.
  - Placing Transactions in a separate aggregate prevents large data loads, reduces memory pressure, and allows independent optimization for querying and reporting.
  - This separation supports Nilsson's emphasis on **decoupling persistence concerns from the domain model**.

Transactions are linked to the Portfolio via an **identifier** (`portfolioId`) rather than a direct object reference, preserving loose coupling and preventing accidental eager loading. This aligns with Evans' recommendation that aggregates should be designed to be loaded and modified atomically without excessive data.

---

## 4. The Core DDD Question: Should Transaction Be Inside the Portfolio Aggregate?

This is the central design question. The naive approach — modeling Transaction as a child entity inside the Portfolio aggregate — feels natural at first. After all, every transaction "belongs" to a portfolio.

But DDD aggregate design is not about ownership. It is about **consistency boundaries** and **invariants**.

The question to ask is:

> *Does the Portfolio aggregate need to read, check, or enforce any invariant that requires the full Transaction list to be loaded in memory?*

The answer, for virtually all portfolio systems, is **no**.

---

## 5. Aggregate Invariants and Consistency Boundaries

Let's examine every invariant the Portfolio aggregate enforces:

| Invariant | Requires Transaction in Aggregate? |
|---|---|
| `cashBalance` is never negative | **No** — enforced by checking `cashBalance` before mutating it |
| Holdings are keyed by TickerSymbol (one per ticker) | **No** — enforced by the `Map<TickerSymbol, Holding>` structure |
| Buy/sell/withdraw rejected if preconditions fail | **No** — preconditions check `cashBalance` and `Holding.totalShares()` |
| Transactions are append-only and immutable | **No** — append-only semantics don't require co-location; an INSERT into a separate table is inherently append-only |

**Zero invariants require Transaction to be loaded or checked during buy/sell/deposit/withdraw.** The aggregate never reads its own transaction list to make a decision. It only appends to it.

### What *must* be strongly consistent?

In the same database transaction:

1. `cashBalance` mutation
2. `Holding` / `Lot` mutations
3. The creation of the `Transaction` record (to avoid ghost entries or lost records)

This consistency requirement can be satisfied with a simple DB transaction spanning two tables — it does **not** require the Transaction to be part of the aggregate's in-memory object graph.

---

## 6. Why "Append-Only History" Does Not Require an Aggregate Relationship

The append-only constraint is a **write pattern**, not a consistency invariant. It means:

- No transaction is ever updated or deleted after creation.
- Transactions are created as a **side-effect** of a successful portfolio operation.

This is the textbook signature of a **domain event** or a **separate ledger**, not an aggregate-internal entity.

A transaction is always created *after* the portfolio state change succeeds. The model itself reveals this:

> "Created only AFTER successful completion of an operation."

This temporal dependency — "record what happened" — is fundamentally different from "enforce what is allowed." Keeping transactions inside the aggregate inflates the consistency boundary **without any correctness benefit**.

---

## 7. The Hidden Problem: Unbounded Collections in Aggregates

A portfolio that trades daily for 10 years accumulates ~2,500+ transactions. Active or algorithmic portfolios can easily reach **100,000 to 1,000,000+** entries.

Including an unbounded collection inside an aggregate creates a **ticking time bomb** that detonates silently as data grows:

| Problem | Impact at 100k+ Transactions |
|---|---|
| **Memory consumption** | Each Transaction entity + Hibernate proxy ≈ 500–800 bytes → 100k entries ≈ **50–80 MB per aggregate load** |
| **Dirty checking** | Hibernate dirty-checks the entire collection on flush → **O(n) comparison every flush** |
| **GC pressure** | Loading and discarding large collections causes major GC pauses |
| **Optimistic locking contention** | Every concurrent operation that appends a transaction bumps the `@Version` → two simultaneous deposits will conflict even though they're logically independent |
| **Serialization** | If Portfolio is serialized (cache, messaging, logging), the entire history travels with it |

### Small-scale caveat

For a portfolio with < 100 transactions, none of this matters. But the design becomes a **structural bottleneck** that is expensive to refactor once data has grown.

---

## 8. Relational Database Reality

The relational database doesn't care about DDD aggregates. It cares about:

- **Row counts** in JOINs
- **Index selectivity** for WHERE clauses
- **Lock granularity** for concurrent writes
- **I/O pages** read from disk

When Transaction lives inside the aggregate, every Portfolio load triggers either:

- **Eager fetch**: a JOIN that returns all transaction rows (catastrophic for large histories)
- **Lazy fetch**: a deferred proxy that Hibernate initializes fully the moment you call `.add()`, `.size()`, or iterate

In a relational DB, the natural query pattern for transaction history is:

```sql
SELECT * FROM portfolio_transaction
WHERE portfolio_id = ?
  AND type = ?
  AND occurred_at BETWEEN ? AND ?
ORDER BY occurred_at DESC
LIMIT 20 OFFSET 0;
```

This query is trivial to optimize with proper indexes. But it is **impossible to express efficiently** through an aggregate's in-memory `List<Transaction>` without loading everything first.

---

## 9. JPA / Hibernate Pitfalls with Large One-To-Many Collections

This section covers the specific Hibernate behaviors that make unbounded `@OneToMany` collections dangerous.

### Eager Loading — The Obvious Problem

```java
// ❌ Loads ALL transactions every time you load Portfolio
@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
private List<Transaction> transactions;
// 100k rows in memory just to sell 10 shares
```

### Lazy Loading — The Subtle Problem

```java
// ⚠️ Deferred, but...
@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<Transaction> transactions;
```

With Hibernate's default `PersistentBag` (unordered `List`), **any `add()` triggers a full collection initialization** because Hibernate must load the collection to manage its internal state. This is the single most common JPA performance trap.

Even with `@OrderColumn` + `PersistentList` or `Set` semantics, the fundamental problems remain:

- **Dirty checking** iterates the entire collection on every `flush()`
- **`CascadeType.ALL`** means `Portfolio.save()` touches all Transaction rows
- **No way to paginate** through a JPA collection — it's all-or-nothing

### The Anti-Pattern

```java
// ❌ ANTI-PATTERN: Transaction as @OneToMany inside Portfolio
@Entity
public class PortfolioJpaEntity {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private List<TransactionJpaEntity> transactions;
    // Problems:
    // 1. LAZY doesn't help — Hibernate loads full collection on .add() with Bag semantics
    // 2. Dirty checking iterates entire collection
    // 3. CascadeType.ALL means Portfolio.save() touches all Transaction rows
    // 4. No way to paginate through JPA collection
}
```

---

## 10. Transaction History API Requirements

A production portfolio system invariably needs a transaction history endpoint:

```
GET /portfolios/{id}/transactions?type=SELL&ticker=AAPL&from=2025-01-01&page=2&size=20
```

This requires **filtering, sorting, and pagination** — operations that are trivial in SQL but impossible to express efficiently through an aggregate's in-memory list without loading everything first.

Typical API requirements include:

- Filter by **transaction type** (DEPOSIT, WITHDRAW, BUY, SELL)
- Filter by **ticker symbol**
- Filter by **date range**
- **Sort** by date (ascending or descending)
- **Paginate** results (cursor-based or offset-based)
- Return **total count** for UI pagination controls

All of these requirements point toward a **direct database query**, not an aggregate traversal.

---

## 11. Architecture Alternatives

### Option A — Transaction Inside Portfolio Aggregate

**How it works**: Transaction is a child entity of Portfolio. All CRUD goes through the aggregate root. Hibernate manages the `@OneToMany` collection.

**Pros:**
- Simple mental model — everything in one place
- Strong consistency trivially guaranteed (single aggregate = single DB transaction)
- Good for prototyping and small datasets

**Cons:**
- All performance risks from Sections 7–9 apply (memory, dirty checking, GC, locking contention)
- Cannot paginate/filter transaction history without loading everything
- Aggregate becomes a god object over time
- Violates Vernon's guideline of keeping aggregates small

**DB mapping:**

```java
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {
    @Id
    private String id;

    @Version
    private Long version;

    private BigDecimal cashBalance;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    @OrderColumn(name = "tx_order") // needed to avoid full reload on add
    private List<TransactionJpaEntity> transactions = new ArrayList<>();
    // Even with LAZY + @OrderColumn, Hibernate initializes collection
    // on size()/add() in many scenarios
}
```

**Query patterns:** Forced to load aggregate → filter in memory, or break encapsulation with direct JPQL bypassing the aggregate. Neither is clean.

**Verdict:** Acceptable only for prototypes or aggregates guaranteed to have < ~50 transactions.

---

### Option B — Separate Transaction Aggregate (Transaction Ledger)

**How it works**: Transaction becomes a standalone entity/aggregate with a `portfolioId` foreign key. Portfolio no longer holds a `List<Transaction>`. An application service coordinates: mutate Portfolio, then create Transaction, in the **same DB transaction**.

**Pros:**
- Portfolio aggregate stays small and fast (only cashBalance + holdings + lots)
- Transaction history is queried independently with full SQL power
- No collection loading, no dirty-checking overhead
- Natural pagination, filtering, indexing
- Eliminates artificial optimistic locking contention
- **Most common production pattern in financial systems**

**Cons:**
- Slightly more complex application service (two repository calls in one transaction)
- Transaction creation is no longer "enforced" by the aggregate root — application service must not forget
- Requires discipline in code reviews to ensure transactions are always created

**Domain model change — Portfolio loses the transactions list:**

```java
public class Portfolio {
    private PortfolioId id;
    private OwnerName ownerName;
    private Money cashBalance;
    private Instant createdAt;
    private Map<TickerSymbol, Holding> holdings;
    // NO transactions field

    public SellResult sell(TickerSymbol ticker, ShareQuantity qty, Price price, Instant occurredAt) {
        Holding holding = findHoldingOrThrow(ticker);
        if (holding.totalShares().compareTo(qty) < 0) {
            throw new InsufficientSharesException(ticker, qty, holding.totalShares());
        }

        CostBasisBreakdown breakdown = holding.sellSharesFIFO(qty);
        Money saleProceeds = price.times(qty);
        Money realizedPnL = saleProceeds.minus(breakdown.totalCostBasis());
        this.cashBalance = this.cashBalance.plus(saleProceeds);

        if (holding.totalShares().isZero()) {
            holdings.remove(ticker);
        }

        // Return result — caller (application service) creates the Transaction record
        return new SellResult(saleProceeds, breakdown.totalCostBasis(), realizedPnL, breakdown);
    }
}
```

**Application service coordination:**

```java
@Service
@Transactional
public class SellStocksService {

    private final PortfolioRepository portfolioRepo;
    private final TransactionRepository transactionRepo;

    public SellStocksResponse execute(SellStocksCommand cmd) {
        Portfolio portfolio = portfolioRepo.getById(cmd.portfolioId());

        SellResult result = portfolio.sell(
            cmd.ticker(), cmd.quantity(), cmd.price(), cmd.occurredAt()
        );

        // Both in the SAME DB transaction
        portfolioRepo.save(portfolio);

        Transaction tx = Transaction.sellTransaction(
            TransactionId.generate(),
            portfolio.id(),
            cmd.ticker(), cmd.quantity(), cmd.price(),
            result.saleProceeds(), result.realizedPnL(),
            cmd.occurredAt()
        );
        transactionRepo.save(tx);

        return SellStocksResponse.from(result);
    }
}
```

**Query patterns:**

```java
public interface TransactionRepository {
    void save(Transaction tx);

    PagedResult<Transaction> findByPortfolio(
        PortfolioId portfolioId,
        TransactionFilter filter,  // type, ticker, dateRange
        SortDirection sortByDate,
        int pageSize,
        TransactionCursor cursor   // keyset pagination cursor
    );
}
```

**Verdict:** **This is the industry standard for financial ledger systems.** Strong recommendation.

---

### Option C — Domain Events + Outbox Pattern

**How it works**: Portfolio emits domain events (`SharesSold`, `FundsDeposited`, etc.). These events are persisted in an outbox table in the same transaction as the aggregate change. A separate consumer builds a `TransactionLedger` read model asynchronously.

**Pros:**
- Cleanest DDD separation — aggregate emits facts, projections build views
- Transaction history becomes a CQRS read model, optimized independently
- Enables event sourcing migration path
- Natural audit trail
- Supports multiple projections (transaction history, analytics, tax reports)

**Cons:**
- **Eventually consistent** transaction history — after a sell, the transaction may not appear for milliseconds to seconds
- Significant infrastructure complexity (outbox table, message relay, consumer, idempotency)
- Overkill for most CRUD-heavy portfolio systems
- Debugging is harder (which event produced which projection row?)
- Requires compensating logic if projection fails

**Domain model change:**

```java
public class Portfolio {
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public SellResult sell(TickerSymbol ticker, ShareQuantity qty, Price price, Instant occurredAt) {
        // ...validate and execute...
        SellResult result = /* ... */;

        pendingEvents.add(new SharesSoldEvent(
            this.id, ticker, qty, price,
            result.saleProceeds(), result.realizedPnL(), occurredAt
        ));

        return result;
    }

    public List<DomainEvent> drainPendingEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }
}
```

**DB mapping — outbox table (same DB as portfolio):**

```sql
CREATE TABLE domain_event_outbox (
    id              BIGSERIAL     PRIMARY KEY,
    aggregate_type  VARCHAR(50)   NOT NULL,
    aggregate_id    VARCHAR(36)   NOT NULL,
    event_type      VARCHAR(100)  NOT NULL,
    payload         JSONB         NOT NULL,
    occurred_at     TIMESTAMP     NOT NULL,
    published       BOOLEAN       NOT NULL DEFAULT FALSE
);

-- Read model: built by event consumer
CREATE TABLE transaction_read_model (
    id              VARCHAR(36)   PRIMARY KEY,
    portfolio_id    VARCHAR(36)   NOT NULL,
    type            VARCHAR(10)   NOT NULL,
    occurred_at     TIMESTAMP     NOT NULL,
    ticker          VARCHAR(10),
    quantity        DECIMAL(18,8),
    price           DECIMAL(18,8),
    amount          DECIMAL(18,2) NOT NULL,
    realized_pnl    DECIMAL(18,2)
);
```

**Consistency:** Portfolio state is strongly consistent. Transaction history is **eventually consistent** (typically < 100ms with polling, < 10ms with WAL-based CDC like Debezium).

**Verdict:** Best for event-driven architectures, microservices, or when multiple downstream consumers need portfolio events. Overkill for a monolith with a single relational DB.

---

### Option D — Hybrid Approach (Recent Transactions Inside Portfolio)

**How it works**: Portfolio keeps a bounded `recentTransactions` list (e.g., last 10–50) for any business rules that might need recent context. Full history lives in a separate table/aggregate.

**Pros:**
- Supports business rules like "reject if duplicate transaction in last 5 minutes"
- Aggregate stays bounded and predictable
- Full history remains independently queryable
- Pragmatic middle ground

**Cons:**
- Two sources of truth for recent transactions (aggregate + external store)
- More complex aggregate design — must define eviction/rotation policy
- If no business rule actually needs recent transactions, this adds complexity for zero benefit
- Hibernate mapping of a bounded collection requires manual truncation logic

**Domain model change:**

```java
public class Portfolio {
    // Bounded: only last N transactions for duplicate detection, etc.
    private Deque<Transaction> recentTransactions; // max 20 entries

    private static final int MAX_RECENT = 20;

    private void recordTransaction(Transaction tx) {
        recentTransactions.addFirst(tx);
        if (recentTransactions.size() > MAX_RECENT) {
            recentTransactions.removeLast();
        }
    }

    // Business rule that requires recent context:
    private void guardAgainstDuplicateTrade(TickerSymbol ticker, Instant occurredAt) {
        boolean duplicate = recentTransactions.stream()
            .anyMatch(t -> t.ticker().equals(ticker)
                && Duration.between(t.occurredAt(), occurredAt).toMinutes() < 1);
        if (duplicate) throw new DuplicateTradeException();
    }
}
```

**Consistency:** Strongly consistent for the bounded window. Full history is managed externally (same DB transaction, different repository — like Option B).

**Verdict:** Only justified if a concrete business rule requires recent transaction context within the aggregate. **Do not add this speculatively.**

---

## 12. Decision Matrix

| Criteria | A: Inside Aggregate | B: Separate Aggregate | C: Events + Projection | D: Hybrid |
|---|---|---|---|---|
| **DDD Purity** | ⚠️ Inflated boundary | ✅ Clean separation | ✅✅ Cleanest | ✅ Clean if justified |
| **Invariant Safety** | ✅ Trivial | ✅ App service discipline | ✅ Event-driven | ✅ Bounded window |
| **Strong Consistency** | ✅ Same aggregate | ✅ Same DB tx | ⚠️ Eventually consistent | ✅ Same DB tx |
| **Read Performance** | ❌ Full load or break encapsulation | ✅ Direct SQL queries | ✅ Optimized read model | ✅ Direct SQL queries |
| **Write Performance** | ❌ Dirty checking, contention | ✅ Simple INSERT | ✅ Simple INSERT | ✅ Simple INSERT |
| **Scalability (100k+ tx)** | ❌ Memory/GC collapse | ✅ Paginated queries | ✅✅ Independent scaling | ✅ Paginated queries |
| **Query Flexibility** | ❌ In-memory only | ✅ Full SQL | ✅ Full SQL | ✅ Full SQL |
| **Implementation Complexity** | ✅ Simple | ✅ Low | ❌ High (infra) | ⚠️ Medium |
| **Testability** | ⚠️ Heavy aggregate setup | ✅ Focused tests | ⚠️ Async testing harder | ✅ Focused tests |
| **Team Familiarity** | ✅ Obvious pattern | ✅ Standard JPA | ⚠️ Requires event infra knowledge | ✅ Standard JPA |

---

## 13. Recommended Architecture for Enterprise Systems

### Default Choice: Option B — Separate Transaction Aggregate with `portfolioId` Reference

This is the correct default for a **Spring Boot + JPA + relational DB** system because:

1. **No invariant requires transactions inside the aggregate.** This is the decisive factor.
2. **Strong consistency is preserved** via a single `@Transactional` method that saves both Portfolio and Transaction.
3. **Transaction history queries become trivial SQL** with proper indexes.
4. **Portfolio aggregate stays lean**: ~5–20 entities (holdings + lots), never grows unbounded.
5. **The pattern is well-understood** by Java/Spring teams and requires zero additional infrastructure.

### What Must Be Strongly Consistent (Same DB Transaction)

- `Portfolio.cashBalance` change
- `Holding` / `Lot` mutations
- `Transaction` INSERT

All three happen in a single `@Transactional` application service method. This is **not** eventual consistency — it's just two repositories participating in the same JDBC transaction.

### Application Service Coordination Pattern

Following DDD's **layered architecture** and Vernon's approach to **application services**, the coordination workflow is:

1. Load the Portfolio aggregate.
2. Apply domain logic to modify Holdings and Lots.
3. Persist the updated Portfolio.
4. Create and persist the corresponding Transaction entry.

In a single-database scenario, all steps occur within an **ACID transaction**. In distributed scenarios, a domain event (e.g., *TradeExecuted*) can be published and consumed to persist the Transaction asynchronously, ensuring eventual consistency.

---

## 14. Implementation Notes (Java + Relational DB)

### 14.1 Correct Portfolio JPA Mapping (Option B)

```java
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Version
    private Long version;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "cash_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "cash_currency", nullable = false, length = 3)
    private String cashCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Holdings ARE part of the aggregate — bounded, invariant-enforcing
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @BatchSize(size = 20) // avoid N+1 when loading holdings
    private Set<HoldingJpaEntity> holdings = new HashSet<>();
    // Use Set (not List) to avoid Hibernate Bag semantics issues

    // NO transactions field — they live in their own table/repository
}
```

### 14.2 Transaction JPA Entity (Standalone)

```java
@Entity
@Table(name = "portfolio_transaction", indexes = {
    @Index(name = "idx_tx_portfolio_date", columnList = "portfolio_id, occurred_at DESC"),
    @Index(name = "idx_tx_portfolio_type", columnList = "portfolio_id, type"),
    @Index(name = "idx_tx_portfolio_ticker_date", columnList = "portfolio_id, ticker, occurred_at DESC")
})
public class TransactionJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "portfolio_id", nullable = false, length = 36)
    private String portfolioId; // FK, but NOT a @ManyToOne — no bidirectional navigation

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TransactionTypeJpa type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ticker", length = 10)
    private String ticker;

    @Column(name = "quantity", precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 8)
    private BigDecimal price;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "realized_pnl", precision = 18, scale = 2)
    private BigDecimal realizedPnL;
}
```

### 14.3 Repository Design

**Domain port — framework-free:**

```java
public interface TransactionRepository {

    void save(Transaction transaction);

    /**
     * Paginated transaction history for a portfolio.
     * This is a READ operation — does NOT load the Portfolio aggregate.
     */
    PagedResult<Transaction> findByPortfolio(
        PortfolioId portfolioId,
        TransactionFilter filter,
        SortDirection sortByDate,
        int pageSize,
        TransactionCursor cursor  // keyset pagination cursor
    );

    List<Transaction> findSellsByPortfolioAndTicker(
        PortfolioId portfolioId,
        TickerSymbol ticker,
        Instant from,
        Instant to
    );
}
```

**Infrastructure implementation:**

```java
@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private final TransactionJpaSpringDataRepo springDataRepo;
    private final TransactionJpaMapper mapper;

    @Override
    public void save(Transaction transaction) {
        springDataRepo.save(mapper.toJpa(transaction));
    }

    @Override
    public PagedResult<Transaction> findByPortfolio(
            PortfolioId portfolioId,
            TransactionFilter filter,
            SortDirection sortByDate,
            int pageSize,
            TransactionCursor cursor) {

        List<TransactionJpaEntity> rows;

        if (cursor == null) {
            rows = springDataRepo.findFirstPage(
                portfolioId.value(),
                filter.type().orElse(null),
                filter.ticker().orElse(null),
                filter.from().orElse(null),
                filter.to().orElse(null),
                PageRequest.of(0, pageSize + 1) // +1 to detect hasNext
            );
        } else {
            rows = springDataRepo.findNextPage(
                portfolioId.value(),
                filter.type().orElse(null),
                filter.ticker().orElse(null),
                cursor.occurredAt(),
                cursor.id(),
                pageSize + 1
            );
        }

        boolean hasNext = rows.size() > pageSize;
        List<TransactionJpaEntity> pageRows = hasNext
            ? rows.subList(0, pageSize)
            : rows;

        List<Transaction> transactions = pageRows.stream()
            .map(mapper::toDomain)
            .toList();

        TransactionCursor nextCursor = hasNext
            ? TransactionCursor.from(pageRows.get(pageSize - 1))
            : null;

        return new PagedResult<>(transactions, nextCursor, hasNext);
    }
}
```

**Spring Data query interface:**

```java
public interface TransactionJpaSpringDataRepo extends JpaRepository<TransactionJpaEntity, String> {

    @Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.portfolioId = :portfolioId
          AND (:type IS NULL OR t.type = :type)
          AND (:ticker IS NULL OR t.ticker = :ticker)
          AND (:from IS NULL OR t.occurredAt >= :from)
          AND (:to IS NULL OR t.occurredAt <= :to)
        ORDER BY t.occurredAt DESC, t.id DESC
        """)
    List<TransactionJpaEntity> findFirstPage(
        @Param("portfolioId") String portfolioId,
        @Param("type") TransactionTypeJpa type,
        @Param("ticker") String ticker,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );

    @Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.portfolioId = :portfolioId
          AND (:type IS NULL OR t.type = :type)
          AND (:ticker IS NULL OR t.ticker = :ticker)
          AND (t.occurredAt < :cursorDate
               OR (t.occurredAt = :cursorDate AND t.id < :cursorId))
        ORDER BY t.occurredAt DESC, t.id DESC
        LIMIT :limit
        """)
    List<TransactionJpaEntity> findNextPage(
        @Param("portfolioId") String portfolioId,
        @Param("type") TransactionTypeJpa type,
        @Param("ticker") String ticker,
        @Param("cursorDate") Instant cursorDate,
        @Param("cursorId") String cursorId,
        @Param("limit") int limit
    );
}
```

---

## 15. Data Model Example

```
┌──────────────────┐         ┌──────────────────────────┐
│    portfolio     │         │   portfolio_transaction   │
├──────────────────┤         ├──────────────────────────┤
│ id          PK   │◄───FK───│ portfolio_id             │
│ version          │         │ id                  PK   │
│ owner_name       │         │ type                     │
│ cash_balance     │         │ occurred_at              │
│ cash_currency    │         │ ticker         [nullable] │
│ created_at       │         │ quantity       [nullable] │
└──────────────────┘         │ price          [nullable] │
        │                    │ amount                    │
        │ 1:N                │ currency                  │
        ▼                    │ realized_pnl  [nullable] │
┌──────────────────┐         └──────────────────────────┘
│    holding       │
├──────────────────┤         Key Indexes:
│ id          PK   │         • (portfolio_id, occurred_at DESC)  ← primary query path
│ portfolio_id FK  │         • (portfolio_id, type)              ← filter by type
│ ticker           │         • (portfolio_id, ticker, occurred_at DESC)
└──────────────────┘
        │ 1:N
        ▼
┌──────────────────┐
│      lot         │
├──────────────────┤
│ id          PK   │
│ holding_id  FK   │
│ purchased_qty    │
│ remaining_qty    │
│ purchase_price   │
│ purchased_at     │
└──────────────────┘
```

The key insight: **Portfolio ↔ Holding ↔ Lot** is a true aggregate (composition, shared invariants). **Portfolio ↔ Transaction** is an association by identifier (separate lifecycle, independent queries).

---

## 16. Query and Pagination Strategies

### Keyset vs Offset Pagination

| Aspect | OFFSET / LIMIT | Keyset (Cursor) |
|---|---|---|
| Page 1 | Fast | Fast |
| Page 1000 | Slow (scans 999 pages) | Fast (seeks by index) |
| Stable under concurrent inserts | ❌ Rows shift | ✅ Cursor is stable |
| Jump to arbitrary page | ✅ Trivial | ❌ Sequential only |
| **Recommended for transaction history** | No | **Yes** |

**Keyset pagination** is the recommended strategy for transaction history because:

1. Transaction history is naturally ordered by `occurred_at DESC`.
2. Users typically scroll forward, not jump to page 500.
3. New transactions inserted at the head don't shift existing pages.
4. Performance is constant regardless of how deep into the history you are.

### Summary Fields on Portfolio

Derived values like **cash balance** and **total market value** should be stored directly on the Portfolio entity. This eliminates the need to recompute them from transaction history:

```sql
-- No need for: SELECT SUM(amount) FROM portfolio_transaction WHERE portfolio_id = ?
-- cashBalance is already maintained on the Portfolio aggregate itself.
```

This is a direct benefit of the DDD approach: the aggregate maintains its own state, and the transaction history is purely a historical record.

---

## 17. Summary

This design:

- **Respects DDD tactical modeling** by keeping aggregates small, cohesive, and invariant-focused (Evans, Vernon, Nilsson).
- **Preserves performance** by avoiding unbounded collections inside a single aggregate.
- **Aligns with relational database reality** — transaction history is queried with SQL, paginated with keyset cursors, and indexed for the access patterns the API requires.
- **Avoids JPA/Hibernate pitfalls** — no dirty-checking overhead, no Bag semantics traps, no accidental eager loading.
- **Fits naturally** with Spring + JPA repository patterns and application service orchestration.
- **Supports future evolution** toward CQRS or event sourcing (as recommended by Vernon) without major restructuring.

> **Key Decision**: The Portfolio aggregate contains Holdings and Lots but not the full list of Transactions. Transactions are modeled as a separate aggregate, linked by `portfolioId` and coordinated via the application layer within a single database transaction. This approach maintains domain purity, scalability, and operational efficiency.

---

## 18. When to Choose a Different Architecture

Use this checklist to determine if the default (Option B) should be replaced:

| Situation | Choose | Why |
|---|---|---|
| Prototype / < 50 transactions ever | **Option A** | Simplicity wins; refactor later if needed |
| Standard enterprise system, relational DB | **Option B** | This is your answer — balanced and proven |
| Microservices with multiple consumers of portfolio events | **Option C** | Invest in event infrastructure for decoupled projections |
| Concrete business rule needs recent tx context (duplicate detection, velocity checks) | **Option D** | But prove the rule exists first — do not add speculatively |
| Event sourced system | **Option C** | Transactions become projections of the event stream |
| Regulatory requirement for immutable audit log separate from operational data | **Option C** | Append-only event store with projections |
| Sub-millisecond reads on transaction history | **Option C (CQRS lite)** | Denormalized read model optimized for the specific query |

### Final Checklist — Recommended Baseline

- [x] **Remove** `List<Transaction>` from the Portfolio aggregate
- [x] **Create** a standalone `TransactionRepository` port in the domain layer
- [x] **Create** Transaction in the same `@Transactional` service method as Portfolio save
- [x] **Index** `(portfolio_id, occurred_at DESC)` as the primary query path
- [x] **Use** keyset pagination for the transaction history endpoint
- [x] **Keep** Portfolio aggregate containing only: cashBalance, holdings, lots
- [x] **Store** `portfolioId` as a plain FK on the transaction table — no `@ManyToOne` bidirectional navigation

### When to Deviate — Prove It First

- [ ] You have a **proven business rule** that reads recent transactions → Consider Option D (Hybrid)
- [ ] You're building a **microservice architecture** with multiple consumers → Consider Option C (Events)
- [ ] You're building a **prototype** with < 50 transactions per portfolio → Option A is fine temporarily
- [ ] You need **sub-millisecond reads** on transaction history → Consider a denormalized read model (CQRS lite)

---

## 19. References

- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Fowler, Martin. \"CQRS.\" *martinfowler.com*, 2011. https://martinfowler.com/bliki/CQRS.html
- Fowler, Martin. *Patterns of Enterprise Application Architecture.* Addison-Wesley, 2002.
- Nilsson, Jimmy. *Applying Domain-Driven Design and Patterns: With Examples in C# and .NET.* Addison-Wesley, 2006.
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013. (Ch. 10 on Aggregates, ch. 14 on Application Services, ch. 4 on CQRS.)
- Young, Greg. \"CQRS Documents.\" 2010. https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf

- [ ] **Regulatory requirements** demand an immutable audit log separate from operational data → Option C with append-only event store