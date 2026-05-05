# HexaStock: Engineering Architecture That Grows Stronger Through Change

**Sell Stock Deep Dive — A Technical Tutorial on Domain-Driven Design, Hexagonal Architecture, and Specification-Driven Development in a Financial Domain**

> *"Architecture is not documentation. It is an operational capability."*

---

## About This Tutorial

This tutorial traces a single use case — **selling stocks** — through every architectural layer of HexaStock, a stock portfolio management system built with Java 21, Spring Boot 3, Domain-Driven Design (DDD) [Evans, 2003; Vernon, 2013], and the Hexagonal Architecture style (Ports and Adapters) [Cockburn, 2005]. By following one request end to end — from a Gherkin specification [North, 2006] to a REST controller, through application-service orchestration, into the aggregate root's FIFO lot-consumption algorithm, and back as a structured financial result — the reader observes how these engineering disciplines compose in a concrete codebase rather than as abstract principles.

**Intended audience:** Software engineers, architects, and technical leads with working knowledge of Java and Spring Boot who wish to study DDD and Hexagonal Architecture applied to a non-trivial codebase.

**What you will learn**

- One end-to-end use case — selling stocks — traced through every layer, from the REST adapter to the persistence adapter and back.
- How the aggregate root `Portfolio` enforces business invariants while the application service orchestrates without making business decisions, following the consistency-boundary rule for aggregates [Evans, 2003, ch. 6; Vernon, 2013, ch. 10].
- How value objects such as `Money`, `ShareQuantity`, and `Ticker` avoid primitive obsession [Fowler, 2018] and encode the ubiquitous language in the type system.
- How the chain Gherkin → `@SpecificationRef` → JUnit → production code yields requirements traceability that is executable, not merely documentary.
- How the hexagonal boundary supports adapter substitution in practice: replacing the live price provider with `FixedPriceStockPriceAdapter` in tests requires no changes to domain or application code, exercising the dependency inversion principle [Martin, 1996; Martin, 2017].

---

## HexaStock in Brief

HexaStock is a stock portfolio management platform supporting ten use cases — portfolio creation, cash management, stock trading with automatic FIFO lot accounting, holdings performance reporting, and transaction history. It is structured as a Maven multi-module project in which module boundaries encode the dependency rule at build time:

```
HexaStock (parent pom)
├── domain/                              → Pure business model — no framework dependencies
├── application/                         → Use case orchestration, inbound and outbound ports
├── adapters-inbound-rest/               → Driving adapter: REST controllers, DTOs, error mapping
├── adapters-outbound-persistence-jpa/   → Driven adapter: JPA entities, repositories, mappers
├── adapters-outbound-market/            → Driven adapter: external stock-price provider clients
└── bootstrap/                           → Spring Boot entry point, composition root, runtime wiring
```

The domain module declares no infrastructure dependencies. All communication with infrastructure passes through port interfaces declared in the application module, and all dependencies point inward: adapter modules depend on the application module, which in turn depends on the domain module. This arrangement operationalises the dependency rule articulated in Clean Architecture [Martin, 2017, ch. 22] and the isolation principle of the Hexagonal style [Cockburn, 2005]. Because `domain/pom.xml` declares no Spring or persistence dependencies, any attempt to introduce a direct reference from the domain to an adapter would fail at build time unless the dependency were explicitly added — a structural safeguard that Maven enforces before ArchUnit fitness tests run (see Section 5). For module descriptions, domain package layout, architectural identity, and the rationale behind the multi-module structure, see **[HexaStock — Project Overview](HEXASTOCK-PROJECT-OVERVIEW.md)**.

---

## Specification-First Engineering

HexaStock follows an engineering sequence grounded in **Behaviour-Driven Development (BDD)** [North, 2006] and **specification-driven design**:

> **Specification → Contract → Tests → Implementation → Refactor under test**

Observable behaviour is expressed as Gherkin scenarios before design decisions are committed. The REST API is specified contract-first using the OpenAPI Specification [OpenAPI Initiative, 2021]. Tests are linked to specifications through `@SpecificationRef` annotations, producing a traceable chain from acceptance criteria to executable proof. For how this chain also supports AI-assisted development, see **[Specification-Driven Development with AI](../specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md)**.

---

## Ubiquitous Language

