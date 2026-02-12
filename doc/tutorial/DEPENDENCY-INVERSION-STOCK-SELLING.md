# Dependency Inversion Principle in Stock Selling: A Real Implementation Analysis

## Table of Contents
- [Responsibility of the Stock Selling Service](#responsibility-of-the-stock-selling-service)
- [Port / Interface Used to Obtain the Stock Price](#port--interface-used-to-obtain-the-stock-price)
- [Concrete Adapters](#concrete-adapters)
  - [1. FinhubStockPriceAdapter](#1-finhubstockpriceadapter)
  - [2. AlphaVantageStockPriceAdapter](#2-alphavantagestockpriceadapter)
- [Full Execution Flow of a Stock Sale](#full-execution-flow-of-a-stock-sale)
  - [Step 1: HTTP Request Arrives at the Driving Adapter](#step-1-http-request-arrives-at-the-driving-adapter)
  - [Step 2: Primary Port Delegates to Application Service](#step-2-primary-port-delegates-to-application-service)
  - [Step 3: Service Retrieves Portfolio from Persistence](#step-3-service-retrieves-portfolio-from-persistence)
  - [Step 4: Service Fetches Current Stock Price](#step-4-service-fetches-current-stock-price)
  - [Step 5: Service Delegates Business Logic to Domain Model](#step-5-service-delegates-business-logic-to-domain-model)
  - [Step 6: Service Persists Updated Portfolio](#step-6-service-persists-updated-portfolio)
  - [Step 7: Service Records Transaction for Audit](#step-7-service-records-transaction-for-audit)
  - [Step 8: Service Returns Result to Controller](#step-8-service-returns-result-to-controller)
  - [Step 9: Controller Converts to DTO and Returns HTTP Response](#step-9-controller-converts-to-dto-and-returns-http-response)
- [Why This Satisfies Dependency Inversion](#why-this-satisfies-dependency-inversion)
  - [1. The Application Service is a High-Level Module](#1-the-application-service-is-a-high-level-module)
  - [2. The Service Depends Only on Abstractions](#2-the-service-depends-only-on-abstractions)
  - [3. Low-Level Modules (Adapters) Implement High-Level Abstractions](#3-low-level-modules-adapters-implement-high-level-abstractions)
  - [4. Abstractions Do Not Depend on Details](#4-abstractions-do-not-depend-on-details)
  - [5. Spring Dependency Injection Resolves the Concrete Implementation](#5-spring-dependency-injection-resolves-the-concrete-implementation)
- [Practical Benefits (Testability, Extensibility)](#practical-benefits-testability-extensibility)
  - [Benefit 1: Testability Without Infrastructure](#benefit-1-testability-without-infrastructure)
  - [Benefit 2: Switching Stock Price Providers Without Code Changes](#benefit-2-switching-stock-price-providers-without-code-changes)
  - [Benefit 3: Adding a New Stock Price Provider is Isolated](#benefit-3-adding-a-new-stock-price-provider-is-isolated)
  - [Benefit 4: Domain Model Remains Pure](#benefit-4-domain-model-remains-pure)
  - [Benefit 5: Improved Error Handling and Resilience](#benefit-5-improved-error-handling-and-resilience)
  - [Benefit 6: Regulatory Compliance and Auditing](#benefit-6-regulatory-compliance-and-auditing)
- [Summary](#summary)

---

## Responsibility of the Stock Selling Service

The stock selling use case is implemented by the `PortfolioStockOperationsService` class, located in the package `cat.gencat.agaur.hexastock.application.service`.

This service is responsible for **orchestrating** the stock selling operation by coordinating multiple secondary adapters and the domain model. Specifically, when selling stocks, the service:

1. **Retrieves the portfolio** from persistence using `PortfolioPort`
2. **Fetches the current stock price** from an external market data provider using `StockPriceProviderPort`
3. **Delegates the business logic** to the domain model (the `Portfolio` aggregate root)
4. **Persists the updated portfolio** back to the database
5. **Records the transaction** for audit purposes using `TransactionPort`

Here is the actual implementation:

```java
@Override
public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
    
    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
    Price price = stockPrice.price();
    
    SellResult sellResult = portfolio.sell(ticker, quantity, price);
    
    portfolioPort.savePortfolio(portfolio);
    
    Transaction transaction = Transaction.createSale(
        portfolioId, ticker, quantity, price,
        sellResult.proceeds(), sellResult.profit());
    transactionPort.save(transaction);
    
    return sellResult;
}
```

Notice what this service **does not do**:
- ❌ It does NOT connect directly to Finnhub, Alpha Vantage, or any specific API
- ❌ It does NOT know which database technology is being used (JPA, MongoDB, etc.)
- ❌ It does NOT contain FIFO accounting logic or profit/loss calculations
- ❌ It does NOT handle HTTP requests or JSON serialization

The service depends exclusively on **abstractions** (interfaces/ports), never on concrete implementations. This is the essence of the Dependency Inversion Principle.

> **💡 Value Objects in the Port Contract:** Notice how the method signature uses `PortfolioId`, `Ticker`, and `ShareQuantity` instead of `String`, `String`, and `int`. This makes the contract self-documenting and type-safe—you cannot accidentally swap a portfolio ID for a ticker symbol.

---

## Port / Interface Used to Obtain the Stock Price

The application service depends on the `StockPriceProviderPort` interface, which is defined in the package `cat.gencat.agaur.hexastock.application.port.out`.

This is a **secondary port** (also called an output port or driven port) that defines the contract for obtaining stock price information:

```java
public interface StockPriceProviderPort {
    /**
     * Fetches the current price for a given stock ticker.
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice record containing the ticker, price, and timestamp
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    StockPrice fetchStockPrice(Ticker ticker);
    
    /**
     * Fetches the current price for each given stock ticker.
     *
     * @param sTickers The set of ticker symbols to get prices for
     * @return A Map containing the StockPrice for each Ticker
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    default Map<Ticker, StockPrice> fetchStockPrice(Set<Ticker> sTickers) {
        return sTickers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::fetchStockPrice
                ));
    }
}
```

**Key characteristics of this port:**
- **Defined in the application layer**, not in the infrastructure layer
- Uses **domain Value Objects** (`Ticker`, `StockPrice`, `Price`) as parameters and return types, never infrastructure-specific types (no JSON, no HTTP)
- The `StockPrice` record contains a `Ticker`, a `Price` value object, and an `Instant`—all domain concepts
- Represents a **capability** the application needs, without specifying how that capability is implemented
- Multiple implementations can exist simultaneously, activated by different Spring profiles

This interface is the abstraction that allows the service to remain decoupled from external providers.

---

## Concrete Adapters

The HexaStock project currently provides **two concrete implementations** of the `StockPriceProviderPort`, both located in the package `cat.gencat.agaur.hexastock.adapter.out.rest`:

### 1. FinhubStockPriceAdapter

This adapter connects to the **Finnhub API** to retrieve real-time stock prices.

**Location:** `cat.gencat.agaur.hexastock.adapter.out.rest.FinhubStockPriceAdapter`

**Activation:** Only active when the Spring profile `finhub` is enabled

**Key implementation details:**

```java
@Component
@Profile("finhub")
public class FinhubStockPriceAdapter implements StockPriceProviderPort {
    
    @Value("${finhub.api.key}")
    private String finhubApiKey;
    
    @Value("${finhub.api.url}")
    private String finhubApiUrl;
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        throttle(); // Rate limiting for free tier
        
        String url = String.format("%s/quote?symbol=%s&token=%s", 
            finhubApiUrl, ticker.value(), finhubApiKey);
        
        RestClient restClient = RestClient.builder()
                .baseUrl(finhubApiUrl)
                .build();
        
        JsonNode quoteJson = restClient.get()
            .uri(url)
            .retrieve()
            .body(JsonNode.class);
        
        if (quoteJson == null || quoteJson.get("c") == null) {
            throw new ExternalApiException("Invalid response from Finnhub API");
        }
        
        double currentPrice = quoteJson.get("c").asDouble();
        
        return new StockPrice(
            ticker,
            Price.of(currentPrice),
            Instant.now());
    }
}
```

This adapter handles:
- HTTP communication using Spring's `RestClient`
- JSON parsing to extract the price from Finnhub's response format
- Error handling specific to the Finnhub API
- Rate limiting to avoid exceeding free-tier quotas
- Conversion from Finnhub's raw data into domain Value Objects (`Price.of(currentPrice)`, `StockPrice` record)

### 2. AlphaVantageStockPriceAdapter

This adapter connects to the **Alpha Vantage API** as an alternative stock price provider.

**Location:** `cat.gencat.agaur.hexastock.adapter.out.rest.AlphaVantageStockPriceAdapter`

**Activation:** Only active when the Spring profile `alphaVantage` is enabled

**Key implementation details:**

```java
@Component
@Profile("alphaVantage")
public class AlphaVantageStockPriceAdapter implements StockPriceProviderPort {
    
    @Value("${alphaVantage.api.key}")
    private String apiKey;
    
    @Value("${alphaVantage.api.base-url}")
    private String baseUrl;
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        throttle(); // Rate limiting for free tier
        
        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", 
            baseUrl, ticker.value(), apiKey);
        
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        
        JsonNode responseJson = restClient.get()
            .uri(url)
            .retrieve()
            .body(JsonNode.class);
        
        if (responseJson == null || 
            responseJson.get("Global Quote") == null || 
            responseJson.get("Global Quote").get("05. price") == null) {
            throw new ExternalApiException("Invalid response from Alpha Vantage API");
        }
        
        double currentPrice = Double.parseDouble(
            responseJson.get("Global Quote").get("05. price").asText());
        
        return new StockPrice(
            ticker,
            Price.of(currentPrice),
            Instant.now());
    }
}
```

Notice that despite calling different APIs with different response formats:
- Both adapters implement the **same interface** (`StockPriceProviderPort`)
- Both return the **same domain record** (`StockPrice` containing `Ticker`, `Price`, and `Instant`)
- Both handle their provider-specific details **internally**
- Both construct domain Value Objects (`Price.of(...)`) from raw primitive data at the adapter boundary

The application core has no knowledge of which adapter is being used at runtime.

---

## Full Execution Flow of a Stock Sale

When a client makes a request to sell stocks, the execution flows through multiple layers following hexagonal architecture principles:

### Step 1: HTTP Request Arrives at the Driving Adapter

**Component:** `PortfolioRestController` (package: `cat.gencat.agaur.hexastock.adapter.in`)

```java
@PostMapping("/{id}/sales")
public ResponseEntity<SaleResponseDTO> sellStock(
        @PathVariable String id, 
        @RequestBody SaleRequestDTO request) {
    
    SellResult result = portfolioStockOperationsUseCase.sellStock(
        PortfolioId.of(id),
        Ticker.of(request.ticker()),
        ShareQuantity.positive(request.quantity()));
    
    return ResponseEntity.ok(
        new SaleResponseDTO(id, request.ticker(), request.quantity(), result));
}
```

**Responsibilities:**
- Receives the HTTP POST request at `/api/portfolios/{id}/sales`
- Extracts path variables (`id`) and request body (`ticker`, `quantity`)
- Converts HTTP-specific primitives into domain **Value Objects** (`PortfolioId.of(id)`, `Ticker.of(...)`, `ShareQuantity.positive(...)`)
- `ShareQuantity.positive(...)` validates that the quantity is greater than zero at the adapter boundary
- Calls the **primary port** (`PortfolioStockOperationsUseCase`), not the concrete service
- Converts the domain result (`SellResult` containing `Money` values) into a DTO for HTTP response

**Dependency direction:** The controller depends on the `PortfolioStockOperationsUseCase` **interface**, not on `PortfolioStockOperationsService` implementation.

### Step 2: Primary Port Delegates to Application Service

**Component:** `PortfolioStockOperationsUseCase` interface (package: `cat.gencat.agaur.hexastock.application.port.in`)

This is a **primary port** (input port or driving port) that defines the use case contract:

```java
public interface PortfolioStockOperationsUseCase {
    SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
}
```

> **💡 Type-safe contract:** The port uses `PortfolioId`, `Ticker`, and `ShareQuantity` instead of `String`, `String`, and `int`. This makes it impossible to accidentally swap parameters—the compiler enforces correctness.

**Implementation:** `PortfolioStockOperationsService` (package: `cat.gencat.agaur.hexastock.application.service`)

The service is annotated with `@Transactional` to ensure ACID guarantees across all operations in this use case.

### Step 3: Service Retrieves Portfolio from Persistence

```java
Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
    .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
```

**Dependency:** `PortfolioPort` (secondary port/interface)

**Actual implementation:** A JPA repository adapter (`JpaPortfolioRepository`) in the infrastructure layer

**What happens:**
- The service calls the port method `getPortfolioById(PortfolioId)`
- The JPA adapter extracts the `String` value from `PortfolioId` and queries the database
- The adapter converts JPA entities to domain objects, wrapping primitives back into Value Objects (`Money`, `ShareQuantity`, `Price`, `PortfolioId`, etc.)
- Returns an `Optional<Portfolio>` (domain model with Value Objects)

The service has no knowledge of SQL, JPA annotations, or database technology.

### Step 4: Service Fetches Current Stock Price

```java
StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
Price price = stockPrice.price();
```

**Dependency:** `StockPriceProviderPort` (secondary port/interface)

**Actual implementation at runtime:** Either `FinhubStockPriceAdapter` or `AlphaVantageStockPriceAdapter`, depending on the active Spring profile

**What happens:**
- The service calls the port method `fetchStockPrice(Ticker)`
- Spring's dependency injection resolves which adapter is active
- The active adapter makes an HTTP call to the external API
- The adapter parses the JSON response
- The adapter converts the raw price into a `Price` value object and constructs a `StockPrice` record
- Returns `StockPrice` (domain record containing `Ticker`, `Price`, and `Instant`)
- The service extracts the `Price` from the `StockPrice` record

The service has no knowledge of HTTP, JSON parsing, API endpoints, or which provider is being used.

### Step 5: Service Delegates Business Logic to Domain Model

```java
SellResult sellResult = portfolio.sell(ticker, quantity, price);
```

**Component:** `Portfolio` aggregate root (package: `cat.gencat.agaur.hexastock.model`)

**What happens inside the domain:**
1. The `Portfolio` validates that `quantity.isPositive()` (using the `ShareQuantity` method)
2. Checks that the holding exists for the given `Ticker`
3. Delegates to the `Holding` entity to execute the sale
4. The `Holding` applies **FIFO accounting** across multiple `Lot` entities
5. Each `Lot` reduces its `remainingShares` (`ShareQuantity`) and calculates cost basis via `calculateCostBasis(ShareQuantity)` → returns `Money`
6. The `Holding` calculates proceeds using `Price.multiply(ShareQuantity)` → returns `Money`
7. The `Holding` creates `SellResult.of(proceeds, costBasis)` which auto-calculates profit
8. Empty lots are removed via `lots.removeIf(Lot::isEmpty)` to keep the aggregate lean
9. The `Portfolio` adds the proceeds (`Money`) to its cash balance
10. Returns a `SellResult` value object containing `Money` proceeds, `Money` costBasis, and `Money` profit

**Critical point:** All business rules and invariants are enforced within the domain model, not in the service. Value Objects provide an additional layer of protection by making invalid states unrepresentable.

### Step 6: Service Persists Updated Portfolio

```java
portfolioPort.savePortfolio(portfolio);
```

**Dependency:** `PortfolioPort` (secondary port/interface)

**What happens:**
- The service calls the port method `savePortfolio()`
- The JPA adapter extracts primitive values from Value Objects (`PortfolioId.value()`, `Money.amount()`, `ShareQuantity.value()`, `Price.value()`, etc.)
- The adapter converts domain objects to JPA entities using these primitives
- The adapter saves entities to the database
- Changes are committed as part of the `@Transactional` boundary

### Step 7: Service Records Transaction for Audit

```java
Transaction transaction = Transaction.createSale(
    portfolioId, ticker, quantity, price,
    sellResult.proceeds(), sellResult.profit());
transactionPort.save(transaction);
```

**Dependency:** `TransactionPort` (secondary port/interface)

**What happens:**
- A `Transaction` domain object is created using the `createSale` factory method with Value Objects (`PortfolioId`, `Ticker`, `ShareQuantity`, `Price`, `Money`, `Money`)
- The service calls the port method `save()`
- The JPA adapter persists the transaction record

If this operation fails, the `@Transactional` annotation ensures that all previous changes (portfolio update) are rolled back.

### Step 8: Service Returns Result to Controller

```java
return sellResult;
```

The service returns the `SellResult` (domain Value Object containing `Money` fields) to the controller.

### Step 9: Controller Converts to DTO and Returns HTTP Response

```java
return ResponseEntity.ok(
    new SaleResponseDTO(id, request.ticker(), request.quantity(), result));
```

The controller wraps the domain result in a DTO (which extracts `BigDecimal` values from `Money` via `.amount()` for JSON serialization) and returns HTTP 200 OK.

---

## Why This Satisfies Dependency Inversion

The **Dependency Inversion Principle** states:

> **High-level modules should not depend on low-level modules. Both should depend on abstractions.**
>
> **Abstractions should not depend on details. Details should depend on abstractions.**

Let's analyze how the HexaStock stock selling implementation satisfies this principle:

### 1. The Application Service is a High-Level Module

`PortfolioStockOperationsService` represents high-level business policy: "To sell stocks, fetch the price, update the portfolio, and record the transaction."

This service does NOT depend on:
- ❌ `FinhubStockPriceAdapter` (low-level detail)
- ❌ `AlphaVantageStockPriceAdapter` (low-level detail)
- ❌ `JpaPortfolioRepository` (low-level detail)
- ❌ `RestClient`, `HttpClient`, or any HTTP library (low-level detail)
- ❌ Jackson JSON parsing (low-level detail)
- ❌ JPA, Hibernate, or any persistence framework (low-level detail)

### 2. The Service Depends Only on Abstractions

The service declares dependencies on **interfaces** (ports):

```java
public class PortfolioStockOperationsService 
    implements PortfolioStockOperationsUseCase {
    
    private final PortfolioPort portfolioPort;
    private final TransactionPort transactionPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    
    // Constructor injection of interfaces, not implementations
    public PortfolioStockOperationsService(
            PortfolioPort portfolioPort, 
            StockPriceProviderPort stockPriceProviderPort, 
            TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.transactionPort = transactionPort;
    }
}
```

These interfaces are defined in the **application layer**, not in the infrastructure layer. The application defines **what** it needs, and the infrastructure provides **how** to get it.

### 3. Low-Level Modules (Adapters) Implement High-Level Abstractions

The concrete adapters implement the interfaces defined by the application:

```java
// Low-level module implements high-level abstraction
@Component
@Profile("finhub")
public class FinhubStockPriceAdapter implements StockPriceProviderPort {
    // Implementation details specific to Finnhub
}

@Component
@Profile("alphaVantage")
public class AlphaVantageStockPriceAdapter implements StockPriceProviderPort {
    // Implementation details specific to Alpha Vantage
}
```

**The dependency arrow points from infrastructure to application, not the other way around.**

Traditionally (without DIP), the dependency would flow like this:

```
Service → FinhubAdapter → Finnhub API (BAD)
```

With DIP, the dependency flows like this:

```
Service → StockPriceProviderPort ← FinhubAdapter → Finnhub API (GOOD)
Service → StockPriceProviderPort ← AlphaVantageAdapter → Alpha Vantage API (GOOD)
```

The service depends on the abstraction, and the adapters depend on the same abstraction. The adapters "plug into" the port.

### 4. Abstractions Do Not Depend on Details

The `StockPriceProviderPort` interface uses only domain concepts:

```java
public interface StockPriceProviderPort {
    StockPrice fetchStockPrice(Ticker ticker);
}
```

It does NOT mention:
- ❌ HTTP methods (GET, POST)
- ❌ JSON, XML, or any data format
- ❌ API endpoints or URLs
- ❌ Authentication mechanisms
- ❌ Specific provider names (Finnhub, Alpha Vantage)

The interface is expressed in the **ubiquitous language** of the domain: "fetch stock price for a ticker." Both `Ticker` (input) and `StockPrice` (output, containing `Price`) are domain Value Objects, not infrastructure types.

### 5. Spring Dependency Injection Resolves the Concrete Implementation

At runtime, Spring's IoC container wires the dependencies:

```java
// When profile "finhub" is active:
PortfolioStockOperationsService service = new PortfolioStockOperationsService(
    jpaPortfolioRepository,        // Implements PortfolioPort
    finhubStockPriceAdapter,       // Implements StockPriceProviderPort
    jpaTransactionRepository       // Implements TransactionPort
);

// When profile "alphaVantage" is active:
PortfolioStockOperationsService service = new PortfolioStockOperationsService(
    jpaPortfolioRepository,        // Implements PortfolioPort
    alphaVantageStockPriceAdapter, // Implements StockPriceProviderPort
    jpaTransactionRepository       // Implements TransactionPort
);
```

The service code **never changes**. Only the configuration (active profile) changes.

---

## Practical Benefits (Testability, Extensibility)

The application of the Dependency Inversion Principle in the stock selling use case provides concrete, measurable benefits:

### Benefit 1: Testability Without Infrastructure

You can test the `PortfolioStockOperationsService` without starting a web server, database, or making real HTTP calls to external APIs.

**Example test setup:**

```java
@Test
void sellStock_shouldCalculateProfitCorrectly() {
    // Arrange: Create mock implementations of ports
    PortfolioPort mockPortfolioPort = mock(PortfolioPort.class);
    StockPriceProviderPort mockPriceProvider = mock(StockPriceProviderPort.class);
    TransactionPort mockTransactionPort = mock(TransactionPort.class);
    
    // Create the service with mock dependencies
    PortfolioStockOperationsService service = new PortfolioStockOperationsService(
        mockPortfolioPort, 
        mockPriceProvider, 
        mockTransactionPort
    );
    
    // Set up test data using Value Objects
    Portfolio portfolio = createTestPortfolio();
    PortfolioId portfolioId = PortfolioId.of("123");
    Ticker aapl = Ticker.of("AAPL");
    
    when(mockPortfolioPort.getPortfolioById(portfolioId))
        .thenReturn(Optional.of(portfolio));
    when(mockPriceProvider.fetchStockPrice(aapl))
        .thenReturn(StockPrice.of(aapl, Price.of(150.0), Instant.now()));
    
    // Act: Execute the use case
    SellResult result = service.sellStock(portfolioId, aapl, ShareQuantity.of(10));
    
    // Assert: Verify business logic
    assertThat(result.proceeds()).isEqualTo(Money.of(1500.0));
    verify(mockPortfolioPort).savePortfolio(portfolio);
    verify(mockTransactionPort).save(any(Transaction.class));
}
```

**Without DIP**, you would need:
- A running database
- A test container with JPA
- A mock HTTP server to simulate Finnhub/Alpha Vantage
- Complex setup and teardown logic

**With DIP**, you only need:
- Mock objects that implement interfaces
- No external dependencies

Tests run **faster** (milliseconds instead of seconds) and are **more reliable** (no network issues, no database connection problems).

### Benefit 2: Switching Stock Price Providers Without Code Changes

If you want to switch from Finnhub to Alpha Vantage, you only need to change the Spring profile:

**Before (using Finnhub):**
```bash
java -jar hexastock.jar --spring.profiles.active=finhub
```

**After (using Alpha Vantage):**
```bash
java -jar hexastock.jar --spring.profiles.active=alphaVantage
```

**No code changes required.** The service continues to work identically because it depends only on the abstraction.

You could even switch providers **per environment**:
- Development: Use a mock provider with fake data
- Staging: Use Finnhub (faster, more generous free tier)
- Production: Use Alpha Vantage (more reliable, paid plan)

### Benefit 3: Adding a New Stock Price Provider is Isolated

Suppose you want to add a **third provider** (e.g., Yahoo Finance, IEX Cloud, or Twelve Data).

**What needs to change:**

1. ✅ Create a new adapter class implementing `StockPriceProviderPort`
2. ✅ Add configuration properties for the new API
3. ✅ Add a new Spring profile

**What does NOT change:**

1. ❌ `PortfolioStockOperationsService` (application service)
2. ❌ `StockPriceProviderPort` (interface)
3. ❌ `PortfolioRestController` (REST adapter)
4. ❌ `Portfolio`, `Holding`, `Lot` (domain model)
5. ❌ Any existing tests

**Example: Adding Twelve Data adapter**

```java
@Component
@Profile("twelvedata")
public class TwelveDataStockPriceAdapter implements StockPriceProviderPort {
    
    @Value("${twelvedata.api.key}")
    private String apiKey;
    
    @Value("${twelvedata.api.base-url}")
    private String baseUrl;
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // Call Twelve Data API
        // Parse their specific JSON format
        // Convert to domain Value Objects:
        //   Price price = Price.of(parsedPrice);
        //   return StockPrice.of(ticker, price, Instant.now());
    }
}
```

Add configuration:

```properties
# application-twelvedata.properties
twelvedata.api.key=your-api-key-here
twelvedata.api.base-url=https://api.twelvedata.com
```

Activate the profile:

```bash
java -jar hexastock.jar --spring.profiles.active=twelvedata
```

The entire application continues to work without any modification to the core logic. This demonstrates the **Open/Closed Principle**: the system is open for extension (adding new providers) but closed for modification (no changes to existing code).

### Benefit 4: Domain Model Remains Pure

The domain model (`Portfolio`, `Holding`, `Lot`, `SellResult`, `Money`, `Price`, `ShareQuantity`, `Ticker`) contains **zero infrastructure dependencies**:

- No JPA annotations (like `@Entity`, `@Table`)
- No HTTP-related imports (like `HttpClient`, `RestTemplate`)
- No JSON serialization annotations (like `@JsonProperty`)
- No Spring framework imports (like `@Service`, `@Component`)

This means:
- The domain can be **tested in isolation** with plain JUnit tests
- The domain can be **reused** in different contexts (CLI application, batch job, message queue consumer)
- The domain can be **evolved** independently of infrastructure concerns
- Value Objects enforce domain rules at construction time (e.g., `Price` must be positive, `ShareQuantity` must be non-negative)

### Benefit 5: Improved Error Handling and Resilience

Because the service depends on abstractions, you can add **cross-cutting concerns** without changing the service code:

**Example: Adding a caching layer**

```java
@Component
@Profile("cached")
public class CachedStockPriceAdapter implements StockPriceProviderPort {
    
    private final StockPriceProviderPort delegate; // Actual provider
    private final Map<Ticker, CachedPrice> cache = new ConcurrentHashMap<>();
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        CachedPrice cached = cache.get(ticker);
        if (cached != null && cached.isValid()) {
            return cached.stockPrice;
        }
        
        StockPrice fresh = delegate.fetchStockPrice(ticker);
        cache.put(ticker, new CachedPrice(fresh));
        return fresh;
    }
}
```

**Example: Adding a fallback mechanism**

```java
@Component
@Profile("resilient")
public class FallbackStockPriceAdapter implements StockPriceProviderPort {
    
    private final StockPriceProviderPort primary;   // e.g., Finnhub
    private final StockPriceProviderPort secondary; // e.g., Alpha Vantage
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        try {
            return primary.fetchStockPrice(ticker);
        } catch (ExternalApiException e) {
            log.warn("Primary provider failed, falling back to secondary");
            return secondary.fetchStockPrice(ticker);
        }
    }
}
```

Both of these enhancements are **transparent** to the application service. The service continues to call `stockPriceProviderPort.fetchStockPrice()` without knowing whether it's talking to a caching adapter, a fallback adapter, or a direct API adapter.

### Benefit 6: Regulatory Compliance and Auditing

In financial applications, you often need to track **which data source** was used for each transaction.

Because the dependency is inverted, you can inject audit logging at the adapter level:

```java
@Component
@Profile("audited-finhub")
public class AuditedFinhubAdapter implements StockPriceProviderPort {
    
    private final FinhubStockPriceAdapter delegate;
    private final AuditLogger auditLogger;
    
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        StockPrice price = delegate.fetchStockPrice(ticker);
        
        auditLogger.log(AuditEvent.builder()
            .eventType("STOCK_PRICE_FETCHED")
            .provider("Finnhub")
            .ticker(ticker.value())
            .price(price.price().amount())
            .timestamp(Instant.now())
            .build());
        
        return price;
    }
}
```

The application service remains unchanged. The audit requirement is satisfied at the infrastructure level.

---

## Summary

The stock selling implementation in HexaStock demonstrates the Dependency Inversion Principle through:

1. **Clear separation of concerns:** The application service orchestrates, the domain model decides, and adapters handle infrastructure
2. **Dependency on abstractions:** The service depends on `StockPriceProviderPort` (interface), never on `FinhubStockPriceAdapter` (implementation)
3. **Inverted dependency direction:** Infrastructure (adapters) depends on application (ports), not vice versa
4. **Multiple interchangeable implementations:** Finnhub and Alpha Vantage adapters can be swapped via configuration
5. **Testability:** The service can be tested with mocks, without starting databases or making HTTP calls
6. **Extensibility:** New providers can be added without modifying existing code
7. **Domain purity:** The domain model contains zero infrastructure dependencies

This architecture enables the system to evolve independently in three dimensions:
- **Domain logic** can change without affecting adapters
- **Infrastructure** can change without affecting domain logic
- **New capabilities** can be added without modifying existing components

The Dependency Inversion Principle, combined with Hexagonal Architecture, creates a system where business logic is protected from technological change and technical decisions can be deferred or reversed with minimal cost.
