# US-08: Get Transaction History

**Purpose:** Review financial operations recorded for a portfolio.

**Endpoint:** `GET /api/portfolios/{id}/transactions`. The default local base URL is `http://localhost:8081`.

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-08.AC-1:** Return **200 OK** with a JSON array of flat transaction objects for the portfolio.
- **US-08.AC-2:** The optional `type` parameter is accepted but ignored; requesting `?type=PURCHASE` still returns all recorded types.

## Response example

```json
[
  {
    "id": "transaction-example",
    "portfolioId": "portfolio-example",
    "type": "PURCHASE",
    "ticker": "AAPL",
    "quantity": 10,
    "unitPrice": 150.0,
    "totalAmount": 1500.0,
    "profit": 0.0,
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

## Details and limits

The REST controller maps domain records through `TransactionDTO.from`; the service does not return DTOs or expose nested value objects. Cash transactions have null `ticker` and `unitPrice`, quantity zero and profit zero. Purchases also have profit zero; sales carry their realized profit.

There is no pagination, date/ticker filtering or guaranteed ordering contract. The `type` string is not validated against an enum by the controller. For an unknown non-blank portfolio ID, the service queries transactions without checking portfolio existence, producing an empty list when none exist.

The annotated integration test for the type parameter checks that records return. It does not establish filtering support. A future filtering feature must change the contract, implementation and assertions together.

## Trace to the code

`PortfolioRestController.getTransactions` → `TransactionService.getTransactions` → `TransactionPort.getTransactionsByPortfolioId`; DTO mapping occurs in the controller.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/get-transaction-history.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
