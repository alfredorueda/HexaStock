# Selling Stocks in HexaStock: A Hexagonal Architecture and DDD Tutorial

## 1. Purpose and Learning Objectives

This tutorial reverse-engineers a **real use case** from the HexaStock codebase: **selling stocks from a portfolio**. By following the actual execution path through real code, you will learn:

- How **Hexagonal Architecture** separates concerns between adapters, ports, and domain logic
- How **Domain-Driven Design (DDD)** protects business invariants through aggregate roots
- Why application services **orchestrate** while aggregates **decide**
- How FIFO accounting is implemented at the domain level
- How domain exceptions translate to HTTP responses

**Critical Note:** Everything in this tutorial references verified code. We do not invent or assume—every class, method, and responsibility described exists in this repository.

---

## 2. Domain Context: What "Selling Stocks" Means in HexaStock

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

**Diagram Reference:** See `diagrams/sell-http-to-port.puml`

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

**Diagram Reference:** See `diagrams/sell-application-service.puml`

### Step 4: Domain Model Enforces Invariants

**File:** `model.Portfolio` (Aggregate Root)

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

**Diagram Reference:** See `diagrams/sell-domain-fifo.puml`

### Step 5: Persistence Adapter Saves Changes

The `PortfolioPort` implementation (a JPA adapter) converts the domain `Portfolio` into JPA entities and persists them.

**Diagram Reference:** See `diagrams/sell-persistence-adapter.puml`

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

### C) Codebase Reality Check: Is Encapsulation Properly Enforced?

Let me inspect the actual domain model:

**File:** `model.Portfolio`

```java
public List<Holding> getHoldings() {
    return new ArrayList<>(holdings.values());
}
```

✅ **Good:** Returns a **copy** of the holdings list, not the internal map. External code cannot modify the portfolio's holdings directly.

**File:** `model.Holding`

```java
public List<Lot> getLots() {
    return lots;
}
```

⚠️ **Potential Issue:** This returns the **actual internal list**, not a copy. External code could theoretically modify the lot list directly:

```java
holding.getLots().clear();  // Would break the holding!
```

#### Possible Improvements

**Option 1: Return an unmodifiable view**
```java
public List<Lot> getLots() {
    return Collections.unmodifiableList(lots);
}
```

**Option 2: Return a defensive copy**
```java
public List<Lot> getLots() {
    return new ArrayList<>(lots);
}
```

**Option 3: Encapsulate completely (best for strict DDD)**
```java
// Remove getLots() entirely
// Add domain-specific query methods instead:
public int getTotalShares() {
    return lots.stream().mapToInt(Lot::getRemaining).sum();
}

public boolean hasEnoughShares(int quantity) {
    return getTotalShares() >= quantity;
}
```

**Current Risk Level:** **Low**. The application service in this codebase does not call `getLots()` to manipulate lots. It correctly delegates to `portfolio.sell()`. However, for teaching purposes, this is a good example of how even in a well-designed system, there's room for improvement.

---

### D) Sequence Diagram: Orchestrator vs Aggregate Root

**Diagram Reference:** See `diagrams/sell-orchestrator-vs-aggregate.puml`

This diagram explicitly shows:
- The **Application Service** calling `Portfolio.sell()` (aggregate root)
- The **Portfolio** calling `Holding.sell()` (controlled entity)
- The **Holding** calling `Lot.reduce()` (controlled entity)
- **NO** direct service → Holding communication
- **NO** direct service → Lot communication

---

### E) Teaching Note

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

This ensures:
1. **Atomicity:** All database operations (save portfolio, save transaction) succeed or fail together
2. **Consistency:** If the transaction record fails to save, the portfolio changes are rolled back
3. **Isolation:** Concurrent sells on the same portfolio are serialized
4. **Durability:** Once committed, the sale is permanent

**ACID guarantees** are handled by Spring's transaction management, not by the domain model. This is correct separation of concerns: the domain enforces **business consistency**, while infrastructure enforces **technical consistency**.

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

**Diagram Reference:** See `diagrams/sell-persistence-adapter.puml`

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

**Diagram Reference:** See `diagrams/sell-error-portfolio-not-found.puml`

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

**Diagram Reference:** See `diagrams/sell-error-invalid-quantity.puml`

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

**Diagram Reference:** See `diagrams/sell-error-sell-more-than-owned.puml`

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
- Identify one exception that you think has an incorrect mapping and propose an alternative with justification

---

### Exercise 4: Test FIFO Across Multiple Lots
**Type:** Coding / Testing

**Goal:** Verify that FIFO accounting works correctly in the domain model.

