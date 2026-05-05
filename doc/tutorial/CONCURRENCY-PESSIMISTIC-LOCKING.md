# Concurrency Control in HexaStock — Pessimistic Locking with JPA/MySQL and Optimistic Concurrency with MongoDB

---

## A. Introduction

### The business problem

A portfolio aggregate in HexaStock has invariants that must hold at every commit: the cash balance must never become negative, holdings must reflect the lots bought and sold in FIFO order, and the recorded transactions must be consistent with the resulting state. As soon as two requests modify the *same* portfolio at the same time — two withdrawals, two purchases, a deposit racing a buy — these invariants are at risk. Without explicit concurrency control, classic anomalies appear: lost updates, double spending, negative balances, and aggregates whose holdings disagree with the transaction log [Gray & Reuter, 1993; Berenson et al., 1995].

Concurrency control is therefore not a non-functional polish for HexaStock; it is part of the correctness of the domain itself.

### Two strategies, one project

HexaStock ships **two outbound persistence adapters** for the same `PortfolioPort`, and each one demonstrates a different — but equally legitimate — concurrency strategy:

1. **Pessimistic locking** in the JPA / MySQL adapter ([adapters-outbound-persistence-jpa](../../adapters-outbound-persistence-jpa)). The aggregate is loaded with a `SELECT ... FOR UPDATE` row lock, so concurrent transactions on the same portfolio are *serialised by the database*.
2. **Optimistic concurrency with retries** in the MongoDB adapter ([adapters-outbound-persistence-mongodb](../../adapters-outbound-persistence-mongodb)). The portfolio document carries a `@Version` field; conflicting writes are detected at write time and the *entire use case* is replayed by an application-level retry aspect.

This tutorial covers both because they are not redundant. They illustrate two different engineering trade-offs that any non-trivial system has to make consciously: prevent conflicts upfront and pay the cost of blocking, or allow conflicts and pay the cost of retries.

### What you will see

- How `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a Spring Data JPA query method translates to a `SELECT ... FOR UPDATE` against MySQL/InnoDB and serialises access to a portfolio row.
- How a Spring Data MongoDB `@Version` field plus a transaction-scoped “expected version” turns every save into a compare-and-swap.
- How the application-layer annotation `@RetryOnWriteConflict` and the `RetryOnWriteConflictAspect` close the loop on optimistic concurrency by re-running the full read-modify-write inside a fresh transaction.
- The real test scaffolding — Testcontainers MySQL and MongoDB, contract tests, integration tests in `bootstrap` — that verifies both strategies.

### Prerequisites

- **Java 21+** (project baseline)
- **Docker** running (Testcontainers spins up MySQL and MongoDB)
- **Maven** (wrapper included: `./mvnw`)
- Familiarity with JPA, Spring transactions and the basics of Spring Data MongoDB

---

## B. Strategy 1 — Pessimistic locking with JPA / MySQL

### Where the code lives

The relevant production classes are:

- [JpaPortfolioSpringDataRepository](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java) — Spring Data JPA repository declaring the locking query method.
- [JpaPortfolioRepository](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java) — secondary adapter implementing `PortfolioPort`, active under the `jpa` Spring profile.
- [PortfolioJpaEntity](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/entity/PortfolioJpaEntity.java) — JPA entity persisted by Hibernate.

The pessimistic lock is declared on the query method:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

The adapter uses it as the default read path for the aggregate, so every business operation that goes through `PortfolioPort.getPortfolioById` already takes the lock:

```java
@Override
public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
    return jpaSpringDataRepository.findByIdForUpdate(portfolioId.value())
            .map(PortfolioMapper::toModelEntity);
}
```

### What actually happens at the database

Hibernate translates `LockModeType.PESSIMISTIC_WRITE` into a `SELECT ... FOR UPDATE` against InnoDB. The semantics of that statement on MySQL 8 are precise [MySQL Reference Manual, §§ "Transaction Isolation Levels", "Locking Reads"]:

1. The statement bypasses the `REPEATABLE READ` snapshot and reads the *current committed* row.
2. It places an **exclusive next-key write lock** on the matched row.
3. Any concurrent transaction trying to acquire a conflicting lock on the same row blocks at the database, until the first transaction commits or rolls back (or the lock-wait timeout fires).
4. On release, the next waiter acquires the lock and proceeds.

This is a database-level guarantee, enforced by the storage engine, not by the JVM. The blocking is observable through standard MySQL diagnostics (`SHOW ENGINE INNODB STATUS`, `performance_schema.data_locks`).

### Why `@Transactional` matters here

The lock lifetime is bounded by the surrounding transaction. In HexaStock, transactions are declared on the application services, e.g. [CashManagementService](../../application/src/main/java/cat/gencat/agaur/hexastock/application/service/CashManagementService.java):

```java
@Transactional
public class CashManagementService implements CashManagementUseCase {

