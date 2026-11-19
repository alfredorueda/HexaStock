# Table of contents

* [Welcome to HexaStock](doc/BOOK-HOME.md)
* [Start Here: Your First Steps](doc/learning/START-HERE.md)

## Reference Use Case

* [Sell Stock Deep Dive](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)
  * [1. Specify the sale](doc/tutorial/sellStocks/SELL-STOCK-01-BEHAVIOUR.md)
  * [2. Model and test the aggregate](doc/tutorial/sellStocks/SELL-STOCK-02-DOMAIN-TEST.md)
  * [3. Follow the FIFO algorithm](doc/tutorial/sellStocks/SELL-STOCK-03-FIFO.md)
  * [4. Cross the HTTP boundary](doc/tutorial/sellStocks/SELL-STOCK-04-HTTP-AND-PORTS.md)
  * [5. Coordinate the use case](doc/tutorial/sellStocks/SELL-STOCK-05-ORCHESTRATION.md)
  * [6. Save and reload the aggregate](doc/tutorial/sellStocks/SELL-STOCK-06-PERSISTENCE.md)
  * [7. Protect consistency](doc/tutorial/sellStocks/SELL-STOCK-07-CONSISTENCY.md)
  * [8. Trace failures](doc/tutorial/sellStocks/SELL-STOCK-08-ERRORS.md)
  * [9. Verify the complete flow](doc/tutorial/sellStocks/SELL-STOCK-09-VERIFICATION.md)
  * [Exercises](doc/tutorial/sellStocks/SELL-STOCK-EXERCISES.md)
  * [References and Further Reading](doc/tutorial/sellStocks/SELL-STOCK-REFERENCE.md)
  * [Ubiquitous Language](doc/tutorial/sellStocks/UBIQUITOUS-LANGUAGE.md)
  * [Architecture Reference Diagrams](doc/tutorial/sellStocks/HEXAGONAL-ARCHITECTURE-REFERENCE-DIAGRAMS.md)
* [HexaStock Project Overview](doc/tutorial/sellStocks/HEXASTOCK-PROJECT-OVERVIEW.md)
* [Sell Stock — Domain Layer Only](doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md)

## Architecture and Design

* [Technical architecture](doc/architecture/TECHNICAL-ARCHITECTURE-SPECIFICATION.md)
  * [1. Map the modules and ports](doc/architecture/ARCHITECTURE-MAP.md)
  * [2. Understand runtime guarantees](doc/architecture/RUNTIME-AND-VERIFICATION.md)
  * [Architecture decisions](doc/architecture/adr/README.md)
    * [ADR-001: Use Java 21 as the baseline language and runtime](doc/architecture/adr/ADR-001-use-java-21-as-baseline-runtime.md)
    * [ADR-002: Use Spring Boot 3 as the application framework](doc/architecture/adr/ADR-002-use-spring-boot-3-as-application-framework.md)
    * [ADR-003: Adopt Hexagonal Architecture with strict dependency inversion](doc/architecture/adr/ADR-003-adopt-hexagonal-architecture.md)
    * [ADR-004: Model the core business using Domain-Driven Design with a rich domain model](doc/architecture/adr/ADR-004-rich-domain-model-with-ddd.md)
    * [ADR-005: Expose functionality via REST API with RFC 7807 error responses](doc/architecture/adr/ADR-005-rest-api-with-rfc7807-errors.md)
    * [ADR-006: Use multi-module Maven build to enforce architectural boundaries](doc/architecture/adr/ADR-006-multi-module-maven-build.md)
    * [ADR-007: Keep domain production code free of frameworks](doc/architecture/adr/ADR-007-domain-module-zero-framework-dependencies.md)
    * [ADR-008: Use MySQL with Spring Data JPA and explicit domain-persistence mapping](doc/architecture/adr/ADR-008-mysql-jpa-with-domain-persistence-mapping.md)
    * [ADR-009: Use Testcontainers for integration tests instead of H2](doc/architecture/adr/ADR-009-testcontainers-for-integration-tests.md)
    * [ADR-010: Enforce architecture rules with ArchUnit fitness tests](doc/architecture/adr/ADR-010-archunit-architecture-fitness-tests.md)
    * [ADR-011: Implement FIFO lot accounting for stock sales in the domain](doc/architecture/adr/ADR-011-fifo-lot-accounting-for-stock-sales.md)
    * [ADR-012: Coordinate JPA portfolio access with a pessimistic lock](doc/architecture/adr/ADR-012-pessimistic-locking-for-aggregate-consistency.md)
    * [ADR-013: Cache stock prices with Caffeine](doc/architecture/adr/ADR-013-cache-stock-prices-with-caffeine.md)
    * [ADR-014: Use @SpecificationRef for specification-to-test traceability](doc/architecture/adr/ADR-014-specification-ref-traceability-annotation.md)
    * [ADR-015: Wire application services explicitly via @Configuration beans](doc/architecture/adr/ADR-015-explicit-bean-wiring-via-configuration.md)
    * [ADR-016: Detect MongoDB conflicts and retry the use case](doc/architecture/adr/ADR-016-optimistic-locking-with-retry-for-mongodb-adapter.md)

