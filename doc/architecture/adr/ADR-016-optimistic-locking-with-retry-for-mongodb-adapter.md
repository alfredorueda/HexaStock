# ADR-016: Detect MongoDB conflicts and retry the use case

## Status

Accepted; implementation and limits clarified against `main`.

## Context

The MongoDB adapter implements the same portfolio and transaction ports as JPA. It stores a portfolio document and separate transaction documents. A sale must not overwrite a concurrent update or commit the portfolio without its transaction record.

## Decision

`PortfolioDocument` carries Spring Data `@Version`. The adapter remembers a loaded version in a transaction-scoped context and uses it when mapping the domain object back to a document. A stale version can cause `OptimisticLockingFailureException`; concurrent MongoDB transactions can also fail with write conflicts.

`MongoTransactionManager` supplies the transaction across document writes and requires a replica set. This is not a MongoDB equivalent of the JPA `SELECT FOR UPDATE` protocol.

Cash and trading operations carry the application-owned `@RetryOnWriteConflict`. The bootstrap aspect surrounds the transaction interceptor. For an ordinary call entering the proxied service without an existing transaction, a failed attempt rolls back before a fresh attempt begins. Existing outer transactions can change propagation behavior and must be analysed separately.

Defaults are **three total attempts** and a fixed **50 ms delay**. The implemented classifier walks causes and accepts Spring concurrency failures and `MongoException`; it is broader than optimistic-version failures alone. It is not profile-limited, so qualifying JPA failures can be retried too.

## Alternatives considered

- Return every conflict immediately: simpler execution, but shifts recovery to the caller.
- Import framework retry annotations into application code: less custom infrastructure, but additional framework coupling.
- Retry only an individual write: can reuse stale decisions and break the intended transaction boundary. The design retries the whole use case instead.

## Consequences

The attempt re-reads state and re-evaluates business rules. It can fail again or now reject the operation for insufficient funds or quantity. Quote lookup is repeated and may return a different price. Internal retry is **not HTTP idempotency**: resending a successful request can execute another trade.

When no original version is present in the transaction context, the adapter can fall back to the current stored version. That fallback does not prove that an arbitrary detached domain object was based on that version. `getAllPortfolios()` does not populate the same per-portfolio read-version context.

The broad exception classifier and any future non-transactional side effects deserve review before extending retried operations. The annotation alone cannot establish that repeating an operation is safe.

## Repository evidence

`PortfolioDocument`, `MongoPortfolioRepository`, the repository’s transaction-scoped version map, `RetryOnWriteConflict`, `RetryOnWriteConflictAspect`, and MongoDB transaction configuration define the mechanism. Shared port tests verify selected common behavior; `MongoOptimisticLockingTest` exercises version conflicts.

## Relation to other specifications

Read [the retry lesson](../../tutorial/concurrency/MONGODB-RETRIES.md) for the sequence and [the adapter reference](../../mongodb-adapter-optimistic-write-and-retry.md) for source links and verification. [ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md) describes the complementary JPA protocol.
