# US-06: Buy Stocks

**Purpose:** Acquire shares at the selected provider quote and record a purchase lot.

**Endpoint:** `POST /api/portfolios/{id}/purchases`. The default local base URL is `http://localhost:8081`.

## Request example

```json
{
  "ticker": "AAPL",
  "quantity": 5
}
```

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-06.AC-1:** With sufficient cash, buying 5 AAPL shares returns **200 OK** with an empty body, reduces cash by `5 × quote`, and adds 5 remaining shares.
- **US-06.AC-2:** Buying more of an already-held ticker adds a new lot to the existing holding.
- **US-06.AC-3:** Insufficient cash returns **409**, title `Insufficient Funds`, provided quote retrieval succeeds.
- **US-06.AC-4:** Quantity zero returns **400**, title `Invalid Quantity`.
- **US-06.AC-5:** A negative quantity returns **400**, title `Invalid Quantity`.
- **US-06.AC-6:** An invalid ticker such as `ZZZZ_INVALID` returns **400**, title `Invalid Ticker`, without creating that holding.
- **US-06.AC-7:** An empty ticker returns **400**, title `Invalid Ticker`.
- **US-06.AC-8:** A valid trade request for an unknown non-blank portfolio ID returns **404**, title `Portfolio Not Found`.

## Details and limits

The request supplies ticker and integer quantity, not a client-selected price. The controller validates the ticker and positive quantity before entering the use case. Valid ticker syntax is exactly 1–5 uppercase ASCII letters; syntactic validity does not establish that a provider recognizes the symbol.

The service loads the portfolio, requests the quote, invokes `Portfolio.buy`, saves state and records a PURCHASE. External provider exceptions handled by the advice return **503**, title `External API Error`. Quotes may be cached; the random demonstration provider is unsuitable for asserting fixed expected costs.

## Trace to the code

`PortfolioRestController.buyStock` → `PortfolioStockOperationsService.buyStock` → `Portfolio.buy` → `Holding.buy` → `Lot.create`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/buy-stocks.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
