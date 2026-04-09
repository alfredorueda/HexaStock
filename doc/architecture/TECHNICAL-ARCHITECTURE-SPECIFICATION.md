# HexaStock - Technical Architecture Specification

**Version:** 1.0
**Date:** 2026-04-09
**Status:** Living document, maintained alongside the codebase

---

## 1. Purpose and scope

This document captures the technological and architectural decisions embodied in the HexaStock repository. It is the explicit specification layer that sits between:

- **Behavioural specifications** (Gherkin `.feature` files under [doc/features/](../features/))
- **API contract** ([doc/openapi.yaml](../openapi.yaml))
- **Domain and architecture visualisations** (PlantUML diagrams under [doc/tutorial/](../tutorial/))
- **Source code and automated tests**

The document does not restate behaviour already defined in Gherkin scenarios, does not duplicate API details covered by the OpenAPI contract, and does not repeat domain structure already shown in PlantUML diagrams.

Instead, it formally documents:

- Why specific technologies were chosen
- How the architecture is structured and enforced
- How domain-driven design principles are applied
- How persistence, testing, and build pipelines support the architecture
- How all specification artefacts relate to one another

Every statement in this document is grounded in repository evidence. Where interpretation is required, it is labelled explicitly.

---

## 2. Specification landscape

HexaStock follows a specification-first engineering discipline, documented in [CONTRIBUTING.md](../../CONTRIBUTING.md):

> Specification -> Contract -> Tests -> Implementation -> Documentation

Five categories of specification artefact exist in the repository, each with a distinct role:

| Artefact | Role | Location |
|----------|------|----------|
| **Gherkin scenarios** | Define *what* the system does in human-readable behavioural terms. Each scenario has a stable identifier (e.g. `US-07.FIFO-1`) referenced by tests via `@SpecificationRef`. | [doc/features/](../features/) |
| **OpenAPI contract** | Machine-readable API contract specifying endpoints, request/response schemas, error codes (RFC 7807), and data types. | [doc/openapi.yaml](../openapi.yaml) |
| **PlantUML diagrams** | Visualise domain model structure, hexagonal architecture layers, sequence flows, and architectural comparisons. | [doc/tutorial/](../tutorial/) (`*/diagrams/*.puml`, 27 files) |
| **ADRs** | Capture discrete architectural and technological decisions with context, alternatives, and consequences. | [doc/architecture/adr/](./adr/) |
| **Source code and tests** | The executable specification. Tests annotated with `@SpecificationRef` create traceable links back to Gherkin scenarios. Architecture tests (ArchUnit) enforce structural rules. | [domain/](../../domain/), [application/](../../application/), [adapters-inbound-rest/](../../adapters-inbound-rest/), [adapters-outbound-persistence-jpa/](../../adapters-outbound-persistence-jpa/), [adapters-outbound-market/](../../adapters-outbound-market/), [bootstrap/](../../bootstrap/) |

These artefacts complement one another:

- Gherkin defines behaviour; tests verify it; source code implements it.
- OpenAPI defines the API contract; the REST adapter implements it; integration tests validate it.
- PlantUML visualises structure; ArchUnit tests enforce it; Maven modules codify it.
- ADRs document the *why* behind decisions that the other artefacts embody.

---

## 3. Project overview

**Repository name:** HexaStock
**GitHub:** `alfredorueda/HexaStock`
**Group ID:** `cat.gencat.agaur`
**Artifact ID:** `HexaStock`
**Version:** `0.0.1-SNAPSHOT`

**Evidence:** Root [pom.xml](../../pom.xml) lines 12-14, [README.md](../../README.md) badge URLs.

### Business domain

HexaStock is a personal investment portfolio management system. Users create portfolios, deposit and withdraw cash, buy and sell stocks at live market prices, and track holdings performance. The domain implements FIFO (First-In-First-Out) lot accounting for stock sales, calculates cost basis, proceeds, and profit/loss per transaction, and enforces business invariants such as insufficient funds, positive quantities, and valid ticker symbols.

**Evidence:** [README.md](../../README.md) introduction; Gherkin feature files ([create-portfolio.feature](../features/create-portfolio.feature), [buy-stocks.feature](../features/buy-stocks.feature), [sell-stocks.feature](../features/sell-stocks.feature), [deposit-funds.feature](../features/deposit-funds.feature), [withdraw-funds.feature](../features/withdraw-funds.feature)); [Portfolio.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java) aggregate root.

### Architectural intent

The project applies Domain-Driven Design and Hexagonal Architecture (Ports and Adapters) to a non-trivial financial domain. It serves both as a production-grade system and as an educational case study for engineering students exploring DDD, hexagonal boundaries, and specification-first development.

**Evidence:** [README.md](../../README.md) ("teaches Domain-Driven Design and Hexagonal Architecture through a realistic financial portfolio domain"); [CONTRIBUTING.md](../../CONTRIBUTING.md) architectural principles section; [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java).

---

## 4. Technology stack

