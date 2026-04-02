# HexaStock — Project Overview and Architectural Foundation

> This companion document provides the full project background for HexaStock — its purpose, architectural identity, module structure, domain organization, and the rationale behind key structural decisions. The [Sell Stock Deep Dive](SELL-STOCK-TUTORIAL.md) references this document for readers who want a deeper understanding of the system before (or alongside) tracing the sell use case through code.

---

## What the System Does

HexaStock is a stock portfolio management platform. The system enables investors to create and manage investment portfolios, deposit and withdraw funds, buy and sell stocks with automatic FIFO lot accounting, track holdings performance with real-time market prices, and view complete transaction history. The platform integrates with external stock price providers (Finnhub, AlphaVantage), persists data through JPA with MySQL, and exposes a RESTful API documented via OpenAPI 3.0.

---

## Architectural Identity

HexaStock is structured according to two complementary architectural disciplines.

**Domain-Driven Design** provides the modeling methodology. The system's core concepts — `Portfolio`, `Holding`, `Lot`, and `Transaction` — are modeled as aggregates, entities, and value objects that encapsulate business rules and protect invariants. Application services orchestrate use cases; controllers and adapters translate at the boundaries.

**Hexagonal Architecture** (Ports and Adapters) provides the structural organization. The domain model has no dependencies on frameworks, databases, or HTTP. It communicates with the outside world exclusively through port interfaces, which are implemented by adapters in the infrastructure layer. All dependencies point inward toward the domain.

---

## Module Structure: A Deliberate Choice

HexaStock is built as a **Maven multi-module project**. Each module corresponds to a distinct architectural responsibility in the hexagonal model, so the boundaries that a developer reads in a diagram are the same boundaries enforced by the build system.

```
HexaStock (parent pom)
├── domain/                              → Pure business model — no framework dependencies
├── application/                         → Use case orchestration, inbound and outbound ports
├── adapters-inbound-rest/               → Driving adapter: REST controllers, DTOs, error mapping
├── adapters-outbound-persistence-jpa/   → Driven adapter: JPA entities, repositories, mappers
├── adapters-outbound-market/            → Driven adapter: external stock-price provider clients
└── bootstrap/                           → Spring Boot entry point, composition root, runtime wiring
```

**`domain`** contains the framework-independent core: aggregates, entities, value objects, domain exceptions, and business rules. It has no dependency on Spring, JPA, or any infrastructure library. All other modules may depend on `domain`; `domain` depends on nothing outside the JDK.

**`application`** defines the use cases that the system supports. Inbound ports (e.g., `PortfolioStockOperationsUseCase`) declare what the outside world can ask the system to do; outbound ports (e.g., `PortfolioPort`, `StockPriceProviderPort`) declare what the application needs from infrastructure. Application services implement the inbound ports by orchestrating domain objects and outbound ports, without ever referencing a concrete adapter.

**`adapters-inbound-rest`** is the driving adapter layer. It translates HTTP requests into calls on inbound ports and maps domain results back to JSON responses. REST controllers, request/response DTOs, and the global exception-handling advice live here.

**`adapters-outbound-persistence-jpa`** is the driven persistence adapter. It implements the outbound `PortfolioPort` and `TransactionPort` using JPA entities, Spring Data repositories, and bidirectional mappers that convert between domain objects and their database representations.

**`adapters-outbound-market`** is the driven external-service adapter. It implements `StockPriceProviderPort` by integrating with third-party market APIs (Finnhub, AlphaVantage), including a mock adapter for offline development and testing.

**`bootstrap`** is the composition root. It contains `HexaStockApplication`, the Spring Boot entry point that scans all modules and wires ports to their adapter implementations at startup. Configuration classes and runtime profiles live here. No business logic resides in this module — its sole purpose is assembly.

### Domain organization: business meaning over technical category

Inside the `domain` module, the model is not organized as a flat technical taxonomy (`entity/`, `valueobject/`, `exception/`). Instead, concepts are grouped by **business meaning**:

```
cat.gencat.agaur.hexastock.model
├── portfolio/    → Portfolio aggregate root, Holding, Lot, SellResult, and related exceptions
├── transaction/  → Transaction entity, TransactionId, TransactionType
├── market/       → StockPrice, Ticker, and market-specific exceptions
├── money/        → Money, Price, ShareQuantity, and monetary validation exceptions
```

`Portfolio` is the central aggregate. Its related concepts — `Holding`, `Lot`, `HoldingPerformance`, `SellResult` — live in the same `portfolio` package because they participate in the same consistency boundary. Shared value objects that cross aggregate boundaries (`Money`, `Price`, `ShareQuantity`) are grouped under `money`, and market-related concepts under `market`. This semantic grouping makes navigating the domain intuitive: the package name tells the reader *what business concept* the code belongs to, not merely *what DDD building block* it implements.

### Why a multi-module structure?

Hexagonal Architecture does not mandate a single filesystem layout. Organizing by feature, by bounded context, or by a combination of both would be equally valid — and often preferable in larger systems with multiple bounded contexts. HexaStock uses module-level separation because its primary audiences — engineers, architects, and teams adopting hexagonal design — benefit most from seeing architectural boundaries enforced physically.

When each layer is a separate Maven module, the compiler itself prevents illegal dependencies: the `domain` module *cannot* import a Spring annotation, and an adapter *cannot* bypass an application port to reach another adapter. This transforms the hexagonal dependency rule from a convention that requires discipline into a constraint that the build enforces automatically. ArchUnit fitness tests (see [Testing Strategy](SELL-STOCK-TUTORIAL.md#5-testing-strategy-overview) in the main tutorial) provide a second enforcement layer: they scan compiled classes across all modules and catch dependency violations — such as a domain class importing a Spring type via a transitive path — that module boundaries alone cannot detect. The result is a codebase where the architecture is not just documented — it is structurally guaranteed at both build time and test time.

These Maven modules do not represent separate bounded contexts. They are architectural partitions inside the same bounded context, used to make dependency rules explicit and enforceable at build time.

---

## Conventions

Code listings throughout the HexaStock documentation are drawn from the actual repository source. Architecture and sequence diagrams are maintained as Mermaid (`.mmd`) or PlantUML (`.puml`) source files under `doc/tutorial/*/diagrams/` and rendered as SVG images. Gherkin scenarios are maintained as canonical `.feature` files under `doc/features/`. All financial calculations use `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`.

---

*Return to the [Sell Stock Deep Dive](SELL-STOCK-TUTORIAL.md) or the [book home page](../../BOOK-HOME.md).*
