# HexaStock: Engineering Architecture That Grows Stronger Through Change

**Sell Stock Deep Dive — A Technical Tutorial on Domain-Driven Design, Hexagonal Architecture, and Specification-Driven Development in a Financial Domain**

> *"Architecture is not documentation. It is an operational capability."*

---

## About This Tutorial

This tutorial traces a single use case — **selling stocks** — through every architectural layer of HexaStock, a stock portfolio management system built with Java 21, Spring Boot 3, Domain-Driven Design (DDD), and Hexagonal Architecture. By following one request end to end — from Gherkin specification to REST controller, through application service orchestration, into the aggregate root's FIFO lot-consumption algorithm, and back as a structured financial result — the reader sees how these engineering disciplines work together as applied practice, not abstract principles.

**Intended audience:** Software engineers, architects, and technical leads with working knowledge of Java and Spring Boot who want to see DDD and Hexagonal Architecture applied in a realistic codebase.

**What you will learn**

- One end-to-end use case — selling stocks — traced through every layer, from REST controller to persistence and back.
- How the aggregate root `Portfolio` protects business invariants while the application service orchestrates without making business decisions.
- How value objects such as `Money`, `ShareQuantity`, and `Ticker` eliminate primitive obsession and encode ubiquitous language in the type system.
- How Gherkin → `@SpecificationRef` → JUnit → production code creates a traceable specification chain.
- Hexagonal architecture proven swappable in practice: replacing a live price provider with `FixedPriceStockPriceAdapter` in tests requires zero changes to domain or application code.

---

## HexaStock in Brief

HexaStock is a stock portfolio management platform supporting 10 use cases — portfolio creation, fund management, stock trading with automatic FIFO lot accounting, holdings performance tracking, and transaction history. It is structured as a Maven multi-module project where module boundaries codify the dependency rule at build time:

```
HexaStock (parent pom)
├── domain/                              → Pure business model — no framework dependencies
├── application/                         → Use case orchestration, inbound and outbound ports
├── adapters-inbound-rest/               → Driving adapter: REST controllers, DTOs, error mapping
├── adapters-outbound-persistence-jpa/   → Driven adapter: JPA entities, repositories, mappers
├── adapters-outbound-market/            → Driven adapter: external stock-price provider clients
└── bootstrap/                           → Spring Boot entry point, composition root, runtime wiring
```

The domain model has zero framework dependencies. All communication with infrastructure passes through port interfaces. Dependencies point inward — adapters depend on ports, never the reverse. Maven module boundaries codify the dependency rule at build time because `domain/pom.xml` does not declare any infrastructure dependencies (Spring, JPA, etc.). The adapter modules depend on ports, and ports are defined in the application module. Any attempt to add a direct dependency from the domain to an outbound adapter module would fail unless that dependency were explicitly declared. For the full project background — detailed module descriptions, domain package layout, architectural identity, and the rationale behind the multi-module structure — see **[HexaStock — Project Overview](HEXASTOCK-PROJECT-OVERVIEW.md)**.

---

## Specification-First Engineering

HexaStock follows a disciplined engineering sequence rooted in **Behaviour-Driven Development (BDD)** and **specification-driven design**:

> **Specification → Contract → Tests → Implementation → Refactor Safely**

Behaviour is defined as Gherkin scenarios before any design decisions are made. The REST API is specified contract-first using OpenAPI 3.0. Tests are linked to specifications through `@SpecificationRef` annotations, creating a traceable chain from business requirements to running code. For how this specification chain also enables AI-assisted development, see **[Specification-Driven Development with AI](../specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md)**.

---

## Ubiquitous Language

Domain-Driven Design demands a single, shared vocabulary — **Ubiquitous Language** — that appears consistently across Gherkin scenarios, domain classes, diagrams, tests, REST endpoints, and package names. In HexaStock, terms like *Portfolio*, *Holding*, *Lot*, *proceeds*, *costBasis*, and *FIFO* form this vocabulary; each term means the same thing in every artifact, and terminology drift is treated as a modelling defect.

For the full treatment — including a cross-artifact traceability table, concrete examples from the sell-stock use case, and a discussion of what goes wrong without consistent naming — see the companion document **[Ubiquitous Language in HexaStock](UBIQUITOUS-LANGUAGE.md)**.

---

## 1. Architecture Overview (Hexagonal / Ports & Adapters)

Before tracing the sell-stock flow through code, this section details the layers, ports, and adapters that form HexaStock's hexagonal architecture — the structural vocabulary the rest of the tutorial relies on.

### Core Architectural Layers

**Application Core** — The heart of the system, completely isolated from external technologies:
- **Domain Layer:** Contains pure business logic (entities, value objects, domain services). This is where business rules like FIFO accounting, invariant protection, and portfolio consistency are enforced. Examples: `Portfolio`, `Holding`, `Lot`, `Ticker`, `Money`, `Price`, `ShareQuantity`, `PortfolioId`, `HoldingId`, `LotId`.
- **Application Layer:** Orchestrates use cases by coordinating domain objects and ports. Application services are thin coordinators with no business logic—they retrieve data, delegate decisions to the domain, and persist results. Examples: `PortfolioStockOperationsService`.

**Ports** — Interfaces that define contracts between the core and the outside world:
- **Inbound Ports (Primary/Driving):** Define what the application can do. These are use case interfaces implemented by application services. Examples: `PortfolioStockOperationsUseCase`.
- **Outbound Ports (Secondary/Driven):** Define what the application needs from external systems. The core depends on these abstractions, not on concrete implementations. Examples: `PortfolioPort`, `StockPriceProviderPort`, `TransactionPort`.

**Adapters** — Concrete implementations that connect the core to the real world:
- **Inbound Adapters (Driving):** Receive requests from users or external systems and translate them into domain operations. Examples: `PortfolioRestController` (HTTP/REST), potential CLI or messaging adapters.
- **Outbound Adapters (Driven):** Implement outbound ports to interact with databases, external APIs, or other infrastructure. Examples: JPA repositories for persistence, Finnhub/AlphaVantage clients for stock prices.

**Dependency Direction:** All dependencies point **inward** toward the domain. Adapters depend on ports, ports are defined by the core, and the domain has zero dependencies on infrastructure. This is **Dependency Inversion** in action.

The following diagram shows HexaStock's hexagonal architecture for the sell-stock use case, mapping each Maven module to its architectural role:

![HexaStock Hexagonal Architecture](https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/diagrams/Rendered/HexaStock_SellStocks.vpd.png?raw=true)

> **📚 Reference sources and diagrams:** For the original 2005 article by Alistair Cockburn, who introduced Hexagonal Architecture (Ports and Adapters), together with widely cited community diagrams by Tom Hombergs and Herberto Graça, see the companion document **[Hexagonal Architecture — Reference Sources and Diagrams](HEXAGONAL-ARCHITECTURE-REFERENCE-DIAGRAMS.md)**.

### How This Tutorial Maps to the Architecture

The sell stock use case flows through these architectural layers:

- **Primary (Driving) Adapters** → `PortfolioRestController` in package `adapter.in`
- **Inbound Ports** → `PortfolioStockOperationsUseCase` interface in `application.port.in`
- **Application Layer** → `PortfolioStockOperationsService` orchestrates the use case, manages transaction boundaries, and coordinates between ports
- **Domain Layer** → `Portfolio` (aggregate root), `Holding`, `Lot` (entities), `Ticker`, `Money`, `Price`, `ShareQuantity`, `SellResult`, `PortfolioId`, `HoldingId`, `LotId` (value objects), domain exceptions
- **Outbound Ports** → `PortfolioPort` (persistence abstraction), `StockPriceProviderPort` (external price data), `TransactionPort` (audit log)
- **Secondary (Driven) Adapters** → JPA repositories (`PortfolioJpaAdapter`), external API clients (`FinnhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`), transaction repositories

Sections 9–15 trace a real HTTP request flowing through these layers, showing how each component fulfils its architectural role while maintaining strict separation of concerns.

---

## 2. Purpose and Scope

The sell-stock use case provides the unifying thread that ties every engineering phase together — from Gherkin specification through domain modelling, hexagonal structure, persistence mapping, error handling, and integration testing. The tutorial follows one request end to end, showing how BDD specifications, DDD aggregates, hexagonal ports and adapters, value objects, FIFO accounting, and a four-level testing strategy work together as applied engineering, not abstract principles.

---

## 3. Functional Specification (Behaviour)

Before designing the domain model or writing implementation code, we start by defining the **observable behaviour** of the sell use case.

User stories typically capture the intent of a feature at a high level, but they are often too ambiguous to serve as executable specifications. Behavior-driven scenarios written in formats such as Gherkin describe concrete system behaviour through explicit inputs, actions, and expected outcomes. Because of this precision, automated tests can often be derived directly from Gherkin scenarios. For this reason, this tutorial uses Gherkin scenarios as the primary functional specification of the sell operation.