| Technology | Version / Detail | Evidence |
|------------|-----------------|----------|
| **Java** | 21 | Root [pom.xml](../../pom.xml): `<java.version>21</java.version>`; [.github/workflows/build.yml](../../.github/workflows/build.yml): `java-version: '21'` |
| **Spring Boot** | 3.5.0 | Root [pom.xml](../../pom.xml): `spring-boot-starter-parent` version `3.5.0` |
| **Build tool** | Apache Maven (multi-module) | Root [pom.xml](../../pom.xml) `<packaging>pom</packaging>` with 6 modules; Maven wrapper ([mvnw](../../mvnw)) included |
| **Persistence** | Spring Data JPA with Hibernate | [adapters-outbound-persistence-jpa/pom.xml](../../adapters-outbound-persistence-jpa/pom.xml): `spring-boot-starter-data-jpa` |
| **Database** | MySQL 8.0.32 | [docker-compose.yml](../../docker-compose.yml): `image: mysql:8.0.32`; [application.properties](../../bootstrap/src/main/resources/application.properties): MySQL JDBC URL; `mysql-connector-j` version `8.2.0` |
| **API documentation** | SpringDoc OpenAPI 2.8.5 | [adapters-inbound-rest/pom.xml](../../adapters-inbound-rest/pom.xml): `springdoc-openapi-starter-webmvc-ui`; [application.properties](../../bootstrap/src/main/resources/application.properties): Swagger UI path at `/swagger-ui.html` |
| **Caching** | Spring Cache with Caffeine | [adapters-outbound-market/pom.xml](../../adapters-outbound-market/pom.xml): `spring-boot-starter-cache` + `caffeine`; [application.properties](../../bootstrap/src/main/resources/application.properties): `spring.cache.type=caffeine` |
| **Web framework** | Spring MVC (Servlet) | [adapters-inbound-rest/pom.xml](../../adapters-inbound-rest/pom.xml): `spring-boot-starter-web`; [bootstrap/pom.xml](../../bootstrap/pom.xml): WAR packaging, `spring-boot-starter-tomcat` as provided |
| **Bean validation** | Spring Boot Starter Validation | [adapters-inbound-rest/pom.xml](../../adapters-inbound-rest/pom.xml): `spring-boot-starter-validation` |
| **Testing: Unit** | JUnit 5 (via `spring-boot-starter-test`) | Root [pom.xml](../../pom.xml) shared dependency; Surefire plugin `3.1.2` includes `**/*Test.java` |
| **Testing: Integration** | REST-Assured 5.4.0, Testcontainers 1.21.4 | [bootstrap/pom.xml](../../bootstrap/pom.xml): `rest-assured`, `testcontainers`, `testcontainers:mysql`, `testcontainers:junit-jupiter` |
| **Testing: Architecture** | ArchUnit 1.3.0 | [bootstrap/pom.xml](../../bootstrap/pom.xml): `archunit-junit5`; [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) |
| **Coverage** | JaCoCo 0.8.10 | Root [pom.xml](../../pom.xml): jacoco-maven-plugin; [bootstrap/pom.xml](../../bootstrap/pom.xml): `report-aggregate` goal |
| **Static analysis** | SonarCloud | [sonar-project.properties](../../sonar-project.properties); [CI_SETUP.md](../../CI_SETUP.md); README badges linking to `sonarcloud.io` |
| **CI/CD** | GitHub Actions | [.github/workflows/build.yml](../../.github/workflows/build.yml): triggers on push/PR to `main`, runs `./mvnw clean verify` |
| **Packaging** | WAR (bootstrap module) | [bootstrap/pom.xml](../../bootstrap/pom.xml): `<packaging>war</packaging>` |
| **External APIs** | Finnhub, AlphaVantage (stock price providers) | [application.properties](../../bootstrap/src/main/resources/application.properties): `finhub.api.url`, `alphaVantage.api.base-url`; [FinhubStockPriceAdapter.java](../../adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/FinhubStockPriceAdapter.java), [AlphaVantageStockPriceAdapter.java](../../adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/AlphaVantageStockPriceAdapter.java) |
| **Diagrams** | PlantUML (27 `.puml` files) | [doc/tutorial/](../tutorial/) (`*/diagrams/*.puml`); render scripts under [scripts/](../../scripts/) |

---

## 5. Architectural style

### Hexagonal Architecture (Ports and Adapters)

HexaStock implements Hexagonal Architecture with strict dependency inversion. The architecture is enforced at three levels: Maven module boundaries, package naming conventions, and automated ArchUnit tests.

**Evidence:** Module structure in root [pom.xml](../../pom.xml); [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java); [CONTRIBUTING.md](../../CONTRIBUTING.md) ("All dependencies point inward toward the domain").

### Module structure

The project is decomposed into six Maven modules:

| Module | Artifact ID | Architectural role |
|--------|------------|-------------------|
| `domain` | `hexastock-domain` | Domain model: aggregates, entities, value objects, domain exceptions. Zero framework dependencies. |
| `application` | `hexastock-application` | Application layer: input ports (use cases), output ports, application services. Minimal dependency on `spring-tx` for `@Transactional`. |
| `adapters-inbound-rest` | `hexastock-adapters-inbound-rest` | Primary (driving) adapter: REST controllers, DTOs, exception handler. Depends on Spring Web, Validation, SpringDoc. |
| `adapters-outbound-persistence-jpa` | `hexastock-adapters-outbound-persistence-jpa` | Secondary (driven) adapter: JPA entities, mappers, Spring Data repositories. Depends on Spring Data JPA, MySQL connector. |
| `adapters-outbound-market` | `hexastock-adapters-outbound-market` | Secondary (driven) adapter: external stock price API clients (Finnhub, AlphaVantage, mock). Depends on Spring Web, Cache, Caffeine. |
| `bootstrap` | `hexastock-bootstrap` | Composition root: Spring Boot entry point, bean wiring, Spring profiles, integration tests. Depends on all other modules. |

