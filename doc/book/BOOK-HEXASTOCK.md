# HexaStock: Engineering Architecture That Grows Stronger Through Change

**A Technical Treatise on Domain-Driven Design and Hexagonal Architecture in a Financial Domain**

---

> *"Architecture is not documentation. It is an operational capability."*

---

## About This Book

This book is a comprehensive technical study of HexaStock, a stock portfolio management system built with Java 21, Spring Boot 3, Domain-Driven Design (DDD), and Hexagonal Architecture. It is not a theoretical survey of design patterns. It is a concrete, code-grounded examination of how these architectural principles work together in a realistic financial domain — and how the resulting system evolves safely under continuous change.

The **stock-selling use case** serves as the primary intellectual and narrative spine. From a single `sell` command, we trace the full lifecycle of a request through every architectural layer: from Gherkin specification to REST controller, through application service orchestration, into the aggregate root's FIFO lot-consumption algorithm, out through persistence adapters, and back as a structured financial result. Every concept in the book — value objects, aggregate boundaries, port interfaces, dependency inversion, concurrency control, error handling, testing strategy, and scalability evolution — connects back to this central operation.

The codebase maintains over 150 automated tests, achieves greater than 90% code coverage as measured by JaCoCo, and holds a Sonar AAA maintainability rating. Every test is linked to Gherkin specifications through `@SpecificationRef` annotations, creating a verifiable traceability chain from business requirements to running code.

### Intended Audience

This book addresses software engineers, architects, and technical leads who have working knowledge of Java and Spring Boot and want to understand how DDD and Hexagonal Architecture function in practice — not as conference abstractions, but as engineering disciplines applied under realistic constraints.

### Conventions

- Code listings are drawn from the actual repository source.
- PlantUML diagrams are referenced by their source path under `doc/tutorial/*/diagrams/`.
- Gherkin scenarios are maintained as canonical `.feature` files under `doc/features/`.
- All financial calculations use `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`.

---

## Acknowledgements