The **ubiquitous language** is a rigorous, shared vocabulary deliberately cultivated by domain experts and software practitioners within a bounded context; it must permeate every expression of the model — spoken, written, or encoded in software [Evans, 2003, ch. 2]. Its role is not merely terminological: it is the primary instrument through which domain understanding is made explicit, discussed, refined, and preserved [Fowler, 2006]. In practice, the language surfaces across Gherkin scenarios, domain classes, tests, architectural diagrams, and REST resources, where it must remain strictly consistent. In HexaStock, terms such as *Portfolio*, *Holding*, *Lot*, *proceeds*, *cost basis*, and *FIFO* constitute this vocabulary; each term carries a single meaning wherever it appears, and any terminology drift is treated as a modelling defect.

For the full treatment — including a cross-artifact traceability table, concrete examples drawn from the sell-stock use case, and a discussion of the failure modes that arise in its absence — see the companion document **[Ubiquitous Language in HexaStock](UBIQUITOUS-LANGUAGE.md)**.

---

## 1. Architecture Overview (Hexagonal / Ports and Adapters)

Before tracing the sell-stock flow through code, this section introduces the layers, ports, and adapters that constitute HexaStock's hexagonal architecture [Cockburn, 2005] — the structural vocabulary on which the remainder of the tutorial relies.

### Core Architectural Layers

**Application Core** — the part of the system isolated from external technologies:
- **Domain Layer.** Contains pure business logic (entities, value objects, domain services). This is where rules such as FIFO lot accounting, invariant enforcement, and portfolio consistency are expressed. Examples: `Portfolio`, `Holding`, `Lot`, `Ticker`, `Money`, `Price`, `ShareQuantity`, `PortfolioId`, `HoldingId`, `LotId`.
- **Application Layer.** Orchestrates use cases by coordinating domain objects and ports. Application services are thin coordinators: they retrieve data through outbound ports, delegate decisions to the domain model, and persist the result. They do not implement business rules [Evans, 2003, ch. 4; Vernon, 2013, ch. 14]. Example: `PortfolioStockOperationsService`.

**Ports** — interfaces that define contracts between the core and the outside world [Cockburn, 2005]:
- **Inbound (primary/driving) ports.** Declare what the application can do; implemented by application services. Example: `PortfolioStockOperationsUseCase`.
- **Outbound (secondary/driven) ports.** Declare what the application requires from external systems. The core depends on these abstractions rather than on concrete implementations. Examples: `PortfolioPort`, `StockPriceProviderPort`, `TransactionPort`.

**Adapters** — concrete implementations that connect the core to the surrounding environment:
- **Inbound (driving) adapters.** Translate external requests into invocations of inbound ports. Example: `PortfolioRestController` (HTTP/REST).
- **Outbound (driven) adapters.** Implement outbound ports to interact with databases, external APIs, or other infrastructure. Examples: JPA repository adapters for persistence; Finnhub/AlphaVantage clients for stock prices.

**Dependency direction.** All source-code dependencies point **inward**, toward the domain. Adapters depend on ports; ports are declared by the application core; the domain has no compile-time dependency on infrastructure. This is the **Dependency Inversion Principle** in operational form [Martin, 1996; Martin, 2017, ch. 11].

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

Sections 9–15 trace an HTTP request flowing through these layers, showing how each component discharges its architectural responsibility while preserving separation of concerns.

---

## 2. Purpose and Scope

The sell-stock use case serves as the unifying thread of this tutorial, connecting Gherkin specification, domain modelling, hexagonal structure, persistence mapping, error handling, and integration testing. A single request is followed end to end in order to show how BDD specifications [North, 2006], DDD aggregates [Evans, 2003; Vernon, 2013], hexagonal ports and adapters [Cockburn, 2005], value objects, FIFO accounting, and a layered testing strategy [Cohn, 2009; Meszaros, 2007] compose as applied engineering rather than as isolated principles.

---

## 3. Functional Specification (Behaviour)

Before designing the domain model or writing implementation code, we first specify the **observable behaviour** of the sell use case.

User stories typically capture the intent of a feature at a high level but are often too ambiguous to serve as executable specifications. Behaviour-driven scenarios written in Gherkin describe concrete system behaviour in terms of explicit inputs, actions, and expected outcomes, which allows automated tests to be derived from them with minimal interpretation [North, 2006]. The Gherkin scenarios below therefore serve as the primary functional specification of the sell operation.

