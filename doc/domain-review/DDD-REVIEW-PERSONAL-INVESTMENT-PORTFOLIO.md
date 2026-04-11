# Architectural Review: Domain-Driven Design and Hexagonal Architecture

**Project:** HexaStock
**Review Date:** April 2026
**Scope:** Full multi-module architecture — domain, application, adapters, bootstrap

---

## 1. Executive Summary

The HexaStock domain model is a well-constructed, behaviour-rich aggregate design that successfully captures the core mechanics of a personal investment portfolio. It is among the better DDD models one encounters in educational codebases and holds up respectably against production-grade designs for its stated scope.

The model's principal DDD strengths are:

- A genuinely rich `Portfolio` aggregate root that enforces cash balance, FIFO lot consumption, and share availability invariants through behaviour, not getters and setters.
- A disciplined value object layer (`Money`, `Price`, `ShareQuantity`, `Ticker`) that eliminates primitive obsession and encodes domain rules at the type level.
- A clean separation between the `Portfolio` aggregate (current positional state) and `Transaction` (historical record), with thoughtful reasoning documented in the code itself.

Its principal remaining DDD concerns are:

- Transaction creation is an application-service concern with no domain involvement, which means the domain model does not participate in ensuring that its own history is correct. Domain events — which would close this gap — are an intentional future enhancement, not yet introduced.
- The `HoldingPerformanceCalculator` domain service re-derives positional data from transactions rather than from the aggregate, creating a parallel truth that could diverge.
- Some financial realism is deliberately sacrificed (single currency, integer share quantities, no fees or taxes), which is acceptable for pedagogy but should be explicitly acknowledged as a boundary.

A previous version of this review identified the `Transaction` entity as a structurally anemic, type-tagged data container. **This has been resolved.** `Transaction` is now a sealed interface hierarchy with four record subtypes (`DepositTransaction`, `WithdrawalTransaction`, `PurchaseTransaction`, `SaleTransaction`), each carrying only the fields meaningful to its transaction kind, with self-validation in every compact constructor. The sealed hierarchy eliminates nullable fields, enables exhaustive pattern matching in Java 21, and aligns each subtype with domain language.

**Hexagonal Architecture verdict:** The project is genuinely hexagonal — not merely in naming conventions, but in substance. The six-module Maven structure (`domain`, `application`, `adapters-inbound-rest`, `adapters-outbound-persistence-jpa`, `adapters-outbound-market`, `bootstrap`) enforces layer dependencies at the build level. The domain module carries zero framework dependencies. The application layer depends only on domain abstractions, outbound port interfaces, and the standard `jakarta.transaction-api` for transactional demarcation — it contains zero Spring imports. Inbound adapters depend on inbound ports (use-case interfaces), outbound adapters implement outbound ports, and the bootstrap module acts as a clean composition root. ArchUnit fitness tests in `HexagonalArchitectureTest` mechanically enforce these boundaries at every build, including an explicit rule verifying that the application layer does not depend on Spring. A secondary concern is the presence of `protected` no-argument constructors and a `addLotFromPersistence()` method on the domain model — pragmatic JPA concessions that do not break the architecture but subtly reveal persistence awareness in the domain's API surface.

A previous version of this review identified a hexagonal leak in `TransactionDTO`, which wrapped the domain `Transaction` object directly, exposing domain structure to the REST API contract. **This has been fixed.** `TransactionDTO` is now a flat record of primitive/simple fields with a `from(Transaction)` factory method, following the same mapping pattern as all other DTOs in the REST adapter.

**Combined verdict:** DDD and Hexagonal Architecture reinforce each other well in this project. The clean domain boundary that makes the DDD aggregate model credible is the same boundary that makes the hexagonal separation genuine. The project is well above average for a teaching codebase and defensible for a simple production application. The transaction model — now a sealed interface hierarchy with self-validating record subtypes — is aligned with the rest of the domain's type-safe, behaviour-rich design. The remaining areas for future improvement are domain event–driven transaction creation (to close the consistency gap between aggregate and audit trail) and richer financial realism (fees, fractional shares, multi-currency).

---

## 2. Domain Framing

### 2.1 What Is This Domain?

A personal investment portfolio is a financial structure in which an individual holds cash and equity positions. The core operations are:

- **Cash management:** depositing and withdrawing funds.
- **Trading:** buying and selling shares of publicly traded companies at market prices.
- **Position tracking:** knowing how many shares of each company are held, at what cost, in what lots.
- **Performance measurement:** calculating unrealised gains, realised gains, cost basis, and profit.

This is a retail investment domain — not institutional trading, not derivatives, not multi-asset-class portfolio management. The distinction matters because it constrains the ubiquitous language and the invariants that the model must protect.

### 2.2 What Invariants Must the Model Protect?

A credible personal investment portfolio model must enforce at a minimum:

1. **Cash sufficiency:** you cannot buy shares or withdraw funds in excess of your available cash balance.
2. **Share availability:** you cannot sell more shares of a stock than you currently hold.
3. **Lot integrity:** individual lots must track their remaining shares accurately through partial sales.
4. **FIFO ordering:** when selling, the oldest lots must be consumed first (assuming FIFO is the chosen accounting method).
5. **Cost basis correctness:** realised gain/loss must be computed from the actual purchase price of the specific lots consumed.
6. **Non-negative quantities:** share quantities and cash balances must never go negative.
7. **Portfolio coherence:** the holdings map must remain consistent with the lots it contains — no orphaned lots, no phantom holdings.

### 2.3 Bounded Context Observations

The HexaStock domain implicitly spans at least three subdomains:

- **Trading:** executing buy/sell operations against market prices.
- **Position management:** tracking what is owned, in what lots, at what cost.
- **Reporting:** computing performance metrics from historical and current data.

The current model merges trading and position management into a single aggregate (`Portfolio`), which is a pragmatic and defensible choice for a personal investment application where these concerns are tightly coupled. Reporting is partially factored out (via `HoldingPerformanceCalculator` and `ReportingService`), but it still reaches into both the aggregate and the transaction store, creating a cross-cutting dependency that deserves attention.

---

## 3. Evaluation of the Current Model

### 3.1 Are the Core Concepts Natural?

**Portfolio → Holding → Lot** is a natural and widely recognised conceptual hierarchy in the investment domain:

- A **Portfolio** is a container for all of an investor's positions and cash.
- A **Holding** represents ownership of a specific stock (identified by ticker).
- A **Lot** represents a specific purchase event — shares acquired at a specific price on a specific date.

This hierarchy mirrors how brokerage systems, tax reporting tools, and personal finance applications model the same domain. It is not an arbitrary technical decomposition; it reflects how investors and accountants actually think about portfolios.

**Transaction** as a separate concept is also natural: it represents a historical record of a financial event. Brokerage statements, tax forms, and audit trails are all built from transaction records.

### 3.2 Does the Model Support Core Invariants?

The model enforces the critical invariants identified in Section 2.2:

| Invariant | Where Enforced | Assessment |
|---|---|---|
| Cash sufficiency | `Portfolio.buy()`, `Portfolio.withdraw()` | Correct. Checked before mutation. |
| Share availability | `Holding.sell()` | Correct. Checked before lot consumption. |
| Lot integrity | `Lot.reduce()` | Correct. Cannot reduce below zero. |
| FIFO ordering | `Holding.sell()` — iterates lots in insertion order; `Holding.addLotFromPersistence()` inserts in chronological order | Correct. Domain-side insertion sort guarantees FIFO regardless of persistence load order. |
| Cost basis correctness | `Lot.calculateCostBasis()` + `SellResult` | Correct. Computed from actual lot prices. |
| Non-negative quantities | `ShareQuantity` constructor, `Money` arithmetic | Correct for shares. Money can go negative (no floor), which is acceptable for intermediate calculations but worth noting. |
| Portfolio coherence | `Portfolio.addHolding()`, `findOrCreateHolding()` | Correct. No duplicate holdings per ticker. |

The invariant enforcement is genuinely behaviour-driven. The aggregate root delegates to its child entities but remains the entry point for all mutations. This is textbook DDD aggregate design executed well.

### 3.3 Is the Model Rich or Anemic?

The model is **genuinely rich**, not anemic:

- `Portfolio.buy()` validates inputs, checks funds, finds or creates holdings, delegates to `Holding.buy()`, and debits the balance — all in one cohesive operation.
- `Portfolio.sell()` validates inputs, locates the holding, delegates to `Holding.sell()` which implements FIFO lot consumption, credits the balance, and returns a structured financial result.
- `Holding.sell()` contains the FIFO algorithm — a non-trivial domain algorithm with real financial semantics.
- `Lot.reduce()` and `Lot.calculateCostBasis()` encapsulate lot-level behaviour.
- `SellResult` is a well-designed value object that captures the financial outcome of a sale.

The `Transaction` sealed interface and its four record subtypes (`DepositTransaction`, `WithdrawalTransaction`, `PurchaseTransaction`, `SaleTransaction`) extend this richness to the transaction model: each subtype carries only the fields meaningful to its kind, compact constructors enforce non-null and positivity invariants, and the sealed hierarchy enables exhaustive pattern matching in Java 21. Section 5 discusses the transaction modelling in detail.

### 3.4 Overall Assessment

The current model is a credible, well-structured DDD design for its problem scope. It is not a toy model dressed in DDD vocabulary; the aggregate boundaries protect real invariants, the value objects encode real domain rules, and the core operations implement real financial logic. The design decisions are documented in the code and reflect genuine architectural reasoning.

---

## 4. Aggregate Design Review

### 4.1 Should Portfolio Be the Aggregate Root?

**Yes.** Portfolio is the natural transactional consistency boundary for the operations this domain supports. Every meaningful operation — buy, sell, deposit, withdraw — modifies the portfolio's state (balance, holdings, lots) and must maintain consistency across those modifications atomically. There is no scenario in this domain where you would modify a holding or lot independently of its portfolio.

The alternative — making Holding or Lot independently addressable aggregates — would break the invariant that buying shares must atomically debit the cash balance and credit the holding. This invariant requires a single transactional boundary, which is exactly what the Portfolio aggregate provides.

### 4.2 Should Holding Live Inside Portfolio?

**Yes, and the current design handles this correctly.** A holding has no meaningful existence outside its portfolio. The `Map<Ticker, Holding>` indexed by ticker is a natural and efficient structure: it enforces the invariant that there is at most one holding per ticker per portfolio, and it enables O(1) lookup during buy/sell operations.

One subtlety: `Holding` has its own `HoldingId`, which gives it entity identity within the aggregate. This is correct — it needs identity for persistence mapping and for distinguishing holdings in collections — but it is important that `HoldingId` is never used as an external reference. Holdings should only be addressed through their portfolio and ticker, never by their internal ID from outside the aggregate. The current code respects this boundary.

### 4.3 Should Lot Live Inside Holding?

**Yes.** Lots are inseparable from their holding. The FIFO sell algorithm iterates over lots within a single holding; splitting lots into a separate aggregate would make this algorithm impossible to implement atomically without distributed coordination.

The `List<Lot>` structure inside `Holding` is appropriate: insertion order preserves FIFO semantics, and the list is iterated during sell operations. An alternative structure (e.g., a `TreeMap` keyed by purchase date) would provide the same FIFO guarantee more explicitly but at the cost of unnecessary complexity for the current requirements.

### 4.4 Is the Aggregate Too Large?

This is the most important design tension in the model, and the answer is nuanced.

**For a personal investment portfolio: no, the aggregate is not too large.** A typical retail investor holds 10–50 distinct stocks, each with 1–20 lots. This means the Portfolio aggregate contains at most a few hundred entities. Loading, persisting, and operating on this aggregate in a single transaction is entirely feasible with any modern ORM and database.

**For an institutional or high-frequency context: yes, it would be.** If a portfolio could contain thousands of distinct holdings or millions of lots, loading the entire aggregate for every operation would become prohibitive. But this is not the domain the model serves.

The key design question is whether the aggregate is too large for **concurrent access**. Since each portfolio belongs to a single owner and personal investment operations are low-frequency (perhaps a few trades per day), contention on the portfolio aggregate lock is effectively zero. This makes the inclusive aggregate boundary a pragmatic and correct choice.

### 4.5 Transaction as a Separate Aggregate

The decision to keep `Transaction` outside the `Portfolio` aggregate is explicitly documented in the `Transaction.java` Javadoc and is **correct for the right reasons**:

- Transaction history is append-only and unbounded. Including it in the Portfolio aggregate would create an ever-growing aggregate that loads all historical transactions for every operation.
- Transactions do not participate in the invariants that the Portfolio aggregate protects. The portfolio does not need to read its transaction history to enforce cash sufficiency, share availability, or FIFO ordering.
- Transactions are written by the application service after the aggregate mutation, which is a reasonable eventual-consistency trade-off for this domain.

However, this separation creates a subtle risk: **the transaction log can become inconsistent with the portfolio state** if the application service fails between saving the portfolio and saving the transaction, or if a developer forgets to create a transaction for some operation. This is discussed further in Section 5.

### 4.6 Aggregate Design Verdict

The aggregate structure — `Portfolio` containing `Holding` containing `Lot`, with `Transaction` external — is appropriate, well-reasoned, and correctly motivated. The boundaries are drawn where the invariants demand them, and the size is appropriate for the domain's access patterns.

---

## 5. Transaction Modelling Review

This section reviews the current transaction modelling, which has been significantly strengthened since the initial codebase.

### 5.1 Current Design

`Transaction` is a **sealed interface** with four record subtypes:

```java
public sealed interface Transaction
        permits DepositTransaction, WithdrawalTransaction, PurchaseTransaction, SaleTransaction
```

