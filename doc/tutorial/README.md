# Tutorial — Requirement Traceability Architecture

## Overview

This repository implements a **requirement traceability model** that creates an explicit, navigable chain from functional requirements to executable code:

```
Specification  →  Gherkin (.feature)  →  Tests (JUnit)  →  Code
```

**Goal:** follow one requirement to the assertions that check it. Start with the [sale requirements](sellStocks/SELL-STOCK-01-BEHAVIOUR.md), then locate the corresponding test.

The [functional specification](../stock-portfolio-api-specification.md), feature files and `@SpecificationRef` annotations provide a traceability convention. They make intended relationships inspectable. They do not enforce complete requirement coverage or automatically detect every inconsistency; review the actual assertions and test results.

---

## The Sell Stocks Pilot (US-07) as the Reference Use Case

The **Sell Stocks use case (US-07)** served as the **reference pilot** for this traceability architecture. It was chosen because it involves the most complex domain logic in the system — FIFO lot consumption with precise financial calculations — making it an ideal candidate for demonstrating the full traceability chain.

The pilot established the patterns that were then extended to all 10 use cases:

1. **Gherkin specification** — [`doc/features/sell-stocks.feature`](../features/sell-stocks.feature) defines two detailed FIFO scenarios with exact numeric expectations.
2. **Domain-level tests** — `HoldingTest` and `PortfolioTest` verify FIFO mechanics and financial calculations, annotated with `@SpecificationRef`.
3. **Integration-level tests** — `PortfolioTradingRestIntegrationTest` and `PortfolioErrorHandlingRestIntegrationTest` verify REST endpoint behaviour end-to-end.
4. **Operation reference** — The US-07 API page links to the canonical `.feature` file and explains its current request, response, and failure contract.

The Sell Stocks tutorials provide a deeper walkthrough:

- [Sell Stock Tutorial](sellStocks/SELL-STOCK-TUTORIAL.md) — nine stages with goals and checkpoints, from specification to end-to-end verification
- [Sell Stock Domain Tutorial](sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md) — focused tutorial on the domain model and FIFO logic

---

## Traceability Chain Explained

### 1. Functional Specification → Gherkin

The [API reference](../stock-portfolio-api-specification.md) provides a separate page for each use case (US-01 through US-10). Each page links to its canonical `.feature` file. Read the feature for the concrete scenarios and the operation page for HTTP shapes and implementation limits.

### 2. Gherkin Feature Files

The [`doc/features/`](../features/) directory contains 15 `.feature` files. The following ten describe baseline use cases:

| Feature File | Use Case | Scenarios |
|---|---|---|
| [`create-portfolio.feature`](../features/create-portfolio.feature) | US-01 | 1 |
| [`get-portfolio.feature`](../features/get-portfolio.feature) | US-02 | 2 |
| [`list-portfolios.feature`](../features/list-portfolios.feature) | US-03 | 2 |
| [`deposit-funds.feature`](../features/deposit-funds.feature) | US-04 | 4 |
| [`withdraw-funds.feature`](../features/withdraw-funds.feature) | US-05 | 6 |
| [`buy-stocks.feature`](../features/buy-stocks.feature) | US-06 | 8 |
| [`sell-stocks.feature`](../features/sell-stocks.feature) | US-07 | 2 (FIFO) |
| [`get-transaction-history.feature`](../features/get-transaction-history.feature) | US-08 | 2 |
| [`get-holdings-performance.feature`](../features/get-holdings-performance.feature) | US-09 | 3 |
| [`get-stock-price.feature`](../features/get-stock-price.feature) | US-10 | 2 |

The five additional files (`fifo-settlement-selling`, `reserved-lot-handling`, `rule-consistency`, `settlement-aware-selling` and `settlement-fees`) specify advanced extensions. Their presence does not mean that settlement is implemented on `main`.

The baseline files include:
- A standard header comment explaining the traceability chain
- A `Feature:` line with the use case identifier (e.g., `US-01`)
- A user-story narrative (`As an... / I want to... / So that...`)
- One or more scenarios with stable identifiers (`US-XX.AC-N`)

These files are **not executed by Cucumber or any BDD framework**. They are specification documents — the tests are the executable layer.

### 3. The `@SpecificationRef` Annotation

The `@SpecificationRef` annotation is the mechanism that links Java tests to Gherkin scenarios:

```java
@SpecificationRef(
    value = "US-07.FIFO-1",           // Scenario identifier
    level = TestLevel.DOMAIN,          // DOMAIN or INTEGRATION
    feature = "sell-stocks.feature"    // Source .feature file
)
```

**Attributes:**

| Attribute | Purpose |
|---|---|
| `value` | The scenario identifier (e.g., `US-04.AC-2`). Matches a scenario ID in the corresponding `.feature` file. |
| `level` | Either `TestLevel.DOMAIN` (unit/domain tests) or `TestLevel.INTEGRATION` (REST integration tests). |
| `feature` | The `.feature` file that contains the referenced scenario (optional for pre-existing annotations). |

The annotation is `@Repeatable`, so a single test method can reference multiple acceptance criteria when it verifies more than one scenario.

### 4. Tests → Code

Tests annotated with `@SpecificationRef` exercise the production code through either:
- **Domain-level tests** — direct invocation of domain entities and services (e.g., `PortfolioTest`, `HoldingTest`, `ReportingServiceTest`, `HoldingPerformanceCalculatorTest`)
- **Integration-level tests** — HTTP requests via RestAssured against the running Spring Boot application (e.g., `PortfolioLifecycleRestIntegrationTest`, `PortfolioTradingRestIntegrationTest`, `PortfolioErrorHandlingRestIntegrationTest`)

---

## Identifier Convention

Baseline acceptance-criterion identifiers use this scheme:

```
US-{NN}.AC-{N}
```

- `US-{NN}` — the use case number (US-01 through US-10)
- `AC-{N}` — the acceptance criterion number within that use case

The sole exception is US-07, which uses `US-07.FIFO-1` and `US-07.FIFO-2` for its detailed FIFO lot consumption scenarios (in addition to `US-07.AC-1` through `US-07.AC-7` for the standard acceptance criteria).

Identifiers are **never renumbered**. If a scenario is removed, its identifier is retired rather than reassigned.

---

## Other Tutorials

This repository includes additional tutorials that complement the traceability architecture:

- [DDD & Hexagonal Architecture Exercise](DDD-Hexagonal-exercise.md) — hands-on exercise exploring the hexagonal architecture pattern
- [Dependency Inversion for Stock Selling](DEPENDENCY-INVERSION-STOCK-SELLING.md) — tutorial on dependency inversion in the context of stock operations
- [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](CONCURRENCY-PESSIMISTIC-LOCKING.md) — tutorial on handling concurrent access to portfolios with pessimistic locking (JPA/MySQL) and optimistic concurrency with retries (MongoDB)

---

## Check one link in the chain

Choose `US-07.FIFO-1`. Find its feature scenario, then search the test sources for the same identifier. Check whether the assertions cover proceeds, cost basis, profit and the remaining lots. A reference without those assertions is incomplete evidence.

`@SpecificationRef` is metadata. The tests execute Java code; the Gherkin is not executed by Cucumber. Keep this distinction when reporting requirement coverage.

## References

- North, Dan. \"Introducing BDD.\" *Better Software*, March 2006. https://dannorth.net/introducing-bdd/
- OpenAPI Initiative. *OpenAPI Specification, Version 3.0.3.* 2021. https://spec.openapis.org/oas/v3.0.3

