# 4. Measure aggregate size and cost

**Goal:** decide whether a consistency boundary or its persistence strategy needs changing. There is no universal row count or megabyte threshold that proves an aggregate is too large.

## Begin with the invariant

The aggregate groups state whose changes must be coordinated. In HexaStock, a purchase checks cash, creates a lot, and debits cash; a sale consumes lots and credits cash. Read the [consistency-boundary lesson](../architecture/aggregates/01-CONSISTENCY-BOUNDARY.md) before considering a split.

History need not be a navigable child collection merely because it concerns the same portfolio. Current transactions are stored separately and written atomically with mutations through the application transaction. Separate aggregates are not automatically eventually consistent.

A report may scan many records without making them part of a command aggregate. Query projections, database aggregation, and snapshots are separate choices explained in [holdings performance](../tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md).

## Build a measurement matrix

Vary dimensions independently rather than assigning unsupported labels such as “typical retail” or “safe enterprise size.”

| Dimension | Why it matters |
|---|---|
| Holdings per portfolio | Breadth of the loaded graph and collection queries |
| Remaining lots per holding | FIFO traversal and mapping work |
| Historical transactions | Reporting/history cost, separate from current lots |
| Concurrent requests to one portfolio | Lock waiting or optimistic conflicts |
| Concurrent requests to different portfolios | Shared connection, CPU, heap, and provider pressure |
| Warm/cold quote cache | Network time and quote freshness |
| Failure injection | Retry amplification, rollback, and recovery |

Use representative and deliberately adverse data, including uneven lot distribution. Depleted lots are removed, but unsold lots can accumulate. Empty holdings also remain in current state.

## Distinguish memory measurements

* **Shallow size:** one object's fields and header.
* **Reachable graph size:** objects reachable from a root, accounting for shared references.
* **Retained size:** memory that would become collectible if a root were removed.
* **Allocation volume/rate:** all new objects created over an operation or interval, including temporary objects.
* **Process memory:** includes more than Java heap, such as thread stacks and native buffers.

Domain objects, managed JPA entities, detached mapping objects, and Hibernate bookkeeping can coexist. Multiplying a guessed object size by a lot count does not measure the request's peak live memory. Object headers, reference compression, alignment, collection capacity, and sharing depend on the runtime; they are not fixed by the Java language specification.

Record the JDK, heap settings, collector, database, dataset, request mix, and warm-up procedure. Use allocation profiling and heap analysis for their distinct questions. Do not label hand estimates as measurements or attach an unsupported ±20% accuracy range.

## Diagnose the limiting resource

Measure response percentiles alongside query count, returned rows, lock waits, transaction duration, connection-pool occupancy, allocation, and GC behavior. A high p99 can come from provider delays, queuing, disk, locks, or many other causes; it is not proof of an oversized aggregate.

A simple capacity sketch is `concurrent occupied connections / mean time held`, under assumptions of continuous utilization and no other bottleneck. It is not a throughput guarantee, and increasing the pool can overload the database. Use it to formulate a test, not to publish a capacity claim.

## Decide from evidence

| Finding | First candidate to investigate |
|---|---|
| Summary read loads irrelevant children | Query projection |
| Many collection round trips | Fetch plan or batching |
| Provider time dominates lock duration | Transaction boundary and quote contract |
| Mapping dominates allocation | Save/mapping strategy |
| Same-root conflict rate dominates | Workload coordination or boundary redesign |
| History scan dominates reporting | Aggregation or a recoverable projection |

A partial aggregate requires a safe partial-write contract. A split must still coordinate cash and holdings. Lazy loading can be a useful persistence technique, but must not leave required invariants unevaluated or expose lazy proxies across an unintended boundary.

**Checkpoint:** justify one proposed change with a measured bottleneck, a preserved invariant, and a post-change comparison under the same workload. A fixed number of lots is not enough.

[Review guide](DDD-REVIEW-PERSONAL-INVESTMENT-PORTFOLIO.md)
