# 1. Test Rules and Orchestration

**Goal:** write assertions that explain the behavior being checked.

## Domain: use real business objects

`PortfolioTest` and `HoldingTest` construct ordinary Java objects. They do not start Spring, a database or an HTTP server. The production domain is framework independent; the tests still use JUnit and other test libraries.

For the sale of 12 shares, check proceeds of 1800.00, cost basis of 1240.00 and profit of 560.00. At the portfolio level, also check the cash increase and remaining shares. At the holding level, inspect the surviving lot and its purchase price.

These tests overlap intentionally: the holding test isolates FIFO; the portfolio test checks its combination with cash. A result assertion is evidence for the tested input, not a mathematical proof of all possible FIFO sequences.

Use fresh objects for each case. Include an invalid operation and verify that the relevant state did not change. A test can fail because its setup or expectation is wrong too; a failure is a signal to investigate, not automatic proof of a production defect.

The [first domain test](../sellStocks/SELL-STOCK-02-DOMAIN-TEST.md) gives a complete starting example.

## Application: control the external capabilities

`PortfolioStockOperationsServiceTest` supplies mocked `PortfolioPort`, `StockPriceProviderPort` and `TransactionPort` dependencies, with real domain objects. The price is controlled so the result is repeatable.

The service tests verify the sequence: load portfolio, fetch quote, save portfolio and record the transaction. They can also inspect the saved record's data. A verification such as `save(any(Transaction.class))` establishes that a call occurred; it does not establish that every recorded field is correct.

Mocking a port makes a failure easy to arrange. For example, return an empty portfolio lookup and check that the use case reports the missing portfolio without calling the provider. Keep FIFO details in domain tests and coordination questions in service tests.

Directly instantiating the service does not exercise Spring transaction or retry advice. Use integration tests for rollback and conflict handling.

## REST: check the translation

`PortfolioRestControllerTest` uses a web slice and mocked use cases. It can check how a request becomes domain value objects and how the result becomes JSON. It can also exercise the exception advice for a failure returned by the mocked use case.

The sale endpoint is `POST /api/portfolios/{id}/sales`; the quote endpoint is `GET /api/stocks/{symbol}`. The latter has no `/price` suffix.

A mocked successful `SellResult` proves response mapping, not the FIFO calculation that would produce it. Likewise, a mocked not-found exception proves HTTP translation, not that persistence actually detects a missing portfolio.

## Checkpoint

A REST test supplies a mocked result with `profit = 560.00` and receives the same JSON value. Has it tested the profit formula?

**Answer:** no. It has tested mapping. A domain test must invoke the calculation with known lots and prices.

[Back to the test map](../TESTING-STRATEGY.md) · [Next: adapters](02-ADAPTER-CONTRACTS.md)
