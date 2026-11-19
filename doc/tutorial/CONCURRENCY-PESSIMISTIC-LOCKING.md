# Concurrency: Two Requests, One Portfolio

**Goal:** explain why correct single-request arithmetic is not enough when requests overlap.

**Prerequisite:** [the sale service](sellStocks/SELL-STOCK-05-ORCHESTRATION.md). Begin with the example below; database-specific details follow on separate pages.

## The lost-update problem

A portfolio starts with 100.00. Two requests each deposit 50.00:

| Step | Request A | Request B |
|---|---|---|
| Read | Sees 100.00 | Sees 100.00 |
| Calculate | Computes 150.00 | Computes 150.00 |
| Save without coordination | Writes 150.00 | Overwrites with 150.00 |

The expected balance is 200.00, but one update has been lost. Both calculations were individually correct. The problem is the relationship between the reads and writes.

A database transaction groups operations for commit or rollback. The isolation and locking strategy determines how overlapping transactions interact. Merely adding `@Transactional` does not explain or prove the concurrency behavior.

## Study two strategies

| Stage | Mechanism | What to learn |
|---|---|---|
| [1. JPA and MySQL](concurrency/JPA-LOCKING.md) | Lock the portfolio row before updating. | Why a second writer waits, and what can still fail. |
| [2. MongoDB and retries](concurrency/MONGODB-RETRIES.md) | Detect a stale write and repeat the use case. | Why a retry must reload state inside a fresh transaction. |
| [Adapter reference](../mongodb-adapter-optimistic-write-and-retry.md) | Version context, mappers and configuration. | How the MongoDB implementation preserves the expected version. |

Both adapters implement `PortfolioPort`. The domain's FIFO calculation contains no database lock or version field. The application declares a transaction boundary and uses a custom retry annotation; bootstrap and the persistence adapter implement those mechanisms.

## Compare costs, not labels

A pessimistic lock can make a competing writer wait while the first request performs its work. An optimistic strategy may discard work and repeat it when a conflict is detected. Neither strategy is universally faster.

Measure transaction duration, contention on the same portfolio, quote-provider latency, lock waits and retry counts. A system with many users can still have a heavily contended shared portfolio. Do not infer a concurrency regime from the domain name alone.

The current sale service fetches the quote after loading the portfolio. Under JPA, that can extend lock duration. Under MongoDB retry, the quote lookup can happen again and may return a different price. These are observable consequences of the implemented ordering.

## Verify the implemented behavior

Use JDK 21 and Docker for the database-backed tests. From the repository root:

```bash
./mvnw install -DskipTests
./mvnw -pl bootstrap -Dtest=RetryOnWriteConflictAspectTest,RetryOnWriteConflictJpaIntegrationTest,RetryOnWriteConflictMongoIntegrationTest test
```

Installing first makes sibling modules and shared test artifacts available. `RetryOnWriteConflictAspectTest` checks the retry policy without a database; the other two tests exercise real databases through Testcontainers.

Do not equate one passing schedule with proof of every concurrent execution. Read which conditions each test forces and which outcomes it asserts.

## Checkpoint

If a stale write is rejected, should the application simply save the same calculated balance again with a newer version?

**Answer:** no. That balance was calculated from old state. Reload the portfolio and rerun the business operation so the new result includes the other committed change.

[Continue: JPA locking](concurrency/JPA-LOCKING.md)

## References

### Foundational works

* Berenson, H., Bernstein, P. A., Gray, J., Melton, J., O’Neil, E., and O’Neil, P. _A Critique of ANSI SQL Isolation Levels._ Proceedings of the 1995 ACM SIGMOD International Conference on Management of Data, 1995.
* Gray, J. and Reuter, A. _Transaction Processing: Concepts and Techniques._ Morgan Kaufmann, 1993.
* Haerder, T. and Reuter, A. _Principles of Transaction-Oriented Database Recovery._ ACM Computing Surveys, 15(4):287–317, December 1983.

### Framework and platform references

* _Hibernate ORM User Guide_ — § "Locking". https://docs.jboss.org/hibernate/orm/current/userguide/html\_single/Hibernate\_User\_Guide.html
* _Jakarta Persistence (JPA) Specification_ — § "Lock Modes". https://jakarta.ee/specifications/persistence/
* _MySQL 8.0 Reference Manual_ — §§ "Transaction Isolation Levels" and "Locking Reads". https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html , https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html
* _Spring Data MongoDB Reference Documentation_ — § "Optimistic Locking". https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html
* _MongoDB Manual_ — § "Transactions" (replica-set requirement, retryable writes). https://www.mongodb.com/docs/manual/core/transactions/
* _Spring Framework Reference Documentation — Data Access_ — § "Declarative transaction management". https://docs.spring.io/spring-framework/reference/data-access/transaction.html
* _Spring Retry_ — `RetryTemplate`. https://github.com/spring-projects/spring-retry
