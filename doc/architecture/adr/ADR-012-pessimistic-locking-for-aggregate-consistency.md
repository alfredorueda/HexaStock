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

- **Optimistic locking (`@Version`):** Uses a version column to detect conflicts. Rejected because financial operations must not fail silently: a "buy" that was accepted by the user should not be retried due to a version conflict. Documented in `doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md`.
- **Application-level lock (e.g. `ReentrantLock`):** Would not work across multiple application instances. Standard alternative.
- **Database-level advisory locks:** More flexible but database-specific and harder to manage within JPA. Standard alternative.
- **No explicit locking (rely on transaction isolation):** Default `READ_COMMITTED` isolation does not prevent lost updates. `SERIALIZABLE` isolation would prevent them but with high contention. Standard alternative.

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