**Evidence:** Each module's `pom.xml` and its `<description>` tag; dependency declarations.

### Dependency direction

Dependencies flow strictly inward:

```
Adapters  -->  Application (ports)  -->  Domain
                    ^                       ^
                    |                       |
              Bootstrap (wires everything)
```

- **Domain** depends on nothing external. No Spring, no JPA, no framework annotations.
- **Application** depends on Domain only. The sole Spring dependency is `spring-tx` for `@Transactional`, explicitly justified in [application/pom.xml](../../application/pom.xml): "minimal Spring dependency justified to preserve existing behavior".
- **Inbound REST adapter** depends on Application (ports) and Spring Web.
- **Outbound adapters** depend on Application (ports) and their respective infrastructure libraries.
- **Adapters do not depend on each other.** ArchUnit tests enforce this: `restDoesNotDependOnPersistence()`, `outboundDoesNotDependOnInbound()`.

**Evidence:** [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) (6 ArchUnit rules); each module's `pom.xml`.

### Package naming conventions

| Layer | Package pattern |
|-------|----------------|
| Domain | `cat.gencat.agaur.hexastock.model.*` |
| Application | `cat.gencat.agaur.hexastock.application.*` |
| Inbound adapters | `cat.gencat.agaur.hexastock.adapter.in.*` |
| Outbound adapters | `cat.gencat.agaur.hexastock.adapter.out.*` |

ArchUnit rules use these package patterns (`..model..`, `..application..`, `..adapter.in..`, `..adapter.out..`) to verify dependency correctness.

**Evidence:** [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) `resideInAPackage` assertions; source file paths.

### Ports

**Input (primary) ports** - interfaces defining use cases the outside world can invoke:

| Port | Package | Description |
|------|---------|-------------|
| `PortfolioManagementUseCase` | `application.port.in` | Create portfolio, get portfolio, deposit, withdraw |
| `PortfolioStockOperationsUseCase` | `application.port.in` | Buy stock, sell stock |
| `ReportingUseCase` | `application.port.in` | Holdings performance |
| `GetStockPriceUseCase` | `application.port.in` | Fetch current stock price |
| `TransactionUseCase` | `application.port.in` | Transaction history retrieval |

**Output (secondary) ports** - interfaces the application core uses to access infrastructure:

| Port | Package | Description |
|------|---------|-------------|
| `PortfolioPort` | `application.port.out` | Portfolio persistence (CRUD) |
| `TransactionPort` | `application.port.out` | Transaction persistence |
| `StockPriceProviderPort` | `application.port.out` | Fetch live stock price from external provider |

**Evidence:** Source files under [application/src/main/java/.../port/in/](../../application/src/main/java/cat/gencat/agaur/hexastock/application/port/in/) and [application/src/main/java/.../port/out/](../../application/src/main/java/cat/gencat/agaur/hexastock/application/port/out/).

### Adapter wiring

The `bootstrap` module's [SpringAppConfig.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/SpringAppConfig.java) is the composition root. It explicitly constructs application services and wires them to their ports:

```java
@Bean
PortfolioManagementUseCase getPortfolioManagementUseCase() {
    return new PortfolioManagementService(portfolioPort, transactionPort);
}
```

This explicit wiring (rather than annotation-based auto-discovery on service classes) keeps application services free from Spring annotations.

**Evidence:** [SpringAppConfig.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/SpringAppConfig.java).

---

## 6. Domain-Driven Design approach

### Confirmed DDD building blocks

| Building block | Implementation | Evidence |
|---------------|---------------|----------|
| **Aggregate root** | `Portfolio` | Javadoc: "In DDD terms, this is an Aggregate Root"; all state changes to Holdings flow through Portfolio methods |
| **Entity** | `Holding` | Javadoc: "In DDD terms, this is an Entity that belongs to the Portfolio aggregate"; has `HoldingId` identity |
| **Entity** | `Lot` | Javadoc: "In DDD terms, this is an Entity that belongs to the Holding aggregate"; has `LotId` identity |
| **Entity** | `Transaction` | Identified by `TransactionId`; records financial events |
| **Value object** | `Money` | Java `record`; immutable; equality by value; normalised to 2 decimal places |
| **Value object** | `Price` | Java `record`; immutable; must be positive |
| **Value object** | `ShareQuantity` | Java `record`; immutable; non-negative |
| **Value object** | `Ticker` | Java `record`; validated against regex `^[A-Z]{1,5}$` |
| **Value object** | `PortfolioId`, `HoldingId`, `LotId`, `TransactionId` | Java `record`s wrapping identifiers; prevent primitive obsession |
| **Value object** | `SellResult` | Java `record`; captures proceeds, costBasis, profit |
| **Value object** | `HoldingPerformance` | Java `record`; computed performance metrics per ticker |
| **Value object** | `StockPrice` | Java `record`; ticker + price + timestamp |
| **Domain service** | `HoldingPerformanceCalculator` | Stateless calculator operating across aggregate and transaction data |
| **Domain exceptions** | `DomainException`, `InsufficientFundsException`, `InvalidAmountException`, `InvalidQuantityException`, `ConflictQuantityException`, `HoldingNotFoundException`, `InvalidTickerException`, `EntityExistsException`, `ExternalApiException` | Self-validating domain types throwing domain-specific exceptions |

