# Runtime and verification: separate the guarantees

**Prerequisite:** [the module map](ARCHITECTURE-MAP.md). **Outcome:** distinguish persistence mapping, transaction atomicity, concurrency control and automated evidence.

## Configuration baseline

| Concern | Repository configuration |
|---|---|
| Runtime | Java 21; Spring Boot 3.5.0 |
| Packaging | Bootstrap WAR; local execution documented through `spring-boot:run` |
| Persistence selection | Choose `jpa` or `mongodb` |
| Quote selection | Choose `finhub`, `alphaVantage` or `mockfinhub` |
| Local HTTP port | 8081 |
| MySQL | Compose image 8.0.32, host port 3307 |
| MongoDB | Replica-set configuration for multi-document transactions |
| Quote cache | Caffeine, 1,000 entries, expiration 300 seconds after insertion |
| Test infrastructure | JUnit, REST-Assured, Testcontainers 1.21.4 and ArchUnit |

These are pinned project choices, not claims about the newest available versions. The `finhub` profile and class spelling has one `n`; the provider is named Finnhub. The random mock avoids a real API key but does not provide a repeatable price. Tests requiring exact prices use a fixed provider.

The default JPA configuration uses `create-drop`. Treat that local database as disposable: the configuration does not preserve application data across normal schema creation/teardown. WAR packaging does not, by itself, establish a production deployment strategy.

## Mapping is not transaction management

Domain objects have no JPA annotations. `PortfolioMapper`, `HoldingMapper`, `LotMapper` and `TransactionMapper` translate with `toJpaEntity()` and `toModelEntity()`. MongoDB has its own document mappings.

JPA cascades propagate persistence operations through the portfolio's holdings and lots; orphan removal deletes children removed from the entity graph. Collections are lazy and use `@BatchSize(30)`. In a simple cold load of one portfolio with H holdings, `2 + ceil(H / 30)` is a useful estimate, not an invariant query count. Actual queries depend on the persistence context, data and fetch plan. Joining a nested chain is not automatically a Cartesian product; fetching sibling collections can multiply rows.

The application transaction coordinates portfolio changes and the separate transaction record. Cascading a portfolio graph does not save that independent history record automatically. See [save and reload](../tutorial/sellStocks/SELL-STOCK-06-PERSISTENCE.md).

## Two concurrency mechanisms

| JPA | MongoDB |
|---|---|
| Single-portfolio lookup uses a pessimistic locking query | Document updates use a version check and transaction conflict detection |
| Cooperating access serializes on the portfolio row | A competing update can invalidate an attempt |
| Lock waits, deadlocks and timeouts remain possible | Failed attempts may need bounded retry |

JPA's single-portfolio lookup is also used by reading/reporting services; there is no distinct unlocked adapter lookup. MongoDB uses a transaction-scoped version map for loaded portfolios, with a current-version fallback when no original version is recorded. That fallback is not a universal stale-object check.

The bootstrap retry aspect can classify concurrency errors from either adapter. Its normal service-entry ordering puts retry outside the transaction, allowing a fresh transaction per attempt. Three attempts are the default, not three retries after the first attempt. Repeating the use case may obtain a different quote or reject the operation after re-reading state. This mechanism does not make repeated HTTP requests idempotent.

Read [the concurrency lessons](../tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) before changing this behavior.

## Quote freshness and failures

The real provider adapters cache by ticker. A cache entry may be reused for five minutes after insertion, and its underlying quote may already be older. This is a teaching trade-off, not an execution-price guarantee.

`ExceptionHandlingAdvice` maps eight known exceptions to Problem Detail responses. Do not infer that every malformed request or unexpected framework exception has the same shape. [The error reference](../api/ERRORS.md) lists the exact mappings and validation order.

## Verification and its limits

Run domain tests first with `./mvnw -pl domain test`. The complete `./mvnw clean verify` requires Docker for database-backed tests. Most existing `*IntegrationTest` classes match Surefire naming; Failsafe uses `*IT`. A test's name and build configuration determine the runner, not its conceptual category alone.

Shared port contracts check selected persistence behaviors for both adapters. HTTP tests exercise controller-to-database paths. ArchUnit checks structure. None substitutes for a concurrent test when the claim is that a second writer waits or a stale version is rejected.

JaCoCo aggregates under `bootstrap/target/site/jacoco-aggregate`. The checked-in workflow has no Sonar scanner step, and some artifact paths refer to obsolete root-level report locations. See [CI configuration](../../CI_SETUP.md) before interpreting uploaded artifacts or badges. Protection settings are hosted on GitHub and can change independently of repository files.

## Checkpoint

A sale is rolled back after a write conflict. Which objects must be reloaded on retry? Which external lookup can change? Which test would establish rollback of both portfolio and transaction record?

**Source evidence:** [configuration](https://github.com/alfredorueda/HexaStock/tree/main/bootstrap/src/main/resources), [JPA adapter](https://github.com/alfredorueda/HexaStock/tree/main/adapters-outbound-persistence-jpa/src/main/java), [MongoDB adapter](https://github.com/alfredorueda/HexaStock/tree/main/adapters-outbound-persistence-mongodb/src/main/java), [bootstrap POM](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/pom.xml).

Return to [the technical reference](TECHNICAL-ARCHITECTURE-SPECIFICATION.md) or continue to [decision records](adr/README.md).
