# US-05: Withdraw Funds

**Purpose:** Remove cash while preventing an overdraft through the withdrawal operation.

**Endpoint:** `POST /api/portfolios/{id}/withdrawals`. The default local base URL is `http://localhost:8081`.

## Request example

```json
{
  "amount": 2000.0
}
```

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-05.AC-1:** Starting with 5000.00, withdrawing 2000.00 returns **200 OK** with an empty body and leaves 3000.00. A WITHDRAWAL record is saved.
- **US-05.AC-2:** Withdrawing zero from an existing portfolio returns **400**, title `Invalid Amount`.
- **US-05.AC-3:** Withdrawing a negative amount returns **400**, title `Invalid Amount`.
- **US-05.AC-4:** Withdrawing 200.00 from a balance of 100.00 returns **409**, title `Insufficient Funds`.
- **US-05.AC-5:** Withdrawing 1.00 from a zero balance returns **409**, title `Insufficient Funds`.
- **US-05.AC-6:** A valid monetary request for an unknown non-blank portfolio ID returns **404**, title `Portfolio Not Found`.

## Details and limits

A successful withdrawal changes cash and records the operation in the same database transaction. The amount must be positive after the domain value object's rounding and must not exceed the current balance.

Concurrent requests also need persistence coordination. The arithmetic check alone does not protect two requests that both read the same starting balance. See [the concurrency introduction](../tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md).

## Trace to the code

`PortfolioRestController.withdraw` → `CashManagementService.withdraw` → `Portfolio.withdraw`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/withdraw-funds.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
