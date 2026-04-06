# Domain Model Review: Personal Investment Portfolio

**Project:** HexaStock
**Review Date:** April 2026
**Scope:** Domain layer — `cat.gencat.agaur.hexastock.model.*`

---

## 1. Executive Summary

The HexaStock domain model is a well-constructed, behaviour-rich aggregate design that successfully captures the core mechanics of a personal investment portfolio. It is among the better DDD models one encounters in educational codebases and holds up respectably against production-grade designs for its stated scope.

The model's principal strengths are:

- A genuinely rich `Portfolio` aggregate root that enforces cash balance, FIFO lot consumption, and share availability invariants through behaviour, not getters and setters.
- A disciplined value object layer (`Money`, `Price`, `ShareQuantity`, `Ticker`) that eliminates primitive obsession and encodes domain rules at the type level.
- A clean separation between the `Portfolio` aggregate (current positional state) and `Transaction` (historical record), with thoughtful reasoning documented in the code itself.

Its principal weaknesses are:

- The `Transaction` entity is structurally anemic: it is a flat, type-tagged data container that conflates four semantically distinct event types into a single class with nullable fields. This is the most significant modelling concern in the codebase.
- Transaction creation is entirely an application-service concern with no domain involvement, which means the domain model does not participate in ensuring that its own history is correct.
- The `HoldingPerformanceCalculator` domain service re-derives positional data from transactions rather than from the aggregate, creating a parallel truth that could diverge.
- Some financial realism is deliberately sacrificed (single currency, integer share quantities, no fees or taxes), which is acceptable for pedagogy but should be explicitly acknowledged as a boundary.

Overall verdict: the model is sound, well above average for a teaching codebase, and defensible for a simple production application. The transaction modelling is the area most deserving of architectural attention.

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
| FIFO ordering | `Holding.sell()` — iterates lots in insertion order | Correct, but depends on `ArrayList` ordering. |
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

The only area where behaviour is conspicuously absent is `Transaction`, which is discussed at length in Section 5.

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

This is the area of the model that deserves the most critical scrutiny.

### 5.1 Current Design

`Transaction` is a domain entity (despite its Javadoc calling it a value object) with:

- A `TransactionId` for identity.
- A `PortfolioId` foreign key.
- A `TransactionType` enum (`DEPOSIT`, `WITHDRAWAL`, `PURCHASE`, `SALE`).
- Fields for `ticker`, `quantity`, `unitPrice`, `totalAmount`, `profit`, and `createdAt`.
- Factory methods for each transaction type.
- A builder for construction.
- No domain behaviour beyond construction.

Transactions are created by the application services (`PortfolioStockOperationsService`, `PortfolioManagementService`) after the aggregate is mutated, and saved through `TransactionPort`.

### 5.2 Structural Critique: The Type-Tag Problem

The most significant design weakness is that `Transaction` is a **tagged union without the benefits of a tagged union**. All four transaction types share the same flat field structure, but different types use different fields:

| Field | DEPOSIT | WITHDRAWAL | PURCHASE | SALE |
|---|---|---|---|---|
| `ticker` | null | null | set | set |
| `quantity` | ZERO | ZERO | set | set |
| `unitPrice` | null | null | set | set |
| `totalAmount` | set | set | set | set |
| `profit` | ZERO | ZERO | ZERO | set |

This means:

- A `DEPOSIT` transaction has a `quantity` field that is always `ShareQuantity.ZERO` — meaningless noise.
- A `PURCHASE` transaction has a `profit` field that is always `Money.ZERO` — a lie by convention.
- A `SALE` transaction's `ticker` field is nullable in the type system but required by the business — a latent null pointer exception.
- There is nothing in the type system preventing the construction of a `DEPOSIT` with a non-null `ticker`, or a `SALE` with zero profit when profit should be mandatory.

This is not a fatal flaw, but it is a missed opportunity for stronger domain modelling.

### 5.3 Behavioural Critique: Anemic Transactions

`Transaction` has no domain behaviour. It is created, persisted, and queried — but it never participates in domain logic, never validates its own consistency, and never enforces any invariant.

