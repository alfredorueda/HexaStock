# ADR-016: Alternative MongoDB persistence adapter with optimistic locking

## Status

Accepted

## Context

The project originally ships a single outbound persistence adapter (JPA/MySQL, see
[ADR-008](ADR-008-mysql-jpa-with-domain-persistence-mapping.md) and
[ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md)).
For pedagogical and architectural reasons we want to demonstrate that the hexagonal
architecture truly enables **adapter interchangeability**: the same application core
must run on top of either a relational store or a document store, selected purely by
Spring profile, **without touching the domain or the application layer**.

The main technical question is how to protect the `Portfolio` aggregate from the
classic *read → decide → write* lost update when two concurrent requests mutate the
same portfolio. The JPA adapter uses **pessimistic locking** (`SELECT ... FOR UPDATE`).
MongoDB does not offer an equivalent row-lock API on a single node.

## Decision

Add a new outbound adapter module `adapters-outbound-persistence-mongodb` that:

1. **Maps the Portfolio aggregate to a single MongoDB document**, with `HoldingDocument`
   and `LotDocument` embedded as sub-documents. This matches DDD's aggregate-as-consistency-boundary
   rule and exploits MongoDB's single-document atomicity.
2. Uses **Spring Data MongoDB optimistic locking** via a `@Version Long version` field
   declared **only on the adapter-local `PortfolioDocument`** — never on the domain model.
3. Implements the existing `PortfolioPort` and `TransactionPort` interfaces unchanged.
4. Is activated by the Spring profile `mongodb`, mirroring the existing `jpa` profile
   gating on `@Profile("jpa")`.
5. Tracks the last-read version per portfolio id inside a thread-scoped map so that
   `savePortfolio(...)` can submit the correct `@Version` value without leaking versioning
   into the domain. One HTTP request = one thread in Spring MVC, so this is correct for
   the current deployment model.

Adapter selection remains profile-driven:

| Profile  | Active adapter beans                                        |
|----------|-------------------------------------------------------------|
| `jpa`    | `JpaPortfolioRepository`, `JpaTransactionRepository`        |
| `mongodb`| `MongoPortfolioRepository`, `MongoTransactionRepository`    |

Profile-specific `application-jpa.properties` and `application-mongodb.properties`
files exclude the autoconfigurations that do not belong to the active profile, so the
other database technology is not initialised.

## Alternatives considered

- **Option A — no concurrency control on Mongo (`save` overwrites):**
  Simplest, but allows lost updates on concurrent buys/sells/deposits.
  Rejected: silently wrong for financial data.
- **Option C — Mongo multi-document transactions (`@Transactional` + replica set):**
  Rejected as overkill. The aggregate already fits in one document, so
  single-document atomicity is sufficient; requiring a replica set would complicate
  deployment for no added safety.
- **Option D — custom compare-and-set via `Mongo​Template.updateFirst(query(_id+version), update(...).inc("version"))`:**
  Semantically identical to `@Version`, but re-implements a feature Spring Data already
  provides. Rejected on maintenance grounds.

## Consequences

**Positive:**
- Domain and application layers are unchanged; only a new adapter module and Spring
  profile-specific configuration are added.
- The Portfolio aggregate maps to a single document, which is both natural for DDD
  and atomic in MongoDB — no cross-collection transactions required.
- Versioning stays entirely inside the adapter's persistence model.
- Demonstrates hexagonal adapter flexibility as the primary pedagogical objective.

**Negative / honest limitations:**
- **Semantic difference vs. JPA adapter.** The JPA adapter **blocks** the losing writer
  until the winner commits. The Mongo adapter **fails** the losing writer with
  `OptimisticLockingFailureException`. Because we cannot modify the application layer
  to add retries, one of two truly concurrent requests on the same portfolio will
  surface an error to the caller under the `mongodb` profile. For a pedagogical
  project this trade-off is acceptable and explicitly documented; a production
  deployment would add a thin retry at the adapter or a cross-cutting retry
  interceptor outside the application layer.
- **Thread-scoped version cache.** The last-read `@Version` is kept in a `ThreadLocal`
  keyed by portfolio id. Correct under the standard one-thread-per-request model
  (Spring MVC/Tomcat). A future move to reactive or async execution would require a
  different propagation mechanism (e.g., Reactor Context or a per-request Spring bean).
- **Transactions collection.** `Transaction` is stored as one document per transaction
  in a separate collection (`portfolio_transaction`), matching the JPA layout. Because
  transactions are immutable, no version field is needed there.

## Repository evidence

- `adapters-outbound-persistence-mongodb/` — new Maven module implementing the ports.
- `PortfolioDocument.version` — `@Version Long` field, adapter-local.
- `MongoPortfolioRepository` — `@Profile("mongodb")`, `PortfolioPort` implementation.
- `bootstrap/src/main/resources/application-mongodb.properties` — profile activation config.
- `MongoPortfolioRepositoryContractTest` / `MongoTransactionRepositoryContractTest` —
  reuse the technology-agnostic `AbstractPortfolioPortContractTest` /
  `AbstractTransactionPortContractTest` under a Testcontainers-managed MongoDB instance.
- `MongoOptimisticLockingTest` — Mongo-specific test proving that a stale-version save
  raises `OptimisticLockingFailureException`, mirroring
  `JpaPessimisticLockingTest` at the semantic level.

## Relation to other specifications

- **ADR-008**: JPA adapter remains the primary/default adapter.
- **ADR-012**: pessimistic locking applies only to the JPA adapter; this ADR is the
  Mongo counterpart and documents the different concurrency semantics explicitly.
- **OpenAPI / Gherkin**: unchanged — behaviour observable through the API is identical,
  except under true race conditions where the Mongo profile may return an error to the
  losing concurrent writer.