Each subtype is a Java record carrying only the fields meaningful to its transaction kind:

- `DepositTransaction(TransactionId, PortfolioId, Money totalAmount, LocalDateTime)` — cash deposited.
- `WithdrawalTransaction(TransactionId, PortfolioId, Money totalAmount, LocalDateTime)` — cash withdrawn.
- `PurchaseTransaction(TransactionId, PortfolioId, Ticker, ShareQuantity, Price, Money totalAmount, LocalDateTime)` — shares bought.
- `SaleTransaction(TransactionId, PortfolioId, Ticker, ShareQuantity, Price, Money totalAmount, Money profit, LocalDateTime)` — shares sold, with realised profit.

The sealed interface defines:

- Core accessor methods: `id()`, `portfolioId()`, `type()`, `totalAmount()`, `createdAt()`.
- Default accessor methods with safe defaults for non-applicable fields: `ticker()` returns `null`, `quantity()` returns `ShareQuantity.ZERO`, `unitPrice()` returns `null`, `profit()` returns `Money.ZERO`. These defaults allow gradual migration away from type-checking in consuming code while pattern matching is the preferred approach.
- Static factory methods (`createDeposit`, `createWithdrawal`, `createPurchase`, `createSale`) for convenience.

Each record's compact constructor enforces non-null invariants and rejects zero or negative amounts and quantities, preventing the construction of semantically invalid transaction instances at the type level.

Transactions are created by the application services (`PortfolioStockOperationsService`, `PortfolioManagementService`) after the aggregate is mutated, and saved through `TransactionPort`.

### 5.2 Structural Assessment: From Type-Tag to Sealed Hierarchy

The previous design used a single flat `Transaction` class with a `TransactionType` enum tag and nullable fields. Different transaction types used different subsets of fields, which meant:

- Deposit and withdrawal transactions carried meaningless `quantity` and `ticker` fields.
- Sale transactions had a `profit` field that was nullable at the type level but required by the business.
- No compile-time guarantee prevented constructing semantically invalid combinations.

**This has been resolved.** The sealed hierarchy eliminates nullable fields entirely. Each subtype carries exactly the fields it needs:

| Field        | `DepositTransaction` | `WithdrawalTransaction` | `PurchaseTransaction` | `SaleTransaction` |
|---|---|---|---|---|
| `totalAmount` | ✅ | ✅ | ✅ | ✅ |
| `ticker`      | — | — | ✅ | ✅ |
| `quantity`    | — | — | ✅ | ✅ |
| `unitPrice`   | — | — | ✅ | ✅ |
| `profit`      | — | — | — | ✅ |

The sealed hierarchy enables exhaustive `switch` expressions in Java 21. The `TransactionMapper` (persistence adapter) and `HoldingPerformanceCalculator` (domain service) both use exhaustive switches on `TransactionType`, ensuring that adding a new transaction subtype produces a compile-time error in every consumer that must handle it.

### 5.3 Behavioural Assessment: Self-Validating Records

Each record subtype validates its own invariants in its compact constructor:

- All subtypes reject null `id`, `portfolioId`, `totalAmount`, and `createdAt`.
- `DepositTransaction` and `WithdrawalTransaction` reject non-positive amounts.
- `PurchaseTransaction` rejects null `ticker`, `unitPrice`, and non-positive `quantity`.
- `SaleTransaction` rejects null `ticker`, `unitPrice`, `profit`, and non-positive `quantity`.

This self-validation is a significant improvement. It is no longer possible to construct a `SaleTransaction` without a ticker, or a `DepositTransaction` with a negative amount. The domain's type system enforces these constraints at construction time, not by convention.

**What remains outside the subtypes:** The `totalAmount` for a `PurchaseTransaction` is `unitPrice × quantity`, but the constructor accepts it as a parameter rather than computing it. This is a pragmatic choice: the factory method (`Transaction.createPurchase`) computes it, while the constructor allows reconstitution from persistence where the stored value should be preserved rather than recomputed. The trade-off is that `totalAmount` could, in theory, be passed incorrectly during reconstitution — but the persistence mapper is the only code path that uses the constructor directly, and it passes the stored value.

### 5.4 Architectural Concern: Who Is Responsible for Creating Transactions?

Transaction creation remains an application-service responsibility:

```java
// In PortfolioStockOperationsService.sellStock()
SellResult sellResult = portfolio.sell(ticker, quantity, price);
portfolioPort.savePortfolio(portfolio);

Transaction transaction = Transaction.createSale(
        portfolioId, ticker, quantity, price, sellResult.proceeds(), sellResult.profit());
transactionPort.save(transaction);
```

This creates two risks that remain present in the current design:

1. **Consistency risk:** If a developer adds a new operation and forgets to create a corresponding transaction, the audit trail becomes incomplete. Both `portfolioPort.savePortfolio()` and `transactionPort.save()` are within a `@Transactional` boundary, which ensures atomicity for infrastructure failures, but does not protect against developer omission.

2. **Transcription burden:** The application service must extract financial details from `SellResult` and pass them to `Transaction.createSale()`. While `SellResult` makes this coupling explicit and reasonable, the responsibility for correctly transcribing domain results into transaction records arguably belongs closer to the domain.

**Intentional trade-off:** Domain events — where the aggregate emits events and an event handler derives transaction records — would close this consistency gap. This enhancement has been intentionally deferred: the current design keeps the domain model free of event infrastructure, and the audit record is written within the same `@Transactional` boundary. Domain events remain the natural evolution path when the project needs asynchronous processing (e.g., notifying a tax service or updating a reporting database).

### 5.5 DDD Classification: Immutable Ledger Entry

The `Transaction` Javadoc correctly describes it as an **immutable ledger entry** — an entity with identity (`TransactionId`), created by the application layer after the aggregate mutation, persisted for auditability and reporting, but never modified after creation. This classification is accurate:

- It is an **entity** because it has a unique identifier (`TransactionId`).
- It is **immutable** because all subtypes are Java records (inherently immutable).
- It is not a **value object** (it has identity) and not a **domain event** (it is not infrastructure).

The use of Java records for the subtypes makes immutability structural rather than conventional — the compiler enforces it.

### 5.6 Should Transactions Live Inside the Portfolio Aggregate?

**No.** The current design's decision to keep transactions external is correct. Transactions are an unbounded, append-only collection. Including them in the aggregate would:

- Force loading all historical transactions for every portfolio operation.
- Make the aggregate grow without bound.
- Couple reporting queries to the transactional consistency boundary.

The trade-off (potential consistency gap between portfolio state and transaction log) is acceptable and manageable through the `@Transactional` boundary.

### 5.7 Transaction Modelling Verdict

The transaction model is now well-aligned with the rest of the domain's type-safe, behaviour-rich design. The sealed hierarchy eliminates the structural anemia that previously characterised this area of the model: nullable fields are gone, each subtype is self-documenting and self-validating, and Java 21 exhaustive pattern matching is used throughout the codebase.

The remaining design concern is transaction creation responsibility. The application service creates transactions manually after aggregate mutations, creating a consistency gap that domain events would close. This is an intentional simplification for the current scope — the `@Transactional` boundary provides atomicity, and the event infrastructure would add complexity without immediate pedagogical or production payoff. Domain events are the recommended next evolutionary step when the project's requirements justify the additional infrastructure.

---

## 6. Invariants and Consistency Analysis

### 6.1 Invariants Enforced Within the Aggregate

The following invariants are correctly enforced within the `Portfolio` aggregate boundary:

**Cash balance sufficiency:**
`Portfolio.buy()` computes `totalCost = price × quantity` and checks `balance.isLessThan(totalCost)` before proceeding. `Portfolio.withdraw()` performs the equivalent check. Both throw `InsufficientFundsException` on violation.

**Share availability:**
`Holding.sell()` checks `getTotalShares().isGreaterThanOrEqual(quantity)` before consuming lots. This correctly sums across all lots in the holding.

**FIFO lot consumption:**
`Holding.sell()` iterates lots in `ArrayList` insertion order, consuming the oldest lots first. This is correct under the assumption that lots are always appended in chronological order — which is guaranteed by `Holding.buy()` calling `lots.add()`.

**Lot integrity:**
`Lot.reduce()` checks that the reduction does not exceed remaining shares. `ShareQuantity` prevents negative values at the value object level.

**Holding uniqueness:**
`Portfolio.addHolding()` rejects duplicate tickers. `findOrCreateHolding()` uses `computeIfAbsent` to ensure at most one holding per ticker.

### 6.2 Invariants Not Enforced or Partially Enforced

**Transaction-portfolio consistency:**
As discussed in Section 5, there is no domain-level guarantee that the transaction log accurately reflects the portfolio's state history. This invariant is enforced by application service convention and the `@Transactional` boundary, not by the domain model itself.

**Balance non-negativity:**
The `Money` value object does not enforce non-negativity. The balance can theoretically go negative through incorrect usage (e.g., subtracting more than available). The aggregate root prevents this for buy and withdraw operations, but `Money.subtract()` itself does not throw on negative results. This is actually correct — `Money` should support negative values for representing losses and intermediate calculations — but it means the non-negativity invariant for portfolio balance depends entirely on the aggregate root's guards.

**Lot ordering stability:**
FIFO correctness depends on lots being ordered chronologically within each `Holding`. The domain now enforces this explicitly: `Holding.addLotFromPersistence()` inserts each lot in chronological order by `purchasedAt`, using an insertion sort. This means FIFO correctness does not silently depend on the persistence adapter's load order. The JPA adapter's `@OrderBy("purchasedAt ASC")` on `HoldingJpaEntity` provides the primary ordering from the database, and the domain-side insertion sort acts as a safety net. Both the adapter convention and the domain-side guarantee are covered by tests.

### 6.3 Cross-Aggregate Consistency

The main cross-aggregate consistency concern is between `Portfolio` and `Transaction`. The application services create both within a single database transaction, which provides atomicity under normal circumstances. However:

- If a new operation is added in the future and the developer forgets to create a corresponding transaction, the log will silently diverge.
- The `HoldingPerformanceCalculator` computes performance from transactions, while the portfolio's holdings track positions independently. These two sources of truth can drift if either is modified without the other.

A domain event–based design (where the aggregate emits events and transactions are derived from those events) would eliminate this risk, but at the cost of additional infrastructure complexity.

### 6.4 Invariant Enforcement Verdict

The model's invariant enforcement is strong within the aggregate boundary. The main gap is the transaction-portfolio consistency invariant, which depends on application-service discipline rather than domain-level guarantees. For the current scope and complexity, this is an acceptable trade-off.

---

## 7. Alternative Modelling Options

### 7.1 Option A: Current Model (Portfolio → Holding → Lot + External Transaction)

**What it is:** The current design. Portfolio is the aggregate root containing holdings and lots. Transactions are external entities created by application services.

**When it fits:** Personal investment applications with moderate data volumes, low concurrency, and straightforward FIFO accounting.

**Advantages:** Simple, intuitive, strong invariant enforcement within the aggregate, clean aggregate boundary, reasonable persistence mapping.

**Disadvantages:** Transaction creation is an application-service responsibility, not a domain concern. Potential for portfolio-transaction consistency drift. HoldingPerformanceCalculator creates a parallel data path.

**DDD quality:** Good. The aggregate is genuinely rich. The main weakness is the hand-off of transaction creation to the application layer.

**Assessment:** Appropriate for the stated problem scope. Not over-engineered.

### 7.2 Option B: Domain Event–Sourced Transactions

**What it is:** The aggregate's mutating operations (buy, sell, deposit, withdraw) produce domain events (`StockPurchased`, `StockSold`, `FundsDeposited`, `FundsWithdrawn`). An event handler converts these events into persistent transaction records.

**When it fits:** Systems where auditability, event replay, or CQRS are important.

**Advantages:**
- The domain guarantees that every state change produces an event, eliminating the risk of forgotten transaction creation.
- Events carry the exact data produced by the domain operation, eliminating the need for the application service to transcribe values.
- Events can drive other side effects (notifications, projections) without coupling the aggregate to those concerns.

**Disadvantages:**
- Requires event infrastructure (event bus, event store, or at minimum a domain event collector pattern).
- Increases architectural complexity.
- Persistence of events requires careful ordering guarantees.

**DDD quality:** Excellent. This is the textbook approach for systems where auditability matters.

**Assessment:** Justified for a production investment application. May be over-engineering for an introductory DDD teaching project, but could be introduced as a progressive enhancement.

### 7.3 Option C: Ledger-First Design

**What it is:** The transaction ledger is the primary data structure. Portfolio positions (holdings, lots, cash balance) are derived projections computed from the ledger, not independently maintained state.

**When it fits:** Accounting systems, double-entry bookkeeping systems, and applications where the ledger is the source of truth.

**Advantages:**
- By construction, ledger and positions can never diverge — positions are always derived from the ledger.
- Natural fit for double-entry accounting, regulatory reporting, and audit trails.
- Supports replay, correction, and retroactive adjustments naturally.

**Disadvantages:**
- Deriving current positions on every operation is expensive unless materialised views or caches are maintained.
- The FIFO lot-consumption algorithm becomes more complex when lots are derived from the ledger rather than maintained as explicit state.
- Breaks the rich aggregate model — there is no `Portfolio` aggregate that holds invariant-protected state; invariants are enforced at write time against derived state.
- Much harder to reason about for DDD beginners.

**DDD quality:** Mixed. It is authentic accounting-domain modelling but does not benefit from aggregate-root-based invariant encapsulation in the traditional DDD sense. Better suited to CQRS/ES architectures.

