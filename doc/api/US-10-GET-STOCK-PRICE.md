# US-10: Get Stock Price

**Purpose:** Read the quote returned by the selected stock-price adapter.

**Endpoint:** `GET /api/stocks/{symbol}`. The default local base URL is `http://localhost:8081`.

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-10.AC-1:** A valid supported symbol with an available provider returns **200 OK** with `symbol`, `price`, `time` and `currency`.
- **US-10.AC-2:** An invalid ticker format returns **400**, title `Invalid Ticker`.

## Response example

```json
{
  "symbol": "AAPL",
  "price": 178.5,
  "time": "2025-01-15T15:30:00Z",
  "currency": "USD"
}
```

## Details and limits

Valid syntax is 1–5 uppercase ASCII letters. The endpoint is `/api/stocks/AAPL`, with no `/price` suffix. A provider failure translated to `ExternalApiException` produces **503**, title `External API Error`.

`time` is an `Instant` serialized with an offset, unlike portfolio and transaction `createdAt` values, which are local date-times. The adapters stamp the quote when constructing it; this is not necessarily an exchange-supplied trade timestamp.

External adapters cache quotes under the configured policy. The `mockfinhub` profile generates random demonstration prices. The response hardcodes `USD`; the domain does not perform currency conversion or validate every provider's instrument currency.

## Trace to the code

`StockRestController.getStockPrice` → `GetStockPriceService.getPrice` → `StockPriceProviderPort.fetchStockPrice`.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/get-stock-price.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
