# US-04: Deposit Funds

**Purpose:** Increase cash available for purchases.

**Endpoint:** `POST /api/portfolios/{id}/deposits`. The default local base URL is `http://localhost:8081`.

## Request example

```json
{
  "amount": 2000.0
}
```

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-04.AC-1:** Starting with 5000.00, depositing 2000.00 returns **200 OK** with an empty body and leaves 7000.00. A DEPOSIT record is saved.
- **US-04.AC-2:** Depositing zero to an existing portfolio returns **400**, title `Invalid Amount`.
- **US-04.AC-3:** Depositing a negative amount to an existing portfolio returns **400**, title `Invalid Amount`.
- **US-04.AC-4:** A request with a valid monetary body and an unknown non-blank portfolio ID returns **404**, title `Portfolio Not Found`.

## Details and limits

Use `GET /api/portfolios/{id}` to see the updated `balance`. Empty success bodies are intentional; success does not mean HTTP 204.

`Money` rounds to two decimal places using `HALF_UP`. The business check applies to that rounded value. A tiny positive input that rounds to zero is rejected. The service looks up the portfolio before invoking its deposit rule; malformed request conversion can fail before the service runs.

## Trace to the code

`PortfolioRestController.deposit` → `CashManagementService.deposit` → `Portfolio.deposit`; the service saves portfolio state and a financial transaction.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/deposit-funds.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
