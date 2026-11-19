# US-01: Create Portfolio

**Purpose:** Create an account with zero cash before making deposits or trades.

**Endpoint:** `POST /api/portfolios`. The default local base URL is `http://localhost:8081`.

## Request example

```json
{
  "ownerName": "Alice"
}
```

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-01.AC-1:** Given owner name `Alice`, creating a portfolio returns **201 Created**, a generated ID and a `Location` header pointing to its URI. The body has `id`, `ownerName`, `cashBalance: 0.00` and `currency: "USD"`.

## Response example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ownerName": "Alice",
  "cashBalance": 0.0,
  "currency": "USD"
}
```

## Details and limits

The `Location` URI is derived from the incoming request. With the default local server it begins `http://localhost:8081/api/portfolios/`.

`CreatePortfolioResponseDTO` uses `cashBalance`. The lookup DTO uses `balance`; clients should not assume both responses have identical fields.

Owner-name validation is limited: the domain constructor rejects null, but the request DTO has no Bean Validation annotations defining a complete blank-name error contract. Do not assume every malformed creation request produces the custom 400 response used for monetary validation.

## Trace to the code

`PortfolioRestController.createPortfolio` → `PortfolioLifecycleService.createPortfolio` → `Portfolio.create`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/create-portfolio.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