**Assessment:** Over-engineering for a personal investment portfolio application. Appropriate for institutional finance or accounting systems. Not recommended for a DDD teaching codebase — it would obscure the aggregate root pattern that the project aims to teach.

### 7.4 Option D: Position-Based Model (No Explicit Lots)

**What it is:** Instead of tracking individual lots, a Holding tracks only the aggregate position: total shares held and average cost per share. FIFO lot tracking is either eliminated or moved to a separate accounting subsystem.

**Advantages:**
- Dramatically simpler model. No lot entity, no FIFO algorithm.
- Sufficient for many personal finance applications that use average-cost accounting.
- Faster persistence and smaller aggregate.

**Disadvantages:**
- Cannot implement FIFO, LIFO, or specific-identification accounting.
- Cannot compute lot-level realised gains.
- Loses important financial information.

**DDD quality:** Acceptable, but semantically impoverished for this particular domain.

**Assessment:** Too simple for HexaStock's stated goals. FIFO lot tracking is a core domain requirement and an excellent teaching vehicle for rich domain behaviour. Removing it would weaken both the domain model and the educational value.

### 7.5 Option E: Holding as Projection, Lot as First-Class Entity

**What it is:** Lots are the primary domain entities. A Holding is not an explicit entity but a projection — a computed view over all lots with the same ticker within a portfolio.

**Advantages:**
- Eliminates the `HoldingId` entity, simplifying the model.
- Lots are the true units of ownership in an investment portfolio; elevating them makes the model more domain-authentic.

**Disadvantages:**
- The FIFO algorithm needs access to all lots for a ticker, which currently comes naturally from `Holding.lots`. Without Holding as an explicit container, the aggregate root must group and iterate lots by ticker — feasible but less encapsulated.
- Loses the clean `Map<Ticker, Holding>` structure that provides O(1) lookup.

**DDD quality:** Arguably more domain-authentic, but at the cost of encapsulation. The current design's use of Holding as an intermediate entity is a modelling judgement that trades domain purity for structural clarity.

**Assessment:** Not clearly superior to the current model. The current Holding entity is a useful structural abstraction even if it is not strictly necessary from a domain perspective.

### 7.6 Recommended Alternative for Production Enhancement

For a production evolution of this model, the most impactful change would be **Option B (domain events)** combined with a **sealed transaction hierarchy**:

- The `Portfolio` aggregate emits domain events for each mutation.
- An event handler converts events into strongly typed, immutable transaction records.
- Transactions use a sealed interface with type-specific subtypes.
- The application service no longer manually creates transactions.

This preserves the strengths of the current aggregate design while addressing its main weakness (transaction-portfolio consistency).

---

## 8. Terminology and Ubiquitous Language Review

### 8.1 Terms That Are Well-Chosen

| Term | Assessment |
|---|---|
| `Portfolio` | Correct and universally understood. The standard term for a collection of investments. |
| `Holding` | Correct. Universally used in brokerage and finance to mean "ownership of a specific security." Some systems use "Position" instead, but both are valid. |
| `Lot` | Correct and precise. The standard accounting term for a specific purchase of shares at a specific price on a specific date. Also called "tax lot" in US contexts. |
| `Ticker` | Correct. The standard short identifier for a publicly traded security. |
| `Money` | Correct. A well-established value object in DDD. |
| `Price` | Correct. Distinguishing price-per-share from total monetary amounts is important and well done here. |
| `ShareQuantity` | Good. More precise than "quantity" alone. Some systems use "Shares" or "Units," but `ShareQuantity` is unambiguous. |
| `SellResult` | Good. Clear, descriptive value object name. An alternative would be `SaleOutcome` or `TradeResult`. |
| `FIFO` | Not a named type in the code, but the selling algorithm implements it. This is correct — the accounting method is a domain concept that should be part of the ubiquitous language even if it is not a class. |

### 8.2 Terms That Deserve Scrutiny

**`Transaction`:**
This term is overloaded. In software engineering, "transaction" typically means a database transaction. In finance, "transaction" means a discrete financial event. In this codebase, `Transaction` is used in the financial sense, which is correct for the domain — but the potential for confusion with database transactions (especially since the services use `@Transactional`) is real. The term is acceptable but should be used carefully in documentation.

Some financial systems distinguish between:
- **Trade / Execution:** The act of buying or selling.
- **Transaction:** Any financial event, including non-trade events (deposits, withdrawals, fees, dividends).
- **Ledger entry:** A record in an accounting ledger.

The current model uses "Transaction" to cover all of these, which is a simplification but not an error.

**`TransactionType` enum values:**
- `DEPOSIT` and `WITHDRAWAL` — correct.
- `PURCHASE` — acceptable but unusual. The more common brokerage term is `BUY`. "Purchase" implies a completed acquisition, which is what this represents, but `BUY` is more idiomatic in investment domain language.
- `SALE` — acceptable. The more common pair would be `BUY` / `SELL`. Using `PURCHASE` / `SALE` instead of `BUY` / `SELL` is a style choice that works but does not match how most brokerage systems or investors speak.

**`ownerName`:**
A string for the portfolio owner. In a real multi-user system, this would be a `UserId` or `OwnerId` value object referencing an identity in the identity management bounded context. For a single-user teaching application, a string is adequate but should be recognised as a simplification.

**`balance`:**
Clear in context but could be more precise. In finance, "balance" can mean various things (account balance, ledger balance, available balance, settled balance). `cashBalance` or `availableCash` would be more unambiguous. The Javadoc says "cash balance available for investment or withdrawal," which clarifies intent, but the field name alone is slightly vague.

### 8.3 Missing Domain Terms

Several domain concepts are absent from the ubiquitous language:

- **Cost basis:** Present in `SellResult` and computed in `Lot.calculateCostBasis()`, but not a named domain type. A `CostBasis` value object would make the concept more explicit.
- **Unrealised gain / Realised gain:** Computed in `Holding.getUnrealizedGain()` and `HoldingPerformanceCalculator`, but not named types in the domain.
- **Position:** Not used. "Position" is a widely understood finance term that overlaps with "Holding." Its absence is fine since "Holding" serves the same purpose, but it should be noted in the ubiquitous language glossary.
- **Order:** Not modelled. In real brokerage systems, there is a distinction between an order (intent to buy/sell) and an execution (the completed trade). The current model skips the order concept, jumping directly from intent to execution. This is appropriate for the domain scope.
- **Fee / Commission / Tax:** Not modelled. Acceptable simplification for a teaching project, but a significant gap for a production system.

### 8.4 Terminology Verdict

The ubiquitous language is well-chosen and internally consistent. The main recommendation is to consider renaming `PURCHASE` / `SALE` to `BUY` / `SELL` for closer alignment with standard investment domain language, and to rename `balance` to `cashBalance` for precision.

---

## 9. Tactical DDD Assessment

### 9.1 Entities vs Value Objects

The model makes correct entity/value object distinctions with one exception:

| Concept | Classification | Correct? |
|---|---|---|
| `Portfolio` | Entity (aggregate root) | Yes |
| `Holding` | Entity | Yes — has identity within the aggregate |
| `Lot` | Entity | Yes — has identity and mutable state (`remainingShares`) |
| `Transaction` | Sealed interface (entity) | Yes — immutable ledger entry with `TransactionId`. Correctly documented as such. |
| `DepositTransaction` | Record (entity subtype) | Yes — sealed subtype of `Transaction`, self-validating |
| `WithdrawalTransaction` | Record (entity subtype) | Yes — sealed subtype of `Transaction`, self-validating |
| `PurchaseTransaction` | Record (entity subtype) | Yes — sealed subtype of `Transaction`, self-validating |
| `SaleTransaction` | Record (entity subtype) | Yes — sealed subtype of `Transaction`, self-validating |
| `Money` | Value Object (record) | Yes |
| `Price` | Value Object (record) | Yes |
| `ShareQuantity` | Value Object (record) | Yes |
| `Ticker` | Value Object (record) | Yes |
| `SellResult` | Value Object (record) | Yes |
| `StockPrice` | Value Object (record) | Yes |
| `HoldingPerformance` | Value Object (record) | Yes |
| `PortfolioId`, `HoldingId`, `LotId`, `TransactionId` | Value Objects (identity wrappers) | Yes |

The use of Java records for value objects is excellent — it guarantees immutability, provides `equals`/`hashCode` automatically, and makes the intent clear.

### 9.2 Aggregate Root Responsibilities

`Portfolio` correctly acts as the sole entry point for all mutations within the aggregate. No external code directly modifies a `Holding` or `Lot`; all operations flow through `Portfolio.buy()`, `Portfolio.sell()`, `Portfolio.deposit()`, and `Portfolio.withdraw()`.

The one exception is `Holding.addLotFromPersistence()`, which is explicitly documented as a persistence-only hook. This is a pragmatic concession to JPA mapping requirements. It is not ideal — it exposes a public method that should not be used by application code — but it is a recognised pattern in DDD implementations that must coexist with ORM frameworks. An alternative would be a package-private method or a dedicated reconstitution factory, but the current approach with clear documentation is acceptable.

### 9.3 Encapsulation of Invariants

Invariant encapsulation is strong:

- `Portfolio.buy()` enforces cash sufficiency before delegating to `Holding.buy()`.
- `Holding.sell()` enforces share availability before consuming lots.
- `Lot.reduce()` enforces non-negative remaining shares.
- `Portfolio.getHoldings()` returns `List.copyOf()`, preventing external mutation of the holdings collection.
- `Holding.getLots()` returns `List.copyOf()`, preventing external mutation of the lots collection.

The defensive copying pattern is consistently applied, which is a sign of careful design.

### 9.4 Identity Choices

All identity types (`PortfolioId`, `HoldingId`, `LotId`, `TransactionId`) use UUID-based string identifiers generated at creation time. This is a standard and appropriate choice:

- UUIDs are globally unique without coordination.
- String representation simplifies persistence and API serialisation.
- Wrapping in dedicated types prevents `String` parameter confusion (e.g., accidentally passing a `PortfolioId` where a `HoldingId` is expected).

One minor observation: `HoldingId` and `LotId` are used within the aggregate but are never exposed through the aggregate's public API (except via `getHoldings()` and `getLots()`). They serve purely as persistence identity and internal equality — a correct use of identity within an aggregate boundary.

### 9.5 Immutability Where Appropriate

Value objects are immutable (enforced by `record`). This is correct.

Entities are mutable where domain behaviour requires it:
- `Portfolio.balance` changes on buy/sell/deposit/withdraw.
- `Lot.remainingShares` changes on sell.
- `Holding.lots` (the list) changes on buy (add) and sell (remove empty lots).

These mutations are well-contained within the aggregate and accessed only through domain operations.

### 9.6 Domain Services

`HoldingPerformanceCalculator` is a domain service that computes per-ticker performance from a portfolio, its transactions, and current market prices. As a domain service, it is correctly stateless and operates on domain objects.

However, there is a design tension: the calculator takes a `Portfolio` (for remaining shares and unrealised gain) and a `List<Transaction>` (for purchased quantities and realised gain), and it must reconcile data from both sources. This means:

- The portfolio knows the current state (holdings, lots).
- The transactions know the historical events (PURCHASE quantities, SALE profits).
- The calculator must ensure these two views are consistent.

A cleaner approach might have the calculator operate entirely on one source (either the aggregate or the transaction log), avoiding the need to reconcile two potentially divergent data sources. But for the current domain scope, the hybrid approach is workable.

### 9.7 Repository Implications

The aggregate design implies the following repository contract:

- `PortfolioPort` must load and save the entire `Portfolio` aggregate (including all holdings and lots) atomically.
- `TransactionPort` must save and query transactions by portfolio.

This is a clean and appropriate repository contract. The aggregate is loaded and saved as a unit, which is the fundamental DDD repository pattern. The JPA adapter must handle the nested entity mapping (Portfolio ↔ Holdings ↔ Lots), which is standard Hibernate territory.

### 9.8 Tactical DDD Verdict

The model exhibits strong tactical DDD discipline: rich behaviour in the aggregate, immutable value objects, proper identity management, defensive copying, and clean repository boundaries. The `Transaction` sealed hierarchy extends this discipline with type-safe, self-validating subtypes that carry only the fields meaningful to each transaction kind.

---

## 10. Financial and Accounting Realism

### 10.1 FIFO Implementation

The FIFO implementation in `Holding.sell()` is correct and non-trivial:

- Lots are iterated in insertion order (oldest first).
- Each lot contributes up to its remaining shares.
- Partially consumed lots retain their remaining balance.
- Fully consumed lots are removed from the list.
- Cost basis is accumulated from each lot's unit price.

This is a faithful implementation of FIFO accounting for tax lot tracking. It handles partial sales correctly and produces accurate cost-basis figures.

**What is missing for production realism:**

- No support for alternative accounting methods (average cost, LIFO, specific lot identification). FIFO is hard-coded. A production system should support multiple methods, either through a strategy pattern or as a configuration choice.
- No provision for wash sale rules (US tax regulation that disallows loss deduction if substantially identical stock is repurchased within 30 days). This is a significant omission for US investors but a reasonable scope exclusion for a teaching project.
- No concept of settlement dates. Real brokerage transactions settle T+1 or T+2 after the trade date. Lots are not available for sale until they settle. The current model treats all buys as immediately available.

### 10.2 Realised vs Unrealised Gains

The model correctly distinguishes between:

- **Realised gain:** Computed at sell time as `proceeds - costBasis` in `SellResult`. This is accurate.
- **Unrealised gain:** Computed in `Holding.getUnrealizedGain()` as `currentMarketValue - purchaseCost` for remaining shares. This is also accurate.

