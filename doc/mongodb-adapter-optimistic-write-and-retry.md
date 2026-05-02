# MongoDB Persistence Adapter: Optimistic Locking and Retry Strategy

## 1) Purpose of this document

This document explains the **optimistic concurrency control strategy implemented in the MongoDB persistence adapter** of HexaStock. The MongoDB adapter is one of two interchangeable persistence implementations available on `main` and is a co-equal alternative to the JPA/MySQL adapter; both implement the same outbound ports (`PortfolioPort`, `TransactionPort`) defined in the `application` module and are selected at runtime via Spring profiles (`jpa` or `mongodb`).

The two adapters illustrate complementary concurrency strategies:

- **JPA adapter** (`adapters-outbound-persistence-jpa`): pessimistic write locking with MySQL (`SELECT … FOR UPDATE`).
- **MongoDB adapter** (`adapters-outbound-persistence-mongodb`): optimistic locking with application-level retry, driven by the `@RetryOnWriteConflict` annotation in the application layer and an aspect-based implementation in the bootstrap module.

This document covers:

- What was implemented.
- Why key decisions were taken.
- How concurrency is handled (JPA vs MongoDB behavior).
- Why retry policy is modeled as an application concern with an infrastructure implementation.
- How to reproduce the same implementation from scratch with Codex / Claude Code / Cursor.

It is intentionally focused on the implemented design, not a broad alternatives analysis.

---

## 2) Scope of the MongoDB adapter

### 2.1 MongoDB outbound persistence adapter

The module `adapters-outbound-persistence-mongodb` implements the same application ports used by JPA:

- `PortfolioPort` via `MongoPortfolioRepository`
- `TransactionPort` via `MongoTransactionRepository`

Main components:

- Documents:
  - `PortfolioDocument`
  - `HoldingDocument`
  - `LotDocument`
  - `TransactionDocument`
- Mappers:
  - `PortfolioDocumentMapper`
  - `HoldingDocumentMapper`
  - `LotDocumentMapper`
  - `TransactionDocumentMapper`
- Spring Data repositories:
  - `MongoPortfolioSpringDataRepository`
  - `MongoTransactionSpringDataRepository`

Activation is profile-based (`@Profile("mongodb")`), so the application can switch between JPA and MongoDB without changing use cases.

### 2.2 Mongo transactions

`MongoPersistenceConfig` provides `MongoTransactionManager` under `mongodb` profile.  
`bootstrap/src/main/resources/application-mongodb.properties` points Mongo URI to a single-node replica set (`replicaSet=rs0`) and disables JPA autoconfiguration when Mongo profile is active.

### 2.3 Optimistic concurrency in Mongo

`PortfolioDocument` includes:

```java
@Version
private Long version;
```

This uses Spring Data MongoDB standard optimistic locking.

### 2.4 Application-level retry contract + infrastructure implementation

To keep `application` framework-agnostic:

- New annotation in `application`:
  - `@RetryOnWriteConflict(maxAttempts = 3)`
- Applied to write use-case methods:
  - `CashManagementService.deposit`
  - `CashManagementService.withdraw`
  - `PortfolioStockOperationsService.buyStock`
  - `PortfolioStockOperationsService.sellStock`

Infrastructure implementation is in `bootstrap`:

- `RetryOnWriteConflictAspect`
- Uses Spring Retry `RetryTemplate`
- Retries on:
  - `OptimisticLockingFailureException`
  - `ConcurrencyFailureException`
  - `MongoException`
- Order is `@Order(Ordered.HIGHEST_PRECEDENCE)` so retry wraps the transactional execution.

---

## 3) Key design decisions and why they were required

## 3.1 Spring Data already has optimistic locking; why custom code still exists

Spring Data MongoDB already supports optimistic locking with `@Version`.  
However, our **domain model does not carry persistence version** (correct in hexagonal design).

If the adapter maps domain -> document without version, Spring can treat the document as new or stale incorrectly depending on value, and conflict detection may not behave as required for our write path.

So the adapter adds a small infrastructure-only mechanism:

- On `getPortfolioById`, it remembers loaded document version in a **transaction-scoped context**.
- On `savePortfolio`, it maps domain data + expected version.
- After save, it refreshes remembered version.

This keeps version handling outside domain/application while preserving standard Spring optimistic locking behavior.

## 3.2 Why retry cannot be inside Mongo adapter save method

Retrying only `savePortfolio` is incorrect for this domain.

Use-case flow is read -> business calculation -> write.  
When a write conflict happens, the stale read must be replaced by a fresh read and the business calculation must be recomputed.

If we only retry `save` with updated version, we may persist a value derived from stale state.

Therefore retry must wrap the **whole use case method**, not only the repository write operation.

## 3.3 Why annotation in `application` and aspect in `bootstrap`

In hexagonal architecture:

- `application` should express business/application policy.
- Infrastructure/framework details belong to outer layers.

`@RetryOnWriteConflict` in `application` expresses intent without Spring dependency.  
`RetryOnWriteConflictAspect` in `bootstrap` maps that intent to Spring Retry.

If we used Spring `@Retryable` directly in use cases, the application layer would be coupled to Spring.  
Current approach allows replacing retry technology by changing only infrastructure wiring.

---

## 4) Concurrency model: JPA vs MongoDB in this project

## 4.1 JPA path

JPA repository reads with pessimistic write lock (`findByIdForUpdate`), so concurrent writes are serialized.  
In normal operation this should not require optimistic retry at the use-case level.

## 4.2 Mongo path

Mongo path uses optimistic locking (`@Version`).  
Concurrent stale updates may race; one update wins, the stale one fails with optimistic/write conflict exception.

