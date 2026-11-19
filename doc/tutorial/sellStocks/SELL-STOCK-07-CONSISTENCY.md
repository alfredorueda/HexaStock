# 7. Protect consistency

**Goal:** Distinguish business invariants, transactions and concurrency.

**Study time:** 15–20 minutes, including the checkpoint. **Before starting:** complete [Stage 6](SELL-STOCK-06-PERSISTENCE.md).

## Three protections, three different jobs

| Protection | What it establishes |
| --- | --- |
| Domain rules | A valid state transition, such as rejecting a sale larger than the holding |
| Database transaction | Portfolio changes and the sale record commit or roll back together |
| Concurrency control | Competing requests cannot overwrite each other's changes using stale state |

A unit test of `Portfolio.sell(...)` verifies a domain transition. It does not establish database atomicity. Conversely, a transaction cannot invent a missing business rule.

## Locate the transaction boundary

`PortfolioStockOperationsService` is annotated at class level:

```java
@Transactional
public class PortfolioStockOperationsService
        implements PortfolioStockOperationsUseCase {
    // Use-case methods run through the Spring-managed bean.
}
```

The import is `jakarta.transaction.Transactional`. Spring supports this annotation and applies transaction advice to calls through the managed proxy. Creating the service with `new` in a unit test does not activate that infrastructure. See [Spring's transaction annotation reference](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).

In the configured application, portfolio and transaction writes participate in the selected database transaction. If an unchecked persistence exception escapes, the transaction rolls back. A rollback restores persistent state; it does not rewind mutations already made to an ordinary Java object retained by a caller.

The external quote request is not part of the database's atomic commit. A local transaction cannot roll back a provider call or guarantee that a quote remains current.

## JPA: serialize access to the portfolio

The committed JPA lookup in [JpaPortfolioSpringDataRepository.java](https://github.com/alfredorueda/HexaStock/blob/main/adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java) uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

`JpaPortfolioRepository.getPortfolioById(...)` calls that lookup. The row lock coordinates writers that follow this same protocol, and is held until transaction completion. The method name alone provides no protection: the annotation and active transaction matter.

For example, two requests both wanting 10 of the same 10 shares must not each succeed using the original state. The lock makes the second request wait, then the domain evaluates the updated inventory.

This implementation fetches the market quote after loading and locking the portfolio. Provider latency therefore extends lock duration. Moving the fetch would change the timing and failure-order trade-offs and deserves a separate design decision.

## MongoDB: detect conflicts and retry

The MongoDB adapter uses `@Version` on `PortfolioDocument` and retains the loaded version in transaction-scoped context. A stale write can fail instead of silently replacing newer state. `MongoPersistenceConfig` supplies a `MongoTransactionManager`; the configured MongoDB deployment needs replica-set support for transactions across portfolio and history documents.

`@RetryOnWriteConflict` marks the use-case method. [RetryOnWriteConflictAspect.java](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictAspect.java) wraps the transaction interceptor, so a retry repeats the **whole** operation in a new transaction: reload, fetch price, evaluate rules and save.

The marker defaults to **3 total attempts**, with a 50 ms fixed backoff in the aspect. The actual retry policy includes Spring concurrency failures and `MongoException`; it is broader than only version mismatches. It does not promise success after all attempts are exhausted.

A retry may fetch a different market price. It is also different from HTTP idempotency: repeating a successfully committed client request can execute a second sale. This use case has no idempotency-key mechanism.

For the extended discussion, continue later with [Concurrency Control](../CONCURRENCY-PESSIMISTIC-LOCKING.md).

## Checkpoint

Describe the outcome when two clients each try to sell all 15 initial shares. Why would `@Transactional` without either locking or conflict detection be insufficient?

**Check your answer:** With the concurrency protocol, one sale can commit and the other must evaluate the depleted state, or retry and then reject it. A transaction alone does not prevent two read–modify–write operations from making decisions on the same original inventory and losing an update.

**Stop here** when you can explain the answer without looking at the code. You can resume at the next stage later.

---

[Previous](SELL-STOCK-06-PERSISTENCE.md) · [Learning path](SELL-STOCK-TUTORIAL.md) · [Next: Stage 8](SELL-STOCK-08-ERRORS.md)
