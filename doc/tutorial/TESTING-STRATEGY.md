# Testing Strategy for a Hexagonal Architecture

**How HexaStock verifies business logic, infrastructure integration, and architectural fitness — from value objects to the running system**

> *"Tests are not proof that the system works. They are proof that you understand what it should do."*

---

## About This Document

This document describes the complete testing strategy of HexaStock — a stock portfolio management system built with Java 21, Spring Boot 3, Domain-Driven Design, and Hexagonal Architecture. Rather than listing tools and configuration, it explains *why* each testing layer exists, *what* it catches that no other layer can, and *how* the layers compose into a coherent verification pipeline.

The **sell stock** use case serves as the running example throughout, because it crosses every architectural boundary: a REST request enters the system, an application service orchestrates the operation, the domain model enforces FIFO lot-consumption rules, a stock price adapter fetches the current market price, and a persistence adapter stores the result. Every test layer has something to say about this flow.

**Intended audience:** Software engineers, architects, and students who want to understand how to test a hexagonal architecture systematically — not just which frameworks to use, but which guarantees each layer provides and where the boundaries between layers fall.

**Companion document:** The [Sell Stock Tutorial](sellStocks/SELL-STOCK-TUTORIAL.md) traces one request end-to-end through every code layer. This document complements it by tracing the same request end-to-end through every *testing* layer.

---

## Table of Contents

