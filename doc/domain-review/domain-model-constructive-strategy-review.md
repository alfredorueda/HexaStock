# 3. Plan runtime improvements without weakening the model

**Goal:** turn a source observation into a small, verifiable proposal. This page describes `main` and possible changes; it contains no measured throughput claims.

## Establish the actual baseline

| Concern | Current behavior | Investigation |
|---|---|---|
| Single portfolio lookup under JPA | `getPortfolioById` uses `findByIdForUpdate`, including reads/reporting | Measure lock duration and whether read requirements need it |
| Portfolio listing | `findAll` maps complete aggregates, without that explicit locking lookup | Measure rows loaded versus fields returned |
| Trading | Load, quote lookup, domain mutation, portfolio save, transaction save | Measure provider time while the transaction is open |
| History | Full unpaged list; supplied type filter ignored | Define ordering/filter/page contracts before implementation |
| Reporting | Full portfolio and history plus sequential quote lookups | Compare alternatives in the reporting lessons |
| JPA fetching | Collection `@BatchSize(size = 30)` already present | Inspect actual SQL and initialization order |
| MongoDB | Versioned writes and retry infrastructure already present | Verify original-version tracking and fresh attempts |

Read [the concurrency lessons](../tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) before changing locking. Pessimistic locks can time out or deadlock. Optimistic checks reject stale writes but do not guarantee successful retries. Current retry infrastructure allows three total attempts with a fixed 50 ms delay; it is not a new exponential-backoff proposal.

## Proposal A: shorten a trading transaction

Fetching a quote before opening the database transaction may reduce lock-held network time. It also changes quote age, retry semantics, and failure order: an unavailable provider could fail before an unknown portfolio is detected. Decide those contracts first.

A safe design sketch is an unannotated coordinator calling a **separate proxied transactional collaborator**, or an infrastructure transaction wrapper, which loads current state, checks rules, and persists both records. Merely moving annotated code into another method of the same object does not activate Spring's proxy interception on self-invocation. See [Spring's transaction annotation documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).

Retaining a class-level transaction on the outer coordinator also defeats the intended separation. Verify the boundary in an integration test, including commit-time failure. Keep Spring-specific transaction machinery outside the application core if preserving the current dependency rules.

Specify whether a retry uses the original quote or obtains a new one. Neither choice provides HTTP request idempotency. If clients can retry a timed-out request, a separate persisted request-identity contract is needed to avoid duplicate business operations.

## Proposal B: introduce query-shaped reads

Portfolio list/detail responses need only summary fields. A core-owned query result can avoid hydrating holdings and lots. The persistence adapter maps its framework-specific projection into that result; the REST adapter maps it to a DTO. Do not return REST DTOs directly from an outbound persistence adapter.

For history, define stable ordering, a tie-breaker, page bounds, filter semantics, and response metadata. Offset pagination can still become expensive at large offsets; a page-size limit bounds returned rows, not all query cost. Keep Spring `Pageable` out of core ports unless deliberately changing their dependency contract.

A non-locking query may be appropriate, but consistency requirements still matter. Removing a write lock is not synonymous with obtaining a coherent snapshot across several queries. Jakarta `@Transactional` has no Spring-style `readOnly` attribute, and a read-only hint does not override an explicit lock request.

## Proposal C: change fetching or saving

Batch fetching can reduce round trips. The estimate `2 + ceil(H / 30)` for H holdings assumes eligible uninitialized collections are available for batching in the same persistence context; it is not an asserted bound for every execution. A round-trip mapping test checks values, not SQL counts.

A nested Portfolio → Holding → Lot join repeats parent columns for child rows. It is not automatically a Cartesian product of all holdings and all lots; independent sibling collections can multiply rows. Compare real result size and plans before selecting joins or entity graphs.

Updating a managed JPA graph may avoid rebuilding detached objects. Targeted updates can reduce work for cash operations, but must retain validation, concurrency checks, history atomicity, and persistence-context consistency. Never save a partial graph through a full replacement contract.

Adding a root `@Version` to JPA requires preserving the originally observed version and ensuring every relevant child mutation participates in the root's conflict protocol. Simply adding an annotation does not prove aggregate-wide version coverage. Test child-only conflicts, detached reconstruction, rollback, and retry.

## Proposal D: change quote retrieval

A per-call sleep is not a global rate limiter. Parallel requests and several application instances can exceed provider quotas despite sleeping. The set-based port method defaults to sequential calls, not a network batch API. Caffeine caching reduces repeated requests but has per-process state and an explicit freshness trade-off.

Before adding concurrency, measure provider latency and quota requirements, define partial failure behavior, and choose bounded dispatch. A shared cache, circuit breaker, or asynchronous pipeline should answer a specific requirement rather than appear as a mandatory architecture upgrade.

**Checkpoint:** choose one proposal and state its observable behavior changes, required tests, measurement method, and rollback plan. Do not claim a percentage speedup before measuring it.

Next: [measure aggregate size](AGGREGATE-SIZE-GUIDELINES.md).
