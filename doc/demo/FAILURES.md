# Anemic Domain Model — Test Failures Report

## Summary

The anemic branch compiles successfully and all 19 domain tests in
`SettlementAwareSellTest.java` execute. However, **8 out of 19 tests fail** due to
two subtle architectural flaws — the kind of inconsistency that naturally emerges when
business logic is scattered across services instead of encapsulated in the domain.

The original 5 integration tests pass because they exercise the service layer, where the
logic was originally written and remains correct. However, **2 new integration tests fail**
when alternative REST endpoints delegate to the flawed domain methods — proving that the
inconsistency is not just a unit-test concern but surfaces through real HTTP flows.

| Metric | Rich Model | Anemic Model |
|--------|-----------|--------------|
| Domain test compilation | ✅ Compiles | ✅ Compiles |
| Domain tests (19) | ✅ 19 pass | ❌ 11 pass, **8 fail** |
| Integration tests (7) | ✅ 7 pass | ❌ 5 pass, **2 fail** |
| Full suite (170 tests) | ✅ All pass | ❌ 160 pass, **10 fail** |

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

## Integration-Level Architectural Failures

The domain-level flaws also surface through REST endpoints that delegate to the domain
rather than inlining the logic in the service. These are full HTTP round-trip tests using
Testcontainers (MySQL 8.0.32) and RestAssured.

### DRIFT-REST-01 — Rule Inconsistency: Eligible Shares Ignores Reservation

**Test:** `SettlementSellIntegrationTest$RuleInconsistency.eligibleSharesShouldExcludeReservedLots`

**Endpoint:** `GET /api/v1/portfolios/{id}/holdings/{ticker}/eligible-shares`

**Scenario:**
1. Create portfolio, deposit $10,000
2. Buy 10 AAPL, then buy 5 AAPL (two separate lots)
3. Age all lots via JDBC (backdate `settlement_date` by 3 days)
4. Reserve the oldest lot (10 shares) via `POST .../lots/{lotId}/reserve`
5. Query eligible shares via the GET endpoint

**Expected:** 5 (only the unreserved lot)
**Actual:** 15 (both lots — reservation is ignored)

**Root cause:** The endpoint delegates to `PortfolioStockOperationsService.getEligibleSharesCount()`,
which calls `holding.getEligibleShares()` → `lot.isAvailableForSale()`. The domain method
only checks settlement, not the `reserved` flag (Flaw #1). The existing sell endpoint uses
inline service logic that correctly checks both conditions.

**Why this matters:** A frontend showing "15 shares available to sell" would mislead the user.
When they attempt to sell 15 via the correct sell endpoint, only 5 would actually be sold —
the two code paths disagree on business state.

---

### DRIFT-REST-02 — Fee Accounting Drift: Aggregate Sell Breaks Accounting Identity

**Test:** `SettlementSellIntegrationTest$FeeAccountingDrift.aggregateSellShouldMaintainAccountingIdentity`

**Endpoint:** `POST /api/v1/portfolios/{id}/aggregate-settlement-sales`

**Scenario:**
1. Create portfolio, deposit $10,000
2. Buy 10 AAPL at $100 (cost basis = $1,000)
3. Age lots via JDBC
4. Sell 10 AAPL via the aggregate endpoint (fixed price = $100)
5. Extract `proceeds`, `costBasis`, `profit`, `fee` from response
6. Verify accounting identity: `costBasis + profit + fee == proceeds`

**Expected:**
- Proceeds: $1,000.00
- Fee: $1.00 (0.1% of $1,000)
- Profit: −$1.00 (proceeds − fee − costBasis = 1000 − 1 − 1000)
- Identity: 1000 + (−1) + 1 = 1000 ✅

**Actual:**
- Proceeds: $1,000.00
- Fee: $1.00
- Profit: $0.00 (fee omitted from profit calculation)
- Identity: 1000 + 0 + 1 = **1001 ≠ 1000** ❌

**Root cause:** The endpoint delegates to `Portfolio.sellWithSettlement()`, which computes
`profit = grossProceeds − costBasis` without subtracting the fee (Flaw #2). The original
`sellStockWithSettlement` service method uses `SellResult.withFee()` which correctly deducts
the fee. Two different sell paths produce different profit values for the same trade.

**Why this matters:** P&L reports generated from the aggregate path would overstate profit
by the fee amount on every trade. The portfolio balance would be correct (it uses net proceeds),
but financial statements built from transaction records would not reconcile.

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