* [Compare DDD, Hexagonal, Onion, and Clean](doc/architecture/DDD-HEXAGONAL-ONION-CLEAN-COMPARATIVE-STUDY.md)
* [Teaching program](doc/architecture/ARCHITECTURE-TRANSFORMATION-PROGRAM.md)
  * [Program overview](doc/architecture/ARCHITECTURE-TRANSFORMATION-PROGRAM-EXECUTIVE-SUMMARY.md)

* [Rich and anemic domain models](doc/tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md)
  * [1. Inspect the current model](doc/tutorial/richVsAnemicDomainModel/01-CURRENT-MODEL.md)
  * [2. Diagnose rule drift](doc/tutorial/richVsAnemicDomainModel/02-RULE-DRIFT.md)
  * [3. Specify a settlement extension](doc/tutorial/richVsAnemicDomainModel/03-SETTLEMENT-EXERCISE.md)
  * [4. Compare designs fairly](doc/tutorial/richVsAnemicDomainModel/04-VERIFICATION.md)
* [Dependency Inversion in Stock Selling](doc/tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md)
  * [Wiring and testing](doc/tutorial/dependencyInversion/WIRING-AND-TESTING.md)
* [DDD Portfolio and Transactions](<doc/DDD Portfolio and Transactions.md>)
  * [1. Choose a consistency boundary](doc/architecture/aggregates/01-CONSISTENCY-BOUNDARY.md)
  * [2. Query history separately](doc/architecture/aggregates/02-HISTORY-AND-SCALE.md)
  * [3. Compare alternatives](doc/architecture/aggregates/03-ALTERNATIVES.md)
* [Remove Lots with Zero Remaining Quantity](<doc/Remove Lots with Zero Remaining Quantity from Portfolio Aggregate.md>)

## Supporting Chapters

* [Testing strategy](doc/tutorial/TESTING-STRATEGY.md)
  * [1. Rules and orchestration](doc/tutorial/testing/01-RULES-AND-ORCHESTRATION.md)
  * [2. Adapter contracts](doc/tutorial/testing/02-ADAPTER-CONTRACTS.md)
  * [3. Integration and architecture](doc/tutorial/testing/03-INTEGRATION-AND-ARCHITECTURE.md)
* [Requirement traceability](doc/tutorial/README.md)

* [Specification-Driven Development with AI](doc/tutorial/specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md)
  * [1. Specify a small change](doc/tutorial/specificationDrivenAI/01-SPECIFY-A-SMALL-CHANGE.md)
  * [2. Reconcile specifications](doc/tutorial/specificationDrivenAI/02-RECONCILE-SPECIFICATIONS.md)
  * [3. Evolve and evaluate](doc/tutorial/specificationDrivenAI/03-EVOLVE-AND-EVALUATE.md)
