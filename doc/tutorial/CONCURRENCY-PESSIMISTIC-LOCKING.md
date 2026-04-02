# Concurrency Control with Pessimistic and Optimistic Locking

---

## Overview

### Purpose

Concurrent financial operations — such as two simultaneous withdrawals on the same portfolio — require correct concurrency control to prevent data corruption. Without it, race conditions can produce lost updates, negative balances, and inconsistent state.

This tutorial uses **pessimistic locking** (`SELECT ... FOR UPDATE`) as the teaching vehicle because it makes serialization explicit and observable: you can see exactly when a transaction blocks and when it proceeds. This makes concurrency bugs — and their solutions — deterministic and easy to demonstrate in tests.

**Pessimistic locking is not the only valid strategy.** Optimistic locking (version-based conflict detection with application-level retries) is a first-class alternative that is often preferable in production when contention is low and retries are acceptable. This tutorial covers both strategies and their trade-offs.

You will see:

- How concurrent withdrawals on the same portfolio can cause data corruption without any concurrency control.
- How `@Lock(LockModeType.PESSIMISTIC_WRITE)` prevents race conditions by serializing database access.
- How `@Version`-based optimistic locking detects conflicts at write time without holding database locks.
- How Java 21 virtual threads reduce the scalability cost of blocking I/O.
- The trade-offs — contention profile, retry cost, throughput, deadlock risk — that determine which strategy fits a given workload.

### Key Concepts

- **Concurrency control**: The general engineering problem of ensuring correctness when multiple transactions access shared mutable state simultaneously.
- **Pessimistic locking**: Prevents conflicts upfront by acquiring database-level row locks (`SELECT ... FOR UPDATE`) that block concurrent access until the owning transaction completes.
- **Optimistic locking**: Allows concurrent access without blocking, then detects conflicts at write time using a version field (`@Version`). Conflicting transactions fail and can be retried.
- **Transaction isolation**: Lock lifetime (pessimistic) or version check (optimistic) is tied to transaction boundaries (commit/rollback).
- **Virtual threads**: Lightweight JVM-managed threads (Java 21+) that park during blocking I/O, reducing the thread-pool cost of pessimistic locking.

### Prerequisites

- **Java 21+** (required for virtual threads)
- **Docker** running (for Testcontainers MySQL instance)
- **Maven** (wrapper included: `./mvnw`)
- Basic understanding of transactions and concurrency

---

## Quick Start

### Checkout the Teaching Branch

This experiment lives in a dedicated teaching branch. Check it out first:

```bash
git checkout teaching/concurrency-pessimistic-locking
```

### Run the Tests

The concurrency tests are **opt-in** via a Maven profile to avoid slowing down regular test runs.

**Run normal tests (excludes concurrency tests):**

```bash
./mvnw test
```

**Run ONLY concurrency tests:**

```bash
./mvnw test -Pconcurrency
```

Expected output: All tests should **pass**, demonstrating that pessimistic locking correctly serializes concurrent access to the same portfolio.

---

## Test Implementation

### Where the Tests Live

The concurrency integration tests are located in:

```
src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioConcurrencyIntegrationTest.java
```

This test class is tagged with `@Tag("concurrency")` so it only runs when the `concurrency` Maven profile is active.

### What the Tests Demonstrate

The test suite includes four key scenarios:

1. **Basic serialization test**: Two concurrent withdrawals of 700 from a balance of 1000. One succeeds (200 OK), one fails (409 Insufficient Funds). Final balance: 300.

2. **Double-spending detection**: Same scenario, but explicitly validates that removing the lock would expose a double-spending bug.

3. **High concurrency stress test**: 10 concurrent deposits and 5 concurrent withdrawals, verifying final balance consistency.

4. **Timing proof**: Captures completion timestamps to prove the second transaction waits for the first to commit.

Each test uses **Java 21 virtual threads** via `Executors.newVirtualThreadPerTaskExecutor()` to simulate concurrent REST API requests.

---

## The Experiment Switch

### Making Tests Pass (With Concurrency Control)

**Default behavior** (tests pass):

The repository uses pessimistic locking via the `findByIdForUpdate()` method in:

```
src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/springdatarepository/JpaPortfolioSpringDataRepository.java
```

**Key code snippet:**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

This annotation generates SQL like:

```sql
SELECT * FROM portfolio WHERE id = ? FOR UPDATE
```

