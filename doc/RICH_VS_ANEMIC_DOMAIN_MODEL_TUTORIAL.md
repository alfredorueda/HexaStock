# Rich Domain Model vs. Anemic Domain Model — A Hands-On Architecture Tutorial

> **HexaStock** — A stock portfolio management system built with Java 21, Spring Boot 3,
> and Hexagonal Architecture. This tutorial uses a real, runnable codebase to demonstrate
> how architectural decisions about domain model richness affect correctness, maintainability,
> and the ability to evolve safely under change.

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Feature Overview: Settlement-Aware FIFO Selling](#2-feature-overview-settlement-aware-fifo-selling)
3. [Repository Navigation](#3-repository-navigation)
4. [Business Rules](#4-business-rules)
5. [Architectural Overview](#5-architectural-overview)
6. [Side-by-Side Code Comparison](#6-side-by-side-code-comparison)
7. [Invariant Enforcement](#7-invariant-enforcement)
8. [Test Analysis — Why Tests Pass or Fail](#8-test-analysis--why-tests-pass-or-fail)
9. [Failure Modes of the Anemic Domain Model](#9-failure-modes-of-the-anemic-domain-model)
10. [Why the Rich Domain Model Works](#10-why-the-rich-domain-model-works)
11. [Practical Takeaways](#11-practical-takeaways)
12. [Webinar Demo Guide](#12-webinar-demo-guide)

---

## 1. Introduction

HexaStock models a personal stock portfolio where users can deposit cash, buy and sell
stocks, track holdings, and view transaction history. The application is built using
**Hexagonal Architecture** (Ports & Adapters) with a clear separation between:

- **Model** (`model/`) — Domain entities and value objects
- **Application** (`application/port/in/`, `application/port/out/`, `application/service/`) — Use cases and service orchestration
- **Adapters** (`adapter/in/`, `adapter/out/`) — REST controllers, JPA persistence, external APIs

The core domain aggregate is **Portfolio → Holding → Lot**, representing a portfolio
that owns holdings of different stocks, each holding composed of individual purchase lots.

This tutorial compares two implementation strategies for the same feature — **settlement-aware
FIFO selling with fees** — across two branches:

| Branch | Architecture | Test Results |
|--------|-------------|-------------|
| `rich-domain-model` | Business rules live inside domain entities | **170 tests, 0 failures** |
| `anemic-domain-model` | Business rules duplicated in service layer | **170 tests, 10 failures** |

Both branches share **the exact same test suite**. The 10 failures on the anemic branch are
not broken tests — they are **correct tests detecting real bugs** caused by rule drift between
duplicated implementations.

---

## 2. Feature Overview: Settlement-Aware FIFO Selling

The newly implemented feature adds three capabilities to the existing sell operation:

### 2.1 Settlement Gating (T+2 Rule)

When shares are purchased, they do not settle immediately. Following the SEC T+2 rule,
a lot becomes available for settlement-aware selling only after 2 business days:

```
Purchase Date: March 15, 2026
Settlement Date: March 17, 2026 (T+2)
```

The `Lot.SETTLEMENT_DAYS = 2` constant defines this. A lot is settled when
`!asOf.isBefore(settlementDate)`.

### 2.2 Lot Reservation

Certain lots can be reserved (e.g., used as collateral). Reserved lots must be excluded
from settlement-aware sales:

- `Lot.reserve()` — marks the lot as reserved
- `Lot.unreserve()` — releases the reservation
- A reserved lot should NOT be available for sale, even if settled

### 2.3 Fees

Settlement-aware sales incur a fee (0.1% of gross proceeds):

```
FEE_RATE = 0.001
fee = grossProceeds × FEE_RATE
netProceeds = grossProceeds − fee
profit = netProceeds − costBasis
```

The accounting identity that must always hold:

```
costBasis + profit + fee = proceeds
```

### 2.4 FIFO Lot Consumption

When selling, lots are consumed in purchase-date order (First In, First Out), but only
lots that are both **settled** and **not reserved** participate. Unsettled or reserved
lots are skipped. Partial consumption of a lot is supported — the remainder stays in
the holding.

---

## 3. Repository Navigation

### 3.1 Clone and Switch Branches

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
```

#### Rich Domain Model (all tests pass)
```bash
git checkout rich-domain-model
./mvnw test          # Expected: 170 tests, 0 failures
```

#### Anemic Domain Model (10 tests fail)
```bash
git checkout anemic-domain-model
./mvnw test          # Expected: 170 tests, 10 failures
                     #   8 domain-level (SettlementAwareSellTest)
                     #   2 integration-level (SettlementSellIntegrationTest)
```

### 3.2 Key Source Files

| File | Path |
|------|------|
| **Portfolio** (Aggregate Root) | `src/main/java/cat/gencat/agaur/hexastock/model/Portfolio.java` |
| **Holding** (Entity) | `src/main/java/cat/gencat/agaur/hexastock/model/Holding.java` |
| **Lot** (Entity) | `src/main/java/cat/gencat/agaur/hexastock/model/Lot.java` |
| **SellResult** (Value Object) | `src/main/java/cat/gencat/agaur/hexastock/model/SellResult.java` |
| **Service** (Application Layer) | `src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java` |
| **Use Case Port** | `src/main/java/cat/gencat/agaur/hexastock/application/port/in/PortfolioStockOperationsUseCase.java` |
| **REST Controller** | `src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java` |
| **Domain Tests** | `src/test/java/cat/gencat/agaur/hexastock/model/SettlementAwareSellTest.java` |
| **Integration Tests** | `src/test/java/cat/gencat/agaur/hexastock/adapter/in/SettlementSellIntegrationTest.java` |
| **Test Base Class** | `src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java` |

### 3.3 Diagrams and Specifications

| Artifact | Path |
|----------|------|
| Rich Architecture Diagram | `doc/diagrams/rich-architecture.puml` |
| Anemic Architecture Diagram | `doc/diagrams/anemic-architecture.puml` |
| Rich Sell Sequence | `doc/diagrams/rich-sell-sequence.puml` |
| Anemic Sell Sequence | `doc/diagrams/anemic-sell-sequence.puml` |
| Invariant Enforcement | `doc/diagrams/invariant-enforcement.puml` |
| Domain Model Class Diagram | `doc/diagrams/domain-model.puml` |
| Rule Drift Timeline | `doc/diagrams/rule-drift.puml` |
| Settlement Selling Spec | `doc/features/settlement-aware-selling.feature` |
| Reserved Lots Spec | `doc/features/reserved-lot-handling.feature` |
| Fee Accounting Spec | `doc/features/settlement-fees.feature` |
| FIFO Settlement Spec | `doc/features/fifo-settlement-selling.feature` |
| Rule Consistency Spec | `doc/features/rule-consistency.feature` |

---

## 4. Business Rules

The settlement-aware selling feature enforces five categories of rules. Each rule is
traced to its implementation in both branches and to the test(s) that verify it.

### Rule 1 — Settlement Gate

> A lot is available for settlement-aware selling only if its settlement date has passed.

- **Implementation**: `Lot.isSettled(asOf)` → `!asOf.isBefore(settlementDate)`
- **Both branches**: ✅ Correct (identical implementation)
- **Tests**: `shouldNotSellUnsettledLots`, `shouldSellExactlySettledQuantity`, `shouldReportEligibleSharesCorrectly`

### Rule 2 — Reservation Exclusion

> A reserved lot must be excluded from settlement-aware sales and from eligible share counts.

- **Rich branch**: ✅ `Lot.isAvailableForSale(asOf)` → `isSettled(asOf) && !reserved`
- **Anemic branch**: ❌ `Lot.isAvailableForSale(asOf)` → `isSettled(asOf)` — **missing `&& !reserved`**
- **Tests**: `reservedLotShouldNotBeAvailable`, `shouldSkipReservedLots`, `shouldSellFromNonReservedLots`, `shouldAllowUnreservingLot`, `shouldDistinguishTotalFromEligible`

### Rule 3 — Fee-Adjusted Profit

> Profit must account for fees: `profit = netProceeds − costBasis = (grossProceeds − fee) − costBasis`.

- **Rich branch**: ✅ `Portfolio.sellWithSettlement()` uses `SellResult.withFee(proceeds, costBasis, fee)`
- **Anemic branch**: ❌ `Portfolio.sellWithSettlement()` computes `profit = grossProceeds.subtract(costBasis)` — **fee omitted**
- **Tests**: `shouldDeductFeeFromProceeds`, `shouldMaintainAccountingIdentity`, `fullScenarioAtomicCorrectness`

### Rule 4 — FIFO Lot Consumption

> Lots must be consumed in purchase-date order. Only settled, non-reserved lots participate.

- **Both branches**: ✅ FIFO ordering is correct when using the correct eligibility check
- **Anemic branch nuance**: The service's inline FIFO logic is correct (checks settlement AND reservation), but the domain's `Holding.sellSettled()` calls the flawed `Lot.isAvailableForSale()` 
- **Tests**: `shouldConsumeSettledLotsInFifoOrder`, `shouldSkipUnsettledLotInFifoOrder`

### Rule 5 — Accounting Identity

> The identity `costBasis + profit + fee = proceeds` must always hold after a sale.

- **Rich branch**: ✅ Guaranteed by `SellResult.withFee()` factory method
- **Anemic branch**: ❌ Violated when using `Portfolio.sellWithSettlement()` because profit formula omits fee
- **Tests**: `shouldMaintainAccountingIdentity`, `aggregateSellShouldMaintainAccountingIdentity`

---

## 5. Architectural Overview

### 5.1 Rich Domain Model Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        REST Controller                              │
│   POST /settlement-sales → sellStockWithSettlement()                │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│                 Application Service (~20 lines)                     │
│                                                                     │
│   1. Load portfolio from repository                                 │
│   2. Fetch stock price from external provider                       │
│   3. Compute fee = grossProceeds × 0.001                            │
│   4. Delegate to portfolio.sellWithSettlement(...)   ◄── ONE CALL   │
│   5. Save portfolio                                                 │
│   6. Record transaction                                             │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│                    Portfolio Aggregate                               │
│                                                                     │
│   sellWithSettlement():                                             │
│     • Validate eligible shares ≥ requested quantity                 │
│     • Validate net proceeds won't cause negative balance            │
│     • Delegate FIFO selling to Holding.sellSettled()                │
│     • Update balance with netProceeds                               │
│     • Return SellResult.withFee(proceeds, costBasis, fee)           │
│                                                                     │
│   Holding.sellSettled():                                            │
│     • Iterate lots in FIFO order                                    │
│     • Skip lots where !isAvailableForSale(asOf)                     │
│     • Consume shares from eligible lots                             │
│     • Return intermediate SellResult                                │
│                                                                     │
│   Lot.isAvailableForSale():                                         │
│     • return isSettled(asOf) && !reserved     ◄── SINGLE TRUTH      │
└─────────────────────────────────────────────────────────────────────┘
```

**Key characteristic**: The service is a thin coordination layer. All business rules
(settlement check, reservation check, FIFO, fee handling, accounting identity) are
enforced inside the aggregate. There is exactly **one code path** for each rule.

### 5.2 Anemic Domain Model Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         REST Controller                              │
│   POST /settlement-sales         → sellStockWithSettlement()         │
│   POST /aggregate-settlement-sales → sellStockWithSettlementAggregate│
│   GET  /eligible-shares          → getEligibleSharesCount()          │
│   POST /lots/{id}/reserve        → reserveLot()                      │
└──┬────────────────────────┬──────────────────────────────────────────┘
   │                        │
   │  PATH A                │  PATH B
   │  (Inline service       │  (Delegates to
   │   logic — CORRECT)     │   domain — FLAWED)
   │                        │
┌──▼────────────────────────▼──────────────────────────────────────────┐
│                  Application Service (~70 lines for PATH A)          │
│                                                                      │
│  PATH A — sellStockWithSettlement():                                 │
│    1. Load portfolio, fetch price                                    │
│    2. Inline eligibility scan:                                       │
│       for each lot:                                                  │
│         if !empty && settled && !reserved → count as eligible        │
│    3. If eligible < requested → throw                                │
│    4. Inline FIFO selling:                                           │
│       for each lot:                                                  │
│         skip if unsettled OR reserved OR empty                       │
│         reduce lot, accumulate costBasis                             │
│    5. Use SellResult.withFee()                                       │
│    6. portfolio.deposit(netProceeds)                                 │
│    7. Save + record transaction                                      │
│                                                                      │
│  PATH B — sellStockWithSettlementAggregate():                        │
│    1. Load portfolio, fetch price, compute fee                       │
│    2. Delegate: portfolio.sellWithSettlement(...)                     │
│    3. Save + record transaction                                      │
│                                                                      │
│  getEligibleSharesCount():                                           │
│    1. Delegate: holding.getEligibleShares(now)                       │
│       → calls Lot.isAvailableForSale()  ◄── FLAWED                  │
└──────────────────────────────────────────────────────────────────────┘
                      │
┌─────────────────────▼────────────────────────────────────────────────┐
│                    Portfolio Aggregate (PASSIVE)                      │
│                                                                      │
│   sellWithSettlement():                                              │
│     • profit = grossProceeds − costBasis  ◄── FLAW: fee omitted      │
│                                                                      │
│   Lot.isAvailableForSale():                                          │
│     • return isSettled(asOf)               ◄── FLAW: reservation     │
│                                                    check missing     │
└──────────────────────────────────────────────────────────────────────┘
```

**Key characteristic**: The service contains correct inline logic, but two additional
code paths (aggregate-sell endpoint and eligible-shares query) delegate to domain
methods that contain **stale, incomplete** implementations of the same rules.

> 📐 See `doc/diagrams/rich-architecture.puml` and `doc/diagrams/anemic-architecture.puml`
> for PlantUML diagrams of these architectures.

---

## 6. Side-by-Side Code Comparison

### 6.1 Lot Availability Check — The Reservation Flaw

This is the root cause of **Flaw #1** — the reservation check is missing from the
anemic branch's domain method.

#### Rich Branch — `Lot.isAvailableForSale()`

```java
/**
 * Checks if this lot is available for sale: must be settled AND not reserved.
 */
public boolean isAvailableForSale(LocalDateTime asOf) {
    return isSettled(asOf) && !reserved;
}
```

#### Anemic Branch — `Lot.isAvailableForSale()`

```java
/**
 * Checks if this lot is available for sale: settled and not reserved.
 *
 * <p>Note: in the anemic model this is a passive data query — the service
 *    is responsible for actually enforcing the constraint during sell.</p>
 */
public boolean isAvailableForSale(LocalDateTime asOf) {
    // BUG: only checks settlement, forgets reservation flag
    return isSettled(asOf);
}
```

**What happened**: Settlement was implemented in Sprint 10. Reservation was added in
Sprint 12 by a different developer who updated the service's inline FIFO logic but
forgot to update this convenience method. When Sprint 14 added the eligible-shares
query endpoint, it called this method — inheriting the bug.

### 6.2 Portfolio Settlement Sell — The Fee Flaw

This is the root cause of **Flaw #2** — the fee is omitted from the profit calculation.

#### Rich Branch — `Portfolio.sellWithSettlement()`

```java
// Delegate FIFO selling to holding (only settled, non-reserved lots)
SellResult intermediateResult = holding.sellSettled(quantity, price, asOf);

// Update balance with net proceeds (after fee)
balance = balance.add(netProceeds);

// Return fee-aware result
return SellResult.withFee(intermediateResult.proceeds(), intermediateResult.costBasis(), fee);
```

The `SellResult.withFee()` factory correctly computes:
```java
public static SellResult withFee(Money proceeds, Money costBasis, Money fee) {
    Money netProceeds = proceeds.subtract(fee);
    Money profit = netProceeds.subtract(costBasis);  // ← fee deducted from profit
    return new SellResult(proceeds, costBasis, profit, fee);
}
```

#### Anemic Branch — `Portfolio.sellWithSettlement()`

```java
// Delegate FIFO lot consumption to Holding
SellResult baseResult = holding.sellSettled(quantity, sellPrice, asOf);
Money costBasis = baseResult.costBasis();

// BUG (Flaw #2 — Fee Calculation Inconsistency):
// Profit should be (grossProceeds − fee) − costBasis = netProceeds − costBasis.
// The anemic model computes it as grossProceeds − costBasis, omitting the fee.
Money profit = grossProceeds.subtract(costBasis);  // ← fee NOT deducted

balance = balance.add(netProceeds);  // ← balance update is correct

return new SellResult(grossProceeds, costBasis, profit, fee);
```

**What happened**: The fee parameter was added in Sprint 12. The developer correctly
updated the balance logic (`balance.add(netProceeds)`) but computed profit using the
pre-fee formula. The service's own `sellStockWithSettlement()` uses `SellResult.withFee()`
and gets the right answer — but the domain method disagrees.

### 6.3 Service Layer — Thin vs. Fat

#### Rich Branch — Service (`~20 lines`)

```java
@Override
public SellResult sellStockWithSettlement(PortfolioId portfolioId, Ticker ticker,
                                           ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
    Price price = stockPrice.price();

    // Calculate fee as 0.1% of gross proceeds
    Money grossProceeds = price.multiply(quantity);
    Money fee = Money.of(grossProceeds.amount().multiply(FEE_RATE));

    LocalDateTime asOf = LocalDateTime.now();

    // All business rules enforced inside the aggregate
    SellResult sellResult = portfolio.sellWithSettlement(ticker, quantity, price, fee, asOf);
    portfolioPort.savePortfolio(portfolio);

    Transaction transaction = Transaction.createSaleWithFee(
            portfolioId, ticker, quantity, price,
            sellResult.proceeds(), sellResult.profit(), sellResult.fee());
    transactionPort.save(transaction);

    return sellResult;
}
```

The service handles only **infrastructure concerns**: loading, saving, fetching prices,
recording transactions. The single call to `portfolio.sellWithSettlement()` delegates
all domain logic to the aggregate.

#### Anemic Branch — Service (`~70 lines for sellStockWithSettlement alone + 3 additional methods`)

```java
@Override
public SellResult sellStockWithSettlement(PortfolioId portfolioId, Ticker ticker,
                                           ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
    // ... fetch price ...

    LocalDateTime asOf = LocalDateTime.now();

    // Calculate eligible shares — logic in service, NOT in domain
    int eligibleShares = 0;
    for (Lot lot : holding.getLots()) {
        if (!lot.isEmpty()
                && lot.getSettlementDate() != null
                && !asOf.isBefore(lot.getSettlementDate())
                && !lot.isReserved()) {                    // ← correct here
            eligibleShares += lot.getRemainingShares().value();
        }
    }
    if (eligibleShares < quantity.value()) { throw ...; }

    // ... fee calculation ...

    // FIFO sell — logic in service, NOT in Holding
    ShareQuantity remainingToSell = quantity;
    Money costBasis = Money.ZERO;
    for (Lot lot : holding.getLots()) {
        if (remainingToSell.isZero()) break;
        if (lot.getSettlementDate() == null || asOf.isBefore(lot.getSettlementDate())) continue;
        if (lot.isReserved()) continue;                    // ← correct here
        if (lot.isEmpty()) continue;

        ShareQuantity sharesSold = lot.getRemainingShares().min(remainingToSell);
        costBasis = costBasis.add(lot.calculateCostBasis(sharesSold));
        lot.reduce(sharesSold);
        remainingToSell = remainingToSell.subtract(sharesSold);
    }

    portfolio.deposit(netProceeds);
    SellResult result = SellResult.withFee(grossProceeds, costBasis, fee);  // ← correct here
    // ... save, record ...
    return result;
}
```

This service method is **correct** — it checks settlement AND reservation in its inline
logic, and uses `SellResult.withFee()`. But the logic exists only here. Two other
code paths delegate to the domain, where the rules are broken:

| Method | Delegates to | Result |
|--------|-------------|--------|
| `sellStockWithSettlement()` | Inline service logic | ✅ Correct |
| `sellStockWithSettlementAggregate()` | `Portfolio.sellWithSettlement()` | ❌ Flawed profit |
| `getEligibleSharesCount()` | `Holding.getEligibleShares()` | ❌ Includes reserved lots |

### 6.4 Use Case Port — 3 vs. 6 Methods

#### Rich Branch — `PortfolioStockOperationsUseCase` (3 methods)

```java
void buyStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
SellResult sellStockWithSettlement(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
```

#### Anemic Branch — `PortfolioStockOperationsUseCase` (6 methods)

```java
void buyStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
SellResult sellStockWithSettlement(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity);
int getEligibleSharesCount(PortfolioId portfolioId, Ticker ticker);        // ← new
void reserveLot(PortfolioId portfolioId, Ticker ticker, LotId lotId);      // ← new
SellResult sellStockWithSettlementAggregate(PortfolioId portfolioId,        // ← new
                                            Ticker ticker, ShareQuantity quantity);
```

The anemic branch leaks internal aggregate structure (lot IDs, eligibility queries)
through the use case port, coupling the application layer to implementation details.

### 6.5 REST Controller — Standard vs. Expanded API Surface

The anemic branch adds **three extra endpoints** that expose aggregate internals:

| Endpoint | Purpose | Flaw |
|----------|---------|------|
| `GET /{id}/holdings/{ticker}/eligible-shares` | Query eligible shares | Delegates to flawed domain method |
| `POST /{id}/holdings/{ticker}/lots/{lotId}/reserve` | Reserve a specific lot | Exposes lot IDs to API consumers |
| `POST /{id}/aggregate-settlement-sales` | Alternate sell path | Delegates to flawed `Portfolio.sellWithSettlement()` |

---

## 7. Invariant Enforcement

An **invariant** is a condition that must always be true. In DDD, the aggregate root
is responsible for enforcing invariants. Let's trace how each architecture handles them.

### 7.1 Invariant: "Reserved lots cannot be sold"

**Rich branch** — enforced at the entity level:

```
Portfolio.sellWithSettlement()
  → Holding.sellSettled()
    → for each lot: if !lot.isAvailableForSale(asOf) → skip
      → Lot.isAvailableForSale(): return isSettled(asOf) && !reserved  ✅
```

There is ONE code path. The invariant is enforced in the entity where the data lives.
Any code that asks "is this lot available?" gets the correct answer.

**Anemic branch** — enforced in one service method, missed in another:

```
PATH A: Service.sellStockWithSettlement()
  → inline: if (lot.isReserved()) continue;  ✅ correct

PATH B: Service.getEligibleSharesCount()
  → Holding.getEligibleShares()
    → Lot.isAvailableForSale(): return isSettled(asOf)  ❌ reservation not checked
```

The invariant is enforced in the service's inline FIFO loop but NOT in the domain's
convenience method. Any new code that calls `Lot.isAvailableForSale()` will get the
wrong answer for reserved lots.

### 7.2 Invariant: "Profit must account for fees"

**Rich branch** — enforced by a single factory method:

```
Portfolio.sellWithSettlement()
  → return SellResult.withFee(proceeds, costBasis, fee)
    → profit = proceeds - fee - costBasis  ✅
```

The `SellResult.withFee()` factory method is the ONLY way to construct a fee-aware
result. It's impossible to get the formula wrong because the formula lives in one place.

**Anemic branch** — two different formulas:

```
PATH A: Service.sellStockWithSettlement()
  → SellResult.withFee(grossProceeds, costBasis, fee)
    → profit = grossProceeds - fee - costBasis  ✅

PATH B: Portfolio.sellWithSettlement()
  → profit = grossProceeds.subtract(costBasis)  ❌ fee omitted
  → return new SellResult(grossProceeds, costBasis, profit, fee)
```

The domain method constructs `SellResult` directly instead of using the factory,
embedding a stale formula.

> 📐 See `doc/diagrams/invariant-enforcement.puml` for a visual comparison.

---

## 8. Test Analysis — Why Tests Pass or Fail

This is the most critical section. Both branches run the **exact same** 19-test
domain test class (`SettlementAwareSellTest`) and the **exact same** 7-test
integration test class (`SettlementSellIntegrationTest`).

### 8.1 Domain Tests — `SettlementAwareSellTest` (19 tests)

All tests call `portfolio.sellWithSettlement()` or inspect domain objects directly.
On the rich branch, the aggregate enforces all invariants. On the anemic branch,
the domain methods have incomplete logic.

#### Tests that PASS on both branches (11 of 19)

These tests do not involve reservation or fee verification:

| # | Test | Nested Class | Why it passes |
|---|------|-------------|---------------|
| 1 | `shouldNotSellUnsettledLots` | SettlementGateTests | Settlement logic is identical |
| 2 | `shouldSellExactlySettledQuantity` | SettlementGateTests | No reservation, no fee |
| 3 | `shouldReportEligibleSharesCorrectly` | SettlementGateTests | No reserved lots in this test |
| 4 | `shouldConsumeSettledLotsInFifoOrder` | FifoTests | No reservation, fee=ZERO |
| 5 | `shouldSkipUnsettledLotInFifoOrder` | FifoTests | No reservation, fee=ZERO |
| 6 | `shouldPartiallyConsumeLot` | PartialLotTests | No reservation, fee=ZERO |
| 7 | `shouldRejectZeroQuantitySell` | EligibilityTests | Input validation, not invariant |
| 8 | `shouldRejectSellForNonExistentHolding` | EligibilityTests | Input validation |
| 9 | `lotShouldReportSettlementStatus` | LotSettlementTests | `isSettled()` — identical |
| 10 | `shouldRejectSellWhenFeeExceedsAvailableCash` | FeeTests | Balance check, not profit formula |
| 11 | `shouldUpdateCashBalanceWithNetProceeds` | FeeTests | Balance uses `netProceeds` — correct on both |

#### Tests that FAIL on the anemic branch (8 of 19)

##### Flaw #1 Failures — Reservation check missing from `Lot.isAvailableForSale()`

| # | Test | Expected | Actual (anemic) | Root Cause |
|---|------|----------|-----------------|------------|
| 12 | `reservedLotShouldNotBeAvailable` | `isAvailableForSale()` → `false` | `true` | Missing `&& !reserved` |
| 13 | `shouldSkipReservedLots` | Throws `InsufficientEligibleSharesException` | Sells reserved lot | Eligible count includes reserved |
| 14 | `shouldSellFromNonReservedLots` | CostBasis=600 (from lot2 only) | CostBasis=500 (from lot1 first) | Reserved lot1 not skipped |
| 15 | `shouldAllowUnreservingLot` | Eligible=0 when reserved, 10 when unreserved | Eligible=10 always | Reservation ignored |
| 16 | `shouldDistinguishTotalFromEligible` | After reservation: eligible=0 | eligible=10 | Same as above |

**Explanation**: All five tests reserve a lot and then verify that the reservation
affects availability. On the anemic branch, `Lot.isAvailableForSale()` only checks
`isSettled(asOf)`, so reservation has no effect on queries through the domain.

##### Flaw #2 Failures — Fee omitted from profit calculation

| # | Test | Expected | Actual (anemic) | Root Cause |
|---|------|----------|-----------------|------------|
| 17 | `shouldDeductFeeFromProceeds` | profit=195 (=1195−1000) | profit=200 (=1200−1000) | Fee not subtracted |
| 18 | `shouldMaintainAccountingIdentity` | costBasis+profit+fee=proceeds | costBasis+profit+fee≠proceeds | Profit inflated |

**Explanation**: These tests call `portfolio.sellWithSettlement()` with a non-zero fee and
verify the profit value. The anemic branch's domain method computes
`profit = grossProceeds - costBasis` instead of `profit = (grossProceeds - fee) - costBasis`.

##### Combined Failure — Both flaws

| # | Test | Outcome |
|---|------|---------|
| 19 | `fullScenarioAtomicCorrectness` | Fails because it reserves a lot AND checks profit with fee |

This test is a comprehensive end-to-end domain scenario that:
1. Creates 4 lots (settled+reserved, settled, settled, unsettled)
2. Reserves one lot
3. Sells 12 shares with a $10 fee
4. Verifies cost basis, profit, fee, accounting identity, and remaining lots

It fails on the anemic branch because of BOTH flaws: the reserved lot is included
in eligibility/FIFO (Flaw #1), and the profit omits the fee (Flaw #2).

### 8.2 Integration Tests — `SettlementSellIntegrationTest` (7 tests)

#### Tests that PASS on both branches (5 of 7)

| Test | Nested Class | Why it passes |
|------|-------------|---------------|
| `sellUnsettledLots_returns409` | SettlementGateRest | Both branches reject unsettled lots via the original endpoint |
| `regularSellStillWorks` | SettlementGateRest | Regular sell doesn't use settlement logic |
| `regularSaleIncludesZeroFee` | FeeInResponse | Regular sell returns fee=0 on both |
| `holdingsSurviveRoundTrip` | PersistenceRoundTrip | Tests persistence, not settlement invariants |
| `transactionRecordsExistAfterRegularSale` | PersistenceRoundTrip | Tests transaction recording |

#### Tests that FAIL on the anemic branch (2 of 7)

These tests exist only on the anemic branch's test file (the rich branch's integration
test class has 5 tests; the anemic branch adds 2 more):

##### `eligibleSharesShouldExcludeReservedLots` (RuleInconsistency)

```java
// Buy 10 + 5 shares of AAPL → age lots → reserve oldest
// Query: GET /holdings/AAPL/eligible-shares
// Expected: 5 (only unreserved lot)
// Actual:  15 (both lots — domain method ignores reservation)
int eligible = getEligibleSharesCount(portfolioId, "AAPL");
assertEquals(5, eligible, "...");
```

**Why it fails**: The endpoint calls `Holding.getEligibleShares()` which calls
`Lot.isAvailableForSale()` which only checks settlement. The reserved lot is
counted as eligible.

**Why this endpoint doesn't exist on the rich branch**: The rich domain model
doesn't need a separate eligible-shares endpoint because the aggregate already
enforces the invariant internally. An external query would be duplicating logic
that belongs inside the aggregate.

##### `aggregateSellShouldMaintainAccountingIdentity` (FeeAccountingDrift)

```java
// Buy 10 shares at $100 → age lots
// Sell via POST /aggregate-settlement-sales
// Expected: costBasis + profit + fee = proceeds
//   proceeds=1000, fee=1, costBasis=1000 → profit should be -1
//   identity: 1000 + (-1) + 1 = 1000 ✓
// Actual: profit = 0 (fee omitted)
//   identity: 1000 + 0 + 1 = 1001 ≠ 1000 ✗
```

**Why it fails**: This endpoint delegates to `Portfolio.sellWithSettlement()` which
computes `profit = grossProceeds - costBasis` (omitting fee). The accounting identity
is violated.

### 8.3 How the Integration Tests Backdated Settlement

The anemic branch's `AbstractPortfolioRestIntegrationTest` includes a JDBC-based helper
that backdates settlement dates to make lots appear settled:

```java
@Autowired JdbcTemplate jdbcTemplate;

protected void settleLots(String portfolioId) {
    jdbcTemplate.update(
        "UPDATE lot SET settlement_date = DATE_SUB(NOW(), INTERVAL 5 DAY) " +
        "WHERE holding_id IN (SELECT id FROM holding WHERE portfolio_id = ?)",
        portfolioId);
}
```

The rich branch does not need this helper because its integration tests only test the
settlement gate (rejecting unsettled lots) and don't need pre-settled lots.

---

## 9. Failure Modes of the Anemic Domain Model

### 9.1 Rule Duplication

The same business rule ("is this lot available for sale?") exists in two places:

1. **Service `sellStockWithSettlement()`** — inline loop with `!lot.isReserved()` → ✅ correct
2. **`Lot.isAvailableForSale()`** — only `isSettled(asOf)` → ❌ incomplete

When rules are duplicated, they can diverge. The developer who added reservation
updated the service logic but forgot the domain method.

### 9.2 Rule Drift Across Sprints

```
Sprint 10: Settlement feature added
  → Lot.isAvailableForSale() = isSettled(asOf)
  → Service checks settlement inline
  → Both agree: ✅

Sprint 12: Reservation feature added (different developer)
  → Service updated: checks settlement AND reservation ✅
  → Lot.isAvailableForSale() NOT updated ❌
  → They now disagree

Sprint 14: Eligible-shares query endpoint added
  → Delegates to Holding.getEligibleShares()
  → Calls Lot.isAvailableForSale() (now stale)
  → Bug shipped to production ❌
```

> 📐 See `doc/diagrams/rule-drift.puml` for a timeline visualization.

### 9.3 Hidden Coupling and Coordination Burden

The anemic branch's service is correct — but its correctness is **fragile**. Any new
feature that needs the "is available for sale?" check faces a choice:

1. Call `Lot.isAvailableForSale()` — fast but wrong (doesn't check reservation)
2. Duplicate the inline logic — correct but creates another copy to maintain
3. Extract a shared utility — adds indirection without solving the root cause

In the rich model, there is no choice to make. You call `Lot.isAvailableForSale()`
and get the right answer. The entity owns its own rules.

### 9.4 API Surface Bloat

The anemic branch requires three additional REST endpoints to expose operations that
the domain should handle internally:

- **Eligible shares query** — needed because the client can't trust the domain to
  enforce the right rules during a sell
- **Reserve lot** — exposes internal lot IDs through the API boundary
- **Aggregate sell** — a "cleaner" alternative that delegates to the domain, but
  the domain is broken

These endpoints increase the attack surface, the documentation burden, and the number
of integration tests required.

---

## 10. Why the Rich Domain Model Works

### 10.1 Single Point of Truth

Every business rule is implemented exactly once, inside the entity that owns the data:

| Rule | Owner | Method |
|------|-------|--------|
| Is this lot available? | `Lot` | `isAvailableForSale()` |
| How many eligible shares? | `Holding` | `getEligibleShares()` |
| FIFO lot consumption | `Holding` | `sellSettled()` |
| Fee-adjusted profit | `Portfolio` | via `SellResult.withFee()` |
| Balance update | `Portfolio` | `sellWithSettlement()` |

No rule is duplicated. No rule can drift.

### 10.2 Aggregate Boundary

The `Portfolio` aggregate root coordinates the entire sell operation:

1. Validates preconditions (eligible shares, cash headroom)
2. Delegates FIFO consumption to `Holding`
3. Updates balance
4. Returns a correct `SellResult`

External code (service, controller, other aggregates) cannot bypass these checks
because the only public method for settlement-aware selling is
`Portfolio.sellWithSettlement()`.

### 10.3 Testability Without Infrastructure

All 19 domain tests run without Spring, without a database, without HTTP. They
instantiate domain objects directly and verify behavior. This makes them:

- **Fast** — milliseconds per test
- **Isolated** — no flaky infrastructure dependencies
- **Comprehensive** — every rule is testable at the unit level

### 10.4 Safe Evolution

When a new rule is added (e.g., "lots under regulatory hold cannot be sold"), there
is exactly one place to add it: `Lot.isAvailableForSale()`. All callers automatically
get the new check. In the anemic model, you'd need to update every inline loop that
checks lot availability.

---

## 11. Practical Takeaways

### When to Use a Rich Domain Model

- Complex business rules with multiple interacting invariants
- Rules that evolve independently across sprints/teams
- Aggregates with internal consistency requirements
- When the cost of a rule violation is high (financial, regulatory)

### When an Anemic Model May Suffice

- CRUD-heavy applications with minimal business logic
- Rapid prototyping where rules are still being discovered
- Simple validation-only logic (field constraints, format checks)
- Read-heavy query models (CQRS read side)

### Key Principles

1. **Encapsulate rules where the data lives** — If `Lot` has a `reserved` field,
   `Lot` should know what that means for availability
2. **Use factory methods to prevent formula drift** — `SellResult.withFee()` makes
   it impossible to get the accounting wrong
3. **Keep services thin** — Services should orchestrate, not calculate. Infrastructure
   concerns (load, save, fetch price, record transaction) belong in services;
   business rules belong in the domain
4. **Let tests detect drift** — The same test suite on both branches proves that
   domain tests are effective architectural guardians
5. **Resist the urge to add convenience endpoints** — If you need a query endpoint
   to check something the aggregate should enforce, the aggregate is too passive

---

## 12. Webinar Demo Guide

### Pre-requisites

- Java 21+
- Docker (for Testcontainers MySQL)
- Git
- IntelliJ IDEA or VS Code with Java extensions

### Demo Flow (30–40 minutes)

#### Step 1 — Show the Rich Model Passing (5 min)

```bash
git checkout rich-domain-model
./mvnw test
```

All 170 tests pass. Highlight the domain test class `SettlementAwareSellTest` — show
that it runs without Spring context and tests the aggregate directly.

#### Step 2 — Walk Through the Code (10 min)

Open `Lot.java` and show `isAvailableForSale()`:
```java
return isSettled(asOf) && !reserved;
```

Open `Portfolio.java` and show `sellWithSettlement()`:
- Highlight `SellResult.withFee()` call at the bottom
- Show that the method does validation, delegates FIFO to `Holding`, and returns a guaranteed-correct result

Open `PortfolioStockOperationsService.java` and show `sellStockWithSettlement()`:
- Count the lines (~20)
- Point out: no loops, no lot iteration, no eligibility checks — just load, delegate, save

#### Step 3 — Switch to the Anemic Branch (2 min)

```bash
git checkout anemic-domain-model
./mvnw test
```

**10 failures** appear. Let the output sink in — same tests, different results.

#### Step 4 — Examine the Failures (10 min)

Open the test output and walk through 2–3 representative failures:

1. **`reservedLotShouldNotBeAvailable`** — Show the assertion:
   `assertFalse(lot.isAvailableForSale(NOW))` after `lot.reserve()`.
   Open `Lot.java` and show the missing `&& !reserved`.

2. **`shouldMaintainAccountingIdentity`** — Show the assertion:
   `assertEquals(result.proceeds(), identity)` where `identity = costBasis + profit + fee`.
   Open `Portfolio.java` and show `profit = grossProceeds.subtract(costBasis)` — fee is missing.

3. **`eligibleSharesShouldExcludeReservedLots`** — Show the integration test that
   reserves a lot, queries eligible shares, and gets the wrong answer. Trace the call
   from controller → service → `Holding.getEligibleShares()` → `Lot.isAvailableForSale()`.

#### Step 5 — Show the Fat Service (5 min)

Open the anemic branch's `PortfolioStockOperationsService.java` and scroll through
`sellStockWithSettlement()`:

- Count the lines (~70)
- Show the inline eligibility check with `!lot.isReserved()` — explain that this is
  correct, but the domain method doesn't have it
- Show `SellResult.withFee()` at the end — explain that the service gets the right
  answer, but the domain's `Portfolio.sellWithSettlement()` doesn't

#### Step 6 — Show the Drift Timeline (3 min)

Open `doc/diagrams/rule-drift.puml` and render it. Walk through Sprint 10 → 12 → 14
and explain how each sprint introduced a new inconsistency.

#### Step 7 — Key Takeaway (2 min)

> "The anemic model's service is correct. But when a second code path was added that
> delegated to the domain, the domain's stale rules produced wrong answers. The tests
> caught this — but in a real codebase, the second path might ship without tests.
> The rich model makes this class of bug impossible."

### Discussion Questions

1. "How would you add a new rule — 'lots under regulatory hold cannot be sold' — to each model?"
2. "What happens when a third developer adds a reporting endpoint that calls `Lot.isAvailableForSale()`?"
3. "Could you refactor the anemic model to be safe without making it rich? What would that look like?"

---

## Appendix A — Test Matrix

### Domain Tests (`SettlementAwareSellTest` — 19 tests)

| # | Nested Class | Test Name | Rich | Anemic | Flaw |
|---|-------------|-----------|------|--------|------|
| 1 | SettlementGateTests | `shouldNotSellUnsettledLots` | ✅ | ✅ | — |
| 2 | SettlementGateTests | `shouldSellExactlySettledQuantity` | ✅ | ✅ | — |
| 3 | SettlementGateTests | `shouldReportEligibleSharesCorrectly` | ✅ | ✅ | — |
| 4 | ReservedLotTests | `shouldSkipReservedLots` | ✅ | ❌ | #1 |
| 5 | ReservedLotTests | `shouldSellFromNonReservedLots` | ✅ | ❌ | #1 |
| 6 | ReservedLotTests | `shouldAllowUnreservingLot` | ✅ | ❌ | #1 |
| 7 | FeeTests | `shouldDeductFeeFromProceeds` | ✅ | ❌ | #2 |
| 8 | FeeTests | `shouldMaintainAccountingIdentity` | ✅ | ❌ | #2 |
| 9 | FeeTests | `shouldRejectSellWhenFeeExceedsAvailableCash` | ✅ | ✅ | — |
| 10 | FeeTests | `shouldUpdateCashBalanceWithNetProceeds` | ✅ | ✅ | — |
| 11 | FifoTests | `shouldConsumeSettledLotsInFifoOrder` | ✅ | ✅ | — |
| 12 | FifoTests | `shouldSkipUnsettledLotInFifoOrder` | ✅ | ✅ | — |
| 13 | PartialLotTests | `shouldPartiallyConsumeLot` | ✅ | ✅ | — |
| 14 | EligibilityTests | `shouldDistinguishTotalFromEligible` | ✅ | ❌ | #1 |
| 15 | EligibilityTests | `shouldRejectZeroQuantitySell` | ✅ | ✅ | — |
| 16 | EligibilityTests | `shouldRejectSellForNonExistentHolding` | ✅ | ✅ | — |
| 17 | LotSettlementTests | `lotShouldReportSettlementStatus` | ✅ | ✅ | — |
| 18 | LotSettlementTests | `reservedLotShouldNotBeAvailable` | ✅ | ❌ | #1 |
| 19 | AtomicConsistencyTests | `fullScenarioAtomicCorrectness` | ✅ | ❌ | #1+#2 |

### Integration Tests (`SettlementSellIntegrationTest`)

| # | Nested Class | Test Name | Rich | Anemic | Note |
|---|-------------|-----------|------|--------|------|
| 1 | SettlementGateRest | `sellUnsettledLots_returns409` | ✅ | ✅ | — |
| 2 | SettlementGateRest | `regularSellStillWorks` | ✅ | ✅ | — |
| 3 | FeeInResponse | `regularSaleIncludesZeroFee` | ✅ | ✅ | — |
| 4 | PersistenceRoundTrip | `holdingsSurviveRoundTrip` | ✅ | ✅ | — |
| 5 | PersistenceRoundTrip | `transactionRecordsExistAfterRegularSale` | ✅ | ✅ | — |
| 6 | RuleInconsistency | `eligibleSharesShouldExcludeReservedLots` | N/A | ❌ | Flaw #1 |
| 7 | FeeAccountingDrift | `aggregateSellShouldMaintainAccountingIdentity` | N/A | ❌ | Flaw #2 |

> Tests #6 and #7 exist only on the anemic branch because the endpoints they test
> (eligible-shares, aggregate-settlement-sales) do not exist on the rich branch.

---

## Appendix B — PlantUML Diagram Index

Render these diagrams using PlantUML (IntelliJ plugin, VS Code extension, or CLI):

```bash
# Render all diagrams to PNG
./scripts/render-diagrams.sh
```

| Diagram | File | Description |
|---------|------|-------------|
| Rich Architecture | `doc/diagrams/rich-architecture.puml` | Thin service → single aggregate call |
| Anemic Architecture | `doc/diagrams/anemic-architecture.puml` | Fat service → inline logic + dual paths |
| Rich Sell Sequence | `doc/diagrams/rich-sell-sequence.puml` | Controller → Service → Portfolio → Holding → Lot |
| Anemic Sell Sequence | `doc/diagrams/anemic-sell-sequence.puml` | Service reads Lot data directly, dual paths |
| Invariant Enforcement | `doc/diagrams/invariant-enforcement.puml` | Side-by-side: correct vs. flawed invariants |
| Domain Model | `doc/diagrams/domain-model.puml` | Class diagram of Portfolio aggregate |
| Rule Drift Timeline | `doc/diagrams/rule-drift.puml` | Sprint 10 → 12 → 14 drift evolution |

---

## Appendix C — Gherkin Specification Index

| Feature File | Focus |
|-------------|-------|
| `doc/features/settlement-aware-selling.feature` | Settlement gate behaviour (domain + REST) |
| `doc/features/reserved-lot-handling.feature` | Lot reservation and its effect on eligibility |
| `doc/features/settlement-fees.feature` | Fee calculations and accounting identity |
| `doc/features/fifo-settlement-selling.feature` | FIFO order with settlement gating |
| `doc/features/rule-consistency.feature` | Drift detection and cross-path inconsistency |

Scenarios tagged `@anemic-branch-only @expected-failure` document behaviors that are
intentionally broken on the anemic branch to demonstrate architectural risk.

---

*This tutorial was generated from the HexaStock codebase on branches `rich-domain-model`
and `anemic-domain-model`. All code excerpts, test names, and failure counts are derived
from the actual source files.*