Both computations are based on lot-level data, which is the correct approach for FIFO accounting.

### 10.3 Cash Management

Cash management is simple but correct:

- Deposits add to balance.
- Withdrawals subtract from balance (with sufficiency check).
- Purchases subtract from balance.
- Sales add proceeds to balance.

**What is missing:**

- No concept of pending or reserved cash (e.g., unsettled trades that have reduced available cash but not settled balance).
- No concept of margin or leverage — all purchases must be fully funded from cash. This is appropriate for a personal investment (non-margin) account.
- No overdraft or credit facility.

### 10.4 Currency

The model assumes single-currency (USD) throughout. This is explicitly documented in `Money` and `Price`. For a personal investment portfolio focused on US equities, this is reasonable. Multi-currency support would require:

- A `Currency` attribute on `Money` and `Price`.
- Exchange rate lookups.
- Currency conversion during reporting.
- Careful handling of foreign exchange gains/losses.

This would significantly increase model complexity and is a justified omission.

### 10.5 Precision and Rounding

The model uses `BigDecimal` with scale 2 and `HALF_UP` rounding throughout. This is appropriate for dollar-and-cents accounting.

**Areas of concern:**

- Share quantities are `int` (`ShareQuantity(int value)`). This means the model cannot represent fractional shares — a limitation that matters for modern brokerage platforms that support fractional share trading. For a teaching project, integer shares are simpler and adequate. For production, `BigDecimal` share quantities might be needed.
- Price is scale 2. Real stock prices often have more than 2 decimal places (US equities trade in sub-penny increments). Scale 2 is a simplification that could cause rounding errors in precise cost-basis calculations.

### 10.6 Fees, Taxes, and Corporate Actions

None of these are modelled:

- **Trading fees / commissions:** Not present. A buy operation's total cost is `price × quantity` with no fee component. Real systems charge per-trade or per-share commissions (though many modern brokers have eliminated these for retail).
- **Taxes:** No tax withholding, no capital gains tax computation, no tax lot optimisation.
- **Dividends:** Not modelled. A real portfolio receives dividends, which must be recorded, optionally reinvested, and reported.
- **Corporate actions:** Stock splits, mergers, spin-offs, and reverse splits all modify lot quantities and prices. None are modelled.

These omissions are all **acceptable for a teaching project** and **would need to be addressed for a production system**. The model's structure (Portfolio → Holding → Lot) would accommodate fees and dividends without fundamental restructuring, but corporate actions (especially stock splits that modify lot quantities and prices retroactively) would require more careful design.

### 10.7 Financial Realism Verdict

The model is financially credible for its stated scope: a simplified personal investment portfolio with FIFO accounting, single currency, no fees, and no corporate actions. The FIFO implementation is correct and the gain calculations are accurate. The main gaps (fractional shares, fees, corporate actions, tax lot methods) are documented or implicit scope exclusions that do not invalidate the model for its purpose.

---

## 11. Hexagonal Architecture Assessment

The preceding sections evaluate HexaStock through a DDD lens — aggregate design, invariant enforcement, ubiquitous language, and tactical patterns. This section evaluates whether the project also implements Hexagonal Architecture (ports and adapters) correctly and honestly, or merely borrows its vocabulary. The assessment is based on the actual source code, Maven module structure, dependency declarations, and runtime wiring — not on package names alone.

### 11.1 Is the Architecture Truly Hexagonal?

**Yes, in substance — not only in naming.**

The project is structured as a six-module Maven reactor:

| Module | Hexagonal Role | Maven Artifact |
|---|---|---|
| `domain` | Domain core — entities, value objects, aggregate, domain services, domain exceptions | `hexastock-domain` |
| `application` | Application core — use-case interfaces (inbound ports), outbound port interfaces, application services | `hexastock-application` |
| `adapters-inbound-rest` | Driving adapter — REST controllers, DTOs, exception handler | `hexastock-adapters-inbound-rest` |
| `adapters-outbound-persistence-jpa` | Driven adapter — JPA entities, mappers, Spring Data repositories | `hexastock-adapters-outbound-persistence-jpa` |
| `adapters-outbound-market` | Driven adapter — external stock price API integration (Finnhub, AlphaVantage, mock) | `hexastock-adapters-outbound-market` |
| `bootstrap` | Composition root — Spring Boot application, Spring configuration, integration tests | `hexastock-bootstrap` |

This structure directly reflects the ports-and-adapters model: the domain and application form the inner hexagon; inbound and outbound adapters form the outer hexagon; and the bootstrap module is the composition root that wires them together. The fact that these are separate Maven modules — not mere packages within a monolith — means that dependency violations produce **compile-time errors**, not runtime surprises. This is meaningfully stronger than a single-module project that relies on developer discipline alone for layer separation.

The domain module's `pom.xml` declares **zero external dependencies** — no Spring, no JPA, no Jakarta, no Jackson, no test frameworks. This is the purest possible domain layer: it compiles against the Java standard library and nothing else.

The application module's `pom.xml` depends on `hexastock-domain` and `jakarta.transaction-api` (for the `@Transactional` annotation). The application layer has no Spring dependency whatsoever — it uses the standard Jakarta Transactions `@Transactional` annotation (`jakarta.transaction.Transactional`) rather than Spring's proprietary version. At runtime, Spring's transaction infrastructure in the bootstrap layer transparently honours the Jakarta annotation through its `JtaTransactionAnnotationParser`, so transactional behaviour is preserved without any compile-time coupling to Spring in the application module.

No adapter module depends on another adapter module. The inbound REST adapter, the outbound JPA adapter, and the outbound market adapter are completely independent. Each depends only on `hexastock-application` (which transitively includes `hexastock-domain`). This means:

- The REST adapter can be replaced with a CLI, gRPC, or messaging adapter without touching persistence or market integration.
- The JPA persistence adapter can be swapped for an in-memory, MongoDB, or event-store adapter without touching REST or market integration.
- The market data adapter already demonstrates this substitutability: three implementations exist (`FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`, `MockFinhubStockPriceAdapter`), selectable at runtime via Spring profiles.

The `bootstrap` module depends on all other modules and acts as the composition root. `SpringAppConfig` manually instantiates application services (use-case implementations), wiring them with output port implementations that Spring discovers through component scanning and profile selection. This explicit wiring makes the dependency graph visible and auditable.

**Verification guard:** The `HexagonalArchitectureTest` class in the bootstrap module uses ArchUnit to enforce seven structural rules at every build:

1. Domain must not depend on application.
2. Domain must not depend on adapters.
3. Domain must not depend on Spring.
4. Application must not depend on adapters.
5. Application must not depend on Spring.
6. Inbound adapters must not depend on outbound adapters.
7. Outbound adapters must not depend on inbound adapters.

These are not aspirational guidelines — they are executable architecture fitness tests that fail the build if violated. This is a mature architectural practice that many production codebases lack.

**Assessment:** The architecture is genuinely hexagonal. The module structure, dependency declarations, compilation isolation, composition root wiring, and automated fitness tests all converge on an honest implementation of ports and adapters. This is not a project that merely names its packages `port.in` and `port.out` while allowing arbitrary cross-layer imports.

### 11.2 Dependency Direction

**Rule:** In hexagonal architecture, all compile-time dependencies must point inward — from adapters toward the application core, and from the application core toward the domain. The domain depends on nothing outside itself.

**Verification:**

| Source → Target | Expected | Actual | Status |
|---|---|---|---|
| `domain` → `application` | Forbidden | Not present | ✅ |
| `domain` → any adapter | Forbidden | Not present | ✅ |
| `domain` → Spring/Jakarta | Forbidden | Not present | ✅ |
| `application` → `domain` | Required | Present (via `hexastock-domain` dependency) | ✅ |
| `application` → any adapter | Forbidden | Not present | ✅ |
| `application` → Spring | Forbidden | Not present (uses `jakarta.transaction-api` for `@Transactional`) | ✅ |
| `adapters-inbound-rest` → `application` | Required | Present (via `hexastock-application` dependency) | ✅ |
| `adapters-outbound-persistence-jpa` → `application` | Required | Present (via `hexastock-application` dependency) | ✅ |
| `adapters-outbound-market` → `application` | Required | Present (via `hexastock-application` dependency) | ✅ |
| Any adapter → any other adapter | Forbidden | Not present | ✅ |
| `bootstrap` → all modules | Required (composition root) | Present | ✅ |

**Specific observations:**

- The domain module is **completely framework-free**. No class in `cat.gencat.agaur.hexastock.model.*` imports anything from `org.springframework`, `jakarta.persistence`, `com.fasterxml.jackson`, or any framework. This is verified by the ArchUnit rule `domainDoesNotDependOnSpring()` and confirmed by examining every import statement in all 26 domain Java files.

- The application module's services (`PortfolioManagementService`, `PortfolioStockOperationsService`, `ReportingService`, `TransactionService`) are annotated with `@Transactional` from `jakarta.transaction` — the standard Jakarta Transactions annotation. They are **not** annotated with `@Service` or `@Component` — Spring does not discover them through component scanning. Instead, `SpringAppConfig` in the bootstrap module explicitly constructs them via `@Bean` factory methods. This is an unusually disciplined approach: the application services have zero Spring imports. Spring's transaction infrastructure recognises the Jakarta annotation at runtime, so transactional behaviour is preserved.

- The inbound REST adapter (`PortfolioRestController`) injects use-case interfaces (`PortfolioManagementUseCase`, `PortfolioStockOperationsUseCase`, `ReportingUseCase`, `TransactionUseCase`) — never concrete service classes. This is textbook inbound port usage.

- The outbound persistence adapter (`JpaPortfolioRepository`, `JpaTransactionRepository`) implements outbound port interfaces (`PortfolioPort`, `TransactionPort`) defined in the application layer. The adapter is annotated with `@Component` and `@Profile("jpa")`, making it profile-selectable. It delegates to Spring Data `JpaRepository` interfaces and uses dedicated mapper classes for domain ↔ JPA entity conversion.

- The outbound market adapter (`FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`, `MockFinhubStockPriceAdapter`) implements the `StockPriceProviderPort` interface, each activated by a distinct Spring profile (`finhub`, `alphaVantage`, `mockfinhub`).

**One subtle observation about domain "awareness" of persistence:**

The `Portfolio` class has a `protected Portfolio() {}` no-argument constructor, and the `Holding` class likewise has `protected Holding() {}`. These exist to support JPA reconstitution (Hibernate requires a no-arg constructor on entities it manages). However, the domain does not actually reference JPA — these constructors are plain Java. The JPA adapter's mapper classes (`PortfolioMapper`, `HoldingMapper`) do not use these constructors; they use the public constructors with full arguments. Instead, the no-arg constructors appear to be a vestigial concession, likely added "just in case" or for a previous persistence strategy. They do not break dependency direction but they are a scent: why would a pure domain object need a no-arg constructor if nothing in its own module uses it?

Similarly, `Holding.addLotFromPersistence(Lot lot)` is a public method documented as "not a business operation — do not call from application services," existing solely for aggregate reconstitution from storage. Its name reveals persistence awareness in the domain vocabulary. A cleaner alternative would be a package-private factory or reconstruction method visible only within the domain, or passing lots through the constructor. This is a minor blemish, not a structural violation.

**Dependency direction verdict:** The dependency direction is correct at every layer. The domain is pristine. The application layer is free of Spring dependencies — transactional demarcation uses the standard Jakarta `@Transactional` annotation, and Spring’s transaction infrastructure recognises it at runtime. No adapter-to-adapter coupling exists. The project enforces these rules both at the Maven dependency level and through ArchUnit tests.

### 11.3 Port Design Quality

Hexagonal architecture defines two kinds of ports:

- **Inbound (driving) ports:** interfaces that the outside world uses to invoke the application. In HexaStock, these are named `*UseCase` and live in `application.port.in`.
- **Outbound (driven) ports:** interfaces that the application uses to reach external systems. In HexaStock, these are named `*Port` and live in `application.port.out`.

#### 11.3.1 Inbound Ports

| Port | Methods | Assessment |
|---|---|---|
| `PortfolioManagementUseCase` | `createPortfolio`, `getPortfolio`, `getAllPortfolios`, `deposit`, `withdraw` | Well-scoped: cohesive set of portfolio lifecycle and cash management operations. Named in domain language, not technical language. |
| `PortfolioStockOperationsUseCase` | `buyStock`, `sellStock` | Excellent. Two methods, each representing a genuine domain use case. Clean separation from portfolio management. |
| `ReportingUseCase` | `getHoldingsPerformance` | Clean single-responsibility port for read-side performance queries. |
| `TransactionUseCase` | `getTransactions` | Minimal read port for transaction history. |
| `GetStockPriceUseCase` | `getPrice` | Clean single-method port for live price lookup. |

**Strengths:**
- The ports are named using **domain language** (`buyStock`, `sellStock`, `deposit`, `withdraw`), not technical language (`executeTransaction`, `processRequest`).
- The ports use **domain types** in their signatures (`PortfolioId`, `Ticker`, `ShareQuantity`, `Money`, `SellResult`, `HoldingPerformance`), not primitives or DTOs.
- The responsibility split between `PortfolioManagementUseCase` (lifecycle + cash) and `PortfolioStockOperationsUseCase` (trading) reflects natural domain boundaries and supports cohesion: a UI that only needs portfolio listing does not need to know about trading operations.
- `ReportingUseCase` is separated from the write-side use cases, which is a step toward CQRS and a clean read/write boundary.

