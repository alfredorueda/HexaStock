# Rich vs Anemic Domain Model — Side-by-Side Comparison

## Feature: Settlement-Aware FIFO Selling with Reserved Lots and Fees

Both branches implement identical external behavior: a `POST /{id}/settlement-sales` endpoint
that sells shares respecting T+2 settlement, lot reservation, FIFO ordering, and 0.1% fee
deduction. The difference is **where** the business logic lives — and what happens when the
same logic is needed in a second place.

---

## Architecture Comparison

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RICH DOMAIN MODEL                                │
│                                                                     │
│  Controller ──► Service (thin) ──► Portfolio.sellWithSettlement()   │
│                                       ├── Holding.getEligibleShares│
│                                       ├── Lot.isSettled()          │
│                                       ├── Lot.isAvailableForSale() │
│                                       └── Lot.reserve()            │
│                                                                     │
│  Domain objects ENFORCE invariants. Service only coordinates.       │
│  ➤ Single source of truth for every business rule.                 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   ANEMIC DOMAIN MODEL                               │
│                                                                     │
│  Controller ──► Service (FAT) ──► lot.getSettlementDate()          │
│                    │                lot.isReserved()                │
│                    │                lot.setReserved(true)           │
│                    ├── calculates eligibility                      │
│                    ├── iterates lots (FIFO)                         │
│                    ├── reduces quantities                           │
│                    ├── computes fees                                │
│                    └── updates balance                              │
│                                                                     │
│  Domain objects have CONVENIENCE METHODS added later, but they     │
│  duplicate the service logic and drift out of sync silently.        │
│  ➤ Two sources of truth → inconsistency.                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The Drift Problem

In the anemic model, the service (`PortfolioStockOperationsService`) was written first with
correct logic. Later, convenience methods were added to domain objects for testability and
reuse. But because the service is the source of truth and never calls the domain methods,
the duplicated logic drifted:

| Rule | Service (correct) | Domain Object (drifted) |
|------|-------------------|------------------------|
| Lot available for sale | `!asOf.isBefore(lot.getSettlementDate()) && !lot.isReserved()` | `Lot.isAvailableForSale()` checks settlement only, forgets reservation |
| Profit formula | Computed correctly via `SellResult.withFee()` | `Portfolio.sellWithSettlement()` computes `proceeds − costBasis`, omits fee |

This is the fundamental risk: **when logic is not owned by the domain, copies diverge**.

---

## Test Results

| Test Suite | Rich Model | Anemic Model |
|-----------|-----------|--------------|
| Domain tests (19) | ✅ 19/19 pass | ❌ 11 pass, **8 fail** |
| Integration tests (5) | ✅ 5/5 pass | ✅ 5/5 pass |
| All other tests (144) | ✅ All pass | ✅ All pass |
| **Total (168)** | **✅ 168 pass** | **❌ 160 pass, 8 fail** |

The 8 failures break down as:
- **5 from Flaw 1** (Reservation Rule Drift): reserved lots treated as available
- **2 from Flaw 2** (Fee Calculation Inconsistency): profit overstated by fee amount
- **1 from both**: combined scenario catches both flaws

See [FAILURES.md](FAILURES.md) for detailed failure analysis.

---

## Code Comparison: Lot Availability

### Rich Model — Single Source of Truth

```java
// Lot.java (rich) — THE authoritative check
public boolean isAvailableForSale(LocalDateTime asOf) {
    return isSettled(asOf) && !reserved;
}
```

Holding and Portfolio delegate to this method. There is no duplication.

### Anemic Model — Duplicated Logic That Drifted

```java
// Lot.java (anemic) — convenience method added later
public boolean isAvailableForSale(LocalDateTime asOf) {
    return isSettled(asOf);
    // ↑ reservation check omitted — developer forgot
}

// PortfolioStockOperationsService.java — original, correct logic
for (Lot lot : holding.getLots()) {
    if (!asOf.isBefore(lot.getSettlementDate()) && !lot.isReserved()) {
        eligibleShares += lot.getRemainingShares().value();
    }
}
```

The service is correct, but anyone using `lot.isAvailableForSale()` gets wrong answers.

---

## Code Comparison: Fee Handling

### Rich Model — Fee Integrated in Domain

```java
// Portfolio.java (rich)
SellResult result = holding.sellSettled(qty, price, asOf);
return SellResult.withFee(result.proceeds(), result.costBasis(), fee);
// withFee() correctly computes: profit = (proceeds - fee) - costBasis
```

### Anemic Model — Fee Added Inconsistently

