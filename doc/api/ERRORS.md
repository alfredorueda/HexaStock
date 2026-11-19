# API Errors and Validation Order

**Goal:** distinguish an expected business rejection from an infrastructure failure.

## Handled application exceptions

`ExceptionHandlingAdvice` maps the following exception types to `application/problem+json` responses using Spring `ProblemDetail`:

| Exception | Status | Title |
|---|---|---|
| `PortfolioNotFoundException` | 404 | Portfolio Not Found |
| `HoldingNotFoundException` | 404 | Holding Not Found |
| `InvalidAmountException` | 400 | Invalid Amount |
| `InvalidQuantityException` | 400 | Invalid Quantity |
| `InvalidTickerException` | 400 | Invalid Ticker |
| `ConflictQuantityException` | 409 | Conflict Quantity |
| `InsufficientFundsException` | 409 | Insufficient Funds |
| `ExternalApiException` | 503 | External API Error |

For example, a zero-quantity sale produces:

```json
{
  "type": "about:blank",
  "title": "Invalid Quantity",
  "status": 400,
  "detail": "Quantity must be positive: 0",
  "instance": "/api/portfolios/example/sales"
}
```

The example path is illustrative. `detail` comes from the exception; provider-specific messages vary. Parse the status and documented structure rather than treating every message as a permanent machine identifier.

The advice does not handle every possible exception. Malformed JSON, missing fields, null values and unexpected infrastructure failures may follow framework handling or an unhandled-error path. Do not extend the table into a claim that all conceivable failures have this exact format.

## Why validation order matters

For a purchase or sale, the controller constructs `PortfolioId`, `Ticker` and a positive `ShareQuantity` before invoking the service. Ticker and quantity conversion failures therefore happen before the portfolio lookup.

The service then loads the portfolio and obtains a quote before invoking the domain trade operation. If the quote provider fails, that failure can be observed before an insufficient-funds or missing-holding check. Acceptance criteria for those business errors assume successful quote retrieval.

For cash operations, `Money` is constructed before service entry; the service looks up the portfolio before invoking the positive-amount business check. A valid zero amount and a malformed null amount are not the same path.

## Read errors as teaching evidence

A REST slice test can verify translation by making a mocked use case throw an exception. An integration test must establish the actual triggering condition. To learn the complete distinction, follow [Stage 8: trace failures](../tutorial/sellStocks/SELL-STOCK-08-ERRORS.md).

[Back to the API reference](../stock-portfolio-api-specification.md)