1. [Testing Layers at a Glance](#1-testing-layers-at-a-glance)
2. [Layer 1 — Domain Unit Tests](#2-layer-1--domain-unit-tests)
3. [Layer 2 — Application Service Tests](#3-layer-2--application-service-tests)
4. [Layer 3 — Inbound Adapter Tests (REST)](#4-layer-3--inbound-adapter-tests-rest)
5. [Layer 4 — Outbound Adapter Tests (Market Price)](#5-layer-4--outbound-adapter-tests-market-price)
6. [Layer 5 — Outbound Adapter Tests (Persistence)](#6-layer-5--outbound-adapter-tests-persistence)
7. [Layer 6 — Full-Stack Integration Tests (Bootstrap)](#7-layer-6--full-stack-integration-tests-bootstrap)
8. [Layer 7 — Architecture Fitness Tests](#8-layer-7--architecture-fitness-tests)
9. [Deep Dive: Deterministic Testing of External Market-Price Adapters](#9-deep-dive-deterministic-testing-of-external-market-price-adapters)
10. [Deep Dive: Persistence Adapter Testing with Future Portability](#10-deep-dive-persistence-adapter-testing-with-future-portability)
11. [Cross-Cutting: Requirements Traceability](#11-cross-cutting-requirements-traceability)
12. [Test Suite Inventory](#12-test-suite-inventory)
13. [Running the Tests](#13-running-the-tests)
14. [References](#14-references)

---

## 1. Testing Layers at a Glance

HexaStock organises its tests into seven layers, each aligned with a hexagonal architecture boundary. The following table summarises what each layer tests, what it needs to run, and what class of bug it catches that no other layer can.

| # | Layer | Module | Scope | Infrastructure | What Only This Layer Catches |
|---|-------|--------|-------|----------------|------|
| 1 | **Domain unit tests** | `domain` | Value objects, entities, aggregate root | None — pure Java | Algorithm errors in FIFO, Money arithmetic, invariant violations |
| 2 | **Application service tests** | `application` | Use case orchestration, port call sequence | Mockito mocks | Wrong port call order, missing save, incorrect delegation |
| 3 | **Inbound adapter tests** | `adapters-inbound-rest` | REST controllers, DTOs, HTTP status codes | `@WebMvcTest`, MockMvc | JSON mapping errors, wrong status codes, missing error handlers |
| 4 | **Outbound adapter tests (market)** | `adapters-outbound-market` | External API clients | WireMock | Incorrect HTTP request construction, response parsing failures |
| 5 | **Outbound adapter tests (persistence)** | `adapters-outbound-persistence-jpa` | JPA repositories, entity mappers | `@DataJpaTest`, Testcontainers MySQL | Mapper field mismatches, JPA mapping errors, query bugs, locking |
| 6 | **Full-stack integration tests** | `bootstrap` | Complete HTTP → DB round-trip | `@SpringBootTest`, Testcontainers, RestAssured | Wiring failures, profile misconfiguration, adapter incompatibilities |
| 7 | **Architecture fitness tests** | `bootstrap` | Dependency direction rules | ArchUnit | Illegal cross-layer dependencies that compile but violate the hexagon |

**Key principle:** Each layer tests something that no other layer tests. Domain tests cannot detect a broken JPA mapping. Integration tests cannot pinpoint which FIFO step is wrong. Architecture tests cannot verify business logic. The layers are complementary, not redundant.

### Following the Sell Request Through Every Test Layer

When a user sells 12 shares of AAPL at market price $150, each testing layer verifies a different aspect of the operation:

1. **Domain** — `HoldingTest` and `PortfolioTest` verify that FIFO consumes 10 shares from Lot #1 at $100 and 2 from Lot #2 at $120, producing `costBasis = $1240`, `proceeds = $1800`, `profit = $560`.
2. **Application** — `PortfolioStockOperationsServiceTest` verifies the orchestration sequence: fetch portfolio → fetch price → `portfolio.sell()` → save portfolio → record transaction.
3. **REST adapter** — `PortfolioRestControllerTest` verifies that `POST /api/portfolios/{id}/sales` returns 200 with `proceeds`, `costBasis`, and `profit` in the JSON response.
4. **Market adapter** — `FinhubStockPriceAdapterTest` verifies that the Finnhub response `{"c":150.00,...}` is correctly parsed into `Price.of("150.00")`.
5. **Persistence adapter** — `JpaPortfolioRepositoryContractTest` verifies that a portfolio with holdings and lots survives a round-trip through JPA and MySQL identically.
6. **Integration** — `PortfolioTradingRestIntegrationTest` verifies the complete flow end-to-end: HTTP request → controller → service → domain → persistence → HTTP response with exact BigDecimal financial assertions.
7. **Architecture** — `HexagonalArchitectureTest` verifies that the domain module has no dependency on adapters or Spring, ensuring the FIFO logic can never be accidentally coupled to infrastructure.

---

## 2. Layer 1 — Domain Unit Tests

**Module:** `domain` · **Tests:** 7 classes, ~99 methods · **Execution time:** milliseconds · **Dependencies:** none (pure Java, JUnit 5)

The domain layer contains pure business logic with zero framework dependencies. Its tests are the fastest in the suite and the most stable — they never break due to database changes, API changes, or configuration drift.

### What These Tests Verify

| Test Class | Responsibility | Key Technique |
|---|---|---|
| `TickerTest` | Ticker value object: format validation (`^[A-Z]{2,5}$`), equality | `@ParameterizedTest` with `@ValueSource` |
| `MoneyTest` | Money arithmetic: add, subtract, multiply, immutability | `@Nested` groups for each operation |
| `LotTest` | Lot entity: reduce, isEmpty, costBasis | Factory methods, identity equality by ID |
| `HoldingTest` | FIFO lot consumption: single-lot, cross-lot, SellResult | `@SpecificationRef` linking to Gherkin |
| `PortfolioTest` | Aggregate invariants: balance + FIFO consistency | Aggregate-level assertions |
| `HoldingPerformanceCalculatorTest` | Holdings performance: unrealised P&L, rounding | `@ParameterizedTest` with `@MethodSource`, 5000-tx dataset |
| `TransactionTest` | Sealed hierarchy: Deposit, Withdrawal, Purchase, Sale | Exhaustive switch, factory validation |

### Sell Stock at the Domain Level

The Gherkin scenario "Selling 12 shares across multiple lots" is translated directly into two complementary domain tests. The `PortfolioTest` verifies the aggregate-level outcome:

```java
// PortfolioTest — aggregate root verifies balance + FIFO together
SellResult result = fundedPortfolio.sell(APPLE, ShareQuantity.of(12), marketSellPrice);

assertEquals(Money.of("1800.00"), result.proceeds());   // 12 × 150
assertEquals(Money.of("1240.00"), result.costBasis());   // (10 × 100) + (2 × 120)
assertEquals(Money.of("560.00"),  result.profit());      // 1800 − 1240
assertEquals(balanceBeforeSell.add(Money.of("1800.00")), fundedPortfolio.getBalance());
```

The `HoldingTest` verifies the FIFO algorithm in isolation, without portfolio-level concerns like cash balance. Both tests verify the same financial results — this is intentional. The `Portfolio` test proves aggregate consistency; the `Holding` test pinpoints exactly where a FIFO bug lives if one occurs.

### Design Principle: No Mocks at the Domain Level

Domain tests never use Mockito. Every object in a domain test is a real instance of the domain model. This is possible because the domain has no dependencies to mock — no ports, no services, no Spring beans. When a domain test fails, the failure is always a business logic error, never a mock configuration error.

---

## 3. Layer 2 — Application Service Tests

**Module:** `application` · **Tests:** 6 classes, 27 methods · **Execution time:** milliseconds · **Dependencies:** JUnit 5, Mockito

Application services are thin orchestrators — they retrieve domain objects through ports, delegate decisions to the domain, and persist results. Their tests verify this orchestration: correct port call sequence, correct delegation, correct exception propagation.

### What These Tests Verify

| Test Class | Use Cases | Key Verification |
|---|---|---|
| `PortfolioStockOperationsServiceTest` | Buy stock, Sell stock | Port call sequence: portfolio → price → save → transaction |
| `PortfolioLifecycleServiceTest` | Create, Get, List portfolios | Delegation to `PortfolioPort`, exception on not-found |
| `CashManagementServiceTest` | Deposit, Withdraw | Call sequence with `inOrder`, `ArgumentCaptor` |
| `GetStockPriceServiceTest` | Get stock price | Thin delegation to `StockPriceProviderPort` |
| `TransactionServiceTest` | Get transactions | Delegation, empty result handling |
| `ReportingServiceTest` | Holdings performance | Orchestration: mocked ports + real calculator |

### Sell Stock at the Application Layer

The `PortfolioStockOperationsServiceTest` mocks all three outbound ports and verifies the orchestration:

```java
// All ports are mocked — we test orchestration, not business logic
var inOrder = inOrder(portfolioPort, stockPriceProviderPort, transactionPort);
inOrder.verify(portfolioPort).getPortfolioById(id);
inOrder.verify(stockPriceProviderPort).fetchStockPrice(AAPL);
inOrder.verify(portfolioPort).savePortfolio(portfolio);
inOrder.verify(transactionPort).save(any(Transaction.class));
```

This test does not verify the FIFO algorithm (the domain layer owns that). It verifies that the service calls the right ports in the right order and returns a `SellResult`. If someone accidentally swapped the save and fetch calls, only this test would catch it.

### Design Principle: Mock Ports, Not Domain Objects

Application service tests mock the outbound ports (`PortfolioPort`, `StockPriceProviderPort`, `TransactionPort`) but use real domain objects (`Portfolio`, `Holding`, `Lot`). The domain model is simple to construct and fast to execute — mocking it would test nothing while hiding real bugs.

---

## 4. Layer 3 — Inbound Adapter Tests (REST)

**Module:** `adapters-inbound-rest` · **Tests:** 2 classes, 19 methods · **Execution time:** ~seconds (Spring slice) · **Dependencies:** `@WebMvcTest`, MockMvc, Mockito

The REST adapter translates between the HTTP world (JSON, path variables, status codes) and the domain world (value objects, use case interfaces). Its tests verify this translation layer in isolation, without starting the full application.

### What These Tests Verify

| Test Class | Endpoints | Key Verification |
|---|---|---|
| `PortfolioRestControllerTest` | All portfolio endpoints | JSON ↔ DTO mapping, HTTP status codes, error responses (RFC 7807) |
| `StockRestControllerTest` | `GET /api/stocks/{ticker}/price` | 200 OK, 503 on external failure, 400 on invalid ticker |

### Sell Stock at the REST Layer

```java
// Sell endpoint: 200 OK with SaleResponseDTO
mockMvc.perform(post("/api/portfolios/{id}/sales", PORTFOLIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\"AAPL\",\"quantity\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.proceeds").value(1500.00))
        .andExpect(jsonPath("$.costBasis").value(1000.00))
        .andExpect(jsonPath("$.profit").value(500.00));
```

The use case is a `@MockitoBean` — no real service, no database, no market adapter. This test verifies that the controller correctly converts the `SellResult` into JSON and returns 200. It also verifies error mapping: 404 for missing portfolios, 409 for insufficient shares, 400 for invalid tickers — all through the `@ControllerAdvice` global error handler.

### Design Principle: @WebMvcTest Slice Tests

`@WebMvcTest` loads only the web layer (controllers, filters, advice), not the full application context. This means test failures are always about the HTTP boundary — never about services, persistence, or wiring. The trade-off is that `@WebMvcTest` cannot detect wiring failures that only manifest in the full context.

---

## 5. Layer 4 — Outbound Adapter Tests (Market Price)

**Module:** `adapters-outbound-market` · **Tests:** 3 classes, 11 methods · **Execution time:** milliseconds · **Dependencies:** WireMock, AssertJ

Market price adapters call external REST APIs (Finnhub, Alpha Vantage) to fetch real-time stock prices. Testing these adapters without calling the real APIs requires an HTTP-level test double — WireMock.

### What These Tests Verify

| Test Class | Adapter | Scenarios |
|---|---|---|
| `FinhubStockPriceAdapterTest` | Finnhub API | Valid response, missing `c` field, server error, null body |
| `AlphaVantageStockPriceAdapterTest` | Alpha Vantage API | Valid Global Quote, missing quote, missing price, server error, non-numeric price |
| `MockFinhubStockPriceAdapterTest` | Random-price mock | Correct ticker, price in range [10, 1000] |

### Anatomy of a WireMock Adapter Test

The `@WireMockTest` annotation starts a local HTTP server. Each test stubs a specific API response and verifies that the adapter parses it correctly — or fails gracefully:

```java
@WireMockTest
class FinhubStockPriceAdapterTest {

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        adapter = new FinhubStockPriceAdapter();
        ReflectionTestUtils.setField(adapter, "finhubApiKey", "test-key");
        ReflectionTestUtils.setField(adapter, "finhubApiUrl", wmInfo.getHttpBaseUrl());
    }

    @Test
    void validResponse() {
        stubFor(get(urlPathEqualTo("/quote"))
                .withQueryParam("symbol", equalTo("AAPL"))
                .withQueryParam("token", equalTo("test-key"))
                .willReturn(okJson("""
                    {"c":175.50,"d":2.5,"dp":1.44,"h":177.0,"l":174.0,"o":175.0,"pc":173.0}
                    """)));

        StockPrice result = adapter.fetchStockPrice(Ticker.of("AAPL"));

        assertThat(result.price().value().doubleValue()).isEqualTo(175.50);
    }
}
```

The test verifies three things simultaneously: (1) the adapter constructs the correct URL with the right query parameters, (2) it parses the JSON response into the correct domain value object, and (3) it propagates `ExternalApiException` when the response is malformed or the server returns an error.

> **Why WireMock instead of mocking the HTTP client?** Both production adapters use Spring's `RestClient` — the modern, fluent HTTP client introduced in Spring Framework 6.1. Each adapter builds a `RestClient` per request via `RestClient.builder()` with a `SimpleClientHttpRequestFactory` for timeout control. Mocking the `RestClient` fluent chain (`get().uri(…).retrieve().body(…)`) would only verify interactions with a mock object — it would not catch errors in URL construction, query-parameter assembly, timeout configuration, or JSON deserialization. WireMock tests the actual HTTP contract at the wire level, including the request path, query parameters, and response parsing.

For a detailed walkthrough of how WireMock eliminates non-determinism when testing external APIs — and why `RestClient`'s fluent API makes HTTP-level testing especially valuable — see [Deep Dive: Deterministic Testing of External Market-Price Adapters](#9-deep-dive-deterministic-testing-of-external-market-price-adapters).

---

## 6. Layer 5 — Outbound Adapter Tests (Persistence)

**Modules:** `adapters-outbound-persistence-jpa`, `adapters-outbound-persistence-mongodb` · **Tests:** ~21 JPA methods + an equivalent MongoDB suite · **Execution time:** ~seconds each (Testcontainers) · **Dependencies:** `@DataJpaTest` / `@DataMongoTest`, Spring Data, Testcontainers (MySQL 8 / MongoDB), AssertJ

Persistence adapter tests verify that domain objects survive the round-trip through the persistence layer and a real database. HexaStock implements two interchangeable adapters — JPA on MySQL and Spring Data on MongoDB — and the test suites of both adapters mirror each other, exercising the same outbound ports (`PortfolioPort`, `TransactionPort`) so that behavioural equivalence between the two technologies is verified, not assumed.

### Test Categories

| Test Class | Adapter | Category | What It Verifies |
|---|---|---|---|
| `MapperTest` | JPA | Pure unit tests | JPA ↔ Domain mapper correctness (no database) |
| `JpaPortfolioRepositoryContractTest` | JPA | Port contract test | `PortfolioPort` contract against real MySQL |
| `JpaTransactionRepositoryContractTest` | JPA | Port contract test | `TransactionPort` contract against real MySQL |
| `JpaPessimisticLockingTest` | JPA | Adapter-specific test | `SELECT ... FOR UPDATE` locking behaviour |
| `MongoMapperTest` | MongoDB | Pure unit tests | Document ↔ Domain mapper correctness (no database) |
| `MongoPortfolioRepositoryContractTest` | MongoDB | Port contract test | `PortfolioPort` contract against real MongoDB |
| `MongoTransactionRepositoryContractTest` | MongoDB | Port contract test | `TransactionPort` contract against real MongoDB |
| `MongoOptimisticLockingTest` | MongoDB | Adapter-specific test | `@Version`-based optimistic locking and retry |

### Port Contract Tests: The Portability Pattern

The most architecturally significant tests in this layer are the **port contract tests**. The abstract contract (`AbstractPortfolioPortContractTest`) defines what any `PortfolioPort` implementation must do — create-and-retrieve, update balance, list all, handle nonexistent IDs, and round-trip holdings with lots. Both the JPA and MongoDB implementations inherit these tests:

```java
// AbstractPortfolioPortContractTest — in application module, no Spring annotations
@Test
protected void portfolioWithHoldingsAndLots_roundTrip() {
    Portfolio portfolio = new Portfolio(PortfolioId.of("p-h"), "Carol", Money.of(5000), NOW);
    // ... add holding with lot ...
    port().createPortfolio(portfolio);

    Portfolio found = port().getPortfolioById(PortfolioId.of("p-h")).orElseThrow();
    assertThat(found.getHoldings()).hasSize(1);
    assertThat(foundHolding.getLots()).hasSize(1);
}
```

```java
// JpaPortfolioRepositoryContractTest — in JPA module, with full Spring/Testcontainers wiring
@DataJpaTest
@Import(JpaPortfolioRepository.class)
class JpaPortfolioRepositoryContractTest extends AbstractPortfolioPortContractTest {

    @Override protected PortfolioPort port() { return repository; }

    // Override + delegate pattern — required for Spring @Transactional resolution
    @Override @Test protected void createAndGetById_roundTrip()    { super.createAndGetById_roundTrip(); }
    @Override @Test protected void portfolioWithHoldingsAndLots_roundTrip() { super.portfolioWithHoldingsAndLots_roundTrip(); }
    // ... remaining contract methods ...
}
```

This design means that HexaStock can swap between MySQL (JPA) and MongoDB without rewriting a single contract test: each adapter only needs to extend `AbstractPortfolioPortContractTest`, supply its repository via `port()`, and all contract assertions run automatically against that technology. The same business behaviour is guaranteed regardless of the persistence technology in use.

For the full architectural rationale, the override-and-delegate pattern explained, and a worked example of adding a second persistence technology, see [Deep Dive: Persistence Adapter Testing with Future Portability](#10-deep-dive-persistence-adapter-testing-with-future-portability).

---

## 7. Layer 6 — Full-Stack Integration Tests (Bootstrap)

**Module:** `bootstrap` · **Tests:** 5 classes, 43 methods · **Execution time:** ~tens of seconds · **Dependencies:** `@SpringBootTest`, Testcontainers MySQL, RestAssured, `@ActiveProfiles`

Integration tests start the complete Spring Boot application, send real HTTP requests via RestAssured, and verify responses against a real MySQL database. They are the only tests that exercise the full wiring — controller → service → domain → adapter → database → response.

### Test Organisation

| Test Class | Responsibility | Key Scenarios |
|---|---|---|
| `AbstractPortfolioRestIntegrationTest` | Shared infrastructure, helper methods | (base class — not executed directly) |
| `PortfolioLifecycleRestIntegrationTest` | Portfolio CRUD, deposits, withdrawals | Create, deposit, withdraw, list, holdings performance |
| `PortfolioTradingRestIntegrationTest` | Buy, sell, FIFO verification | End-to-end trading, Gherkin FIFO with deterministic prices |
| `PortfolioErrorHandlingRestIntegrationTest` | 404 errors on missing portfolios | All endpoints with nonexistent portfolio |
| `PortfolioTransactionHistoryRestIntegrationTest` | Transaction history retrieval | All transactions, filtered by type |
| `StockPriceRestIntegrationTest` | Stock price endpoint | 200 OK, 400 invalid ticker |

### Sell Stock at the Integration Level

The `GherkinFifoScenarios` nested class inside `PortfolioTradingRestIntegrationTest` maps the Gherkin specification to end-to-end HTTP assertions:

```java
@Test
@SpecificationRef(value = "US-07.FIFO-2", level = TestLevel.INTEGRATION,
                  feature = "sell-stocks.feature")
void sellSharesAcrossMultipleLots_FIFOGherkinScenario() {
    fixedPriceAdapter.enqueuePrice(Price.of("150.00"));

    sellPrecise(portfolioId, "AAPL", 12)
            .statusCode(200)
            .body("proceeds",  comparesEqualTo(new BigDecimal("1800.00")))
            .body("costBasis", comparesEqualTo(new BigDecimal("1240.00")))
            .body("profit",    comparesEqualTo(new BigDecimal("560.00")));

    getHoldings(portfolioId)
            .body("find { it.ticker == 'AAPL' }.remaining", equalTo(3));
}
```

This test verifies the same financial results as the domain tests — `costBasis = 1240.00` proves FIFO order — but through the real HTTP stack, real JSON serialisation, real Spring transaction management, and a real MySQL database. If FIFO is correct in the domain but broken in integration, the cause is in the wiring: a mapper losing data, a transaction boundary misplaced, or a JPA cascade missing.

### Profile-Based Adapter Substitution

Integration tests use `@ActiveProfiles({"test", "jpa", "mockfinhub"})` to activate the JPA persistence adapter and a mock stock price provider. The `PortfolioTradingRestIntegrationTest` goes further — its `@TestConfiguration` provides a `FixedPriceStockPriceAdapter` marked `@Primary`, which overrides the mock for Gherkin scenarios that require deterministic prices. This layered adapter substitution demonstrates a core hexagonal architecture benefit: the domain and application layers are completely unaware of which adapter is active.

---

## 8. Layer 7 — Architecture Fitness Tests

**Module:** `bootstrap` · **Tests:** 1 class, 5(+) rules · **Execution time:** ~seconds · **Dependencies:** ArchUnit

Architecture fitness tests do not verify business behaviour — they verify structural constraints. Using ArchUnit, `HexagonalArchitectureTest` scans compiled classes from all modules and enforces dependency direction rules:

```java
@Test
void domainDoesNotDependOnApplication() {
    noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .check(allClasses);
}
```

### Rules Enforced

| Rule | Guard |
|---|---|
| Domain → Application | Prevents domain from depending on use case interfaces |
| Domain → Adapters | Prevents domain from depending on infrastructure |
| Domain → Spring | Prevents framework annotations from leaking into the domain |
| Application → Adapters | Prevents application from depending on concrete implementations |
| Inbound → Outbound adapters | Prevents REST controllers from directly calling persistence |
| Outbound → Inbound adapters | Prevents persistence from depending on REST controllers |

### Two-Layer Structural Safety Net

These rules complement Maven module boundaries. Maven prevents compile-time dependencies *between modules* — `domain/pom.xml` simply does not declare Spring or JPA dependencies, so the compiler rejects any import. ArchUnit catches violations *within the same classpath*: transitive dependencies, reflection-based coupling, or accidental imports that compile because the classes happen to be on the test classpath. Together, Maven module boundaries and ArchUnit rules form a two-layer structural safety net.

---

## 9. Deep Dive: Deterministic Testing of External Market-Price Adapters

### The Problem

When a user sells stocks, the application service calls `StockPriceProviderPort.fetchStockPrice(ticker)` to get the current market price. In production, this calls a real financial API — Finnhub or Alpha Vantage — which returns a different price every second. A test that depends on a live API is non-deterministic by definition: it cannot verify exact financial outcomes because the price changes between test runs.

This creates a three-part testing challenge:

1. **Unit testing the adapter itself:** Verify that the adapter correctly constructs the HTTP request and parses the response.
2. **Integration testing with deterministic prices:** Verify exact FIFO financial calculations end-to-end with known prices.
3. **Proving the hexagon works:** Demonstrate that adapters are genuinely swappable without changes to domain or application code.

### Solution Part 1: WireMock for Adapter-Level Tests

Each market adapter has a dedicated WireMock test class that stubs the external API at the HTTP level:

```
adapters-outbound-market/
└── src/test/java/
    └── adapter/out/rest/
        ├── FinhubStockPriceAdapterTest.java        ← WireMock: Finnhub API
        ├── AlphaVantageStockPriceAdapterTest.java   ← WireMock: Alpha Vantage API
        └── MockFinhubStockPriceAdapterTest.java     ← Unit test: random-price mock
```

**Why WireMock and not Mockito?** Both the `FinhubStockPriceAdapter` and the `AlphaVantageStockPriceAdapter` are concrete classes that use Spring's **`RestClient`** — the modern, fluent HTTP client introduced in Spring Framework 6.1 (Spring Boot 3.2+). Each adapter builds a `RestClient` instance per request using `RestClient.builder()` with a `SimpleClientHttpRequestFactory` for timeout control, and then makes the actual HTTP call through the fluent chain `restClient.get().uri(url).retrieve().body(JsonNode.class)`.

Mocking this `RestClient` fluent API chain would mean stubbing `get()`, `uri()`, `retrieve()`, and `body()` in sequence — and would only verify that the adapter calls those methods in the expected order. Such a mock would **not** validate:

- whether the URL is correctly assembled from `baseUrl`, path, and query parameters
- whether the `symbol` and `token`/`apikey` query parameters are correct
- whether `SimpleClientHttpRequestFactory` timeout settings take effect
- whether `body(JsonNode.class)` correctly parses the response JSON into a `JsonNode`
- whether the adapter correctly navigates the JSON structure (`"c"` for Finnhub, `"Global Quote"."05. price"` for Alpha Vantage)
- whether the adapter throws the right `ExternalApiException` when the response is malformed

WireMock eliminates this gap by starting a real HTTP server. The adapter sends a real HTTP request to `localhost:<port>`, and WireMock matches the incoming request against the stub — verifying URL path, query parameters, and headers — then returns a canned JSON response. The adapter's Jackson-based deserialization runs against the actual JSON payload, and the test asserts the final domain value object. The full HTTP contract — from URL construction to response parsing — is exercised in a single test.

**What WireMock tests catch:**

| Defect | Example | How WireMock Detects It |
|---|---|---|
| Wrong URL path | `/quotes` instead of `/quote` | Stub doesn't match → connection error or timeout |
| Missing query parameter | Forgot `token` param | `withQueryParam` assertion fails |
| Incorrect JSON field navigation | Reading `"price"` instead of `"c"` | `assertThat(result.price())...` fails |
| Missing error handling | No catch for 500 response | `stubFor(serverError())` + `assertThatThrownBy(...)` |
| Null-safety gap | API returns `null` JSON body | `stubFor(okJson("null"))` exposes `NullPointerException` |
| `RestClient` builder misconfiguration | Wrong `baseUrl` or missing `requestFactory` | Request goes to wrong endpoint or ignores timeout |

**What remains outside the scope of WireMock adapter tests:**

- **Spring wiring:** `@Value` injection, `@Profile` activation, and `@Cacheable` behaviour are not exercised because the tests instantiate the adapter directly, without a Spring context.
- **Concurrency under load:** The 500ms throttle (`Thread.sleep`) is present in production code but is not meaningfully validated.
- **Real API contract drift:** WireMock stubs are a snapshot of the expected API contract. If Finnhub changes its response format, the stubs must be manually updated.

**Setting up the adapter without Spring:** `ReflectionTestUtils` injects the WireMock server's URL and a test API key directly into the adapter's `@Value`-annotated fields, bypassing the need for a Spring context:

```java
@BeforeEach
void setUp(WireMockRuntimeInfo wmInfo) {
    adapter = new FinhubStockPriceAdapter();
    ReflectionTestUtils.setField(adapter, "finhubApiKey", "test-key");
    ReflectionTestUtils.setField(adapter, "finhubApiUrl", wmInfo.getHttpBaseUrl());
}
```

This keeps the adapter tests fast (no Spring context) and focused (only the HTTP contract is under test).

### Solution Part 2: FixedPriceStockPriceAdapter for Integration Tests

WireMock solves adapter-level testing. But for full-stack integration tests that verify exact FIFO calculations, a different approach is needed. The `PortfolioTradingRestIntegrationTest` introduces a `FixedPriceStockPriceAdapter` — a queue-based adapter that returns pre-configured prices in order:

```java
static class FixedPriceStockPriceAdapter implements StockPriceProviderPort {

    private final ConcurrentLinkedQueue<Price> priceQueue = new ConcurrentLinkedQueue<>();
    private final Price fallbackPrice;

    void enqueuePrice(Price price) { priceQueue.add(price); }

    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        Price price = priceQueue.poll();
        return new StockPrice(ticker, price != null ? price : fallbackPrice, Instant.now());
    }
}
```

This adapter is registered as a `@Primary` Spring bean via `@TestConfiguration`:

```java
@TestConfiguration
static class FixedPriceConfiguration {
    @Bean @Primary
    FixedPriceStockPriceAdapter fixedPriceStockPriceAdapter() {
        return new FixedPriceStockPriceAdapter(Price.of("150.00"));
    }
}
```

The Gherkin FIFO tests then enqueue the exact prices from the specification:

```java
@BeforeEach
void setUpGherkinScenario() {
    fixedPriceAdapter.clear();
    portfolioId = createPortfolio("Alice");
    deposit(portfolioId, 100_000);

    fixedPriceAdapter.enqueuePrice(Price.of("100.00"));    // buy @ 100 (Lot #1)
    buy(portfolioId, "AAPL", 10);

    fixedPriceAdapter.enqueuePrice(Price.of("120.00"));    // buy @ 120 (Lot #2)
    buy(portfolioId, "AAPL", 5);
}
```

When the sell test enqueues `Price.of("150.00")` and sells 12 shares, the financial results are deterministic: `proceeds = 1800.00`, `costBasis = 1240.00`, `profit = 560.00`. The `costBasis` value *is* the mathematical proof of FIFO order — if LIFO were used, costBasis would be `5×120 + 7×100 = 1300.00`.

### Solution Part 3: Three Adapters, One Port Interface

HexaStock has three implementations of `StockPriceProviderPort`, each serving a different purpose:

| Adapter | Purpose | Activated By |
|---|---|---|
| `FinhubStockPriceAdapter` | Production: real Finnhub API | `finhub` profile |
| `AlphaVantageStockPriceAdapter` | Production: alternative provider | `alphaVantage` profile |
| `MockFinhubStockPriceAdapter` | Testing: random prices in [10, 1000] | `mockfinhub` profile |
| `FixedPriceStockPriceAdapter` | Testing: deterministic price queue | `@Primary` in `@TestConfiguration` |

None of these adapters required changes to domain or application code. The `PortfolioStockOperationsService` calls `stockPriceProviderPort.fetchStockPrice(ticker)` regardless of which adapter is injected. This is not an abstract design claim — it is proven by the test suite, which runs the same business logic with different adapters in different test classes.

---

## 10. Deep Dive: Persistence Adapter Testing with Future Portability

### The Architecture

In HexaStock's hexagonal architecture, the application layer defines outbound port interfaces — `PortfolioPort` and `TransactionPort` — that describe what the application needs from persistence, without specifying *how* persistence works. The JPA adapter implements these ports using Spring Data JPA and MySQL. But the hexagonal promise is that we could replace JPA with MongoDB, Redis, or any other technology without changing the application or domain layers.

Testing this promise requires a testing pattern that separates *what the port must do* from *how the adapter does it*.

### The Abstract Contract Pattern

The `AbstractPortfolioPortContractTest` defines the complete contract for `PortfolioPort` — nine test methods covering five scenarios:

```
application/src/test/java/
└── application/port/out/
    ├── AbstractPortfolioPortContractTest.java     ← 5 contract tests (technology-agnostic)
    └── AbstractTransactionPortContractTest.java   ← 4 contract tests (technology-agnostic)
```

These abstract classes live in the `application` module — the same module that defines the port interfaces. They have **no Spring annotations, no JPA, no Testcontainers**. They use only JUnit 5 and AssertJ. Each subclass must implement a single method:

```java
/** Subclasses provide the implementation under test. */
protected abstract PortfolioPort port();
```

### The JPA Implementation

The JPA adapter module subclasses the abstract contract:

```
adapters-outbound-persistence-jpa/src/test/java/
└── adapter/out/persistence/jpa/repository/
    ├── JpaPortfolioRepositoryContractTest.java     ← Runs 5 contract tests against MySQL
    └── JpaTransactionRepositoryContractTest.java   ← Runs 4 contract tests against MySQL
```

The JPA subclass adds all the infrastructure wiring: `@DataJpaTest`, `@DynamicPropertySource` pointing to the shared Testcontainers MySQL, `@Import` of the repository class, and `@ActiveProfiles("jpa")`.

### The Override-and-Delegate Pattern

A subtle but important detail: each contract test method is overridden in the JPA subclass with a one-line delegation to `super`:

```java
@Override @Test protected void createAndGetById_roundTrip() {
    super.createAndGetById_roundTrip();
}
```

This is required because Spring's `TransactionalTestExecutionListener` resolves `@Transactional` by looking at the **concrete class**, not the abstract superclass. If the test methods were inherited without override, Spring would not find `@Transactional` (via `@DataJpaTest`) on the method's declaring class, and the test would run without transaction management — silently producing incorrect results. The override ensures Spring resolves the transactional boundary from the concrete JPA test class.

### The SharedMySQLContainer Singleton

All JPA tests in the module share a single MySQL container instance, started once on first class-load:

```java
public final class SharedMySQLContainer {
    public static final MySQLContainer<?> INSTANCE =
            new MySQLContainer<>("mysql:8.0.32").withDatabaseName("testdb");

    static { INSTANCE.start(); }

    private SharedMySQLContainer() { }
}
```

Each test class references the singleton via `@DynamicPropertySource`:

```java
@DynamicPropertySource
static void dbProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
}
```

This means the MySQL container starts once per JVM, not once per test class — saving ~5 seconds per class. The `@Transactional` annotation (from `@DataJpaTest`) ensures each test runs in its own transaction that is rolled back after the test, so tests are isolated despite sharing the same database.

### Why Not H2?

HexaStock previously used H2 for persistence tests but migrated to Testcontainers MySQL for several reasons:

1. **SQL dialect differences:** H2's SQL dialect diverges from MySQL in subtle ways — reserved word handling, default collation, auto-increment behaviour. A persistence test that passes on H2 may fail on MySQL in production.
2. **Locking behaviour:** The `JpaPessimisticLockingTest` verifies `SELECT ... FOR UPDATE` with InnoDB row-level locking. H2 does not support InnoDB and handles pessimistic locks differently.
3. **Production fidelity:** Testcontainers runs the exact same MySQL 8.0.32 image used in production. If a query works in the test, it works in production. This eliminates an entire class of "works on my machine" failures.

### Future Portability: Adding MongoDB

If HexaStock were migrated to MongoDB, the testing pattern would be:

1. Create a new module `adapters-outbound-persistence-mongo`.
2. Implement `MongoPortfolioRepository` that implements `PortfolioPort`.
3. Create `MongoPortfolioRepositoryContractTest` extending `AbstractPortfolioPortContractTest`.
4. Provide a Testcontainers MongoDB instance and implement `port()` to return the MongoDB repository.
5. Run the tests — all nine contract assertions execute automatically.

The abstract contract does not need to change. The domain does not need to change. The application layer does not need to change. Only the new adapter module is added, and the contract tests guarantee that its behaviour matches the JPA adapter.

### JPA-Specific Tests

Not all persistence concerns are expressed through ports. The `JpaPessimisticLockingTest` verifies JPA-specific behaviour — `findByIdForUpdate` acquiring a pessimistic lock and the locked entity being managed in the persistence context:

```java
@Test
void findByIdForUpdate_acquiresPessimisticLock() {
    springDataRepository.saveAndFlush(entity);
    em.clear();

    var locked = springDataRepository.findByIdForUpdate("p-lock");

    assertThat(locked).isPresent();
    assertThat(em.contains(locked.get()))
            .as("Entity should be managed (lock held in current persistence context)")
            .isTrue();
}
```

This test is explicitly *not* part of the port contract — a MongoDB adapter would not have pessimistic locking. It lives in the JPA module and tests JPA-specific guarantees.

---

## 11. Cross-Cutting: Requirements Traceability

### The @SpecificationRef Annotation

HexaStock implements a lightweight traceability chain linking business requirements to running code:

```
API Specification  →  Gherkin Scenario (.feature)  →  @SpecificationRef  →  Test Method  →  Production Code
```

The `@SpecificationRef` annotation tags each test with the Gherkin scenario it verifies:

```java
@SpecificationRef(
    value = "US-07.FIFO-1",                // Scenario identifier
    level = TestLevel.DOMAIN,               // DOMAIN or INTEGRATION
    feature = "sell-stocks.feature"          // Source .feature file
)
```

This annotation is `@Repeatable` — a single test can reference multiple acceptance criteria — and purely informational. It creates no runtime behaviour; it is metadata that makes the testing strategy auditable.

### Gherkin as Specification, Not Execution

The 15 Gherkin feature files in `doc/features/` are **specification documents**, not executable Cucumber tests. HexaStock does not use a BDD framework. The Gherkin scenarios define the expected behaviour; JUnit tests annotated with `@SpecificationRef` are the executable layer. This keeps the testing infrastructure simple while preserving the specification's business-readable format.

### Specification Coverage by Layer

The sell stock use case demonstrates how the same Gherkin scenario is verified at multiple testing levels:

| Scenario | Domain Test | Application Test | REST Test | Integration Test |
|---|---|---|---|---|
| US-07.FIFO-1 (single-lot sell) | `PortfolioTest`, `HoldingTest` | `PortfolioStockOperationsServiceTest` | `PortfolioRestControllerTest` | `PortfolioTradingRestIntegrationTest` |
| US-07.FIFO-2 (cross-lot sell) | `PortfolioTest`, `HoldingTest` | — | — | `PortfolioTradingRestIntegrationTest` |

Each level verifies the same requirement, but from a different perspective and catching a different class of defect.

---

## 12. Test Suite Inventory

### By Module and Layer

| Module | Layer | Test Classes | Test Methods | Key Technologies |
|---|---|---|---|---|
| `domain` | Domain | 7 | ~99 | JUnit 5, `@ParameterizedTest`, `@SpecificationRef` |
| `application` | Services | 6 | 27 | JUnit 5, Mockito |
| `application` | Contracts (abstract) | 2 | 9 (template) | JUnit 5, AssertJ |
| `adapters-inbound-rest` | REST | 2 | 19 | `@WebMvcTest`, MockMvc, Mockito |
| `adapters-outbound-market` | Market | 3 | 11 | WireMock, AssertJ |
| `adapters-outbound-persistence-jpa` | Persistence | 4 | 21 | `@DataJpaTest`, Testcontainers, AssertJ |
| `bootstrap` | Integration | 5 | 43 | `@SpringBootTest`, Testcontainers, RestAssured |
| `bootstrap` | Architecture | 1 | 5+ | ArchUnit |
| **Total** | | **30** | **~234** | |

### Gherkin Feature Files

| Feature File | Use Case | Status |
|---|---|---|
| `create-portfolio.feature` | US-01 — Create Portfolio | Implemented |
| `get-portfolio.feature` | US-02 — Get Portfolio | Implemented |
| `list-portfolios.feature` | US-03 — List All Portfolios | Implemented |
| `deposit-funds.feature` | US-04 — Deposit Funds | Implemented |
| `withdraw-funds.feature` | US-05 — Withdraw Funds | Implemented |
| `buy-stocks.feature` | US-06 — Buy Stocks | Implemented |
| `sell-stocks.feature` | US-07 — Sell Stocks | Implemented |
| `get-transaction-history.feature` | US-08 — Transaction History | Implemented |
| `get-holdings-performance.feature` | US-09 — Holdings Performance | Implemented |
| `get-stock-price.feature` | US-10 — Get Stock Price | Implemented |
| `settlement-aware-selling.feature` | Settlement-Aware Selling | Future |
| `reserved-lot-handling.feature` | Reserved Lot Handling | Future |
| `settlement-fees.feature` | Settlement Fee Accounting | Future |
| `fifo-settlement-selling.feature` | FIFO Settlement & Lot Lifecycle | Future |
| `rule-consistency.feature` | Rule Consistency & Drift Detection | Future |

---

## 13. Running the Tests

### Prerequisites

- **Java 21** (Temurin recommended)
- **Docker** running (required by Testcontainers)
- **Maven** (or use the included `./mvnw` wrapper)

### Run All Tests

```bash
./mvnw clean verify
```

This runs all seven test layers, including Testcontainers-managed MySQL instances. JaCoCo coverage data is collected during the build.

### Run a Single Module

```bash
./mvnw -pl domain test                          # domain unit tests only (~milliseconds)
./mvnw -pl application test                      # application service tests
./mvnw -pl adapters-inbound-rest test            # REST adapter tests
./mvnw -pl adapters-outbound-market test         # WireMock market adapter tests
./mvnw -pl adapters-outbound-persistence-jpa test  # JPA + Testcontainers tests
./mvnw -pl bootstrap test                        # integration + ArchUnit tests
```

### Run Only Domain Tests (No Docker)

```bash
./mvnw -pl domain test
```

Domain tests have zero infrastructure dependencies — they run without Docker, without a database, and without network access.

---

## 14. References

### Foundational Works

- Beck, Kent. *Test-Driven Development: By Example.* Addison-Wesley, 2003.
- Cockburn, Alistair. \"Hexagonal Architecture.\" 2005. https://alistair.cockburn.us/hexagonal-architecture/
- Cohn, Mike. *Succeeding with Agile: Software Development Using Scrum.* Addison-Wesley, 2009. (The test pyramid.)
- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Fowler, Martin. \"TestPyramid.\" *martinfowler.com*, 2012. https://martinfowler.com/bliki/TestPyramid.html
- Fowler, Martin. \"TestDouble.\" *martinfowler.com*, 2006. https://martinfowler.com/bliki/TestDouble.html
- Meszaros, Gerard. *xUnit Test Patterns: Refactoring Test Code.* Addison-Wesley, 2007.
- North, Dan. \"Introducing BDD.\" *Better Software*, March 2006. https://dannorth.net/introducing-bdd/
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013.

### Testing Framework Documentation

- **JUnit 5:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito:** https://site.mockito.org/
- **AssertJ:** https://assertj.github.io/doc/
- **Testcontainers:** https://testcontainers.com/
- **WireMock:** https://wiremock.org/docs/
- **ArchUnit:** https://www.archunit.org/userguide/html/000_Index.html
- **REST Assured:** https://rest-assured.io/

### Spring Testing Documentation

- **`@WebMvcTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests
- **`@DataJpaTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa
- **`@SpringBootTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html

### Project Documents

- [Sell Stock Tutorial](sellStocks/SELL-STOCK-TUTORIAL.md) — end-to-end use case traced through every code layer
- [Tutorial README — Traceability Chain](README.md) — `@SpecificationRef` architecture and identifier conventions
- [Stock Portfolio API Specification](../stock-portfolio-api-specification.md) — all 10 use cases with acceptance criteria
- [Gherkin Feature Files](../features/) — 15 `.feature` files providing executable behavioural specifications
- [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](CONCURRENCY-PESSIMISTIC-LOCKING.md) — race-condition demonstrations with real tests

---

*This document is part of the [HexaStock documentation ecosystem](sellStocks/SELL-STOCK-TUTORIAL.md#reading-map-the-hexastock-documentation-ecosystem).*