    @Override
    @RetryOnWriteConflict
    public void withdraw(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = getPortfolio(portfolioId);   // acquires PESSIMISTIC_WRITE
        portfolio.withdraw(amount);                        // domain logic
        portfolioPort.savePortfolio(portfolio);            // INSERT/UPDATE inside same tx
        transactionPort.save(Transaction.createWithdrawal(portfolioId, amount));
        // commit -> lock released
    }
}
```

The lock is acquired the instant `findByIdForUpdate` runs, held for the full duration of the domain logic and the writes, and released exactly at commit (or rollback). A second concurrent `withdraw` on the same portfolio cannot read the row until that point — which is precisely what makes the read-modify-write sequence safe.

`@RetryOnWriteConflict` is also present, but in the JPA path it is effectively a no-op: the contention is resolved by blocking, so conflicts that would cause a retry never occur in the first place. Its presence keeps the use case independent of the chosen adapter (see Section E).

### Where this is appropriate

The pessimistic strategy fits cases where:

- **Contention is expected to be non-trivial.** Serialising at the database avoids wasteful work that would otherwise be thrown away by retries.
- **Retries are expensive or undesirable.** If the use case calls external services, re-running it has a real cost.
- **Deterministic, observable serialisation is valuable.** This is the case in classroom, audit and debugging contexts: the blocking is explicit and easy to reason about.

### Tests for the JPA pessimistic lock

The lock itself is exercised by an adapter-level test that runs against a real MySQL instance through Testcontainers (H2 cannot reproduce InnoDB locking faithfully):

- [JpaPessimisticLockingTest](../../adapters-outbound-persistence-jpa/src/test/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPessimisticLockingTest.java) — verifies that `findByIdForUpdate` returns the entity managed inside the current persistence context (i.e., the lock is held), and that calling it on a non-existing id is safe.

The end-to-end behaviour — concurrent use cases that finish without retries because the database serialises them — is covered in `bootstrap`:

- [RetryOnWriteConflictJpaIntegrationTest](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictJpaIntegrationTest.java) — fires two concurrent `deposit` calls on the same portfolio and asserts:
  - the final balance is `200` (both deposits applied),
  - the retry counter equals `2` (one attempt per call, no retries triggered).

Run the JPA-side concurrency tests with:

```bash
./mvnw -pl adapters-outbound-persistence-jpa test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictJpaIntegrationTest test
```

(Both require Docker; Testcontainers will start MySQL automatically.)

---

## C. Strategy 2 — Optimistic concurrency with MongoDB and retries

### Why optimistic, not pessimistic

MongoDB does not provide a row-level pessimistic lock comparable to InnoDB's `SELECT ... FOR UPDATE`. The natural concurrency primitive in Spring Data MongoDB is the `@Version` field: every write becomes a compare-and-swap on `(_id, version)`. If the version observed at read time has changed by the time the write is issued, the driver throws `OptimisticLockingFailureException` and the write is rejected.

Therefore the MongoDB adapter does not block; it **detects** conflicts at write time and relies on an application-level retry to re-execute the use case. This is **optimistic concurrency with retries**, and it is a deliberate choice — not a workaround for missing features.

### Where the code lives

- [PortfolioDocument](../../adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/document/PortfolioDocument.java) — the persistence document, with `@Version Long version`.
- [MongoPortfolioRepository](../../adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/repository/MongoPortfolioRepository.java) — secondary adapter under the `mongodb` profile.
- [PortfolioDocumentMapper](../../adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/mapper/PortfolioDocumentMapper.java) — maps domain `Portfolio` ↔ `PortfolioDocument`, propagating the version.
- [MongoPersistenceConfig](../../adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/config/MongoPersistenceConfig.java) — wires a `MongoTransactionManager` so that the adapter can participate in Spring transactions.

The version field is plain Spring Data:

```java
@Document(collection = "portfolios")
public class PortfolioDocument {
    @Id private String id;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal balance;
    @Version private Long version;
    // ...
}
```

### The compare-and-swap, end to end

The domain `Portfolio` does *not* know about the version — that is a persistence concern. The adapter therefore has to remember, for the duration of a transaction, which version of each portfolio was loaded, so that the subsequent `save` can be issued against the same version. This is exactly what [MongoPortfolioRepository](../../adapters-outbound-persistence-mongodb/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/repository/MongoPortfolioRepository.java) does:

```java
@Override
public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
    return mongoSpringDataRepository.findById(portfolioId.value())
            .map(document -> {
                rememberVersion(document.getId(), document.getVersion());
                return PortfolioDocumentMapper.toModelEntity(document);
            });
}