They describe what the system must do in business terms, independently of any technical design decisions.

<!--
  Link strategy: this intentionally points to the canonical GitHub markdown source rather than
  a relative path or a GitBook-internal anchor. GitBook's slug-generation rules for headings
  containing punctuation (em dashes, dots, IDs like "US-07") are not guaranteed to match
  GitHub's, so a relative anchor would render unreliably under GitBook. The absolute GitHub
  URL with the GitHub-generated fragment is stable and resolves correctly from both renderers.
-->
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

The Gherkin scenarios above describe observable behaviour at the **Portfolio level** — the aggregate root in the domain model [Evans, 2003, ch. 6]. Because the scenario expresses a Portfolio-level outcome (a balance update that must remain consistent with a sequence of lot consumptions), the primary executable specification is expressed through `Portfolio.sell(...)`; a complementary test on `Holding` verifies the internal FIFO algorithm in isolation. This separation reflects a deliberate DDD decision: external behaviour is exposed by the aggregate root, and tests are placed at the abstraction level that matches the invariant under verification.

HexaStock therefore validates the behaviour at **two complementary levels**:

1. **Aggregate-level behaviour verification** — a `Portfolio` test that exercises the complete sell operation through the aggregate root and asserts financial results, balance update, and FIFO lot consumption as a single consistent unit.
2. **Focused algorithm verification** — a `Holding` test that verifies the internal FIFO lot-consumption algorithm in isolation, independently of portfolio-level concerns such as cash balance.

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

> **📖 Full treatment:** This section provides a focused overview of the four testing levels relevant to the sell use case. For the complete testing strategy covering all seven testing layers — including application service tests, WireMock-based market adapter tests, abstract port contract tests for persistence portability, and architecture fitness tests — see **[Testing Strategy for a Hexagonal Architecture](../TESTING-STRATEGY.md)**.

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
- Enforces the invariant *"a holding cannot sell more shares than it owns"* via `getTotalShares().isLessThan(quantity)`, raising `ConflictQuantityException` when violated.
- Computes proceeds up front using `Price.multiply(ShareQuantity)`, which returns a `Money`.
- Implements FIFO across multiple lots with an `Iterator` to allow safe in-place removal of depleted lots.
- Delegates cost-basis computation to each `Lot` via `lot.calculateCostBasis(take)`.
- Removes depleted lots inline through `iterator.remove()`, keeping the aggregate's internal state minimal.
- Produces the outcome via `SellResult.of(proceeds, costBasis)`, which derives `profit` from its inputs.

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

This section addresses a central design concern in DDD and Hexagonal Architecture: the allocation of responsibility between application services (orchestrators) and aggregates (invariant enforcers) [Evans, 2003, chs. 4 and 6; Vernon, 2013, ch. 14].

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

A single sell operation produces multiple persistent state changes: the portfolio's cash balance is updated, one or more lots are reduced (and possibly removed) under FIFO, and an audit `Transaction` record is written. These changes form a single unit of work in Fowler's sense [Fowler, 2002] and must either commit atomically or be rolled back as a whole; partial persistence would leave the portfolio in an inconsistent state.

HexaStock uses `jakarta.transaction.Transactional` (the Jakarta EE annotation), which Spring recognises and processes through its `TransactionInterceptor` around each annotated method. When a service method is invoked, a JPA-backed transaction is started; if the method returns normally, the transaction commits, and if an unchecked exception escapes, the transaction is rolled back [Spring Framework Reference, § "Declarative transaction management"; Bauer, King & Gregory, 2015]. The ACID properties [Haerder & Reuter, 1983; Gray & Reuter, 1993] are delivered by the underlying database under this demarcation, as follows:

1. **Atomicity.** All writes performed inside the transaction either commit together or are rolled back together. If saving the audit `Transaction` fails, the portfolio's balance and lot updates are rolled back as well.
2. **Consistency.** All declared database integrity constraints (primary keys, foreign keys, non-null constraints, unique indexes) must hold when the transaction commits. Domain invariants — for example, "a lot's remaining shares must never be negative" — are enforced by the aggregate itself before any persistence occurs; the database then protects its own integrity constraints at commit time.
3. **Isolation.** Concurrent transactions observe one another's effects only according to the configured isolation level. HexaStock runs on MySQL 8 with InnoDB, whose default isolation level is `REPEATABLE READ` [MySQL Reference Manual, § "Transaction Isolation Levels"]. `@Transactional` alone does **not** serialise concurrent access to the same aggregate: under `REPEATABLE READ`, the lost-update anomaly can still occur for read-modify-write flows of the kind performed during a sale. Preventing it requires explicit locking — pessimistic or optimistic (see below).
4. **Durability.** Once the transaction commits, its effects survive system failure.