* [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md)
  * [1. Coordinate JPA writers](doc/tutorial/concurrency/JPA-LOCKING.md)
  * [2. Retry MongoDB conflicts](doc/tutorial/concurrency/MONGODB-RETRIES.md)
  * [MongoDB adapter reference](doc/mongodb-adapter-optimistic-write-and-retry.md)
* [Holdings performance and scale](doc/tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md)
  * [1. Calculate one report](doc/tutorial/portfolioReporting/01-CURRENT-CALCULATION.md)
  * [2. Measure the request](doc/tutorial/portfolioReporting/02-MEASUREMENT.md)
  * [3. Compare query alternatives](doc/tutorial/portfolioReporting/03-QUERY-ALTERNATIVES.md)
  * [4. Maintain a projection](doc/tutorial/portfolioReporting/04-SNAPSHOTS.md)
* [Watchlists and Market Sentinel](doc/tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md)
  * [1. Model watchlist commands](doc/tutorial/watchlists/01-WATCHLIST-COMMANDS.md)
  * [2. Detect threshold matches](doc/tutorial/watchlists/02-DETECTION-AND-PORTS.md)
  * [3. Persist and verify](doc/tutorial/watchlists/03-PERSISTENCE-AND-TESTS.md)
  * [4. Extend alert behavior](doc/tutorial/watchlists/04-PROGRESSIVE-EXTENSIONS.md)
  * [Review the proposed design](doc/tutorial/watchlists/MARKET-SENTINEL-ARCHITECTURAL-REVIEW.md)
* [Lot Selection Strategies — DDD Hexagonal Exercise](doc/tutorial/DDD-Hexagonal-exercise.md)

## Technical Review

* [Review the domain and architecture](doc/domain-review/DDD-REVIEW-PERSONAL-INVESTMENT-PORTFOLIO.md)
  * [1. Inspect invariants and limits](doc/domain-review/01-INVARIANTS-AND-LIMITS.md)
  * [2. Compare model alternatives](doc/domain-review/02-MODEL-ALTERNATIVES.md)
  * [3. Plan runtime improvements](doc/domain-review/domain-model-constructive-strategy-review.md)
  * [4. Measure aggregate size](doc/domain-review/AGGREGATE-SIZE-GUIDELINES.md)

## Specifications

* [Stock Portfolio API Specification](doc/stock-portfolio-api-specification.md)
  * [Errors and validation order](doc/api/ERRORS.md)
  * [US-01: Create Portfolio](doc/api/US-01-CREATE-PORTFOLIO.md)
  * [US-02: Get Portfolio](doc/api/US-02-GET-PORTFOLIO.md)
  * [US-03: List All Portfolios](doc/api/US-03-LIST-PORTFOLIOS.md)
  * [US-04: Deposit Funds](doc/api/US-04-DEPOSIT-FUNDS.md)
  * [US-05: Withdraw Funds](doc/api/US-05-WITHDRAW-FUNDS.md)
  * [US-06: Buy Stocks](doc/api/US-06-BUY-STOCKS.md)
  * [US-07: Sell Stocks](doc/api/US-07-SELL-STOCKS.md)
  * [US-08: Get Transaction History](doc/api/US-08-GET-TRANSACTION-HISTORY.md)
  * [US-09: Get Holdings Performance](doc/api/US-09-GET-HOLDINGS-PERFORMANCE.md)
  * [US-10: Get Stock Price](doc/api/US-10-GET-STOCK-PRICE.md)
  * [Behavior and extension exercises](doc/api/BEHAVIOUR-AND-EXTENSIONS.md)

## Project

* [Classroom demonstration](doc/demo/Agenda.md)
* [Maintain diagrams](doc/diagrams/README.md)
* [Contributing](CONTRIBUTING.md)
* [CI: how checks protect main](CI_DISCIPLINE_WALKTHROUGH.md)
  * [Configuration and report locations](CI_SETUP.md)
  * [Classroom demonstration](CI_DEMO_SCRIPT.md)
* [Acknowledgements](doc/ACKNOWLEDGEMENTS.md)

## Legal

* [Licensing](doc/legal/LICENSE.md)
* [Book License — CC BY-NC 4.0](doc/legal/BOOK-LICENSE.md)