@Override
public void savePortfolio(Portfolio portfolio) {
    String portfolioId = portfolio.getId().value();
    Long expectedVersion = resolveExpectedVersion(portfolioId);

    PortfolioDocument saved = mongoSpringDataRepository.save(
            PortfolioDocumentMapper.toDocument(portfolio, expectedVersion)
    );
    rememberVersion(saved.getId(), saved.getVersion());
}
```

The “expected version” is stored in a `Map<String, Long>` bound to the current Spring transaction via `TransactionSynchronizationManager`, and unbound automatically on transaction completion. The effect is:

1. The first `getPortfolioById` inside a transaction reads the document and remembers its version.
2. The matching `savePortfolio` issues the write with that exact version.
3. Spring Data MongoDB sends an `update` whose filter includes `{ _id: ?, version: ? }` and increments the version on success. If a concurrent transaction has already advanced the version, *zero documents match* and Spring translates the result into `OptimisticLockingFailureException`.

There is no row lock and no waiting. Concurrent readers of the same document proceed in parallel; the conflict is decided by the database at the moment of the write.

### How retries close the loop

Detecting a conflict is only half the strategy. The other half is recovering from it: the use case must be re-run from scratch — re-read the aggregate, re-apply the domain logic, re-issue the write — inside a *fresh* transaction. That is the responsibility of the [`@RetryOnWriteConflict`](../../application/src/main/java/cat/gencat/agaur/hexastock/application/annotation/RetryOnWriteConflict.java) annotation, declared in the `application` layer:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryOnWriteConflict {
    int maxAttempts() default 3;
}
```

It is purely a marker — no framework dependency. The actual retry mechanics live in [RetryOnWriteConflictAspect](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictAspect.java) in the `bootstrap` module:

```java
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RetryOnWriteConflictAspect {

    @Around("@annotation(cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict)")
    public Object retry(ProceedingJoinPoint joinPoint) {
        int maxAttempts = Math.max(1, resolveRetryAnnotation(joinPoint).maxAttempts());

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .retryOn(OptimisticLockingFailureException.class)
                .retryOn(ConcurrencyFailureException.class)
                .retryOn(MongoException.class)
                .traversingCauses()
                .fixedBackoff(50)
                .build();

        return retryTemplate.execute(context -> joinPoint.proceed());
    }
}
```

Two design points are worth highlighting:

- **Default policy:** up to **3 attempts** (1 initial + 2 retries) with a **fixed 50 ms backoff**, retrying on `OptimisticLockingFailureException`, `ConcurrencyFailureException` and `MongoException` (with cause traversal). Individual use cases can raise the cap with `@RetryOnWriteConflict(maxAttempts = N)`.
- **Aspect ordering:** the aspect is declared with `@Order(Ordered.HIGHEST_PRECEDENCE)` so that retries sit **outside** Spring's transaction interceptor. Each retry therefore runs in a brand-new transaction. The opposite ordering (transaction outside, retry inside) would re-execute the body inside an already doomed transaction, never roll back between attempts, and never succeed. The annotation's Javadoc spells this out explicitly.

The use cases that need this protection are annotated accordingly. Examples in the codebase:

- [CashManagementService.deposit](../../application/src/main/java/cat/gencat/agaur/hexastock/application/service/CashManagementService.java) and `withdraw` — both `@Transactional` and `@RetryOnWriteConflict`.
- [PortfolioStockOperationsService.buyStock](../../application/src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java) and `sellStock` — same treatment.

The same annotated method serves both adapters: under JPA the retry never fires (the lock has already serialised the work); under MongoDB it is the safety net that turns optimistic detection into a complete strategy.

### What triggers a retry, concretely

Inside a single use-case attempt, the sequence is:

1. `@Transactional` opens a transaction (Mongo session).
2. `getPortfolioById` reads the document and binds its version to the transaction-scoped map.
3. The domain mutates the in-memory `Portfolio`.
4. `savePortfolio` issues the versioned update.
5. If another transaction committed first, the update matches zero documents → Spring throws `OptimisticLockingFailureException` → the transaction rolls back.
6. The retry aspect catches it, waits 50 ms, and re-invokes the method, which now reads the new committed state and is virtually certain to succeed on the second try.

### Tests for MongoDB optimistic concurrency

The strategy is covered at three levels:

