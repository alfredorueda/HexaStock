# ADR-012: Coordinate JPA portfolio access with a pessimistic lock

## Status

Accepted; describes the JPA implementation on `main`.

## Context

Two operations can read the same portfolio balance and both decide that funds are sufficient. A transaction alone does not make this read–decide–write protocol safe from lost updates. Cooperating writers need a common point of coordination.

## Decision

`JpaPortfolioRepository.getPortfolioById()` calls the Spring Data method `findByIdForUpdate()`, annotated with `@Lock(PESSIMISTIC_WRITE)`. Under MySQL this uses a locking read of the portfolio row. The application transaction holds the lock while it loads the aggregate, applies the operation and persists changes.

This is the only single-portfolio lookup path in that adapter: it is also used by lookup/reporting use cases. There is no separate unlocked `findById()` adapter method for read-only operations. `getAllPortfolios()` uses `findAll()` without this explicit lock.

The parent-row lock coordinates code that follows this protocol. It does not automatically lock every child row or protect writers that bypass it. A lock on an existing row found by a unique key should not be described as necessarily a next-key range lock.

## Alternatives considered

- Optimistic version checks: detect competing updates and require failure handling or retry; implemented for MongoDB in [ADR-016](ADR-016-optimistic-locking-with-retry-for-mongodb-adapter.md).
- A process-local lock: cannot coordinate multiple application instances.
- Different transaction isolation: requires analysing the actual database and query semantics; it is not interchangeable with this explicit protocol.

## Consequences

Cooperating writers serialize access to one portfolio. Lock waits, timeouts and deadlocks remain possible. A slow quote request inside the transaction can lengthen the lock hold time. Reads using the same single-portfolio path can also contend.

The shared retry aspect can retry Spring concurrency failures under JPA too. Pessimistic locking does not make retry a no-op or guarantee that every operation succeeds.

## Repository evidence

- `JpaPortfolioSpringDataRepository` in `adapter.out.persistence.jpa.springdatarepository`: locking query.
- `JpaPortfolioRepository.getPortfolioById()`: call site.
- Application service `@Transactional` boundaries and bootstrap `RetryOnWriteConflictAspect`.

## Relation to other specifications

The [JPA lesson](../../tutorial/concurrency/JPA-LOCKING.md) explains the protocol and meaningful concurrent tests. Port contract tests check shared behavior; a normal round-trip test alone does not prove blocking between two transactions.
