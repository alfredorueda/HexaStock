# Aggregate Root Size — Principled Guidance for Modern Java Backends

> Audience: senior engineers and architects designing DDD aggregates in Java 21+ / Spring Boot / Hibernate / cloud-native stacks (e.g., AWS).
> Purpose: a defensible answer to *"how big can an aggregate get before it hurts?"* — without inventing fake precision.

---

## 1. Principled answer (DDD first)

In Domain-Driven Design, the size of an Aggregate Root is **not primarily a performance question**. It is a **consistency boundary** question.

Established guidance (Evans, Vernon):

- An aggregate is the **smallest unit of transactional consistency**. Everything inside it is updated atomically; everything outside is eventually consistent.
- "Design **small** aggregates" (Vernon, *Effective Aggregate Design*, 2011). Prefer referencing other aggregates by **identity**, not by composition.
- The aggregate exists to **protect invariants**. If an invariant does not require two entities to be modified together, they likely belong in **different aggregates**.

So the first question is never *"how many bytes?"* but: **"which invariants force these objects to be loaded, mutated, and persisted as one transactional unit?"** If an entity is inside the aggregate only because "it felt natural," it is a modeling smell, not a performance problem to optimize later.

Performance-driven sizing only becomes the dominant concern **after** the consistency boundary is correctly drawn. At that point the heuristics below apply.

---

## 2. Practical heuristics (ranges, not magic numbers)

These ranges are **engineering heuristics**, not literature. They assume a typical Spring Boot + Hibernate + PostgreSQL/MySQL stack, Java 21, modest pod sizing (e.g., 2–4 vCPU, 1–4 GB heap), and OLTP-style traffic.

| Tier | Child entities loaded per aggregate | Hydrated heap footprint (rough) | Verdict |
|---|---|---|---|
| **Ideal** | tens (≈ 10–10²) | tens of KB | Healthy. Fast hydration, negligible GC, short transactions. |
| **Acceptable** | hundreds (≈ 10²–10³) | hundreds of KB | Fine for most OLTP workloads if writes are infrequent or contention is low. |
| **Warning** | low thousands (≈ 10³–10⁴) | ~MB | Latency, lock duration and GC start to matter. Needs benchmarking, pagination of read models, and review of the consistency boundary. |
| **Redesign** | tens of thousands or more, or > ~10 MB hydrated | many MB | Almost certainly the wrong boundary. Split the aggregate, introduce a separate read model, or move children to their own aggregate referenced by ID. |

Treat these as **orders of magnitude**, not thresholds. A 1,200-lot aggregate is not categorically worse than a 900-lot one; a 50,000-lot aggregate almost certainly is.

---

## 3. Why those ranges — the underlying forces

### 3.1 Transaction duration
A larger aggregate means a longer SELECT … FOR UPDATE / pessimistic lock window, longer Hibernate dirty-checking, and a longer flush. Transaction time grows roughly linearly with hydrated rows; tail latency grows faster. Long transactions amplify deadlocks and connection-pool exhaustion.

### 3.2 Concurrency and lock contention
The aggregate is the unit of write serialization. The **bigger the aggregate, the larger the surface that conflicts**. With optimistic locking (`@Version`), large aggregates produce more `OptimisticLockException` retries under load. With pessimistic locking, they produce queuing. Either way, **throughput per aggregate instance drops as size grows**, even if single-request latency looks acceptable.

### 3.3 ORM hydration cost
Hibernate cost is dominated by:
- Number of rows materialized (N+1 risk on collections).
- Number of associations traversed.
- Entity/collection cache churn in the persistence context.

Hydration is rarely linear in practice; it is **super-linear** once collection joins fan out (Cartesian products via `JOIN FETCH` on multiple `@OneToMany`).

### 3.4 GC pressure
Per-request allocation of large object graphs (entities, proxies, collections, snapshots for dirty checking) increases young-gen pressure. With G1/ZGC on Java 21 this is usually fine until you cross into MB-per-request territory at high RPS — at which point allocation rate, not heap size, becomes the bottleneck.

### 3.5 p95 / p99 latency
Average latency hides aggregate-size pain. The tail does not. Larger aggregates produce **fatter tails** because of variance in row count, lock waits, and GC pauses correlated with allocation. A use case whose p50 is 30 ms and p99 is 600 ms is almost always suffering from oversized aggregates or N+1 hydration.

---

## 4. How size interacts with modeling choices

### 4.1 Number of child entities
The dominant factor. A flat aggregate with 10,000 children is usually worse than a deep aggregate with 100 nodes, because row count drives both DB and ORM cost.

### 4.2 Deep object graphs
Depth multiplies traversal cost and increases the chance of accidental lazy-loading inside the domain layer (`LazyInitializationException`, or worse, silent N+1). Depth also makes invariants harder to reason about — a smell that the boundary is too wide.

### 4.3 Lazy vs eager loading
Lazy loading is **not** a fix for an oversized aggregate; it is a way to **hide** the problem until production. From a DDD perspective, an aggregate must be **fully loadable in a consistent state** to enforce its invariants. If you cannot afford to load it, the boundary is wrong. Use lazy loading for *cross-aggregate* references, not to amputate parts of the same aggregate.

