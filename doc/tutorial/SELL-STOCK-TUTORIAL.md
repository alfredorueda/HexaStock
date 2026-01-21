# Selling Stocks in HexaStock: Hexagonal Architecture and DDD Tutorial

## Table of Contents

- [1. Purpose and Learning Objectives](#1-purpose-and-learning-objectives)
- [2. Domain Context: What "Selling Stocks" Means in HexaStock](#2-domain-context-what-selling-stocks-means-in-hexastock)
- [3. Entry Point: The REST Endpoint (Driving Adapter)](#3-entry-point-the-rest-endpoint-driving-adapter)
- [4. Hexagonal Architecture Map for the SELL Use Case](#4-hexagonal-architecture-map-for-the-sell-use-case)
- [5. Step-by-Step Execution Trace: Happy Path](#5-step-by-step-execution-trace-happy-path)
  - [Step 1: Controller Receives Request](#step-1-controller-receives-request)
  - [Step 2: Controller Calls Inbound Port](#step-2-controller-calls-inbound-port)
  - [Step 3: Application Service Orchestrates](#step-3-application-service-orchestrates)
  - [Step 4: Domain Model Enforces Invariants](#step-4-domain-model-enforces-invariants)
  - [Step 5: Persistence Adapter Saves Changes](#step-5-persistence-adapter-saves-changes)
  - [Step 6: Response Returns to Client](#step-6-response-returns-to-client)
- [6. Why Application Services Orchestrate and Aggregates Protect Invariants](#6--why-application-services-orchestrate-and-aggregates-protect-invariants)
  - [A) Roles Explained with Real Code](#a-roles-explained-with-real-code)
  - [B) Concrete Domain Example: Why Direct Manipulation Breaks Invariants](#b-concrete-domain-example-why-direct-manipulation-breaks-invariants)
  - [C) Sequence Diagram: Orchestrator vs Aggregate Root](#c-sequence-diagram-orchestrator-vs-aggregate-root)
  - [D) Teaching Note](#d-teaching-note)
- [7. Transactionality and Consistency](#7-transactionality-and-consistency)
- [8. Persistence Mapping](#8-persistence-mapping)
  - [Domain Model → JPA Entities](#domain-model--jpa-entities)
  - [Repositories](#repositories)
- [9. Error Flows](#9-error-flows)
  - [Error 1: Portfolio Not Found](#error-1-portfolio-not-found)
  - [Error 2: Invalid Quantity](#error-2-invalid-quantity)
  - [Error 3: Selling More Than Owned](#error-3-selling-more-than-owned)
- [10. Key Takeaways](#10-key-takeaways)
  - [About Hexagonal Architecture](#about-hexagonal-architecture)
  - [About Domain-Driven Design](#about-domain-driven-design)
- [11. Summary: The Complete Sell Flow](#11-summary-the-complete-sell-flow)
- [12. Exercises for Students](#12-exercises-for-students)
  - [Exercise 1: Trace the Buy Flow](#exercise-1-trace-the-buy-flow)
  - [Exercise 2: Identify Aggregate Boundaries](#exercise-2-identify-aggregate-boundaries)
  - [Exercise 3: Map Domain Exceptions to HTTP Status Codes](#exercise-3-map-domain-exceptions-to-http-status-codes)
  - [Exercise 4: Explain the Role of @Transactional](#exercise-4-explain-the-role-of-transactional)
  - [Exercise 5: Add a Maximum Sell Percentage Invariant](#exercise-5-add-a-maximum-sell-percentage-invariant)
  - [Exercise 6: Distinguish Value Objects from Entities](#exercise-6-distinguish-value-objects-from-entities)
  - [Exercise 7: Add a Third Stock Price Provider Adapter (Prove the Hexagon Works)](#exercise-7-add-a-third-stock-price-provider-adapter-prove-the-hexagon-works)
- [13. References](#13-references)

> **💡 How to use this Table of Contents:**  
> Click any link to jump directly to that section. The structure follows the document's hierarchy: main sections (##) are at the top level, subsections (###) are indented once, and specific exercises or cases (####) are indented twice. Use your browser's back button or scroll to navigate between sections.

---

## 1. Purpose and Learning Objectives

This tutorial explains a **real use case** from the HexaStock codebase: **selling stocks from a portfolio**. By following the actual execution path through real code, you will learn:

- How **Hexagonal Architecture** separates concerns between adapters, ports, and domain logic
- How **Domain-Driven Design (DDD)** protects business invariants through aggregate roots
- Why application services **orchestrate** while aggregates **decide**
- How FIFO accounting is implemented at the domain level
- How domain exceptions translate to HTTP responses

---

## 2. Domain Context: What "Selling Stocks" Means in HexaStock

<img width="1693" height="1576" alt="image" src="https://github.com/user-attachments/assets/dcc5c599-319d-48ae-838d-c6be7800d952" />

In this system:

- A **Portfolio** represents an investor's account containing cash and stock holdings
- A **Holding** tracks all shares owned for a specific stock ticker (e.g., AAPL)
- A **Lot** represents a single purchase transaction—a batch of shares bought at a specific price and time
- **FIFO (First-In-First-Out)** accounting is used: when selling, the oldest lots are sold first
- A **Transaction** record is created for every financial activity (deposit, withdrawal, purchase, sale)

When you sell stocks in HexaStock:
1. The system fetches the current market price
2. It applies FIFO to determine which lots to draw from
3. It calculates proceeds (money received), cost basis (original purchase cost), and profit/loss
4. It updates the portfolio's cash balance and holdings
5. It records a transaction for audit purposes

---

## 3. Entry Point: The REST Endpoint (Driving Adapter)

**File:** `src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java`

**Endpoint:** `POST /api/portfolios/{id}/sales`

```java
@PostMapping("/{id}/sales")
public ResponseEntity<SaleResponseDTO> sellStock(@PathVariable String id,
                                                 @RequestBody SaleRequestDTO request) {
    SellResult result =
            portfolioStockOperationsUseCase.sellStock(id, Ticker.of(request.ticker()),
                    request.quantity());
    return ResponseEntity
            .ok(new SaleResponseDTO(id, request.ticker(), request.quantity(), result));
}
```

**Request Body Example:**
```json
{
  "ticker": "AAPL",
  "quantity": 5
}
```

**Why This Is a Driving Adapter:**
- It receives HTTP requests from the outside world
- It **depends on the inbound port** (`PortfolioStockOperationsUseCase`), **not** on the implementation
- It converts HTTP-specific data (JSON, path variables) into domain objects (`Ticker`)
- It converts domain results (`SellResult`) into DTOs (`SaleResponseDTO`)
- It handles HTTP concerns (status codes, response entities)

This controller **drives** the application by calling its use cases. It does not contain business logic.

<img width="2168" height="1434" alt="image" src="https://github.com/user-attachments/assets/a9ffd45a-46d5-4406-bb6c-28af84482f96" />


---

## 4. Hexagonal Architecture Map for the SELL Use Case

Here is the complete architecture trace for selling stocks:

| Layer | Component | Type | Package/Class |
|-------|-----------|------|---------------|
| **Driving Adapter** | REST Controller | HTTP → Application | `adapter.in.PortfolioRestController` |
| **Primary Port** | Inbound Interface | Use Case Definition | `application.port.in.PortfolioStockOperationsUseCase` |
| **Application Service** | Orchestrator | Use Case Implementation | `application.service.PortfolioStockOperationsService` |
| **Domain Model** | Aggregate Root | Business Logic | `model.Portfolio` |
| **Domain Model** | Entity | Business Logic | `model.Holding` |
| **Domain Model** | Entity | Business Logic | `model.Lot` |
| **Secondary Port** | Outbound Interface | Persistence Contract | `application.port.out.PortfolioPort` |
| **Secondary Port** | Outbound Interface | Price Provider Contract | `application.port.out.StockPriceProviderPort` |
| **Secondary Port** | Outbound Interface | Transaction Storage Contract | `application.port.out.TransactionPort` |
| **Driven Adapters** | JPA Repositories | Application → Database | `adapter.out.jpa.*` |

**Diagram Reference:** See [`diagrams/sell-http-to-port.puml`](diagrams/sell-http-to-port.puml)

---

## 5. Step-by-Step Execution Trace: Happy Path

### Step 1: Controller Receives Request

The `PortfolioRestController` receives:
```json
POST /api/portfolios/abc-123/sales
{
  "ticker": "AAPL",
  "quantity": 5
}
```

It extracts:
- Portfolio ID: `"abc-123"` (from path)
- Ticker: `"AAPL"` (from request body)
- Quantity: `5` (from request body)

### Step 2: Controller Calls Inbound Port

```java
SellResult result = portfolioStockOperationsUseCase.sellStock(
    id, 
    Ticker.of(request.ticker()), 
    request.quantity()
);
```

The controller calls the **use case interface**, not a concrete class. This is dependency inversion in action.

### Step 3: Application Service Orchestrates

**File:** `application.service.PortfolioStockOperationsService`

```java
@Override
public SellResult sellStock(String portfolioId, Ticker ticker, int quantity) {
    // 1. Retrieve portfolio from persistence
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

    // 2. Fetch current stock price from external provider
    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);

    // 3. Delegate to domain model (AGGREGATE ROOT)
    SellResult sellResult = portfolio.sell(ticker, quantity, 
        BigDecimal.valueOf(stockPrice.price()));

    // 4. Persist updated portfolio
    portfolioPort.savePortfolio(portfolio);

    // 5. Record transaction for audit
    Transaction transaction = Transaction.createSale(
        portfolioId, ticker, quantity, 
        BigDecimal.valueOf(stockPrice.price()), 
        sellResult.proceeds(), 
        sellResult.profit()
    );
    transactionPort.save(transaction);

    return sellResult;
}
```

**Notice what the service does:**
- ✅ Retrieves data from adapters
- ✅ Calls the aggregate root
- ✅ Coordinates persistence
- ❌ Does **NOT** contain domain rules
- ❌ Does **NOT** manipulate nested entities directly

**Diagram Reference:** See [`diagrams/sell-application-service.puml`](diagrams/sell-application-service.puml)

<img width="2086" height="1300" alt="image" src="https://github.com/user-attachments/assets/b10eb9a9-2adf-4cc8-93b5-9f6ea682cbef" />


### Step 4: Domain Model Enforces Invariants

**File:** `model.Portfolio`

```java
public SellResult sell(Ticker ticker, int quantity, BigDecimal price) {
    if (quantity <= 0)
        throw new InvalidQuantityException("Quantity must be positive");

    if (price.compareTo(BigDecimal.ZERO) <= 0)
        throw new InvalidAmountException("Price must be positive");

    if (!holdings.containsKey(ticker))
        throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker);

    Holding holding = holdings.get(ticker);

    SellResult result = holding.sell(quantity, price);
    balance = balance.add(result.proceeds());
    
    return result;
}
```

The Portfolio (aggregate root):
- Validates inputs
- Protects the invariant: "You can only sell holdings you own"
- Delegates to the Holding entity
- Updates its own cash balance
- Returns the result

**File:** `model.Holding`

```java
public SellResult sell(int quantity, BigDecimal sellPrice) {
    if (getTotalShares() < quantity) {
        throw new ConflictQuantityException("Not enough shares to sell");
    }

    BigDecimal proceeds = sellPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal costBasis = BigDecimal.ZERO;
    int remaining = quantity;

    // FIFO: Sell from oldest lots first
    for (Lot lot : lots) {
        if (remaining == 0) break;
        
        int toSell = Math.min(remaining, lot.getRemaining());
        lot.reduce(toSell);
        costBasis = costBasis.add(
            lot.getUnitPrice().multiply(BigDecimal.valueOf(toSell))
        );
        remaining -= toSell;
    }

    BigDecimal profit = proceeds.subtract(costBasis);
    return new SellResult(proceeds, costBasis, profit);
}
```

The Holding:
- Protects the invariant: "You cannot sell more shares than you own"
- Implements FIFO across multiple lots
- Calculates cost basis from the original purchase prices
- Calculates profit/loss

**File:** `model.Lot`

```java
public void reduce(int qty) {
    if (qty > remaining) {
        throw new ConflictQuantityException("Cannot reduce by more than remaining quantity");
    }
    remaining -= qty;
}
```

The Lot:
- Protects the invariant: "Remaining shares cannot go negative"
- Updates its remaining quantity

**Diagram Reference:** See [`diagrams/sell-domain-fifo.puml`](diagrams/sell-domain-fifo.puml)

<img width="1831" height="2343" alt="image" src="https://github.com/user-attachments/assets/d9a1aa88-1f1c-4d37-99a1-d7e2c37aae38" />


### Step 5: Persistence Adapter Saves Changes

The `PortfolioPort` implementation (a JPA adapter) converts the domain `Portfolio` into JPA entities and persists them.

**Diagram Reference:** See [`diagrams/sell-persistence-adapter.puml`](diagrams/sell-persistence-adapter.puml)

<img width="2154" height="1562" alt="image" src="https://github.com/user-attachments/assets/67813333-e842-4b65-9887-861056e36d31" />


### Step 6: Response Returns to Client

The controller wraps the `SellResult` in a DTO and returns:

```json
HTTP 200 OK
{
  "portfolioId": "abc-123",
  "ticker": "AAPL",
  "quantity": 5,
  "proceeds": 750.00,
  "costBasis": 625.00,
  "profit": 125.00
}
```

---

## 6. 🎯 Why Application Services Orchestrate and Aggregates Protect Invariants

This is the **most important concept** in DDD and Hexagonal Architecture.

### A) Roles Explained with Real Code

**Inbound Port (Contract):**

```java
// application.port.in.PortfolioStockOperationsUseCase
public interface PortfolioStockOperationsUseCase {
    SellResult sellStock(String portfolioId, Ticker ticker, int quantity);
}
```

This interface defines **what** the application can do, not **how**.

**Application Service (Orchestrator):**

```java
// application.service.PortfolioStockOperationsService
@Transactional
public class PortfolioStockOperationsService 
    implements PortfolioStockOperationsUseCase {
    
    private final PortfolioPort portfolioPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final TransactionPort transactionPort;

    @Override
    public SellResult sellStock(String portfolioId, Ticker ticker, int quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);

        SellResult sellResult = portfolio.sell(ticker, quantity, 
            BigDecimal.valueOf(stockPrice.price()));

        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSale(...);
        transactionPort.save(transaction);

        return sellResult;
    }
}
```

**Role:** **DIRECTOR OF ORCHESTRA**
- It retrieves the portfolio
- It fetches the price
- It **delegates the decision** to the aggregate root
- It persists the changes
- It records the transaction

**What it does NOT do:**
- ❌ Validate quantities
- ❌ Check if holdings exist
- ❌ Implement FIFO logic
- ❌ Calculate profit/loss
- ❌ Update lot quantities directly

**Aggregate Root (Decision Maker):**

```java
// model.Portfolio
public SellResult sell(Ticker ticker, int quantity, BigDecimal price) {
    if (quantity <= 0)
        throw new InvalidQuantityException("Quantity must be positive");

    if (!holdings.containsKey(ticker))
        throw new HoldingNotFoundException("Holding not found");

    Holding holding = holdings.get(ticker);
    SellResult result = holding.sell(quantity, price);
    balance = balance.add(result.proceeds());
    
    return result;
}
```

**Role:** **GUARDIAN OF INVARIANTS**
- It enforces "quantity must be positive"
- It enforces "you can only sell holdings you own"
- It updates the balance **consistently** with the sale
- It controls access to its nested entities (Holding, Lot)

---

### B) Concrete Domain Example: Why Direct Manipulation Breaks Invariants

#### ❌ **Anti-Pattern: Service Manipulating Nested Entities Directly**

Imagine if the application service did this:

```java
// WRONG! DO NOT DO THIS!
@Override
public SellResult sellStock(String portfolioId, Ticker ticker, int quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId).orElseThrow();
    Holding holding = portfolio.getHoldings().stream()
        .filter(h -> h.getTicker().equals(ticker))
        .findFirst()
        .orElseThrow();
    
    // Service directly manipulates lots - DANGEROUS!
    int remaining = quantity;
    for (Lot lot : holding.getLots()) {
        if (remaining > 0) {
            int toSell = Math.min(remaining, lot.getRemaining());
            lot.reduce(toSell);  // Direct manipulation!
            remaining -= toSell;
        }
    }
    
    // Update balance - might be inconsistent!
    portfolio.setBalance(portfolio.getBalance().add(someAmount));
    
    portfolioPort.savePortfolio(portfolio);
}
```

**What breaks:**

1. **FIFO Logic Duplication:** The FIFO algorithm is now in the service, not in the domain. If business rules change (e.g., switch to LIFO), you must change the service, not the domain model.

2. **Invariant Violation Risk:** What if the service forgets to check `getTotalShares() < quantity`? The portfolio would be in an invalid state.

3. **Balance Inconsistency:** What if the balance update logic doesn't match the actual proceeds calculation? The portfolio becomes corrupted.

4. **No Central Enforcement:** If another use case (e.g., "bulk sell") also needs to sell stocks, it must duplicate all validation and calculation logic.

5. **Testability:** You now need integration tests to verify FIFO works correctly. With the current design, you can unit-test `Holding.sell()` in isolation.

#### ✅ **Correct Pattern: Aggregate Root Protects Invariants**

In the real code:

```java
// Application service: simple orchestration
SellResult sellResult = portfolio.sell(ticker, quantity, price);
```

The `Portfolio` aggregate:
- **Validates** inputs
- **Checks** holdings exist
- **Delegates** to `Holding` (which it controls)
- **Updates** balance consistently
- **Returns** a complete result

**Benefits:**
- All domain rules are in **one place** (the domain model)
- The service cannot corrupt the portfolio state
- Tests can verify invariants in isolation
- Business logic changes are localized

---

### C) Sequence Diagram: Orchestrator vs Aggregate Root

**Diagram Reference:** See [`diagrams/sell-orchestrator-vs-aggregate.puml`](diagrams/sell-orchestrator-vs-aggregate.puml)

This diagram explicitly shows:
- The **Application Service** calling `Portfolio.sell()` (aggregate root)
- The **Portfolio** calling `Holding.sell()` (controlled entity)
- The **Holding** calling `Lot.reduce()` (controlled entity)
- **NO** direct service → Holding communication
- **NO** direct service → Lot communication

---

### D) Teaching Note

> **💡 Key Principle**
>
> **Application services coordinate; aggregates decide.**
>
> The application service is a **traffic controller**. It fetches data, calls the aggregate, and saves results. It does not make business decisions.
>
> The aggregate root is a **consistency boundary**. All changes to entities within the aggregate must go through the root. This ensures invariants are never violated.

---

## 7. Transactionality and Consistency

The application service is annotated with `@Transactional`:

```java
@Transactional
public class PortfolioStockOperationsService 
    implements PortfolioStockOperationsUseCase {
    // ...
}
```

### Why Transactions Matter for Stock Selling

Selling stocks is a **critical financial operation** that must maintain strict consistency guarantees:

1. **Multi-Step Operation:** A single sell involves multiple database writes:
   - Update portfolio balance (add proceeds)
   - Update holding lot quantities (reduce shares via FIFO)
   - Record transaction for audit trail
   
   These changes must **all succeed or all fail together**. Partial updates would corrupt the portfolio state.

2. **Consistency Across Aggregates:** Portfolio state and transaction history must remain synchronized. If the transaction record fails to save, the portfolio changes must be rolled back to prevent discrepancies in financial reporting.

3. **Invariant Protection:** The domain model enforces business rules (e.g., "cannot sell more than you own"), but the transaction boundary ensures these validations and subsequent state changes happen atomically—no other thread can observe an intermediate state.

### ACID Guarantees in Action

Spring's `@Transactional` ensures:

1. **Atomicity:** All database operations (save portfolio, save transaction) succeed or fail together
2. **Consistency:** If the transaction record fails to save, the portfolio changes are rolled back
3. **Isolation:** Concurrent sells on the same portfolio are serialized (preventing race conditions)
4. **Durability:** Once committed, the sale is permanent

**Important separation of concerns:** The domain enforces **business consistency** (invariants, validations), while infrastructure enforces **technical consistency** (ACID properties, transaction boundaries).

### Concurrency Risks in Financial Operations

When multiple users (or concurrent requests from the same user) attempt to sell stocks from the same portfolio simultaneously, several problems can arise without proper synchronization:

**Lost Update Problem:**
- Request 1 reads balance = $1000
- Request 2 reads balance = $1000 (stale)
- Request 1 sells stock, adds $500 proceeds → balance = $1500, commits
- Request 2 sells stock, adds $300 proceeds → calculates $1300 based on stale read, commits
- **Result:** Final balance is $1300, but should be $1800. The first update is lost.

**Double-Spending:**
- Request 1 reads holding: 10 shares available
- Request 2 reads holding: 10 shares available (stale)
- Request 1 sells 10 shares, commits
- Request 2 attempts to sell 10 shares, but only 0 remain
- Without proper isolation, Request 2 might observe an inconsistent intermediate state.

**FIFO Corruption:**
- Two concurrent sells might both attempt to reduce the same lot simultaneously
- Without serialization, lot quantities could become negative or inconsistent
- The aggregate's invariants would be violated mid-operation

### How HexaStock Handles Concurrency

HexaStock uses **database-level transaction isolation** to prevent these issues:

- The `@Transactional` annotation establishes a transaction boundary at the application service level
- Database isolation (typically READ_COMMITTED or higher) ensures that concurrent transactions see consistent snapshots
- For high-contention scenarios, **pessimistic locking** can be applied using JPA's `@Lock(LockModeType.PESSIMISTIC_WRITE)`, which serializes access to specific portfolio rows
- This ensures that when Transaction 1 is processing a sale, Transaction 2 waits until Transaction 1 commits before reading the portfolio

The transaction boundary is intentionally placed at the **application service**, not the domain model, because:
- Domain objects should be technology-agnostic (pure business logic)
- Transaction management is an infrastructure concern
- The service coordinates multiple operations (fetch, execute domain logic, persist, audit) that must succeed or fail atomically

---

> **📖 Deep Dive: Concurrency and Locking**
>
> This tutorial focuses on the architectural and domain design aspects of stock selling. For a **detailed explanation of concurrency control mechanisms**, including:
> - Pessimistic locking with `SELECT ... FOR UPDATE`
> - Optimistic locking with version fields
> - Transaction isolation levels and their trade-offs
> - Race condition demonstrations with real tests
> - When to use which strategy in production financial systems
>
> See the dedicated tutorial: **[Concurrency Control with Pessimistic Database Locking](CONCURRENCY-PESSIMISTIC-LOCKING.md)**

---

## 8. Persistence Mapping

### Domain Model → JPA Entities

The `Portfolio` domain object is mapped to a `PortfolioEntity` (JPA):
- `Portfolio.balance` → `PortfolioEntity.balance`
- `Portfolio.holdings` → `PortfolioEntity.holdings` (one-to-many)

A **mapper** converts between the two:
```java
Portfolio domainPortfolio = PortfolioMapper.toDomain(portfolioEntity);
PortfolioEntity jpaEntity = PortfolioMapper.toEntity(domainPortfolio);
```

### Repositories

- `PortfolioRepository` (JPA) implements `PortfolioPort` (domain interface)
- `TransactionRepository` (JPA) implements `TransactionPort` (domain interface)

This inversion of dependencies is the essence of Hexagonal Architecture: the domain defines **what** it needs (ports), and adapters provide **how** (implementations).

**Diagram Reference:** See [`diagrams/sell-persistence-adapter.puml`](diagrams/sell-persistence-adapter.puml)

<img width="2154" height="1562" alt="image" src="https://github.com/user-attachments/assets/4d8b0c91-e936-41aa-b020-8b7a39c539c0" />


---

## 9. Error Flows

### Error 1: Portfolio Not Found

**Trigger:** Selling from a non-existent portfolio

**Exception:** `PortfolioNotFoundException` (domain exception)

**Code:**
```java
Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
    .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
```

**HTTP Response:**
```json
HTTP 404 Not Found
{
  "title": "Portfolio Not Found",
  "detail": "Portfolio with ID abc-123 not found",
  "status": 404
}
```

**Exception Handler:** The `@RestControllerAdvice` class catches `PortfolioNotFoundException` and converts it to HTTP 404.

**Diagram Reference:** See [`diagrams/sell-error-portfolio-not-found.puml`](diagrams/sell-error-portfolio-not-found.puml)

<img width="2897" height="1570" alt="image" src="https://github.com/user-attachments/assets/6e4cba0e-a52d-4c1c-85f8-9b3ee85e5e5a" />


---

### Error 2: Invalid Quantity

**Trigger:** Selling zero or negative shares

**Exception:** `InvalidQuantityException` (domain exception)

**Code:**
```java
// In Portfolio.sell()
if (quantity <= 0)
    throw new InvalidQuantityException("Quantity must be positive");
```

**HTTP Response:**
```json
HTTP 400 Bad Request
{
  "title": "Invalid Quantity",
  "detail": "Quantity must be positive",
  "status": 400
}
```

**Diagram Reference:** See [`diagrams/sell-error-invalid-quantity.puml`](diagrams/sell-error-invalid-quantity.puml)

<img width="3078" height="1890" alt="image" src="https://github.com/user-attachments/assets/87832f18-b11f-4ba4-960f-0582de36a7ef" />


---

### Error 3: Selling More Than Owned

**Trigger:** Trying to sell 10 shares when you only own 3

**Exception:** `ConflictQuantityException` (domain exception)

**Code:**
```java
// In Holding.sell()
if (getTotalShares() < quantity) {
    throw new ConflictQuantityException("Not enough shares to sell");
}
```

**HTTP Response:**
```json
HTTP 409 Conflict
{
  "title": "Conflict Quantity",
  "detail": "Not enough shares to sell",
  "status": 409
}
```

**Diagram Reference:** See [`diagrams/sell-error-sell-more-than-owned.puml`](diagrams/sell-error-sell-more-than-owned.puml)

<img width="3582" height="2370" alt="image" src="https://github.com/user-attachments/assets/889f9305-297a-49dd-8004-6fe4a34cd928" />


---

## 10. Key Takeaways

### About Hexagonal Architecture

1. **Ports Define Contracts:** The application defines interfaces (ports) that adapters implement. This allows swapping implementations (e.g., change from Finnhub to AlphaVantage for stock prices) without changing the core.

2. **Dependency Inversion:** The controller depends on `PortfolioStockOperationsUseCase` (interface), not on the concrete service. The service depends on `PortfolioPort` (interface), not on the JPA repository.

3. **Testability:** You can test the application service with mock ports. You can test the domain model with no infrastructure at all.

4. **Adapters Are Replaceable:** The REST controller could be replaced with a CLI adapter, a gRPC service, or a message queue consumer—all using the same ports.

### About Domain-Driven Design

1. **Aggregate Roots Protect Boundaries:** `Portfolio` is the aggregate root. All changes to `Holding` and `Lot` must go through the `Portfolio`. This prevents inconsistent states.

2. **Application Services Are Thin:** The service has **no business logic**. It only coordinates. If you see `if` statements, calculations, or validations in a service, you're doing it wrong.

3. **Domain Exceptions Are Business Language:** `ConflictQuantityException` is not a technical exception like `NullPointerException`. It represents a business rule violation. This makes the code self-documenting.

4. **FIFO Is Domain Logic:** The FIFO algorithm is in `Holding.sell()`, not in a service or adapter. This is where it belongs: close to the data it operates on, protected by the aggregate root.

5. **Encapsulation Matters:** The `Portfolio` does not expose a `setBalance()` method. The only way to change the balance is through domain methods like `deposit()`, `withdraw()`, `buy()`, or `sell()`. This prevents corruption.

---

## 11. Summary: The Complete Sell Flow

```
HTTP Request
    ↓
PortfolioRestController (Driving Adapter)
    ↓ calls
PortfolioStockOperationsUseCase (Primary Port / Interface)
    ↓ implemented by
PortfolioStockOperationsService (Application Service)
    ↓ uses
PortfolioPort (Secondary Port / Interface) → fetch portfolio
StockPriceProviderPort (Secondary Port / Interface) → fetch price
    ↓ delegates to
Portfolio.sell() (Aggregate Root - Domain Logic)
    ↓ delegates to
Holding.sell() (Entity - Domain Logic)
    ↓ delegates to
Lot.reduce() (Entity - Domain Logic)
    ↓ returns
SellResult (Value Object)
    ↓ service saves
PortfolioPort.savePortfolio() (Secondary Port)
TransactionPort.save() (Secondary Port)
    ↓ implemented by
JPA Repositories (Driven Adapters)
    ↓ returns
HTTP Response (SaleResponseDTO)
```

---

## 12. Exercises for Students

The following exercises form a progressive learning path designed to deepen your understanding of Hexagonal Architecture and Domain-Driven Design through hands-on work with the HexaStock codebase.

---

### Exercise 1: Trace the Buy Flow
**Type:** Execution Understanding / Documentation

**Goal:** Understand how the `buyStock` use case mirrors the `sellStock` flow.

**What to deliver:**
- A written document (similar to section 5 of this tutorial) that traces the complete execution path for buying stocks
- Include: REST endpoint → Controller → Inbound Port → Application Service → Domain Model → Persistence
- Identify which classes validate business rules and where ACID guarantees are enforced
- Note one key difference between buy and sell operations

---

### Exercise 2: Identify Aggregate Boundaries
**Type:** Reasoning / Explanation

**Goal:** Understand why Portfolio is the aggregate root and what it protects.

**What to deliver:**
- A written explanation (300-500 words) answering:
  - Why is `Portfolio` the aggregate root instead of `Holding` or `Lot`?
  - What invariants would break if `Holding` were exposed as a separate aggregate?
  - Why must balance updates and holding modifications happen together atomically?
- Use concrete examples from the sell operation to support your reasoning

---

### Exercise 3: Map Domain Exceptions to HTTP Status Codes
**Type:** Reasoning / Design

**Goal:** Understand how domain exceptions become HTTP responses.

**What to deliver:**
- A table mapping each domain exception to its appropriate HTTP status code
- For each mapping, explain WHY that status code is correct (not just "because that's what the code does")

---

### Exercise 4: Explain the Role of @Transactional
**Type:** Reasoning / Explanation

**Goal:** Understand when and why Spring transactions are needed.

**What to deliver:**
- A written explanation answering:
  - Why is `@Transactional` on the application service, not the domain model?
  - What would happen if `portfolioPort.savePortfolio()` succeeds but `transactionPort.save()` fails?
  - Could the domain model enforce ACID guarantees itself? Why or why not?
- Propose a scenario where transaction management might fail and explain the consequences

---

## Exercise 5: Add a Maximum Sell Percentage Invariant

**Type:** Mixed (Design + Coding + Reasoning)
**Goal:** Implement a non-trivial business invariant using Domain-Driven Design principles.

---

## Business Rules

In a single sell transaction, a portfolio must respect the following rules **per holding (per ticker)**:

### Rule 1 — Small sells are always allowed

A portfolio may sell **up to 10 shares** of a holding **without any percentage restriction**, as long as enough shares exist.

### Rule 2 — Large sells are limited

When selling **more than 10 shares** in a single transaction, the portfolio **cannot sell more than 50% of the shares of the affected holding**.

The percentage is calculated using the number of shares **held before the sale**.

> **Formal rule:**
>
> - If `sharesToSell <= 10` -> allowed
> - If `sharesToSell > 10` -> must satisfy: 
>   sharesToSell <= holdingSharesBefore * 0.50
>   


---

## Clarifications

* The rule applies **per holding (per ticker)**, not to the whole portfolio.
* The rule is **not** evaluated per lot.
* The invariant must be checked **before any state change occurs**.

---

## Examples (AAPL)

### Example 1 — Valid (✅ small sell)

* AAPL holding has **3 shares**
* Sell request: **1 share**

Result: allowed.

---

### Example 2 — Valid (✅ boundary case)

* AAPL holding has **12 shares**
* Sell request: **10 shares**

Result: allowed.

---

### Example 3 — Valid (✅ large sell within limit)

* AAPL holding has **22 shares**
* Sell request: **11 shares**

50% of 22 = 11 → allowed.

---

### Example 4 — Invalid (❌ large sell exceeding limit)

* AAPL holding has **20 shares**
* Sell request: **11 shares**

50% of 20 = 10 → not allowed.

Result: throw `ExcessiveSaleException`.
No state must change.

---

## What to Deliver

### 1. Design Decision (written explanation)

Decide **where this invariant should be implemented**:

* `PortfolioRestController`
* `PortfolioStockOperationsService`
* `Portfolio.sell()`
* `Holding.sell()`

Justify your choice using DDD concepts:

* Aggregate boundaries
* Invariants
* Encapsulation of business rules

---

### 2. Implementation (code)

* Enforce the rule in the appropriate domain class
* Introduce a new domain exception: `ExcessiveSaleException`
* Ensure the invariant is validated **before any mutation**

---

### 3. Test (code)

Write at least some tests proving:

* Selling **10 or fewer** shares always succeeds (if shares exist)
* Selling **more than 10** shares succeeds only if it is **≤ 50%** of the holding
* Selling **more than 10** shares and **exceeding 50%** fails with `ExcessiveSaleException`
* Tests run **without infrastructure**

---

### 4. Reflection (written)

* How would you support a future requirement where the 50% limit is **configurable per portfolio**?
* Would that change **where the invariant lives**? Why or why not?

---

### Exercise 6: Distinguish Value Objects from Entities
**Type:** Reasoning / Explanation

**Goal:** Understand the difference between entities and value objects in DDD.

**What to deliver:**
- A written explanation (400-600 words) analyzing:
  - Why is `Ticker` a value object while `Lot` is an entity?
  - Why is `Money` a value object while `Portfolio` is an entity?
  - What would happen if `SellResult` had an ID and was persisted as an entity?
- Propose converting `Ticker` into an entity with validation rules (e.g., must be uppercase, 1-5 characters). Would this be a good design? Why or why not?

---

### Exercise 7: Add a Third Stock Price Provider Adapter (Prove the Hexagon Works)

**Type:** Coding + Architecture Validation (Driven Adapter / Outbound Port)
**Goal:** Implement a **new outbound adapter** for market data that plugs into the existing port:

* `cat/gencat/agaur/hexastock/application/port/out/StockPriceProviderPort.java`

…and demonstrate that the **core of the system (domain + application services + REST controllers)** remains unchanged.

---

#### Context (what already exists in HexaStock)

HexaStock already has **two** implementations of the same outbound port (`StockPriceProviderPort`), each calling a different external provider:

* **Finnhub adapter**
* **AlphaVantage adapter**

They are both **driven adapters** (outbound): the application calls them through the port, and the adapter calls an external HTTP API.

Your task is to add a **third adapter**, using a different provider, with the same contract and behavior.

---

## Provider Options (examples)

Pick **one** provider that offers a free tier or freemium plan. You may choose any provider you find online, but here are common options:
* **https://site.financialmodelingprep.com/**
* **Twelve Data**
* **Marketstack**
* **Financial Modeling Prep (FMP)**
* **IEX Cloud** (often limited free tier)
* **Alpaca Market Data**

You can also pick another provider not listed here, as long as:

* it exposes a "latest price" endpoint,
* it authenticates via API key,
* it returns data you can map to your domain `StockPrice` model.

---

## What to deliver

### 1) Implement the new adapter class (and its package)

Create a new package under `adapter.out`, for example:

* `cat.gencat.agaur.hexastock.adapter.out.twelvedata`
* or `...adapter.out.marketstack`
* or `...adapter.out.fmp`

Then implement the port:

```java
package cat.gencat.agaur.hexastock.adapter.out.twelvedata;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.StockPrice;

public class TwelveDataStockPriceProviderAdapter implements StockPriceProviderPort {

    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // 1) Call provider HTTP API
        // 2) Parse JSON response
        // 3) Map to domain object StockPrice
        // 4) Handle errors/rate limits in a consistent way
        throw new UnsupportedOperationException("TODO");
    }
}
```

**Strict rule:**
✅ You may add new classes in the adapter layer
❌ You must NOT change the port interface
❌ You must NOT change the use case (`PortfolioStockOperationsService`)
❌ You must NOT change the domain (`Portfolio`, `Holding`, `Lot`)
❌ You must NOT change the REST controller

This is the point of the exercise: **only infrastructure changes**.

---

### 2) Add configuration to select the provider

Make it possible to switch providers without touching the core code. Use one of these approaches:

**Option A: Spring Profiles (recommended for teaching)**

* `application-finnhub.properties`
* `application-alphavantage.properties`
* `application-twelvedata.properties`

Then activate via:

* `-Dspring.profiles.active=twelvedata`

**Option B: Property-based selection**

* `stock.price.provider=twelvedata`

Then create conditional beans.

Your final result must allow:

* Finnhub (existing)
* AlphaVantage (existing)
* Your new provider (new)

---

### 3) API key management (free-tier ready)

* Store the API key outside code:

    * environment variable, or
    * profile properties file.
* If the key is missing, fail fast with a clear error message.

---

### 4) Error handling contract (keep behavior consistent)

Your adapter must handle, at minimum:

* invalid ticker / symbol not found,
* rate limit exceeded (HTTP 429 or provider-specific message),
* provider downtime or network error.

**Deliverable:** a short note describing how your adapter translates those cases into exceptions used by the application (or a consistent exception strategy already present in the codebase).

---

### 5) Tests (prove the adapter works without breaking the hexagon)

Write one of these:

**Option A (strongly recommended): Adapter unit test with mocked HTTP**

* Use WireMock / MockWebServer
* Verify:

    * correct URL is called,
    * ticker is passed correctly,
    * response JSON is mapped correctly to `StockPrice`.

**Option B: Run the existing sell integration test with your adapter**

* Run `PortfolioRestControllerIntegrationTest` (or equivalent)
* Switch profile to your adapter
* Show that the **same sell flow works** (controller → service → domain → port → adapter)

---

## Proof of Hexagonal Architecture (mandatory explanation)

Write a short explanation (8–12 lines) answering:

1. What changed in the codebase?
2. What did not change? (name concrete packages/classes)
3. Why does the port make this possible?

**Expected conclusion:**

> We replaced a driven adapter (infrastructure) while keeping the domain and application core intact, proving that Hexagonal Architecture isolates the core from external dependencies.

---

## Extra Challenge (optional)

Add a small "provider comparison" markdown note:

* which endpoint you used,
* whether the free tier provides real-time or delayed price,
* what the call limits are.

---

**Success criteria:** You can sell stocks using your new provider by changing only configuration (profile/property). The use case and domain behave exactly the same because they depend only on `StockPriceProviderPort`, not on the external API.

---

**End of Exercises**

Work through these exercises in order. Each builds on concepts from earlier exercises. Discuss your solutions with peers and instructors to deepen your understanding of Hexagonal Architecture and Domain-Driven Design.

---

## 13. References

- **API Specification:** `doc/stock-portfolio-api-specification.md`
- **Integration Tests:** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestControllerIntegrationTest.java`
- **Domain Tests:** `src/test/java/cat/gencat/agaur/hexastock/model/PortfolioTest.java`
- **Source Code:** `src/main/java/cat/gencat/agaur/hexastock/`

---
