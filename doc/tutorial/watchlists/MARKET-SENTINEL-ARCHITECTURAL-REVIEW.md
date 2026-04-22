# Architectural Review — Watchlist Market Sentinel Notification Algorithm

> **Scope.** This document is a senior-architect review of the *core notification algorithm* that drives the Watchlist Market Sentinel inside HexaStock: periodically fetch prices for the distinct tickers used by active watchlists, detect which alerts trip, and emit buy-signal notifications. It evaluates the **algorithmic shape** and the **architectural placement** of that algorithm — not any particular line of code.
>
> **Audience.** Architects, senior engineers, and academic readers studying hexagonal architecture and DDD on a realistic example.
>
> **Reference artefacts.**
> - Tutorial: [WATCHLISTS-MARKET-SENTINEL.md](WATCHLISTS-MARKET-SENTINEL.md)
> - Specs: [`specs/`](specs/)
> - ADRs: [ADR-016](../../architecture/adr/ADR-016-watchlists-bounded-context.md), [ADR-017](../../architecture/adr/ADR-017-cqrs-pragmatic-separation-for-market-sentinel.md), [ADR-018](../../architecture/adr/ADR-018-watchlist-list-of-alertentry-instead-of-map.md), [ADR-019](../../architecture/adr/ADR-019-scheduler-as-driving-adapter.md), [ADR-020](../../architecture/adr/ADR-020-notification-outbound-port.md), [ADR-021](../../architecture/adr/ADR-021-progressive-level-roadmap-for-watchlists.md)

---

## 1. Executive summary

The current Market Sentinel algorithm is **architecturally sound and pedagogically excellent**. It is a textbook example of how to apply *pragmatic CQRS* inside a hexagonal core: a thin application service orchestrates outbound ports (`WatchlistQueryPort`, `StockPriceProviderPort`, `NotificationPort`) without depending on any framework, and a separate driving adapter handles scheduling. The chosen optimisations — **batching on distinct tickers, projection-only reads, no aggregate hydration on the read path** — are exactly the right baseline for the stated workload.

The algorithm should be **kept as the baseline** and **refined incrementally**. The two improvements that genuinely matter at any non-toy scale are (a) deduplication / cooldown on the notification side (already scheduled as Level 5 in the spec roadmap) and (b) explicit handling of partial price-provider failures. Everything else — event-driven decomposition, streaming, denormalised read stores, partitioned schedulers — is justified only under genuine scale or reliability pressure that this codebase does not yet face. There is **no architectural reason for a redesign**.

Overall assessment: **strong baseline, defensible under code review, not naïve, and explicitly future-proofed by the L1→L5 roadmap (ADR-021).**

---

## 2. Algorithm overview

### 2.1 Conceptual steps

1. **Discover the working set.** Ask the read side for the **distinct set of tickers** referenced by *active* watchlists. This is a pure query — no aggregate is loaded.
2. **Fetch prices in one batch.** Send the entire ticker set to the price provider in a single call, receiving a `Map<Ticker, StockPrice>`. The provider call is the most expensive step (network + rate limits + currency conversion).
3. **Evaluate triggered alerts per ticker.** For each ticker that has a price, ask the read side which alert entries trip given the current price. The threshold comparison happens **in the database**, not in Java memory.
4. **Emit notifications.** For each triggered alert projection, build a domain `BuySignal` value object and hand it to the outbound `NotificationPort`. Delivery semantics are owned by the adapter.

### 2.2 Implicit assumptions

