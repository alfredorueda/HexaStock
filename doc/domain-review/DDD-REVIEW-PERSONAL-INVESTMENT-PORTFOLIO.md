# Review the domain and architecture

An architectural review should distinguish observed behavior, a possible weakness, and a proposed change. This route applies that distinction to HexaStock's `main` branch. It is an educational review, not a production certification or a workload benchmark.

Complete the [sell-stock tutorial](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md) first. Review one concern per session rather than reading an exhaustive verdict in one sitting.

| Lesson | Review question | Deliverable |
|---|---|---|
| [1. Inspect invariants](01-INVARIANTS-AND-LIMITS.md) | Which rules are actually enforced, and through which paths? | Evidence table with one counterexample |
| [2. Compare model alternatives](02-MODEL-ALTERNATIVES.md) | What requirement would justify a different model? | A decision with costs and verification |
| [3. Plan runtime improvements](domain-model-constructive-strategy-review.md) | What would change in transactions, queries, and adapters? | A small, testable proposal |
| [4. Measure aggregate size](AGGREGATE-SIZE-GUIDELINES.md) | Which resource limits the workload? | A reproducible measurement plan |

Allow 30–45 minutes per lesson. No runtime changes are made by this review.

## Baseline and evidence

There are seven child Maven modules plus the parent build, including both JPA and MongoDB persistence adapters. The domain production code uses no framework APIs; application code uses Jakarta transactions and no Spring imports. Bootstrap supplies runtime wiring. Test dependencies are a separate concern from production dependencies.

Start from the [architecture map](../architecture/ARCHITECTURE-MAP.md), [runtime guarantees](../architecture/RUNTIME-AND-VERIFICATION.md), and [ADRs](../architecture/adr/README.md). Seven ArchUnit rules check selected package dependencies. They do not prove every property of DDD, data consistency, security, or operational readiness.

DDD aggregate boundaries and hexagonal dependency boundaries are different concepts. An aggregate groups state protected by coordinated operations; the application core can contain multiple aggregates and services. A model can have appropriate dependencies while still exposing a mutable child or implementing an incorrect rule.

The useful findings in this project are concrete: FIFO is visible in `Holding.sell`, adapters translate representations, and ports support focused tests. The useful limitations are equally concrete: exposed mutable children, unbounded history reads, quote retrieval inside transactions, and contracts whose edge cases need explicit decisions.

[Book home](../BOOK-HOME.md)
