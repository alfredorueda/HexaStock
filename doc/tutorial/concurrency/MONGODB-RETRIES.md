# 2. Detect Conflicts and Retry the Use Case

**Goal:** explain the complete read–calculate–write retry boundary.

## Preserve the version that was read

`PortfolioDocument` has a Spring Data `@Version Long version`. The domain `Portfolio` has no persistence version field. The MongoDB adapter therefore remembers the loaded version in transaction-scoped state and supplies it when mapping the updated domain object back to a document.

For supported versioned writes, Spring Data includes the expected version in the update condition and advances it on success. A stale version can produce `OptimisticLockingFailureException`. Database transactions can also fail with a MongoDB write conflict; the observable exception is not always the same. See [Spring Data's optimistic-locking documentation](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking).

Optimistic application concurrency does not mean that MongoDB has no internal locks or waiting. Transactions can encounter lock acquisition limits and write conflicts, as described in [MongoDB's transaction considerations](https://www.mongodb.com/docs/manual/core/transactions-production-consideration/).

## Repeat the whole operation

Suppose two deposits read version 4 and balance 100.00. One succeeds with a balance of 150.00. The other must start a fresh attempt, read the updated state and calculate 200.00. Retrying only its old save would preserve the wrong calculation.

The application annotation `@RetryOnWriteConflict` marks `deposit`, `withdraw`, `buyStock` and `sellStock`. Its implementation lives in `RetryOnWriteConflictAspect` in bootstrap.

| Policy | Current implementation |
|---|---|
| Attempt limit | Three total attempts by default: one initial attempt and up to two retries. |
| Delay | Fixed 50 ms between retry attempts. |
| Exceptions | `OptimisticLockingFailureException`, `ConcurrencyFailureException` and `MongoException`, with cause traversal. |
| Ordering | Highest precedence, outside the transaction interceptor. |

The exception policy is broader than write conflicts: it includes all `MongoException` instances. It does not inspect only retryable labels or particular conflict codes. Document this behavior accurately before proposing a narrower policy.

## Why the transaction must be inside the retry

For the normal service entry path, each attempt enters the transaction interceptor, reads fresh state and either commits or rolls back. The outer retry advice can then start another attempt.

This depends on runtime wiring and call context. The annotation alone does not prove safe retry, and an existing outer transaction can change propagation behavior. Do not move the retry inside an already failed transaction or bypass the proxy and expect the same semantics.

The MongoDB profile supplies `MongoTransactionManager`. The local Compose and Testcontainers setups use a replica set, required for the multi-document transaction that updates the portfolio and its separate transaction record.

## Retry is not request idempotency

Retrying a rolled-back database attempt is different from receiving the same HTTP request twice after one invocation has already committed. The latter can apply the operation twice; there is no HTTP idempotency-key mechanism in this baseline.

A retry also reruns the quote lookup for trading operations. It may receive a different quote, and nontransactional external effects would not automatically roll back. Adding notifications or payments to the flow would require a separate design for those effects.

Retries can be exhausted under continued contention. A second attempt is not guaranteed to succeed.

## Checkpoint

Why does the integration test force two initial reads of the same state?

**Answer:** without controlled overlap, concurrent tasks may execute sequentially and never exercise the conflict path. A useful test establishes the race, then checks the final balance and evidence of additional attempts.

[Previous: JPA locking](JPA-LOCKING.md) · [Next: MongoDB adapter reference](../../mongodb-adapter-optimistic-write-and-retry.md)
