# 2. Measure before optimizing

**Outcome:** propose a reproducible measurement instead of treating a transaction count as a universal capacity limit.

## Count the work, then measure its cost

The current request loads a portfolio graph and full transaction history, obtains quotes and computes results. Its latency may be dominated by database access, mapping, a cache miss, quote latency, allocation, contention or calculation. Source inspection identifies candidates; it cannot identify the bottleneck for every workload.

There is no measured threshold in this tutorial at which 20,000 or 100,000 transactions becomes safe or unsafe. Likewise, neither database aggregation nor a snapshot gives unlimited capacity.

## Two different memory questions

| Quantity | Meaning | Example |
|---|---|---|
| Retained live data | Objects still reachable while a request is processed | Loaded transactions, portfolio, persistence context |
| Allocation volume | Objects created over a period, including short-lived intermediates | Decimal arithmetic results and mappings |

Do not add these quantities and call the result simultaneous heap usage. Suppose a measured request retains 6 MiB and allocates 3 MiB of short-lived intermediates. Fifty overlapping requests might retain roughly 300 MiB under similar conditions, plus shared application and database-client overhead. They do not necessarily retain 450 MiB. Allocation **rate** additionally depends on requests per second and affects collector work.

The 6 MiB and 3 MiB figures are illustrative inputs to the reasoning, not measurements of HexaStock. Object sizes, sharing, escape analysis, ORM state and collector behavior must be observed on the target JVM.

## A small measurement plan

1. Fix the Java version, heap, container memory and database configuration.
2. Generate reproducible histories with several transaction counts, ticker counts and remaining-lot counts. Record the distribution, including large portfolios.
3. Use a deterministic quote stub to isolate local costs, then test representative provider latency and cache hit/miss patterns separately.
4. Run warm-up and sustained concurrent requests. Report throughput, errors, p50/p95/p99 latency, retained memory, allocation rate, GC activity, query counts and lock waits.
5. Profile the slow path. Change one factor and repeat with the same dataset.

p95 means that 95% of measured requests completed at or below that latency; it is not a worst-case bound. Record sample counts and workload duration so comparisons are interpretable.

## Why start sequentially?

The calculator uses a sequential scan with local accumulators. This is straightforward to test and profile. Parallel streams can add scheduling and merging costs and may contend for shared execution resources. Neither the break-even point nor deterministic throughput follows from the number of CPU cores alone.

Parallel provider calls also need a concurrency limit, rate-limit policy and timeout behavior. More threads do not create a larger API quota. A provider-specific batch endpoint is an adapter option only if that provider actually supports the required contract.

## Choose the next experiment from evidence

| Observed cost | Candidate experiment |
|---|---|
| Quote latency or rate-limit failures | Inspect existing cache behavior, request coalescing and provider-supported batching |
| Loading transaction objects | Query projections or database pre-aggregation |
| Loading large portfolio graphs | A dedicated read port for the necessary holding data |
| Lock waits during reporting | Review the locked JPA lookup and the required read consistency |
| Calculation or allocation | Profile accumulator and result construction before adding parallelism |

A cheaper query must still return the same defined values. Preserve correctness examples and measure the complete request, not only the arithmetic loop.

**Checkpoint:** explain why reducing calculator allocations may barely change latency when a quote cache miss dominates the request.

Next: [query alternatives](03-QUERY-ALTERNATIVES.md). [Chapter index](HOLDINGS-PERFORMANCE-AT-SCALE.md).