**Evidence:** Source files under [domain/src/main/java/.../model/](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/); each class's Javadoc.

### Aggregate boundary protection

The `Portfolio` aggregate root is the sole entry point for all state changes:

- **Deposits and withdrawals** modify `Portfolio.balance` directly.
- **Buy operations** delegate to `Holding.buy()` through `Portfolio.buy()`, which also validates funds.
- **Sell operations** delegate to `Holding.sell()` (which implements FIFO lot consumption) through `Portfolio.sell()`, which also updates the balance.
- **Holdings** are accessed through a private `Map<Ticker, Holding>` - the `findOrCreateHolding()` method is private.
- **Lots** are managed internally by `Holding` - created during buy, consumed during sell.

No external code directly manipulates Holdings or Lots.

**Evidence:** [Portfolio.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java) - `buy()`, `sell()`, `deposit()`, `withdraw()` methods; [Holding.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java) - `buy()`, `sell()` methods; private `holdings` map.

### FIFO lot accounting

The sell operation implements FIFO accounting within `Holding.sell()`:

1. Iterate lots in insertion order (oldest first)
2. For each lot, take `min(lot.remaining, requestedQuantity)`
3. Accumulate cost basis from each lot's purchase price
4. Reduce each lot's remaining shares
5. Remove fully depleted lots

This business rule is entirely within the domain layer, with no infrastructure dependency.

**Evidence:** `Holding.sell()` method; [sell-stocks.feature](../features/sell-stocks.feature) Gherkin scenarios (US-07.FIFO-1, US-07.FIFO-2); [PortfolioTradingRestIntegrationTest](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioTradingRestIntegrationTest.java).GherkinFifoScenarios test class.

### Ubiquitous language

The codebase consistently uses domain terminology: Portfolio, Holding, Lot, Ticker, ShareQuantity, Money, Price, SellResult, cost basis, proceeds, profit, FIFO. These terms appear in domain classes, Gherkin features, OpenAPI schemas, DTOs, test names, and documentation.

**Evidence:** Cross-referencing class names, Gherkin scenario text, OpenAPI schema names, and test method names.

### Self-validating domain types

Domain value objects reject invalid states at construction time:

- `Money`: null-checks, 2-decimal normalisation
- `Price`: must be strictly positive
- `ShareQuantity`: must be non-negative; `positive()` factory requires > 0
- `Ticker`: must match `^[A-Z]{1,5}$`
- All ID types: must not be null or blank

**Evidence:** Constructor validation in [Money.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Money.java), [Price.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Price.java), [ShareQuantity.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/ShareQuantity.java), [Ticker.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/market/Ticker.java), [PortfolioId.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/PortfolioId.java).

---

## 7. REST API strategy

### Controller structure

Two REST controllers serve as primary (driving) adapters:

| Controller | Path | Responsibility |
|-----------|------|---------------|
| `PortfolioRestController` | `/api/portfolios` | Portfolio CRUD, cash management, stock trading, transactions, holdings |
| `StockRestController` | `/api/stocks` | Stock price lookup |

Controllers depend exclusively on input port interfaces (`*UseCase`), never on application services directly.

**Evidence:** [PortfolioRestController.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java) constructor parameters are all `*UseCase` interfaces; [StockRestController.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/StockRestController.java).

### API model separation

The REST adapter maintains its own DTO layer under `adapter.in.webmodel.*`:

- Request DTOs: `CreatePortfolioDTO`, `DepositRequestDTO`, `WithdrawalRequestDTO`, `PurchaseDTO`, `SaleRequestDTO`
- Response DTOs: `CreatePortfolioResponseDTO`, `PortfolioResponseDTO`, `SaleResponseDTO`, `StockPriceDTO`, `HoldingDTO`, `TransactionDTO`
- Error DTO: `ErrorResponse`

DTOs are converted to/from domain objects at the controller boundary. Response DTOs use static factory methods (e.g. `CreatePortfolioResponseDTO.from(portfolio)`).

**Evidence:** Source files under [adapters-inbound-rest/src/main/java/.../adapter/in/webmodel/](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/webmodel/).

### Error handling

A centralised `@ControllerAdvice` ([ExceptionHandlingAdvice.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/ExceptionHandlingAdvice.java)) translates domain exceptions to RFC 7807 `ProblemDetail` responses:

| Domain exception | HTTP status | Problem title |
|-----------------|-------------|--------------|
| `PortfolioNotFoundException` | 404 | Portfolio Not Found |
| `HoldingNotFoundException` | 404 | Holding Not Found |
| `InvalidAmountException` | 400 | Invalid Amount |
| `InvalidQuantityException` | 400 | Invalid Quantity |
| `InvalidTickerException` | 400 | Invalid Ticker |
| `ConflictQuantityException` | 409 | Conflict Quantity |
| `InsufficientFundsException` | 409 | Insufficient Funds |
| `ExternalApiException` | 503 | External API Error |

**Evidence:** [ExceptionHandlingAdvice.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/ExceptionHandlingAdvice.java); integration tests verifying status codes and ProblemDetail fields.

### Resource creation pattern

Portfolio creation returns HTTP 201 with a `Location` header constructed via `ServletUriComponentsBuilder`.

**Evidence:** [PortfolioRestController.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java) `createPortfolio()` method.

---

## 8. Persistence strategy

### Technology and pattern

Persistence is implemented via Spring Data JPA with Hibernate and MySQL 8.0.32. The persistence adapter is isolated in its own Maven module (`adapters-outbound-persistence-jpa`), activated only when the `jpa` Spring profile is active.

**Evidence:** [JpaPortfolioRepository.java](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java): `@Profile("jpa")`; [adapters-outbound-persistence-jpa/pom.xml](../../adapters-outbound-persistence-jpa/pom.xml).

### Domain-persistence mapping

The persistence adapter maintains a separate JPA entity model:

| Domain class | JPA entity | Mapper |
|-------------|-----------|--------|
| `Portfolio` | `PortfolioJpaEntity` | `PortfolioMapper` |
| `Holding` | `HoldingJpaEntity` | `HoldingMapper` |
| `Lot` | `LotJpaEntity` | `LotMapper` |
| `Transaction` | `TransactionJpaEntity` | `TransactionMapper` |

Mappers are static utility classes that convert between domain objects and JPA entities in both directions. This acts as an anti-corruption layer preserving domain model purity.

**Evidence:** Mapper classes under `adapter.out.persistence.jpa.mapper.*`; JPA entity classes under `adapter.out.persistence.jpa.entity.*`.

### Aggregate persistence

The `PortfolioJpaEntity` maps the entire aggregate:

- `@OneToMany(cascade = ALL, orphanRemoval = true)` for Holdings
- Holdings similarly cascade to Lots
- JPA handles the full aggregate graph in a single save operation

The `addHolding()` method on `Portfolio` is documented as "Persistence-only hook used to reconstitute the aggregate from storage. Not a business operation."

**Evidence:** [PortfolioJpaEntity.java](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/entity/PortfolioJpaEntity.java) JPA annotations; [Holding.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java) `addLot()` Javadoc.

### Concurrency control

The repository uses pessimistic write locking for portfolio reads that precede updates:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

This prevents race conditions during concurrent financial operations on the same portfolio.

**Evidence:** [JpaPortfolioSpringDataRepository.java](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioSpringDataRepository.java); [CONCURRENCY-PESSIMISTIC-LOCKING.md](../tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md).

### Transactional boundaries

Application services are annotated with `@Transactional`, establishing the unit of work at the use-case level:

```java
@Transactional
public class PortfolioManagementService implements PortfolioManagementUseCase { ... }

@Transactional
public class PortfolioStockOperationsService implements PortfolioStockOperationsUseCase { ... }
```

**Evidence:** [PortfolioManagementService.java](../../application/src/main/java/cat/gencat/agaur/hexastock/application/PortfolioManagementService.java), [PortfolioStockOperationsService.java](../../application/src/main/java/cat/gencat/agaur/hexastock/application/PortfolioStockOperationsService.java).

### Schema management

- **Production:** `spring.jpa.hibernate.ddl-auto=create-drop` (auto-generated schema)
- **Test:** `spring.jpa.hibernate.ddl-auto=create` (avoids `create-drop` issues in CI when multiple Spring contexts share a Testcontainers instance)

*Architectural interpretation based on repository evidence:* The use of `create-drop` in production properties indicates the project is not yet in a production deployment phase. A production deployment would typically use a migration tool such as Flyway or Liquibase.

**Evidence:** [application.properties](../../bootstrap/src/main/resources/application.properties), [application-test.properties](../../bootstrap/src/test/resources/application-test.properties) (with detailed comment explaining the `create` choice).

---

## 9. Testing strategy

### Test classification

The project uses a custom `TestLevel` enum and `@SpecificationRef` annotation to classify and trace tests:

```java
public enum TestLevel {
    DOMAIN,       // Pure domain test - no infrastructure, no framework
    INTEGRATION   // Full-stack integration test - HTTP through to persistence
}
```

**Evidence:** [domain/src/test/java/.../TestLevel.java](../../domain/src/test/java/cat/gencat/agaur/hexastock/TestLevel.java), [domain/src/test/java/.../SpecificationRef.java](../../domain/src/test/java/cat/gencat/agaur/hexastock/SpecificationRef.java).

### Test levels

