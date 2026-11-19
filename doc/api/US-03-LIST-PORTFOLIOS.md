# US-03: List All Portfolios

**Purpose:** Inspect the portfolios in the learning application.

**Endpoint:** `GET /api/portfolios`. The default local base URL is `http://localhost:8081`.

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-03.AC-1:** When portfolios exist, return **200 OK** with an array of their lookup representations. For Alice, Bob and Charlie with balances 1000.00, 2500.00 and 0.00, those values appear in the corresponding entries.
- **US-03.AC-2:** When no portfolios exist, return **200 OK** with `[]`.

## Response example

```json
[
  {
    "id": "portfolio-alice",
    "ownerName": "Alice",
    "balance": 1000.0,
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

## Details and limits

The example array contains one portfolio for readability; IDs shown in examples are placeholders. The endpoint currently has no pagination, owner filter or explicit sort parameter.

Charlie can have zero cash without a zero-value deposit: creation already starts at zero. A deposit of zero is rejected. User-story roles such as investor or administrator express a learning scenario, not an implemented authorization policy.

## Trace to the code

`PortfolioRestController.getAllPortfolios` → `PortfolioLifecycleService.getAllPortfolios` → `PortfolioPort.getAllPortfolios`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/list-portfolios.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