**What to deliver:**
- A unit test for `Holding.sell()` with the following scenario:
  - Buy 10 shares at $100 on Jan 1
  - Buy 5 shares at $120 on Feb 1
  - Buy 8 shares at $110 on Mar 1
  - Sell 12 shares at $150
- Assert that:
  - The correct lots are reduced
  - Cost basis is calculated from the first two lots only
  - Profit/loss is correct
- The test must run without any infrastructure (no database, no Spring context)

---

### Exercise 5: Explain the Role of @Transactional
**Type:** Reasoning / Explanation

**Goal:** Understand when and why Spring transactions are needed.

**What to deliver:**
- A written explanation answering:
  - Why is `@Transactional` on the application service, not the domain model?
  - What would happen if `portfolioPort.savePortfolio()` succeeds but `transactionPort.save()` fails?
  - Could the domain model enforce ACID guarantees itself? Why or why not?
- Propose a scenario where transaction management might fail and explain the consequences

---

### Exercise 6: Add a Maximum Sell Percentage Invariant
**Type:** Mixed (Design + Coding + Reasoning)

**Goal:** Implement a critical business rule using DDD principles.

**Business Rule:** A portfolio cannot sell more than 50% of the shares of a holding in a single transaction.

**What to deliver:**

1. **Design Decision (written explanation):**
   - WHERE should this rule be implemented? Options:
     - In `PortfolioRestController`
     - In `PortfolioStockOperationsService`
     - In `Portfolio.sell()`
     - In `Holding.sell()`
   - Justify your choice using DDD concepts: aggregate boundaries, invariants, encapsulation
   - Explain what could go wrong if implemented in the application service instead

2. **Implementation (code):**
   - Add the validation to the appropriate class
   - Throw a new domain exception: `ExcessiveSaleException`
   - Ensure the rule is enforced BEFORE any state changes

3. **Test (code):**
   - Write at least one domain-level unit test verifying:
     - Selling 50% or less succeeds
     - Selling 51% fails with `ExcessiveSaleException`
     - The test runs without infrastructure

4. **Reflection (written):**
   - How would you handle a requirement to make the percentage configurable per portfolio?
   - Would that change where the rule lives? Why or why not?

---

### Exercise 7: Improve Encapsulation in Holding
**Type:** Coding / Refactoring

**Goal:** Fix the encapsulation violation identified in section 6.C of this tutorial.