- **Aspect unit test** — [RetryOnWriteConflictAspectTest](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictAspectTest.java) wires the aspect with `AspectJProxyFactory` and verifies that a probe service which throws `OptimisticLockingFailureException` twice and then succeeds is invoked exactly 3 times, and that a probe that always conflicts stops after `maxAttempts`.
- **Adapter contract test** — [MongoOptimisticLockingTest](../../adapters-outbound-persistence-mongodb/src/test/java/cat/gencat/agaur/hexastock/adapter/out/persistence/mongodb/repository/MongoOptimisticLockingTest.java) runs against a real MongoDB replica set via Testcontainers (`SharedMongoDBContainer`), uses a `CyclicBarrier` to align two concurrent transactions on the same portfolio, and asserts that exactly one succeeds and the other fails with `OptimisticLockingFailureException` (or a wrapped `MongoException`).
- **End-to-end integration test** — [RetryOnWriteConflictMongoIntegrationTest](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/config/RetryOnWriteConflictMongoIntegrationTest.java) bootstraps the full Spring context with the `mongodb` profile, uses a `@MockitoSpyBean` on `MongoPortfolioRepository` to *force* both concurrent calls to read the same stale state, and asserts:
  - the final balance is `200` (both deposits eventually applied),
  - the retry counter (collected by [RetryAttemptCountingTestConfig](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/config/RetryAttemptCountingTestConfig.java)) is **strictly greater than 2** — i.e., at least one of the two use cases had to be retried.

Run them with:

```bash
./mvnw -pl adapters-outbound-persistence-mongodb test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictMongoIntegrationTest test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictAspectTest test
```

---

## D. Comparison

| Dimension | JPA / MySQL adapter | MongoDB adapter |
|---|---|---|
| Persistence technology | Relational (MySQL 8 / InnoDB) via Hibernate | Document store (MongoDB ≥ 4.0, replica set required for transactions) |
| Concurrency strategy | **Pessimistic locking** | **Optimistic concurrency control** |
| Detection vs. prevention | Conflicts are *prevented* by acquiring the lock before the read-modify-write | Conflicts are *detected* at write time by a versioned compare-and-swap |
| Mechanism | `@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE` (next-key write lock) | `@Version` on `PortfolioDocument` + transaction-scoped expected version + `OptimisticLockingFailureException` |
| Do transactions block? | Yes — the second transaction waits at the database until the first commits or rolls back | No — both transactions proceed in parallel; the loser fails at commit |
| Retry needed? | No (the lock has already serialised the work). `@RetryOnWriteConflict` is harmless and adapter-agnostic | Yes. `@RetryOnWriteConflict` (default 3 attempts, 50 ms fixed backoff) re-runs the full use case in a fresh transaction |
| Where conflicts surface | Lock-wait timeouts, deadlocks, connection-hold time | `OptimisticLockingFailureException` / `ConcurrencyFailureException` / `MongoException` |
| Strengths | Deterministic serialisation; no wasted work; observable and easy to teach | No blocking; better throughput when contention is rare; works naturally across application instances without a shared lock manager |
| Weaknesses | Connection-hold time grows with transaction duration; risk of deadlocks; potential thread/connection-pool pressure under sustained contention | Throughput collapses under high contention as the retry rate climbs; the use case must be safely re-runnable |
| Best fit in HexaStock | The teaching/demonstration adapter where the serialisation mechanics are the point | The default for environments where horizontal scalability and non-blocking behaviour matter, and per-portfolio contention is naturally low |

---

## E. Design rationale — why HexaStock keeps both

HexaStock includes both adapters not because one is universally superior, but because the *concurrency strategy is a property of the chosen adapter, not of the domain*. The hexagonal boundary makes that explicit:

- The domain `Portfolio` has no notion of locks, versions, or retries.
- The application services declare *intent* — `@Transactional` (this is a unit of work) and `@RetryOnWriteConflict` (this unit of work is safe to re-run on conflict). Both annotations are framework-agnostic in the application layer; the heavy lifting lives in `bootstrap`.
- Each outbound adapter chooses the concurrency primitive that is idiomatic for its underlying technology: row locks for InnoDB, `@Version` for MongoDB.

This separation is what allows the same use cases — `deposit`, `withdraw`, `buyStock`, `sellStock` — to run unmodified on top of either adapter, and to remain correct under contention in both cases.

The pragmatic guidance is the same as in the literature on transaction processing [Gray & Reuter, 1993; Berenson et al., 1995]: pick pessimistic locking when contention on the same aggregate is frequent and retries are expensive; pick optimistic concurrency with retries when contention is rare, retries are cheap, and you want to avoid blocking. Per-user portfolios sit firmly in the second regime; shared inventory or pool-style aggregates would sit in the first.

