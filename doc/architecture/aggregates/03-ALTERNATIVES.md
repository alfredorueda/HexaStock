# 3. Compare Aggregate and History Alternatives

**Goal:** choose a design from requirements and evidence rather than a pattern ranking.

The current baseline keeps history separate and coordinates writes synchronously. The alternatives below are design options, not additional implementations on `main`.

## Four options

| Option | When it may help | Main cost or question |
|---|---|---|
| A. History inside the aggregate | A genuinely small history is needed by a business invariant. | How is growth bounded, and which operations load it? |
| B. Separate records, one database transaction | Current state and its operation record must commit together in one database. | The service must reliably create both writes. This is the baseline approach. |
| C. Events and a separate read model | Several consumers need facts about completed operations, or reads need an independently maintained model. | Publication reliability, duplicate handling, projection lag and recovery. |
| D. Limited recent context plus full external history | A specific rule needs a bounded window of recent activity. | Define the window and keep the two representations consistent. |

There is no universal threshold such as “50 transactions is safe” or “100,000 transactions always fail.” Workload, invariants and implementation determine the result.

## Examine option C carefully

A transactional outbox stores a message in the same database transaction as the state change. A relay publishes it later. A relay can publish more than once, so consumers must handle duplicates. This introduces operational work beyond calling a second repository. See [Richardson's transactional outbox pattern](https://microservices.io/patterns/data/transactional-outbox.html).

A projection built asynchronously may lag behind the command result. Define acceptable lag and recovery behavior; do not promise a fixed number of milliseconds without measurement. A failed projection usually needs retry or rebuild procedures, not automatic reversal of the original successful trade.

CQRS separates update and read models. It does not require event sourcing, a message broker or separate databases. Fowler discusses both its usefulness and its additional complexity in [CQRS](https://martinfowler.com/bliki/CQRS.html).

## Examine option D carefully

“Remember the last 20 transactions” is not equivalent to “detect duplicates in the last five minutes.” A busy portfolio could perform more than 20 operations in that interval. A time-window rule requires a retention or query strategy that actually covers the window.

Likewise, rejecting the same ticker twice is not a general idempotency mechanism. Two identical-looking trades may be intentional. An idempotency design needs an explicit request identity and rules for replayed responses.

## Make a decision you can test

Write down:

1. The invariant or query requirement driving the change.
2. The state needed to evaluate it.
3. The consistency expected by the caller.
4. The failure behavior, including retries and duplicate delivery where relevant.
5. The measurements or tests that would justify revisiting the choice.

For the present teaching use case, the separate-record approach keeps the sale flow understandable while coordinating persistence. It does not establish that the same choice is best for every financial system.

## Checkpoint

A second application needs sale notifications. Is storing a transaction record enough to guarantee delivery to it?

**Answer:** no. Saving local state and delivering an external message are different operations. A reliable publication mechanism, such as an outbox with duplicate-tolerant consumers, would be additional work.

[Previous: history and scale](02-HISTORY-AND-SCALE.md) · [Back to the learning path](../../DDD%20Portfolio%20and%20Transactions.md)
