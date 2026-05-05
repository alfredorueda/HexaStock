# Tutorial — Requirement Traceability Architecture

## Overview

This repository implements a **requirement traceability model** that creates an explicit, navigable chain from functional requirements to executable code:

```
Specification  →  Gherkin (.feature)  →  Tests (JUnit)  →  Code
```

Every acceptance criterion in the [functional specification](../stock-portfolio-api-specification.md) is linked to a Gherkin scenario, which is in turn referenced by one or more Java tests via the `@SpecificationRef` annotation. This architecture ensures that:

- Every requirement is verifiable and verified.
- Every test can be traced back to the business behaviour it validates.
- Changes to requirements are immediately visible in the test layer.

---

## The Sell Stocks Pilot (US-07) as the Reference Use Case

The **Sell Stocks use case (US-07)** served as the **reference pilot** for this traceability architecture. It was chosen because it involves the most complex domain logic in the system — FIFO lot consumption with precise financial calculations — making it an ideal candidate for demonstrating the full traceability chain.

The pilot established the patterns that were then extended to all 10 use cases:

1. **Gherkin specification** — [`doc/features/sell-stocks.feature`](../features/sell-stocks.feature) defines two detailed FIFO scenarios with exact numeric expectations.
2. **Domain-level tests** — `HoldingTest` and `PortfolioTest` verify FIFO mechanics and financial calculations, annotated with `@SpecificationRef`.
3. **Integration-level tests** — `PortfolioTradingRestIntegrationTest` and `PortfolioErrorHandlingRestIntegrationTest` verify REST endpoint behaviour end-to-end.
4. **Inline Gherkin in the specification** — The functional specification document embeds the Gherkin scenarios directly within the US-07 section, with a canonical source reference back to the `.feature` file.

The Sell Stocks tutorials provide a deeper walkthrough:

- [Sell Stock Tutorial](sellStocks/SELL-STOCK-TUTORIAL.md) — end-to-end tutorial covering the hexagonal architecture layers
- [Sell Stock Domain Tutorial](sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md) — focused tutorial on the domain model and FIFO logic

---

## Traceability Chain Explained

### 1. Functional Specification → Gherkin

The [functional specification](../stock-portfolio-api-specification.md) defines acceptance criteria for each use case (US-01 through US-10) in a tabular Given/When/Then format. Each use case section now includes an embedded **Gherkin block** showing the exact scenarios, plus a **Canonical source** reference pointing to the standalone `.feature` file.

### 2. Gherkin Feature Files

The [`doc/features/`](../features/) directory contains 10 `.feature` files — one per use case:

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

Each feature file includes:
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

All identifiers follow a consistent scheme:

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

## Summary

This traceability architecture ensures that every functional requirement is:

1. **Specified** in the functional specification document.
2. **Described** in Gherkin scenarios with stable identifiers [North, 2006].
3. **Verified** at the appropriate level (domain and/or integration).
4. **Linked** via `@SpecificationRef` annotations.

The resulting chain is auditable: each acceptance criterion maps to one or more executable tests, and each test declares, through its annotation, which criterion it verifies. This makes requirements coverage inspectable by static analysis and makes the impact of a specification change visible in the test layer without any external tooling.

## References

- North, Dan. \"Introducing BDD.\" *Better Software*, March 2006. https://dannorth.net/introducing-bdd/
- OpenAPI Initiative. *OpenAPI Specification, Version 3.0.3.* 2021. https://spec.openapis.org/oas/v3.0.3

