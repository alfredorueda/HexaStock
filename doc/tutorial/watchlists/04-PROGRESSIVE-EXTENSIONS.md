# 4. Extend alert behavior deliberately

**Goal:** complete one optional level after the required Level 1 assignment. Each extension changes a contract; keep its specification, schema, and tests together.

## Level 2: several thresholds for one ticker

The Level 1 collection already supports AAPL thresholds 150, 140, and 130. Add a query for one ticker's alerts and demonstrate that a price of 128 matches all three. Define response order explicitly if it matters to clients.

This exercise adds access and tests; it does not need a different collection simply because it is a new lesson.

## Level 3: percentage decrease

Choose the reference first: a price captured when the alert is created, or a separately obtained previous close. The existing `StockPrice` type does not contain previous close. A per-alert creation reference cannot be replaced with one global ticker reference in the SQL query.

For a strictly positive reference, define:

```text
percentage decrease = 100 × (reference − observed price) / reference
```

For reference 200 and observed price 190, the decrease is 5%. Specify whether the boundary is inclusive, permissible percentages, rounding, and reference refresh policy. Reject a zero reference. Test above, at, and below the boundary.

An enum with nullable fields needs validation for permitted and forbidden combinations. Separate condition types can instead make invalid combinations harder to represent. Both are design options; neither avoids persistence migration and DTO changes.

## Level 4: explanatory memo

Add an optional note, for example with a 500-character limit. A memo explains the user's condition; it does not implement a contextual filter. A true filter needs its own data source and predicate.

Decide whether editing a memo changes alert identity. Usually the condition should remain the same alert. Adding a record component changes generated equality and constructor signatures, so reusing `List.contains()` over every component would accidentally permit duplicate conditions with different memos. Define a condition key or stable alert ID and test edits explicitly.

## Level 5: cooldown and delivery state

Specify a non-negative cooldown and an injected clock. At exactly the end of the cooldown, define the alert as eligible again. Include a stable alert ID before tracking its delivery state.

Cooldown controls observable behavior. Reserving an eligible alert and recording delivery are **commands**, so introduce a delivery-state port rather than hiding `markAlertTriggered` inside a query port. Concurrent workers must claim eligibility atomically; a read-then-write check can send duplicates.

Two failure windows deserve separate tests:

| Sequence | Failure consequence |
|---|---|
| Send, then record success | A crash after send can cause duplicate delivery |
| Record success, then send | A crash before send can lose the notification |

A transactional outbox can persist delivery intent atomically with relevant state. A dispatcher retries pending work; consumers or providers still need idempotency for duplicate attempts. An outbox alone does not guarantee exactly-once external delivery. A plain in-process executor can lose queued work on process failure.

Define retry limits, recovery of abandoned claims, behavior after edits/deactivation, and retention of delivery history. Changing those rules may require new ports and aggregate responsibilities. Hexagonal architecture makes dependencies explicit; it does not guarantee that new requirements only change adapters.

**Checkpoint:** test two workers attempting the same alert and a crash after external send. State precisely which duplicate or loss guarantees your design provides.

Return to the [assignment guide](WATCHLISTS-MARKET-SENTINEL.md) or read the [design review](MARKET-SENTINEL-ARCHITECTURAL-REVIEW.md).
