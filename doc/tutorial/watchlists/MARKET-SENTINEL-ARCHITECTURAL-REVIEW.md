# Review the proposed Market Sentinel design

This review evaluates the [watchlist assignment](WATCHLISTS-MARKET-SENTINEL.md), not a deployed feature. `main` has no watchlist implementation, workload measurements, or watchlist-specific ADRs. ADR-016 describes MongoDB retries, not a watchlist bounded context.

Start after the four lessons. The review deliverable is a short decision record stating the required behavior, evidence, alternatives, and limitations.

## What the baseline makes clear

The proposed design separates aggregate edits, projection queries, quote retrieval, and notification effects. A thin scheduler can invoke the same application use case as a test. This supports focused tests and replaceable adapters. It does not establish throughput, reliability, or deployment readiness.

Separate read and write models can use one store. CQRS does not inherently require events or event sourcing; separate stores introduce additional synchronization concerns. Even a shared store allows races across separate queries and external effects.

## Questions that change the design

| Requirement | Evidence to collect | Possible response |
|---|---|---|
| Fresh observations | Provider timestamps, cache age, polling duration | Explicit maximum age and stale-quote behavior |
| Quota compliance | Actual provider request limits and cache misses | Bounded concurrency, rate limiting, retry policy |
| Timely detection | Distribution of cycle duration and missed transient conditions | Shorter polling or a streaming feed, if justified |
| Many matching alerts | Maximum result size and notification latency | Pagination, bounded dispatch, fairness across tickers |
| Multiple workers | Overlap and duplicate-claim tests | Partition ownership or atomic claims with recovery |
| Reliable delivery | Crash-window tests and provider idempotency contract | Durable delivery intent and deduplicated retries |

A stale price can change whether an alert matches. Provider limits can matter even for a small ticker set. A later polling cycle cannot necessarily recover a condition that was true only during an outage.

## Compare alternatives using a measured bottleneck

* A batched database query can reduce per-ticker round trips. Join a table or values set of observed `(ticker, price)` pairs and apply the **threshold inequality**; tuple equality alone does not evaluate a threshold condition.
* A materialized projection can reduce joins, but it needs a refresh/rebuild policy and a consistency contract. Do not predict a percentage speedup without a benchmark.
* Paging bounds returned batches, but needs stable ordering and a policy for edits during traversal. It does not automatically bound all retained state.
* A streaming feed changes ingestion, ordering, replay, and recovery requirements. It is not simply a faster scheduler.
* Notification adapters isolate channel details, while delivery guarantees often require application-level state and new contracts.

No fixed number of users or tickers proves these alternatives necessary. Test representative alert distribution, slow providers, all-alerts-matching bursts, and concurrent edits. Measure end-to-end latency, query plans, allocation, lock waits, and failure recovery separately.

## Review rubric

Accept the Level 1 classroom implementation when its invariants, deterministic examples, adapter mapping, and deliberately repeatable console notifications are tested. Before real external delivery, specify freshness, failure recovery, duplicate handling, identity, and authorization according to the intended product requirements.

The assignment omits authentication to focus on architecture. Owner labels are not an access-control mechanism. A real multi-user extension needs an identity and authorization model; that is a separate requirement, not a property supplied by a repository port.

**Discussion:** which guarantee would you add first for your chosen use case, and what evidence makes it more important than another optimization?

[Assignment guide](WATCHLISTS-MARKET-SENTINEL.md) · [Current architecture decisions](../../architecture/adr/README.md)