| Level | Runner | Scope | Location | Convention |
|-------|--------|-------|----------|------------|
| **Domain unit tests** | Surefire | Pure domain logic, no Spring context | [domain/src/test/](../../domain/src/test/) | `*Test.java` |
| **Application unit tests** | Surefire | Application services with mocked ports | [application/src/test/](../../application/src/test/) | `*Test.java` |
| **Integration tests** | Surefire (in bootstrap) | Full HTTP stack with real database | [bootstrap/src/test/](../../bootstrap/src/test/) | `*Test.java` (extends `AbstractPortfolioRestIntegrationTest`) |
| **Architecture tests** | Surefire (in bootstrap) | ArchUnit structural validation | [bootstrap/src/test/](../../bootstrap/src/test/) | [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) |

**Evidence:** Test source locations; Surefire and Failsafe plugin configuration in root [pom.xml](../../pom.xml).

### Integration test infrastructure

Integration tests in the `bootstrap` module use:

- **Testcontainers** with MySQL 8.0.32 via the JDBC URL convention (`jdbc:tc:mysql:8.0.32:///testdb`)
- **REST-Assured** for HTTP-level assertions
- **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** for full application context
- **`@ActiveProfiles({"test", "jpa", "mockfinhub"})`** activating test config, JPA persistence, and mocked stock prices

The `AbstractPortfolioRestIntegrationTest` provides reusable helper methods for common operations (create portfolio, deposit, buy, sell, etc.).

**Evidence:** [AbstractPortfolioRestIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java); [application-test.properties](../../bootstrap/src/test/resources/application-test.properties).

### Deterministic test fixtures

The `PortfolioTradingRestIntegrationTest` uses a `@TestConfiguration` with a `FixedPriceStockPriceAdapter` that provides deterministic, controllable stock prices. This enables precise FIFO sell scenario testing with exact expected values.

**Evidence:** `PortfolioTradingRestIntegrationTest.FixedPriceConfiguration` inner class; `GherkinFifoScenarios` nested test class.

### Specification traceability

Tests are annotated with `@SpecificationRef` linking to Gherkin scenarios:

```java
@SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.INTEGRATION, feature = "sell-stocks.feature")
void sellSharesConsumedFromSingleLot_FIFOGherkinScenario() { ... }
```

This creates an explicit, navigable traceability chain:

```
Requirement -> Scenario (.feature) -> Test (JUnit) -> Code
```

The annotation is `@Repeatable` (via `@SpecificationRefs`), allowing tests to reference multiple scenarios.

**Evidence:** [SpecificationRef.java](../../domain/src/test/java/cat/gencat/agaur/hexastock/SpecificationRef.java); usage across [PortfolioLifecycleRestIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioLifecycleRestIntegrationTest.java), [PortfolioTradingRestIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioTradingRestIntegrationTest.java), domain tests.

### Architecture fitness tests

[HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) enforces six rules via ArchUnit:

1. Domain does not depend on Application
2. Domain does not depend on Adapters
3. Domain does not depend on Spring
4. Application does not depend on Adapters
5. Inbound REST adapter does not depend on outbound persistence adapter
6. Outbound adapters do not depend on inbound REST adapter

**Evidence:** [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) - 6 `@Test` methods in 3 `@Nested` groups.

### Quality expectations

From [CONTRIBUTING.md](../../CONTRIBUTING.md):

- All tests must pass (`./mvnw clean verify`)
- Coverage must not regress (target >90% line coverage, JaCoCo)
- No new SonarQube issues
- Target AAA maintainability rating in Sonar

**Evidence:** [CONTRIBUTING.md](../../CONTRIBUTING.md) "Code Quality Expectations" section.

---

## 10. Build, quality, and governance

### Maven multi-module structure

The root POM ([pom.xml](../../pom.xml)) defines:

- Parent: `spring-boot-starter-parent:3.5.0`
- 6 child modules
- Shared properties: Java 21, library versions
- Shared test dependency: `spring-boot-starter-test`
- Plugin management: Surefire `3.1.2`, Failsafe `3.1.2`, JaCoCo `0.8.10`

The `domain` module produces a `test-jar` so that shared test utilities (`@SpecificationRef`, `TestLevel`) can be reused across modules.

**Evidence:** Root [pom.xml](../../pom.xml); [domain/pom.xml](../../domain/pom.xml) `maven-jar-plugin` with `test-jar` goal.

### Surefire and Failsafe

- **Surefire** runs unit tests (`**/*Test.java`, `**/*Tests.java`)
- **Failsafe** runs integration tests (`**/*IT.java`) with explicit execution bindings in [bootstrap/pom.xml](../../bootstrap/pom.xml)

**Evidence:** Root [pom.xml](../../pom.xml) plugin management; [bootstrap/pom.xml](../../bootstrap/pom.xml) Failsafe execution.

### JaCoCo coverage

- Each module instruments code via `prepare-agent`
- The `bootstrap` module aggregates coverage via `report-aggregate` during the `verify` phase

**Evidence:** Root [pom.xml](../../pom.xml) JaCoCo `prepare-agent`; [bootstrap/pom.xml](../../bootstrap/pom.xml) JaCoCo `report-aggregate`.

### SonarCloud integration

[sonar-project.properties](../../sonar-project.properties) configures:

- Organisation, project key, and host URL via environment variables
- Source encoding: UTF-8
- Java binaries and test binaries paths
- JaCoCo XML report path: `target/site/jacoco-merged/jacoco.xml`

README badges link to SonarCloud for Quality Gate, Security Rating, Reliability Rating, and Maintainability Rating.