**Weaknesses:**
- `PortfolioManagementUseCase` groups `createPortfolio`/`getPortfolio`/`getAllPortfolios` (lifecycle queries) with `deposit`/`withdraw` (financial mutations) in a single interface. This violates Interface Segregation: a consumer that only lists portfolios is forced to see deposit and withdraw methods. A production design might split this into `PortfolioLifecycleUseCase` and `CashManagementUseCase`.
- `TransactionUseCase.getTransactions(String portfolioId, Optional<String> type)` accepts raw `String` parameters where domain types (`PortfolioId`, `TransactionType`) would be more appropriate and type-safe. This inconsistency is notable because all other inbound ports correctly use domain value objects. The `Optional<String> type` parameter for filtering is especially weak: it pushes parsing logic into the service rather than letting the port express the filtering intent through the type system.
- `GetStockPriceUseCase` is fine but sits slightly outside the portfolio domain — it exposes raw market data retrieval as a first-class use case. Whether stock price lookup belongs in the portfolio application layer or in a separate market-data bounded context is debatable, but for a single-bounded-context project this is acceptable.

**Port verbosity observation:** Five inbound ports for a relatively small application may seem fine-grained, but each port aligns with a distinct actor intent (manage portfolios, trade, report, view history, check prices). This granularity supports independent evolution and makes the system auditable: each entry point into the application has an explicit named contract.

#### 11.3.2 Outbound Ports

| Port | Methods | Assessment |
|---|---|---|
| `PortfolioPort` | `getPortfolioById`, `createPortfolio`, `savePortfolio`, `getAllPortfolios` | Repository port for the Portfolio aggregate. |
| `TransactionPort` | `getTransactionsByPortfolioId`, `save` | Repository port for Transaction entities. |
| `StockPriceProviderPort` | `fetchStockPrice(Ticker)`, `fetchStockPrice(Set<Ticker>)` | External market data port with single and batch fetch. |

**Strengths:**
- Ports are named in **domain language** (`PortfolioPort`, `TransactionPort`, `StockPriceProviderPort`), not persistence language (`PortfolioRepository`, `TransactionDAO`).
- Port methods use domain types (`Portfolio`, `PortfolioId`, `Transaction`, `Ticker`, `StockPrice`) — the adapter is responsible for translating these to/from its own representation (JPA entities, API responses).
- `StockPriceProviderPort` provides a default method `fetchStockPrice(Set<Ticker>)` that iterates over singles, allowing simple adapters to implement only the single-ticker method while sophisticated adapters can override with a batch implementation. This is a thoughtful API design.
- `PortfolioPort` methods are aggregate-granular: `savePortfolio` saves the entire aggregate (Portfolio + Holdings + Lots) atomically, which is the correct DDD repository pattern.

**Weaknesses:**
- `PortfolioPort` mixes commands and queries (`createPortfolio`/`savePortfolio` vs `getPortfolioById`/`getAllPortfolios`) in a single interface. In a CQRS-conscious design, read and write ports would be separate.
- The distinction between `createPortfolio` and `savePortfolio` is unclear from the port interface alone — both accept a `Portfolio` and return `void`. The semantics depend on the adapter implementation (insert vs update). An alternative would be a single `save` method, or the port could return the persisted entity to confirm success.
- `PortfolioPort` declares `PortfolioNotFoundException` in its import signature (visible in its Javadoc), but this exception is an application-layer concept (`application.exception.PortfolioNotFoundException`). An outbound port should ideally not reference application-layer exceptions — it should return `Optional<Portfolio>` and let the application service decide whether to throw. The current design partially does this (returns `Optional`) but the exception import creates a conceptual coupling.

**Overall port quality:** The ports are well-designed for this scope. They express domain contracts rather than technical APIs, they use domain types for type safety, and they provide clean seams for adapter substitution. The weaknesses are matters of granularity and interface segregation, not structural defects.

### 11.4 Adapter Quality

#### 11.4.1 Inbound Adapter: REST

The REST inbound adapter consists of two controllers (`PortfolioRestController`, `StockRestController`), a global exception handler (`ExceptionHandlingAdvice`), an error response class, and eleven DTO records.

**Thin and focused:** The controllers contain no business logic. Each endpoint method follows the same pattern:
1. Extract raw values from the HTTP request (path variables, request body DTOs).
2. Construct domain value objects (`PortfolioId.of(id)`, `Ticker.of(request.ticker())`, `ShareQuantity.positive(request.quantity())`).
3. Invoke a use-case port method.
4. Map the domain result to a response DTO.
5. Return an HTTP response.

This is exemplary adapter behaviour: the adapter is a **translator** between the HTTP protocol world and the domain/application world. No business decisions are made in the controller. For example, `buyStock()` does not check cash sufficiency — it delegates to the use case, which delegates to the aggregate, where the invariant lives.

**DTO mapping:** All eleven DTOs properly map between domain types and primitive representations. `CreatePortfolioResponseDTO.from(Portfolio)`, `PortfolioResponseDTO.from(Portfolio)`, `SaleResponseDTO.from(...)`, `StockPriceDTO.fromDomainModel(StockPrice)`, and `TransactionDTO.from(Transaction)` all destructure domain objects into primitive fields suitable for JSON serialisation.

`TransactionDTO` deserves specific mention: it is a flat record with primitive fields (`String id`, `String portfolioId`, `String type`, `String ticker`, `Integer quantity`, `BigDecimal unitPrice`, `BigDecimal totalAmount`, `BigDecimal profit`, `LocalDateTime createdAt`) and a `from(Transaction)` factory method that extracts values from the sealed interface's accessor methods. This follows the same anti-corruption mapping pattern as every other DTO in the REST adapter, keeping the API contract fully decoupled from the domain model. The sealed hierarchy's default accessor methods (`ticker()` returns `null`, `quantity()` returns `ZERO`, `unitPrice()` returns `null`, `profit()` returns `ZERO`) allow the DTO mapper to access these fields uniformly while nullable fields serialise naturally as JSON `null`.

**Exception handling:** `ExceptionHandlingAdvice` translates domain and application exceptions into appropriate HTTP status codes using `ProblemDetail` (RFC 7807). Each exception handler maps to a specific HTTP status:
- `PortfolioNotFoundException`, `HoldingNotFoundException` → 404
- `InvalidAmountException`, `InvalidQuantityException`, `InvalidTickerException` → 400
- `ConflictQuantityException`, `InsufficientFundsException` → 409
- `ExternalApiException` → 503

This is a well-designed exception-to-HTTP-status translation layer. The adapter knows about domain exceptions (which it must, to translate them), but it does not catch and reinterpret domain logic — it only maps exception types to HTTP semantics.

#### 11.4.2 Outbound Adapter: JPA Persistence

The JPA persistence adapter consists of four JPA entity classes (`PortfolioJpaEntity`, `HoldingJpaEntity`, `LotJpaEntity`, `TransactionJpaEntity`), four mapper classes (`PortfolioMapper`, `HoldingMapper`, `LotMapper`, `TransactionMapper`), and two Spring Data repository interfaces (`JpaPortfolioSpringDataRepository`, `JpaTransactionSpringDataRepository`) behind two adapter classes (`JpaPortfolioRepository`, `JpaTransactionRepository`) that implement the outbound ports.

**Anti-corruption layer:** The mapper classes provide a clean anti-corruption layer between the domain model and the JPA persistence model. The domain knows nothing about `PortfolioJpaEntity`; the JPA layer knows nothing about `Portfolio`'s internal behaviour. The mappers translate in both directions:

- `PortfolioMapper.toModelEntity(PortfolioJpaEntity)` reconstructs the domain `Portfolio` aggregate from JPA entities, recursively mapping holdings and lots via `HoldingMapper` and `LotMapper`. The reconstruction uses `Portfolio`'s public constructor and `portfolio.addHolding()`, which means the aggregate's own structural invariants (no duplicate tickers) are enforced during reconstitution.
- `PortfolioMapper.toJpaEntity(Portfolio)` flattens the domain aggregate into JPA entities, extracting primitive values from value objects (`entity.getId().value()`, `entity.getBalance().amount()`).

This bidirectional mapping is the correct hexagonal pattern: the adapter translates between the domain's language and the persistence technology's language.

**FIFO ordering preservation:** The `HoldingJpaEntity` annotates its `lots` collection with `@OrderBy("purchasedAt ASC")`, ensuring that lots are loaded from the database in chronological order. Additionally, `Holding.addLotFromPersistence()` performs an insertion sort by `purchasedAt`, guaranteeing chronological order at the domain level regardless of persistence load order. This dual-safety approach addresses the FIFO concern raised in Section 6.2: the adapter provides the primary ordering, and the domain enforces it as a safety net. Both layers are tested independently.

**Pessimistic locking:** `JpaPortfolioSpringDataRepository` defines a custom query `findByIdForUpdate` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. The `JpaPortfolioRepository.getPortfolioById()` method uses this lock-aware query, ensuring that concurrent modifications to the same portfolio are serialised at the database level. This is a critical correctness property for a financial application and is correctly placed in the adapter — the domain should not know about database locking strategies.

**Aggregate-granular persistence:** `JpaPortfolioRepository.savePortfolio()` maps the entire domain aggregate to a JPA entity graph and saves it as a unit. The `@OneToMany(cascade = ALL, orphanRemoval = true)` annotations on `PortfolioJpaEntity.holdings` and `HoldingJpaEntity.lots` ensure that Hibernate cascades saves and deletes through the entity hierarchy. This matches the DDD aggregate repository pattern: the aggregate is loaded and saved as a whole.

**Profile-based activation:** Both `JpaPortfolioRepository` and `JpaTransactionRepository` are annotated with `@Profile("jpa")`, meaning they are only active when the `jpa` Spring profile is enabled. This enables testing with alternative (e.g., in-memory) port implementations without any adapter code changes.

**No business logic in the adapter:** The JPA adapter contains no business rules, no validation logic, no domain computations. It maps, persists, and retrieves — exactly what an adapter should do.

#### 11.4.3 Outbound Adapter: Market Data

Three implementations of `StockPriceProviderPort` exist:

- `FinhubStockPriceAdapter` — calls the Finnhub REST API, parses JSON, constructs `StockPrice` domain objects. Annotated with `@Cacheable` using Caffeine cache and `@Profile("finhub")`.
- `AlphaVantageStockPriceAdapter` — calls the AlphaVantage REST API with a similar pattern. Uses `@Profile("alphaVantage")`.
- `MockFinhubStockPriceAdapter` — generates random prices using `SecureRandom`. Uses `@Profile("mockfinhub")`.

**Strengths:**
- Three implementations of the same port demonstrate **genuine substitutability** — the hexagonal promise that adapters can be swapped is not theoretical; it is exercised in practice.
- The mock adapter enables integration tests (`PortfolioTradingRestIntegrationTest` and others) to run without external API dependencies, which is exactly the testability benefit that hexagonal architecture promises.
- Both real adapters include throttling (`Thread.sleep(THROTTLE_MS)`) and caching (`@Cacheable`) — infrastructure concerns that belong in the adapter, not in the application core.
- Both real adapters throw `ExternalApiException` (a domain exception) when the external service returns invalid data, translating infrastructure failures into domain-language exceptions.

**Observation:** The fact that `ExternalApiException` is a domain exception (`cat.gencat.agaur.hexastock.model.ExternalApiException`) used by adapters to signal infrastructure failures creates a mild conceptual tension. The domain defines an exception type for infrastructure failures, even though the domain itself never encounters infrastructure. This is arguably backwards — the adapter should define its own exception, and the application or domain should define an abstract "external dependency failed" concept if needed. In practice, this is harmless because the exception flows outward (adapter → application → REST exception handler), but it reveals a minor layer-awareness concern.

#### 11.4.4 Adapter Quality Verdict

The adapters are well-constructed. They are thin, focused on translation, and free of business logic. The JPA adapter provides a textbook anti-corruption layer with dedicated JPA entities, bidirectional mappers, and aggregate-granular persistence. The market adapter demonstrates genuine adapter substitutability. The REST adapter is clean: all DTOs — including `TransactionDTO` — follow the same pattern of mapping domain objects to flat, primitive-valued records suitable for JSON serialisation, keeping the API contract fully decoupled from the domain model.

### 11.5 Application Layer Orchestration

The application layer contains five service classes, each implementing one inbound port. The services are responsible for use-case orchestration: loading domain objects through outbound ports, delegating business decisions to the domain, persisting results, and recording transactions.

**Orchestration vs business logic placement:**

| Service | Domain Logic Location | Application Logic | Assessment |
|---|---|---|---|
| `PortfolioManagementService.createPortfolio` | `Portfolio.create()` constructs the aggregate | Service calls `portfolioPort.createPortfolio()` | ✅ Correct: creation logic in domain, persistence in application |
| `PortfolioManagementService.deposit` | `Portfolio.deposit(amount)` validates and mutates balance | Service orchestrates load → mutate → save → record transaction | ✅ Correct: validation and mutation in domain, orchestration in application |
| `PortfolioManagementService.withdraw` | `Portfolio.withdraw(amount)` validates, checks sufficiency, mutates | Same orchestration pattern | ✅ Correct |
| `PortfolioStockOperationsService.buyStock` | `Portfolio.buy()` validates, checks funds, delegates to `Holding.buy()`, creates `Lot` | Service fetches price, orchestrates flow, records transaction | ✅ Correct: all investment logic in domain |
| `PortfolioStockOperationsService.sellStock` | `Portfolio.sell()` → `Holding.sell()` implements FIFO, computes cost basis, returns `SellResult` | Service fetches price, orchestrates flow, records transaction | ✅ Correct: FIFO algorithm entirely in domain |
| `ReportingService.getHoldingsPerformance` | `HoldingPerformanceCalculator` computes metrics | Service loads portfolio, transactions, fetches prices, delegates to domain service | ✅ Correct: computation in domain service, I/O orchestration in application |
| `TransactionService.getTransactions` | None | Delegates directly to `TransactionPort` | ✅ Correct (read-only pass-through) |
| `GetStockPriceService.getPrice` | None | Delegates directly to `StockPriceProviderPort` | ✅ Correct (read-only pass-through) |

