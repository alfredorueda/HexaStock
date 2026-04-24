[![Build Status](https://github.com/alfredorueda/HexaStock/workflows/CI/badge.svg)](https://github.com/alfredorueda/HexaStock/actions)

[![Quality Gate](https://sonarcloud.io/api/project_badges/quality_gate?project=alfredorueda_HexaStock)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock)

[![License](https://img.shields.io/badge/Code-Apache_2.0-blue.svg)](LICENSE) [![License: CC BY-NC 4.0](https://img.shields.io/badge/Book-CC_BY--NC_4.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc/4.0/)

---

### Domain-Driven Design and Hexagonal Architecture in a Financial Domain

This repository is accompanied by a technical book that serves as the long-form reference for the project. The book traces the architectural and domain reasoning behind HexaStock — from strategic design decisions and aggregate boundaries to persistence trade-offs and testing strategies — following the conceptual frameworks of Domain-Driven Design [Evans, 2003; Vernon, 2013] and the Hexagonal Architecture style [Cockburn, 2005].

The financial domain is not incidental. It surfaces modelling tensions that are difficult to contrive artificially: FIFO lot accounting, settlement mechanics, and concurrent mutation of a shared portfolio under transactional guarantees. The resulting case study grounds every design decision in concrete business semantics rather than textbook abstractions.

The book and the repository are complementary. The codebase provides the executable implementation; the book documents the reasoning, the alternatives considered, and the architectural principles that shaped each decision. Taken together, they offer a study path for engineers and architects applying DDD and Hexagonal Architecture to non-trivial domains.

**[➜ Explore the full book](https://alfredo-rueda-unsain.gitbook.io/alfredo-rueda-unsain-docs)**

---

# HexaStock

HexaStock is an instructional project that illustrates Domain-Driven Design and Hexagonal Architecture through a realistic financial-portfolio domain. Developed for software engineering students and workshop participants, it provides a complete, testable system with non-trivial business rules, multiple interchangeable adapters, and explicit architectural boundaries enforced at both module and fitness-test levels.

### Sell Stock Deep Dive (Reference Use Case)

> **[doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)**

The sell-stock use case is the tutorial reference of the project. It concentrates the highest density of domain logic in the system and therefore provides the most complete illustration of DDD and Hexagonal Architecture in practice. The tutorial covers:

- Full execution trace: REST adapter → application service → domain model → persistence adapter.
- Aggregate boundary enforcement (`Portfolio` as aggregate root enforcing invariants) [Evans, 2003, ch. 6].
- FIFO lot-accounting logic implemented entirely inside the domain layer.
- Orchestration by application services versus rule enforcement by the aggregate.
- Exercises for training sessions and self-directed study.

### Documentation Map

| Document | Description |
|----------|-------------|
| [API Specification](doc/stock-portfolio-api-specification.md) | User stories, domain model, error handling, HTTP examples |
| [Sell Stock Tutorial](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md) | End-to-end use case walkthrough with exercises |
| [Rich vs Anemic Domain Model](doc/tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) | Architectural comparison of domain modelling approaches |
| [HTTP Requests](doc/calls.http) | Pre-built requests for manual API testing in IntelliJ |
| [OpenAPI Definition](doc/openapi.yaml) | Machine-readable API contract |
| [DDD Portfolio and Transactions](doc/DDD%20Portfolio%20and%20Transactions.md) | Domain design notes for portfolios and transactions |
| [Remove Zero-Quantity Lots](doc/Remove%20Lots%20with%20Zero%20Remaining%20Quantity%20from%20Portfolio%20Aggregate.md) | Design discussion on aggregate cleanup |
| [Dependency Inversion (Selling)](doc/tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md) | Tutorial on dependency inversion in the sell flow |
| [Concurrency and Pessimistic Locking](doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) | Handling concurrent portfolio updates |
| [DDD Hexagonal Exercise](doc/tutorial/DDD-Hexagonal-exercise.md) | Guided exercise on hexagonal patterns |
| [Holdings Performance at Scale](doc/tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) | Reporting and performance considerations |
| [Watchlists: Market Sentinel](doc/tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) | Watchlist feature design and tutorial |
| [MongoDB Adapter: Optimistic Write & Retry](doc/mongodb-adapter-optimistic-write-and-retry.md) | Concurrency strategy of the MongoDB persistence adapter |
| [Spring Modulith Notifications POC](doc/architecture/SPRING-MODULITH-NOTIFICATIONS-POC.md) | Experimental in-process event-driven boundary between Watchlists and Notifications (modular monolith, no Kafka) |
| [Spring Modulith Global Refactoring Plan](doc/architecture/SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md) | Phased plan for evolving HexaStock toward Spring Modulith while preserving DDD, hexagonal, and Maven multi-module discipline |
| [Modulith Bounded Context Inventory](doc/architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md) | Living catalogue of which legacy classes will eventually move into each Modulith application module |

## Project Structure

HexaStock is a Maven multi-module project. Each module corresponds to a layer of the hexagonal architecture, so the dependency rule (adapters depend on the application core, never the reverse) is enforced at build time.

| Module | Role |
|--------|------|
| [`domain`](domain) | Pure domain model: aggregates (`Portfolio`, `Holding`, `Lot`), value objects, domain services and exceptions. No framework dependencies. |
| [`application`](application) | Application services and ports. Inbound ports (`PortfolioLifecycleUseCase`, `CashManagementUseCase`, `PortfolioStockOperationsUseCase`, `TransactionUseCase`, `ReportingUseCase`, `GetStockPriceUseCase`) and outbound ports (`PortfolioPort`, `TransactionPort`, `StockPriceProviderPort`). |
| [`adapters-inbound-rest`](adapters-inbound-rest) | Inbound REST adapter: Spring MVC controllers, DTOs and global error handling. |
| [`adapters-outbound-persistence-jpa`](adapters-outbound-persistence-jpa) | Outbound persistence adapter implementing `PortfolioPort` and `TransactionPort` with Spring Data JPA over MySQL. Active under the `jpa` profile. |
| [`adapters-outbound-persistence-mongodb`](adapters-outbound-persistence-mongodb) | Outbound persistence adapter implementing the same ports with Spring Data MongoDB. Active under the `mongodb` profile. Provides its own `MongoTransactionManager` and requires a MongoDB replica set for multi-document transactions. |
| [`adapters-outbound-market`](adapters-outbound-market) | Outbound adapter implementing `StockPriceProviderPort` against external market data providers (Finnhub, Alpha Vantage) with a mock variant for tests. |
| [`bootstrap`](bootstrap) | Spring Boot composition root (`HexaStockApplication`), profile-specific configuration (`application-jpa.properties`, `application-mongodb.properties`), cross-cutting concerns and end-to-end integration tests. |

### Persistence adapters

The two persistence adapters implement the same outbound ports (`PortfolioPort`, `TransactionPort`) defined in the `application` module, so the domain and application layers are unaware of which technology is in use.

## Prerequisites

- JDK 21 or higher
- Apache Maven 3.6 or higher
- Docker (for MySQL via Docker Compose and for TestContainers during tests)
- IntelliJ IDEA Ultimate (recommended)
- Git

## Getting Started

### 1. Clone and run the tests

No API keys or database setup needed — TestContainers handles everything automatically (Docker must be running):

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
./mvnw clean test
```

### 2. Run the application locally

To run the full application with real stock prices you need three things: the databases, your API keys, and the Spring Boot profiles.

**Start local databases (MySQL + MongoDB):**

```bash
docker-compose up -d
```

**Set up your API keys** (one-time step):

```bash
cp .env.example .env        # copy the example file
# edit .env and add your real keys (free-tier keys work fine)
```

Free-tier keys: [finnhub.io](https://finnhub.io/) · [alphavantage.co](https://www.alphavantage.co/support/#api-key)

**Start the application:**

```bash
source .env
./mvnw install -DskipTests -q
./mvnw spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=jpa,finhub
# or:
# ./mvnw spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=mongodb,finhub
```

The first command compiles and installs all modules into your local Maven repository (only needed once, or after code changes). The second starts the app.

The app starts on **http://localhost:8081**. Swagger UI is at [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html).

> **Why two Maven commands?** HexaStock is a multi-module project. `spring-boot:run` can only target a single module (`-pl bootstrap`), so the sibling modules (domain, application, adapters) must be installed first.

### 3. Try the API

Pre-configured HTTP requests are in [doc/calls.http](doc/calls.http).

In IntelliJ IDEA, open the file and click the green **Run** arrow next to any request. Example workflow:

1. Create a portfolio
2. Deposit cash
3. Buy stocks (e.g. AAPL, MSFT)
4. Check portfolio status
5. Sell stocks
6. View transaction history

## Spring Profiles

The application requires two Spring profiles to start:

| Category | Profile | Description |
|----------|---------|-------------|
| Persistence (choose one) | `jpa` | Spring Data JPA / Hibernate over MySQL; pessimistic locking. |
| | `mongodb` | Spring Data MongoDB; optimistic locking with retry. Requires a replica set (the bundled `docker-compose.yml` configures `rs0`). |
| Stock price provider (choose one) | `finhub` | Finnhub real-time API |
| | `alphaVantage` | Alpha Vantage API |
| | `mockfinhub` | Random prices — no API key needed (used by tests) |

Valid combinations: `jpa,finhub` · `jpa,alphaVantage` · `jpa,mockfinhub` · `mongodb,finhub` · `mongodb,alphaVantage` · `mongodb,mockfinhub`

**From IntelliJ IDEA:** open *Run > Edit Configurations*, set the *Active profiles* field (e.g. `jpa,finhub`) and add environment variables from your `.env` file.

## API Keys Configuration

API keys are read from **environment variables** so that real secrets are never committed to version control.

| Variable | Purpose | Required for |
|----------|---------|-------------|
| `FINNHUB_API_KEY` | [Finnhub](https://finnhub.io/) stock price API | Running with profile `finhub` |
| `ALPHA_VANTAGE_API_KEY` | [Alpha Vantage](https://www.alphavantage.co/) stock price API | Running with profile `alphaVantage` |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection URI | Running with profile `mongodb` |

> **Tests do not require real API keys.** The test suite uses the `mockfinhub` profile with TestContainers, so `./mvnw clean test` works out of the box without any `.env` file.

The `.env` file is listed in `.gitignore` and will never be committed.

## Troubleshooting: "Could not find a valid Docker environment"

HexaStock uses **Testcontainers 1.21.4**, which handles Docker Engine 27.x and 29.x out of the box. If you still see this error:

1. Run `docker version` — confirm the API version is ≥ 1.44
2. Check for a stale `~/.docker-java.properties` that might force a wrong API version — delete or fix it
3. If you use **Podman** or **Colima**, create `~/.docker-java.properties` with:
   ```properties
   api.version=1.44
   ```
4. Make sure Docker Desktop is running and the Docker socket is accessible

## Acknowledgments

The pedagogical idea behind HexaStock — building a financial portfolio system as a Project-Based Learning experience — was first encountered during software engineering training engagements delivered in collaboration with [Neueda](https://neueda.com/), where the author works as an instructor for major international financial institutions. That experience inspired the later creation and expansion of HexaStock as a dedicated open-source project focused on Hexagonal Architecture and Domain-Driven Design.

A special acknowledgment goes to [Agència de Gestió d'Ajuts Universitaris i de Recerca (AGAUR)](https://agaur.gencat.cat/ca/inici), part of the Public Administration of Catalonia, where HexaStock was first formally adopted for institutional training.

Particular thanks to [Francisco José Nebrera](https://www.linkedin.com/in/francisco-jose-nebrera/), whose initiative led to that first organizational implementation.

The original Java package prefix `cat.gencat.agaur.hexastock` reflects this early institutional context. The project was later open-sourced and continues to evolve.

## License

This project uses a **dual-license model** to reflect the distinct nature of its components.

### Source Code — Apache License 2.0

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

All source code in this repository is licensed under the [Apache License, Version 2.0](LICENSE). You are free to use, modify, and distribute the software for any purpose — including learning, teaching, research, consulting, and commercial use — subject to the terms of the license. See the [LICENSE](LICENSE) and [NOTICE](NOTICE) files for details.

### Book and Editorial Content — CC BY-NC 4.0

[![License: CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

This book and its editorial written content are licensed under the [Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0)](https://creativecommons.org/licenses/by-nc/4.0/).

You are welcome to read, share, and adapt this material for non-commercial purposes, provided that appropriate credit is given.

For commercial use, including commercial training, paid courses, corporate programs, commercial publishing, or reuse within paid products or services, prior permission is required.

### Why Dual Licensing?

- **Source code** is licensed under Apache License 2.0.
- **Book and editorial written content** are licensed under CC BY-NC 4.0.

This dual-license approach is intentional: the software remains open source under Apache 2.0, while the book is shared openly for learning and non-commercial educational use under Creative Commons.

> If you use HexaStock in a university course, a corporate training programme, a consulting engagement, or as the foundation for your own project, the author would genuinely appreciate hearing about it. A quick note — an issue, a discussion, or a message — goes a long way and helps the project grow in the right directions. **This is a courtesy invitation, not a legal requirement.**

## Using HexaStock? Let us know

If you would like to share how you are using HexaStock — or simply say hello — feel free to [open a discussion](https://github.com/alfredorueda/HexaStock/discussions) or [create an issue](https://github.com/alfredorueda/HexaStock/issues). Hearing from practitioners, students, and organisations helps shape the project and is always welcome.