### 4.4 Write-side consistency boundary
The aggregate boundary should match the **invariants that must hold after every commit**. Anything outside that set should be:
- a separate aggregate (referenced by ID), or
- a **read model / projection** (CQRS-lite) optimized for queries, never for invariants.

This is the single most effective tool for keeping aggregates small.

---

## 5. Concrete example — Portfolio / Holdings / Lots / Sell Shares

Consider this domain (the one in this repository):

- `Portfolio` (aggregate root)
  - `Holding` per ticker (e.g., AAPL, MSFT, …)
    - `Lot` per purchase (FIFO/LIFO accounting unit)

**Question:** to sell `N` shares of AAPL, must we load the whole `Portfolio`?

#### The DDD answer
It depends on **which invariants the sell operation must enforce atomically**:

- *"You cannot sell more shares of a ticker than you own"* → invariant scoped to **one `Holding`**.
- *"FIFO/LIFO consumption order across lots of that ticker"* → invariant scoped to **one `Holding`**.
- *"Total portfolio cash / margin / exposure limit"* → invariant scoped to the **whole `Portfolio`**.

If the sell use case only needs the first two, the consistency boundary for *sell* is the **`Holding` for that ticker**, not the entire `Portfolio`. The other holdings are irrelevant to the invariant and should not participate in the transaction.

#### Practical implication
Two reasonable designs exist:

1. **`Portfolio` as aggregate root, but load only the relevant `Holding`.**
   The aggregate root is still `Portfolio` for identity and authorization, but the *repository method* for sell loads only `holdingByTicker(portfolioId, ticker)` with its lots. This is acceptable if the team can guarantee no other invariant spanning holdings is silently violated. It is essentially a performance compromise on top of a wider modeling boundary.

2. **`Holding` as its own aggregate, referenced by `PortfolioId`.**
   Cleaner from a DDD standpoint when no cross-holding invariant exists. Each sell loads exactly one small aggregate. `Portfolio` becomes a thin coordinator / read model. This scales linearly with users, not with portfolio breadth.

**Recommendation for a sell use case:** from a didactic standpoint, hydrating the entire `Portfolio` — as the present implementation does — constitutes a deliberate and defensible simplification. It makes every invariant of the sell operation visible and testable inside a single, easily reasoned-about consistency boundary, preserves the visibility of the aggregate root, and avoids prematurely entangling the learner with repository-level optimization concerns. Once those fundamentals are clear, a production-oriented evolution is to align the unit of load with the actual consistency boundary of the operation. Where the invariants of the sell operation are confined to a single ticker, two evolutionary paths are available: restricting the repository to load only the pertinent `Holding` (option 1), or promoting `Holding` to an aggregate in its own right, referenced by `PortfolioId` (option 2). Both alternatives improve transactional cost, mitigate cross-ticker contention, and yield latency characteristics that remain stable as portfolios grow — and should be understood as a natural maturation of the design from didactic clarity toward production scalability, not as a repudiation of the pedagogical model from which it departs.

---

## 6. Final recommendation (quotable)

> **Size the aggregate by the invariants it must protect, not by the navigability of the domain model. In a modern Java 21 / Spring Boot / Hibernate stack, aim for aggregates that hydrate in tens to low hundreds of rows and a few tens of kilobytes. Treat low-thousands as a warning sign requiring measurement, and tens of thousands as evidence that the consistency boundary is wrong. When in doubt, split the aggregate and reference by identity — small aggregates are almost always the correct default.**

---

## 7. Separation of claims

| Claim | Source |
|---|---|
| Aggregates are consistency boundaries; prefer small aggregates; reference other aggregates by identity. | **Established DDD literature** (Evans, *Domain-Driven Design*, 2003; Vernon, *Effective Aggregate Design*, 2011; Vernon, *Implementing Domain-Driven Design*, 2013). |
| Aggregates must be loadable in a fully consistent state to enforce invariants. | **Established DDD literature.** |
| Use CQRS / read models for query-shaped concerns rather than expanding the aggregate. | **Established practice**, originating with Young / Dahan, widely adopted. |
| Numerical ranges (tens / hundreds / thousands of children; KB / MB hydrated). | **Engineering heuristic.** Not from the literature. Reasonable defaults for typical OLTP Spring Boot stacks; must be validated per system. |
| Statements about GC pressure, lock contention, p95 tails, and ORM super-linear cost on multi-collection fetches. | **Engineering heuristic** grounded in well-known JVM/Hibernate behavior, but the exact crossover points are **system-specific**. |
| Whether a `Portfolio` should be one aggregate or split into `Holding` aggregates. | **Modeling decision.** Depends on the actual invariants of *your* business. The analysis above is a guide, not a verdict. |
| Exact thresholds for *your* service (rows, MB, ms). | **Requires benchmarking** under realistic load, with realistic data distribution (long-tail portfolios, hot tickers, peak concurrency). |

---

*Rule of thumb for the classroom:* **"If you have to ask whether the aggregate is too big, it probably is — and the fix is almost never lazy loading; it is a smaller boundary."**