**Balance assessment:** The application services are appropriately thin. They perform orchestration — load, delegate, save, record — without absorbing domain logic. The FIFO algorithm lives in `Holding.sell()`, not in `PortfolioStockOperationsService`. Cash sufficiency checks live in `Portfolio.buy()` and `Portfolio.withdraw()`, not in the services. `HoldingPerformanceCalculator` lives in the domain module, not in the application module. This distribution is correct.

**Transaction creation concern (cross-reference with DDD Section 5.4):** The application services are responsible for creating `Transaction` records after each domain operation. This creates an orchestration burden: the service must know which `Transaction.create*()` factory method to call and which domain results to pass. As analysed in the DDD sections, this creates a consistency risk (a future developer could forget to create a transaction for a new operation) and a knowledge-leakage concern (the service must understand the financial semantics of `SellResult` to record the transaction correctly).

From a hexagonal perspective, this concern manifests as **the application layer doing too much transcription work** — it must translate between the domain operation's output (`SellResult`) and the transaction record's input (`Transaction.createSale(..., sellResult.proceeds(), sellResult.profit())`). If the domain emitted events, the application service would be simpler: orchestrate the domain operation, save the aggregate, and let an event handler create the transaction from the domain event. The hexagonal boundary would be cleaner because the event handler (which is adapter-like infrastructure) would handle the transcription, not the application core.

**`@Transactional` placement:** All five services are annotated with `@Transactional` at the class level. This means every public method in each service runs within a Spring-managed database transaction. This is correct for the write-side services (portfolio mutations + transaction recording must be atomic), but slightly heavy-handed for the read-side services (`TransactionService.getTransactions`, `GetStockPriceService.getPrice`). Read-only operations do not need write-transaction overhead. A minor optimisation would be `@Transactional(readOnly = true)` for read-side methods or splitting read and write service concerns more explicitly.

### 11.6 Domain Protection from Infrastructure

A key hexagonal architecture goal is protecting the domain model from infrastructure distortion. The question is: does the domain model in HexaStock look like a financial domain model, or does it look like a persistence model dressed up in domain vocabulary?

**The domain looks like a domain model, not a persistence model.** Specific evidence:

1. **No JPA annotations anywhere in the domain module.** Not a single `@Entity`, `@Id`, `@Column`, `@OneToMany`, `@ManyToOne`, or `@Table` annotation exists in any domain class. The JPA entity hierarchy (`PortfolioJpaEntity`, `HoldingJpaEntity`, `LotJpaEntity`, `TransactionJpaEntity`) is entirely confined to the persistence adapter.

2. **No Jackson annotations.** No `@JsonProperty`, `@JsonIgnore`, or `@JsonCreator` annotations exist in the domain. Serialisation is the adapter's concern.

3. **No Spring annotations.** No `@Service`, `@Component`, `@Repository`, or `@Autowired` in the domain.

4. **Value objects are Java records.** `Money`, `Price`, `ShareQuantity`, `Ticker`, `SellResult`, `HoldingPerformance`, `StockPrice`, and all ID types are records — immutable, with auto-generated `equals`/`hashCode`/`toString`. Records cannot be JPA-managed entities (Hibernate requires mutable classes), which makes the value object / JPA entity distinction structural rather than convention-based.

5. **Collections are domain-natural.** `Portfolio` uses `Map<Ticker, Holding>` (indexed by ticker for O(1) lookup); `Holding` uses `List<Lot>` (ordered for FIFO). These collections serve domain purposes, not persistence mapping. The JPA adapter translates them: `Map<Ticker, Holding>` becomes `Set<HoldingJpaEntity>` with a `@JoinColumn`; `List<Lot>` becomes `List<LotJpaEntity>` with `@OrderBy`.

**Minor infrastructure awareness in the domain:**

1. **`protected` no-arg constructors.** `Portfolio`, `Holding`, and `Lot` (the three mutable entities) each have a `protected` no-arg constructor. These are a common JPA-ORM accommodation — Hibernate needs a no-arg constructor for proxied entities. However, in this architecture the domain entities are **not** Hibernate-managed; the JPA entities are separate classes in the adapter. The no-arg constructors are therefore vestigial: they existed in an earlier design (or were added prophylactically) and are currently unused. They do not break anything, but they represent a lingering persistence concern in the domain's API surface. Removing them would make the domain demonstrably independent of any ORM requirement.

2. **`Holding.addLotFromPersistence(Lot lot)`.** This public method is documented as a persistence hook, and its name explicitly references persistence. It exists so that `PortfolioMapper.toModelEntity()` can reconstruct the `Holding → Lot` relationship during aggregate reconstitution from storage. The method enforces the no-duplicate-lot-ID invariant internally, which is correct, but its naming and purpose reveal that the domain's API was shaped by a persistence need rather than a business need. A cleaner approach would be passing the lots through the `Holding` constructor or providing a domain-motivated method like `reconstitute(List<Lot> lots)` that does not name persistence.

3. **`HoldingPerformance` uses `BigDecimal` instead of domain value objects.** The `HoldingPerformance` record uses raw `BigDecimal` for all its fields (`quantity`, `remaining`, `averagePurchasePrice`, `currentPrice`, `unrealizedGain`, `realizedGain`) rather than the domain's `Money`, `Price`, and `ShareQuantity` types. This appears to be an accommodation for easy serialisation and DTO mapping — the REST adapter can map `BigDecimal` fields directly without destructuring value objects. While pragmatic, it means `HoldingPerformance` is a **leaky abstraction** within the domain: it breaks the type discipline that the rest of the domain enforces with `Money`, `Price`, and `ShareQuantity`.

**Domain protection verdict:** The domain is well-protected. The three observations above are minor concessions, not structural compromises. The absence of any framework annotation in the domain is the strongest evidence that infrastructure has not distorted the domain model. The domain looks like what a financial domain expert would design, not what a JPA-first developer would produce.

### 11.7 End-to-End Boundary Trace of Key Use Cases

To verify that hexagonal boundaries hold in practice, this section traces four representative use cases from HTTP entry to database persistence.

#### 11.7.1 Deposit Funds

```
HTTP POST /api/portfolios/{id}/deposits  { "amount": 500.00 }
```

1. **Inbound adapter** (`PortfolioRestController.deposit`):
   - Extracts `id` from path variable, `amount` from request body `DepositRequestDTO`.
   - Constructs domain value objects: `PortfolioId.of(id)`, `Money.of(request.amount())`.
   - Calls `portfolioManagementUseCase.deposit(portfolioId, amount)` — the inbound port.

2. **Application service** (`PortfolioManagementService.deposit`):
   - Calls `portfolioPort.getPortfolioById(portfolioId)` — outbound port for loading.
   - Calls `portfolio.deposit(amount)` — domain operation. `Portfolio.deposit()` validates that the amount is positive and adds it to the balance. No infrastructure involved.
   - Calls `portfolioPort.savePortfolio(portfolio)` — outbound port for persisting.
   - Calls `Transaction.createDeposit(portfolioId, amount)` then `transactionPort.save(transaction)` — outbound port for recording.

3. **Outbound persistence adapter** (`JpaPortfolioRepository`):
   - `getPortfolioById`: calls `jpaSpringDataRepository.findByIdForUpdate()` (pessimistic lock), maps result via `PortfolioMapper.toModelEntity()` → returns domain `Portfolio`.
   - `savePortfolio`: maps domain `Portfolio` via `PortfolioMapper.toJpaEntity()`, saves via `jpaSpringDataRepository.save()`.

**Boundary assessment:** ✅ Clean hexagonal flow. Control enters through the inbound port, business logic executes in the domain, persistence occurs through the outbound port. No layer skipping, no business logic in adapters, no infrastructure in the domain.

#### 11.7.2 Buy Stock

```
HTTP POST /api/portfolios/{id}/purchases  { "ticker": "AAPL", "quantity": 10 }
```

1. **Inbound adapter** (`PortfolioRestController.buyStock`):
   - Constructs `PortfolioId.of(id)`, `Ticker.of(request.ticker())`, `ShareQuantity.positive(request.quantity())`.
   - Calls `portfolioStockOperationsUseCase.buyStock(...)` — inbound port.

2. **Application service** (`PortfolioStockOperationsService.buyStock`):
   - Calls `portfolioPort.getPortfolioById(portfolioId)` — outbound port (persistence).
   - Calls `stockPriceProviderPort.fetchStockPrice(ticker)` — outbound port (market data).
   - Calls `portfolio.buy(ticker, quantity, stockPrice.price())` — domain operation. The aggregate validates quantity, checks cash sufficiency, finds or creates the holding, delegates to `Holding.buy()` which creates a `Lot`, and debits the balance.
   - Calls `portfolioPort.savePortfolio(portfolio)` then `transactionPort.save(transaction)` — outbound ports (persistence).

3. **Outbound market adapter** (e.g., `FinhubStockPriceAdapter.fetchStockPrice`):
   - Calls Finnhub REST API, parses JSON response, constructs `StockPrice(ticker, Price.of(price), time)` — domain value object.
   - Returns through the outbound port interface.

**Boundary assessment:** ✅ Clean. Two outbound adapters are invoked (market + persistence), but the application service orchestrates them through port interfaces. The aggregate root performs all business validation. Price discovery is an adapter concern; the domain receives a `Price` and is unaware of where it came from.

#### 11.7.3 Sell Stock (FIFO)

```
HTTP POST /api/portfolios/{id}/sales  { "ticker": "AAPL", "quantity": 5 }
```

1. **Inbound adapter** (`PortfolioRestController.sellStock`):
   - Constructs domain value objects, calls inbound port.
   - Receives `SellResult`, maps it to `SaleResponseDTO` using `SaleResponseDTO.from(...)`.

2. **Application service** (`PortfolioStockOperationsService.sellStock`):
   - Loads portfolio, fetches price, calls `portfolio.sell(ticker, quantity, price)`.
   - The aggregate delegates to `Holding.sell()`, which implements the FIFO algorithm: iterates lots in chronological order, reduces lot shares, accumulates cost basis, removes depleted lots, and computes `SellResult(proceeds, costBasis, profit)`.
   - Saves portfolio, records transaction with proceeds and profit from `SellResult`.

3. **Domain logic (FIFO):** Entirely within `Holding.sell()`:
   - Validates share availability against total remaining across all lots.
   - Iterates lots via `ArrayList` iterator (oldest first by insertion order).
   - For each lot: takes `min(lot.remaining, quantity.remaining)`, accumulates cost basis, reduces lot, removes if empty.
   - Returns `SellResult.of(proceeds, costBasis)` which computes profit as `proceeds - costBasis`.

**Boundary assessment:** ✅ Clean. The most complex business logic in the system (FIFO lot consumption with cost basis tracking) lives entirely in the domain aggregate. The application service is a thin orchestrator. The adapter merely translates HTTP to domain and back.

**Cross-reference with DDD:** The FIFO algorithm's correctness depends on lot ordering, which in turn depends on the persistence adapter's `@OrderBy("purchasedAt ASC")` annotation on `HoldingJpaEntity.lots`. This is an implicit contract between the domain and adapter — the domain assumes insertion-ordered lots, and the adapter guarantees chronological ordering from the database. This is an acceptable contract for this architecture, but it is not enforced by the domain itself. From a hexagonal perspective, this is a clean adapter responsibility: the adapter ensures that the data it provides through the outbound port meets the domain's expectations.

#### 11.7.4 Holdings Performance Report

```
HTTP GET /api/portfolios/{id}/holdings
```

1. **Inbound adapter** (`PortfolioRestController.getHoldings`):
   - Calls `reportingUseCase.getHoldingsPerformance(id)` — inbound port.
   - Maps `List<HoldingPerformance>` to `List<HoldingDTO>` — each `HoldingPerformance` record's `BigDecimal` fields are passed directly to `HoldingDTO`.

2. **Application service** (`ReportingService.getHoldingsPerformance`):
   - Loads portfolio via `portfolioPort` — outbound port.
   - Loads transactions via `transactionPort` — outbound port.
   - Determines distinct tickers from portfolio holdings.
   - Fetches live prices via `stockPriceProviderPort.fetchStockPrice(Set<Ticker>)` — outbound port.
   - Delegates to `HoldingPerformanceCalculator.getHoldingsPerformance(portfolio, transactions, tickerPrices)` — domain service.

3. **Domain service** (`HoldingPerformanceCalculator`):
   - Single-pass O(T) aggregation over transactions to compute per-ticker bought quantities, bought costs, and realised gains.
   - For each ticker: reads remaining shares and unrealised gain from the `Portfolio` aggregate.
   - Builds `HoldingPerformance` records.

**Boundary assessment:** ✅ Clean hexagonal flow. The application service orchestrates three outbound ports (load portfolio, load transactions, fetch prices) and delegates computation to a domain service. No business logic in the application service beyond orchestration.

**Cross-reference with DDD (Section 5.4, 9.6):** This use case highlights the "parallel truth" concern: `HoldingPerformanceCalculator` reads remaining shares from the aggregate (`portfolio.getHolding(ticker).getTotalShares()`) but reads total purchased quantities from the transaction history. If these two data sources diverge, the performance calculation will be inconsistent. From a hexagonal perspective, this is an orchestration concern: the application service is responsible for ensuring that both data sources are consistent, but it has no mechanism to verify this. A domain-event-based design would eliminate this concern because transactions would be derived from the same events that update the aggregate.

