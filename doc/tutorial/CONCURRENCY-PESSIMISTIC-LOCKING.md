# Concurrency Control with Pessimistic Database Locking

## Table of Contents

- [Overview](#overview)
  - [Purpose](#purpose)
  - [Key Concepts](#key-concepts)
  - [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
  - [Checkout the Teaching Branch](#checkout-the-teaching-branch)
  - [Run the Tests](#run-the-tests)
- [Test Implementation](#test-implementation)
  - [Where the Tests Live](#where-the-tests-live)
  - [What the Tests Demonstrate](#what-the-tests-demonstrate)
- [The Experiment Switch](#the-experiment-switch)
  - [Making Tests Pass (With Locking)](#making-tests-pass-with-locking)
  - [Making Tests Fail (Without Locking)](#making-tests-fail-without-locking)
- [How It Works](#how-it-works)
  - [Database Row-Level Locking](#database-row-level-locking)
  - [What Happens to Concurrent Transactions](#what-happens-to-concurrent-transactions)
  - [JDBC and Blocking I/O](#jdbc-and-blocking-io)
  - [JPA and Hibernate Perspective](#jpa-and-hibernate-perspective)
  - [Spring @Transactional](#spring-transactional)
  - [Java 21 Virtual Threads](#java-21-virtual-threads)
- [Teaching Branch Instrumentation](#teaching-branch-instrumentation)
  - [The Conditional Sleep](#the-conditional-sleep)
  - [Why This Exists](#why-this-exists)
  - [Production Warning](#production-warning)
- [Troubleshooting](#troubleshooting)
- [Summary](#summary)

---

## Overview

### Purpose

This tutorial demonstrates why **pessimistic database locking** is essential for concurrent financial operations in the HexaStock application. You will see:

- How concurrent withdrawals on the same portfolio can cause data corruption without proper locking.
- How `@Lock(LockModeType.PESSIMISTIC_WRITE)` prevents race conditions by serializing database access.
- How Java 21 virtual threads make blocking I/O scalable without exhausting system resources.
- The difference between correct serialized behavior (with locking) and incorrect concurrent behavior (without locking).

### Key Concepts

- **Pessimistic locking**: Database-level row locks that prevent concurrent modifications.
- **SELECT ... FOR UPDATE**: SQL construct that acquires an exclusive lock on selected rows.
- **Transaction isolation**: Lock lifetime is tied to transaction boundaries (commit/rollback).
- **Virtual threads**: Lightweight JVM-managed threads that park during blocking I/O operations.

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

Expected output: All tests should **pass**, demonstrating that pessimistic locking correctly serializes concurrent operations.

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

### Making Tests Pass (With Locking)

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

### Making Tests Fail (Without Locking)

**⚠️ For demonstration purposes only. Revert immediately after testing.**

To see the concurrency bug exposed, edit the repository adapter:

**File to edit:**

```
src/main/java/cat/gencat/agaur/hexastock/adapter/out/persistence/jpa/repository/JpaPortfolioRepository.java
```

**Find the `getPortfolioById()` method and change:**

```java
// WITH LOCKING (correct):
return jpaPortfolioSpringDataRepository.findByIdForUpdate(portfolioId)

// WITHOUT LOCKING (demonstrates bug):
return jpaPortfolioSpringDataRepository.findById(portfolioId)
```

Now run the tests again:

```bash
./mvnw test -Pconcurrency
```

**Expected result**: Tests **fail** because both concurrent transactions read `balance=1000` during the race window, both proceed with withdrawal, and the final balance becomes incorrect (e.g., -400 or corrupted).

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
public void withdraw(String portfolioId, Money amount) {
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

**Why virtual threads matter:**

- When a virtual thread blocks on I/O (e.g., waiting for the database lock), the JVM **parks** (unmounts) it from its carrier thread.
- The carrier thread becomes available to run other virtual threads.
- This allows thousands of virtual threads to wait for database locks without exhausting OS-level thread resources.

**Important**: Virtual threads do not change the database locking behavior. The lock is still enforced by MySQL. Virtual threads only improve server-side scalability by making blocking I/O cheap.

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

**Key takeaways:**

- **Database row locks** (not JDBC or Java locks) prevent concurrent modifications to the same portfolio.
- **@Lock(PESSIMISTIC_WRITE)** translates to `SELECT ... FOR UPDATE` at the SQL level.
- **@Transactional** defines lock lifetime (acquired at query, released at commit/rollback).
- **Virtual threads** make blocking I/O scalable by parking threads during waits, but do not change database locking semantics.
- **Concurrency tests are opt-in** via the `-Pconcurrency` Maven profile to avoid slowing down regular builds.
- **Teaching instrumentation** (conditional sleep) exists only in this branch to widen race windows for demonstration purposes.

**Experiment with confidence:**

1. Run tests with locking enabled → tests pass, balance correct.
2. Remove the lock temporarily → tests fail, balance corrupted.
3. Revert the change → tests pass again.

