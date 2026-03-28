[![Build Status](https://github.com/alfredorueda/HexaStock/workflows/CI/badge.svg)](https://github.com/alfredorueda/HexaStock/actions)

[![Quality Gate](https://sonarcloud.io/api/project_badges/quality_gate?project=alfredorueda_HexaStock)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock)

[![License](https://img.shields.io/badge/Code-Apache_2.0-blue.svg)](LICENSE) [![License: CC BY-NC 4.0](https://img.shields.io/badge/Book-CC_BY--NC_4.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc/4.0/)

---

### Domain-Driven Design and Hexagonal Architecture in a Financial Domain

This repository is accompanied by a comprehensive technical book that serves as the primary long-form reference for the project. It traces the full architectural and domain journey behind HexaStock — from strategic design decisions and aggregate boundaries to persistence trade-offs and testing strategies.

The financial domain was chosen deliberately: it surfaces real modelling tensions — FIFO lot accounting, settlement mechanics, concurrent portfolio mutations — without being artificially contrived. The result is a case study where every design choice has a concrete, defensible reason grounded in actual business semantics.

The book and the repository are designed to complement each other. The codebase provides the working implementation; the book provides the _why_ — the reasoning, the alternatives considered, and the architectural principles that shaped each decision. Together, they offer a complete learning path for engineers and architects working with DDD and Hexagonal Architecture in non-trivial domains.

**[➜ Explore the full book](https://alfredo-rueda-unsain.gitbook.io/alfredo-rueda-unsain-docs)**

---

# HexaStock

HexaStock is a project that teaches Domain-Driven Design and Hexagonal Architecture through a realistic financial portfolio domain. Built for engineering students and workshop participants, it provides a complete, testable system with real business rules, multiple interchangeable adapters, and clear architectural boundaries.

## Core Documentation

Three documents form the backbone of HexaStock's documentation.

### Stock Portfolio API Specification

> **[doc/stock-portfolio-api-specification.md](doc/stock-portfolio-api-specification.md)**

The functional reference for the entire system:

- User stories covering portfolio creation, deposits, withdrawals, buying/selling stocks, holdings, transactions, and stock price queries
- Global error handling following RFC 7807 Problem Details
- Domain model overview with PlantUML diagrams
- HTTP request/response examples for every endpoint

### Sell Stock Deep Dive (Reference Use Case)

> **[doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)**

The sell stock use case is the architectural reference of the project. It concentrates the highest density of domain logic and best illustrates DDD and Hexagonal Architecture in practice. The tutorial covers:

- Full execution trace: REST adapter → application service → domain model → persistence
- Aggregate boundary protection (`Portfolio` as aggregate root enforcing invariants)
- FIFO accounting logic implemented entirely in the domain layer
- Orchestration by application services vs. rule enforcement by the aggregate
- Hands-on exercises for training sessions and self-guided study

### Rich vs Anemic Domain Model (Architectural Deep Dive)

> **[doc/tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md](doc/tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)**

A side-by-side architectural comparison of the two fundamental approaches to domain modelling. Using HexaStock's own sell-stock use case, it shows how business rules migrate between layers depending on the chosen model. The tutorial covers:

- Rich domain model: invariants enforced inside the aggregate, FIFO logic in `Holding.sell()`
- Anemic domain model: entities reduced to data carriers, business rules pushed to application services
- Seven PlantUML diagrams (class, sequence, and architecture) contrasting both approaches
- Concrete consequences for testability, encapsulation, and aggregate boundary protection
- When an anemic model can be a pragmatic choice and when it becomes a liability

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

## Prerequisites

- JDK 21 or higher
- Apache Maven 3.6 or higher
- IntelliJ IDEA Ultimate (recommended)
- Docker (for running MySQL in a container)
- Git

## Troubleshooting: "Could not find a valid Docker environment"

HexaStock uses **Testcontainers 1.21.4**, which handles Docker Engine 27.x and 29.x out of the box. If you still see this error:

1. Run `docker version` — confirm the API version is ≥ 1.44
2. Check for a stale `~/.docker-java.properties` that might force a wrong API version — delete or fix it
3. If you use **Podman** or **Colima**, create `~/.docker-java.properties` with:
   ```properties
   api.version=1.44
   ```
4. Make sure Docker Desktop is running and the Docker socket is accessible

## Running the Application

### Mandatory Spring Profiles

The application will **not start** without activating the required profiles. You must specify:

- **Persistence (mandatory):** `jpa`
- **Stock price provider (choose one):** `finhub` or `alphaVantage`

Valid combinations:
- `jpa,finhub`
- `jpa,alphaVantage`

**From the command line:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=jpa,finhub
```

**From IntelliJ IDEA:** open *Run > Edit Configurations*, set the *Active profiles* field to `jpa,finhub` or `jpa,alphaVantage`, then run.

The application starts on port **8081**.

## API Keys Configuration

API keys are read from **environment variables** so that real secrets are never committed to version control.

| Variable | Purpose | Required for |
|----------|---------|-------------|
| `FINNHUB_API_KEY` | [Finnhub](https://finnhub.io/) stock price API | Running with profile `finhub` |
| `ALPHA_VANTAGE_API_KEY` | [Alpha Vantage](https://www.alphavantage.co/) stock price API | Running with profile `alphaVantage` |

> **Tests do not require real API keys.** The test suite uses the `mockfinhub` profile with TestContainers, so `./mvnw clean test` works out of the box.

### Quick setup

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and replace the placeholders with your real keys (free-tier keys work fine).
3. Export the variables before running the application:
   ```bash
   source .env
   ./mvnw spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=jpa,finhub
   ```

Alternatively, set the variables in your IntelliJ run configuration (*Run > Edit Configurations > Environment variables*).

The `.env` file is listed in `.gitignore` and will never be committed.

## Interacting with the Application

Pre-configured HTTP requests are in [doc/calls.http](doc/calls.http).

**To use in IntelliJ IDEA:**
1. Open `doc/calls.http`
2. Ensure the application is running
3. Click the green **Run** arrow next to any request

**Example workflow:**
1. Create a portfolio
2. Deposit cash
3. Buy stocks (e.g. AAPL, MSFT)
4. Check portfolio status
5. Sell stocks
6. View transaction history

Swagger UI is available at `http://localhost:8081/swagger-ui.html`.

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