### 11.8 Testability Consequences

One of hexagonal architecture's primary promises is improved testability: the ability to test domain and application logic without infrastructure.

**Evidence that hexagonal structure improves testability:**

1. **Domain tests are pure unit tests.** The domain module contains only Java standard library code. Domain tests (in `domain/src/test/`) test aggregate behaviour, value object validation, and the FIFO algorithm without any Spring context, database, or external service. `Portfolio.buy()`, `Holding.sell()`, `Lot.reduce()`, `Money.add()` — all are testable with plain JUnit assertions.

2. **Application tests mock outbound ports.** `ReportingServiceTest` demonstrates the pattern: the three outbound ports (`PortfolioPort`, `TransactionPort`, `StockPriceProviderPort`) are mocked with Mockito, and the real `HoldingPerformanceCalculator` (a pure domain service) is used directly. The test verifies:
   - Correct exception on unknown portfolio ID.
   - Correct delegation to the domain calculator.
   - Correct port call sequence (portfolio first, then transactions, then prices).

   This is only possible because the application service depends on **port interfaces**, not concrete adapter classes. If `ReportingService` depended directly on `JpaPortfolioRepository` or `FinhubStockPriceAdapter`, mocking would require Spring context initialisation or Testcontainers — dramatically increasing test time and complexity.

3. **Integration tests are isolated by adapter profiles.** The bootstrap module's integration tests use `@Profile("mockfinhub")` to substitute the real Finnhub adapter with a mock that returns random prices. This enables full end-to-end HTTP testing (via RestAssured + Testcontainers for MySQL) without depending on external market data APIs. The `@Profile("jpa")` annotation on persistence adapters similarly enables future in-memory adapter substitution for faster test cycles.

4. **ArchUnit tests enforce architecture mechanically.** `HexagonalArchitectureTest` runs as a standard JUnit test, meaning architectural violations are caught during `mvn test`, not during code review or production debugging.

**Testability weaknesses:**

1. **No unit tests for REST controllers.** The inbound REST adapter is tested only through integration tests in the bootstrap module. Unit testing the controller in isolation (mocking the use-case ports) would verify DTO mapping, HTTP status codes, and request validation independently of the full Spring context. The hexagonal structure makes this easy — the controller depends on port interfaces — but the project does not exploit this opportunity.

2. **No unit tests for persistence mappers.** The bidirectional mapper classes (`PortfolioMapper`, `HoldingMapper`, `LotMapper`, `TransactionMapper`) are pure functions that convert between domain and JPA objects. They should be unit-testable without a database. Testing them would catch mapping errors (field mismatches, null handling, ordering changes) before integration tests.

3. **Application services are not annotated with `@Service`**, which means they are not Spring beans by default. This is architecturally correct (they are instantiated explicitly in `SpringAppConfig`), but it means that testing them requires manual construction rather than using `@MockBean` and `@Autowired`. The current test approach (manual construction in `@BeforeEach`) is actually cleaner and faster than Spring-managed testing, so this is a strength disguised as a minor inconvenience.

**Testability verdict:** The hexagonal structure delivers real testability benefits. Domain logic is testable in pure unit tests. Application logic is testable with mocked ports. Integration tests use adapter profile substitution to avoid external dependencies. The project could go further by adding controller unit tests and mapper unit tests, but the current test architecture is sound.

### 11.9 Hexagonal Risks and Architectural Smells

This section identifies concrete hexagonal risks, ordered by severity.

#### Severity: High

No high-severity hexagonal risks remain. The previously identified `TransactionDTO` domain leak (where `TransactionDTO(Transaction transaction)` exposed the domain entity directly in the REST API) has been resolved. `TransactionDTO` is now a flat record with primitive fields and a `from(Transaction)` factory method, following the same anti-corruption pattern as all other DTOs in the REST adapter.

#### Severity: Medium

**1. Application services create transactions manually — dual-write orchestration burden.**
This is both a DDD and hexagonal concern. The application service must correctly transcribe domain results into `Transaction` records. From a hexagonal perspective, this makes the application layer thicker than necessary: it must understand domain semantics (what fields a `SALE` transaction needs, where to get proceeds and profit from `SellResult`) rather than simply orchestrating port calls. A domain event pattern would push this concern to the adapter layer (event handler) and make the application service simpler.

**2. `ExternalApiException` is a domain exception used to signal adapter failures.**
`ExternalApiException` lives in `cat.gencat.agaur.hexastock.model` (the domain root package), but it represents an infrastructure failure (external API unavailable or returning invalid data). Domain exceptions should model **business rule violations** (insufficient funds, invalid quantity), not infrastructure failures. The adapters (`FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`) throw this exception.

**Remediation:** Move `ExternalApiException` to the application layer or create an adapter-specific exception that the application layer translates into an application-level error.

**3. `HoldingPerformance` uses `BigDecimal` instead of domain value objects.**
As noted in Section 11.6, this record breaks the domain's type discipline. It uses `BigDecimal` for quantities, prices, and monetary amounts where the rest of the domain uses `ShareQuantity`, `Price`, and `Money`. This appears to be an optimisation for DTO mapping but it weakens type safety within the domain itself.

#### Severity: Low

**4. `protected` no-arg constructors in domain entities.**
The no-arg constructors on `Portfolio`, `Holding`, and `Lot` are vestigial ORM accommodations not currently needed (since the domain entities are not JPA-managed). They are harmless but signal false persistence awareness.

**5. `Holding.addLotFromPersistence()` names persistence in the domain.**
A minor naming issue: the method's name references "persistence" in the domain's ubiquitous language. `reconstitute(Lot lot)` or simply providing lots through the constructor would be cleaner.

**6. Application services use Jakarta `@Transactional` — a standard API, not a framework coupling.**
The application layer uses `jakarta.transaction.Transactional` rather than a Spring-specific annotation. This is a standard Java API, not a framework concession. Spring’s transaction infrastructure recognises the Jakarta annotation at runtime, so transactional behaviour is preserved without compile-time coupling to any framework.

**8. `PortfolioManagementUseCase` aggregates multiple concerns.**
Five methods spanning lifecycle management and cash operations in one interface. This is an interface segregation concern, not a hexagonal boundary violation, but it increases coupling for consumers that need only a subset of the operations.

**9. `TransactionUseCase.getTransactions` uses `String` instead of domain types.**
The use of `String portfolioId` and `Optional<String> type` where `PortfolioId` and `Optional<TransactionType>` would be more appropriate is a minor type-safety gap in the port definition.

#### Not Present (Positive Findings)

The following common hexagonal anti-patterns were evaluated and **not found**:

- ❌ **Bidirectional coupling between layers.** Not present. Dependencies are strictly unidirectional inward.
- ❌ **Adapter-to-adapter dependencies.** Not present. REST, JPA, and market adapters are mutually independent.
- ❌ **Domain model shaped by JPA.** Not present. Domain entities use `Map`, `List`, records — not JPA-friendly `Set<>` with `@ManyToOne` back-references.
- ❌ **Framework annotations in the domain.** Not present. Zero Spring, Zero JPA, Zero Jackson annotations in the domain module.
- ❌ **Application service as transaction script.** Not present. Services orchestrate; business logic lives in aggregates and domain services.
- ❌ **Duplicate business rules across layers.** Not present. Validation happens in the domain (value object constructors, aggregate methods); adapters and application services do not re-validate.
- ❌ **Hidden dependence on adapter ordering.** The FIFO ordering dependency on `@OrderBy` is explicit and documented, not hidden.

---

## 12. Combined DDD + Hexagonal Architecture Verdict

### 12.1 DDD Quality Assessment

**Rating: Strong.**

The HexaStock domain model is a credible, behaviour-rich DDD implementation. The `Portfolio` aggregate root enforces real financial invariants. Value objects eliminate primitive obsession. The FIFO lot-consumption algorithm is genuine domain logic, not framework glue. The aggregate boundary is correctly drawn. The `Transaction` sealed interface with four self-validating record subtypes demonstrates Java 21 type-safe domain modelling. The remaining DDD concern — application-service-driven transaction creation rather than domain events — is a clear intentional trade-off that does not undermine the aggregate root's integrity.

### 12.2 Hexagonal Architecture Quality Assessment

**Rating: Strong.**

The hexagonal architecture is genuine, enforced at the Maven module level, tested by ArchUnit fitness rules, and demonstrated by working adapter substitution (three market data implementations, profile-based selection). The domain is framework-free. The application layer is also framework-free — it uses the standard Jakarta `@Transactional` annotation for transactional demarcation, with Spring providing the transaction infrastructure at runtime in the outer layer. Adapters are thin, focused, and independent. The composition root is explicit. All DTOs — including `TransactionDTO` — follow the same anti-corruption pattern of mapping domain objects to flat, primitive-valued records. No significant hexagonal violations remain.

### 12.3 How DDD and Hexagonal Architecture Reinforce Each Other

In HexaStock, the two architectural approaches are **mutually reinforcing**, not competing:

1. **The aggregate boundary is the hexagonal core boundary.** The `Portfolio` aggregate defines what the domain protects; the hexagonal boundary defines what the domain is insulated from. These are the same boundary, expressed through different lenses.

2. **Value objects enforce type safety across layers.** Domain value objects (`Money`, `Price`, `ShareQuantity`, `Ticker`) appear in inbound port signatures, outbound port signatures, and the domain itself. Adapters must construct them (translating from primitives) and destructure them (translating back to primitives). This forces adapters to go through the domain's type system, reinforcing both DDD type discipline and hexagonal boundary discipline.

3. **The FIFO algorithm demonstrates both patterns simultaneously.** The FIFO lot-consumption logic lives in `Holding.sell()` (DDD: rich aggregate behaviour; Hexagonal: domain core has no infrastructure dependency). The persistence adapter ensures lots are loaded in chronological order (DDD: repository contract guarantees aggregate preconditions; Hexagonal: adapter translates infrastructure behaviour into domain expectations). The application service orchestrates the flow without knowledge of FIFO mechanics (DDD: service delegates to aggregate; Hexagonal: service uses ports, not concrete adapters).

4. **The reporting concern reveals tension in both lenses.** `HoldingPerformanceCalculator` merges data from the aggregate and the transaction store (DDD: parallel truth risk, as analysed in Section 9.6). This same concern appears through the hexagonal lens: the application service (`ReportingService`) must orchestrate three outbound ports to assemble the inputs for the domain service, creating a thick orchestration layer for what is conceptually a single query. Both DDD and hexagonal analysis point to the same root cause — the transaction record is disconnected from the aggregate — and suggest the same solution: domain events that unify the data flow.

5. **Transaction modelling reinforces both patterns.** The `Transaction` sealed hierarchy (DDD: type-safe, self-validating domain entities with Java 21 records) directly supports hexagonal discipline: `TransactionDTO.from(Transaction)` uses the sealed interface's accessor methods to produce a cleanly mapped primitive DTO, and `TransactionMapper` in the JPA adapter uses exhaustive switches on `TransactionType` to translate between domain and persistence representations. The improvement in DDD modelling (sealed hierarchy replacing flat tagged union) naturally improved hexagonal compliance (the previous wrapper-DTO leak was eliminated as part of the same refactoring).

### 12.4 Overall Combined Verdict

HexaStock is a well-executed educational project that successfully demonstrates both DDD and Hexagonal Architecture in a mutually reinforcing way. It is not a toy example: the domain model protects real financial invariants, the hexagonal structure is mechanically enforced, and the adapter layer demonstrates genuine substitutability. The `Transaction` sealed hierarchy and flat `TransactionDTO` mapping complete the alignment between the two architectural approaches — the domain model is consistently type-safe and behaviour-rich, and all adapters consistently translate between domain and infrastructure representations without leaking domain concepts.

**For a teaching codebase:** This project is exceptionally well-suited. It provides concrete, working examples of aggregate roots, value objects, domain services, sealed interface hierarchies with Java 21 records, inbound and outbound ports, adapter mapping, anti-corruption layers, composition root wiring, profile-based adapter substitution, and architectural fitness tests. The transaction creation responsibility (application service vs. domain events) is itself a valuable teaching discussion topic.

**For a production application:** The project provides a sound architectural foundation. The remaining improvements — domain events for transaction creation, relocating `ExternalApiException`, stronger `HoldingPerformance` typing — are incremental refinements, not structural redesigns.

---

## 13. Final Recommendations

### 13.1 Overall Verdict

The HexaStock domain model is a **well-designed, behaviour-rich DDD model** that successfully captures the essential mechanics of a personal investment portfolio. It is significantly above average for educational DDD projects and would serve as a credible starting point for a simple production application.

### 13.2 Main Strengths

**DDD:**

1. **Genuinely rich aggregate root.** `Portfolio.buy()` and `Portfolio.sell()` are real domain operations with real invariant enforcement, not getters and setters dressed in business method names.
2. **Disciplined value object design.** `Money`, `Price`, `ShareQuantity`, and `Ticker` are exemplary value objects — immutable, self-validating, and eliminating primitive obsession.
3. **Correct aggregate boundaries.** The Portfolio → Holding → Lot hierarchy is a natural fit for the domain, and the decision to keep Transaction external is well-reasoned and documented.
4. **Non-trivial domain algorithm.** The FIFO lot-consumption algorithm in `Holding.sell()` is real domain logic, not framework glue. Its correctness relies on domain knowledge (lot ordering, partial consumption, cost-basis accumulation), making it an excellent teaching vehicle.
5. **Thoughtful documentation.** The code contains detailed Javadoc explaining design decisions, alternatives considered, and DDD classification. The `ShareQuantity` documentation on why zero is allowed is particularly well-written.