The `FOR UPDATE` clause acquires a database row lock that lasts until the transaction commits or rolls back.

### Making Tests Fail (Without Any Concurrency Control)

**⚠️ For demonstration purposes only. Revert immediately after testing.**

To expose the race condition, remove all concurrency control by editing the repository adapter:

**File to edit:**

```
src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java
```

**Find the `getPortfolioById()` method and change:**

```java
// WITH PESSIMISTIC LOCKING (serializes concurrent access):
return jpaPortfolioSpringDataRepository.findByIdForUpdate(portfolioId)

// WITHOUT ANY CONCURRENCY CONTROL (exposes race condition):
return jpaPortfolioSpringDataRepository.findById(portfolioId)
```

Now run the tests again:

```bash
./mvnw test -Pconcurrency
```

**Expected result**: Tests **fail** because without any concurrency control, both concurrent transactions read `balance=1000` during the race window, both proceed with withdrawal, and the final balance becomes incorrect (e.g., -400 or corrupted). This is the lost-update problem that both pessimistic and optimistic locking exist to solve.

**⚠️ Important**: After demonstrating the failure, revert the change immediately:

```bash
git restore src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java
```

---

## How It Works

### Database Row-Level Locking

When a transaction executes `SELECT ... FOR UPDATE`:

1. The database engine (MySQL in this case) places an **exclusive lock** on the selected row(s).
2. Other transactions attempting to acquire the same lock will **block** at the database level.
3. The lock is held until the owning transaction commits or rolls back.
4. Once released, the next waiting transaction can acquire the lock and proceed.

This is a **database-level guarantee**, not a Java-level construct. The lock is enforced by the MySQL server, not by JDBC or Hibernate.

### What Happens to Concurrent Transactions

**Scenario**: Two concurrent requests try to withdraw 700 from the same portfolio with a balance of 1000.

**With pessimistic locking:**

1. Request 1 starts a transaction and executes `SELECT ... FOR UPDATE`. Lock acquired.
2. Request 2 starts a transaction and attempts `SELECT ... FOR UPDATE` on the same row. **Blocks at the database.**
3. Request 1 modifies the balance (1000 → 300) and commits. Lock released.
4. Request 2 now acquires the lock, reads the updated balance (300), and fails validation because 700 > 300.

**Without locking:**

1. Request 1 reads balance = 1000 (no lock).
2. Request 2 reads balance = 1000 (no lock, sees stale data).
3. Both proceed with withdrawal logic independently.
4. Final balance becomes -400 or corrupted (lost update problem).

### JDBC and Blocking I/O

When the second transaction attempts to acquire a lock held by the first:

- The database server cannot respond until the lock is available (or timeout occurs).
- The JDBC driver's `executeQuery()` call is **synchronous** and blocks waiting for the response.
- From the application's perspective, the thread appears blocked on I/O.

**Important**: The blocking happens at the network/database level, not because Java is holding a lock. The JDBC client is simply waiting for the MySQL server to reply.

### JPA and Hibernate Perspective

The `@Lock(LockModeType.PESSIMISTIC_WRITE)` annotation tells Hibernate to:

1. Append `FOR UPDATE` to the generated SQL query.
2. Execute the query within the current transaction context.
3. Hold the lock until the transaction boundary (commit or rollback).

**Lock lifetime is tied to the transaction:**

- If the transaction is never committed, the lock is never released (except on timeout).
- This is why `@Transactional` is critical for pessimistic locking to work correctly.

### Spring @Transactional

The `@Transactional` annotation on the use case service method defines the transaction boundary:

```java
@Transactional
public void withdraw(PortfolioId portfolioId, Money amount) {
    Portfolio portfolio = getPortfolio(portfolioId); // Lock acquired here
    portfolio.withdraw(amount);
    portfolioPort.savePortfolio(portfolio);
    // Lock released here when transaction commits
}
```

**Why this matters:**

- The lock is acquired when `getPortfolio()` calls `findByIdForUpdate()`.
- The lock is held for the **entire method duration** (including the withdrawal logic and save operation).
- The lock is only released when the method completes and Spring commits the transaction.

### Java 21 Virtual Threads