This is partly by design: transactions in this model are historical records, not active domain objects. But even as historical records, they could benefit from:

- **Self-validation:** A `SALE` transaction should reject construction with a null ticker. A `DEPOSIT` should reject construction with a non-zero quantity.
- **Derived field computation:** The `totalAmount` for a `PURCHASE` is always `unitPrice × quantity`, but the factory method computes this externally and passes it in. The transaction could compute it internally and guarantee consistency.
- **Equality and identity semantics:** The current design gives transactions UUID identity but no `equals`/`hashCode` override, which means identity comparison falls back to object reference equality.

### 5.4 Architectural Critique: Who Is Responsible for Creating Transactions?

Currently, the application service creates transactions:

```java
// In PortfolioStockOperationsService.sellStock()
SellResult sellResult = portfolio.sell(ticker, quantity, price);
portfolioPort.savePortfolio(portfolio);

Transaction transaction = Transaction.createSale(
        portfolioId, ticker, quantity, price, sellResult.proceeds(), sellResult.profit());
transactionPort.save(transaction);
```

This creates two risks:

1. **Consistency risk:** If `portfolioPort.savePortfolio()` succeeds but `transactionPort.save()` fails (or is never called due to a bug), the portfolio state and transaction history diverge. Both calls are within a `@Transactional` boundary, which mitigates the infrastructure failure case, but does not protect against developer error in future modifications.

2. **Knowledge leakage:** The application service must know the internal financial details of the sell operation (proceeds, profit) to construct the transaction. This information comes from `SellResult`, which is a reasonable coupling, but it means the application service is responsible for correctly transcribing domain results into transaction records — a responsibility that arguably belongs to the domain.

A stronger design would have the aggregate produce the transaction record (or a domain event from which the record is derived), ensuring that the domain itself guarantees the completeness and correctness of its own audit trail. This is discussed in the alternatives section.

### 5.5 The "Value Object" Misnomer

The `Transaction` Javadoc describes it as a "Value Object," but it has a `TransactionId`, is persisted with identity, and is created with `TransactionId.generate()`. It is an **entity** by every DDD definition. Calling it a value object is misleading and should be corrected.

One could argue that a transaction is conceptually immutable (once created, it is never modified), and therefore "value-like." This is true, but immutability alone does not make something a value object. A value object is identified by its attributes, not by a unique identifier. A transaction with a `TransactionId` is an entity with immutable state — a perfectly valid and common pattern, but distinct from a value object.

### 5.6 What Transaction Should Be

For this domain, `Transaction` is best understood as an **immutable ledger entry** — an entity with identity, created by or on behalf of the domain, persisted for auditability and reporting, but never modified after creation. This is different from a domain event (which is infrastructure), a value object (which has no identity), or a mutable entity (which changes state).

The current implementation is close to this, but it would benefit from:

1. Making immutability explicit (final fields, no protected no-arg constructor, or using a record).
2. Splitting into type-specific classes or using a sealed hierarchy.
3. Adding self-validation per transaction type.
4. Optionally, having the domain produce transaction records rather than the application service.

### 5.7 Should Transaction Types Be Modelled Separately?

**There is a strong argument for a sealed hierarchy.** Java 21 provides the language features to do this elegantly:

```
sealed interface Transaction permits DepositTransaction, WithdrawalTransaction,
                                     PurchaseTransaction, SaleTransaction
```

Each subtype would carry only the fields relevant to it:

- `DepositTransaction(TransactionId, PortfolioId, Money amount, LocalDateTime)`
- `WithdrawalTransaction(TransactionId, PortfolioId, Money amount, LocalDateTime)`
- `PurchaseTransaction(TransactionId, PortfolioId, Ticker, ShareQuantity, Price, Money totalCost, LocalDateTime)`
- `SaleTransaction(TransactionId, PortfolioId, Ticker, ShareQuantity, Price, Money proceeds, Money costBasis, Money profit, LocalDateTime)`