The pedagogical idea behind HexaStock — building a financial portfolio system as a Project-Based Learning experience — was first encountered during software engineering training engagements delivered in collaboration with [Neueda](https://neueda.com/), where the author works as an instructor for major international financial institutions. That experience, working at the intersection of financial domain complexity and engineering education, inspired the later creation and expansion of HexaStock as a dedicated open-source project focused on Hexagonal Architecture and Domain-Driven Design.

A special acknowledgement is owed to the [Agència de Gestió d'Ajuts Universitaris i de Recerca (AGAUR)](https://agaur.gencat.cat/ca/inici), part of the Public Administration of Catalonia (Generalitat de Catalunya), where HexaStock was first formally adopted for institutional training. The original Java package prefix `cat.gencat.agaur.hexastock` reflects this early institutional context, and AGAUR's adoption of the project confirmed the value of teaching architecture through realistic, domain-rich systems.

Particular thanks go to [Francisco José Nebrera](https://www.linkedin.com/in/francisco-jose-nebrera/), whose initiative led to that first organizational implementation of HexaStock within AGAUR.

The project was later open-sourced under the Apache License, Version 2.0, and continues to evolve as a teaching, consulting, and architecture-learning asset for Domain-Driven Design and Hexagonal Architecture.

---

## Table of Contents

- [Acknowledgements](#acknowledgements)
- [Part I — Foundations](#part-i--foundations)
  - [Chapter 1: The HexaStock System](#chapter-1-the-hexastock-system)
  - [Chapter 2: Specification-First Engineering](#chapter-2-specification-first-engineering)
  - [Chapter 3: The Domain Model](#chapter-3-the-domain-model)
- [Part II — The Stock-Selling Spine](#part-ii--the-stock-selling-spine)
  - [Chapter 4: Selling Stocks — The Domain Layer](#chapter-4-selling-stocks--the-domain-layer)
  - [Chapter 5: The FIFO Lot-Consumption Algorithm](#chapter-5-the-fifo-lot-consumption-algorithm)
  - [Chapter 6: Hexagonal Architecture in Action](#chapter-6-hexagonal-architecture-in-action)
  - [Chapter 7: The Full Execution Trace](#chapter-7-the-full-execution-trace)
  - [Chapter 8: Error Flows and Domain Exceptions](#chapter-8-error-flows-and-domain-exceptions)
- [Part III — Architectural Deepening](#part-iii--architectural-deepening)
  - [Chapter 9: Dependency Inversion and Adapter Swapping](#chapter-9-dependency-inversion-and-adapter-swapping)
  - [Chapter 10: Persistence as an Adapter](#chapter-10-persistence-as-an-adapter)
  - [Chapter 11: Rich vs. Anemic Domain Models](#chapter-11-rich-vs-anemic-domain-models)
  - [Chapter 12: Aggregate Design Decisions](#chapter-12-aggregate-design-decisions)
- [Part IV — Cross-Cutting Concerns](#part-iv--cross-cutting-concerns)
  - [Chapter 13: Testing Strategy and Requirements Traceability](#chapter-13-testing-strategy-and-requirements-traceability)
  - [Chapter 14: Concurrency Control](#chapter-14-concurrency-control)
  - [Chapter 15: Settlement-Aware Selling](#chapter-15-settlement-aware-selling)
- [Part V — Evolving the Architecture](#part-v--evolving-the-architecture)
  - [Chapter 16: Holdings Performance at Scale](#chapter-16-holdings-performance-at-scale)
  - [Chapter 17: Watchlists, Market Sentinel, and CQRS](#chapter-17-watchlists-market-sentinel-and-cqrs)
  - [Chapter 18: Extending Lot Selection Strategies](#chapter-18-extending-lot-selection-strategies)
  - [Chapter 19: Architecture in the Age of AI](#chapter-19-architecture-in-the-age-of-ai)
- [Appendices](#appendices)
  - [Appendix A: Complete API Specification](#appendix-a-complete-api-specification)
  - [Appendix B: Diagram Index](#appendix-b-diagram-index)
  - [Appendix C: Gherkin Feature File Index](#appendix-c-gherkin-feature-file-index)

---

# Part I — Foundations

---

## Chapter 1: The HexaStock System

### 1.1 What HexaStock Is

HexaStock is a stock portfolio management platform designed for the Spanish market, where tax regulations require the use of FIFO (First-In, First-Out) accounting when selling stocks. The system enables investors to:

- Create and manage investment portfolios
- Deposit and withdraw funds
- Buy and sell stocks with automatic FIFO lot accounting
- Track holdings performance with real-time market prices
- View transaction history

The platform integrates with external stock price providers (Finnhub, AlphaVantage), persists data through JPA with MySQL, and exposes a RESTful API documented via OpenAPI. It runs on Java 21 and Spring Boot 3.

### 1.2 Architectural Identity

HexaStock is structured according to two complementary architectural disciplines:

**Domain-Driven Design (DDD)** provides the modeling methodology. The system's core concepts — `Portfolio`, `Holding`, `Lot`, `Transaction` — are modeled as aggregates, entities, and value objects that encapsulate business rules and protect invariants. Business logic lives inside the domain, not in services or controllers.

**Hexagonal Architecture (Ports and Adapters)** provides the structural organization. The domain model has no dependencies on frameworks, databases, or HTTP. It communicates with the outside world exclusively through port interfaces, which are implemented by adapters in the infrastructure layer. This structural isolation means that:

- The domain can be tested with pure unit tests — no database, no web server, no Spring context.
- Infrastructure can be replaced without modifying business logic.
- Business rule changes impact only the domain layer.

These are not aspirational statements. They are architectural invariants validated by the test suite.

### 1.3 The Netflix Precedent

Netflix Engineering publicly documented its use of Hexagonal Architecture as a structural strategy for adaptability at scale. As described in their technical blog, isolating core business logic from infrastructure enabled safe technology evolution, reduced ripple effects across services, and provided structural resilience in distributed systems. HexaStock applies the same architectural philosophy in a financial domain context.

### 1.4 Package Structure

The codebase follows a strict package layout that mirrors the hexagonal architecture:

```
cat.gencat.agaur.hexastock
├── model/                          # Domain layer
│   ├── Portfolio.java              # Aggregate root
│   ├── Holding.java                # Entity
│   ├── Lot.java                    # Entity
│   ├── Transaction.java            # Separate aggregate
│   ├── Money.java                  # Value object
│   ├── Price.java                  # Value object
│   ├── ShareQuantity.java          # Value object
│   ├── Ticker.java                 # Value object
│   ├── SellResult.java             # Value object
│   ├── StockPrice.java             # Value object
│   ├── PortfolioId.java            # Identity value object
│   ├── HoldingId.java              # Identity value object
│   ├── LotId.java                  # Identity value object
│   ├── TransactionId.java          # Identity value object
│   ├── service/                    # Domain services
│   │   └── HoldingPerformanceCalculator.java
│   └── exception/                  # Domain exceptions
│       ├── DomainException.java
│       ├── InvalidAmountException.java
│       ├── InvalidQuantityException.java
│       ├── InvalidTickerException.java
│       ├── InsufficientFundsException.java
│       ├── ConflictQuantityException.java
│       ├── HoldingNotFoundException.java
│       ├── PortfolioNotFoundException.java
│       └── EntityExistsException.java
├── application/                    # Application layer
│   ├── port/
│   │   ├── in/                     # Inbound ports (use cases)
│   │   │   ├── PortfolioStockOperationsUseCase.java
│   │   │   ├── PortfolioManagementUseCase.java
│   │   │   ├── GetStockPriceUseCase.java
│   │   │   ├── ReportingUseCase.java
│   │   │   └── TransactionUseCase.java
│   │   └── out/                    # Outbound ports
│   │       ├── PortfolioPort.java
│   │       ├── StockPriceProviderPort.java
│   │       └── TransactionPort.java
│   └── service/                    # Application services
│       ├── PortfolioStockOperationsService.java
│       ├── PortfolioManagementService.java
│       ├── GetStockPriceService.java
│       ├── ReportingService.java
│       └── TransactionService.java
└── adapter/                        # Infrastructure layer
    ├── in/                         # Driving adapters
    │   ├── PortfolioRestController.java
    │   ├── StockRestController.java
    │   ├── ExceptionHandlingAdvice.java
    │   └── webmodel/               # DTOs
    │       ├── CreatePortfolioDTO.java
    │       ├── PortfolioResponseDTO.java
    │       ├── PurchaseDTO.java
    │       ├── SaleRequestDTO.java
    │       ├── SaleResponseDTO.java
    │       ├── HoldingDTO.java
    │       └── ...
    └── out/                        # Driven adapters
        ├── persistence/jpa/
        │   ├── repository/
        │   ├── entity/
        │   ├── mapper/
        │   └── springdatarepository/
        └── rest/
            ├── FinhubStockPriceAdapter.java
            ├── AlphaVantageStockPriceAdapter.java
            └── MockFinhubStockPriceAdapter.java
```

The dependency rule is absolute: arrows point inward. The domain layer depends on nothing. The application layer depends only on the domain. Adapters depend on the application layer through ports. No framework annotation appears inside `model/`. No infrastructure type leaks into a port interface.

### 1.5 Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 (with virtual threads support) |
| Framework | Spring Boot 3.x |
| Persistence | JPA / Hibernate with MySQL |
| Build | Maven |
| Testing | JUnit 5, RestAssured, Testcontainers, Mockito |
| API Documentation | OpenAPI 3.0 |
| Code Quality | SonarQube (AAA rating), JaCoCo (>90% coverage) |
| CI | GitHub Actions |
| Specification | Gherkin `.feature` files |

### 1.6 Filesystem Layout: A Deliberate Trade-Off

The package structure shown in Section 1.4 organizes code primarily by **architectural role** — `model/`, `application/`, `adapter/in/`, `adapter/out/`. This layout makes the hexagonal architecture immediately visible: a developer arriving at the codebase for the first time can identify the domain, the ports, and the adapters by reading directory names alone.

This is a deliberate design choice, not an oversight. It optimizes for **architectural transparency** and **pedagogical legibility** — qualities that are central to HexaStock's mission as a teaching, consulting, and architecture-learning asset.

However, it is important to understand that Hexagonal Architecture does not prescribe a single mandatory filesystem layout. The architectural discipline defines a **logical structure** — domain at the center, ports as contracts, adapters at the periphery — and a **dependency rule** — all arrows point inward. These constraints operate at the level of dependencies and responsibilities, not at the level of directory naming.

#### Two Valid Packaging Strategies

A project can remain fully hexagonal under at least two different packaging approaches:

| Dimension | Organization by Architectural Role | Organization by Feature / Domain Module |
|---|---|---|
| Top-level packages | `model/`, `application/`, `adapter/` | `portfolio/`, `trading/`, `reporting/` |
| Architectural visibility | Immediately obvious from directory names | Requires inspection within each module |
| Business capability grouping | Scattered across layers | Co-located within each feature module |
| Cognitive scalability | Strong for ≤ ~10 domain concepts | Scales to large systems with many bounded contexts |
| Onboarding emphasis | New developers see architecture first | New developers see features first |
| Typical adopters | Teaching, consulting, small-to-medium projects | Enterprise systems, microservice decomposition |

In a feature-based layout, each module (`portfolio/`, `reporting/`, `watchlists/`) would contain its own internal `model/`, `application/`, and `adapter/` sub-packages. The hexagonal principles — dependency inversion, port interfaces, adapter isolation — are preserved within each module. The difference is organizational, not architectural.

#### Why HexaStock Uses Role-Based Organization

HexaStock prioritizes architectural legibility because its primary audiences — engineering students, workshop participants, and consulting clients — benefit most from seeing the hexagonal structure explicitly in the filesystem. When a student reads the project and encounters `model/Portfolio.java`, `application/port/in/PortfolioStockOperationsUseCase.java`, and `adapter/out/persistence/jpa/`, the architectural intent is self-documenting. Every directory name communicates a role in the hexagonal vocabulary.

In a larger-scale production system with dozens of bounded contexts and hundreds of domain objects, this same structure would become unwieldy. A `model/` package containing entities from portfolio management, reporting, watchlists, and settlement processing would lose its clarity. In that scenario, organizing by bounded context or business capability — with each module internally structured as a hexagon — would provide better cognitive scalability.

#### The Key Insight

Logical architecture and filesystem layout are related but not identical. A well-organized filesystem should reflect architectural intent, but the filesystem is a representation of the architecture, not the architecture itself. The dependency rule, the port contracts, and the domain isolation *are* the architecture. The package names are one valid way of expressing them.

An experienced architect reading HexaStock's codebase should understand: this structure was chosen for clarity and teaching value, and it succeeds at that purpose. The same architectural principles can be expressed through alternative packaging strategies when the scale, team structure, or domain complexity warrants it.

---

## Chapter 2: Specification-First Engineering

### 2.1 The Engineering Loop

HexaStock follows a disciplined engineering sequence:

> **Specification → Contract → Tests → Implementation → Refactor Safely**

Behaviour is defined before implementation. Contracts are explicit and versioned. Tests protect architectural integrity. Refactoring remains safe because the test suite validates every business rule and architectural boundary.

### 2.2 Gherkin as Behavioural Specification

Every use case in HexaStock begins as a Gherkin scenario — structured natural-language descriptions that define preconditions, actions, and expected outcomes. These are not documentation artifacts that drift from the code. They are the canonical source of truth for system behaviour, maintained as `.feature` files under `doc/features/`.

For the sell-stocks use case — the central operation of this book — the specification reads:

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

  Scenario: Selling shares consumed across multiple lots
    When I sell 12 shares of AAPL
    Then the sale response contains:
      | Field     | Value   |
      | ticker    | AAPL    |
      | quantity  |      12 |
      | proceeds  | 1800.00 |
      | costBasis | 1240.00 |
      | profit    |  560.00 |
    And FIFO consumed 10 shares from Lot #1 at 100.00
        and 2 shares from Lot #2 at 120.00
    And Lot #1 is fully depleted and removed
```

The scenario is precise enough to derive automated tests with exact numeric assertions.

### 2.3 The @SpecificationRef Traceability Chain

HexaStock connects specifications to tests through a custom `@SpecificationRef` annotation. Every integration and domain test carries a reference to the acceptance criterion it validates:

```java
@Test
@SpecificationRef("US-07.AC-1")
void sellReturnsProceeds_andUpdatesHoldings() { ... }

@Test
@SpecificationRef("US-07.AC-3")
void sellMoreThanOwned_returns409() { ... }
```

This creates a verifiable traceability chain:

```
Gherkin Scenario (doc/features/sell-stocks.feature)
    ↓ referenced by
API Specification (doc/stock-portfolio-api-specification.md, US-07)
    ↓ referenced by
@SpecificationRef("US-07.AC-1") on Java test method
    ↓ validates
Domain + Adapter behaviour
```

Any requirement change propagates through this chain. If a Gherkin scenario changes, the corresponding test must change, and the test failure signals that the implementation needs updating.

### 2.4 OpenAPI Contract

The REST API is defined contract-first using OpenAPI 3.0 (`doc/openapi.yaml`). The contract specifies endpoints, request/response schemas, and error models conforming to RFC 7807 Problem Details. Integration tests validate that the running application conforms to this contract.

The error contract maps domain exceptions to HTTP responses:

| Domain Exception | HTTP Status | Problem Detail Title |
|---|---|---|
| `PortfolioNotFoundException` | 404 | Portfolio Not Found |
| `HoldingNotFoundException` | 404 | Holding Not Found |
| `InsufficientFundsException` | 409 | Insufficient Funds |
| `ConflictQuantityException` | 409 | Conflict Quantity |
| `InvalidAmountException` | 400 | Invalid Amount |
| `InvalidQuantityException` | 400 | Invalid Quantity |
| `InvalidTickerException` | 400 | Invalid Ticker |
| `EntityExistsException` | 409 | Entity Already Exists |

---

## Chapter 3: The Domain Model

### 3.1 Aggregate Design

The domain model follows DDD aggregate design principles. The central aggregate is `Portfolio`, which serves as the aggregate root:

```
Portfolio (Aggregate Root)
├── Holding (Entity)
│   └── Lot (Entity)
└── Transaction (Separate Aggregate)
```

**Portfolio** is the consistency boundary. All state changes to holdings and lots must pass through `Portfolio`'s methods. External callers never directly manipulate a `Holding` or `Lot` — they invoke `Portfolio.buy()`, `Portfolio.sell()`, `Portfolio.deposit()`, or `Portfolio.withdraw()`, and the aggregate root enforces invariants and coordinates the change.

**Transaction** is a separate aggregate, deliberately excluded from the Portfolio aggregate. This decision, documented in the project's architectural decision records, is grounded in invariant analysis: no business rule requires a Transaction to be present within the Portfolio aggregate to maintain consistency. Including transactions inside Portfolio would create an unbounded collection that grows with every operation — a well-known anti-pattern in DDD that leads to performance degradation, dirty-checking overhead in JPA, and eventual memory exhaustion.

The following class diagram shows the complete domain model — aggregate roots, entities, value objects, and their relationships:

[![HexaStock Domain Model — class diagram showing the Portfolio aggregate root, Holding and Lot entities, and all value objects](doc/tutorial/sellStocks/diagrams/Rendered/domain-class-diagram.png)](doc/tutorial/sellStocks/diagrams/Rendered/domain-class-diagram.svg)

*Figure 3.1 — Domain class diagram. The Portfolio aggregate root contains Holdings, which contain Lots. Value objects (Money, Price, ShareQuantity, Ticker, SellResult) enforce structural validity at construction time. ([PlantUML source](doc/tutorial/sellStocks/diagrams/domain-class-diagram.puml))*

### 3.2 The Portfolio Aggregate Root

The `Portfolio` class encapsulates the complete lifecycle of an investment account:

```java
public class Portfolio {
    private PortfolioId id;
    private String ownerName;
    private Money balance;
    private LocalDateTime createdAt;
    private final Map<Ticker, Holding> holdings = new HashMap<>();

    public static Portfolio create(String ownerName) {
        return new Portfolio(
            PortfolioId.generate(), ownerName,
            Money.ZERO, LocalDateTime.now());
    }

    public void deposit(Money money) {
        if (!money.isPositive())
            throw new InvalidAmountException("Deposit amount must be positive");
        this.balance = this.balance.add(money);
    }

    public void withdraw(Money money) {
        if (!money.isPositive())
            throw new InvalidAmountException("Withdrawal amount must be positive");
        if (balance.isLessThan(money))
            throw new InsufficientFundsException(
                "Insufficient funds for withdrawal");
        this.balance = this.balance.subtract(money);
    }

    public void buy(Ticker ticker, ShareQuantity quantity, Price price) {
        if (!quantity.isPositive())
            throw new InvalidQuantityException("Quantity must be positive");
        Money totalCost = price.multiply(quantity);
        if (balance.isLessThan(totalCost))
            throw new InsufficientFundsException(
                "Insufficient funds to buy " + quantity
                + " shares of " + ticker);
        Holding holding = findOrCreateHolding(ticker);
        holding.buy(quantity, price);
        balance = balance.subtract(totalCost);
    }

    public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
        if (!quantity.isPositive())
            throw new InvalidQuantityException("Quantity must be positive");
        if (!holdings.containsKey(ticker))
            throw new HoldingNotFoundException(
                "Holding not found in portfolio: " + ticker);
        Holding holding = holdings.get(ticker);
        SellResult result = holding.sell(quantity, price);
        balance = balance.add(result.proceeds());
        return result;
    }
}
```

Three design principles are visible:

1. **Invariant enforcement**: Every mutating method validates preconditions before modifying state. Invalid operations throw domain exceptions, not generic runtime exceptions.

2. **Encapsulation**: The `holdings` map is private. External code cannot add, remove, or modify holdings directly. The only way to change portfolio state is through the aggregate root's methods.

3. **Atomic consistency**: The `sell` method simultaneously updates the holding (via FIFO lot consumption) and the cash balance (by adding proceeds). These changes happen together within a single method invocation, ensuring the aggregate is never in an inconsistent state.

### 3.3 Value Objects

HexaStock uses value objects extensively to replace primitive types with domain concepts. Each value object enforces its own validity constraints at construction time:

#### Money

```java
public record Money(BigDecimal amount) {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final Money ZERO =
        new Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE));

    public Money {
        Objects.requireNonNull(amount, "'amount' must not be null");
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public Money add(Money augend) {
        return new Money(amount.add(augend.amount));
    }

    public Money subtract(Money subtrahend) {
        return new Money(amount.subtract(subtrahend.amount));
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }
}
```

`Money` normalizes all values to scale 2 with `HALF_UP` rounding at construction time. This guarantees that every `Money` instance in the system uses consistent precision — a critical property for financial calculations. The rounding rule is defined once, as `static final` constants, and applied uniformly.

#### ShareQuantity

```java
public record ShareQuantity(int value) {
    public static final ShareQuantity ZERO = new ShareQuantity(0);

    public ShareQuantity {
        if (value < 0)
            throw new InvalidQuantityException(
                "Share quantity cannot be negative: " + value);
    }

    public static ShareQuantity positive(int value) {
        if (value <= 0)
            throw new InvalidQuantityException(
                "Quantity must be positive");
        return new ShareQuantity(value);
    }

    public ShareQuantity min(ShareQuantity other) {
        return new ShareQuantity(Math.min(this.value, other.value));
    }

    public boolean isPositive() { return value > 0; }
    public boolean isZero() { return value == 0; }
}
```

`ShareQuantity` illustrates a deliberate design decision. The base constructor allows zero because zero is valid domain state — a lot whose shares have been fully sold has `remainingShares = ShareQuantity.ZERO`. But operational commands (buy, sell) require strictly positive quantities. The `positive()` factory method enforces this at the system boundary, while the aggregate root's methods use `isPositive()` to validate before mutating state.

This separation between state quantities (≥ 0) and operation quantities (> 0) avoids the need for separate types while keeping the invariants clear.

#### Ticker

```java
public record Ticker(String value) {
    public Ticker {
        if (value == null || value.isBlank())
            throw new InvalidTickerException("Ticker must not be blank");
        if (!value.matches("^[A-Z]{1,5}$"))
            throw new InvalidTickerException(
                "Invalid ticker format: " + value);
    }
}
```

A `Ticker` is always 1–5 uppercase letters. This constraint is enforced at construction. No code downstream needs to validate ticker format — if a `Ticker` instance exists, it is valid.

#### SellResult

```java
public record SellResult(Money proceeds, Money costBasis, Money profit) {
    public static SellResult of(Money proceeds, Money costBasis) {
        Money profit = proceeds.subtract(costBasis);
        return new SellResult(proceeds, costBasis, profit);
    }

    public boolean isProfitable() { return profit.isPositive(); }
    public boolean isLoss() { return profit.isNegative(); }
}
```

`SellResult` is the value object returned by the FIFO sell algorithm. It captures the three financial outcomes of a sale: how much the investor received (proceeds), what the shares originally cost (cost basis), and the difference (profit or loss). The `of` factory method ensures profit is always calculated consistently as `proceeds − costBasis`.

### 3.4 The Invariant Hierarchy

Invariants are organized by their enforcement point:

| Invariant | Enforced By | Level |
|---|---|---|
| Money amount has scale 2, HALF_UP | `Money` compact constructor | Value object |
| Ticker matches `^[A-Z]{1,5}$` | `Ticker` compact constructor | Value object |
| ShareQuantity ≥ 0 | `ShareQuantity` compact constructor | Value object |
| Deposit/withdrawal amount > 0 | `Portfolio.deposit()` / `withdraw()` | Aggregate root |
| Buy/sell quantity > 0 | `Portfolio.buy()` / `sell()` | Aggregate root |
| Sufficient funds for purchase | `Portfolio.buy()` | Aggregate root |
| Sufficient funds for withdrawal | `Portfolio.withdraw()` | Aggregate root |
| Holding exists for sell operations | `Portfolio.sell()` | Aggregate root |
| Sufficient shares for sell operations | `Holding.sell()` | Entity |

The pattern is consistent: value objects enforce structural validity (format, range), aggregates enforce business rules (sufficient funds, existence), and entities enforce algorithmic preconditions (sufficient shares for FIFO consumption).

---

# Part II — The Stock-Selling Spine

---

## Chapter 4: Selling Stocks — The Domain Layer

### 4.1 What "Selling Stocks" Means

In financial terms, selling stocks involves exchanging owned shares for cash at the current market price. In HexaStock, this operation is governed by FIFO (First-In, First-Out) accounting: when shares are sold, the system consumes them from the oldest lots first.

This is not an arbitrary implementation choice. FIFO is a regulatory requirement for the Spanish market. The business rule is:

> When selling shares, iterate through the holding's lots in chronological order (oldest first) and consume shares from each lot until the requested quantity is fulfilled.

This rule determines the **cost basis** of a sale — how much the sold shares originally cost — which directly determines the investor's realized profit or loss.

### 4.2 Financial Definitions

| Term | Formula | Description |
|---|---|---|
| **Proceeds** | `quantitySold × salePrice` | Total revenue from the sale |
| **Cost Basis** | `Σ (sharesFromLotᵢ × purchasePriceᵢ)` applying FIFO | Original acquisition cost of the sold shares |
| **Profit** | `proceeds − costBasis` | Realized gain (positive) or loss (negative) |

### 4.3 The Domain Entry Point

All sell operations enter the domain through `Portfolio.sell()`:

```java
public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
    if (!quantity.isPositive())
        throw new InvalidQuantityException("Quantity must be positive");
    if (!holdings.containsKey(ticker))
        throw new HoldingNotFoundException(
            "Holding not found in portfolio: " + ticker);

    Holding holding = holdings.get(ticker);
    SellResult result = holding.sell(quantity, price);
    balance = balance.add(result.proceeds());
    return result;
}
```

The aggregate root's responsibilities are precisely scoped:

1. **Validate** — quantity must be positive, holding must exist
2. **Delegate** — the actual FIFO algorithm runs inside `Holding.sell()`
3. **Coordinate** — after the sale, update the cash balance with proceeds
4. **Return** — provide the financial result to the caller

The aggregate root does not contain the FIFO algorithm. It does not know about lots. It delegates to the `Holding` entity, which owns the lot collection and the consumption logic.

---

## Chapter 5: The FIFO Lot-Consumption Algorithm

### 5.1 The Algorithm

The FIFO sell algorithm is the most important piece of business logic in HexaStock. It lives inside `Holding.sell()`:

```java
public SellResult sell(ShareQuantity quantity, Price sellPrice) {
    if (!getTotalShares().isGreaterThanOrEqual(quantity)) {
        throw new ConflictQuantityException(
            "Not enough shares to sell. Available: "
            + getTotalShares().value()
            + ", Requested: " + quantity.value());
    }

    Money proceeds = sellPrice.multiply(quantity);
    Money costBasis = Money.ZERO;
    ShareQuantity remaining = quantity;

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

The following sequence diagram visualizes the FIFO lot-consumption process as it unfolds within the domain layer:

[![FIFO lot-consumption sequence — Portfolio delegates to Holding, which iterates through Lots in chronological order](doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.png)

*Figure 5.1 — FIFO lot-consumption sequence. The Holding iterates through Lots in purchase order, consuming shares from each until the requested quantity is fulfilled. Depleted lots are removed. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml))*

### 5.2 Step-by-Step Trace

Consider the Gherkin scenario: Alice holds AAPL with Lot #1 (10 shares at $100) and Lot #2 (5 shares at $120). She sells 12 shares at $150.

**Step 1 — Validation:**
Total shares = 10 + 5 = 15. Requested = 12. 15 ≥ 12, so proceed.

**Step 2 — Calculate proceeds:**
`proceeds = 12 × $150 = $1,800.00`

**Step 3 — FIFO iteration, Lot #1:**
- `take = min(10, 12) = 10` shares from Lot #1
- `costBasis += 10 × $100.00 = $1,000.00`
- `lot.reduce(10)` → remaining shares = 0
- Lot is empty → `iterator.remove()` — lot is removed
- `remaining = 12 − 10 = 2` shares still to sell

**Step 4 — FIFO iteration, Lot #2:**
- `take = min(5, 2) = 2` shares from Lot #2
- `costBasis += 2 × $120.00 = $240.00`
- `lot.reduce(2)` → remaining shares = 3
- Lot is not empty → stays
- `remaining = 2 − 2 = 0` — request fulfilled

**Step 5 — Build result:**
- `costBasis = $1,000.00 + $240.00 = $1,240.00`
- `profit = $1,800.00 − $1,240.00 = $560.00`
- Return `SellResult(proceeds=$1800.00, costBasis=$1240.00, profit=$560.00)`

**Step 6 — Back in Portfolio.sell():**
- `balance = balance + $1,800.00`
- Lot #1 is gone. Lot #2 has 3 remaining shares at $120.

### 5.3 Lot Lifecycle

Each `Lot` tracks its own state:

```java
public class Lot {
    private LotId id;
    private ShareQuantity initialShares;
    private ShareQuantity remainingShares;
    private Price unitPrice;
    private LocalDateTime purchasedAt;

    public static Lot create(ShareQuantity quantity, Price unitPrice) {
        if (!quantity.isPositive())
            throw new InvalidQuantityException("Quantity must be positive");
        return new Lot(LotId.generate(), quantity, quantity,
                       unitPrice, LocalDateTime.now());
    }

    public void reduce(ShareQuantity quantity) {
        if (quantity.value() > remainingShares.value())
            throw new ConflictQuantityException(
                "Cannot reduce by more than remaining quantity");
        remainingShares = remainingShares.subtract(quantity);
    }

    public Money calculateCostBasis(ShareQuantity quantity) {
        return unitPrice.multiply(quantity);
    }

    public boolean isEmpty() {
        return remainingShares.isZero();
    }
}
```

A lot is created when shares are purchased. Its `initialShares` never changes — it records how many shares were originally bought. Its `remainingShares` decreases as shares are sold via FIFO. When `remainingShares` reaches zero, the lot is empty and is removed from the holding.

### 5.4 Depleted Lot Removal

When a lot reaches zero remaining shares, the FIFO algorithm removes it from the holding's lot list using `iterator.remove()`. This design decision is documented in the project's architectural records with citations from Evans, Vernon, and Nilsson:

> A depleted lot carries no remaining business state. Its `remainingShares` is zero; it cannot participate in future sell operations. Keeping it in the aggregate serves no invariant — no business rule references a depleted lot.

Historical information (when the shares were originally purchased, at what price) is preserved by the `Transaction` aggregate, which records every purchase and sale as an immutable event. The domain aggregate stores current operational state; the transaction log stores history.

### 5.5 Domain Tests for the Algorithm

The FIFO algorithm is tested at two levels:

**Holding-level test** — verifies the algorithm in isolation:

```java
@Test
@DisplayName("Should sell shares across multiple lots using FIFO")
void shouldSellSharesAcrossMultipleLots_GherkinScenario() {
    Holding holding = Holding.create(APPLE);
    holding.buy(ShareQuantity.of(10), Price.of("100.00"));
    holding.buy(ShareQuantity.of(5), Price.of("120.00"));

    SellResult result = holding.sell(
        ShareQuantity.of(12), Price.of("150.00"));

    assertEquals(Money.of("1800.00"), result.proceeds());
    assertEquals(Money.of("1240.00"), result.costBasis());
    assertEquals(Money.of("560.00"), result.profit());
    assertEquals(1, holding.getLots().size());
    assertEquals(ShareQuantity.of(3),
        holding.getLots().getFirst().getRemainingShares());
}
```

**Portfolio-level test** — verifies the algorithm operates correctly within the aggregate root, including balance updates:

```java
@Test
@DisplayName("Should sell via aggregate root (Gherkin scenario)")
void shouldSellSharesUsingFIFOThroughPortfolioAggregateRoot() {
    Portfolio portfolio = new Portfolio(
        PortfolioId.generate(), "Alice",
        Money.of("10000.00"), LocalDateTime.now());

    portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("100.00"));
    portfolio.buy(APPLE, ShareQuantity.of(5), Price.of("120.00"));
    Money balanceBefore = portfolio.getBalance(); // 8400.00

    SellResult result = portfolio.sell(
        APPLE, ShareQuantity.of(12), Price.of("150.00"));

    assertEquals(Money.of("1800.00"), result.proceeds());
    assertEquals(Money.of("1240.00"), result.costBasis());
    assertEquals(Money.of("560.00"), result.profit());
    assertEquals(balanceBefore.add(Money.of("1800.00")),
        portfolio.getBalance());
}
```

Both tests run with no database, no web server, no Spring context. They exercise pure domain logic and execute in milliseconds.

---

## Chapter 6: Hexagonal Architecture in Action

### 6.1 Layers and Dependencies

The hexagonal architecture organizes code into three concentric layers:

**Domain Layer** (`model/`): Contains the aggregate root, entities, value objects, and domain exceptions. Has zero dependencies on Spring, JPA, HTTP, or any framework. Every class in this package is a plain Java object.

**Application Layer** (`application/`): Contains port interfaces and application services. Inbound ports define use cases (`PortfolioStockOperationsUseCase`). Outbound ports define data access contracts (`PortfolioPort`, `StockPriceProviderPort`, `TransactionPort`). Application services implement inbound ports and depend on outbound ports.

**Adapter Layer** (`adapter/`): Contains driving adapters (REST controllers) and driven adapters (JPA repositories, stock price API clients). Adapters implement port interfaces. They depend on the application layer but never on each other.

The following diagram shows how a sell request flows through the hexagonal architecture from HTTP adapter to domain and back:

[![Sell use case flow — the complete path through hexagonal layers](doc/tutorial/sellStocks/diagrams/Rendered/sell-use-case-flow.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-use-case-flow.svg)

*Figure 6.1 — Sell use case flow. The request enters through the driving adapter (REST controller), passes through the inbound port into the application service, delegates to the domain aggregate, and returns through the same layers. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-use-case-flow.puml))*

### 6.2 Inbound Port: The Use Case Interface

The stock-selling use case is defined by an inbound port:

```java
public interface PortfolioStockOperationsUseCase {
    void buyStock(PortfolioId portfolioId, Ticker ticker,
                  ShareQuantity quantity);

    SellResult sellStock(PortfolioId portfolioId, Ticker ticker,
                         ShareQuantity quantity);
}
```

This interface is the contract between the outside world and the application core. The REST controller depends on this interface — not on the implementation. The interface uses only domain types (`PortfolioId`, `Ticker`, `ShareQuantity`, `SellResult`), keeping the contract framework-free.

### 6.3 Application Service: Orchestration Without Business Logic

The application service implements the inbound port and coordinates the use case:

```java
@Transactional
public class PortfolioStockOperationsService
        implements PortfolioStockOperationsUseCase {

    private final PortfolioPort portfolioPort;
    private final TransactionPort transactionPort;
    private final StockPriceProviderPort stockPriceProviderPort;

    @Override
    public SellResult sellStock(PortfolioId portfolioId,
            Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(
                portfolioId.value()));

        StockPrice stockPrice =
            stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        SellResult result = portfolio.sell(ticker, quantity, price);
        portfolioPort.savePortfolio(portfolio);

        Money totalAmount = price.multiply(quantity);
        Transaction transaction = Transaction.createSale(
            portfolioId, ticker, quantity, price,
            totalAmount, result.profit());
        transactionPort.save(transaction);

        return result;
    }
}
```

The service follows a strict pattern:

1. **Load** — retrieve the portfolio from the repository
2. **Fetch** — get the current market price from an external provider
3. **Delegate** — call the domain method (`portfolio.sell()`), which enforces all business rules
4. **Persist** — save the updated portfolio state
5. **Record** — create and save a transaction for auditing
6. **Return** — hand the result back to the caller

The service contains no business logic. It does not check whether the portfolio has enough shares. It does not compute cost basis or profit. It does not decide which lots to consume. All of that is the domain's responsibility. The service orchestrates the sequence of operations and coordinates between ports.

The following diagram contrasts the application service's orchestration role with the aggregate's rule-enforcement role — a distinction central to the hexagonal architecture:

[![Orchestrator vs. Aggregate — service coordinates, aggregate enforces](doc/tutorial/sellStocks/diagrams/Rendered/sell-orchestrator-vs-aggregate.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-orchestrator-vs-aggregate.svg)

*Figure 6.2 — Orchestrator vs. aggregate. The application service orchestrates I/O and sequencing; the aggregate root owns business rules and invariant enforcement. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-orchestrator-vs-aggregate.puml))*

### 6.4 Outbound Ports: Contracts for Infrastructure

The application core defines what it needs from infrastructure through outbound port interfaces:

```java
public interface PortfolioPort {
    Optional<Portfolio> getPortfolioById(PortfolioId id);
    Portfolio savePortfolio(Portfolio portfolio);
    List<Portfolio> getAllPortfolios();
}

public interface StockPriceProviderPort {
    StockPrice fetchStockPrice(Ticker ticker);
    default Map<Ticker, StockPrice> fetchStockPrice(Set<Ticker> tickers) {
        return tickers.stream().collect(
            Collectors.toMap(Function.identity(),
                             this::fetchStockPrice));
    }
}

public interface TransactionPort {
    Transaction save(Transaction transaction);
    List<Transaction> getTransactionsByPortfolioId(PortfolioId portfolioId);
}
```

These interfaces use only domain types. The `PortfolioPort` returns `Portfolio` domain objects, not JPA entities. The `StockPriceProviderPort` returns `StockPrice` domain value objects, not HTTP response DTOs. The adaptation between infrastructure types and domain types happens inside the adapters.

### 6.5 Driving Adapter: The REST Controller

The REST controller is a driving (or primary) adapter that translates HTTP requests into use case invocations:

```java
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioRestController {

    private final PortfolioStockOperationsUseCase stockOperationsUseCase;

    @PostMapping("/{id}/sales")
    public ResponseEntity<SaleResponseDTO> sellStock(
            @PathVariable String id,
            @RequestBody SaleRequestDTO request) {

        PortfolioId portfolioId = new PortfolioId(id);
        Ticker ticker = Ticker.of(request.ticker());
        ShareQuantity quantity =
            ShareQuantity.positive(request.quantity());

        SellResult result =
            stockOperationsUseCase.sellStock(
                portfolioId, ticker, quantity);

        SaleResponseDTO response = new SaleResponseDTO(
            id, request.ticker(), request.quantity(),
            result.proceeds().amount(),
            result.costBasis().amount(),
            result.profit().amount());

        return ResponseEntity.ok(response);
    }
}
```

The controller performs three responsibilities:

1. **Translate inbound** — convert HTTP primitives (path variables, JSON body) into domain types (`PortfolioId`, `Ticker`, `ShareQuantity`)
2. **Invoke use case** — call the inbound port interface, not the service implementation
3. **Translate outbound** — convert the domain result (`SellResult`) into an HTTP response DTO

The controller contains no business logic. It does not validate whether the quantity is sufficient — that is the domain's job. It does validate that the quantity is positive via `ShareQuantity.positive()`, which is a value object construction concern, not a business rule.

The interaction between the REST controller and the inbound port is shown in the following diagram:

[![HTTP to Port — controller translates HTTP primitives to domain types and invokes the use case port](doc/tutorial/sellStocks/diagrams/Rendered/sell-http-to-port.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-http-to-port.svg)

*Figure 6.3 — HTTP to port. The controller converts path variables and JSON into domain value objects, then invokes the inbound port interface. No business logic executes in the adapter layer. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-http-to-port.puml))*

---

## Chapter 7: The Full Execution Trace

### 7.1 From HTTP to Domain and Back

The complete journey of a sell request through the hexagonal architecture:

```
HTTP POST /api/portfolios/{id}/sales
  {"ticker":"AAPL","quantity":12}
                │
                ▼
┌─────────────────────────────────────┐
│  PortfolioRestController            │  ← Driving Adapter
│  • Parses path variable → PortfolioId
│  • Parses JSON body → Ticker, ShareQuantity
│  • Calls stockOperationsUseCase.sellStock()
└─────────────┬───────────────────────┘
              │ (via inbound port interface)
              ▼
┌─────────────────────────────────────┐
│  PortfolioStockOperationsService    │  ← Application Service
│  • portfolioPort.getPortfolioById() │
│  • stockPriceProviderPort.fetchStockPrice()
│  • portfolio.sell(ticker, qty, price) ← delegates to domain
│  • portfolioPort.savePortfolio()    │
│  • transactionPort.save()           │
└─────────────┬───────────────────────┘
              │ (domain method call)
              ▼
┌─────────────────────────────────────┐
│  Portfolio.sell()                    │  ← Aggregate Root
│  • Validates quantity > 0           │
│  • Validates holding exists         │
│  • Delegates to holding.sell()      │
│  • Adds proceeds to balance         │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Holding.sell()                     │  ← Entity
│  • Validates sufficient shares      │
│  • FIFO lot iteration               │
│  • Accumulates cost basis           │
│  • Removes depleted lots            │
│  • Returns SellResult               │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Lot.reduce() / Lot.calculateCostBasis()  ← Entity
│  • Reduces remaining shares         │
│  • Computes cost = unitPrice × qty  │
└─────────────────────────────────────┘
```

### 7.2 The Return Path

After the domain completes the sell operation:

1. `Holding.sell()` returns `SellResult(proceeds, costBasis, profit)` to `Portfolio.sell()`
2. `Portfolio.sell()` adds proceeds to balance, returns `SellResult` to the application service
3. The service saves the updated portfolio via `portfolioPort.savePortfolio()`
4. The service creates and saves a `Transaction` record via `transactionPort.save()`
5. The service returns `SellResult` to the controller
6. The controller maps `SellResult` to `SaleResponseDTO` and returns HTTP 200:

```json
{
  "portfolioId": "550e8400-e29b-41d4-a716-446655440000",
  "ticker": "AAPL",
  "quantity": 12,
  "proceeds": 1800.00,
  "costBasis": 1240.00,
  "profit": 560.00
}
```

### 7.3 Architectural Boundary Crossings

The request crosses exactly four boundaries:

| Crossing | From | To | Mechanism |
|---|---|---|---|
| 1 | HTTP | Controller | Spring `@RequestBody` deserialization |
| 2 | Controller | Application | Inbound port interface invocation |
| 3 | Application | Domain | Direct method call (`portfolio.sell()`) |
| 4 | Application | Infrastructure | Outbound port interface invocation |

At each boundary, types are translated. HTTP strings become domain value objects. Domain aggregates become JPA entities. Domain results become response DTOs. No infrastructure type ever reaches the domain. No domain type ever appears in HTTP responses without translation through a DTO.

---

## Chapter 8: Error Flows and Domain Exceptions

### 8.1 Domain Exception Hierarchy

HexaStock defines a hierarchy of domain exceptions rooted in `DomainException`:

```
DomainException (abstract)
├── PortfolioNotFoundException
├── HoldingNotFoundException
├── InsufficientFundsException
├── ConflictQuantityException
├── InvalidAmountException
├── InvalidQuantityException
├── InvalidTickerException
└── EntityExistsException
```

Each exception carries semantic meaning in the domain language. `HoldingNotFoundException` means the portfolio does not contain the requested ticker. `ConflictQuantityException` means the investor is trying to sell more shares than they own. These are not technical errors — they are expected business outcomes that the system handles gracefully.

### 8.2 Exception-to-HTTP Mapping

A centralized `ExceptionHandlingAdvice` translates domain exceptions into RFC 7807 Problem Details:

```java
@RestControllerAdvice
public class ExceptionHandlingAdvice {

    @ExceptionHandler(PortfolioNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            PortfolioNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Portfolio Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(ConflictQuantityException.class)
    ResponseEntity<ProblemDetail> handleConflict(
            ConflictQuantityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict Quantity");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
    // ... other handlers
}
```

When Alice tries to sell 20 shares of AAPL but only holds 15, the system produces:

```json
{
  "type": "about:blank",
  "title": "Conflict Quantity",
  "status": 409,
  "detail": "Not enough shares to sell. Available: 15, Requested: 20"
}
```

### 8.3 Error Flow Tracing

Consider the sell-more-than-owned scenario:

1. Controller receives `POST /api/portfolios/{id}/sales` with `quantity: 20`
2. Controller creates `ShareQuantity.positive(20)` — valid, passes
3. Controller calls `stockOperationsUseCase.sellStock(...)`
4. Service loads portfolio, fetches price, calls `portfolio.sell(AAPL, 20, price)`
5. `Portfolio.sell()` verifies holding exists — AAPL is held, passes
6. `Portfolio.sell()` delegates to `holding.sell(20, price)`
7. `Holding.sell()` checks `getTotalShares() >= 20` — 15 < 20, fails
8. `Holding.sell()` throws `ConflictQuantityException("Not enough shares...")`
9. Exception propagates through `Portfolio.sell()` → service → controller
10. `ExceptionHandlingAdvice` catches it, produces HTTP 409 with Problem Detail

The domain enforces the invariant. The exception carries the domain's explanation. The advice layer translates it to HTTP. No business logic exists in the error handling path.

The following diagram traces this exact error flow through the hexagonal layers:

[![Error flow — sell more than owned](doc/tutorial/sellStocks/diagrams/Rendered/sell-error-sell-more-than-owned.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-error-sell-more-than-owned.png)

*Figure 8.1 — Error flow: sell more shares than owned. The ConflictQuantityException originates in the domain (Holding.sell), propagates through Portfolio.sell and the application service, and is caught by ExceptionHandlingAdvice, which translates it to an HTTP 409 response with RFC 7807 Problem Details. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-error-sell-more-than-owned.puml))*

---

# Part III — Architectural Deepening

---

## Chapter 9: Dependency Inversion and Adapter Swapping

### 9.1 The Dependency Inversion Principle in HexaStock

The classical dependency rule says high-level modules should not depend on low-level modules — both should depend on abstractions. In hexagonal architecture, this principle is structural: the application core defines port interfaces, and infrastructure adapters implement them.

For stock price retrieval, the dependency graph is:

```
PortfolioStockOperationsService
        │
        │ depends on (interface)
        ▼
StockPriceProviderPort ◄── interface
        ▲
        │ implements
        │
┌───────┴────────────────────┐
│  FinhubStockPriceAdapter   │
│  AlphaVantageAdapter       │
│  FixedPriceStockPriceAdapter (test) │
└────────────────────────────┘
```

The application service never knows which adapter is active. It calls `stockPriceProviderPort.fetchStockPrice(ticker)` and receives a `StockPrice` domain object. Whether that price came from Finnhub's REST API, AlphaVantage's API, or a hardcoded test value is entirely transparent to the application core.

### 9.2 Production Adapters

**FinhubStockPriceAdapter** connects to Finnhub's market data API:

```java
@Profile("finnhub")
@Component
public class FinhubStockPriceAdapter implements StockPriceProviderPort {
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // HTTP call to Finnhub API
        // Parse JSON response
        // Map to StockPrice domain object
    }
}
```

**AlphaVantageStockPriceAdapter** connects to AlphaVantage's API:

```java
@Profile("alphavantage")
@Component
public class AlphaVantageStockPriceAdapter implements StockPriceProviderPort {
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // HTTP call to AlphaVantage API
        // Parse JSON response
        // Map to StockPrice domain object
    }
}
```

Both adapters implement the same port interface. Switching between providers requires only changing the active Spring profile — no domain or application code changes.

### 9.3 Test Adapter

For integration tests, HexaStock uses a `FixedPriceStockPriceAdapter` that returns deterministic prices:

```java
@Profile("test")
@Component
public class FixedPriceStockPriceAdapter implements StockPriceProviderPort {
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        return new StockPrice(ticker, Price.of("150.00"), Instant.now());
    }
}
```

This adapter eliminates external API dependencies from the test suite. Integration tests run with deterministic, reproducible prices. The domain and application layers behave identically whether the price comes from Finnhub, AlphaVantage, or this fixed adapter — demonstrating that the hexagonal architecture's adapter abstraction works in practice, not just in theory.

### 9.4 Why This Matters

Adapter swapping is not a convenience feature. It provides three concrete engineering benefits:

1. **Vendor resilience**: If Finnhub changes their API, deprecates an endpoint, or increases pricing, only the `FinhubStockPriceAdapter` needs to change. The domain and all tests are unaffected.

2. **Deterministic testing**: Integration tests use fixed prices, eliminating flakiness from network latency, API rate limits, and market-hours availability.

3. **Provider independence**: Adding a third provider (Yahoo Finance, IEX Cloud) requires only a new adapter class with the appropriate `@Profile`. Zero changes to existing code.

---

## Chapter 10: Persistence as an Adapter

### 10.1 The Mapper Pattern

In hexagonal architecture, persistence is an adapter — an implementation detail that the domain must not know about. HexaStock achieves this separation through a mapper pattern that translates between domain objects and JPA entities.

The domain model uses domain types:

```
Portfolio (domain) ← PortfolioId, Money, Map<Ticker, Holding>
    └── Holding (domain) ← HoldingId, Ticker, List<Lot>
        └── Lot (domain) ← LotId, ShareQuantity, Price
```

The persistence layer uses JPA entities with database annotations:

```
PortfolioJpaEntity ← @Entity, @Id, @OneToMany
    └── HoldingJpaEntity ← @Entity, @ManyToOne, @OneToMany
        └── LotJpaEntity ← @Entity, @ManyToOne
```

Mappers translate between these representations:

```java
public class PortfolioMapper {
    public static PortfolioJpaEntity toEntity(Portfolio domain) {
        // Map domain aggregate to JPA entity graph
    }

    public static Portfolio toDomain(PortfolioJpaEntity entity) {
        // Reconstitute domain aggregate from JPA entity graph
    }
}
```

### 10.2 Why Not Annotate Domain Objects Directly?

Many Spring Boot applications place JPA annotations (`@Entity`, `@Column`, `@OneToMany`) directly on domain objects. This is pragmatic but creates a structural coupling: the domain model now depends on the JPA specification.

HexaStock deliberately separates them:

| Approach | Domain Purity | Mapping Overhead | Infrastructure Freedom |
|---|---|---|---|
| Annotated domain objects | Compromised — imports `jakarta.persistence` | None | Locked to JPA |
| Separate JPA entities + mappers | Preserved — zero framework imports | Mapper classes required | Can swap to MongoDB, JDBC, etc. |

The mapper pattern means the domain model can be tested without any JPA infrastructure. It also means that switching to a different persistence technology (MongoDB, for example) requires only a new adapter with new document models and mappers — the domain layer remains untouched.

The following diagram illustrates the persistence adapter's internal structure — how domain objects are mapped to JPA entities and how the adapter implements the outbound port:

[![Persistence adapter — domain-to-JPA mapping through the adapter layer](doc/tutorial/sellStocks/diagrams/Rendered/sell-persistence-adapter.png)](doc/tutorial/sellStocks/diagrams/Rendered/sell-persistence-adapter.svg)

*Figure 10.1 — Persistence adapter. The JPA adapter implements the PortfolioPort interface, using mappers to translate between domain aggregates and JPA entity graphs. The domain layer has no knowledge of JPA. ([PlantUML source](doc/tutorial/sellStocks/diagrams/sell-persistence-adapter.puml))*

### 10.3 Infrastructure Replaceability

The DDD-Hexagonal exercise in HexaStock explicitly challenges engineers to add a MongoDB adapter as an optional extension. The requirements are strict:

- The domain layer must not be modified to support MongoDB.
- No MongoDB-specific annotations or types in domain classes.
- All existing domain tests must remain unchanged and pass.
- All MongoDB code lives in the adapter layer.
- The existing repository ports must be reused — the MongoDB adapter implements the same `PortfolioPort` interface as the JPA adapter.

This exercise demonstrates the architectural promise of hexagonal architecture: infrastructure changes impact only infrastructure code.

---

## Chapter 11: Rich vs. Anemic Domain Models

### 11.1 The Distinction

HexaStock exists on two Git branches that demonstrate the practical consequences of domain model design:

- **`rich-domain-model`**: Business logic lives inside domain objects. `Portfolio.sell()` validates quantities, checks holdings, delegates to `Holding.sell()`, and updates the balance. The domain protects its own invariants.

- **`anemic-domain-model`**: Domain objects are data containers with getters and setters. Business logic migrates to application services. The service manually iterates lots, computes cost basis, updates balances, and removes depleted lots.

The architectural difference is immediately visible in the following diagrams. In the rich model, the domain layer contains business logic and enforces invariants; in the anemic model, business logic migrates to the application service layer:

[![Rich domain model architecture — business logic inside the domain](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/rich-architecture.png)](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/rich-architecture.svg)

*Figure 11.1 — Rich domain model architecture. Business rules live inside the aggregate; the application service orchestrates without enforcing invariants. ([PlantUML source](doc/tutorial/richVsAnemicDomainModel/diagrams/rich-architecture.puml))*

[![Anemic domain model architecture — business logic migrated to services](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/anemic-architecture.png)](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/anemic-architecture.svg)

*Figure 11.2 — Anemic domain model architecture. Domain objects become data carriers; business logic scatters into the application service. ([PlantUML source](doc/tutorial/richVsAnemicDomainModel/diagrams/anemic-architecture.puml))*

### 11.2 Invariant Enforcement: Inside vs. Outside

In the rich model, the validation hierarchy is clear:

```java
// Rich model — Portfolio.sell()
public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
    if (!quantity.isPositive())
        throw new InvalidQuantityException("Quantity must be positive");
    if (!holdings.containsKey(ticker))
        throw new HoldingNotFoundException("...");
    Holding holding = holdings.get(ticker);
    SellResult result = holding.sell(quantity, price);
    balance = balance.add(result.proceeds());
    return result;
}
```

In the anemic model, the same logic scatters across the service:

```java
// Anemic model — Service does everything
public SellResult sellStock(PortfolioId id, Ticker ticker,
                            ShareQuantity qty) {
    Portfolio portfolio = portfolioPort.getPortfolioById(id).orElseThrow(...);
    // Service validates
    if (!qty.isPositive()) throw new InvalidQuantityException("...");
    // Service checks holding
    Holding holding = portfolio.getHoldings().stream()
        .filter(h -> h.getTicker().equals(ticker))
        .findFirst()
        .orElseThrow(() -> new HoldingNotFoundException("..."));
    // Service iterates lots manually
    Money costBasis = Money.ZERO;
    ShareQuantity remaining = qty;
    Iterator<Lot> iterator = holding.getLots().iterator();
    while (remaining.isPositive() && iterator.hasNext()) {
        Lot lot = iterator.next();
        ShareQuantity take = lot.getRemainingShares().min(remaining);
        costBasis = costBasis.add(lot.getUnitPrice()
            .multiply(take));
        lot.setRemainingShares(
            lot.getRemainingShares().subtract(take));
        remaining = remaining.subtract(take);
        if (lot.getRemainingShares().isZero())
            iterator.remove();
    }
    // Service updates balance
    Money proceeds = price.multiply(qty);
    portfolio.setBalance(portfolio.getBalance().add(proceeds));
    // ...
}
```

### 11.3 The Test Matrix

The rich-domain branch runs all 170+ tests and they pass. The anemic branch, created by migrating business logic from domain objects to services, results in 10 test failures. These failures demonstrate rule drift — invariants that were previously enforced by the aggregate are now scattered across services, and some enforcement points were lost in the migration.

| Metric | Rich Model | Anemic Model |
|---|---|---|
| Total tests | 170+ | 170+ |
| Passing | 170+ | ~160 |
| Failures | 0 | ~10 |
| Business logic location | Domain objects | Services |
| Invariant enforcement | Guaranteed by aggregate | Scattered, incomplete |

The failures are not bugs in the traditional sense. They are architectural consequences. When the sell logic moves from `Holding.sell()` (which cannot be bypassed) to a service method (which can be circumvented by calling domain setters directly), invariants become suggestions rather than guarantees.

The following diagrams contrast where invariant checks execute in each model and how rules drift over time in the anemic approach:

[![Invariant enforcement — where business rules are checked in each model](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/invariant-enforcement.png)](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/invariant-enforcement.svg)

*Figure 11.3 — Invariant enforcement comparison. In the rich model, invariants are enforced inside the aggregate, making them mandatory for every code path. In the anemic model, enforcement depends on the calling service. ([PlantUML source](doc/tutorial/richVsAnemicDomainModel/diagrams/invariant-enforcement.puml))*

[![Rule drift — how business rules scatter in the anemic model over time](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/rule-drift.png)](doc/tutorial/richVsAnemicDomainModel/diagrams/Rendered/rule-drift.svg)

*Figure 11.4 — Rule drift. As the system grows and new services are added, validation logic that was centralized in the aggregate scatters across multiple service methods. Some paths inevitably miss enforcement points, creating silent invariant violations. ([PlantUML source](doc/tutorial/richVsAnemicDomainModel/diagrams/rule-drift.puml))*

### 11.4 Settlement-Aware Selling as Evidence

The settlement-aware selling extension (Chapter 15) provides the strongest evidence for the rich model. Settlement introduces new invariants: lots within their T+2 settlement window cannot be sold; reserved shares must be tracked separately; fees must be deducted from proceeds.

In the rich model, these new invariants are added to `Holding.sell()` and `Lot`, extending the existing invariant enforcement chain. Every code path that sells shares automatically inherits the new constraints.

In the anemic model, settlement logic must be added to every service method that performs a sale — and to any future service that might also manipulate lots. If a developer creates a new service that accesses lots directly (perhaps for a bulk operation), they must remember to check settlement dates, verify reservations, and compute fees. If they forget, the system silently permits trades on unsettled lots. The domain objects, being passive data containers, offer no protection.

---

## Chapter 12: Aggregate Design Decisions

### 12.1 Why Transaction Is a Separate Aggregate

The most significant aggregate design decision in HexaStock is the separation of `Transaction` from `Portfolio`. The rationale is built on three pillars:

**Invariant analysis.** No business rule in HexaStock requires a Transaction to be present within the Portfolio aggregate to maintain consistency. Sell operations validate share quantities against lots, not against past transactions. Balance updates are computed from proceeds, not by replaying transactions. The Transaction is a record of what happened, not a participant in what is happening.

**Unbounded collection problem.** A portfolio accumulates transactions over its lifetime — deposits, withdrawals, purchases, sales. A highly active portfolio could accumulate tens or hundreds of thousands of transaction records. Including all transactions inside the Portfolio aggregate means loading the entire history every time the portfolio is accessed for any operation:

| Transactions | Estimated Size |
|---|---|
| 1,000 | ~0.5 MB |
| 10,000 | ~5 MB |
| 100,000 | ~50-80 MB |

Loading 50 MB of transaction history to process a single stock purchase is architecturally unsound.

**JPA dirty-checking overhead.** JPA tracks changes to managed entities. With 100,000 Transaction entities in the persistence context, Hibernate's dirty-checking mechanism must inspect every entity on flush — even though no transactions were modified. This creates a performance tax proportional to the collection size, applied on every write operation.

By modeling Transaction as its own aggregate, linked to Portfolio by `PortfolioId`, the system loads transactions only when explicitly needed (e.g., for the transaction history endpoint or holdings performance calculation).

### 12.2 Holding and Lot Placement

`Holding` and `Lot` are entities within the `Portfolio` aggregate, not separate aggregates. This is because business rules require them to be consistent with the portfolio's cash balance:

- When buying, the portfolio must have sufficient funds and the lot must be added to the correct holding atomically.
- When selling, the FIFO lot consumption and the balance update must happen together.

If `Holding` were a separate aggregate, the sell operation would span two aggregates (`Portfolio` for the balance, `Holding` for the lots), requiring distributed coordination. Keeping them within a single aggregate ensures transactional consistency.

The portfolio's holdings collection is bounded by the number of distinct tickers held — typically a few dozen for individual investors. This is a naturally bounded collection, unlike transactions, which grow without limit.

---

# Part IV — Cross-Cutting Concerns

---

## Chapter 13: Testing Strategy and Requirements Traceability

### 13.1 Test Architecture

HexaStock's test suite is organized into two categories:

**Domain tests** — pure unit tests that exercise the domain model with no infrastructure:

- `PortfolioTest` — aggregate root operations (buy, sell, deposit, withdraw), invariant violations
- `HoldingTest` — FIFO algorithm, cross-lot selling, loss scenarios
- `HoldingPerformanceCalculatorTest` — single-pass O(T) aggregation algorithm
- Execute in milliseconds, no database, no web server, deterministic

**Integration tests** — validate adapter wiring and end-to-end flows:

- `PortfolioTradingRestIntegrationTest` — buy/sell via HTTP, response formatting
- `PortfolioLifecycleRestIntegrationTest` — create/get/list portfolios
- `PortfolioErrorHandlingRestIntegrationTest` — error mapping, Problem Details
- `StockPriceRestIntegrationTest` — stock price endpoint
- Use Testcontainers for MySQL, RestAssured for HTTP assertions
- Use `FixedPriceStockPriceAdapter` for deterministic pricing

### 13.2 Testcontainers

Integration tests use Testcontainers to run an actual MySQL database in a Docker container. This avoids the semantic differences between H2 (commonly used for in-memory testing) and production MySQL. Every integration test runs against the same database engine used in production.

The test lifecycle:

1. Testcontainers starts a MySQL container before the test class
2. Spring Boot connects to the containerized database
3. Tests execute HTTP requests via RestAssured
4. Each test method runs in a transactional context that rolls back after assertion
5. The container is stopped after the test class completes

### 13.3 The @SpecificationRef Pattern

Every test method carries a `@SpecificationRef` annotation that links it to a specific acceptance criterion:

```java
@Nested
@DisplayName("Selling Shares")
class SellingShares {

    @Test
    @SpecificationRef("US-07.AC-1")
    @DisplayName("sell returns proceeds and updates holdings")
    void sellReturnsProceeds_andUpdatesHoldings() {
        // Arrange: deposit funds, buy shares
        // Act: sell shares
        // Assert: response contains proceeds, costBasis, profit
        // Assert: holding has correct remaining shares
    }

    @Test
    @SpecificationRef("US-07.AC-3")
    @DisplayName("sell more than owned returns 409")
    void sellMoreThanOwned_returns409() {
        // Arrange: portfolio holds 5 shares
        // Act: try to sell 10 shares
        // Assert: HTTP 409 with "Conflict Quantity" title
    }
}
```

This pattern provides bidirectional traceability:

- From specification to code: given a Gherkin scenario (US-07.AC-3), find the test that validates it
- From code to specification: given a test, understand what business requirement it protects

### 13.4 Quality Metrics

The test suite as a whole achieves:

- **150+ automated tests** covering all happy paths, error scenarios, domain invariant violations, edge cases, and adapter boundaries
- **>90% code coverage** as measured by JaCoCo
- **Sonar AAA maintainability rating** — indicating clean, maintainable code with no critical issues
- **Gherkin-linked tests** — every test traces back to a specification scenario

These are not aspirational targets. They are current measurements from the CI pipeline.

---

## Chapter 14: Concurrency Control

### 14.1 The Problem

In a concurrent environment, two users (or two requests from the same user) might attempt to sell shares from the same portfolio simultaneously. Without protection, both requests could read the same portfolio state, both conclude there are sufficient shares, and both execute the sale — resulting in selling more shares than actually exist.

### 14.2 Pessimistic Locking

HexaStock's primary concurrency strategy is pessimistic locking — acquiring a database lock when the portfolio is loaded, preventing other transactions from modifying the same row until the current transaction completes.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdWithLock(@Param("id") String id);
```

This translates to `SELECT ... FOR UPDATE` in SQL. When a transaction executes this query, the database locks the portfolio row. Any concurrent transaction attempting to load the same portfolio will block until the first transaction commits or rolls back.

**Advantages:**
- Simple to implement — one annotation on the repository query
- Deterministic — no optimistic retry logic needed
- Strong consistency — no possibility of concurrent modification

**Disadvantages:**
- Serializes access to the same portfolio — limits throughput for highly active portfolios
- Deadlock potential — if transactions acquire locks in different orders
- Blocking — threads wait instead of failing fast

### 14.3 Optimistic Locking

As an alternative, HexaStock demonstrates optimistic locking using JPA's `@Version` annotation:

```java
@Entity
public class PortfolioJpaEntity {
    @Id
    private String id;

    @Version
    private Long version;
    // ...
}
```

With optimistic locking, no database lock is acquired during read. Instead, when the entity is saved, JPA includes a version check in the UPDATE statement:

```sql
UPDATE portfolio SET balance = ?, version = version + 1
WHERE id = ? AND version = ?
```

If the version has changed (because another transaction modified the portfolio concurrently), the UPDATE affects zero rows, and JPA throws `OptimisticLockException`. The application can catch this and retry the operation.

### 14.4 Virtual Threads Consideration

Java 21 introduces virtual threads — lightweight threads that can block without consuming OS threads. With pessimistic locking, a thread blocked on `SELECT ... FOR UPDATE` occupies an OS thread in the traditional threading model. With virtual threads, the same blocking operation simply parks the virtual thread, freeing the underlying carrier thread for other work.

This makes pessimistic locking more practical in high-concurrency environments with Java 21. The teaching branch in HexaStock includes instrumented examples that demonstrate the difference.

---

## Chapter 15: Settlement-Aware Selling

### 15.1 T+2 Settlement

In financial markets, stock purchases do not settle immediately. The standard settlement cycle is T+2: the transaction settles two business days after the trade date. During this settlement window, the purchased shares are not yet fully owned by the investor.

HexaStock's rich-vs-anemic comparison extends the sell operation to account for settlement:

- Lots within their T+2 settlement window are **not available for sale**
- A `reservedShares` field tracks shares committed to pending sell operations
- Settlement fees are deducted from sale proceeds

### 15.2 The Accounting Identity

Settlement-aware selling introduces a fundamental accounting identity that the domain must enforce:

```
availableShares = remainingShares − reservedShares
```

This identity constrains every sell operation: the system must consider not just how many shares exist (`remainingShares`) but how many are actually available for sale after accounting for reservations and unsettled purchases.

### 15.3 Gherkin Specification for Settlement

The settlement extension has its own Gherkin specifications:

```gherkin
Feature: Settlement-Aware Stock Selling

  Scenario: Selling only settled lots
    Given Alice owns 10 shares of AAPL purchased 5 days ago (settled)
    And Alice owns 5 shares of AAPL purchased today (unsettled, T+2)
    When Alice sells 8 shares of AAPL
    Then 8 shares are sold from the settled lot
    And the unsettled lot remains untouched

  Scenario: Rejecting sale of unsettled shares
    Given Alice owns 5 shares of AAPL purchased today (unsettled)
    And no other AAPL lots are settled
    When Alice tries to sell 3 shares of AAPL
    Then the system rejects the sale with "Not enough settled shares"
```

### 15.4 Fee Accounting

Settlement-aware selling includes fee handling. The fee-adjusted accounting identity:

```
proceeds    = quantity × salePrice
fees        = computed from sale parameters
netProceeds = proceeds − fees
profit      = netProceeds − costBasis
```

The fee calculation is encapsulated within the domain, ensuring that all profit/loss calculations consistently account for transaction costs.

### 15.5 Rich Model Advantage

The settlement extension provides the strongest demonstration of the rich model's advantage. In the rich model:

- Settlement checks are integrated into `Holding.sell()` — every code path that sells shares automatically respects settlement constraints
- The accounting identity (`availableShares = remainingShares − reservedShares`) is enforced as an invariant
- Fee deductions are part of the `SellResult` calculation
- No external code can bypass these checks

In the anemic model, settlement logic must be added to every service that accesses lots. If a new service or endpoint is created that sells shares but forgets to check settlement dates, the system silently allows trading on unsettled lots — a regulatory violation.

---

# Part V — Evolving the Architecture

---

## Chapter 16: Holdings Performance at Scale

### 16.1 The Reporting Use Case

HexaStock provides a holdings performance endpoint that, for each stock in a portfolio, computes:

- Total shares purchased (all buy transactions)
- Remaining shares (after sells)
- Average purchase price (weighted)
- Current market price (from the stock price provider)
- Unrealized gain (paper profit/loss on shares still held)
- Realized gain (actual profit/loss from completed sales)

This use case is implemented by `ReportingService`, which orchestrates data from three sources: the portfolio (for current holdings), the transaction log (for historical data), and the stock price provider (for live prices).

### 16.2 Approach A — Single-Pass In-Memory Aggregation

The current implementation uses a `HoldingPerformanceCalculator` domain service that iterates the transaction list exactly once:

```java
static final class TickerAccumulator {
    BigDecimal totalBoughtQty  = BigDecimal.ZERO;
    BigDecimal totalBoughtCost = BigDecimal.ZERO;
    BigDecimal realizedGain    = BigDecimal.ZERO;
}
```

For each transaction, a switch expression dispatches on the type:

```java
switch (tx.getType()) {
    case PURCHASE -> {
        var qty = BigDecimal.valueOf(tx.getQuantity().value());
        acc.totalBoughtQty  = acc.totalBoughtQty.add(qty);
        acc.totalBoughtCost = acc.totalBoughtCost.add(
            tx.getUnitPrice().value().multiply(qty));
    }
    case SALE -> acc.realizedGain = acc.realizedGain.add(
        tx.getProfit().amount());
}
```

| Metric | Value |
|---|---|
| Time complexity | O(T) — single scan of T transactions |
| Memory | O(H) — one accumulator per distinct ticker |
| Passes over data | 1 |
| Domain purity | Full — no I/O, pure computation |
| Testability | Unit testable — no database needed |

This approach is appropriate for portfolios with up to ~50,000 transactions. The domain service has no I/O. It receives all data as method arguments and returns an immutable result. Testing is trivial — the test suite exercises it with in-memory portfolios and transaction lists.

The following sequence diagram shows how Approach A orchestrates the single-pass aggregation entirely in the application layer, with the domain service performing pure computation:

[![Approach A — single-pass in-memory aggregation](doc/tutorial/portfolioReporting/diagrams/Rendered/approachA-sequence.png)](doc/tutorial/portfolioReporting/diagrams/Rendered/approachA-sequence.svg)

*Figure 16.1 — Approach A: single-pass in-memory aggregation. The ReportingService loads transactions via the port, delegates to HoldingPerformanceCalculator for O(T) aggregation, fetches current prices, and returns HoldingPerformanceDTOs. ([PlantUML source](doc/tutorial/portfolioReporting/diagrams/approachA-sequence.puml))*

### 16.3 Scalability Analysis

The working set per request scales linearly with transaction count:

| Concurrent Requests | Transactions/Portfolio | Aggregate Heap |
|---|---|---|
| 1 | 20,000 | ~9 MB |
| 10 | 20,000 | ~90 MB |
| 50 | 20,000 | ~450 MB |

The critical insight: the compute time is not the bottleneck. Database query time and price API latency dominate. Optimizing the aggregation algorithm yields diminishing returns — the next improvement must reduce I/O.

### 16.4 Approach B — DB-Powered Query Side

Push aggregation into the database using `GROUP BY`:

```sql
SELECT
    ticker,
    SUM(CASE WHEN type = 'PURCHASE' THEN quantity ELSE 0 END)
        AS total_bought_qty,
    SUM(CASE WHEN type = 'PURCHASE' THEN quantity * unit_price ELSE 0 END)
        AS total_bought_cost,
    SUM(CASE WHEN type = 'SALE' THEN profit ELSE 0 END)
        AS realized_gain
FROM transactions
WHERE portfolio_id = :portfolioId AND type IN ('PURCHASE', 'SALE')
GROUP BY ticker
```

The application receives H rows (one per ticker) instead of T full entities. No `Transaction` entity is ever materialized.

In hexagonal architecture terms, this requires a new outbound port:

```java
public interface HoldingsPerformanceQueryPort {
    List<TickerAggregateDTO> getAggregatedHoldings(PortfolioId id);
}
```

The tradeoff: domain purity is compromised — aggregation logic now lives in SQL. Rounding behaviour depends on the database engine. Testing requires integration tests with a real database.

### 16.5 Approach C — Hybrid

The recommended production path when T exceeds Approach A's comfortable range. The database performs heavy lifting (summing quantities and costs), but the domain retains ownership of business-sensitive calculations (rounding, average price, unrealized gain).

```java
public interface TransactionQueryPort {
    List<RawTickerAggregate> getRawAggregates(PortfolioId id);
}
```

The boundary is explicit: **addition and multiplication are safe in SQL; division and rounding must stay in Java.** SQL `ROUND()` behaviour is vendor-specific. Java's `SCALE = 2` and `RoundingMode.HALF_UP` are defined once as constants and applied uniformly.

### 16.6 Approach D — Snapshot / CQRS Read Model

For unlimited read scalability, pre-compute and store aggregated holdings data:

```sql
CREATE TABLE holdings_snapshot (
    portfolio_id      VARCHAR(36)    NOT NULL,
    ticker            VARCHAR(5)     NOT NULL,
    total_bought_qty  DECIMAL(18,2)  NOT NULL,
    total_bought_cost DECIMAL(18,2)  NOT NULL,
    remaining_shares  INT            NOT NULL,
    realized_gain     DECIMAL(18,2)  NOT NULL,
    last_updated_at   TIMESTAMP      NOT NULL,
    PRIMARY KEY (portfolio_id, ticker)
);
```

Reading becomes O(H) with no aggregation. The snapshot can be updated synchronously (within the buy/sell transaction) or asynchronously (via domain events). Asynchronous updates introduce eventual consistency — acceptable for dashboards, unacceptable for transactional operations.

The snapshot approach introduces a distinct read model alongside the write model — the structural foundation of full CQRS:

[![Snapshot architecture — pre-computed read model alongside the transactional write model](doc/tutorial/portfolioReporting/diagrams/Rendered/snapshot-architecture.png)](doc/tutorial/portfolioReporting/diagrams/Rendered/snapshot-architecture.svg)

*Figure 16.2 — Snapshot architecture (Approach D). A dedicated read model stores pre-aggregated holdings data, decoupling read performance from transaction volume. The write model remains authoritative; the snapshot is a projection optimized for queries. ([PlantUML source](doc/tutorial/portfolioReporting/diagrams/snapshot-architecture.puml))*

### 16.7 Decision Matrix

| Dimension | Approach A | Approach B | Approach C | Snapshot |
|---|---|---|---|---|
| JVM memory | O(T) | O(H) | O(H) | O(H) |
| Architectural purity | Full | Compromised | High | Partial |
| Rounding correctness | Guaranteed | DB-dependent | Guaranteed | Guaranteed (if finalized in Java) |
| Testability | Unit tests | Integration only | Unit + integration | Complex |
| Production suitability | Up to ~50K tx | Millions | Millions | Unlimited |
| Implementation effort | Done | Low | Medium | High |

The correct approach depends on measurable deployment characteristics, not architectural preference. Instrument Approach A first. Run load tests. Identify the bottleneck. If it is the price API, optimize there (caching, batching). If it is transaction loading or heap pressure, move to Approach C. Re-measure.

---

## Chapter 17: Watchlists, Market Sentinel, and CQRS

### 17.1 The Problem Space

HexaStock extends its domain to include automated market monitoring: investors create personalized watchlists with price alerts, and a background "Market Sentinel" process continuously monitors market conditions and generates buy signals when opportunities arise.

This feature demonstrates how Command Query Responsibility Segregation (CQRS) naturally emerges when scaling read-heavy monitoring operations.

### 17.2 Why CQRS Emerges

Write operations (creating watchlists, adding alerts) and read operations (Market Sentinel detection) have fundamentally different characteristics:

| Aspect | Write Side | Read Side |
|---|---|---|
| Modifies state | Yes | No |
| Needs aggregate invariants | Yes | No |
| Can use projections | No | Yes |
| Frequency | Infrequent | Very frequent (scheduled) |
| Scale concern | Single aggregate | All active watchlists |

The naive approach — loading all watchlist aggregates into memory, iterating through each alert, and fetching prices repeatedly — does not scale. With 10,000 users and 200,000 watchlists, memory consumption explodes and price API calls multiply unnecessarily.

CQRS separates these concerns:
- **Commands** go through the rich domain model, loading aggregates and enforcing invariants
- **Queries** bypass the domain model entirely, using optimized database projections

The following diagram provides a side-by-side view of the command and query paths, making the structural separation visible:

[![CQRS overview — command path through aggregates vs. query path through projections](doc/tutorial/watchlists/diagrams/Rendered/cqrs-read-vs-write-overview.png)](doc/tutorial/watchlists/diagrams/Rendered/cqrs-read-vs-write-overview.svg)

*Figure 17.1 — CQRS read vs. write overview. The write side loads aggregates and enforces invariants through the domain model. The read side bypasses aggregates entirely, using projection queries optimized for the monitoring use case. ([PlantUML source](doc/tutorial/watchlists/diagrams/cqrs-read-vs-write-overview.puml))*

### 17.3 The Watchlist Aggregate (Write Side)

```java
public class Watchlist {
    private final WatchlistId id;
    private final String ownerName;
    private String listName;
    private boolean active;
    private final List<AlertEntry> alerts = new ArrayList<>();

    public void addAlert(Ticker ticker, Money thresholdPrice) {
        AlertEntry newAlert = new AlertEntry(ticker, thresholdPrice);
        if (alerts.contains(newAlert))
            throw new DuplicateAlertException(ticker, thresholdPrice);
        alerts.add(newAlert);
    }

    public void removeAlert(Ticker ticker, Money thresholdPrice) {
        AlertEntry target = new AlertEntry(ticker, thresholdPrice);
        if (!alerts.remove(target))
            throw new AlertNotFoundException(ticker, thresholdPrice);
    }
}
```

The aggregate uses `List<AlertEntry>` rather than `Map<Ticker, AlertEntry>`. This is a deliberate data structure choice: a `Map` would enforce at most one alert per ticker, preventing the ladder approach where an investor sets alerts at $150, $140, and $130 for the same stock. The `List` allows multiple entries for the same ticker at different thresholds.

### 17.4 The Market Sentinel Algorithm (Read Side)

The read side uses an optimized four-step algorithm that never loads a domain aggregate:

1. **Find distinct tickers** in active watchlists (one SQL query)
2. **Fetch prices once per ticker** (using `StockPriceProviderPort.fetchStockPrice(Set<Ticker>)`)
3. **Query triggered alerts** (database filters `threshold_price >= currentPrice`)
4. **Notify** via the notification adapter

```java
public class MarketSentinelService {
    private final WatchlistQueryPort queryPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final NotificationPort notificationPort;

    public void detectBuySignals() {
        Set<Ticker> tickers =
            queryPort.findDistinctTickersInActiveWatchlists();
        Map<Ticker, StockPrice> prices =
            stockPriceProviderPort.fetchStockPrice(tickers);

        for (Ticker ticker : tickers) {
            StockPrice stockPrice = prices.get(ticker);
            Money currentPrice = stockPrice.price().toMoney();
            queryPort.findTriggeredAlerts(ticker, currentPrice)
                .forEach(view ->
                    notificationPort.notifyBuySignal(
                        new BuySignal(view.ownerName(),
                            view.listName(), ticker,
                            view.thresholdPrice(), stockPrice)));
        }
    }
}
```

The query port uses projection queries — flat, denormalized views of data optimized for the read path:

```java
public interface WatchlistQueryPort {
    Set<Ticker> findDistinctTickersInActiveWatchlists();
    List<TriggeredAlertView> findTriggeredAlerts(
        Ticker ticker, Money currentPrice);
}
```

The database does the filtering. Only triggered alerts are returned. No aggregate is ever materialized.

The complete Market Sentinel detection flow is captured in the following diagram:

[![Market Sentinel detection flow — four-step read-side algorithm](doc/tutorial/watchlists/diagrams/Rendered/market-sentinel-detection-flow.png)](doc/tutorial/watchlists/diagrams/Rendered/market-sentinel-detection-flow.svg)

*Figure 17.2 — Market Sentinel detection flow. The algorithm queries distinct tickers, fetches prices once per ticker, filters triggered alerts via database projection, and dispatches notifications. No domain aggregate is loaded during detection. ([PlantUML source](doc/tutorial/watchlists/diagrams/market-sentinel-detection-flow.puml))*

### 17.5 Progressive Model Evolution

The alert system is designed to evolve through five progressive levels:

| Level | Feature | Domain Change |
|---|---|---|
| 1 | Price threshold alerts | `Watchlist` with `List<AlertEntry>` |
| 2 | Ladder alerts (multiple per ticker) | None — enabled by Level 1's List design |
| 3 | Percentage-change alerts | Add `AlertType` enum to `AlertEntry` |
| 4 | Contextual filters (memo) | Add `memo` field to `AlertEntry` |
| 5 | Alert fatigue prevention | Add `cooldownMinutes` to `AlertEntry` |

The `Watchlist` aggregate itself does not change after Level 1. It always holds a `List<AlertEntry>`. All evolution happens inside the `AlertEntry` value object. The container is stable; the contents evolve.

### 17.6 Pragmatic vs. Full CQRS

HexaStock applies a pragmatic, single-database form of CQRS. Both command and query paths share the same underlying relational database. There is no separate read store, no event-driven synchronization, and no eventual consistency to manage.

This approach captures the essential architectural insight — that read and write operations benefit from different code paths and data access strategies — without introducing the operational complexity of fully decoupled models. The step from this pragmatic form to a fully decoupled architecture is an infrastructure evolution, not a conceptual leap.

---

## Chapter 18: Extending Lot Selection Strategies

### 18.1 The Business Requirement

HexaStock was designed for the Spanish market, where FIFO is mandatory. To expand internationally, the system must support additional lot selection policies:

- **LIFO (Last-In, First-Out)** — most recently acquired shares are sold first
- **HIFO (Highest-In, First-Out)** — shares purchased at the highest price are sold first
- **Specific Lot Identification** — client explicitly selects which lots to sell (advanced, optional)

### 18.2 The Architectural Test

This extension is the definitive test of the architecture's claims. The guiding principle:

> **Business changes should primarily impact the domain. Infrastructure changes should impact infrastructure code only. The domain must not depend on frameworks, databases, or technical details.**

Lot selection is a business rule change. Therefore, the impact should be concentrated in the domain layer.

### 18.3 Design Constraints

The solution must satisfy:

- Lot selection logic resides entirely within the domain
- Policy selection depends on Portfolio state, not on external components
- New policies can be added without modifying existing policy implementations
- FIFO remains available and is not broken

**Explicitly forbidden:**
- Business logic in REST controllers
- Policy selection in the web layer
- Domain rules in infrastructure code
- Conditional logic scattered across layers

### 18.4 Policy as Portfolio State

Each Portfolio has an associated lot selection policy defined at creation time:

1. The policy is set when the portfolio is created
2. The policy is persisted as part of the aggregate state
3. The policy cannot be changed after creation
4. All sell operations automatically apply the configured policy

This design means the sell algorithm becomes polymorphic: `Holding.sell()` (or a strategy object within the domain) selects lots based on the portfolio's configured policy rather than always using FIFO order.

### 18.5 Proving the Architecture

The exercise validates both sides of the hexagonal architecture promise:

- **Business rule change (lot policies)**: Impact is concentrated in the domain layer — the aggregate, the lot selection algorithm, and domain tests. Services, controllers, and persistence adapters require minimal changes (accepting the policy parameter at creation time, persisting it as a column).

- **Infrastructure change (MongoDB adapter, Chapter 10)**: Impact is concentrated in the adapter layer — new document models, mappers, and repository implementations. The domain layer remains unchanged. All domain tests pass without modification.

Together, these extensions illustrate the complete architectural story: business logic is protected from infrastructure volatility, and infrastructure is protected from business logic evolution.

---

## Chapter 19: Architecture in the Age of AI

### 19.1 The Acceleration Paradox

AI accelerates implementation. Architecture defines structure. Without architecture, AI accelerates entropy. With architecture, AI amplifies engineering precision.

The distinction matters because AI code generation operates at the implementation level — it produces classes, methods, and tests. It does not produce architectural boundaries, invariant hierarchies, or aggregate design decisions. Those require engineering judgment about what changes together, what must remain consistent, and where the boundaries belong.

### 19.2 AI as Architectural Amplifier

In a well-architected system like HexaStock:

- AI can generate a new lot selection strategy by reading the existing domain model and creating a conforming implementation
- AI can generate a new stock price adapter by implementing the `StockPriceProviderPort` interface
- AI can generate integration tests by following the existing `@SpecificationRef` pattern
- AI can generate persistence code by following the mapper pattern

Each of these is safe because the architecture constrains where the generated code goes and what it depends on. The hexagonal boundaries prevent AI-generated code from accidentally coupling business logic to infrastructure.

### 19.3 AI Without Architecture

Without clear boundaries, AI generates code that works locally but creates coupling globally. A service that directly accesses the database, formats HTTP responses, and enforces business rules — all in one class — functions correctly. But it makes every future change ripple across layers. The initial velocity is high. The ongoing cost compounds.

### 19.4 The Organizational Imperative

Organizations adopting architectural discipline gain:

- **Infrastructure independence** — technology choices remain reversible
- **Safer evolution** — changes are isolated to the appropriate layer
- **Reduced technical debt** — boundaries prevent accidental coupling
- **Faster onboarding** — the architecture is self-documenting through package structure and port interfaces
- **Structural clarity** — the system's organization communicates intent

These properties do not emerge from pattern adoption. They emerge from boundary enforcement — the daily discipline of asking "where does this logic belong?" and placing it correctly.

---

# Appendices

---

## Appendix A: Complete API Specification

### A.1 Use Cases Summary

| ID | Use Case | Method | Endpoint |
|---|---|---|---|
| US-01 | Create Portfolio | POST | `/api/portfolios` |
| US-02 | Get Portfolio | GET | `/api/portfolios/{id}` |
| US-03 | List Portfolios | GET | `/api/portfolios` |
| US-04 | Deposit Funds | POST | `/api/portfolios/{id}/deposits` |
| US-05 | Withdraw Funds | POST | `/api/portfolios/{id}/withdrawals` |
| US-06 | Buy Stocks | POST | `/api/portfolios/{id}/purchases` |
| US-07 | Sell Stocks | POST | `/api/portfolios/{id}/sales` |
| US-08 | Get Transaction History | GET | `/api/portfolios/{id}/transactions` |
| US-09 | Get Holdings Performance | GET | `/api/portfolios/{id}/holdings` |
| US-10 | Get Stock Price | GET | `/api/stocks/{symbol}` |

### A.2 Error Contract (RFC 7807)

All errors use RFC 7807 Problem Details with a consistent structure:

```json
{
  "type": "about:blank",
  "title": "Human-readable error category",
  "status": 400,
  "detail": "Specific error message with context"
}
```

Global error mapping:

| HTTP Status | Title | Domain Exception |
|---|---|---|
| 400 | Invalid Amount | `InvalidAmountException` |
| 400 | Invalid Quantity | `InvalidQuantityException` |
| 400 | Invalid Ticker | `InvalidTickerException` |
| 404 | Portfolio Not Found | `PortfolioNotFoundException` |
| 404 | Holding Not Found | `HoldingNotFoundException` |
| 409 | Insufficient Funds | `InsufficientFundsException` |
| 409 | Conflict Quantity | `ConflictQuantityException` |
| 409 | Entity Already Exists | `EntityExistsException` |

### A.3 Sell Stocks Response

```json
{
  "portfolioId": "550e8400-e29b-41d4-a716-446655440000",
  "ticker": "AAPL",
  "quantity": 12,
  "proceeds": 1800.00,
  "costBasis": 1240.00,
  "profit": 560.00
}
```

### A.4 Holdings Performance Response

```json
[
  {
    "ticker": "AAPL",
    "quantity": 10,
    "remaining": 10,
    "averagePurchasePrice": 150.00,
    "currentPrice": 160.00,
    "unrealizedGain": 100.00,
    "realizedGain": 0.00
  }
]
```

---

## Appendix B: Diagram Index

The following PlantUML diagrams are maintained in the repository and should be consulted as visual companions to the text:

### Stock Selling Diagrams

| Diagram | Source Path | Description |
|---|---|---|
| Domain Class Diagram | `doc/tutorial/sellStocks/diagrams/domain-class-diagram.puml` | Complete domain model with entities, value objects, and relationships |
| Sell Use Case Flow | `doc/tutorial/sellStocks/diagrams/sell-use-case-flow.puml` | High-level flow from HTTP request to domain operation |
| Sell Domain Sequence | `doc/tutorial/sellStocks/diagrams/sell-domain-sequence.puml` | Detailed sequence within the domain layer |
| Sell Domain FIFO | `doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml` | FIFO algorithm step-by-step visualization |
| Sell Application Service | `doc/tutorial/sellStocks/diagrams/sell-application-service.puml` | Application service orchestration flow |
| Sell HTTP to Port | `doc/tutorial/sellStocks/diagrams/sell-http-to-port.puml` | Controller to use case interface invocation |
| Sell Persistence Adapter | `doc/tutorial/sellStocks/diagrams/sell-persistence-adapter.puml` | JPA adapter and mapper flow |
| Sell Error — Quantity | `doc/tutorial/sellStocks/diagrams/sell-error-sell-more-than-owned.puml` | Error flow for insufficient shares |
| Sell Error — Not Found | `doc/tutorial/sellStocks/diagrams/sell-error-portfolio-not-found.puml` | Error flow for missing portfolio |
| Sell Error — Invalid Quantity | `doc/tutorial/sellStocks/diagrams/sell-error-invalid-quantity.puml` | Error flow for zero/negative quantity |
| Orchestrator vs. Aggregate | `doc/tutorial/sellStocks/diagrams/sell-orchestrator-vs-aggregate.puml` | Comparison of service vs. domain responsibilities |

### Rich vs. Anemic Diagrams

| Diagram | Source Path | Description |
|---|---|---|
| Rich Architecture | `doc/tutorial/richVsAnemicDomainModel/diagrams/rich-architecture.puml` | Hexagonal architecture with rich domain |
| Anemic Architecture | `doc/tutorial/richVsAnemicDomainModel/diagrams/anemic-architecture.puml` | Architecture with anemic domain model |
| Rich Sell Sequence | `doc/tutorial/richVsAnemicDomainModel/diagrams/rich-sell-sequence.puml` | Sell flow in rich model |
| Anemic Sell Sequence | `doc/tutorial/richVsAnemicDomainModel/diagrams/anemic-sell-sequence.puml` | Sell flow in anemic model |
| Invariant Enforcement | `doc/tutorial/richVsAnemicDomainModel/diagrams/invariant-enforcement.puml` | Where invariants are checked in each model |
| Rule Drift | `doc/tutorial/richVsAnemicDomainModel/diagrams/rule-drift.puml` | How rules scatter in the anemic model |
| Domain Model | `doc/tutorial/richVsAnemicDomainModel/diagrams/domain-model.puml` | Class diagram comparing both approaches |

### Reporting and Scalability Diagrams

| Diagram | Source Path | Description |
|---|---|---|
| Approach A Sequence | `doc/tutorial/portfolioReporting/diagrams/approachA-sequence.puml` | In-memory aggregation flow |
| Approach B Sequence | `doc/tutorial/portfolioReporting/diagrams/approachB-sequence.puml` | DB-powered query flow |
| Approach C Sequence | `doc/tutorial/portfolioReporting/diagrams/approachC-sequence.puml` | Hybrid approach flow |
| Snapshot Architecture | `doc/tutorial/portfolioReporting/diagrams/snapshot-architecture.puml` | CQRS read model architecture |

### Watchlists and CQRS Diagrams

| Diagram | Source Path | Description |
|---|---|---|
| Watchlist Command Flow | `doc/tutorial/watchlists/diagrams/watchlist-command-flow.puml` | Write-side aggregate flow |
| Market Sentinel Detection | `doc/tutorial/watchlists/diagrams/market-sentinel-detection-flow.puml` | Read-side detection algorithm |
| CQRS Overview | `doc/tutorial/watchlists/diagrams/cqrs-read-vs-write-overview.puml` | Side-by-side command and query paths |

---

## Appendix C: Gherkin Feature File Index

All Gherkin specifications are maintained as canonical `.feature` files under `doc/features/`:

| Feature File | Use Case | Key Scenarios |
|---|---|---|
| `create-portfolio.feature` | US-01 | Create with valid name, reject blank name |
| `get-portfolio.feature` | US-02 | Retrieve existing, handle not found |
| `list-portfolios.feature` | US-03 | List all, empty list |
| `deposit-funds.feature` | US-04 | Valid deposit, reject zero/negative |
| `withdraw-funds.feature` | US-05 | Valid withdrawal, insufficient funds, invalid amount |
| `buy-stocks.feature` | US-06 | Sufficient funds, insufficient funds, invalid ticker/quantity |
| `sell-stocks.feature` | US-07 | FIFO single lot, FIFO cross-lot, insufficient shares |
| `get-transaction-history.feature` | US-08 | Retrieve transactions, type filter parameter |
| `get-holdings-performance.feature` | US-09 | Portfolio with holdings, empty portfolio, not found |
| `get-stock-price.feature` | US-10 | Valid ticker, invalid ticker |
| `fifo-settlement-selling.feature` | — | FIFO with settlement-aware constraints |
| `settlement-aware-selling.feature` | — | T+2 settlement window enforcement |
| `reserved-lot-handling.feature` | — | Reserved shares tracking |
| `settlement-fees.feature` | — | Fee deduction from proceeds |
| `rule-consistency.feature` | — | Cross-model invariant consistency |