The tests use virtual threads to simulate concurrent REST API requests:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Response> future1 = executor.submit(() -> withdraw(portfolioId, 700));
    Future<Response> future2 = executor.submit(() -> withdraw(portfolioId, 700));
    // ...
}
```

**Why virtual threads matter for pessimistic locking:**

Pessimistic locking's main scalability cost is that blocked threads consume resources while waiting for a lock. Virtual threads reduce this cost significantly:

- When a virtual thread blocks on I/O (e.g., waiting for the database lock), the JVM **parks** (unmounts) it from its carrier thread.
- The carrier thread becomes available to run other virtual threads.
- This allows thousands of virtual threads to wait for database locks without exhausting OS-level thread resources.

**Important**: Virtual threads do not change the database locking behavior. The lock is still enforced by MySQL. Virtual threads reduce the *thread-pool cost* of blocking, but they do not eliminate other costs of pessimistic locking — such as connection hold time, deadlock risk, or reduced throughput under high contention.

---

## Teaching Branch Instrumentation

### The Conditional Sleep

This branch contains teaching-only instrumentation to widen the race window. The code is located in:

```
src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioManagementService.java
```

**Relevant snippet (inside the `withdraw()` method):**

```java
if (environment.acceptsProfiles(Profiles.of("test-concurrency"))) {
    try {
        Thread.sleep(200); // Widens race window
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
portfolio.withdraw(amount);
```

This sleep is placed **after reading the portfolio** but **before modifying it**.

### Why This Exists

**Without the sleep:**

- The race window is microseconds (time between read and write).
- Concurrent tests may pass inconsistently because one transaction often completes before the other even starts.

**With the sleep:**

- The race window is widened to 200ms.
- Both concurrent requests reliably read the same balance (1000) during this window.
- This makes the concurrency bug deterministic and easy to demonstrate in tests.

**Activation:**

The sleep only executes when the `test-concurrency` Spring profile is active. The concurrency tests activate this profile:

```java
@ActiveProfiles({"test", "jpa", "mockfinhub", "test-concurrency"})
```

### Production Warning

**⚠️ This instrumentation must NEVER be merged into main or deployed to production.**

Proper concurrency control (pessimistic or optimistic locking) handles race conditions correctly without timing manipulation. Artificial delays:

- Slow down the system unnecessarily.
- Do not reflect real-world timing characteristics.
- Are acceptable only for pedagogical demonstrations.

---

## Optimistic Locking: A Complementary Strategy

The previous sections demonstrated pessimistic locking because it makes serialization visible. This section presents **optimistic locking** — a concurrency-control strategy that detects conflicts at write time rather than preventing them upfront. In many production workloads, optimistic locking is the preferred approach.

### What Is Optimistic Locking

**Optimistic locking** assumes that conflicts are **rare** and allows multiple transactions to read and process the same data concurrently without blocking. Instead of acquiring database locks upfront, it **detects** conflicts at commit time and fails the transaction if the data was modified by another transaction.

**Core principle:**

- Each entity has a **version field** (e.g., an incrementing counter or timestamp).
- When reading data, the transaction remembers the version.
- When writing, the transaction includes the version in the `WHERE` clause.
- If the version changed (meaning another transaction modified the row), the update affects 0 rows and the transaction fails with an `OptimisticLockException`.

This approach eliminates database row locks entirely, allowing concurrent reads and processing without blocking. Conflicts are resolved by retrying the failed transaction.

### How It Works

**Step-by-step flow:**

1. **Transaction 1** reads portfolio with `balance=1000, version=5`.
2. **Transaction 2** reads the same portfolio with `balance=1000, version=5` (no blocking).
3. Transaction 1 processes withdrawal, calculates `balance=300`, and commits with SQL:
   ```sql
   UPDATE portfolio SET balance=300, version=6 WHERE id=? AND version=5
   ```
   The update succeeds (1 row affected). Version increments to 6.

4. Transaction 2 attempts to commit with SQL:
   ```sql
   UPDATE portfolio SET balance=300, version=6 WHERE id=? AND version=5
   ```
   The update fails (0 rows affected) because version is now 6, not 5.

5. Hibernate throws `OptimisticLockException`. Application can retry from step 2.

**Key insight:** No database locks are held during processing. The conflict is detected atomically at write time.

### JPA Implementation with @Version

**Add a version field to your JPA entity:**

```java
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {
    @Id
    private String id;
    
    private BigDecimal balance;
    
    @Version
    private Long version; // Automatically managed by JPA
    
    // ...existing fields and methods...
}
```

**How JPA handles versioning:**

- **On read**: JPA loads the current version value into the entity.
- **On update**: JPA automatically:
  - Increments the version field.
  - Adds `WHERE version = ?` to the `UPDATE` statement.
  - Throws `OptimisticLockException` if 0 rows are affected.

**No changes needed in repository code:**

```java
@Repository
public interface JpaPortfolioSpringDataRepository extends JpaRepository<PortfolioJpaEntity, String> {
    // Standard findById() is sufficient - no @Lock annotation needed
}
```

### Conflict Detection and Resolution

**What happens when two transactions conflict:**

**Scenario:** Two concurrent requests withdraw 700 from balance=1000, version=1.

1. Request 1 reads: `balance=1000, version=1`
2. Request 2 reads: `balance=1000, version=1` (no blocking)
3. Request 1 calculates new balance=300, commits successfully:
   ```sql
   UPDATE portfolio SET balance=300, version=2 WHERE id='X' AND version=1
   ```
   Result: 1 row updated. Database now has `balance=300, version=2`.

4. Request 2 calculates new balance=300, attempts commit:
   ```sql
   UPDATE portfolio SET balance=300, version=2 WHERE id='X' AND version=1
   ```
   Result: 0 rows updated (version is now 2, not 1).

5. Hibernate detects 0 rows affected and throws `OptimisticLockException`.

**At the application layer:**

- Request 1 returns 200 OK.
- Request 2 catches `OptimisticLockException` and returns 409 Conflict (or retries).

### Application-Level Retry Logic

**Basic retry pattern:**

```java
@Service
public class PortfolioService {
    
    private static final int MAX_RETRIES = 3;
    
    public void withdrawWithRetry(PortfolioId portfolioId, Money amount) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                withdrawOnce(portfolioId, amount);
                return; // Success
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new ConcurrentModificationException("Too many conflicts", e);
                }
                // Optional: exponential backoff
                sleep(attempt * 50);
            }
        }
    }
    
    @Transactional
    public void withdrawOnce(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
        portfolio.withdraw(amount);
        portfolioRepository.save(portfolio);
    }
}
```

**Key considerations:**

- **Idempotency**: Ensure retry logic doesn't cause duplicate operations (e.g., use idempotency keys).
- **Retry limits**: Prevent infinite retry loops under high contention.
- **Backoff strategy**: Add delays between retries to reduce contention spikes.
- **User feedback**: After multiple failures, return 409 Conflict or 503 Service Unavailable.

### Scalability Trade-offs

Optimistic locking has different scalability characteristics than pessimistic locking. Neither is universally superior — the right choice depends on the contention profile.

**1. No database row blocking:**

- Pessimistic: Transaction 2 blocks waiting for Transaction 1's lock (potentially 100-500ms).
- Optimistic: Transaction 2 proceeds immediately with its own read and processing.

**2. No lock hold time:**

- Pessimistic: Lock held for entire transaction duration (read + business logic + write). Long transactions amplify contention.
- Optimistic: No lock held during processing. Version check happens atomically during the `UPDATE`.

**3. Higher throughput under low contention:**

- If conflicts are rare (typical for user-specific aggregates like portfolios), most transactions succeed on first attempt.
- No threads blocked waiting for locks → better CPU utilization.

**4. Better fit for distributed systems:**

- Optimistic locking works across multiple application instances without coordination.
- Database only serializes at the moment of write, not during processing.

**5. Degradation under high contention:**

- Optimistic: retry rate increases, adding latency, but the system remains responsive and no threads are blocked.
- Pessimistic: blocked threads hold database connections longer, which can lead to connection pool exhaustion under sustained high contention. (Virtual threads reduce the OS-thread cost of blocking, but do not eliminate the connection-hold problem.)

**Portfolio operations as a realistic example:**

- A user portfolio is typically modified by that user alone.
- Concurrent writes to the same portfolio are rare (seconds or minutes apart, not milliseconds).
- When conflicts do occur, a single retry usually succeeds.
- This low-contention profile makes optimistic locking a strong default choice.

### Choosing Between Pessimistic and Optimistic Locking

The right strategy depends on the workload, not on an ideological preference. The table below summarizes the key selection criteria:

| Criterion | Favours pessimistic | Favours optimistic |
|---|---|---|
| **Contention frequency** | High (same rows accessed concurrently by many transactions) | Low (same row rarely accessed concurrently) |
| **Cost of retry** | High (expensive computation, external API calls, side effects) | Low (short transactions, idempotent operations) |
| **Transaction duration** | Short (lock hold time is brief) | Any (no lock held during processing) |
| **Deadlock risk tolerance** | Acceptable (deadlocks can be detected and retried) | Preferred to avoid (no locks, no deadlocks) |
| **Throughput / scalability** | Acceptable under moderate load | Critical (high-throughput, distributed systems) |
| **Deployment topology** | Single application instance or shared database | Multiple instances, no shared lock manager |
| **User experience** | Blocking latency is acceptable | Immediate response preferred; retries invisible to user |
| **Observability / teaching** | Serialization is explicit and easy to demonstrate | Conflict detection requires version-field inspection |

**Financial systems in practice** use different strategies for different operations. There is no single rule:

- User portfolios and account balances (low contention per account): optimistic locking with retry logic is a common default.
- Shared inventory or pool resources with high contention: pessimistic locking may be more appropriate to avoid excessive retries.
- Trading platforms: often combine optimistic locking with event sourcing, eventual consistency, or saga patterns depending on the specific workflow.
- Regulatory or audit-critical operations: correctness is required, but both strategies provide it — the choice depends on contention and retry cost, not on the regulatory requirement itself.

---

## Troubleshooting

### Docker Not Running

**Error**: Testcontainers fails to start MySQL container.

**Solution**: Ensure Docker Desktop is running:

```bash
docker ps
```

If Docker is not running, start it and retry.

### Port Conflicts

**Error**: Port already in use (e.g., 3306).

**Solution**: Testcontainers uses random ports by default. If you see port conflicts, stop conflicting services or let Testcontainers assign a different port automatically.

### Slow Machines and Timeouts

**Error**: Tests fail with timeout exceptions.

**Solution**: Increase timeout values in the test if needed. The current timeouts are generous (10-15 seconds), but extremely slow machines may need adjustment.

### Running a Single Test Class

**Run only the concurrency test class:**

```bash
./mvnw test -Dtest=PortfolioConcurrencyIntegrationTest
```

**Or in your IDE:** Right-click the test class and select "Run" or "Debug".

### Maven Profile Not Active

**Error**: Concurrency tests don't run.

**Solution**: Ensure you use the `-Pconcurrency` flag:

```bash
./mvnw test -Pconcurrency
```

---

## Summary

**The engineering problem:** Concurrent access to shared mutable state — such as a portfolio balance — requires explicit concurrency control. Without it, race conditions produce lost updates and corrupted data.

**What this tutorial demonstrated:**

This tutorial used **pessimistic locking** as the teaching vehicle because `SELECT ... FOR UPDATE` makes serialization explicit: you can observe exactly when a transaction blocks and when it proceeds. The conditional sleep widens the race window, making the concurrency bug deterministic and reproducible in tests.

- `@Lock(PESSIMISTIC_WRITE)` translates to `SELECT ... FOR UPDATE` at the SQL level.
- Database row locks (not Java locks) serialize access to the same portfolio.
- `@Transactional` defines lock lifetime: acquired at query, released at commit/rollback.
- Java 21 virtual threads reduce the thread-pool cost of blocking I/O, but do not eliminate other costs of pessimistic locking (connection hold time, deadlock risk).
- Concurrency tests are opt-in via the `-Pconcurrency` Maven profile.

**Both strategies are correct; the right choice depends on context:**

- **Pessimistic locking** prevents conflicts upfront. It is a strong fit when contention is frequent, retries are expensive, or serialization must be guaranteed before processing.
- **Optimistic locking** detects conflicts at write time. It is a strong fit when contention is rare, retries are cheap, and throughput or scalability is a priority.

Professional engineering requires understanding both mechanisms - how they work, when each is appropriate, and what trade-offs they carry. The selection criteria (contention profile, retry cost, transaction duration, deployment topology, deadlock tolerance) are discussed in detail in the "Choosing Between Pessimistic and Optimistic Locking" section above.

**Experiment with confidence:**

1. Run tests with locking enabled → tests pass, balance correct.
2. Remove the lock temporarily → tests fail, balance corrupted.
3. Revert the change → tests pass again.

This hands-on experience demonstrates the concurrency problem that both pessimistic and optimistic locking strategies exist to solve.