This eliminates nullable fields, makes each type self-documenting, enables exhaustive pattern matching, and aligns with domain language (a "sale" is not the same concept as a "deposit" — they should not be the same type).

The trade-off is persistence complexity: a single `transactions` table with a discriminator column is simpler to map than a sealed hierarchy. But this is an adapter concern, not a domain concern, and should not drive the domain model.

### 5.8 Should Transactions Live Inside the Portfolio Aggregate?

**No.** The current design's decision to keep transactions external is correct. Transactions are an unbounded, append-only collection. Including them in the aggregate would:

- Force loading all historical transactions for every portfolio operation.
- Make the aggregate grow without bound.
- Couple reporting queries to the transactional consistency boundary.

The trade-off (potential consistency gap between portfolio state and transaction log) is acceptable and manageable through the `@Transactional` boundary.

### 5.9 Transaction Modelling Verdict

The transaction model is the weakest part of the domain design. It is functional but structurally anemic, uses nullable fields where type safety would be better, and places transaction creation responsibility in the application service rather than the domain. For a teaching codebase, this is an excellent opportunity to demonstrate the progression from a simple tagged-union design to a richer, type-safe, domain-driven transaction model.

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
FIFO correctness depends on `ArrayList` insertion order being stable across persistence round-trips. If the JPA adapter loads lots in a different order (e.g., by `LotId` rather than `purchasedAt`), FIFO would silently break. This is a subtle invariant that is enforced by convention between the domain and persistence layers, not by the domain model itself. The persistence adapter should be tested against this specifically.

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
| `Transaction` | Entity (documented as VO) | Should be entity. Has `TransactionId`. Documentation says "Value Object" but this is incorrect. |
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

The model exhibits strong tactical DDD discipline: rich behaviour in the aggregate, immutable value objects, proper identity management, defensive copying, and clean repository boundaries. The main tactical improvement needed is correcting the `Transaction` classification from "value object" to "entity" and strengthening its internal validation.

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

## 11. Final Recommendations

### 11.1 Overall Verdict

The HexaStock domain model is a **well-designed, behaviour-rich DDD model** that successfully captures the essential mechanics of a personal investment portfolio. It is significantly above average for educational DDD projects and would serve as a credible starting point for a simple production application.

### 11.2 Main Strengths

1. **Genuinely rich aggregate root.** `Portfolio.buy()` and `Portfolio.sell()` are real domain operations with real invariant enforcement, not getters and setters dressed in business method names.
2. **Disciplined value object design.** `Money`, `Price`, `ShareQuantity`, and `Ticker` are exemplary value objects — immutable, self-validating, and eliminating primitive obsession.
3. **Correct aggregate boundaries.** The Portfolio → Holding → Lot hierarchy is a natural fit for the domain, and the decision to keep Transaction external is well-reasoned and documented.
4. **Non-trivial domain algorithm.** The FIFO lot-consumption algorithm in `Holding.sell()` is real domain logic, not framework glue. Its correctness relies on domain knowledge (lot ordering, partial consumption, cost-basis accumulation), making it an excellent teaching vehicle.
5. **Thoughtful documentation.** The code contains detailed Javadoc explaining design decisions, alternatives considered, and DDD classification. The `ShareQuantity` documentation on why zero is allowed is particularly well-written.

### 11.3 Main Weaknesses

1. **Anemic Transaction model.** `Transaction` is a flat, type-tagged data container with nullable fields and no domain behaviour. It is the most significant modelling weakness.
2. **Transaction creation in application service.** The domain model does not participate in ensuring that its own audit trail is correct. Transaction creation is an application-service responsibility, creating a consistency gap addressable through domain events.
3. **Transaction misclassified as Value Object.** The Javadoc calls it a Value Object, but it is an entity with identity.
4. **Parallel truth in performance calculation.** `HoldingPerformanceCalculator` derives positional data from transactions while the aggregate maintains positions independently. These two data paths could diverge.
5. **FIFO ordering depends on persistence.** The FIFO guarantee depends on `ArrayList` insertion order being preserved through persistence round-trips — an implicit contract between domain and adapter layers.