A short terminology note, since the three concepts are often conflated:

- **Pessimistic locking** — acquire a lock *before* the read so concurrent writers are forced to wait. Used by the JPA adapter.
- **Optimistic locking** — attach a version to each row/document and let the database reject a stale write at commit time. Used by the MongoDB adapter at the storage level.
- **Optimistic concurrency *with retries*** — the full strategy: optimistic detection plus an application-level policy that re-executes the unit of work on conflict. This is what HexaStock implements end-to-end on the MongoDB side via `@RetryOnWriteConflict`.

---

## F. Reproducing the experiments

There is no special Maven profile to enable: the concurrency tests are part of the regular test suite of the corresponding modules.

```bash
# JPA (MySQL via Testcontainers): pessimistic-lock contract + integration
./mvnw -pl adapters-outbound-persistence-jpa test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictJpaIntegrationTest test

# MongoDB (replica set via Testcontainers): optimistic-concurrency contract + integration
./mvnw -pl adapters-outbound-persistence-mongodb test
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictMongoIntegrationTest test

# Aspect unit test (no containers required)
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictAspectTest test
```

Docker must be running for the Testcontainers-based tests. Ports are assigned dynamically by Testcontainers, so no host-port configuration is required.

### A useful experiment: removing the lock from the JPA adapter

To observe the lost-update problem first hand, edit [JpaPortfolioRepository.getPortfolioById](../../adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java) and replace `findByIdForUpdate(...)` with the standard `findById(...)`:

```java
// Original (pessimistic lock):
return jpaSpringDataRepository.findByIdForUpdate(portfolioId.value())
        .map(PortfolioMapper::toModelEntity);

// Demonstration only — removes serialisation:
return jpaSpringDataRepository.findById(portfolioId.value())
        .map(PortfolioMapper::toModelEntity);
```

Re-run the JPA integration test; the concurrent-deposit scenario will start to expose lost updates under load. **Revert the change immediately** with:

```bash
git restore adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java
```

The MongoDB equivalent — removing `@Version` from `PortfolioDocument` — would similarly disable optimistic detection and let stale writes overwrite each other.

---

## G. Summary

- HexaStock implements **two concrete concurrency strategies**, one per persistence adapter, behind the same `PortfolioPort`:
  - JPA / MySQL → **pessimistic locking** via `@Lock(PESSIMISTIC_WRITE)` and `SELECT ... FOR UPDATE`.
  - MongoDB → **optimistic concurrency with retries** via Spring Data `@Version` and the `@RetryOnWriteConflict` aspect.
- Both strategies are correct. Their differences are in *how* conflicts are handled (block vs. detect-and-retry) and in their performance and operational profile, not in the guarantees they offer to the domain.
- The application layer is concurrency-aware but technology-agnostic: `@Transactional` defines the unit of work, `@RetryOnWriteConflict` declares it is safe to re-run, and the bootstrap aspect supplies the actual retry policy (3 attempts, 50 ms fixed backoff).
- The choice between the two strategies is not ideological. It is driven by contention frequency, retry cost, transaction duration, the chosen storage engine, and the deployment topology — exactly the criteria summarised in Section D.

---

## References

### Foundational works

- Berenson, H., Bernstein, P. A., Gray, J., Melton, J., O’Neil, E., and O’Neil, P. *A Critique of ANSI SQL Isolation Levels.* Proceedings of the 1995 ACM SIGMOD International Conference on Management of Data, 1995.
- Gray, J. and Reuter, A. *Transaction Processing: Concepts and Techniques.* Morgan Kaufmann, 1993.
- Haerder, T. and Reuter, A. *Principles of Transaction-Oriented Database Recovery.* ACM Computing Surveys, 15(4):287–317, December 1983.

### Framework and platform references

- *Hibernate ORM User Guide* — § "Locking". https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html
- *Jakarta Persistence (JPA) Specification* — § "Lock Modes". https://jakarta.ee/specifications/persistence/
- *MySQL 8.0 Reference Manual* — §§ "Transaction Isolation Levels" and "Locking Reads". https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html , https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html
- *Spring Data MongoDB Reference Documentation* — § "Optimistic Locking". https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html
- *MongoDB Manual* — § "Transactions" (replica-set requirement, retryable writes). https://www.mongodb.com/docs/manual/core/transactions/
- *Spring Framework Reference Documentation — Data Access* — § "Declarative transaction management". https://docs.spring.io/spring-framework/reference/data-access/transaction.html
- *Spring Retry* — `RetryTemplate`. https://github.com/spring-projects/spring-retry
