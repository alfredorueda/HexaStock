[![Build Status](https://github.com/alfredorueda/HexaStock/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/alfredorueda/HexaStock/actions/workflows/build.yml?query=branch%3Amain)

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=alert_status&branch=main)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock&branch=main) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=security_rating&branch=main)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock&branch=main) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=reliability_rating&branch=main)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock&branch=main) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=alfredorueda_HexaStock&metric=sqale_rating&branch=main)](https://sonarcloud.io/summary/new_code?id=alfredorueda_HexaStock&branch=main)

[![License](https://img.shields.io/badge/Code-Apache_2.0-blue.svg)](LICENSE) [![License: CC BY-NC 4.0](https://img.shields.io/badge/Book-CC_BY--NC_4.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc/4.0/)


# HexaStock

Learn to separate business rules from databases and HTTP by building a stock portfolio application. The example is small enough to study, but includes meaningful problems: cash balances, purchases, FIFO sales and concurrent updates.

You need basic Java and unit-testing knowledge to begin. You do not need prior experience with Domain-Driven Design (DDD) or hexagonal architecture.

**[Start the learning path](doc/learning/START-HERE.md)** · **[Read the book](https://alfredo-rueda-unsain.gitbook.io/alfredo-rueda-unsain-docs)** · **[Follow a sale in nine stages](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)**

## First run: business rules only

Install **JDK 21** and Git. Use the included Maven wrapper; a separate Maven installation and a paid IDE are unnecessary.

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
java -version
./mvnw -pl domain test
```

Expected result: `BUILD SUCCESS`. This first step runs the domain tests without Docker, Spring application startup or market-data keys. On Windows, use `mvnw.cmd` in place of `./mvnw`.

Then read [the sale requirements](doc/tutorial/sellStocks/SELL-STOCK-01-BEHAVIOUR.md) and [the first domain test](doc/tutorial/sellStocks/SELL-STOCK-02-DOMAIN-TEST.md). The key question is concrete: if shares were bought at different prices, what is the cost of the shares sold?

## Run the HTTP application

This step adds a database and a web server. Install and start Docker with Compose support first.

```bash
docker compose up -d mysql
./mvnw install -DskipTests
./mvnw spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=jpa,mockfinhub
```

Wait until MySQL is ready before starting the application. The first Maven command installs sibling modules needed by `bootstrap`; rerun it after changing their code. The second command starts the application on **http://localhost:8081**.

Open [Swagger UI](http://localhost:8081/swagger-ui.html), or use [the HTTP request collection](doc/calls.http). Create a portfolio, deposit cash, buy shares and then sell some of them. Copy the returned portfolio ID into subsequent requests.

The `mockfinhub` provider generates random prices. It is useful for exploration, but repeated requests are not an accounting test with a fixed expected price. Automated trading tests use a separate fixed-price adapter.

**Local database lifecycle:** the checked-in JPA configuration uses `spring.jpa.hibernate.ddl-auto=create-drop`. It recreates the application's tables at startup and drops them on a normal shutdown. Use this configuration with disposable learning data.

Stop the application with Ctrl+C and stop the database with `docker compose stop mysql`.

## Choose adapters when you are ready

An *adapter* connects application logic to a technology. Choose one persistence profile and one price-provider profile:

| Category | Profile | Purpose |
|---|---|---|
| Persistence | `jpa` | MySQL; pessimistic write locking |
| Persistence | `mongodb` | MongoDB; optimistic version checks and transaction retries |
| Price provider | `mockfinhub` | Random demonstration prices; no key |
| Price provider | `finhub` | Finnhub API; requires `FINNHUB_API_KEY` |
| Price provider | `alphaVantage` | Alpha Vantage API; requires `ALPHA_VANTAGE_API_KEY` |

For MongoDB, start `docker compose up -d mongo` and use `mongodb,mockfinhub`. The bundled service configures the `rs0` replica set needed for transactions. The default URI is `mongodb://localhost:27017/hexaStock?replicaSet=rs0`; override it with `SPRING_DATA_MONGODB_URI` if needed.

To try an external provider, copy `.env.example` to `.env`, replace the relevant placeholder and run `source .env` before the Maven command. The example uses `export`, so the variables reach the Java process. In an IDE, set these environment variables in the run configuration instead. `.env` is ignored by Git; keep credentials out of tracked files.

## Run the complete checks

With Docker running:

```bash
./mvnw clean verify
```

Testcontainers creates test databases; you do not need to start the Compose services for these tests. The command matches the lifecycle used by CI. If Docker discovery fails, first check `docker version`, `docker info` and the active Docker context. Investigate local socket or environment configuration before changing global Docker settings.

## Find your way through the code

A *portfolio* owns cash and holdings. A holding groups purchase lots for one ticker. A sale consumes the oldest remaining lots first: First In, First Out (FIFO).

| Module | Responsibility |
|---|---|
| [`domain`](domain) | Business rules. `Portfolio` is the aggregate root; `Holding` and `Lot` are child entities. No production framework dependencies. |
| [`application`](application) | Use cases, orchestration and port interfaces. Uses Jakarta transaction annotations. |
| [`adapters-inbound-rest`](adapters-inbound-rest) | HTTP controllers, request/response objects and error translation. |
| [`adapters-outbound-persistence-jpa`](adapters-outbound-persistence-jpa) | Maps the domain to relational entities and MySQL. |
| [`adapters-outbound-persistence-mongodb`](adapters-outbound-persistence-mongodb) | Maps the same domain to MongoDB documents. |
| [`adapters-outbound-market`](adapters-outbound-market) | Obtains prices from a provider or the demonstration adapter. |
| [`bootstrap`](bootstrap) | Starts Spring Boot, selects adapters and wires dependencies. |

The Maven module graph and architecture tests check specific dependency boundaries. They help preserve the design; they do not prove every business rule or encapsulation property.

## Continue reading

| Your next question | Read |
|---|---|
| Where should I begin? | [Beginner learning path](doc/learning/START-HERE.md) |
| How does one sale work? | [Staged sell-stock tutorial](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md) |
| What does each business term mean? | [Domain vocabulary](doc/tutorial/sellStocks/UBIQUITOUS-LANGUAGE.md) |
| Which HTTP operations exist? | [API specification](doc/stock-portfolio-api-specification.md) and [OpenAPI](doc/openapi.yaml) |
| How can I contribute? | [Contribution guide](CONTRIBUTING.md) |
| Where is the rest of the book? | [Table of contents](SUMMARY.md) |

The implemented baseline supports portfolios, cash movements, purchases, FIFO sales, transaction history and holdings reports. Watchlists, settlement, fees and alternative lot-selection rules appear in advanced design material or exercises; they are not all implemented features of `main`.

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
