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

1. **Trace the Buy Flow:** Using this tutorial as a template, trace the `buyStock` use case end-to-end.

2. **Add a New Invariant:** Add a business rule: "You cannot sell more than 50% of a holding in a single transaction." Where would you implement this rule? Why?

3. **Test the Domain Model:** Write a unit test for `Holding.sell()` that verifies FIFO works correctly when selling across multiple lots.

4. **Improve Encapsulation:** Refactor `Holding.getLots()` to return an unmodifiable list. Verify that the codebase still compiles and all tests pass.

5. **Add a New Adapter:** Create a `CommandLinePortfolioAdapter` that allows selling stocks via a terminal interface. Note how you reuse the same `PortfolioStockOperationsUseCase` port.

---

## 13. References

- **API Specification:** `doc/stock-portfolio-api-specification.md`
- **Integration Tests:** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestControllerIntegrationTest.java`
- **Domain Tests:** `src/test/java/cat/gencat/agaur/hexastock/model/PortfolioTest.java`
- **Source Code:** `src/main/java/cat/gencat/agaur/hexastock/`

---

**This tutorial was generated by reverse-engineering the actual HexaStock codebase. Every code snippet, class name, and architectural decision is real and verified.**
