# US-09: Get Holdings Performance

**Purpose:** Compare historical purchases, remaining shares and recorded gains.

**Endpoint:** `GET /api/portfolios/{id}/holdings`. The default local base URL is `http://localhost:8081`.

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-09.AC-1:** A portfolio with stock transactions returns **200 OK** with per-ticker performance objects when quote retrieval succeeds.
- **US-09.AC-2:** A newly created portfolio returns **200 OK** with `[]`.
- **US-09.AC-3:** An unknown non-blank portfolio ID returns **404**, title `Portfolio Not Found`.

## Response example

```json
[
  {
    "ticker": "AAPL",
    "quantity": 10,
    "remaining": 10,
    "averagePurchasePrice": 150.0,
    "currentPrice": 160.0,
    "unrealizedGain": 100.0,
    "realizedGain": 0.0
  }
]
```

## Details and limits

This example assumes 10 shares bought at 150.00 and a quote of 160.00.

| Field | Meaning |
|---|---|
| `quantity` | Total shares purchased in the recorded PURCHASE history. |
| `remaining` | Shares currently held in the domain holding. |
| `averagePurchasePrice` | Total historical purchase cost divided by historical purchased quantity, rounded to two decimals. |
| `currentPrice` | Provider quote, which may be cached. |
| `unrealizedGain` | Current value minus purchase cost of remaining lots. |
| `realizedGain` | Sum of profit recorded on SALE transactions for the ticker. |

A fully sold ticker can remain in this report with zero `remaining`; its historical `quantity` need not be zero. The service loads the full transaction list and fetches quotes before invoking the domain calculator.

The calculator can use zero price and unrealized gain for a missing quote-map entry. The service does **not** catch a provider failure and turn it into that fallback: `ExternalApiException` propagates to **503**. There is no `/performance` endpoint or `limit` parameter.

## Trace to the code

`PortfolioRestController.getHoldings` → `ReportingService.getHoldingsPerformance` → `HoldingPerformanceCalculator`, which returns domain `HoldingPerformance` values mapped by the controller.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/get-holdings-performance.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
