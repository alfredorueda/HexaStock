# ADR-016: Use optimistic locking with application-level retry for the MongoDB persistence adapter

## Status

Accepted

## Context

HexaStock provides two interchangeable persistence adapters that implement the same outbound ports (`PortfolioPort`, `TransactionPort`) defined in the `application` module:

- `adapters-outbound-persistence-jpa` — Spring Data JPA over MySQL, active under the `jpa` Spring profile.
- `adapters-outbound-persistence-mongodb` — Spring Data MongoDB, active under the `mongodb` profile.

[ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md) established **pessimistic write locking** (`SELECT … FOR UPDATE`) as the concurrency strategy for the JPA adapter. That decision is appropriate for a relational backend that natively supports row-level locks and where blocking semantics are well understood by operators.

The MongoDB adapter cannot follow the same strategy directly:

- MongoDB does not provide a portable equivalent to `SELECT … FOR UPDATE` outside multi-document transactions on a replica set.
- Its idiomatic concurrency model is **optimistic**: a `@Version` field on the aggregate document is checked on every write, and concurrent writers that lose the race receive an `OptimisticLockingFailureException`.
- Wrapping each use case in a long-running transaction with document-level locking would negate most of the operational reasons for choosing MongoDB in the first place (latency, availability, document-shaped storage).

A coherent concurrency strategy for the MongoDB adapter is therefore required, and it must remain consistent with the existing application-layer abstractions so that adapters remain interchangeable.

## Decision

Use **optimistic locking** at the persistence layer combined with **application-level retry** driven by a domain-meaningful annotation declared in the application module.

1. **Persistence layer.** The `PortfolioDocument` (and equivalent documents) carry a Spring Data `@Version` field. MongoDB write operations bump the version atomically and reject conflicting writes with `OptimisticLockingFailureException`.
2. **Application layer.** Use cases that may legitimately be retried after a write conflict are annotated with `@RetryOnWriteConflict`, defined in `cat.gencat.agaur.hexastock.application.annotation`. This is a framework-free annotation that expresses a business-level decision: *the entire unit of work is safe to repeat from scratch when its commit fails due to an optimistic-locking conflict.*
3. **Bootstrap layer.** The actual retry behaviour is implemented by `RetryOnWriteConflictAspect` in the bootstrap module. The aspect intercepts annotated methods, retries the unit of work a bounded number of times on `OptimisticLockingFailureException`, and propagates the failure if the retry budget is exhausted.
4. **Aspect ordering.** `RetryOnWriteConflictAspect` is ordered **outside** the `@Transactional` proxy, so each retry runs inside a fresh transaction. This is the only correct ordering: retrying inside a poisoned transaction would never recover.

The JPA adapter is unaffected: under the `jpa` profile the same use cases run with the pessimistic strategy of [ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md), and `@RetryOnWriteConflict` is effectively a no-op because pessimistic locking blocks rather than failing fast.

## Alternatives considered

- **Pessimistic locking on MongoDB via multi-document transactions.** Rejected as the default strategy: it forces a replica set in every environment, increases latency, and inverts MongoDB's natural concurrency model. Multi-document transactions are still used where atomicity across documents is required (`MongoTransactionManager` in the bootstrap module), but not as a substitute for row locks.
- **Retry inside the JPA adapter as well, instead of pessimistic locking.** Rejected as the project-wide default for the reasons given in [ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md): financial use cases must not silently retry behind the user's back when the underlying database supports straightforward serialisation. Optimistic-with-retry remains valid for MongoDB because it is the idiomatic local strategy and the retried unit of work is identical in semantics to the original.
- **Configuring retry via Spring Retry's `@Retryable` directly in the application module.** Rejected to keep the application module free of framework-specific retry annotations. `@RetryOnWriteConflict` carries no framework dependency; the underlying retry library can be replaced by changing only the bootstrap aspect.
- **Implementing the aspect in the application module.** Rejected because aspects belong to the composition root: they are infrastructure concerns and would otherwise leak Spring AOP into a layer that is intentionally framework-agnostic.

## Consequences

**Positive:**
- Each persistence adapter uses the concurrency strategy that is idiomatic for its underlying technology.
- The application module remains framework-free: `@RetryOnWriteConflict` is a marker annotation expressing intent, not a transport for Spring Retry semantics.
- Adapters remain interchangeable behind the same outbound ports; the contract test suite (`AbstractPortfolioPortContractTest`, `AbstractTransactionPortContractTest`) verifies behavioural equivalence between JPA and MongoDB.
- Retry behaviour is observable and bounded: the aspect controls maximum attempts and back-off, and exhaustion is reported as a normal failure of the use case.

**Negative:**
- Two concurrency strategies coexist in the same codebase, which requires the reader to understand which one is active under the chosen profile.
- Retry semantics impose a soft requirement on use cases: only operations that are safe to repeat from scratch (idempotent at the unit-of-work level) may carry `@RetryOnWriteConflict`. The annotation is therefore added deliberately, not by default.
- Operating MongoDB transactions requires a replica set; this is a deployment constraint that does not exist for the JPA adapter.

## Repository evidence

- `application/src/main/java/cat/gencat/agaur/hexastock/application/annotation/RetryOnWriteConflict.java` — annotation definition with rationale in Javadoc.
- `application/src/main/java/cat/gencat/agaur/hexastock/application/service/CashManagementService.java`, `PortfolioStockOperationsService.java` — use cases annotated with `@RetryOnWriteConflict`.
- `adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/document/PortfolioDocument.java` — `@Version` field for optimistic locking.
- `adapters-outbound-persistence-mongodb/src/test/...` — `MongoOptimisticLockingTest` validates conflict detection; `MongoPortfolioRepositoryContractTest` and `MongoTransactionRepositoryContractTest` validate behavioural equivalence with the JPA adapter through the shared abstract contracts.
- `doc/mongodb-adapter-optimistic-write-and-retry.md` — companion tutorial covering the complete design and worked verification commands.

## Relation to other specifications

- **ADR-012:** Establishes pessimistic locking for the JPA adapter. This ADR is its symmetric counterpart for the MongoDB adapter; together they define HexaStock's per-adapter concurrency policy.
- **Gherkin:** Concurrency is not specified at the behavioural level; both adapters must satisfy the same behavioural specifications.
- **OpenAPI:** No relation. API clients are unaware of the concurrency mechanism, including the existence of internal retries.
- **PlantUML / Mermaid:** Not currently represented; a sequence diagram showing the conflict-and-retry path would be a useful future addition.
