# Exercise: extend lot selection policies

**Level:** intermediate, after the [domain-only FIFO lessons](sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md). **Baseline:** `main` implements FIFO. LIFO, HIFO and specific-lot selection below are assignment requirements, not existing features or claims about permitted tax treatment.

The teaching scenario is a product team requesting alternative lot-selection policies for simulations. Your task is to add those policies while keeping business decisions in the domain and making the necessary contract and persistence changes explicit.

## 1. Define the behavior before choosing a pattern

Mandatory requirements:

1. A portfolio receives its policy when created.
2. The policy is persisted as aggregate state and cannot be changed after creation.
3. Omitted policy means FIFO, preserving existing clients' behavior.
4. Every sale uses that portfolio's policy.
5. Support FIFO, LIFO and HIFO; keep the existing FIFO behavior covered by tests.

| Policy | Selection rule |
|---|---|
| FIFO | Oldest purchase first |
| LIFO | Newest purchase first |
| HIFO | Highest unit purchase price first |

Specify a deterministic secondary order for ties. For example, HIFO can break equal prices by purchase timestamp and then lot ID. Document the chosen ordering and preserve it across save/reload. The current timestamp ordering does not itself define a stable tie-breaker across storage implementations.

**Checkpoint:** explain why merely sorting by price is insufficient for two lots with the same price.

## 2. Calculate and test a shared example

Use three chronologically ordered lots and sell 12 shares at $150:

| Lot | Quantity | Unit price |
|---|---:|---:|
| Oldest | 10 | $100 |
| Middle | 8 | $130 |
| Newest | 5 | $110 |

| Policy | Consumed shares | Cost basis | Proceeds | Profit |
|---|---|---:|---:|---:|
| FIFO | 10 oldest + 2 middle | $1,260 | $1,800 | $540 |
| LIFO | 5 newest + 7 middle | $1,460 | $1,800 | $340 |
| HIFO | 8 middle + 4 newest | $1,480 | $1,800 | $320 |

All three leave 11 shares and credit $1,800. Their remaining lots and cost basis differ. Verify the calculations manually before implementing a strategy.

Add tests for a single lot, partial consumption, a sale spanning lots, exact depletion, ordering ties, an unknown policy at the input boundary, a missing holding, and an excessive quantity. A rejected sale must leave cash and lots unchanged. Do not remove a partially consumed lot or mutate the stored list's canonical order accidentally while selecting candidates.

Run domain tests without infrastructure:

```bash
./mvnw -pl domain test
```

## 3. Implement and connect the policy

The domain chooses and consumes lots. You may use a strategy interface, enum behavior or another cohesive design; justify the abstraction with the policies it supports. Avoid repeating policy switches in controllers, services and mappers.

| Layer | Expected change |
|---|---|
| Domain | Policy state, selection behavior and invariants |
| Application | Carry the creation choice and coordinate existing use cases |
| REST | Translate the new creation field and define invalid-value responses |
| Persistence adapters | Map and restore the policy without choosing the business algorithm |
| Bootstrap | Wiring changes only if the chosen design needs them |

A business change can legitimately affect DTOs and persistence mappings when it adds state. “Rules belong in the domain” does not mean that every other file must remain unchanged. Define how old persisted portfolios receive FIFO and how new data remains readable; the local `create-drop` setup is not a data migration strategy.

The domain must not acquire Spring, JPA or MongoDB dependencies. Production port interfaces are in `application`, not in the domain package. Preserve the current transaction and concurrency mechanisms when connecting the new behavior.

**Checkpoint:** save a non-FIFO portfolio, reload it and show that the next sale still uses its original policy.

## 4. Optional: specific lot identification

`SPECIFIC_ID` is an additional immutable portfolio policy. A sale for that policy must include chosen lot IDs and quantities. FIFO, LIFO and HIFO requests must reject explicit selections rather than silently ignoring them.

Define and test unknown IDs, lots belonging to another holding, duplicate IDs, nonpositive quantities, quantities above a lot's remainder and a selection total that differs from the requested sale quantity. Validate the whole plan before mutation. An exposed lot ID is not inherently a boundary violation; the aggregate must still enforce ownership and consistency.

This is an optional extension. Assess it separately from the mandatory three-policy work.

## 5. Optional: verify both existing persistence adapters

`main` already contains JPA/MySQL and MongoDB adapters. The updated exercise is to extend their mappings for policy state and compare save/reload behavior, rather than add MongoDB from scratch.

Choose one persistence profile at runtime: `jpa` or `mongodb`. Reuse the existing portfolio and transaction ports, keep technology-specific types in adapters and run the relevant shared contracts. Domain tests remain independent of the selected storage technology.

Also explain the different concurrency mechanisms: JPA uses a locking portfolio lookup; MongoDB uses version checks and transactions, with bounded application retry. Passing the same round-trip contract does not prove identical locking behavior. See [the concurrency lessons](CONCURRENCY-PESSIMISTIC-LOCKING.md).

## Submission and assessment

Submit a brief behavior specification, the implementation, focused domain tests, adapter evidence for the supported storage profiles, and a short design decision. Identify any unsupported optional extension explicitly.

Assess correctness first, then responsibility placement, deterministic ordering, persistence fidelity, test quality and clarity. Optional work does not substitute for the mandatory behavior and should not penalize students who omit it. Course-specific grading weights remain with the instructor.

A good explanation can trace one sale from request to chosen lots and show which test would fail if the policy were ignored.

## References

- [Sell-stock walkthrough](sellStocks/SELL-STOCK-TUTORIAL.md) and [architecture map](../architecture/ARCHITECTURE-MAP.md).
- Eric Evans, *Domain-Driven Design*, 2003; Vaughn Vernon, *Implementing Domain-Driven Design*, 2013.
- Erich Gamma, Richard Helm, Ralph Johnson and John Vlissides, *Design Patterns*, 1994, for the Strategy pattern.