**Separation of concerns.** Business consistency (aggregate invariants, value-object validation) is enforced by the domain model; technical consistency (ACID, transaction boundaries, isolation) is enforced by infrastructure under application-service demarcation. The domain model remains agnostic of the transaction manager.

### Concurrency Risks in Financial Operations

When concurrent requests target the same portfolio, the following anomalies are possible in the absence of adequate concurrency control [Gray & Reuter, 1993; Berenson et al., 1995]:

**Lost update.**
- Request 1 reads `balance = Money.of(1000)`.
- Request 2 reads `balance = Money.of(1000)` (stale).
- Request 1 sells stock, adds proceeds → `Money.of(1500)`, commits.
- Request 2 sells stock and computes its new balance from its stale read → `Money.of(1300)`, commits.
- Observed final balance is 1300, when it should be 1800.

**Double-spending of inventory.**
- Both requests read `ShareQuantity.of(10)` as available.
- Request 1 sells 10 shares and commits.
- Request 2 attempts to sell 10 shares when in fact `ShareQuantity.ZERO` remains.

**Lot inconsistency under FIFO.**
- Two concurrent sales attempt to reduce the same lot without serialisation.
- Intermediate states can violate the invariant that a lot's remaining quantity is non-negative.

### How HexaStock Handles Concurrency

HexaStock relies on the database transaction manager complemented by explicit locking:

- `@Transactional` establishes the unit-of-work boundary at the application-service level.
- MySQL InnoDB's default `REPEATABLE READ` prevents non-repeatable reads of the same row within a transaction but does not prevent lost updates in read-modify-write sequences across transactions. For financial operations such as stock selling, an additional mechanism is therefore required: **pessimistic locking** (`SELECT ... FOR UPDATE`, exposed through JPA as `@Lock(LockModeType.PESSIMISTIC_WRITE)`) or **optimistic locking** (version fields and `OptimisticLockException`-based retry).
- HexaStock serialises access to a specific portfolio row for the duration of the transaction using pessimistic locking on the portfolio lookup performed at the start of the use case. The rationale, trade-offs, and empirical race-condition reproduction are documented in the companion study cited below.

The transaction boundary is placed at the **application service** — not inside the domain model — because transaction demarcation is an infrastructure concern, and domain objects must remain technology-agnostic [Evans, 2003, ch. 4; Vernon, 2013, ch. 14].

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
> See the companion study: **[Concurrency Control: Pessimistic Locking and Optimistic Concurrency](../CONCURRENCY-PESSIMISTIC-LOCKING.md)**

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

### Aggregate Loading and Query Behaviour

The mapper code above reveals an important performance characteristic. When `PortfolioMapper.toModelEntity()` is called:

1. **`jpaEntity.getHoldings()`** triggers a lazy-load SQL query to fetch all holdings for this portfolio.
2. **For each holding**, `HoldingMapper.toModelEntity()` calls `jpaEntity.getLots()`, triggering another lazy-load query per holding.

Without optimisation, loading a portfolio with *H* holdings produces **2 + H SQL queries** — one for the portfolio itself (the `SELECT ... FOR UPDATE` from `findByIdForUpdate`), one for the holdings collection, and one per holding for its lots. This is the classic **N+1 query problem**: the number of queries grows linearly with the number of holdings.

For a portfolio with 3 holdings, that means 5 queries. For 20 holdings, 22 queries. Each additional holding adds a round-trip to the database.

**The fix: Hibernate `@BatchSize`**

HexaStock addresses this with Hibernate's `@BatchSize(size = 30)` annotation on both `@OneToMany` collections:

```java
// PortfolioJpaEntity.java
@OneToMany(cascade = ALL, orphanRemoval = true)
@JoinColumn(name = "portfolio_id")
@BatchSize(size = 30)
private Set<HoldingJpaEntity> holdings = new HashSet<>();

// HoldingJpaEntity.java
@OneToMany(cascade = ALL, orphanRemoval = true)
@JoinColumn(name = "holding_id")
@OrderBy("purchasedAt ASC")
@BatchSize(size = 30)
private List<LotJpaEntity> lots = new ArrayList<>();
```

When Hibernate initialises a lazy collection annotated with `@BatchSize`, it looks for other uninitialised collections of the same role in the persistence context and loads up to the batch size in a single `IN`-clause query [Hibernate ORM User Guide, § "Batch fetching"]. This yields:

- **Without `@BatchSize`:** one query per holding's lots, i.e. *H* queries.
- **With `@BatchSize(size = 30)`:** ⌈*H* / 30⌉ queries for all holdings' lots.

For any portfolio with up to 30 holdings, all lots are loaded in a **single query** instead of *H* separate queries, and the total query count is reduced from **2 + H** to a constant **3** (portfolio + holdings + one batched query for lots).

> **Why not `JOIN FETCH`?** A two-level `JOIN FETCH` (portfolio → holdings → lots) produces a Cartesian product in the SQL result set: every combination of holding × lot yields a row. For a portfolio with 10 holdings averaging 5 lots each, the result set would contain 50 rows rather than 10 + 50, and Hibernate would need to deduplicate in memory. `@BatchSize` avoids the Cartesian product by issuing separate but batched queries, and requires no changes to the JPQL query or the mapper traversal logic [Hibernate ORM User Guide, § "Fetching strategies"].

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

**Trigger:** Attempting to sell 100 shares when only 10 are owned

**Exception:** `ConflictQuantityException` (domain exception)

**Code:**