The Gherkin scenarios below describe what the system must do in business terms, independent of any technical design decisions.

**Source of truth:** [US-07 — Sell Stocks (API Specification)](https://github.com/alfredorueda/HexaStock/blob/main/doc/stock-portfolio-api-specification.md#27-us-07--sell-stocks)

> **Canonical Gherkin:** [`doc/features/sell-stocks.feature`](../../features/sell-stocks.feature) — the scenarios below are reproduced for readability; the `.feature` file is the single source referenced by `@SpecificationRef` annotations in tests.

```gherkin
Feature: Sell Stocks with FIFO Lot Consumption

  Background:
    Given a portfolio exists for owner "Alice"
    And the portfolio holds AAPL with the following lots (in purchase order):
      | Lot # | Shares | Purchase Price |
      |     1 |     10 |        100.00  |
      |     2 |      5 |        120.00  |
    And the current market price for AAPL is 150.00

  Scenario: Selling shares consumed entirely from a single lot
    When I sell 8 shares of AAPL
    Then the sale response contains:
      | Field     | Value   |
      | ticker    | AAPL    |
      | quantity  |       8 |
      | proceeds  | 1200.00 |
      | costBasis |  800.00 |
      | profit    |  400.00 |
    And FIFO consumed 8 shares from Lot #1 at 100.00
    And the AAPL holding lots are now:
      | Lot # | Initial Shares | Remaining Shares | Purchase Price |
      |     1 |             10 |                2 |        100.00  |
      |     2 |              5 |                5 |        120.00  |
    And the portfolio cash balance has increased by 1200.00

  # Calculation breakdown:
  #   FIFO step 1: Lot #1 has 10 remaining → take min(10, 8) = 8 shares
  #                costBasis contribution = 8 × 100.00 = 800.00
  #                Lot #1 remaining: 10 − 8 = 2
  #   Total shares sold: 8 (request fulfilled)
  #   proceeds  = 8 × 150.00  = 1200.00
  #   costBasis = 800.00
  #   profit    = 1200.00 − 800.00 = 400.00

  Scenario: Selling shares consumed across multiple lots
    When I sell 12 shares of AAPL
    Then the sale response contains:
      | Field     | Value   |
      | ticker    | AAPL    |
      | quantity  |      12 |
      | proceeds  | 1800.00 |
      | costBasis | 1240.00 |
      | profit    |  560.00 |
    And FIFO consumed 10 shares from Lot #1 at 100.00 and 2 shares from Lot #2 at 120.00
    And Lot #1 is fully depleted and removed
    And the AAPL holding lots are now:
      | Lot # | Initial Shares | Remaining Shares | Purchase Price |
      |     2 |              5 |                3 |        120.00  |
    And the portfolio cash balance has increased by 1800.00

  # Calculation breakdown:
  #   FIFO step 1: Lot #1 has 10 remaining → take min(10, 12) = 10 shares
  #                costBasis contribution = 10 × 100.00 = 1000.00
  #                Lot #1 remaining: 10 − 10 = 0 → lot is empty, removed
  #                Shares still to sell: 12 − 10 = 2
  #   FIFO step 2: Lot #2 has 5 remaining → take min(5, 2) = 2 shares
  #                costBasis contribution = 2 × 120.00 = 240.00
  #                Lot #2 remaining: 5 − 2 = 3
  #                Shares still to sell: 2 − 2 = 0
  #   Total shares sold: 12 (request fulfilled)
  #   proceeds  = 12 × 150.00 = 1800.00
  #   costBasis = 1000.00 + 240.00 = 1240.00
  #   profit    = 1800.00 − 1240.00 = 560.00
```

---

## 4. Executable Specification (JUnit)

The Gherkin scenarios above describe observable behaviour at the **Portfolio level** — the aggregate root in our DDD model. Because this scenario describes observable behaviour at the `Portfolio` level, the most appropriate primary executable specification validates it through `Portfolio.sell(...)`, while a complementary `Holding` test verifies the internal FIFO algorithm in isolation. This is a deliberate DDD design choice: business behaviour is exposed by the aggregate root, and tests should reflect that boundary.

HexaStock validates this behaviour at **two complementary levels**:

1. **Aggregate-level behaviour verification** — a `Portfolio` test that exercises the complete sell operation through the aggregate root, verifying financial results, balance updates, and FIFO lot consumption as a single consistent unit.
2. **Focused algorithm verification** — a `Holding` test that verifies the internal FIFO lot-consumption algorithm in isolation, independent of portfolio-level concerns like cash balance.

### Primary: Aggregate Root Test (`Portfolio`)

This test is the direct executable translation of the Gherkin scenario. It invokes `Portfolio.sell(...)` exactly as the application service would, and asserts every observable outcome described in the specification: proceeds, cost basis, profit, balance update, and FIFO lot state.

**Test source:** [PortfolioTest.java — shouldSellSharesUsingFIFOThroughPortfolioAggregateRoot_GherkinScenario](https://github.com/alfredorueda/HexaStock/blob/9f52de7b30dd683952b5a1b10ac63c878535444a/src/test/java/cat/gencat/agaur/hexastock/model/PortfolioTest.java#L201)

```java
@Test
@DisplayName("Should sell shares across multiple lots using FIFO through the aggregate root (Gherkin scenario)")
void shouldSellSharesUsingFIFOThroughPortfolioAggregateRoot_GherkinScenario() {
    // Background: a portfolio with sufficient funds to buy AAPL lots
    Price purchasePrice1 = Price.of("100.00");
    Price purchasePrice2 = Price.of("120.00");
    Price marketSellPrice = Price.of("150.00");

    Portfolio fundedPortfolio = new Portfolio(
            PortfolioId.generate(), "Alice", Money.of("10000.00"), LocalDateTime.now());

    // Background: buy 10 shares of AAPL @ 100, then 5 shares @ 120
    fundedPortfolio.buy(APPLE, ShareQuantity.of(10), purchasePrice1);
    fundedPortfolio.buy(APPLE, ShareQuantity.of(5), purchasePrice2);

    Money balanceBeforeSell = fundedPortfolio.getBalance(); // 10000 - 1000 - 600 = 8400

    // When: sell 12 shares of AAPL @ 150 through the aggregate root
    SellResult result = fundedPortfolio.sell(APPLE, ShareQuantity.of(12), marketSellPrice);

    // Then: financial results match Gherkin expectations
    assertEquals(Money.of("1800.00"), result.proceeds());   // 12 × 150
    assertEquals(Money.of("1240.00"), result.costBasis());   // (10 × 100) + (2 × 120)
    assertEquals(Money.of("560.00"), result.profit());       // 1800 − 1240

    // And: portfolio balance increased by proceeds
    assertEquals(balanceBeforeSell.add(Money.of("1800.00")), fundedPortfolio.getBalance());

    // And: FIFO lot consumption — only Lot #2 survives with 3 remaining shares
    Holding aaplHolding = fundedPortfolio.getHolding(APPLE);
    assertEquals(ShareQuantity.of(3), aaplHolding.getTotalShares());
    assertEquals(1, aaplHolding.getLots().size());

    Lot remainingLot = aaplHolding.getLots().getFirst();
    assertEquals(ShareQuantity.of(3), remainingLot.getRemainingShares());
    assertEquals(purchasePrice2, remainingLot.getUnitPrice());
}
```

> **💡 Why test through the aggregate root?** The Gherkin scenario says *"the portfolio cash balance has increased by 1800.00"* — this is a Portfolio-level invariant. Only a test that calls `Portfolio.sell(...)` can verify that the balance update and the FIFO lot consumption happen together atomically and consistently. A `Holding`-level test cannot observe the balance at all.

### Complementary: Internal FIFO Algorithm Test (`Holding`)

HexaStock also contains a more focused domain test at the `Holding` level that verifies the FIFO lot-consumption algorithm in isolation. This lower-level test validates that the internal rule implementation is correct — shares are consumed from the oldest lot first, depleted lots are removed, and cost basis is calculated correctly — without involving portfolio-level concerns such as cash balance management.

**Test source:** [HoldingTest.java — shouldSellSharesAcrossMultipleLots_GherkinScenario](https://github.com/alfredorueda/HexaStock/blob/44fa1ff6e29b79faccb0952a5103475eb4f03061/src/test/java/cat/gencat/agaur/hexastock/model/HoldingTest.java#L181)

```java
@Test
@DisplayName("Should sell shares across multiple lots using FIFO (Gherkin scenario)")
void shouldSellSharesAcrossMultipleLots_GherkinScenario() {
    // Background: buy 10 shares @ 100, then 5 shares @ 120
    holding.buy(ShareQuantity.of(10), PRICE_100);
    holding.buy(ShareQuantity.of(5), PRICE_120);

    // When: sell 12 shares @ 150 (market price from Gherkin)
    SellResult result = holding.sell(ShareQuantity.of(12), PRICE_150);

    // Then: 3 remaining shares, only Lot #2 survives
    assertEquals(ShareQuantity.of(3), holding.getTotalShares());
    assertEquals(1, holding.getLots().size());

    Lot remainingLot = holding.getLots().getFirst();
    assertEquals(ShareQuantity.of(3), remainingLot.getRemainingShares());
    assertEquals(PRICE_120, remainingLot.getUnitPrice());

    // And: financial results match Gherkin expectations
    assertEquals(Money.of("1800.00"), result.proceeds());
    assertEquals(Money.of("1240.00"), result.costBasis());
    assertEquals(Money.of("560.00"), result.profit());
}
```

> **💡 Two levels, one truth:** Both tests verify the same FIFO financial results (proceeds, cost basis, profit). The Portfolio test additionally verifies aggregate consistency (balance update, aggregate encapsulation). The Holding test provides a fast, focused verification of the algorithm itself. Together they form a complete executable specification at the appropriate DDD abstraction levels.

---

## 5. Testing Strategy Overview

HexaStock verifies the sell use case at four complementary testing levels:

1. **Domain algorithm tests** — validate the FIFO lot-consumption logic in isolation (`HoldingTest`)
2. **Aggregate behaviour tests** — validate portfolio invariants and financial consistency through the aggregate root (`PortfolioTest`)
3. **Integration tests** — validate the complete flow through HTTP, application services, persistence, and adapters (`PortfolioTradingRestIntegrationTest`)
4. **Architecture fitness tests** — validate that dependency directions conform to hexagonal architecture rules (`HexagonalArchitectureTest`)

The first two levels are introduced in sections 3–4, alongside the functional specification, because they verify domain logic independently of infrastructure. Integration tests appear later in section 16, once the full architecture — controllers, services, ports, adapters, and persistence — has been explained.

Architecture fitness tests operate at a different axis: rather than verifying business behaviour, they verify structural constraints. Using [ArchUnit](https://www.archunit.org/), `HexagonalArchitectureTest` scans compiled classes from all modules and enforces six rules:

| Layer | Rule |
|---|---|
| **Domain** | Must not depend on the application layer |
| **Domain** | Must not depend on any adapter |
| **Domain** | Must not depend on the Spring Framework |
| **Application** | Must not depend on any adapter |
| **Adapter (inbound)** | Must not depend on outbound adapters |
| **Adapter (outbound)** | Must not depend on inbound adapters |

These rules complement the Maven module boundaries described earlier. Maven prevents illegal compile-time dependencies *between modules*; ArchUnit catches illegal dependencies *within the same compiled classpath*, including transitive ones that module boundaries alone cannot detect. Together, they form a two-layer structural safety net.

This ordering reflects the natural direction of design: define behaviour first, model the domain, then verify the entire stack — and finally, guard the architecture itself.

---

## 6. Domain Context: What "Selling Stocks" Means in HexaStock

<a href="diagrams/Rendered/HexaStock%20Domain%20Model.svg"><img src="diagrams/Rendered/HexaStock%20Domain%20Model.png" alt="HexaStock Domain Model" width="100%" /></a>

> **📐 Design note:** In the diagram above, `Portfolio` and `Transaction` are modeled as **separate aggregates**. This is a deliberate DDD design decision — transaction history is kept outside the Portfolio aggregate boundary. The rationale is explained in the companion document [Portfolio and Transaction Aggregates in DDD](https://github.com/alfredorueda/HexaStock/blob/main/doc/DDD%20Portfolio%20and%20Transactions.md).

In this system:

- A **Portfolio** represents an investor's account containing cash (`Money`) and stock holdings
- A **Holding** tracks all shares owned for a specific stock ticker (e.g., `Ticker.of("AAPL")`)
- A **Lot** represents a single purchase transaction—a batch of shares (`ShareQuantity`) bought at a specific price (`Price`) and time
- **FIFO (First-In-First-Out)** accounting is used: when selling, the oldest lots are sold first
- A **Transaction** record is created for every financial activity (deposit, withdrawal, purchase, sale)

When you sell stocks in HexaStock:
1. The system fetches the current market price (returned as a `StockPrice` containing a `Price` value object)
2. It applies FIFO to determine which lots to draw from
3. It calculates proceeds (`Money` received), cost basis (`Money` originally paid), and profit/loss (`Money`)
4. It updates the portfolio's cash balance (`Money`) and holdings
5. It records a transaction for audit purposes

> **💡 Why Value Objects?**
> The domain uses `Money`, `Price`, `ShareQuantity`, `Ticker`, `PortfolioId`, `HoldingId`, and `LotId` instead of primitives (`BigDecimal`, `int`, `String`). This eliminates an entire class of bugs (e.g., passing a quantity where a price is expected), enforces validation at construction time, and makes the code self-documenting through the [ubiquitous language](#ubiquitous-language-one-domain-vocabulary-across-all-artifacts).

> **📖 Architectural perspective:** The fact that `Portfolio`, `Holding`, and `Lot` contain behaviour — not just data — is a deliberate design choice known as a **rich domain model**. To understand how this design compares to an anemic alternative where entities are plain data holders, see **[Rich vs Anemic Domain Model](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)**.

---

## 7. The REST Entry Point of the SELL Use Case

**File:** `src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java`

**Endpoint:** `POST /api/portfolios/{id}/sales`

```java
@PostMapping("/{id}/sales")
public ResponseEntity<SaleResponseDTO> sellStock(@PathVariable String id,
                                                 @RequestBody SaleRequestDTO request) {
    SellResult result =
            portfolioStockOperationsUseCase.sellStock(
                    PortfolioId.of(id),
                    Ticker.of(request.ticker()),
                    ShareQuantity.positive(request.quantity()));
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
- It converts HTTP-specific data (JSON, path variables) into **Value Objects** (`PortfolioId.of(id)`, `Ticker.of(...)`, `ShareQuantity.positive(...)`)
- It converts domain results (`SellResult`) into DTOs (`SaleResponseDTO`)
- It handles HTTP concerns (status codes, response entities)

> **💡 Boundary mapping:** Notice how the controller is the **translation layer** between the external world (primitives in JSON/path) and the domain world (Value Objects). The DTO `SaleRequestDTO(String ticker, int quantity)` uses primitives because JSON is a primitive format. The controller immediately wraps these into `Ticker`, `ShareQuantity`, and `PortfolioId` before crossing into the application core. `ShareQuantity.positive(...)` rejects zero or negative values at the boundary.

This controller **drives** the application by calling its use cases. It does not contain business logic.

<a href="diagrams/Rendered/sell-http-to-port.svg"><img src="diagrams/Rendered/sell-http-to-port.png" alt="Sell HTTP to Port" width="100%" /></a>

This controller represents the REST entry point of the SELL use case into the application core. Before tracing how a request flows through the system step by step, the next section presents the architectural map of the components involved in this use case.

---

## 8. Hexagonal Architecture Map for the SELL Use Case

The table below provides a quick-reference map of every component the sell request touches. Each entry corresponds to a class or interface discussed in the execution trace that follows in Section 9.

| Layer | Component | Type | Package/Class |
|-------|-----------|------|---------------|
| **Driving Adapter** | REST Controller | HTTP → Application | `adapter.in.PortfolioRestController` |
| **Primary Port** | Inbound Interface | Use Case Definition | `application.port.in.PortfolioStockOperationsUseCase` |
| **Application Service** | Orchestrator | Use Case Implementation | `application.service.PortfolioStockOperationsService` |
| **Domain Model** | Aggregate Root | Business Logic | `model.Portfolio` |
| **Domain Model** | Entity | Business Logic | `model.Holding` |
| **Domain Model** | Entity | Business Logic | `model.Lot` |
| **Domain Model** | Value Objects | Type Safety & Validation | `model.Money`, `model.Price`, `model.ShareQuantity`, `model.Ticker`, `model.PortfolioId`, `model.HoldingId`, `model.LotId`, `model.SellResult` |
| **Secondary Port** | Outbound Interface | Persistence Contract | `application.port.out.PortfolioPort` |
| **Secondary Port** | Outbound Interface | Price Provider Contract | `application.port.out.StockPriceProviderPort` |
| **Secondary Port** | Outbound Interface | Transaction Storage Contract | `application.port.out.TransactionPort` |
| **Driven Adapters** | JPA Repositories | Application → Database | `adapter.out.jpa.*` |

**Diagram Reference:** See [`diagrams/sell-http-to-port.puml`](diagrams/sell-http-to-port.puml)

---

## 9. Execution Trace of the SELL Use Case (Happy Path)

Now that we have seen both the entry point and the architectural components involved, we can trace how a sell request moves through the system step by step.

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
- Portfolio ID: `"abc-123"` (from path) → wrapped as `PortfolioId.of("abc-123")`
- Ticker: `"AAPL"` (from request body) → wrapped as `Ticker.of("AAPL")`
- Quantity: `5` (from request body) → wrapped as `ShareQuantity.positive(5)`

### Step 2: Controller Calls Inbound Port

```java
SellResult result = portfolioStockOperationsUseCase.sellStock(
    PortfolioId.of(id),
    Ticker.of(request.ticker()),
    ShareQuantity.positive(request.quantity())
);
```

The controller calls the **use case interface**, not a concrete class. This is dependency inversion in action. Notice how all parameters are **Value Objects**, not primitives—the type system prevents accidentally swapping a portfolio ID for a ticker.

### Step 3: Application Service Orchestrates

**File:** `application.service.PortfolioStockOperationsService`

```java
@Override
public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    // 1. Retrieve portfolio from persistence
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

    // 2. Fetch current stock price from external provider
    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
    Price price = stockPrice.price();

    // 3. Delegate to domain model (AGGREGATE ROOT)
    SellResult sellResult = portfolio.sell(ticker, quantity, price);

    // 4. Persist updated portfolio
    portfolioPort.savePortfolio(portfolio);

    // 5. Record transaction for audit
    Transaction transaction = Transaction.createSale(
        portfolioId, ticker, quantity, price,
        sellResult.proceeds(), sellResult.profit()
    );
    transactionPort.save(transaction);

    return sellResult;
}
```

**Notice what the service does:**
- ✅ Retrieves data from adapters (using `PortfolioId`, `Ticker` value objects)
- ✅ Extracts the `Price` from the `StockPrice` returned by the provider
- ✅ Calls the aggregate root with Value Objects (`Ticker`, `ShareQuantity`, `Price`)
- ✅ Coordinates persistence
- ❌ Does **NOT** contain domain rules
- ❌ Does **NOT** manipulate nested entities directly

**Diagram Reference:** See [`diagrams/sell-application-service.puml`](diagrams/sell-application-service.puml)

<a href="diagrams/Rendered/sell-application-service.svg"><img src="diagrams/Rendered/sell-application-service.png" alt="Sell Application Service" width="100%" /></a>

### Step 4: Domain Model Enforces Invariants

**File:** `model.Portfolio`

```java
public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
    if (!quantity.isPositive())
        throw new InvalidQuantityException("Quantity must be positive");

    if (!holdings.containsKey(ticker))
        throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker);

    Holding holding = holdings.get(ticker);
    SellResult result = holding.sell(quantity, price);
    balance = balance.add(result.proceeds());

    return result;
}
```

The Portfolio (aggregate root):
- Validates inputs using Value Object methods (`quantity.isPositive()`)
- Protects the invariant: "You can only sell holdings you own"
- Delegates to the Holding entity
- Updates its own cash balance (`Money`)
- Returns the result (`SellResult`)

> **💡 Value Object validation:** Much of the validation that used to be manual (`if (quantity <= 0)`) is now built into the Value Objects themselves. `ShareQuantity` rejects negative values at construction time. `Price` rejects non-positive values at construction time. The domain methods provide an additional layer of protection for business-level invariants.

**File:** `model.Holding`

```java
public SellResult sell(ShareQuantity quantity, Price sellPrice) {
    if (getTotalShares().isLessThan(quantity)) {
        throw new ConflictQuantityException(
                "Not enough shares to sell. Available: " + getTotalShares().value()
                        + ", Requested: " + quantity.value());
    }

    Money proceeds = sellPrice.multiply(quantity);
    Money costBasis = Money.ZERO;
    ShareQuantity remaining = quantity;

    // FIFO: Sell from oldest lots first
    Iterator<Lot> iterator = lots.iterator();
    while (remaining.isPositive() && iterator.hasNext()) {
        Lot lot = iterator.next();
        ShareQuantity take = lot.getRemainingShares().min(remaining);
        costBasis = costBasis.add(lot.calculateCostBasis(take));
        lot.reduce(take);
        remaining = remaining.subtract(take);
        if (lot.isEmpty()) {
            iterator.remove();
        }
    }

    return SellResult.of(proceeds, costBasis);
}
```

The Holding:
- Protects the invariant: "You cannot sell more shares than you own" (using `ShareQuantity.isGreaterThanOrEqual()`)
- Calculates proceeds upfront using `Price.multiply(ShareQuantity)` → returns `Money`
- Implements FIFO across multiple lots using an `Iterator` for safe in-place removal
- Delegates cost basis calculation to each `Lot` via `lot.calculateCostBasis(take)`
- Removes depleted lots inline via `iterator.remove()` to keep the aggregate lean
- Creates the result via `SellResult.of(proceeds, costBasis)` which auto-calculates profit

**File:** `model.Lot`

```java
public void reduce(ShareQuantity quantity) {
    if (quantity.value() > remainingShares.value()) {
        throw new ConflictQuantityException("Cannot reduce by more than remaining quantity");
    }
    remainingShares = remainingShares.subtract(quantity);
}

public Money calculateCostBasis(ShareQuantity quantity) {
    return unitPrice.multiply(quantity);
}
```

The Lot:
- Protects the invariant: "Remaining shares cannot go negative"
- Updates its remaining quantity using `ShareQuantity.subtract()`
- Calculates cost basis using `Price.multiply(ShareQuantity)` → returns `Money`

**Diagram Reference:** See [`diagrams/sell-domain-fifo.puml`](diagrams/sell-domain-fifo.puml)

<a href="diagrams/Rendered/sell-domain-fifo.svg"><img src="diagrams/Rendered/sell-domain-fifo.png" alt="Sell Domain FIFO" width="100%" /></a>

### Step 5: Persistence Adapter Saves Changes

The `PortfolioPort` implementation (a JPA adapter) converts the domain `Portfolio` into JPA entities and persists them.

**Diagram Reference:** See [`diagrams/sell-persistence-adapter.puml`](diagrams/sell-persistence-adapter.puml)

<a href="diagrams/Rendered/sell-persistence-adapter.svg"><img src="diagrams/Rendered/sell-persistence-adapter.png" alt="Sell Persistence Adapter" width="100%" /></a>

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

> **💡 DTO mapping:** The `SaleResponseDTO` constructor accepts the domain `SellResult` and extracts the raw `BigDecimal` values from `Money` via `.amount()` for JSON serialization. This keeps the boundary clean: Value Objects inside the hexagon, primitives outside.

---

## 10. Orchestration vs. Invariants

This is the **most important concept** in DDD and Hexagonal Architecture.

### A) Roles Explained with Real Code

**Inbound Port (Contract):**

```java
// application.port.in.PortfolioStockOperationsUseCase
public interface PortfolioStockOperationsUseCase {
    SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
}
```

This interface defines **what** the application can do, not **how**. The port speaks the domain's ubiquitous language through Value Objects (`PortfolioId`, `Ticker`, `ShareQuantity`) instead of primitives.

**Application Service (Orchestrator):** The full service code was shown in Section 9, Step 3. The key observation is that the service retrieves, delegates, and persists, but never decides. It does **not** validate quantities, check holdings, implement FIFO, calculate profits, or update lots directly. It is a **coordinator**, not a decision maker.

**Aggregate Root (Decision Maker):** The `Portfolio.sell()` method shown in Section 9, Step 4 validates inputs, delegates to `Holding`, updates the cash balance, and returns a complete `SellResult`. It is the **guardian of invariants** — the single entry point through which all state changes to holdings and lots must pass.

---

### B) Concrete Domain Example: Why Direct Manipulation Breaks Invariants

#### ❌ **Anti-Pattern: Service Manipulating Nested Entities Directly**

Imagine if the application service did this:

```java
// WRONG! DO NOT DO THIS!
@Override
public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId).orElseThrow();

    Holding holding = portfolio.getHoldings().stream()
        .filter(h -> h.getTicker().equals(ticker))
        .findFirst()
        .orElseThrow();

    // Service directly manipulates lots - DANGEROUS!
    ShareQuantity remaining = quantity;
    for (Lot lot : holding.getLots()) {
        if (remaining.isPositive()) {
            ShareQuantity toSell = lot.getRemainingShares().min(remaining);
            lot.reduce(toSell);  // Direct manipulation!
            remaining = remaining.subtract(toSell);
        }
    }

    // Update balance - might be inconsistent!
    Money someAmount = Price.of(150).multiply(quantity);
    portfolio.deposit(someAmount); // WRONG way to add proceeds!

    portfolioPort.savePortfolio(portfolio);
}
```

**What breaks:**

1. **FIFO Logic Duplication:** The FIFO algorithm is now in the service, not in the domain. If business rules change (e.g., switch to LIFO), you must change the service, not the domain model.

2. **Invariant Violation Risk:** What if the service forgets to check `getTotalShares().value() < quantity.value()`? The portfolio would be in an invalid state.

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
- **Validates** inputs using Value Object methods
- **Checks** holdings exist
- **Delegates** to `Holding` (which it controls)
- **Updates** balance consistently with `Money.add()`
- **Returns** a complete `SellResult`

**Benefits:**
- All domain rules are in **one place** (the domain model)
- The service cannot corrupt the portfolio state
- Tests can verify invariants in isolation
- Business logic changes are localized

---

### C) Sequence Diagram: Orchestrator vs Aggregate Root

**Diagram Reference:** See [`diagrams/sell-orchestrator-vs-aggregate.puml`](diagrams/sell-orchestrator-vs-aggregate.puml)

This diagram explicitly shows:
- The **Application Service** calling `Portfolio.sell(Ticker, ShareQuantity, Price)` (aggregate root)
- The **Portfolio** calling `Holding.sell(ShareQuantity, Price)` (controlled entity)
- The **Holding** calling `Lot.reduce(ShareQuantity)` (controlled entity)
- **NO** direct service → Holding communication
- **NO** direct service → Lot communication

---

### D) Design Principle

> **💡 Key Principle**
>
> **Application services coordinate; aggregates decide.**
>
> The application service is a **traffic controller**. It fetches data, calls the aggregate, and saves results. It does not make business decisions.
>
> The aggregate root is a **consistency boundary**. All changes to entities within the aggregate must go through the root. This ensures invariants are never violated.
>
> **Value Objects** reinforce this boundary by making the types expressive. You cannot accidentally pass a `ShareQuantity` where a `Price` is expected—the compiler catches it.

> **📖 Deep Dive: Rich vs Anemic Domain Model**
>
> The separation above — aggregates enforcing invariants while services only orchestrate — is the defining characteristic of a **rich domain model**. In an anemic model, the aggregate becomes a passive data carrier and the business rules migrate into the service layer. For a detailed architectural comparison using HexaStock's own sell flow, see the companion study **[Rich vs Anemic Domain Model](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)**.

> **📐 Design decision — Domain events:**
> HexaStock currently persists the `Transaction` audit record from the application service (`transactionPort.save(...)`) rather than raising a domain event from `Portfolio.sell()`. This is a pragmatic choice: the current design keeps the domain model free of event infrastructure, and the audit record is written within the same `@Transactional` boundary. In a production system that required asynchronous processing (e.g., notifying a tax service, updating a reporting database), domain events dispatched from the aggregate would be the natural evolution — but the architectural benefit of that pattern is not needed here, and introducing it would add complexity without pedagogical payoff.

---

## 11. Transactionality and Consistency

The application service is annotated with `@Transactional`:

```java
@Transactional
public class PortfolioStockOperationsService
    implements PortfolioStockOperationsUseCase {
    // ...
}
```

### Why Transactions Matter for Stock Selling

Selling stocks involves multiple database writes — updating the portfolio balance, reducing lot quantities via FIFO, and recording an audit transaction. These must **all succeed or all fail together**; partial updates would corrupt the portfolio state. Spring's `@Transactional` ensures ACID guarantees:

1. **Atomicity:** All database operations succeed or fail together
2. **Consistency:** If the transaction record fails to save, the portfolio changes are rolled back
3. **Isolation:** Concurrent transactions see a consistent view of the data at their isolation level (typically READ_COMMITTED). Note that `@Transactional` alone does not serialize concurrent access — preventing race conditions on the same portfolio requires additional mechanisms such as pessimistic locking (see below)
4. **Durability:** Once committed, the sale is permanent

**Key separation of concerns:** The domain enforces **business consistency** (invariants, validations via Value Objects), while infrastructure enforces **technical consistency** (ACID properties, transaction boundaries).

### Concurrency Risks in Financial Operations

When concurrent requests target the same portfolio, several problems can arise without proper synchronization:

**Lost Update Problem:**
- Request 1 reads balance = `Money.of(1000)`
- Request 2 reads balance = `Money.of(1000)` (stale)
- Request 1 sells stock, adds proceeds → balance = `Money.of(1500)`, commits
- Request 2 sells stock, adds proceeds → calculates `Money.of(1300)` based on stale read, commits
- **Result:** Final balance is $1300, but should be $1800.

**Double-Spending:**
- Both requests read `ShareQuantity.of(10)` available
- Request 1 sells 10 shares and commits
- Request 2 attempts to sell 10 shares, but only `ShareQuantity.ZERO` remain

**FIFO Corruption:**
- Two concurrent sells attempt to reduce the same lot simultaneously
- Without serialization, lot `ShareQuantity` values could become negative

### How HexaStock Handles Concurrency

HexaStock uses **database-level transaction isolation** combined with explicit locking:

- `@Transactional` establishes the boundary at the application service level
- Database isolation (typically READ_COMMITTED) prevents dirty reads but does not prevent lost updates or non-repeatable reads. For financial operations like stock selling, stronger mechanisms such as pessimistic locking (`SELECT FOR UPDATE`) or optimistic locking with version fields are required to avoid race conditions
- For high-contention scenarios, **pessimistic locking** via `@Lock(LockModeType.PESSIMISTIC_WRITE)` serializes access to specific portfolio rows

The transaction boundary is placed at the **application service** — not the domain model — because transaction management is an infrastructure concern and domain objects should remain technology-agnostic.

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
> See the companion study: **[Concurrency Control with Pessimistic and Optimistic Locking](../CONCURRENCY-PESSIMISTIC-LOCKING.md)**

---

## 12. Persistence Mapping

### Domain Model → JPA Entities

The `Portfolio` domain object is mapped to a `PortfolioEntity` (JPA):
- `Portfolio.id` (`PortfolioId`) → `PortfolioEntity.id` (`String`)
- `Portfolio.balance` (`Money`) → `PortfolioEntity.balance` (`BigDecimal`)
- `Portfolio.holdings` (`Map<Ticker, Holding>`) → `PortfolioEntity.holdings` (one-to-many)

A **mapper** converts between the two, extracting primitive values from Value Objects for persistence and re-wrapping them when loading:

```java
Portfolio domainPortfolio = PortfolioMapper.toModelEntity(portfolioJpaEntity);
PortfolioJpaEntity jpaEntity = PortfolioMapper.toJpaEntity(domainPortfolio);
```

The following excerpt shows the core of the real `PortfolioMapper` implementation:

```java
// PortfolioMapper.java (excerpt, simplified for readability)
public class PortfolioMapper {

    public static Portfolio toModelEntity(PortfolioJpaEntity jpaEntity) {
        Portfolio portfolio = new Portfolio(
                PortfolioId.of(jpaEntity.getId()),
                jpaEntity.getOwnerName(),
                Money.of(jpaEntity.getBalance()),
                jpaEntity.getCreatedAt()
        );
        for (var holdingJpaEntity : jpaEntity.getHoldings()) {
            portfolio.addHolding(HoldingMapper.toModelEntity(holdingJpaEntity));
        }
        return portfolio;
    }

    public static PortfolioJpaEntity toJpaEntity(Portfolio entity) {
        PortfolioJpaEntity portfolioJpaEntity = new PortfolioJpaEntity(
                entity.getId().value(),
                entity.getOwnerName(),
                entity.getBalance().amount(),
                entity.getCreatedAt()
        );
        portfolioJpaEntity.setHoldings(entity.getHoldings().stream()
                .map(HoldingMapper::toJpaEntity)
                .collect(Collectors.toSet()));
        return portfolioJpaEntity;
    }
}
```

Mapping nested entities such as holdings and lots requires careful handling of identity and lazy-loading concerns. See the full `PortfolioMapper.java` (and its companions `HoldingMapper` and `LotMapper`) in `adapters-outbound-persistence-jpa/.../mapper/` for the complete implementation.

### Repositories

- `PortfolioRepository` (JPA) implements `PortfolioPort` (domain interface)
- `TransactionRepository` (JPA) implements `TransactionPort` (domain interface)

The persistence layer deals with primitives (`String`, `BigDecimal`, `int`), while the domain layer uses Value Objects. The mapper handles the translation — this is Dependency Inversion as described in Section 1.

**Diagram Reference:** See [`diagrams/sell-persistence-adapter.puml`](diagrams/sell-persistence-adapter.puml)

<a href="diagrams/Rendered/sell-persistence-adapter.svg"><img src="diagrams/Rendered/sell-persistence-adapter.png" alt="Sell Persistence Adapter" width="100%" /></a>

---

## 13. Error Flows

### Error 1: Portfolio Not Found

**Trigger:** Selling from a non-existent portfolio

**Exception:** `PortfolioNotFoundException` (application exception)

> **📐 Architectural note:** `PortfolioNotFoundException` is defined in the application module (`application.exception`) and extends `RuntimeException`. It is thrown by application services when looking up an aggregate that does not exist — a precondition check that occurs *before* the domain model is invoked. This correctly places the exception in the application layer: missing-aggregate retrieval is an application-level concern, not a domain invariant. Domain exceptions (those extending `DomainException`) are reserved for business-rule violations detected inside the aggregate root.

**Code:**

```java
Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
    .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
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

**Exception Handler:** The `@ControllerAdvice` class (`ExceptionHandlingAdvice`) catches `PortfolioNotFoundException` and converts it to HTTP 404. Although this is an application exception (not a domain exception), it flows through the same `@ControllerAdvice` mechanism that handles domain exceptions.

**Diagram Reference:** See [`diagrams/sell-error-portfolio-not-found.puml`](diagrams/sell-error-portfolio-not-found.puml)

<a href="diagrams/Rendered/sell-error-portfolio-not-found.svg"><img src="diagrams/Rendered/sell-error-portfolio-not-found.png" alt="Sell Error — Portfolio Not Found" width="100%" /></a>

---

### Error 2: Invalid Quantity

**Trigger:** Selling zero or negative shares

**Exception:** `InvalidQuantityException` (domain exception)

**Code:**

```java
// In the controller, ShareQuantity.positive() rejects non-positive values:
ShareQuantity.positive(request.quantity())
// → throws InvalidQuantityException("Quantity must be positive: 0")

// In Portfolio.sell(), an additional guard:
if (!quantity.isPositive())
    throw new InvalidQuantityException("Quantity must be positive");
```

> **💡 Defense in depth:** The `ShareQuantity.positive()` factory method validates at the adapter boundary, and `Portfolio.sell()` validates again inside the domain. This layered approach ensures protection even if a caller bypasses the controller.

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

<a href="diagrams/Rendered/sell-error-invalid-quantity.svg"><img src="diagrams/Rendered/sell-error-invalid-quantity.png" alt="Sell Error — Invalid Quantity" width="100%" /></a>

---

### Error 3: Selling More Than Owned

**Trigger:** Trying to sell 100 shares when you only own 10

**Exception:** `ConflictQuantityException` (domain exception)

**Code:**

```java
// In Holding.sell()
if (!getTotalShares().isGreaterThanOrEqual(quantity)) {
    throw new ConflictQuantityException(
            "Not enough shares to sell. Available: " + getTotalShares().value()
                    + ", Requested: " + quantity.value());
}
```

**HTTP Response:**

```json
HTTP 409 Conflict
{
  "title": "Conflict Quantity",
  "detail": "Not enough shares to sell. Available: 10, Requested: 100",
  "status": 409
}
```

**Diagram Reference:** See [`diagrams/sell-error-sell-more-than-owned.puml`](diagrams/sell-error-sell-more-than-owned.puml)

<a href="diagrams/Rendered/sell-error-sell-more-than-owned.svg"><img src="diagrams/Rendered/sell-error-sell-more-than-owned.png" alt="Sell Error — Selling More Than Owned" width="100%" /></a>

### Integration Test: Verifying the 409 Response

The real integration test in `PortfolioTradingRestIntegrationTest` verifies this error end-to-end through HTTP. The following snippet is taken directly from the repository (portfolio creation and funding are performed in the `@BeforeEach` setup — omitted here for brevity):

```java
// PortfolioTradingRestIntegrationTest.SellingShares (excerpt)
@Test
@SpecificationRef(value = "US-07.AC-3", level = TestLevel.INTEGRATION)
void sellMoreThanOwned_returns409() {
    sell(portfolioId, "AAPL", 10)
            .statusCode(409)
            .body("title", equalTo("Conflict Quantity"))
            .body("detail", containsString("Not enough shares to sell"))
            .body("status", equalTo(409));
}
```

The `sell()` helper method issues a `POST /api/portfolios/{id}/sales` request. The `ConflictQuantityException` thrown by `Holding.sell()` propagates through the application service unchanged; `ExceptionHandlingAdvice` maps it to an RFC 7807 problem body with HTTP 409.

---

### A Note on Infrastructure Failures

The error flows above cover **domain exceptions** — business rule violations that the domain model detects and names. In production, the sell operation can also fail for **infrastructure reasons**: the stock price provider may be unreachable, may return an error (e.g., HTTP 429 rate limit), or may time out. These failures occur at the adapter boundary (e.g., inside `FinnhubStockPriceAdapter.fetchStockPrice()`), before the domain model is even invoked. The application service does not catch these explicitly — they propagate as unchecked exceptions and are translated by the global `@ControllerAdvice` (`ExceptionHandlingAdvice`) into appropriate HTTP responses (typically 502 Bad Gateway or 503 Service Unavailable). A detailed treatment of infrastructure error handling and resilience strategies is outside the scope of this tutorial.

Production systems would typically add resilience patterns such as retries with backoff, circuit breakers (for example with Resilience4j), or cached price fallbacks when the external provider is unavailable. These concerns belong in the adapter layer and are orthogonal to the domain model, so they can be added without changing domain or application code.

---

## 14. Key Takeaways

### What the Sell Use Case Proved About the Architecture

- **One use case exposed every layer.** A single `POST /api/portfolios/{id}/sales` request exercised the REST adapter, inbound port, application service, aggregate root, two entities, five value objects, three outbound ports, JPA persistence, and global error handling — all verifiable through the end-to-end trace.
- **The hexagon kept its promise.** Swapping `MockFinhubStockPriceAdapter` for `FixedPriceStockPriceAdapter` in tests required zero changes to domain or application code. Dependency inversion is not just theory here — it is a testable, demonstrable property.
- **Aggregate root enforcement prevented a real class of bugs.** The anti-pattern in Section 10B showed how direct service manipulation of lots produces FIFO duplication, balance inconsistency, and invariant violation. The aggregate root boundary is the reason that one-line `portfolio.sell(ticker, quantity, price)` call is both correct and complete.
- **FIFO accounting lived entirely in `Holding.sell()`** — not in the service, not in the controller, not in the persistence layer. The companion **[Rich vs Anemic Domain Model study](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)** shows what happens when this logic migrates to the service layer.
- **Value Objects created the ubiquitous language in code.** `Money`, `Price`, `ShareQuantity`, and `Ticker` did not just prevent primitive-obsession bugs — they made the domain's vocabulary compile-time-enforced. The Gherkin scenario says "proceeds = 1800.00" and the test asserts `assertEquals(Money.of("1800.00"), result.proceeds())`; every term is the same in both artifacts.
- **The specification chain held end to end.** Gherkin → `@SpecificationRef` → JUnit → domain code created a traceable chain from business requirement to executable proof. The `costBasis` values in the integration tests are the mathematical proof of FIFO order.
- **Domain exceptions spoke business language across the boundary.** `ConflictQuantityException` originated in `Holding.sell()`, propagated through the application service, and was translated by `ExceptionHandlingAdvice` into HTTP 409 with an RFC 7807 problem body — no framework-specific exception handling in the domain.

---

## 15. Summary: The Complete Sell Flow

```
HTTP Request
    ↓
PortfolioRestController (Driving Adapter)
    ↓ maps primitives to Value Objects:
    │   PortfolioId.of(id), Ticker.of(ticker), ShareQuantity.positive(quantity)
    ↓ calls
PortfolioStockOperationsUseCase (Primary Port / Interface)
    ↓ implemented by
PortfolioStockOperationsService (Application Service)
    ↓ uses
PortfolioPort (Secondary Port) → fetch portfolio by PortfolioId
StockPriceProviderPort (Secondary Port) → fetch StockPrice (contains Price)
    ↓ delegates to
Portfolio.sell(Ticker, ShareQuantity, Price) (Aggregate Root - Domain Logic)
    ↓ delegates to
Holding.sell(ShareQuantity, Price) (Entity - Domain Logic)
    ↓ delegates to
Lot.reduce(ShareQuantity) (Entity - Domain Logic)
    ↓ returns
SellResult (Value Object: Money proceeds, Money costBasis, Money profit)
    ↓ service saves
PortfolioPort.savePortfolio() (Secondary Port)
TransactionPort.save() (Secondary Port)
    ↓ implemented by
JPA Repositories (Driven Adapters)
    ↓ returns
HTTP Response (SaleResponseDTO — primitives extracted from Value Objects)
```

> **💡 Why `SellResult` is a Value Object:** `SellResult` has no identity and no lifecycle — it is an immutable snapshot of the financial outcome of one sell operation. It carries `proceeds`, `costBasis`, and `profit` as `Money` values, and `profit` is derived (proceeds minus costBasis). Two `SellResult` instances with the same values are interchangeable. Making it a Value Object keeps the domain model clean: the aggregate produces a result, the service passes it along, and the controller maps it to a DTO — no persistence, no mutation, no identity tracking.

---

## 16. Integration Testing: Verifying the Sell Use Case End-to-End

The domain-level tests in sections 3–4 verify that the FIFO algorithm and aggregate invariants are correct **in isolation**. But as outlined in the testing strategy (Section 5), a fully working system also requires that all layers collaborate correctly through real HTTP calls and a real database. This is the role of the **REST integration tests**.

### Why Integration Tests Matter

Domain unit tests catch **algorithm bugs** (wrong FIFO order, incorrect cost basis calculation). Integration tests catch **wiring and infrastructure bugs**:

- JSON serialization/deserialization: Does the `SaleResponseDTO` correctly expose `proceeds`, `costBasis`, and `profit`?
- HTTP status codes: Does a sell on a non-existent portfolio return `404`?
- Persistence round-tripping: Are lots correctly saved and reloaded after a partial sell?
- Value Object ↔ primitive mapping: Does `ShareQuantity.of(12)` arrive correctly at the domain layer from the REST endpoint?
- Adapter substitution: Can we **swap the stock price adapter** at test time without changing any domain or application code?

### Test Architecture: One Abstract Base, Three Focused Test Classes

The integration tests follow a **split-by-responsibility** structure that mirrors the hexagonal architecture:

| Test Class | Responsibility | Key Scenarios |
|---|---|---|
| `AbstractPortfolioRestIntegrationTest` | Shared infrastructure: Testcontainers, RestAssured, JSON builders, helper methods | (base class — not executed directly) |
| `PortfolioLifecycleRestIntegrationTest` | Portfolio CRUD, deposits, withdrawals, listing | Create, deposit, withdraw, list all portfolios |
| `PortfolioTradingRestIntegrationTest` | Buy, sell, end-to-end trading, **Gherkin FIFO** | Buy/sell happy paths, error paths, FIFO scenarios |
| `PortfolioErrorHandlingRestIntegrationTest` | 404s on non-existent portfolios | Buy/sell/deposit/withdraw/get on missing portfolio |

**Source:** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/`

### Hexagonal Proof: The FixedPriceStockPriceAdapter

The `MockFinhubStockPriceAdapter` used in non-Gherkin tests returns random prices — useful for general testing but unsuitable for verifying exact FIFO calculations. The `PortfolioTradingRestIntegrationTest` overrides it with a `FixedPriceStockPriceAdapter` that accepts a **queue of deterministic prices**:

```java
// FixedPriceStockPriceAdapter — deterministic, queue-based stock price adapter
//   Each enqueuePrice() provides the price for the next service call.
//   When the queue is empty, it falls back to a default price (150.00).
fixedPriceAdapter.enqueuePrice(Price.of("100.00"));  // next buy → 100.00
fixedPriceAdapter.enqueuePrice(Price.of("120.00"));  // next buy → 120.00
fixedPriceAdapter.enqueuePrice(Price.of("150.00"));  // next sell → 150.00
```

This pattern demonstrates a core hexagonal architecture benefit: **adapters are swappable**. The domain model and application service are completely unaware of which stock price provider is being used. The `@Primary` annotation ensures the fixed-price adapter takes precedence over the random-price mock during the trading tests:

```java
@TestConfiguration
static class FixedPriceConfiguration {
    @Bean
    @Primary
    FixedPriceStockPriceAdapter fixedPriceStockPriceAdapter() {
        return new FixedPriceStockPriceAdapter();
    }
}
```

> **💡 Why this matters:** This is not just a testing trick — it's a **proof that the hexagon works**. In production the stock price adapter could be swapped to a different financial data provider (e.g., Finnhub, Alpha Vantage, Yahoo Finance) without changing a single line of domain or application code.

### Gherkin FIFO Integration Tests

Inside `PortfolioTradingRestIntegrationTest`, the `GherkinFifoScenarios` nested class directly maps the Gherkin scenarios from the Functional Specification to end-to-end HTTP tests:

**Scenario 1 — Selling 8 shares from a single lot:**

```java
@Test
@DisplayName("Selling 8 shares consumed entirely from a single lot (Gherkin Scenario 1)")
void sellSharesConsumedFromSingleLot_FIFOGherkinScenario() {
    fixedPriceAdapter.enqueuePrice(Price.of("150.00"));

    sellPrecise(portfolioId, "AAPL", 8)
            .statusCode(200)
            .body("proceeds",  comparesEqualTo(new BigDecimal("1200.00")))
            .body("costBasis", comparesEqualTo(new BigDecimal("800.00")))
            .body("profit",    comparesEqualTo(new BigDecimal("400.00")));

    getHoldings(portfolioId)
            .body("find { it.ticker == 'AAPL' }.remaining", equalTo(7));
}
```

**Scenario 2 — Selling 12 shares across multiple lots:**

```java
@Test
@DisplayName("Selling 12 shares consumed across multiple lots (Gherkin Scenario 2)")
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

> **💡 How does `costBasis` prove FIFO?** In Scenario 1, `costBasis = 800.00 = 8 × 100.00` — all shares came from Lot #1 (price 100.00). If LIFO were used, cost would be `5×120 + 3×100 = 900.00`. In Scenario 2, `costBasis = 1240.00 = (10 × 100.00) + (2 × 120.00)` — Lot #1 is fully depleted first, then 2 shares from Lot #2. The financial results are the **mathematical proof** of FIFO order.

### Three Verification Levels

The sell use case is now verified at three complementary levels:

| Level | Test Class | What It Catches |
|---|---|---|
| **Domain algorithm** | `HoldingTest` | FIFO lot-consumption logic errors |
| **Aggregate consistency** | `PortfolioTest` | Balance + FIFO invariant violations |
| **Full stack** | `PortfolioTradingRestIntegrationTest` | Wiring, serialization, persistence, adapter integration |

Together, these three levels form a complete verification pipeline from the Gherkin specification down to the HTTP endpoint.

### Requirements Traceability: Linking Tests to Specifications

A natural question arises when a project reaches this level of testing maturity: **how do we know which tests verify which requirements?** In HexaStock, we answer this with a lightweight **requirements traceability** chain:

```
Requirement (API Spec)  →  Gherkin Scenario (.feature)  →  @SpecificationRef  →  Test Method  →  Production Code
```

Each link in this chain serves a distinct purpose:

1. **API Specification** (`stock-portfolio-api-specification.md`) — defines acceptance criteria using IDs like `US-07.AC-1` (User Story 07, Acceptance Criterion 1).
2. **Gherkin Scenarios** (`doc/features/sell-stocks.feature`) — translate acceptance criteria into concrete, readable behaviours with explicit inputs and expected outputs. Scenario IDs like `US-07.FIFO-1` extend the numbering scheme for detailed FIFO-specific scenarios.
3. **`@SpecificationRef` annotation** — a custom Java annotation that tags each test method with the scenario ID it verifies and the testing level (DOMAIN or INTEGRATION).
4. **Test methods** — the executable proof that the production code satisfies the requirement.

This traceability is deliberately **lightweight and non-invasive**: no frameworks, no external tools, no runtime overhead. The annotation is purely informational — a human or tool can scan the codebase to produce a traceability matrix, but the tests themselves are unaffected.

> **Why this matters for AI-assisted development:** When using AI tools to generate or modify tests, the `@SpecificationRef` annotation preserves institutional knowledge. An AI can read the annotation and understand *why* a test exists, not just *what* it asserts. This makes AI-generated changes safer because the tool can verify that every acceptance criterion remains covered. For a detailed treatment of how specifications like these enable high-quality AI-assisted implementation — including a real consulting case where Gherkin, UML, and OpenAPI artifacts drove end-to-end code generation — see **[Specification-Driven Development with AI](../specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md)**.

---

## 17. Exercises

Seven progressive exercises — covering buy-flow tracing, aggregate boundaries, domain exceptions, transactionality, invariant implementation, value object vs entity reasoning, and adding a third stock price provider adapter — are available in the companion document **[Sell-Stock Exercises](SELL-STOCK-EXERCISES.md)**. They are designed for self-directed practice; instructors may assign them selectively.

---

## Reading Map: The HexaStock Documentation Ecosystem

This tutorial is part of a larger documentation ecosystem. The HexaStock repository contains interconnected Markdown texts, each treating a specific architectural theme in depth. They can be read selectively or progressively. The reading map below groups companion documents by theme so the reader can navigate to deeper treatments of topics introduced here.

**Domain-Driven Design**

- [DDD Portfolio and Transactions](../../DDD%20Portfolio%20and%20Transactions.md) — Why Portfolio and Transaction are separate aggregates: aggregate invariants, consistency boundaries, unbounded collection pitfalls, JPA/Hibernate considerations, and a decision matrix grounded in Evans and Vernon.
- [Remove Lots with Zero Remaining Quantity from Portfolio Aggregate](../../Remove%20Lots%20with%20Zero%20Remaining%20Quantity%20from%20Portfolio%20Aggregate.md) — Design decision on whether to retain or prune fully consumed lots from the Portfolio aggregate, with formal analysis based on DDD principles.
- [Rich vs Anemic Domain Model](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) — Rich vs. anemic domain model: a side-by-side architectural comparison using HexaStock's settlement-aware FIFO selling, with failure mode demonstration.

**Hexagonal Architecture and Dependency Inversion**

- [Dependency Inversion in Stock Selling](../DEPENDENCY-INVERSION-STOCK-SELLING.md) — The Dependency Inversion Principle as implemented in the stock-selling service: full execution flow through ports and adapters, with testability and extensibility analysis.

**Concurrency and Persistence**

- [Concurrency Control with Pessimistic and Optimistic Locking](../CONCURRENCY-PESSIMISTIC-LOCKING.md) — Pessimistic and optimistic locking, transaction isolation levels, race condition demonstrations with real tests, and Java 21 virtual thread considerations.

**Scalability and Evolution**

- [Holdings Performance at Scale](../portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) — Four strategies for holdings performance reporting — from in-memory aggregation to CQRS read models — with engineering decision matrix.
- [Watchlists & Market Sentinel](../watchlists/WATCHLISTS-MARKET-SENTINEL.md) — Automated market monitoring and watchlists with CQRS, progressive domain model evolution, and alert fatigue prevention.

**Domain Extensions**

- [DDD Hexagonal Exercise — Lot Selection Strategies](../DDD-Hexagonal-exercise.md) — Extending lot selection strategies beyond FIFO (LIFO, highest-cost, lowest-cost, specific lot) with Strategy pattern and hexagonal structure.

**API and Specification**

- [Stock Portfolio API Specification](../../stock-portfolio-api-specification.md) — Complete REST API specification for all 10 use cases, RFC 7807 error contract, domain model, and exception mapping.
- [Gherkin Feature Files](../../features/) — Fifteen Gherkin feature files defining executable behavioural specifications for the full system.

**Companion Domain Study**

- [Sell Stock — Domain Layer Only](SELL-STOCK-DOMAIN-TUTORIAL.md) — A focused companion covering only the domain model layer of the sell operation, with no HTTP, persistence, or adapter concerns.

**Requirements Traceability**

- [Tutorial README — Traceability Chain](../README.md) — Architecture of the requirement traceability chain: Specification → Gherkin → Tests → Code, with the sell-stocks use case as the reference pilot.

**Specification-Driven Development with AI**

- [Specification-Driven Development with AI](../specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md) — How structurally precise specifications (Gherkin, UML, OpenAPI, ADRs) enable high-quality AI-assisted implementation. Includes a real consulting case, success conditions, failure modes, and practical guidance for teams.

---

## Acknowledgements

The acknowledgements for this tutorial are maintained separately. See [Acknowledgements](../../ACKNOWLEDGEMENTS.md).

---

## References

### Foundational Works

- Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." *alistair.cockburn.us*, 2005. https://alistair.cockburn.us/hexagonal-architecture/
- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Evans, Eric. *Domain-Driven Design Reference: Definitions and Pattern Summaries.* Dog Ear Publishing, 2014.
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013.
- Fowler, Martin. "Ubiquitous Language." *martinfowler.com*, 2006. https://martinfowler.com/bliki/UbiquitousLanguage.html
- Hombergs, Tom. *Get Your Hands Dirty on Clean Architecture.* Packt Publishing, 2019. Reference implementation: [BuckPal](https://github.com/thombergs/buckpal).
- Graça, Herberto. "DDD, Hexagonal, Onion, Clean, CQRS, … How I Put It All Together." *herbertograca.com*, 2017. https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/

### Standards and Specifications

- Nottingham, M. and Wilde, E. "Problem Details for HTTP APIs." RFC 7807, IETF, March 2016. https://www.rfc-editor.org/rfc/rfc7807
- OpenAPI Initiative. *OpenAPI Specification, Version 3.0.* https://spec.openapis.org/oas/v3.0.3

### Project References

- **API Specification:** `doc/stock-portfolio-api-specification.md`
- **Gherkin Specification (canonical):** `doc/features/sell-stocks.feature`
- **Traceability Annotation:** `src/test/java/cat/gencat/agaur/hexastock/specification/SpecificationRef.java`
- **Integration Tests (shared base):** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java`
- **Integration Tests (trading + FIFO):** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioTradingRestIntegrationTest.java`
- **Integration Tests (lifecycle):** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioLifecycleRestIntegrationTest.java`
- **Integration Tests (error handling):** `src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioErrorHandlingRestIntegrationTest.java`
- **Domain Tests:** `src/test/java/cat/gencat/agaur/hexastock/model/PortfolioTest.java`
- **Source Code:** `src/main/java/cat/gencat/agaur/hexastock/`
- **Value Object Tests:** `src/test/java/cat/gencat/agaur/hexastock/model/MoneyTest.java`, `ShareQuantityTest.java`, etc.
- **Architecture Fitness Tests:** `bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java`

---

## Appendix A: Engineering Toolchain

The implementation traced in this tutorial relies on a coordinated set of tools. Each entry below identifies the specific role the tool plays in the sell-stock use case and in the broader engineering practices the tutorial demonstrates.

**Platform and Build**

- **Java 21** (LTS) — the language runtime for the entire system. The domain model, application services, adapters, and all test code target Java 21. The tutorial's code listings use modern language features such as records (`SellResult`, `SaleRequestDTO`), sealed types, and the `getFirst()` collection method.
- **Spring Boot 3.5** — provides the application framework: dependency injection, `@Transactional` boundaries at the application service level, `@ControllerAdvice` for global error mapping, and `@SpringBootTest` for full-context integration tests. The bootstrap module wires all hexagonal layers together at startup.
- **Apache Maven** — orchestrates the multi-module build (`domain`, `application`, `adapters-inbound-rest`, `adapters-outbound-persistence-jpa`, `adapters-outbound-market`, `bootstrap`). Module-level dependency declarations enforce the hexagonal dependency rule at compile time: `domain` has zero framework dependencies; adapters depend on ports, never on each other.

**Testing**

- **JUnit 5** — the test execution platform. Domain tests (`PortfolioTest`, `HoldingTest`) use `@Test`, `@DisplayName`, and `assertEquals` to create executable specifications directly translatable from Gherkin scenarios. Integration tests extend a shared abstract base class that manages lifecycle and assertions across the full HTTP stack.
- **REST Assured 5.4** — drives the integration tests in `PortfolioTradingRestIntegrationTest` and its sibling classes. Fluent assertions such as `.statusCode(200)`, `.body("proceeds", comparesEqualTo(...))` verify JSON responses against expected FIFO financial results, making cost-basis values the mathematical proof of lot-consumption order.
- **Testcontainers** — provisions a disposable MySQL 8 container for each integration test run, ensuring that persistence round-tripping (domain → JPA entity → database → domain) is tested against a real relational engine, not an in-memory substitute.
- **ArchUnit** — scans compiled classes across all modules and enforces six hexagonal dependency rules in `HexagonalArchitectureTest`. It complements Maven's module boundaries by catching transitive dependency violations within the same classpath that module declarations alone cannot detect. https://www.archunit.org/

**Persistence and Data**

- **Spring Data JPA / Hibernate** — implements the outbound `PortfolioPort` and `TransactionPort` adapters. Bidirectional mappers convert between the framework-independent domain model (`Portfolio`, `Holding`, `Lot`) and JPA entities (`PortfolioEntity`, `HoldingEntity`, `LotEntity`), keeping the persistence layer on the adapter side of the hexagonal boundary.
- **MySQL 8** — the production database. Integration tests run against a Testcontainers-managed MySQL instance; local development uses a Docker Compose-provisioned container on port 3307.

**API and Specification**

- **OpenAPI 3.0 / SpringDoc** — the REST API is specified contract-first. SpringDoc (`springdoc-openapi-starter-webmvc-ui`) generates interactive Swagger UI documentation from the controller annotations. The tutorial's API Specification document defines acceptance criteria (e.g., `US-07.AC-1`) that Gherkin scenarios and `@SpecificationRef` annotations trace back to.
- **Gherkin** — `.feature` files under `doc/features/` serve as the canonical functional specification. The sell-stocks scenarios define FIFO behaviour in business language; domain and integration tests are derived directly from them, creating an executable specification chain that the tutorial traces end to end.

**Diagramming**

- **PlantUML** — sequence diagrams (e.g., `sell-application-service.puml`, `sell-domain-fifo.puml`, `sell-error-portfolio-not-found.puml`) trace the sell request through architectural layers. Source files are maintained under `doc/tutorial/sellStocks/diagrams/` and rendered as SVG images referenced throughout the chapter.
- **Mermaid** — the hexagonal architecture overview diagram is maintained as a `.mmd` source file (`hexastock-hexagonal-architecture.mmd`) and rendered with `mmdc`. Mermaid's text-based format makes the diagram versionable alongside the code it describes.

**Quality and Continuous Integration**

- **JaCoCo 0.8** — collects test coverage data across all modules. Coverage reports are generated during `mvn verify` and uploaded as build artefacts in the CI pipeline.
- **SonarQube** — static analysis and quality gate enforcement. The project includes a `sonar-project.properties` configuration that feeds JaCoCo XML reports into Sonar for code quality tracking.
- **GitHub Actions** — the CI pipeline (`.github/workflows/build.yml`) runs on every push and pull request to `main`: checks out the code, provisions JDK 21 (Temurin), executes `mvn clean verify` (which triggers Testcontainers, all test levels, and JaCoCo instrumentation), and uploads test results and coverage reports as build artefacts.
- **Docker / Docker Compose** — provides the local development MySQL instance and is required by Testcontainers in the CI environment. The `docker-compose.yml` at the project root defines the database service.

---
