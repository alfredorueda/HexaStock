# ADR-012: Use pessimistic locking for portfolio aggregate consistency

## Status

Accepted

## Context

Financial operations (deposit, withdraw, buy, sell) modify the portfolio aggregate's balance and holdings. In a concurrent environment, two simultaneous operations on the same portfolio could read the same balance, both passes the insufficient-funds check, and both write back conflicting state (lost update problem). The concurrency control mechanism must prevent this without requiring application-level retry logic.

## Decision

Use JPA pessimistic write locking (`LockModeType.PESSIMISTIC_WRITE`) when loading a portfolio for update:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

This translates to a SQL `SELECT ... FOR UPDATE`, which acquires an exclusive row lock on the portfolio row. Concurrent transactions attempting to modify the same portfolio will block until the lock is released.

The `JpaPortfolioRepository.findById()` method for read-only access (e.g. `getPortfolio()`) does **not** use pessimistic locking. Only the `findByIdForUpdate()` method, used by write operations, acquires the lock.

## Alternatives considered

- **Optimistic locking (`@Version`):** Uses a version column to detect conflicts. Rejected as the **default** strategy for the JPA / MySQL adapter because financial operations must not silently retry on a relational backend that natively supports straightforward serialisation. Optimistic locking is, however, the idiomatic choice for the MongoDB adapter, where it is combined with bounded application-level retry; see [ADR-016: Optimistic locking with retry for the MongoDB adapter](ADR-016-optimistic-locking-with-retry-for-mongodb-adapter.md) and `doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md`.
- **Application-level lock (e.g. `ReentrantLock`):** Would not work across multiple application instances. Standard alternative.
- **Database-level advisory locks:** More flexible but database-specific and harder to manage within JPA. Standard alternative.
- **No explicit locking (rely on transaction isolation):** MySQL/InnoDB's default isolation is `REPEATABLE READ`, which prevents non-repeatable reads but does not by itself prevent the lost-update pattern of *read \u2192 decide \u2192 write* across concurrent transactions. Raising to `SERIALIZABLE` would prevent lost updates but impose higher contention and is SQL-standard semantics that InnoDB implements via next-key locking on reads; this is effectively equivalent to using `SELECT ... FOR UPDATE` on the critical read, but global to all reads. Standard alternative [Berenson et al., 1995; MySQL 8 Reference Manual, \u00a7\u00a7 Transaction Isolation Levels and Locking Reads].

## Consequences

**Positive:**
- Guarantees consistency for concurrent financial operations on the same portfolio.
- Lock scope is minimal (single aggregate row).
- No retry logic needed in application services - the transaction will always see consistent state.
- Read-only operations (`getPortfolio`, `getAllPortfolios`) are not affected by locking.

**Negative:**
- Pessimistic locking can cause blocking under high contention on the same portfolio.
- Requires database support for `SELECT ... FOR UPDATE` (supported by MySQL).
- If transactions are held open too long, lock contention can cause timeouts.

## Repository evidence

- `JpaPortfolioSpringDataRepository.java`: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdForUpdate(@Param("id") String id)`
- `JpaPortfolioRepository.java`: `findById()` calls `springRepository.findByIdForUpdate(id)` for write operations
- `doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md`: Comprehensive tutorial explaining the concurrency problem, locking strategies, and why pessimistic locking was chosen
- `PortfolioStockOperationsService.java`: `@Transactional` ensures the lock is held for the duration of the use case
- `application.properties`: `spring.jpa.hibernate.ddl-auto=create-drop` (Hibernate manages schema)

## Relation to other specifications

- **Gherkin:** Concurrency is not explicitly specified in Gherkin scenarios (which focus on single-user behaviour). This ADR addresses a technical concern that is invisible at the behavioural specification level.
- **OpenAPI:** No relation. API clients are unaware of locking.
- **PlantUML:** Not directly represented in current diagrams, but could be visualised in a sequence diagram showing concurrent access patterns.
