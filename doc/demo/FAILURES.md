# Anemic Domain Model — Compilation Failures Report

## Summary

When the **same domain-level tests** (`SettlementAwareSellTest.java`) from the rich branch are
applied to the anemic branch, they produce **33 compilation errors** across **19 test methods**.
All errors are of type `cannot find symbol` — the tests call behavioral methods that simply
**do not exist** on the anemic domain objects.

The integration tests (`SettlementSellIntegrationTest.java`) compile and pass because they invoke
the REST API, which routes through `PortfolioStockOperationsService` where the logic actually lives.

---

## Missing Methods (8 methods across 3 classes)

| Class | Missing Method | Errors | Purpose in Rich Model |
|-------|---------------|--------|-----------------------|
| `Portfolio` | `sellWithSettlement(Ticker, ShareQuantity, Price, Money, LocalDateTime)` | 14 | Orchestrates FIFO sell with settlement & fee logic |
| `Holding` | `getEligibleShares(LocalDateTime)` | 6 | Counts settled, non-reserved shares |
| `Portfolio` | `reserveLot(Ticker, LotId)` | 5 | Marks a lot as reserved (concurrency guard) |
| `Lot` | `isSettled(LocalDateTime)` | 3 | Checks if T+2 settlement has elapsed |
| `Lot` | `isAvailableForSale(LocalDateTime)` | 2 | settled AND not reserved |
| `Lot` | `reserve()` | 1 | Sets reserved flag with guard |
| `Lot` | `availableShares(LocalDateTime)` | 1 | Returns shares if available, else 0 |
| `Portfolio` | `unreserveLot(Ticker, LotId)` | 1 | Clears reserved flag |

---

## Affected Test Methods

All 19 domain tests fail to compile. Below is the mapping of each test to the missing methods it calls.

### Nested class: `SettlementFiltering`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 1 | `shouldNotSellUnsettledLots` | 97 | `Portfolio.sellWithSettlement()` |
| 2 | `shouldSellExactlySettledQuantity` | 108 | `Portfolio.sellWithSettlement()` |
| 3 | `shouldReportEligibleSharesCorrectly` | 129 | `Holding.getEligibleShares()` |
| 4 | `shouldSkipReservedLots` | 152, 156 | `Portfolio.reserveLot()`, `Portfolio.sellWithSettlement()` |
| 5 | `shouldSellFromNonReservedLots` | 169, 172 | `Portfolio.reserveLot()`, `Portfolio.sellWithSettlement()` |
| 6 | `shouldAllowUnreservingLot` | 195, 196, 198, 199 | `Portfolio.reserveLot()`, `Holding.getEligibleShares()`, `Portfolio.unreserveLot()`, `Holding.getEligibleShares()` |

### Nested class: `FeeCalculation`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 7 | `shouldDeductFeeFromProceeds` | 218 | `Portfolio.sellWithSettlement()` |
| 8 | `shouldMaintainAccountingIdentity` | 240 | `Portfolio.sellWithSettlement()` |
| 9 | `shouldRejectSellWhenFeeExceedsAvailableCash` | 270 | `Portfolio.sellWithSettlement()` |
| 10 | `shouldUpdateCashBalanceWithNetProceeds` | 282 | `Portfolio.reserveLot()`, `Portfolio.sellWithSettlement()` |

### Nested class: `FifoOrder`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 11 | `shouldConsumeSettledLotsInFifoOrder` | 309 | `Portfolio.sellWithSettlement()` |
| 12 | `shouldSkipUnsettledLotInFifoOrder` | 331 | `Portfolio.sellWithSettlement()` |

### Nested class: `EdgeCases`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 13 | `shouldPartiallyConsumeLot` | 357 | `Portfolio.reserveLot()`, `Holding.getEligibleShares()` |
| 14 | `shouldDistinguishTotalFromEligible` | 383, 386, 387 | `Holding.getEligibleShares()`, `Portfolio.reserveLot()`, `Holding.getEligibleShares()` |
| 15 | `shouldRejectZeroQuantitySell` | 396 | `Portfolio.sellWithSettlement()` |
| 16 | `shouldRejectSellForNonExistentHolding` | 405 | `Portfolio.sellWithSettlement()` |

### Nested class: `LotBehavior`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 17 | `lotShouldReportSettlementStatus` | 429, 431, 433 | `Lot.isSettled()` ×3 |
| 18 | `reservedLotShouldNotBeAvailable` | 446, 447, 448, 449 | `Lot.isAvailableForSale()`, `Lot.reserve()`, `Lot.isAvailableForSale()`, `Lot.availableShares()` |

### Nested class: `AtomicConsistency`

| # | Test Method | Lines with Errors | Missing Methods Called |
|---|------------|-------------------|----------------------|
| 19 | `fullScenarioAtomicCorrectness` | 478, 481, 487 | `Portfolio.reserveLot()`, `Holding.getEligibleShares()`, `Portfolio.sellWithSettlement()` |

---

## Root Cause Analysis

In the **anemic model**, domain objects are pure data carriers:

```java
// Anemic Lot — has data but no behavior
public class Lot {
    private LocalDateTime settlementDate;
    private boolean reserved;

    public LocalDateTime getSettlementDate() { return settlementDate; }
    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }
    // NO isSettled(), isAvailableForSale(), availableShares(), reserve()
}
```

All business logic lives in `PortfolioStockOperationsService.sellStockWithSettlement()`, which
manually iterates lots, checks dates, and mutates state — breaking encapsulation and making
domain-level unit testing impossible.

In the **rich model**, the same operations are encapsulated in the aggregate:

```java
// Rich Lot — encapsulates settlement invariants
public class Lot {
    public boolean isSettled(LocalDateTime now) {
        return !now.isBefore(settlementDate);
    }
    public boolean isAvailableForSale(LocalDateTime now) {
        return isSettled(now) && !reserved;
    }
    public int availableShares(LocalDateTime now) {
        return isAvailableForSale(now) ? remainingQuantity : 0;
    }
    public void reserve() {
        if (reserved) throw new IllegalStateException("Lot already reserved");
        this.reserved = true;
    }
}
```

---

## Conclusion

The 33 compilation errors are not bugs — they are **proof that an anemic domain model
cannot be tested at the domain level**. The only way to test settlement-aware selling
on the anemic branch is through integration tests that exercise the service layer,
which defeats the purpose of fast, isolated domain testing.

| Metric | Rich Model | Anemic Model |
|--------|-----------|--------------|
| Domain test compilation | ✅ Compiles | ❌ 33 errors |
| Domain tests run | ✅ 19/19 pass | ❌ Cannot run |
| Integration tests | ✅ 5/5 pass | ✅ 5/5 pass |
| Full suite (168 tests) | ✅ All pass | ❌ Cannot compile tests |