**Hexagonal Architecture:**

6. **Genuine module-level separation.** Six Maven modules enforce layer boundaries at compile time, not by convention.
7. **Framework-free domain.** Zero framework dependencies in the domain module — the purest possible domain core.
8. **Executable architecture fitness tests.** `HexagonalArchitectureTest` mechanically enforces hexagonal dependency rules at every build with ArchUnit.
9. **Demonstrated adapter substitutability.** Three market data implementations (`FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`, `MockFinhubStockPriceAdapter`) prove that port-adapter separation is not theoretical.
10. **Clean anti-corruption layer.** Dedicated JPA entities and bidirectional mapper classes insulate the domain from persistence technology.

### 13.3 Remaining Concerns

**DDD:**

1. **Transaction creation in application service.** The domain model does not participate in ensuring that its own audit trail is correct. Transaction creation is an application-service responsibility, creating a consistency gap addressable through domain events.
2. **Parallel truth in performance calculation.** `HoldingPerformanceCalculator` derives positional data from transactions while the aggregate maintains positions independently. These two data paths could diverge.

**Hexagonal Architecture:**

3. **`ExternalApiException` is a domain exception for infrastructure failures.** An infrastructure concern (`ExternalApiException`) is modelled as a domain exception, creating a minor conceptual layer violation.
4. **`HoldingPerformance` uses `BigDecimal` instead of domain value objects.** Breaks the type discipline within the domain for apparent DTO-mapping convenience.
5. **No unit tests for REST controllers or persistence mappers.** The hexagonal structure enables isolated adapter testing, but this opportunity is not fully exploited.

### 13.4 Most Important Risks

**DDD Risks:**

1. **Silent portfolio-transaction divergence.** If a future developer adds a new operation and forgets to create a transaction, the audit trail becomes incomplete with no runtime warning.
2. **Integer share quantities limiting future evolution.** If the domain later needs fractional shares, `ShareQuantity(int)` cannot represent them.

**Hexagonal Risks:**

3. **Reporting flow creates thick orchestration.** `ReportingService` must coordinate three outbound ports and pass results to a domain service, creating a non-trivial orchestration surface that could become fragile as the reporting model evolves.

### 13.5 Practical Recommendation for Teaching

Keep the current model and architecture. Both are well-suited for teaching:

**DDD teaching value:**
- Rich aggregate root with real invariants.
- Nested entities (Holding, Lot) within the aggregate.
- Value objects with self-validation.
- Non-trivial domain algorithm (FIFO).
- Clean separation between aggregate and external entity.
- Sealed interface hierarchy with Java 21 records for transaction modelling.
- Transaction creation responsibility (application service vs. domain events) as a discussion topic.

**Hexagonal Architecture teaching value:**
- Multi-module Maven structure demonstrating compile-time layer enforcement.
- Framework-free domain module as a concrete example of domain protection.
- Inbound and outbound port interfaces with clear naming conventions.
- Dedicated JPA entity layer with bidirectional mappers (anti-corruption layer).
- Three implementations of a single outbound port (market data), demonstrating real adapter substitutability.
- Profile-based adapter selection showing runtime flexibility.
- ArchUnit fitness tests demonstrating executable architecture rules.
- Explicit composition root wiring in `SpringAppConfig` showing how ports and adapters are connected.
- Consistent DTO mapping pattern: all response DTOs (including `TransactionDTO`) follow the same `from()` factory method pattern, mapping domain objects to flat primitive records.
- The contrast between services that are `@Component`-annotated (adapters) and services that are `@Bean`-constructed (application services) illustrates different wiring strategies and their trade-offs.

The domain event alternative can be presented alongside the current design, showing how both DDD and hexagonal architecture guide toward the same improved design for transaction creation.

### 13.6 Practical Recommendation for Production

Apply the following targeted improvements:

**DDD improvements:**

1. **Introduce domain events.** Have `Portfolio.buy()` and `Portfolio.sell()` produce domain events (`StockPurchased`, `StockSold`). Derive transactions from events.
2. ~~**Refactor Transaction into a sealed hierarchy.**~~ ✅ **IMPLEMENTED.** `Transaction` is now a sealed interface with four record subtypes. Nullable fields are eliminated. Each subtype is self-documenting and self-validating.
3. ~~**Correct the Transaction Javadoc.**~~ ✅ **IMPLEMENTED.** Javadoc now correctly describes `Transaction` as an "immutable ledger entry."
4. ~~**Guarantee lot ordering explicitly.**~~ ✅ **IMPLEMENTED.** `Holding.addLotFromPersistence()` inserts lots in chronological order by `purchasedAt`, providing a domain-side safety net in addition to the JPA adapter's `@OrderBy("purchasedAt ASC")`.
5. **Rename `balance` to `cashBalance`** and consider `PURCHASE`/`SALE` → `BUY`/`SELL`.

**Hexagonal Architecture improvements:**

6. ~~**Fix `TransactionDTO`.**~~ ✅ **IMPLEMENTED.** `TransactionDTO` is now a flat record with primitive fields and a `from(Transaction)` factory method, following the established pattern.
7. **Move `ExternalApiException` out of the domain module.** Place it in the application layer or define an abstract failure concept in the domain and concrete infrastructure exceptions in the adapters.
8. **Add `@Transactional(readOnly = true)` to read-side services.** `TransactionService` and `GetStockPriceService` do not modify data and should not acquire write locks.
9. **Remove vestigial `protected` no-arg constructors from domain entities.** The domain entities are not JPA-managed; these constructors serve no purpose and signal false persistence awareness.
10. **Rename `Holding.addLotFromPersistence()` to `Holding.reconstitute(Lot)`** or accept lots through the constructor, removing persistence vocabulary from the domain.
11. **Split `PortfolioManagementUseCase`** into `PortfolioLifecycleUseCase` (create, get, list) and `CashManagementUseCase` (deposit, withdraw) for better interface segregation.
12. **Add unit tests for REST controllers and persistence mappers.** The hexagonal structure enables isolated adapter testing — exploit this for faster, more targeted test feedback.

These changes are incremental and preserve both the model's existing DDD strengths and the architecture's existing hexagonal integrity. No structural redesign is required.

---

## 14. Suggested Evolutionary Improvements

The following sketch illustrates the recommended improvements while preserving the current model's core strengths.

### 14.1 Aggregate Structure (Unchanged)

```
Portfolio (aggregate root)
├── cashBalance: Money
├── holdings: Map<Ticker, Holding>
│   └── Holding (entity)
│       ├── ticker: Ticker
│       └── lots: List<Lot>  (ordered by purchasedAt)
│           └── Lot (entity)
│               ├── initialShares: ShareQuantity
│               ├── remainingShares: ShareQuantity
│               ├── unitPrice: Price
│               └── purchasedAt: LocalDateTime
└── domainEvents: List<DomainEvent>  (← NEW: collected during operations)
```

### 14.2 Domain Events (New)

```java
sealed interface PortfolioEvent {
    PortfolioId portfolioId();
    LocalDateTime occurredAt();
}

record FundsDeposited(PortfolioId portfolioId, Money amount, LocalDateTime occurredAt)
    implements PortfolioEvent {}

record FundsWithdrawn(PortfolioId portfolioId, Money amount, LocalDateTime occurredAt)
    implements PortfolioEvent {}

record StockPurchased(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity,
                      Price unitPrice, Money totalCost, LocalDateTime occurredAt)
    implements PortfolioEvent {}

record StockSold(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity,
                 Price sellPrice, Money proceeds, Money costBasis, Money profit,
                 LocalDateTime occurredAt)
    implements PortfolioEvent {}
```

### 14.3 Transaction Sealed Hierarchy ✅ IMPLEMENTED

The following sealed hierarchy is now implemented in the codebase:

```java
public sealed interface Transaction
        permits DepositTransaction, WithdrawalTransaction,
                PurchaseTransaction, SaleTransaction {

    TransactionId id();
    PortfolioId portfolioId();
    TransactionType type();
    Money totalAmount();
    LocalDateTime createdAt();

    // Default accessors for fields not applicable to all subtypes
    default Ticker ticker() { return null; }
    default ShareQuantity quantity() { return ShareQuantity.ZERO; }
    default Price unitPrice() { return null; }
    default Money profit() { return Money.ZERO; }
}

public record DepositTransaction(TransactionId id, PortfolioId portfolioId,
                                  Money totalAmount, LocalDateTime createdAt)
    implements Transaction {
    // Compact constructor validates non-null and positive amount
    public TransactionType type() { return TransactionType.DEPOSIT; }
}

public record WithdrawalTransaction(TransactionId id, PortfolioId portfolioId,
                                     Money totalAmount, LocalDateTime createdAt)
    implements Transaction {
    public TransactionType type() { return TransactionType.WITHDRAWAL; }
}

public record PurchaseTransaction(TransactionId id, PortfolioId portfolioId,
                                   Ticker ticker, ShareQuantity quantity, Price unitPrice,
                                   Money totalAmount, LocalDateTime createdAt)
    implements Transaction {
    public TransactionType type() { return TransactionType.PURCHASE; }
}

public record SaleTransaction(TransactionId id, PortfolioId portfolioId,
                               Ticker ticker, ShareQuantity quantity, Price unitPrice,
                               Money totalAmount, Money profit, LocalDateTime createdAt)
    implements Transaction {
    public TransactionType type() { return TransactionType.SALE; }
}
```

Each record subtype carries only the fields meaningful to its transaction kind. Compact constructors enforce non-null and positivity invariants at construction time. Java records provide structural immutability and automatic `equals()`/`hashCode()` based on all fields.

### 14.4 Application Service (Revised Flow)

```java
// Illustrative — not compilable production code
public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.load(portfolioId);
    StockPrice price = stockPricePort.fetch(ticker);

    SellResult result = portfolio.sell(ticker, quantity, price.price());
    // portfolio.sell() internally registers a StockSold domain event

    portfolioPort.save(portfolio);
    // Event handler converts StockSold → SaleTransaction and persists it

    return result;
}
```

### 14.5 What Has Changed and What Remains

| Aspect | Previous | Current | Future (Domain Events) |
|---|---|---|---|
| Transaction types | Single class + enum tag | ✅ Sealed interface + records | Unchanged |
| Nullable fields | Yes (ticker, unitPrice for deposits) | ✅ Eliminated | Unchanged |
| Self-validation | None | ✅ Compact constructor invariants | Unchanged |
| `TransactionDTO` | Wrapped domain entity | ✅ Flat DTO with `from()` factory | Unchanged |
| FIFO ordering | Implicit ArrayList convention | ✅ Domain-side chronological insertion | Unchanged |
| Transaction creation | Application service | Application service | Domain event → event handler |
| Audit completeness | Convention-dependent | Convention-dependent | Guaranteed by event emission |
| Domain event support | None | None | Built-in event collection |
| Aggregate structure | Unchanged | Unchanged | Unchanged |
| FIFO algorithm | Unchanged | Unchanged | Unchanged |
| Value objects | Unchanged | Unchanged | Unchanged |

This revision is evolutionary, not revolutionary. It addresses the identified weaknesses while preserving everything that already works well.

### 14.6 Hexagonal Architecture Improvements

The hexagonal architecture requires fewer structural changes than the DDD model because the architecture is already sound. The following improvements refine what exists rather than restructure it.

#### 14.6.1 `TransactionDTO` Mapping ✅ IMPLEMENTED

`TransactionDTO` is now a flat record with primitive fields and a `from(Transaction)` factory method:

```java
public record TransactionDTO(
        String id,
        String portfolioId,
        String type,
        String ticker,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BigDecimal profit,
        LocalDateTime createdAt
) {
    public static TransactionDTO from(Transaction tx) {
        return new TransactionDTO(
                tx.id().value(),
                tx.portfolioId().value(),
                tx.type().name(),
                tx.ticker() != null ? tx.ticker().value() : null,
                tx.quantity() != null ? tx.quantity().value() : null,
                tx.unitPrice() != null ? tx.unitPrice().value() : null,
                tx.totalAmount().amount(),
                tx.profit() != null ? tx.profit().amount() : null,
                tx.createdAt()
        );
    }
}
```

This follows the established pattern of `PortfolioResponseDTO.from()` and `SaleResponseDTO.from()`. The sealed interface's default accessor methods (`ticker()` returns `null`, `quantity()` returns `ZERO`, etc.) allow uniform field access across all transaction subtypes.

#### 14.6.2 Relocate `ExternalApiException`

Move `ExternalApiException` from `cat.gencat.agaur.hexastock.model` to `cat.gencat.agaur.hexastock.application.exception`, alongside `PortfolioNotFoundException` and `HoldingNotFoundException`. The market adapter still throws it, the REST exception handler still catches it — only the package location changes, but the conceptual alignment improves: infrastructure failures are now an application-layer concern, not a domain concept.

#### 14.6.3 Domain Events Improve Hexagonal Flow

The domain event proposal from Section 14.2 also simplifies the hexagonal flow:

```
Current:
  Adapter → Port → Service → Aggregate → Service creates Transaction → Port → Adapter

Revised:
  Adapter → Port → Service → Aggregate (emits event) → Service saves aggregate → Port → Adapter
                                                       → Event handler creates Transaction → Port → Adapter
```

The service becomes thinner (orchestrate and save, not transcribe), and the event handler (an application-layer component) handles the translation between domain events and transaction records. This aligns with hexagonal principles: each component has a single translation responsibility.

---

*End of Review*
