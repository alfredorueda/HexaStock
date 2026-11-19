# 2. Compare model alternatives

**Goal:** select a design because it meets an explicit requirement. The alternatives below are proposals; `main` retains its current aggregate and application-created transaction records.

## Start with the current design

Portfolio → Holding → Lot keeps cash and position changes visible together. A sale consumes lots, calculates proceeds/cost/profit, updates cash, and returns a result. The application persists current state and a financial transaction in one configured database transaction.

Separate history storage does not imply eventual consistency: two separate aggregate records can participate in one local transaction. Conversely, a domain event does not automatically create a durable, atomic history entry.

| Alternative | Requirement it can address | Costs and verification |
|---|---|---|
| Keep current aggregate | Teach and coordinate cash, holdings, and FIFO | Test supported mutation paths, persistence conflicts, and history writes |
| Collect domain events | Several consumers need the same operation facts | Ensure emission, durable capture, ordering, rollback, and duplicate handling |
| Ledger-derived state | Rebuild state from authoritative operations | Define complete event data, replay/versioning, corrections, and projection recovery |
| Average-cost position | Business explicitly chooses average cost | Changes FIFO behavior; cannot substitute without a requirement change |
| Lots without a Holding entity | Group lots by ticker without a persistent holding identity | Move grouping and FIFO responsibility somewhere explicit |
| Independent Holding aggregate | Measured contention or a different consistency boundary | Coordinate cash and position updates; test failure and concurrency windows |

## Domain events are a coordination choice

An aggregate can return or collect facts such as `StockSold`. A handler may derive the financial transaction from that fact. This reduces repeated transcription if several workflows use it, but someone must still emit, persist, and handle the event correctly.

For mandatory history in the same database, handle or persist the relevant record before commit in the same transaction. For asynchronous external effects, atomically persist delivery intent, then dispatch with recovery and idempotency. Publishing after commit without durable capture can lose the event on a crash. An in-process listener is not automatically asynchronous or durable.

Domain events, event sourcing, and message brokers are different choices. Event sourcing makes a complete ordered event history authoritative for reconstruction; simply adding a `StockSold` notification does not do so. Events can remain plain domain values while coordination belongs in application code and delivery infrastructure in adapters.

## A ledger can still support a rich model

Deriving state from a ledger does not prohibit an aggregate that replays events, enforces rules, and produces new events. Derived projections can still be wrong or stale, so “positions can never diverge” is not a valid guarantee. Test replay against known histories, duplicate events, interrupted rebuilds, and schema evolution.

HexaStock's current transaction records are not a complete event-sourcing specification. They omit some historical lot-level details needed by richer reconstruction or audit requirements. Define the required questions before choosing a new history model.

## Splitting a holding changes coordination

Current sales update cash as well as the selected holding; purchases check and debit shared cash. Optimizing only the ticker's lots does not remove those cross-state obligations.

With one transactional database, an application can coordinate separate aggregates in one transaction if the chosen model permits it. Separate stores may need reservations, compensation, or eventual consistency. Neither “a split always requires a saga” nor “a split preserves all guarantees for free” is accurate.

Partially loading a `Portfolio` and then using the existing full-graph save is particularly risky: missing children can be mistaken for removals under orphan removal. A partial read needs a distinct representation or a save contract that cannot overwrite unseen state.

## Small changes also need a reason

Renaming `PURCHASE`/`SALE` to `BUY`/`SELL` is a vocabulary and compatibility decision, not an architectural repair. Similarly, moving an external-provider exception to the application layer or strengthening report types may improve clarity, but requires checking imports, adapters, clients, and tests. A query result using decimals is a trade-off, not proof that the model is anemic.

**Checkpoint:** write a one-page decision with the requirement, current evidence, selected option, rejected alternatives, compatibility impact, and a test that could disprove the expected benefit.

Next: [plan runtime improvements](domain-model-constructive-strategy-review.md).
