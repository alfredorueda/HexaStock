# US-07: Sell Stocks

**Purpose:** Consume purchase lots in FIFO order and return the financial result.

**Endpoint:** `POST /api/portfolios/{id}/sales`. The default local base URL is `http://localhost:8081`.

## Request example

```json
{
  "ticker": "AAPL",
  "quantity": 12
}
```

## Acceptance criteria

The existing criterion identifiers remain stable. Read these alongside the [error contract](../stock-portfolio-api-specification.md#1-global-error-contract).

- **US-07.AC-1:** From a holding of 5 shares, selling 3 returns **200 OK** with sale details and leaves 2 shares.
- **US-07.AC-2:** For lots of 10 at 100.00 and 5 at 120.00, selling 8 at 150.00 consumes the oldest lot first: proceeds 1200.00, cost basis 800.00 and profit 400.00.
- **US-07.AC-3:** Selling 10 when only 5 are held returns **409**, title `Conflict Quantity`, provided quote retrieval succeeds.
- **US-07.AC-4:** Quantity zero returns **400**, title `Invalid Quantity`.
- **US-07.AC-5:** A negative quantity returns **400**, title `Invalid Quantity`.
- **US-07.AC-6:** Selling a ticker absent from the portfolio returns **404**, title `Holding Not Found`, provided quote retrieval succeeds.
- **US-07.AC-7:** A valid trade request for an unknown non-blank portfolio ID returns **404**, title `Portfolio Not Found`.

## Response example

```json
{
  "portfolioId": "550e8400-e29b-41d4-a716-446655440000",
  "ticker": "AAPL",
  "quantity": 12,
  "proceeds": 1800.0,
  "costBasis": 1240.0,
  "profit": 560.0
}
```

## Details and limits

The example response assumes two lots of 10 at 100.00 and 5 at 120.00, and a sale quote of 150.00. It matches `US-07.FIFO-2`: consume 10 from the first lot and 2 from the second; 3 remain. `US-07.FIFO-1` is the single-lot case in AC-2.

`proceeds = quantity × sale price`; `costBasis` sums the original cost of the consumed shares; `profit = proceeds − costBasis`. Cash increases by proceeds, not profit. The baseline has no settlement gate or fee deduction.

Malformed ticker syntax returns **400**, title `Invalid Ticker`, during controller conversion. After a complete sale, depleted lots are removed but the empty holding remains. A subsequent positive sale from it fails for insufficient quantity if quote retrieval succeeds. These observed behaviors resolve ambiguities in the former specification; they do not imply every case already has a dedicated annotated test.

For the algorithm, diagrams and checkpoints, use [the staged sale tutorial](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md).

## Trace to the code

`PortfolioRestController.sellStock` → `PortfolioStockOperationsService.sellStock` → `Portfolio.sell` → `Holding.sell`; the service saves state and a SALE record.

[Canonical Gherkin](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/sell-stocks.feature) describes the examples. It is not executed by Cucumber; `@SpecificationRef` links selected JUnit tests to requirements and does not guarantee complete coverage.

[Back to the API reference](../stock-portfolio-api-specification.md)