### 11.4 Most Important Risks

1. **Silent portfolio-transaction divergence.** If a future developer adds a new operation and forgets to create a transaction, the audit trail becomes incomplete with no runtime warning.
2. **FIFO corruption through mis-ordered lot loading.** If the JPA adapter changes its query ordering, FIFO breaks silently.
3. **Integer share quantities limiting future evolution.** If the domain later needs fractional shares, `ShareQuantity(int)` cannot represent them.

### 11.5 Practical Recommendation for a DDD Teaching Project

Keep the current model. It is well-suited for teaching:

- Rich aggregate root with real invariants.
- Nested entities (Holding, Lot) within the aggregate.
- Value objects with self-validation.
- Non-trivial domain algorithm (FIFO).
- Clean separation between aggregate and external entity.
- Transaction modelling as a discussion topic (what's wrong? how could it be improved?).

Consider using the Transaction design as a **deliberate teaching exercise**: present the current tagged-union design, analyse its weaknesses, and challenge students to propose a sealed-hierarchy or domain-event–based alternative.

### 11.6 Practical Recommendation for a Production Application

Apply the following targeted improvements:

1. **Introduce domain events.** Have `Portfolio.buy()` and `Portfolio.sell()` produce domain events (`StockPurchased`, `StockSold`). Derive transactions from events.
2. **Refactor Transaction into a sealed hierarchy.** Eliminate nullable fields. Make each transaction type self-documenting and self-validating.
3. **Correct the Transaction Javadoc.** It is an entity, not a value object.
4. **Guarantee lot ordering explicitly.** Add an `ordinal` or `purchasedAt` comparator and enforce ordering at load time, not by implicit `ArrayList` convention.
5. **Rename `balance` to `cashBalance`** and consider `PURCHASE`/`SALE` → `BUY`/`SELL`.

These changes are incremental and preserve the model's existing strengths.

---

## 12. Suggested Revised Model

The following sketch illustrates the recommended improvements while preserving the current model's core strengths.

### 12.1 Aggregate Structure (Unchanged)

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

### 12.2 Domain Events (New)

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

### 12.3 Transaction Sealed Hierarchy (Revised)

```java
sealed interface Transaction permits
    DepositTransaction, WithdrawalTransaction,
    PurchaseTransaction, SaleTransaction {

    TransactionId id();
    PortfolioId portfolioId();
    Money amount();
    LocalDateTime createdAt();
}

record DepositTransaction(TransactionId id, PortfolioId portfolioId,
                          Money amount, LocalDateTime createdAt)
    implements Transaction {}

record WithdrawalTransaction(TransactionId id, PortfolioId portfolioId,
                             Money amount, LocalDateTime createdAt)
    implements Transaction {}

record PurchaseTransaction(TransactionId id, PortfolioId portfolioId,
                           Ticker ticker, ShareQuantity quantity, Price unitPrice,
                           Money amount, LocalDateTime createdAt)
    implements Transaction {}

record SaleTransaction(TransactionId id, PortfolioId portfolioId,
                       Ticker ticker, ShareQuantity quantity, Price sellPrice,
                       Money amount, Money costBasis, Money profit,
                       LocalDateTime createdAt)
    implements Transaction {}
```

### 12.4 Application Service (Revised Flow)

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

### 12.5 What This Changes

| Aspect | Current | Revised |
|---|---|---|
| Transaction creation | Application service | Domain event → event handler |
| Transaction types | Single class + enum tag | Sealed interface + records |
| Nullable fields | Yes (ticker, unitPrice for deposits) | Eliminated |
| Audit completeness | Convention-dependent | Guaranteed by event emission |
| Domain event support | None | Built-in event collection |
| Aggregate structure | Unchanged | Unchanged |
| FIFO algorithm | Unchanged | Unchanged |
| Value objects | Unchanged | Unchanged |

This revision is evolutionary, not revolutionary. It addresses the identified weaknesses while preserving everything that already works well.

---

*End of Review*
