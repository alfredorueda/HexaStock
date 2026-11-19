# 9. Verify the complete flow

**Goal:** Connect deterministic HTTP tests to the specification.

**Study time:** 15–20 minutes, including the checkpoint. **Before starting:** complete [Stage 8](SELL-STOCK-08-ERRORS.md).

## Choose the test that answers the question

| Layer | Existing test | Evidence provided |
| --- | --- | --- |
| Lot algorithm | `HoldingTest` | FIFO cost and remaining lots |
| Aggregate | `PortfolioTest` | Cash and inventory change together in memory |
| Application | `PortfolioStockOperationsServiceTest` | Orchestration through port doubles |
| REST adapter | `PortfolioRestControllerTest` | HTTP mapping with mocked use cases |
| Persistence | `JpaPortfolioRepositoryContractTest`, `MongoPortfolioRepositoryContractTest` | Port behaviour against real storage |
| Full request | `PortfolioTradingRestIntegrationTest` | HTTP, wiring, domain and MySQL round trips |
| Architecture | `HexagonalArchitectureTest` | Selected dependency constraints |

These tests complement each other. A passing example provides evidence for its assertions, not a proof of every invariant, failure mode or concurrency schedule.

## Make the price sequence deterministic

The [trading integration test](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioTradingRestIntegrationTest.java) imports a `@TestConfiguration` containing a `@Primary` price adapter. The actual bean constructor requires a fallback price:

```java
@Bean
@Primary
FixedPriceStockPriceAdapter fixedPriceStockPriceAdapter() {
    return new FixedPriceStockPriceAdapter(Price.of("150.00"));
}
```

Each fetch consumes the next queued price, or uses 150.00 when the queue is empty. This makes both the Gherkin and other trading tests deterministic; other integration classes may use the random `mockfinhub` adapter.

Before **each** Gherkin scenario, the fixture clears the queue, creates Alice's portfolio, deposits 100000, then queues and consumes **100.00** for the 10-share purchase and **120.00** for the 5-share purchase. The test queues **150.00** for its sale. The deposit differs from the domain example, but the sale result and cash increase are identical.

This is adapter substitution through `StockPriceProviderPort`: the domain and use case stay unchanged. It demonstrates this replacement, not that every possible provider automatically satisfies the contract.

## Assert exact financial results

This excerpt from `GherkinFifoScenarios` relies on that fixture and the class's helper methods:

```java
@Test
@SpecificationRef(value = "US-07.FIFO-2", level = TestLevel.INTEGRATION,
                  feature = "sell-stocks.feature")
void sellSharesAcrossMultipleLots_FIFOGherkinScenario() {
    fixedPriceAdapter.enqueuePrice(Price.of("150.00"));

    sellPrecise(portfolioId, "AAPL", 12)
            .statusCode(200)
            .body("proceeds", comparesEqualTo(new BigDecimal("1800.00")))
            .body("costBasis", comparesEqualTo(new BigDecimal("1240.00")))
            .body("profit", comparesEqualTo(new BigDecimal("560.00")));

    getHoldings(portfolioId)
            .body("find { it.ticker == 'AAPL' }.remaining", equalTo(3));
}
```

`sellPrecise(...)` sets REST Assured's JSON number return type to `BIG_DECIMAL`. `comparesEqualTo(...)` compares numeric values without requiring identical decimal scales. This avoids binary floating-point assertions for the financial fields.

For these lots, FIFO cost is **1240.00**; LIFO cost would be **1300.00** (`5 × 120 + 7 × 100`). Thus cost basis distinguishes the two policies for this fixture. It does not prove correct FIFO for every input. The HTTP test checks aggregate remaining shares; the domain test also checks the individual surviving lot.

## Run checks in increasing scope

From the repository root, with **JDK 21**:

```bash
# Domain, application and REST adapter tests; no database required
./mvnw -pl domain,application,adapters-inbound-rest -am test
```

For the full build, start a Docker-compatible engine first:

```bash
./mvnw clean verify
```

The full build includes Testcontainers-backed persistence tests; it needs Docker and access to the required container images. The trading HTTP tests use the `test`, `jpa` and `mockfinhub` profiles, replace the price adapter and start MySQL through Testcontainers. They do not require a live market API key. MongoDB persistence tests exercise the other adapter separately.

## Close the traceability chain

```text
US-07 acceptance criteria
  → sell-stocks.feature / US-07.FIFO-2
  → @SpecificationRef on a JUnit method
  → assertions against production behaviour
```

`@SpecificationRef` is metadata, not a test runner or a coverage guarantee. Its presence does not verify that assertions are adequate; review those assertions against the scenario.

`HexagonalArchitectureTest` contains **seven** rules: domain must not depend on application, adapters or Spring; application must not depend on adapters or Spring; inbound and outbound adapters must not depend on each other. These checks inspect class dependencies. They do not enforce every architectural property, and the current rules do not prohibit dependencies between different outbound adapters.

You can now explain the complete path: HTTP translation → use-case orchestration → domain sale → persistence and history → commit → response. For deeper study, use the [references](SELL-STOCK-REFERENCE.md) or choose an [exercise](SELL-STOCK-EXERCISES.md).

## Checkpoint

What would a passing 1240.00 cost-basis assertion miss if it were the only assertion? Name one domain check and one infrastructure check needed to complement it.

**Check your answer:** It could miss an incorrect cash update or incorrectly retained lots. Assert cash and surviving lot state through the aggregate; use persistence/HTTP tests for save-and-reload behaviour and dedicated transactional/concurrency tests for rollback and competing writes.

**Stop here** when you can explain the answer without looking at the code. You can resume at the next stage later.

---

[Previous](SELL-STOCK-08-ERRORS.md) · [Learning path](SELL-STOCK-TUTORIAL.md) · [Optional exercises](SELL-STOCK-EXERCISES.md)