**What to deliver:**
- Refactor `Holding.getLots()` to return an unmodifiable list
- Verify that all existing tests still pass
- Identify any code that directly calls `getLots()` and assess whether it violates aggregate boundaries
- If violations exist, propose a refactoring strategy (you don't need to implement it, just describe it)

---

### Exercise 8: Mock Ports to Test the Application Service
**Type:** Coding / Testing

**Goal:** Test the orchestration logic in isolation.

**What to deliver:**
- A unit test for `PortfolioStockOperationsService.sellStock()` that:
  - Mocks `PortfolioPort`, `StockPriceProviderPort`, and `TransactionPort`
  - Verifies the service calls each port in the correct order
  - Verifies the service delegates to `portfolio.sell()` with the fetched price
  - Verifies the service saves both the portfolio and the transaction
- Use Mockito or a similar mocking framework
- The test must NOT start a Spring context

---

### Exercise 9: Distinguish Value Objects from Entities
**Type:** Reasoning / Explanation

**Goal:** Understand the difference between entities and value objects in DDD.

**What to deliver:**
- A written explanation (400-600 words) analyzing:
  - Why is `Ticker` a value object while `Lot` is an entity?
  - Why is `Money` a value object while `Portfolio` is an entity?
  - What would happen if `SellResult` had an ID and was persisted as an entity?
- Propose converting `Ticker` into an entity with validation rules (e.g., must be uppercase, 1-5 characters). Would this be a good design? Why or why not?

---

### Exercise 10: Add a New Inbound Port for Bulk Operations
**Type:** Design + Coding

**Goal:** Extend the hexagonal architecture with a new use case.

**Business Requirement:** Support selling shares of multiple tickers in a single API call.

**What to deliver:**

1. **Port Definition (code):**
   - Create `BulkStockOperationsUseCase` interface with:
     ```java
     Map<Ticker, SellResult> sellMultiple(String portfolioId, Map<Ticker, Integer> sales);
     ```

2. **Service Implementation (code):**
   - Implement the port in a new application service
   - Reuse the existing `Portfolio.sell()` method for each ticker
   - Ensure the entire operation is transactional (all succeed or all fail)

3. **REST Endpoint (code):**
   - Add a new controller method: `POST /api/portfolios/{id}/bulk-sales`
   - Request body: `{"sales": [{"ticker": "AAPL", "quantity": 5}, ...]}`

4. **Design Reflection (written):**
   - Why is it better to create a new port rather than modify `PortfolioStockOperationsUseCase`?
   - How does this demonstrate the Open/Closed Principle?

---

### Exercise 11: Add a Command-Line Adapter
**Type:** Coding / Hexagonal Architecture

**Goal:** Demonstrate that ports enable multiple adapters.

**What to deliver:**
- Create a `CommandLinePortfolioAdapter` class that:
  - Uses `PortfolioStockOperationsUseCase` (the same port used by the REST controller)
  - Reads commands from `System.in` (e.g., "sell abc-123 AAPL 5")
  - Prints results to `System.out`
  - Handles exceptions gracefully
- Write a main method that demonstrates:
  - Creating a portfolio
  - Depositing funds
  - Buying stocks
  - Selling stocks
- Explain in a comment: How many lines of business logic did you need to write? Why so few?

---

### Exercise 12: Design a New Outbound Port for Market Data
**Type:** Design + Reasoning

**Goal:** Understand how outbound ports abstract external dependencies.

**Scenario:** The business wants to support multiple stock price providers (Finnhub, AlphaVantage, Yahoo Finance) and switch between them at runtime.

**What to deliver:**

1. **Current Analysis (written):**
   - How is `StockPriceProviderPort` currently implemented?
   - What would need to change to support multiple providers?

2. **Design Proposal (written + code interfaces):**
   - Design a strategy pattern for multiple providers
   - Define any new interfaces or classes needed
   - Explain how to make the provider choice configurable (e.g., via application.properties)

3. **Trade-off Analysis (written):**
   - What are the benefits of this design?
   - What are the costs (complexity, maintenance)?
   - When would you NOT recommend this abstraction?

---

### Exercise 13: Refactor to Remove Empty Lots
**Type:** Coding + Domain Modeling

**Goal:** Improve the domain model based on business requirements.

**Business Requirement:** After selling all shares from a lot, the lot should be removed from the holding to avoid cluttering the database.

**What to deliver:**

1. **Implementation (code):**
   - Modify `Holding.sell()` to remove lots with `remaining == 0`
   - Ensure FIFO order is preserved

2. **Testing (code):**
   - Write a test that sells all shares from a holding across multiple lots
   - Assert that empty lots are removed
   - Assert that the holding itself is removed from the portfolio if all lots are gone

3. **Impact Analysis (written):**
   - Does this change affect the aggregate boundary? Why or why not?
   - Could this operation be done by the application service instead? What would be the risks?
   - How does this change affect transaction history queries?

---

### Exercise 14: Add Tax Lot Identification
**Type:** Advanced Domain Modeling

**Goal:** Extend the domain model with a more complex accounting method.

**Business Requirement:** Support "specific identification" accounting where investors can choose which lots to sell (instead of FIFO).

**What to deliver:**

1. **Design Proposal (written):**
   - How would you modify the aggregate to support both FIFO and specific lot selection?
   - Should the choice be per-portfolio, per-sale, or globally configured?
   - How would you represent "which lots to sell" in the API?

2. **Domain Model Changes (code or pseudocode):**
   - Modify `Portfolio.sell()` or create a new method `sellSpecificLots()`
   - Show how the method signature would change
   - Sketch the validation logic (you don't need to implement the full algorithm)

3. **Trade-off Discussion (written):**
   - What invariants become harder to enforce?
   - How does this affect testability?
   - Would you recommend this feature? Under what conditions?

---

### Exercise 15: Evaluate Aggregate Redesign for Scalability
**Type:** Advanced Reasoning / Architecture

**Goal:** Critically analyze when aggregate boundaries should change.

**Scenario:** The portfolio system is growing. Some portfolios have 1000+ holdings with 10,000+ lots. Loading the entire aggregate is slow.

**What to deliver:**

1. **Problem Analysis (written):**
   - Why is the current aggregate design problematic at scale?
   - What specific operations become slow?
   - How does the aggregate boundary contribute to the problem?

2. **Alternative Design (written):**
   - Propose splitting `Portfolio` into multiple aggregates
   - Define new aggregate roots and their boundaries
   - Explain how cross-aggregate operations (e.g., selling from multiple holdings) would work
   - Discuss eventual consistency trade-offs

3. **Decision Framework (written):**
   - Under what conditions should you keep the current design?
   - At what scale would you recommend the redesign?
   - How would you migrate existing data?
   - What tests would you write to ensure the redesign preserves correctness?

4. **DDD Reflection (written):**
   - Is it acceptable to change aggregate boundaries based on technical concerns?
   - How do you balance DDD purity with pragmatic performance needs?
   - What would Eric Evans say about this decision?

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

**This tutorial was generated by reverse-engineering the actual HexaStock codebase. Every code snippet, class name, and architectural decision is real and verified.**