```java
// Portfolio.java (anemic) — convenience method added later
Money profit = grossProceeds.subtract(costBasis);
// ↑ BUG: should be grossProceeds.subtract(fee).subtract(costBasis)
balance = balance.add(netProceeds);  // ← balance is correct
return new SellResult(grossProceeds, costBasis, profit, fee);
// Profit is wrong, but balance is right — inconsistency is hard to spot
```

---

## Method-Level Comparison

### `Lot` — Settlement & Reservation

| Capability | Rich Model | Anemic Model |
|-----------|-----------|--------------|
| Is lot settled? | `lot.isSettled(asOf)` | `lot.isSettled(asOf)` — same |
| Is lot available? | `lot.isAvailableForSale(asOf)` — checks settled + !reserved | `lot.isAvailableForSale(asOf)` — ⚠️ checks settled only |
| Available shares | `lot.availableShares(asOf)` | `lot.availableShares(asOf)` — ⚠️ uses flawed availability check |
| Reserve lot | `lot.reserve()` — with guard | `lot.reserve()` — simple flag set |

### `Holding` — Eligibility Queries

| Capability | Rich Model | Anemic Model |
|-----------|-----------|--------------|
| Eligible shares | `holding.getEligibleShares(asOf)` — correct | `holding.getEligibleShares(asOf)` — ⚠️ includes reserved lots |
| Settled FIFO sell | `holding.sellSettled(qty, price, asOf)` — correct | `holding.sellSettled(qty, price, asOf)` — ⚠️ sells reserved lots |
| Find lot by ID | `holding.findLotById(lotId)` | `holding.findLotById(lotId)` — same |

### `Portfolio` — Aggregate Operations

| Capability | Rich Model | Anemic Model |
|-----------|-----------|--------------|
| Sell with settlement | `portfolio.sellWithSettlement(...)` — correct | `portfolio.sellWithSettlement(...)` — ⚠️ profit ignores fee |
| Reserve lot | `portfolio.reserveLot(ticker, lotId)` | `portfolio.reserveLot(ticker, lotId)` — same |
| Unreserve lot | `portfolio.unreserveLot(ticker, lotId)` | `portfolio.unreserveLot(ticker, lotId)` — same |

---

## Invariant Protection

| Invariant | Rich Model | Anemic Model |
|-----------|-----------|--------------|
| Cannot sell unsettled lots | Enforced in `Holding.sellSettled()` | ✅ Works (settlement check is correct) |
| Cannot sell reserved lots | Enforced in `Lot.isAvailableForSale()` | ❌ **Broken** — reservation not checked |
| Fee accounting identity | Correct in `SellResult.withFee()` | ❌ **Broken** — profit omits fee |
| FIFO ordering | Enforced in `Holding.sellSettled()` | ✅ Works (list order = insertion order) |
| Fee must not exceed cash | Checked in `Portfolio.sellWithSettlement()` | ✅ Works (balance check before sell) |

---

## Testability Impact

| Aspect | Rich Model | Anemic Model |
|--------|-----------|--------------|
| Domain unit tests | ✅ 19 tests, all pass | ❌ 19 tests, 8 fail |
| Tests compile? | ✅ Yes | ✅ Yes |
| Tests run? | ✅ Yes | ✅ Yes |
| Tests correct? | ✅ All assertions hold | ❌ Domain methods give wrong answers |
| Integration tests | ✅ 5 pass (tests real behavior) | ✅ 5 pass (service logic is correct) |
| Root cause of failures | N/A | Duplicated logic between service and domain |

---

## Key Takeaway

> **The anemic model's domain tests fail not because the methods are missing,
> but because they contain subtly wrong copies of the service's logic.**

This is worse than compilation errors — the code looks correct, compiles successfully,
and some tests pass. The failures are semantic: the domain objects return plausible
but incorrect values. In production, this manifests as data inconsistencies that are
extremely hard to diagnose.

---

## When to Use Each Approach

| Scenario | Recommendation |
|---------|---------------|
| Complex business rules with invariants | **Rich Model** — rules are self-documenting and self-enforcing |
| Simple CRUD with minimal logic | **Anemic Model** — overhead of rich model not justified |
| Multiple consumers of same logic | **Rich Model** — logic isn't duplicated across services |
| Rapid prototyping | **Anemic Model** — faster to write initially |
| Long-term maintainability | **Rich Model** — invariants survive refactoring |
| Team with OOP experience | **Rich Model** — leverages encapsulation skills |
| Team focused on procedural style | **Anemic Model** — familiar transaction-script pattern |
