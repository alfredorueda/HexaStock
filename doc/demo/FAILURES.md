# Anemic Domain Model — Test Failures Report

## Summary

The anemic branch compiles successfully and all 19 domain tests in
`SettlementAwareSellTest.java` execute. However, **8 out of 19 tests fail** due to
two subtle architectural flaws — the kind of inconsistency that naturally emerges when
business logic is scattered across services instead of encapsulated in the domain.

The integration tests (`SettlementSellIntegrationTest.java`) pass because they exercise
the service layer, where the logic was originally written and remains correct.

| Metric | Rich Model | Anemic Model |
|--------|-----------|--------------|
| Domain test compilation | ✅ Compiles | ✅ Compiles |
| Domain tests (19) | ✅ 19 pass | ❌ 11 pass, **8 fail** |
| Integration tests (5) | ✅ 5 pass | ✅ 5 pass |
| Full suite (168 tests) | ✅ All pass | ❌ 160 pass, 8 fail |

---

## Architectural Flaws

### Flaw 1 — Reservation Rule Drift

**Location:** `Lot.isAvailableForSale(LocalDateTime)`

In the rich model, `isAvailableForSale()` enforces both invariants:

```java
// Rich model — correct
public boolean isAvailableForSale(LocalDateTime asOf) {
    return isSettled(asOf) && !reserved;
}
```

In the anemic model, the method was added in a later sprint. The developer copied the
settlement check from the service but forgot the reservation check:

```java
// Anemic model — BUG: forgets reservation flag
public boolean isAvailableForSale(LocalDateTime asOf) {
    return isSettled(asOf);
    //  ↑ Missing: && !reserved
}
```

This is realistic: the service's `sellStockWithSettlement()` correctly checks both
`!asOf.isBefore(lot.getSettlementDate())` and `!lot.isReserved()`, but when a
convenience method was later added to `Lot`, the second condition was omitted.
The service still works because it uses its own inline check — the domain method
was never actually called by the service. It only surfaces when tests call the
domain method directly.

**Cascading effect:** `Holding.getEligibleShares()` and `Holding.sellSettled()` both
delegate to `lot.availableShares()` → `lot.isAvailableForSale()`, so they also ignore
reservations.

**Tests affected (5):**

| Test | Expected | Actual | Root Cause |
|------|----------|--------|-----------|
| `reservedLotShouldNotBeAvailable` | `isAvailableForSale` = false after reserve | true | Reservation not checked |
| `shouldSkipReservedLots` | Exception: only 5 eligible | No exception: 15 eligible | Reserved lot counted |
| `shouldSellFromNonReservedLots` | costBasis = 600 (from lot2) | 500 (from lot1) | Sold from reserved lot |
| `shouldAllowUnreservingLot` | 0 eligible after reserve | 10 eligible | Reservation has no effect |
| `shouldDistinguishTotalFromEligible` | 0 eligible after reserve | 10 eligible | Reservation has no effect |

---

### Flaw 2 — Fee Calculation Inconsistency

**Location:** `Portfolio.sellWithSettlement()`

The profit formula omits the fee deduction. The developer correctly implemented the
balance update (`balance += netProceeds`) but computed profit from gross proceeds:

```java
// Anemic model — BUG: profit ignores fee
Money profit = grossProceeds.subtract(costBasis);
//  ↑ Should be: grossProceeds.subtract(fee).subtract(costBasis)
```

This breaks the accounting identity: `costBasis + profit + fee = proceeds`.

| Component | Correct | Buggy |
|-----------|---------|-------|
| Gross proceeds | $1200 | $1200 |
| Fee | $5 | $5 |
| Net proceeds | $1195 | $1195 |
| Cost basis | $1000 | $1000 |
| Profit | **$195** | **$200** (5 too high) |
| Identity check | 1000 + 195 + 5 = 1200 ✅ | 1000 + 200 + 5 = 1205 ≠ 1200 ❌ |

**Tests affected (2 + 1 combined):**

| Test | Expected | Actual | Root Cause |
|------|----------|--------|-----------|
| `shouldDeductFeeFromProceeds` | profit = 195.00 | 200.00 | Fee omitted from profit |
| `shouldMaintainAccountingIdentity` | identity holds | 1512.00 ≠ 1500.00 | Identity broken |

---

### Combined Failure

| Test | Flaws Triggered |
|------|----------------|
| `fullScenarioAtomicCorrectness` | Flaw 1 (eligible = 18, expected 13) + Flaw 2 (profit wrong) |

---

## Passing Tests (11)

These tests do not involve reserved lots or non-zero fees, so the flaws are invisible:

| Test | Why It Passes |
|------|--------------|
| `shouldNotSellUnsettledLots` | Settlement check works correctly |
| `shouldSellExactlySettledQuantity` | No reserved lots, fee = 0 |
| `shouldReportEligibleSharesCorrectly` | No reserved lots |
| `shouldConsumeSettledLotsInFifoOrder` | FIFO works, no reserved lots, fee = 0 |
| `shouldSkipUnsettledLotInFifoOrder` | Settlement gate correct, fee = 0 |
| `shouldPartiallyConsumeLot` | No reserved lots, fee = 0 |
| `shouldRejectZeroQuantitySell` | Basic validation |
| `shouldRejectSellForNonExistentHolding` | Basic validation |
| `lotShouldReportSettlementStatus` | `isSettled()` works correctly |
| `shouldRejectSellWhenFeeExceedsAvailableCash` | Balance check occurs before sell |
| `shouldUpdateCashBalanceWithNetProceeds` | Balance uses netProceeds (correct); test only checks balance, not profit |

---

## Root Cause Analysis

Both flaws share a common root cause: **duplicated logic with subtle divergence**.

In the anemic model, business rules exist in two places:

1. **The service** (`PortfolioStockOperationsService.sellStockWithSettlement()`) — the original,
   correct implementation that manually iterates lots, checks settlement AND reservation,
   and computes fees correctly.

2. **The domain objects** (`Lot.isAvailableForSale()`, `Portfolio.sellWithSettlement()`) —
   convenience methods added later that duplicate the logic but drift from the source of truth.

In the rich model, there is only one place for each rule — the domain object itself.
The service delegates to the domain, so there is no duplication and no drift.

This is the **fundamental weakness of the anemic pattern**: when logic is not owned by the
domain, any attempt to add domain-level queries or operations risks creating an inconsistent
copy. The service remains correct while the domain silently diverges.

---

## How This Would Manifest in Production

1. **Reservation drift**: A UI that calls `getEligibleShares()` to display available quantity
   would show reserved lots as sellable. When the user tries to sell via the service, fewer
   shares would be available than displayed — causing confusing runtime errors.

2. **Fee drift**: Reports generated from `SellResult.profit()` would overstate gains by the
   fee amount. The actual cash balance would be correct (net proceeds), but P&L reports
   would not reconcile with bank statements.