**Evidence:** [sonar-project.properties](../../sonar-project.properties); [README.md](../../README.md) badge URLs; [CI_SETUP.md](../../CI_SETUP.md).

### GitHub Actions CI

[.github/workflows/build.yml](../../.github/workflows/build.yml) defines a single job `build-and-test`:

1. Checkout with full history (`fetch-depth: 0`)
2. Set up JDK 21 (Temurin distribution, Maven cache)
3. Docker version diagnostics
4. Run `./mvnw -B -U -ntp clean verify`
5. On failure: print surefire/failsafe reports
6. Upload test results as artifacts
7. Upload JaCoCo reports as artifacts

Triggers: push to `main`, pull requests targeting `main`.

**Evidence:** [.github/workflows/build.yml](../../.github/workflows/build.yml).

### Branch protection

[CI_SETUP.md](../../CI_SETUP.md) recommends configuring branch protection on `main` with required status checks (`build-and-test`, SonarCloud Quality Gate). Cannot confirm from repository evidence whether this is currently active on GitHub.

---

## 11. Traceability

### Specification-to-code traceability matrix

| Specification source | Traced by | Verified by | Implemented in |
|---------------------|-----------|-------------|----------------|
| [create-portfolio.feature](../features/create-portfolio.feature) (US-01) | `@SpecificationRef("US-01.AC-1")` | `PortfolioLifecycleRestIntegrationTest` | `PortfolioManagementService.createPortfolio()` |
| [get-portfolio.feature](../features/get-portfolio.feature) (US-02) | `@SpecificationRef("US-02.AC-1")` | `PortfolioLifecycleRestIntegrationTest` | `PortfolioManagementService.getPortfolio()` |
| [list-portfolios.feature](../features/list-portfolios.feature) (US-03) | `@SpecificationRef("US-03.AC-1")` | `PortfolioLifecycleRestIntegrationTest` | `PortfolioManagementService.getAllPortfolios()` |
| [deposit-funds.feature](../features/deposit-funds.feature) (US-04) | `@SpecificationRef("US-04.AC-*")` | `PortfolioLifecycleRestIntegrationTest` | `Portfolio.deposit()` |
| [withdraw-funds.feature](../features/withdraw-funds.feature) (US-05) | `@SpecificationRef("US-05.AC-*")` | `PortfolioLifecycleRestIntegrationTest` | `Portfolio.withdraw()` |
| [buy-stocks.feature](../features/buy-stocks.feature) (US-06) | `@SpecificationRef("US-06.AC-*")` | `PortfolioTradingRestIntegrationTest` | `Portfolio.buy()`, `Holding.buy()`, `Lot.create()` |
| [sell-stocks.feature](../features/sell-stocks.feature) (US-07) | `@SpecificationRef("US-07.FIFO-*")` | `PortfolioTradingRestIntegrationTest.GherkinFifoScenarios` | `Portfolio.sell()`, `Holding.sell()`, `Lot.reduce()` |
| [get-holdings-performance.feature](../features/get-holdings-performance.feature) (US-09) | `@SpecificationRef("US-09.AC-*")` | `PortfolioLifecycleRestIntegrationTest` | `ReportingService`, `HoldingPerformanceCalculator` |
| [openapi.yaml](../openapi.yaml) | Integration tests validate HTTP contracts | `*RestIntegrationTest` classes | REST controllers + DTOs |
| PlantUML diagrams | ArchUnit tests validate structure | [HexagonalArchitectureTest](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) | Module boundaries, package structure |

### Architecture enforcement chain

```
PlantUML diagrams (visualise intent)
        |
        v
ArchUnit tests (enforce structure)
        |
        v
Maven modules (codify boundaries)
        |
        v
Package naming (enable detection)
```

---

## 12. Known constraints and explicit trade-offs

### Single-currency assumption

The domain assumes all monetary values are in USD. [Money.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Money.java) Javadoc states: "Single-Currency Assumption: This implementation assumes all monetary values are in USD."

**Evidence:** [Money.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Money.java) class-level Javadoc.

### Schema management via DDL auto-generation

The project uses Hibernate DDL auto-generation (`create-drop` / `create`) rather than a versioned migration tool. This is appropriate for an educational/development-phase project but would need to change for production deployment.

*Architectural interpretation based on repository evidence:* The absence of Flyway or Liquibase, combined with `create-drop` in production properties, strongly suggests the project is not intended for production data persistence across restarts.

### WAR packaging

The bootstrap module is packaged as a WAR with `spring-boot-starter-tomcat` as `provided` scope, suggesting deployment to an external servlet container is the intended deployment model.

**Evidence:** [bootstrap/pom.xml](../../bootstrap/pom.xml): `<packaging>war</packaging>`, `spring-boot-starter-tomcat` scope `provided`.

### Minimal `@Transactional` dependency in application layer

The application layer introduces a deliberate, minimal dependency on `spring-tx` for `@Transactional` support. The [application/pom.xml](../../application/pom.xml) explicitly justifies this: "minimal Spring dependency justified to preserve existing behavior".

*Architectural interpretation based on repository evidence:* This is a pragmatic trade-off. Pure hexagonal architecture would push transaction management to the adapter layer, but `@Transactional` on application services is a widely accepted compromise that keeps transaction boundaries aligned with use-case boundaries.