```java
// In Holding.sell()
if (getTotalShares().isLessThan(quantity)) {
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

### What the Sell Use Case Showed About the Architecture

- **One use case exercised every layer.** A single `POST /api/portfolios/{id}/sales` request traversed the REST adapter, the inbound port, the application service, the aggregate root, two entities, several value objects, three outbound ports, the JPA persistence adapter, and the global error-handling layer, each of which was verifiable through the end-to-end trace.
- **The hexagonal boundary supported adapter substitution.** Replacing `MockFinhubStockPriceAdapter` with `FixedPriceStockPriceAdapter` in tests required no changes to domain or application code. Dependency inversion is expressed here as a structural property of the codebase, verifiable both by the ArchUnit fitness tests and by the substitution itself [Martin, 1996; Martin, 2017].
- **Aggregate-root enforcement eliminated a concrete class of defects.** The anti-pattern in Section 10 B demonstrated how direct manipulation of internal lots from the service layer would duplicate the FIFO algorithm, risk balance inconsistency, and expose invariant violations. Concentrating state transitions behind `portfolio.sell(ticker, quantity, price)` is what makes that single call both complete and correct [Evans, 2003, ch. 6; Vernon, 2013, ch. 10].
- **FIFO accounting remained in `Holding.sell()`** — not in the service, not in the controller, not in the persistence adapter. The companion **[Rich vs Anemic Domain Model study](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)** shows the failure modes that appear when such logic migrates to the service layer [Fowler, 2003].
- **Value objects encoded the ubiquitous language in the type system.** `Money`, `Price`, `ShareQuantity`, and `Ticker` not only prevent a family of primitive-related defects [Fowler, 2018]; they make the domain vocabulary compile-time-checked. The Gherkin scenario states *"proceeds = 1800.00"* and the test asserts `assertEquals(Money.of("1800.00"), result.proceeds())`; the term is identical in both artefacts.
- **The specification chain remained intact end to end.** Gherkin → `@SpecificationRef` → JUnit → domain code forms a traceable chain from acceptance criterion to executable proof. The `costBasis` values asserted in the integration tests constitute the mathematical proof that the FIFO order is preserved.
- **Domain exceptions carried business semantics across the boundary.** `ConflictQuantityException` originates in `Holding.sell()`, propagates through the application service unchanged, and is translated by `ExceptionHandlingAdvice` into HTTP 409 with an RFC 7807 problem body [Nottingham & Wilde, 2016] — without any framework-specific exception handling in the domain.

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

- [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](../CONCURRENCY-PESSIMISTIC-LOCKING.md) — Pessimistic locking (JPA/MySQL) and optimistic concurrency with retries (MongoDB), with race-condition demonstrations backed by real tests.

**Testing**

- [Testing Strategy for a Hexagonal Architecture](../TESTING-STRATEGY.md) — The complete testing strategy across all seven layers — domain, application, REST, market adapters (WireMock), persistence (Testcontainers, abstract port contracts), full-stack integration, and ArchUnit architecture fitness — with deep dives on deterministic market-price testing and persistence portability.

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

I would like to thank Víctor García (https://www.linkedin.com/in/garcia-victor/) for his collaboration on the hexagonal architecture diagram included in this tutorial. His work helped sharpen the clarity and precision of the visual material, strengthening the tutorial as a resource for both academic learning and professional practice.

For broader acknowledgements covering the HexaStock project as a whole, see [Acknowledgements](../../ACKNOWLEDGEMENTS.md).

---

## References

### Foundational Works

- Berenson, H., Bernstein, P. A., Gray, J., Melton, J., O’Neil, E., and O’Neil, P. "A Critique of ANSI SQL Isolation Levels." *Proceedings of the 1995 ACM SIGMOD International Conference on Management of Data*, 1995.
- Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." *alistair.cockburn.us*, 2005. https://alistair.cockburn.us/hexagonal-architecture/
- Cohn, Mike. *Succeeding with Agile: Software Development Using Scrum.* Addison-Wesley, 2009. (Source of the test-pyramid model.)
- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Evans, Eric. *Domain-Driven Design Reference: Definitions and Pattern Summaries.* Dog Ear Publishing, 2014.
- Fowler, Martin. *Patterns of Enterprise Application Architecture.* Addison-Wesley, 2002. (Unit of Work, Repository, Domain Model, Data Mapper.)
- Fowler, Martin. "AnemicDomainModel." *martinfowler.com*, 2003. https://martinfowler.com/bliki/AnemicDomainModel.html
- Fowler, Martin. "Ubiquitous Language." *martinfowler.com*, 2006. https://martinfowler.com/bliki/UbiquitousLanguage.html
- Fowler, Martin. *Refactoring: Improving the Design of Existing Code.* 2nd ed., Addison-Wesley, 2018. (Primitive Obsession code smell.)
- Gray, Jim and Reuter, Andreas. *Transaction Processing: Concepts and Techniques.* Morgan Kaufmann, 1993.
- Haerder, Theo and Reuter, Andreas. "Principles of Transaction-Oriented Database Recovery." *ACM Computing Surveys*, 15(4):287–317, December 1983. (Original statement of the ACID properties.)
- Hombergs, Tom. *Get Your Hands Dirty on Clean Architecture.* Packt Publishing, 2019. Reference implementation: [BuckPal](https://github.com/thombergs/buckpal).
- Graça, Herberto. "DDD, Hexagonal, Onion, Clean, CQRS, … How I Put It All Together." *herbertograca.com*, 2017. https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/
- Martin, Robert C. "The Dependency Inversion Principle." *C++ Report*, 8(6), May 1996.
- Martin, Robert C. *Clean Architecture: A Craftsman's Guide to Software Structure and Design.* Prentice Hall, 2017.
- Meszaros, Gerard. *xUnit Test Patterns: Refactoring Test Code.* Addison-Wesley, 2007.
- North, Dan. "Introducing BDD." *Better Software*, March 2006. https://dannorth.net/introducing-bdd/
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013.

### Framework and Platform References

- Bauer, Christian, King, Gavin, and Gregory, Gary. *Java Persistence with Hibernate.* 2nd ed., Manning, 2015.
- *Hibernate ORM User Guide.* Red Hat. https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html (cited for batch fetching and fetching strategies).
- *MySQL 8.0 Reference Manual.* Oracle. https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html (cited for InnoDB transaction isolation levels).
- *Spring Framework Reference Documentation — Data Access.* Pivotal/VMware. https://docs.spring.io/spring-framework/reference/data-access/transaction.html (cited for declarative transaction management).

### Standards and Specifications

- Nottingham, M. and Wilde, E. "Problem Details for HTTP APIs." RFC 7807, IETF, March 2016. https://www.rfc-editor.org/rfc/rfc7807
- OpenAPI Initiative. *OpenAPI Specification, Version 3.0.3.* 2021. https://spec.openapis.org/oas/v3.0.3

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
