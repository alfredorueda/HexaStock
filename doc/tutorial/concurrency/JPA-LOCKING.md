# 1. Coordinate Writers with a JPA Lock

**Goal:** trace the lifetime and limits of the portfolio lock.

## The current read path

`JpaPortfolioRepository.getPortfolioById` delegates to `findByIdForUpdate`, declared in `JpaPortfolioSpringDataRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

This annotation is active on `main`. The adapter maps the managed JPA entity into a separate domain object. The transaction retains the database lock while the application modifies and saves that domain state.

The service's Jakarta `@Transactional` annotation is interpreted by Spring transaction infrastructure. A normal application request enters through the proxied service. Constructing the service with `new` in a unit test does not create a database transaction.

## Follow two deposits

1. Request A loads the portfolio with the locking query.
2. Request B tries to acquire a conflicting lock on the same portfolio and waits.
3. A updates cash, saves the portfolio and records the deposit.
4. A's transaction commits or rolls back, releasing its locks.
5. B can proceed using the state available to its locking read.

For an existing row located by a unique-key equality condition, InnoDB locks the index record rather than necessarily taking a next-key lock. Range searches and missing-key cases can have different locking behavior. See [MySQL's lock rules](https://dev.mysql.com/doc/refman/8.0/en/innodb-locks-set.html).

The portfolio-row lock coordinates operations that follow this protocol. It is not a magical lock on every child object or every possible database access path. Direct writes that bypass the protocol need separate analysis.

## What can still go wrong?

Pessimistic locking does not eliminate deadlocks, lock-wait timeouts or connection pressure. Long-running work inside the transaction increases the time other writers may wait.

The retry aspect is also active for annotated JPA use cases. The normal concurrent-deposit test expects one attempt per invocation, but that does not mean JPA can never retry: the policy includes Spring `ConcurrencyFailureException` and its subclasses.

The current `getPortfolioById` port does not distinguish a read-only lookup from a read-for-update. Callers using it can therefore acquire the same pessimistic lock even when they only need to display a portfolio. A dedicated query path would be a design extension.

## Read the evidence accurately

`JpaPessimisticLockingTest` verifies that the locking query returns a managed entity and handles an absent ID. A managed-entity assertion alone does not demonstrate that a second transaction blocks.

`RetryOnWriteConflictJpaIntegrationTest` exercises concurrent deposits and checks their combined balance and attempt count. That is stronger behavioral evidence for this scenario, but it does not cover every isolation anomaly or arbitrary transaction pattern.

Run the adapter tests after installing sibling modules:

```bash
./mvnw -pl adapters-outbound-persistence-jpa test
```

Use the [source repository](https://github.com/alfredorueda/HexaStock/blob/main/adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java) to inspect the active query rather than disabling it in the learning application.

## Checkpoint

Does the lock end when the repository method returns?

**Answer:** no. It is held by the surrounding database transaction until completion, including the later domain calculation and persistence work.

[Back to the concurrency introduction](../CONCURRENCY-PESSIMISTIC-LOCKING.md) · [Next: MongoDB retries](MONGODB-RETRIES.md)
