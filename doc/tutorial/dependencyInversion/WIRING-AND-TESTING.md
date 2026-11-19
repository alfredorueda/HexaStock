# Wiring and Testing Inverted Dependencies

**Goal:** connect an interface to an implementation, then explain what can be tested without infrastructure.

**Prerequisite:** [dependency inversion through a sale](../DEPENDENCY-INVERSION-STOCK-SELLING.md).

## Constructor injection makes the choice explicit

The service constructor receives three ports in this order:

```java
public PortfolioStockOperationsService(
        PortfolioPort portfolioPort,
        StockPriceProviderPort stockPriceProviderPort,
        TransactionPort transactionPort)
```

This signature is an excerpt, not a complete class. The full implementation is in [PortfolioStockOperationsService.java](https://github.com/alfredorueda/HexaStock/blob/main/application/src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java).

`SpringAppConfig` creates the service using `@Bean`. Profiles select the concrete adapters. Choose one persistence profile and one price profile:

| Profile | Quote implementation |
|---|---|
| `finhub` | `FinhubStockPriceAdapter` calls Finnhub. |
| `alphaVantage` | `AlphaVantageStockPriceAdapter` calls Alpha Vantage. |
| `mockfinhub` | `MockFinhubStockPriceAdapter` generates random prices. |

The repository's spelling is `finhub` for the profile and Java class, while the provider's name is Finnhub. The random adapter is useful for a demonstration, but fixed expected calculations need a controlled quote.

For a local application without credentials, follow the [README setup](../../../README.md) and run:

```bash
./mvnw spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=jpa,mockfinhub
```

The database must be running and sibling modules installed as described there. Changing only the quote profile changes provider selection; it does not make provider latency, quote freshness or failures identical.

## Test the orchestration through the ports

In a service unit test, provide a portfolio containing known lots, a price provider returning a fixed `StockPrice`, and a transaction port that captures the recorded sale. Instantiate the service directly.

Verify the result and recorded data. A call-count assertion alone does not show that the correct amount, ticker or quantity was saved. Keep the detailed FIFO arithmetic in domain tests; use the service test to check coordination and relevant failure paths.

This test needs no database or external API. It also cannot prove database rollback, locking, Spring profile selection or HTTP error mapping. Those concerns need integration tests. See [Stage 9](../sellStocks/SELL-STOCK-09-VERIFICATION.md) for the complementary levels.

## Extend the adapter without overstating the guarantee

A new provider may fit the existing port if it can satisfy the same semantic contract. Add its implementation, configuration and adapter tests. Verify that the expected profile selects it.

If the new requirement introduces quote currencies, exchange timestamps or freshness constraints not represented by the port, the contract may need to evolve too. An interface reduces coupling to protocol details; it does not eliminate the need to coordinate semantic changes.

The current external adapters use Spring caching with a configured five-minute expiry and a maximum of 1,000 entries. The demonstration adapter does not have the same caching behavior. A cached quote may be reused rather than fetched again.

Decorators for fallback, additional caching or audit metadata are possible extensions. They are not automatically configured by declaring another implementation of the same port: wiring must distinguish the delegate from the wrapper. Recording a provider name in a log also does not, by itself, satisfy a complete audit requirement.

## Checkpoint

A service unit test passes with a fixed-price provider. What remains unverified?

**Answer:** external response parsing, runtime wiring, database transaction behavior and the HTTP response. Use separate adapter and integration tests for those boundaries.

[Back to dependency inversion](../DEPENDENCY-INVERSION-STOCK-SELLING.md) · [Continue to consistency](../CONCURRENCY-PESSIMISTIC-LOCKING.md)
