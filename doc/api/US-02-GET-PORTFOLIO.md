# US-02: Get Portfolio

**Purpose:** Read a portfolio's identity, owner, cash balance and creation time.

**Endpoint:** `GET /api/portfolios/{id}`. The default local base URL is `http://localhost:8081`.

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-02.AC-1:** An existing ID returns **200 OK** with `id`, `ownerName`, `balance` and `createdAt`.
- **US-02.AC-2:** An unknown non-blank ID returns **404**, title `Portfolio Not Found`.

## Response example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ownerName": "Alice",
  "balance": 10000.0,
  "createdAt": "2025-01-15T10:30:00"
}
```

## Details and limits

Replace `{id}` with the ID returned by creation. Generated IDs are UUID strings, but `PortfolioId` validates non-blank text rather than UUID syntax.

`createdAt` is a local date-time without a timezone offset. It should not be interpreted as a globally unambiguous instant. This response does not embed holdings or financial transactions; those have separate endpoints.

## Trace to the code

`PortfolioRestController.getPortfolio` → `PortfolioLifecycleService.getPortfolio` → `PortfolioPort.getPortfolioById`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/get-portfolio.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
