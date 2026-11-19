# MongoDB Adapter: Version Tracking and Retry Reference

**Scope:** the implementation on `main`. For a first explanation, read [the concurrency introduction](tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) and [the retry lesson](tutorial/concurrency/MONGODB-RETRIES.md).

## Locate each responsibility

| Location | Responsibility |
|---|---|
| `application.port.out.PortfolioPort` | Portfolio persistence capability. |
| `application.port.out.TransactionPort` | Financial transaction persistence capability. |
| `MongoPortfolioRepository` | Implements the portfolio port and tracks expected versions. |
| `MongoTransactionRepository` | Implements the transaction-record port. |
| `PortfolioDocument` | Persisted portfolio, including Spring Data `@Version`. |
| `PortfolioDocumentMapper` | Converts domain state to and from documents; accepts an explicit version for saving. |
| `MongoPersistenceConfig` | Supplies `MongoTransactionManager` under the `mongodb` profile. |
| `RetryOnWriteConflictAspect` | Repeats annotated use-case invocations according to the configured policy. |

Documents embed holdings and lots; financial transactions use their own representation. The domain remains unaware of MongoDB version fields.

## Track the version across mapping

In the normal transactional read-modify-write path:

1. `getPortfolioById` reads a `PortfolioDocument`.
2. The adapter remembers its ID and version in a map bound to `TransactionSynchronizationManager`.
3. The mapper returns a domain `Portfolio` without the persistence version.
4. After the domain update, `savePortfolio` resolves the remembered version and calls `toDocument(portfolio, expectedVersion)`.
5. Spring Data performs the versioned save. The adapter remembers the saved version.
6. Transaction completion unbinds the context.

Keeping the version that was read is essential. Looking up the newest version immediately before saving an old calculation would not make that calculation correct.

## Understand the fallback's limit

If no version is remembered, `resolveExpectedVersion` reads a version from storage. `rememberVersion` does nothing when transaction synchronization is unavailable. This fallback exists in the code, but it is not equivalent to preserving the original version across an arbitrary detached read-modify-write.

The concurrency reasoning therefore relies on the normal transactional use-case path. `getAllPortfolios` maps documents without populating this version context; it should not be treated as a substitute for the intended update-loading path.

## Retry policy and wiring

`@RetryOnWriteConflict` is a custom application annotation. The application has no Spring Retry dependency, although it does use Jakarta `@Transactional`. Bootstrap implements the retry with Spring Retry and orders it outside the transaction interceptor.

The default is **three total attempts**, with **50 ms fixed backoff**. The exception families include Spring concurrency failures and all `MongoException` instances, traversing causes. This is broader than retrying only transient write-conflict labels.

An annotation does not establish replay safety on its own. Review external effects, quote freshness, transaction propagation and the behavior when attempts are exhausted. The current mechanism is not HTTP request deduplication.

## Runtime configuration

Activate `mongodb` with one price-provider profile, for example `mongodb,mockfinhub`. The profile disables relational auto-configuration and uses the default URI:

```text
mongodb://localhost:27017/hexaStock?replicaSet=rs0
```

Override it with `SPRING_DATA_MONGODB_URI`. The bundled Compose service uses MongoDB 7.0 and initializes `rs0`; tests use a Testcontainers replica set. These learning configurations are distinct from a deployment and availability design.

## Evidence to inspect

| Test | What it contributes |
|---|---|
| `MongoPortfolioRepositoryContractTest` | Portfolio port behavior against MongoDB. |
| `MongoTransactionRepositoryContractTest` | Transaction-record port behavior. |
| `MapperTest` | Representation conversions, including version mapping. |
| `MongoOptimisticLockingTest` | Controlled competing updates and conflict detection. |
| `RetryOnWriteConflictAspectTest` | Retry success, attempt limits and policy behavior. |
| `RetryOnWriteConflictMongoIntegrationTest` | Concurrent deposits that force stale initial reads and require retries. |
| `RetryOnWriteConflictJpaIntegrationTest` | The comparable JPA scenario, expecting no extra attempts in that test. |

Equal test counts do not imply equal confidence. Compare exercised behaviors and assertions, especially rollback and failure cases.

With JDK 21 and Docker running:

```bash
./mvnw install -DskipTests
./mvnw -pl adapters-outbound-persistence-mongodb test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictAspectTest,RetryOnWriteConflictMongoIntegrationTest,RetryOnWriteConflictJpaIntegrationTest test
```

## Review or reproduce the design

For a learning exercise, first describe the race with two deposits. Then map each responsibility above to a class and a test. Verify that a rejected attempt rolls back before a new attempt reads state, and that version context is cleaned up after completion.

Do not retry only a stale save, substitute a fresh version into a stale calculation, or assume the aspect validates external side effects. These are the central correctness questions to explain in a review.

[Source: MongoPortfolioRepository](https://github.com/alfredorueda/HexaStock/blob/main/adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/repository/MongoPortfolioRepository.java) · [Source: retry aspect](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictAspect.java) · [ADR-016](architecture/adr/ADR-016-optimistic-locking-with-retry-for-mongodb-adapter.md)