### Stock price mock for tests

Integration tests use a `mockfinhub` profile to avoid requiring real API keys. The `MockFinhubStockPriceAdapter` and `FixedPriceStockPriceAdapter` demonstrate the interchangeability of outbound adapters.

**Evidence:** [AbstractPortfolioRestIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java): `@ActiveProfiles({"test", "jpa", "mockfinhub"})`; [MockFinhubStockPriceAdapter.java](../../adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/MockFinhubStockPriceAdapter.java); `FixedPriceConfiguration`.

---

## 13. Open questions and decisions not yet explicit

The following areas suggest implicit decisions that are not yet formally documented:

1. **Authentication and authorisation** - No security layer is present. Cannot confirm from repository evidence whether this is intentional scope limitation or pending work.

2. **Event sourcing and domain events** - The project records transactions as a side effect of operations but does not use domain events for decoupling. Cannot confirm whether this was a deliberate decision.

3. **CQRS separation** - The [doc/tutorial/watchlists/](../tutorial/watchlists/) diagrams reference CQRS patterns, but the current implementation uses the same model for reads and writes. The boundary between current state and future direction is unclear.

4. **Versioned database migrations** - The current DDL auto-generation approach has no documented migration strategy for schema evolution.

5. **API versioning** - No API versioning strategy is documented or implemented. All endpoints are under `/api/`.

6. **Rate limiting and resilience** - External API calls (Finnhub, AlphaVantage) have no documented circuit breaker or retry strategy.

7. **Settlement mechanics** - [fifo-settlement-selling.feature](../features/fifo-settlement-selling.feature) specifies settlement-aware selling with T+2 rules and lot reservation, but the current domain model does not yet implement settlement dates or lot reservation. This appears to be a forward-looking specification.

8. **Deployment strategy** - The WAR packaging and Docker Compose for MySQL suggest a deployment model, but no Dockerfile for the application itself or Kubernetes manifests exist.

These items are candidates for the ADR backlog (see [doc/architecture/adr/README.md](./adr/README.md)).

---

## 14. References

Key repository artefacts used as evidence:

### Build and configuration
- [pom.xml](../../pom.xml) - Root POM with module definitions and shared dependencies
- [domain/pom.xml](../../domain/pom.xml) - Domain module (zero framework dependencies)
- [application/pom.xml](../../application/pom.xml) - Application module with justified `spring-tx` dependency
- [adapters-inbound-rest/pom.xml](../../adapters-inbound-rest/pom.xml) - REST adapter dependencies
- [adapters-outbound-persistence-jpa/pom.xml](../../adapters-outbound-persistence-jpa/pom.xml) - JPA adapter dependencies
- [adapters-outbound-market/pom.xml](../../adapters-outbound-market/pom.xml) - Market adapter dependencies
- [bootstrap/pom.xml](../../bootstrap/pom.xml) - Composition root with test infrastructure
- [docker-compose.yml](../../docker-compose.yml) - MySQL container definition
- [sonar-project.properties](../../sonar-project.properties) - SonarCloud configuration
- [.github/workflows/build.yml](../../.github/workflows/build.yml) - CI pipeline

### Domain model
- [Portfolio.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java) - Aggregate root
- [Holding.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java) - Entity with FIFO sell logic
- [Lot.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Lot.java) - Entity tracking purchase lots
- [Money.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/money/Money.java) - Value object
- [Ticker.java](../../domain/src/main/java/cat/gencat/agaur/hexastock/model/market/Ticker.java) - Value object with validation

### Architecture enforcement
- [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java) - ArchUnit fitness tests
- [SpringAppConfig.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/SpringAppConfig.java) - Composition root

### Ports and adapters
- [PortfolioManagementUseCase.java](../../application/src/main/java/cat/gencat/agaur/hexastock/application/port/in/PortfolioManagementUseCase.java) - Input port
- [PortfolioPort.java](../../application/src/main/java/cat/gencat/agaur/hexastock/application/port/out/PortfolioPort.java) - Output port
- [PortfolioRestController.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java) - Primary adapter
- [JpaPortfolioRepository.java](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java) - Secondary adapter
- [ExceptionHandlingAdvice.java](../../adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/adapter/in/ExceptionHandlingAdvice.java) - Error mapping

### Test infrastructure
- [SpecificationRef.java](../../domain/src/test/java/cat/gencat/agaur/hexastock/SpecificationRef.java) - Traceability annotation
- [AbstractPortfolioRestIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java) - Integration test base
- [application-test.properties](../../bootstrap/src/test/resources/application-test.properties) - Testcontainers JDBC config

### Specifications
- [openapi.yaml](../openapi.yaml) - API contract
- [sell-stocks.feature](../features/sell-stocks.feature) - FIFO sell specification
- [buy-stocks.feature](../features/buy-stocks.feature) - Buy specification
- [create-portfolio.feature](../features/create-portfolio.feature) - Portfolio creation specification

### Documentation
- [README.md](../../README.md) - Project overview and setup
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Contribution guidelines and architectural principles
- [CI_SETUP.md](../../CI_SETUP.md) - CI/CD setup instructions