The implemented retry policy then re-executes the full use case in a new attempt.

---

## 5) What was implemented specifically for conflict handling

### 5.1 Mongo document and mapping

- Added `version` field with `@Version` in `PortfolioDocument`.
- Mapper supports explicit version:
  - `toDocument(entity)` (default)
  - `toDocument(entity, version)` (used in saves)

### 5.2 Mongo repository behavior

`MongoPortfolioRepository`:

- Tracks per-portfolio expected version in transaction synchronization context.
- Reads record version on load and after save/create.
- Uses tracked version when saving to trigger proper optimistic check.
- Has fallback read if context is missing.
- Cleans context in `afterCompletion`.

### 5.3 Retry implementation

`RetryOnWriteConflictAspect`:

- Resolves annotation from concrete method.
- Builds `RetryTemplate` with annotation max attempts.
- Retries only known write-conflict exception families.
- Keeps checked exceptions disallowed for these methods (defensive guard).

---

## 6) Tests added and coverage status

### 6.1 Mongo adapter tests

- `MongoPortfolioRepositoryContractTest` (Portfolio port contract)
- `MongoTransactionRepositoryContractTest` (Transaction port contract)
- `MongoOptimisticLockingTest` (concurrent stale-write conflict detection)
- `MapperTest` (comprehensive mapper tests, including version mapping)

### 6.2 Retry tests in bootstrap

- `RetryOnWriteConflictAspectTest` (unit behavior of aspect)
- `RetryOnWriteConflictJpaIntegrationTest`
  - validates concurrent JPA path does not trigger extra retries
- `RetryOnWriteConflictMongoIntegrationTest`
  - forces concurrent stale reads on first attempt and validates retries occur

### 6.3 Test count parity requested

Current counts:

- JPA tests: 22
- Mongo adapter tests: 22

This was intentionally aligned to keep similar confidence between both persistence adapters.

---

## 7) Practical reviewer notes (for trainer/PR reviewer)

- Domain model was not polluted with infrastructure version field.
- Application layer does not depend on Spring Retry APIs.
- Retry policy is explicit and readable at use-case level.
- Infrastructure can be swapped with minimal impact (only aspect/wiring).
- Conflict handling is validated both at adapter-level and end-to-end integration level.

Short note on alternatives (not chosen):

- Retry only in adapter `save` was rejected because it does not recompute business logic on fresh state.
- Spring `@Retryable` directly in application use cases was rejected to avoid framework coupling.

---

## 8) Implementation blueprint for coding agents (Codex / Claude / Cursor)

Use the following as a direct implementation brief to reproduce this solution with minimal deviation.

```text
You are implementing MongoDB optimistic concurrency + application-level retry in a hexagonal Spring project.

Goals:
1) Keep domain and application ports unchanged.
2) Use MongoDB as outbound persistence adapter under profile "mongodb".
3) Implement optimistic locking in Mongo with @Version.
4) Ensure retries re-run full use case (read + business logic + write), not just repository save.
5) Keep application layer free from Spring retry dependencies.

Mandatory implementation details:
- In PortfolioDocument:
  - Add @Version Long version.
  - Expose getter/setter.
- In PortfolioDocumentMapper:
  - Keep toDocument(entity) and add toDocument(entity, Long version).
- In MongoPortfolioRepository:
  - On getPortfolioById: load document and remember version in tx-scoped context.
  - On savePortfolio: map with expected version from context (fallback read if missing).
  - On create/save: refresh remembered version.
  - Use TransactionSynchronizationManager resource binding and cleanup after completion.
- In application module:
  - Create @RetryOnWriteConflict annotation with maxAttempts default 3.
  - Annotate write use-case methods:
    - deposit, withdraw, buyStock, sellStock.
- In bootstrap module:
  - Add aspect RetryOnWriteConflictAspect.
  - @Around on @RetryOnWriteConflict.
  - @Order(HIGHEST_PRECEDENCE) so retry wraps transactions.
  - Implement with RetryTemplate.
  - Retry on OptimisticLockingFailureException, ConcurrencyFailureException, MongoException.
  - Read maxAttempts from annotation.
- Dependencies:
  - bootstrap: spring-boot-starter-aop, spring-retry.
  - bootstrap test: testcontainers mongodb.

Tests to implement:
- Mongo transaction port contract test (extends AbstractTransactionPortContractTest).
- Mongo optimistic concurrency test with two concurrent tx workers reading same portfolio then saving.
  - Expect one success + one conflict exception.
- Retry aspect unit test:
  - success-after-retries and stops-at-max-attempts.
- Retry integration tests:
  - JPA profile: concurrent operations should not need extra retry attempts.
  - Mongo profile: force first-attempt stale concurrent reads and verify retries occur.
- Keep Mongo adapter test volume roughly aligned with JPA tests.

Acceptance criteria:
- No changes to domain model for version handling.
- Application module has no direct dependency on Spring Retry annotations/classes.
- Full retry behavior is validated by tests.
- All targeted tests pass with Maven.
```

---

## 9) Suggested verification commands

Representative commands used to validate the MongoDB adapter:

```bash
./mvnw -pl adapters-outbound-persistence-mongodb -am test \
  -Dtest=MongoPortfolioRepositoryContractTest,MongoTransactionRepositoryContractTest,MongoOptimisticLockingTest,MapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true
```

```bash
./mvnw -pl bootstrap -am test \
  -Dtest=RetryOnWriteConflictAspectTest,RetryOnWriteConflictJpaIntegrationTest,RetryOnWriteConflictMongoIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true
```