- Price reads are **idempotent and side-effect free**, and the provider supports a batch call. (HexaStock's provider port already exposes `fetchStockPrice(Set<Ticker>)`.)
- The number of *distinct* tickers across all active watchlists is **substantially smaller** than the number of alerts — i.e. tickers are heavily reused across users (a realistic assumption in equities).
- The detection cycle frequency (default 60 s) is **fast enough for the user's expectations** but **slow enough for the price provider's rate limit**.
- A buy signal is **stateless** at L1: triggering the same alert in successive cycles is acceptable; deduplication is an L5 concern.
- The scheduler is a **single, in-process** trigger. Multi-instance deployment is out of scope at L1 and explicitly addressed by future evolution.

### 2.3 What the algorithm optimises for

In order of priority:

1. **External-API economy.** One batch call per cycle, regardless of user count. This is the biggest cost saving in any market-data-driven system.
2. **Architectural clarity.** Read side and write side use distinct ports (ADR-017) so the algorithm is comprehensible by reading the application service alone.
3. **Predictable database load.** Projection queries with parameterised filters; no `N+1`; no aggregate hydration.
4. **Pedagogical legibility.** Each step maps to a sentence — discover, fetch, evaluate, notify — so the design teaches well.

It deliberately does **not** optimise for: at-most-once delivery, transactional coupling between detection and notification, or hard real-time latency. Those are conscious deferrals.

---

## 3. Strengths of the current approach

- **Correct CQRS placement.** The read path uses a dedicated `WatchlistQueryPort` returning flat projections (`TriggeredAlertView`); the write side keeps the aggregate. This is *pragmatic* CQRS — same store, separated responsibilities — and it is the right level of ceremony for this domain (ADR-017).
- **Batching by distinct ticker is the right primary optimisation.** It collapses the cost from `O(alerts)` provider calls to `O(distinct tickers)` provider calls. For a realistic equity workload, that is a one- to two-order-of-magnitude reduction.
- **Threshold filtering in SQL.** Pushing `threshold_price >= :currentPrice` to the database means the JVM never sees rows it would discard. The result set size is bounded by *triggered* alerts, not by *configured* alerts.
- **Framework-free application core.** The service is a plain Java class implementing an inbound port; Spring wiring lives in the bootstrap module per ADR-015. The core compiles and tests without Spring on the classpath.
- **Driving-adapter scheduler.** `@Scheduled` lives in an inbound adapter (ADR-019). The application service has no notion of *when* it runs — it can be triggered by a cron, an HTTP probe, a test harness, or a CLI without changing.
- **Outbound port for notifications.** `NotificationPort` keeps the delivery channel pluggable (ADR-020). Switching from console log to email, push, or Kafka is a one-adapter change.
- **Aggregate boundary survives the algorithm.** The `Watchlist` aggregate is never loaded by the Sentinel, so its size and invariants impose zero cost on the hot path. The choice of `List<AlertEntry>` (ADR-018) makes Level 2 (ladder alerts) free of aggregate change.
- **Explicit, additive level roadmap.** The L1→L5 progression (ADR-021) makes every concern this review raises *already scheduled*: ladders (L2), percentage alerts (L3), memos (L4), cooldown (L5). The design admits future complexity without inviting it prematurely.

---

## 4. Weaknesses and risks

### 4.1 Real, in-scope concerns

- **Notification duplication at L1.** Without deduplication, a ticker that sits below threshold for many cycles re-fires every cycle. This is acknowledged and explicitly deferred to L5 (`cooldownMinutes` + infrastructure `lastTriggeredAt`). Until L5 ships, the L1 contract is *intentionally noisy*.
- **Missing-price handling is implicit.** If the provider omits a ticker (transient outage, unknown symbol), the algorithm should *skip silently and retry next cycle*. That behaviour is now correct, but it is a contract worth documenting, not just observing in code, because it means **a degraded provider produces silent missed signals**, not exceptions.
- **No explicit transaction boundary.** The detection cycle reads, evaluates, and writes via three separate ports. A successful `notifyBuySignal` followed by a failed `markAlertTriggered` (L5) yields **at-least-once delivery**. This is the correct trade-off but must be documented as such — at-most-once would require either two-phase delivery or an outbox pattern, both disproportionate at this scale.

### 4.2 Acceptable trade-offs (do not "fix" without business pressure)

- **One round-trip per ticker for triggered alerts.** Step 3 is a loop of `findTriggeredAlerts(ticker, price)` calls — `O(distinct tickers)` queries. A single batch query (`WHERE (ticker, price) IN (…)`) would collapse this further but at the cost of a more complex query shape and a less pedagogical port. Worth doing only when the distinct-ticker set crosses a few hundred per cycle.
- **Single-process scheduler.** Two app instances will run two scheduler ticks. Until a leader-election or distributed-lock mechanism is in place, this can double notifications per cycle. Acceptable for L1; revisit when going multi-instance.
- **Threshold semantics encoded in SQL.** `threshold_price >= currentPrice` lives in the query, not in a domain rule. This is a deliberate CQRS choice — the *projection* knows the trigger condition. The *write side* (`AlertEntry` invariants) does not duplicate it. This is fine, but it means a future change in threshold semantics (e.g. tolerance bands) must be applied in **two** places: the query and any future domain evaluation. Worth noting in the spec; not a defect.

### 4.3 Speculative concerns (do not act on without evidence)

- **Provider rate limiting under huge ticker sets.** Real concern only beyond ~thousands of distinct tickers per cycle.
- **In-memory result-set size.** The triggered-alerts projection set is bounded by *triggered* rows, which in steady state is small. Only a problem in a market-wide crash scenario, where the right answer is back-pressure and rate-limiting the notification adapter, not redesigning the algorithm.
- **Cache coherency with the price cache (ADR-013).** The Caffeine cache used by the price provider can serve a stale price within the cycle. For threshold semantics this is harmless (a price marginally older than `now` does not change the qualitative trigger).

---

## 5. Hexagonal Architecture assessment

The algorithm is a **canonical example** of well-placed hexagonal orchestration:

- **Inbound port (`MarketSentinelUseCase`)** owns the use-case verb (`detectBuySignals`). It is the only thing the scheduler sees.
- **Outbound ports** form a clean tripod: read (`WatchlistQueryPort`), upstream collaborator (`StockPriceProviderPort`), downstream collaborator (`NotificationPort`). Each direction of the hexagon is represented exactly once.
- **No infrastructure leakage.** The application service does not know about JPA, Mongo, HTTP, scheduling, logging frameworks, or message brokers. ADR-015's `@Bean`-wiring strategy keeps Spring annotations out of the core module.
- **Adapter symmetry is preserved.** The two persistence adapters (JPA, Mongo) implement the same query port; the test of "could we add a third" returns *yes* without algorithm changes.
- **The scheduler is a driving adapter, not domain logic.** This is a subtle but important call. Time *is* an external concern; pretending otherwise is one of the most common hexagonal anti-patterns.

The only mild tension: the projection record (`TriggeredAlertView`) sits next to the outbound port and is not a domain object. This is correct (it is a *read model artefact*) but it is also the place where future contributors are most likely to start adding behaviour. The spec already calls it out as "not a domain object"; an ArchUnit fitness rule preventing references to it from the `domain` module would lock the discipline in.

**Verdict:** the placement is right; the boundaries are honest; the algorithm passes a strict hexagonal review.

---

## 6. Domain-Driven Design assessment

The DDD posture here is **pragmatic and defensible**, not maximalist.

- **The Sentinel itself is correctly an *application service*, not a *domain service*.** Its job is *orchestration*: pull tickers, pull prices, ask "what tripped?", dispatch. None of these steps require domain knowledge that does not already live in the projection or the aggregate.
- **The Watchlist aggregate owns the invariants that matter** (no exact duplicate alert, threshold > 0, etc.). The Sentinel never violates them because it never writes to the aggregate.
- **`BuySignal` is a domain value object** — small, immutable, with a static factory that enforces a real invariant (the alert view's ticker must match the price's ticker). This is the right amount of domain modelling for a *signal*; making `BuySignal` a richer entity with lifecycle would be over-modelling.
- **Ubiquitous language is honoured.** Watchlist, alert, threshold, signal, ticker, price, owner — every concept in the algorithm has a name in the spec, the OpenAPI, and the Gherkin scenarios. The traceability matrix is the proof.
- **Bounded context boundaries are clean.** Watchlists and Portfolios share the *shared kernel* (`Ticker`, `Money`, `StockPrice`) but no aggregate references. The Sentinel cannot accidentally couple the two contexts because there is no path through the dependency graph (ADR-016).

The one DDD nuance worth airing: at L1 the algorithm's "trigger semantics" (`threshold_price >= currentPrice`) live in SQL, not in a domain method. From a purist DDD angle this is a minor smell — the *domain rule* "an alert trips when price reaches threshold" is expressed in the projection query rather than in code that an architect could read in the domain module. The pragmatic counter-argument (and the one this codebase implicitly takes) is that the rule is **trivial, stable, and expressed in the projection contract** documented in the spec. When L3 introduces `PERCENTAGE_DROP`, the rule becomes non-trivial and the question of where it should live deserves an explicit decision (an ADR is warranted at that point).

---

## 7. Performance and scalability analysis

### 7.1 Complexity

Let **U** be active watchlists, **A** total active alerts, **T** distinct active tickers, **R** triggered alerts per cycle (`R ≪ A` in steady state).

| Step | Cost per cycle |
|---|---|
| Discover distinct tickers | 1 query, returns `T` rows |
| Batch fetch prices | 1 provider call, payload `T` |
| Evaluate triggered alerts | `T` queries, returning `R` rows total |
| Notify | `R` adapter invocations |

Total: **`O(T)` queries + 1 external call + `O(R)` notifications**. This is essentially optimal for a pull-based architecture without precomputed materialised views.

### 7.2 What the design does *not* scale to without modification

- **Tens of thousands of distinct tickers per cycle.** The `O(T)` projection queries become the bottleneck before the provider call does. Mitigation: collapse step 3 into one query with a `(ticker, price)` value-list join. Implementation cost is moderate; not justified before the load actually appears.
- **Sub-second detection latency.** The pull model has a worst-case latency of one full cycle. Sub-second SLAs require switching to a push model (ticker price stream → in-memory ladder of thresholds). This is a different architecture, not a refinement.
- **Multi-instance horizontal scale-out.** The single-process scheduler will multiply notifications by the replica count. Mitigation: distributed lock (Redis, ShedLock) or leader election. Cheap to add when needed.
- **Burst notifications during market-wide moves.** A flash crash can trigger thousands of notifications in one cycle. The current synchronous notification loop will blockingly wait on the adapter. Mitigation: asynchronous dispatch with a bounded executor or an internal outbox.

### 7.3 Memory

Bounded by `T` (price map) + the largest single triggered-alert page (`R`). Both are tiny compared to a hydrated aggregate world. The algorithm can run in a heap measured in MB even at non-trivial scale.

---

## 8. Database and query design trade-offs

### 8.1 What the algorithm gets right

- **Two narrow projection queries** (`SELECT DISTINCT ticker …`, `SELECT … WHERE threshold_price >= ?`) are easier to index, easier to plan, and easier to read than a single mega-query.
- **No aggregate hydration on the read path.** No JPA `@OneToMany` cascade, no Mongo full-document fetch.
- **Indexable predicates.** `(active, ticker)` for step 1 and `(ticker, threshold_price)` for step 3 are obvious composite indexes. Both are highly selective in steady state.

### 8.2 Where to invest if scale demands it

- **Materialised view of `(watchlist_id, ticker, threshold_price, owner_name, list_name)` filtered by `active = true`.** Refreshed on aggregate write; queried instead of the join with the watchlist table. Buys you ~30–50% on step 3 in JPA, more on Mongo.
- **Single batched evaluation query.** Instead of `O(T)` round-trips, do one query: `WHERE (ticker, price) IN ((:t1,:p1),(:t2,:p2),…)`. Adds query-shape complexity; reduces round-trips.
- **Push detection into the write side.** Each price tick triggers an indexed lookup of alerts at-or-above that price for that ticker. Reverses the polarity of the algorithm and is only worth it under streaming-price feeds.

### 8.3 When *not* to optimise the queries

If `T < 1000` and the cycle period is ≥ 30 s, the current shape is inside the database's noise floor. Optimising further is *technical theatre* — it makes the design look sophisticated without measurably improving anything.

---

## 9. Reliability and fault tolerance

### 9.1 Failure modes and current behaviour

| Failure | Current behaviour | Severity | Recommended mitigation |
|---|---|---|---|
| Price missing for some tickers | Skipped silently, retried next cycle | Low | Document the contract; emit a metric `sentinel.missing_prices_total` |
| Price provider total outage | Cycle aborts on provider exception (assumed) | Medium | Wrap provider call in a circuit breaker (Resilience4j); skip cycle on open circuit; alert on prolonged outage |
| Notification adapter failure | Exception propagates; remaining alerts in *that ticker* not notified | Medium | Per-alert try/catch in the dispatch loop; metric `sentinel.notify_failures_total`; do **not** mark as triggered if delivery failed |
| Duplicate scheduler tick (cron drift, multi-instance) | Double notifications | Medium → High at L5 | Distributed lock or leader election before going multi-instance |
| At-least-once delivery (L5 `markAlertTriggered` after notify) | Possible duplicate within a cooldown window if mark-triggered fails | Acceptable | Document explicitly; deduplication on the consumer side is a common requirement anyway |
| Database failure mid-cycle | Cycle aborts; next cycle catches up | Low | No action needed |
| Long-running notification adapter blocking the loop | Cycle exceeds its period; next tick may pile up | Medium | Asynchronous dispatch with bounded queue and shed-load policy |

### 9.2 Production essentials vs optional sophistication

**Essential before going to production:**

- Per-alert try/catch around `notifyBuySignal` so one bad delivery does not poison the cycle.
- Metrics: cycle duration, distinct tickers, triggered alerts, notify failures, missing prices.
- Circuit breaker around the price provider.
- Leader election if running > 1 replica.

**Optional sophistication (only if SLAs demand it):**

- Outbox pattern for exactly-once semantics.
- Idempotency key on `notifyBuySignal` (carries `alertEntryId` + cooldown bucket).
- Asynchronous dispatch with bounded executor.
- Streaming price feed replacing the pull cycle.

---

## 10. Alternative designs and trade-offs

| Alternative | What it improves | Cost / complexity | When justified | Verdict for HexaStock |
|---|---|---|---|---|
| **Single batched evaluation query** (collapse step 3 into one SQL) | Round-trip count, latency at scale | More complex query shape, tied to one SQL dialect, harder to test | When `T` exceeds a few hundred per cycle | **Defer.** Add when metrics show the round-trips dominate. |
| **Event-driven (price-tick → eligible alerts)** | Sub-second latency, no polling | Streaming infrastructure (Kafka, Flink), reverse index of thresholds, more moving parts | When SLAs require sub-cycle latency or feeds are intrinsically streaming | **Out of scope.** A different architecture, not a refinement. Mention as future evolution. |
| **Asynchronous notification pipeline** (in-process executor or outbox) | Cycle latency under burst load, isolation between detect and deliver | Bounded queue tuning, back-pressure design, observability cost | When a single bursty cycle can exceed the period | **Improve later.** Add the executor + outbox in the same release that introduces real notification channels. |
| **Materialised denormalised read view** | Step 3 query speed, simpler indexing | Refresh strategy on aggregate writes; consistency lag | When the join in step 3 becomes the bottleneck | **Defer.** The current projection is already simple. |
| **Streaming / chunked processing** of tickers | Memory ceiling, fairness across users | Stateful chunking, partial-cycle semantics | When `T` is too large to fit comfortably in one batch call | **Out of scope.** Provider batch size will hit limits first. |
| **Push detection into the write side** (alert-as-trigger) | Real-time-ish notifications | Inverts the architecture; couples writes to price feeds | When ingesting a tick stream | **Out of scope** for a polling baseline. |
| **Precomputed cooldown bucket on the projection** | Stable deduplication across cycles without round-trips to update `lastTriggeredAt` | Slightly larger projection; cache invalidation | When notification volume is large enough that the L5 `markAlertTriggered` write is itself a bottleneck | **Improve later.** A natural Level-6 evolution after L5 ships. |

The honest summary: **none of these alternatives is *better* than the current baseline in absolute terms.** Each one trades simplicity for a specific guarantee. The current design pays the right price for the current requirements.

---

## 11. Academic perspective

This algorithm is **an unusually good teaching example**:

- It exercises **all four** of the architecture pillars HexaStock teaches — Hexagonal, DDD, CQRS, and progressive level-by-level evolution — within a single, comprehensible use case.
- It demonstrates **why CQRS exists** in the most legible possible way: the read-side cost of loading aggregates is so obviously wasteful here that students grasp the motivation without needing a separate lesson.
- It demonstrates **why a port boundary is not bureaucratic ceremony**: the Sentinel reads cleanly because it speaks only to ports, and the framework-free application service makes "what the use case does" inspectable in isolation.
- It demonstrates **DDD's pragmatism**: the trigger semantics live in SQL because they are trivial and stable, and the textbook would lose nothing by acknowledging that.
- The L1→L5 roadmap shows students how a feature **grows additively** without retrofitting, which is one of the hardest DDD lessons to teach with a static example.

Academic caveats worth surfacing in the lesson:

- The L1 algorithm is intentionally noisy; the L5 cooldown is not optional luxury but a real production requirement. Teaching L1 without explicitly framing L5 as "the missing half" risks normalising the noisy contract.
- The pull-based design is *one* legitimate architecture; in a course covering streaming systems, the contrast with a push-based equivalent is illuminating and not currently shown.
- "Pragmatic CQRS" should be named as such; calling it just "CQRS" invites confusion with the full event-sourced variant.

---

## 12. Professional industry perspective

In a real engineering team this design would be received as **"the right baseline — let's ship it"**. It is a pragmatic solution that a senior engineer would defend in a design review without apology. The choices the design makes — pull-based, single-store CQRS, batching by distinct ticker, framework-free core — are exactly the choices a competent team would make for a product at the "first ten thousand users" stage.

What the team would want to see *before production*:

- Metrics and a dashboard (cycle duration, missing prices, notify failures).
- A circuit breaker around the price provider.
- Per-alert error isolation in the notification loop.
- Leader election or a distributed lock if HA matters.
- The L5 cooldown shipped before any user-facing notification channel beyond logs.

What would *not* trigger a redesign:

- Adding a new alert type (the discriminated `AlertCreateRequest` schema already accommodates this).
- Adding a new notification channel (one new adapter).
- Adding a new persistence backend (one new query adapter).
- Reaching 10× current load (the algorithm is well inside its complexity envelope).

What *would* trigger a redesign:

- Switching to a streaming price feed.
- Sub-second notification SLAs.
- Multi-tenant isolation requirements (per-tenant rate limits, per-tenant scheduling fairness).

In short: the design is **idealised in the right places** (clean ports, framework-free core, additive evolution) and **realistic in the right places** (single-store CQRS, projection queries, synchronous notification). It is not a toy example dressed up; it is a real baseline.

---

## 13. Final recommendation

**Keep the current algorithm as the production baseline.** It is a strong, defensible design that holds up to a serious architectural review. The hexagonal placement is correct, the DDD posture is honest, and the CQRS pragmatics match the workload.

**Improve now (before any user-facing release):**

1. Land Level 5 (cooldown) — without it, the L1 contract is intentionally noisy and unsuitable for a production user.
2. Per-alert error isolation around `notifyBuySignal` so one bad delivery does not poison the cycle.
3. Metrics: cycle duration, missing prices, notify failures, distinct tickers, triggered alerts.
4. Circuit breaker on the price provider.

**Improve when scale requires it (defer with discipline):**

- Single batched evaluation query if `T` (distinct tickers per cycle) exceeds a few hundred.
- Asynchronous notification dispatch when a burst can exceed the cycle period.
- Distributed lock / leader election when going multi-instance.
- Materialised denormalised view if step 3's join becomes the dominant cost.

**Improve only if the business context fundamentally changes (do not pre-build):**

- Event-driven, push-based architecture (sub-second latency).
- Outbox pattern for exactly-once delivery semantics.
- Per-tenant scheduling fairness and rate limits.
- Streaming price feed integration.

---

## Decision table

| Aspect | Keep (it is a strength) | Improve now (before production) | Improve later if scale requires it |
|---|---|---|---|
| Pull-based polling cycle | ✅ | | (replace only with streaming feed) |
| Batching by distinct ticker | ✅ | | |
| Pragmatic single-store CQRS (ADR-017) | ✅ | | |
| Framework-free application core (ADR-007/015) | ✅ | | |
| Scheduler as a driving adapter (ADR-019) | ✅ | | |
| Outbound `NotificationPort` (ADR-020) | ✅ | | |
| Aggregate not loaded on read path | ✅ | | |
| Threshold filter pushed to SQL | ✅ | | (factor out at L3 when rule becomes non-trivial) |
| `List<AlertEntry>` choice (ADR-018) | ✅ | | |
| L1→L5 additive roadmap (ADR-021) | ✅ | | |
| Notification deduplication / cooldown | | ✅ (ship L5) | |
| Per-alert error isolation in dispatch loop | | ✅ | |
| Metrics & dashboards | | ✅ | |
| Circuit breaker on price provider | | ✅ | |
| Documenting at-least-once delivery contract | | ✅ | |
| `O(T)` projection queries → single batched query | | | ✅ |
| Asynchronous notification dispatch / outbox | | | ✅ |
| Distributed lock / leader election (multi-instance) | | | ✅ |
| Materialised denormalised read view | | | ✅ |
| Event-driven / streaming architecture | | | (only on business shift) |
| Exactly-once delivery semantics | | | (only on business shift) |

---

*Reviewer's closing note.* The instinct of an experienced architect on first reading this algorithm is to look for the trick — the place where the simplicity is buying something dishonest. There is no such place. The algorithm is simple because the problem allows it to be simple, and the architecture makes that simplicity *visible* rather than accidental. That is the highest compliment an architectural design can earn.
