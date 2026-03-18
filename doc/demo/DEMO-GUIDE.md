# Demo Guide — Rich vs Anemic Domain Model

## Prerequisites

- Java 21+, Maven (or use `./mvnw`), Docker (for Testcontainers/MySQL)
- Two branches: `rich-domain-model` and `anemic-domain-model`
- Both branches share the same external API, tests, persistence layer, and DTOs

---

## Quick Start

```bash
# 1. Verify you're on the rich branch
git checkout rich-domain-model

# 2. Run ALL tests (170 tests — 19 domain + 7 integration + 144 existing)
./mvnw clean verify

# 3. Switch to anemic branch
git checkout anemic-domain-model

# 4. Run ALL tests — observe 10 test failures (8 domain, 2 integration)
./mvnw clean test
```

---

## Demo Script (30-min session)

### Part 1 — The Feature (5 min)

Introduce the feature being implemented on both branches:

> **Settlement-Aware FIFO Selling** — When selling stocks, only lots that have
> completed T+2 settlement can be sold. Lots can be reserved (locked from sale).
> A 0.1% transaction fee is deducted from proceeds. FIFO ordering determines
> which lots are consumed first.

Key rules:
- `Lot.SETTLEMENT_DAYS = 2` — lot must be 2+ days old to sell
- Reserved lots are skipped during sale
- Fee = 0.1% of gross proceeds; deducted before crediting balance
- If fee exceeds available cash, sale is rejected
- `SellResult` carries `proceeds`, `costBasis`, `profit`, `fee`

### Part 2 — Rich Domain Model (10 min)

```bash
git checkout rich-domain-model
```

**Show domain code** — open in order:

1. **`Lot.java`** (190 lines) — highlight behavioral methods:
   - `isSettled(LocalDateTime asOf)` — encapsulates T+2 rule
   - `isAvailableForSale(LocalDateTime asOf)` — settled AND not reserved
   - `availableShares(LocalDateTime asOf)` — returns 0 if unavailable
   - `reserve()` / `unreserve()` — with guard against double-reserve

2. **`Holding.java`** (200 lines) — highlight:
   - `getEligibleShares(LocalDateTime asOf)` — sums available across lots
   - `sellSettled(ShareQuantity, Price, LocalDateTime)` — FIFO with settlement filter
   - `findLotById(LotId)` — aggregate navigation

3. **`Portfolio.java`** (376 lines) — highlight:
   - `sellWithSettlement(Ticker, ShareQuantity, Price, Money, LocalDateTime)` — the orchestrator
   - `reserveLot(Ticker, LotId)` / `unreserveLot(Ticker, LotId)` — aggregate-level ops
   - Note: fee calculation, balance check, transaction creation — all inside the aggregate

4. **`PortfolioStockOperationsService.java`** (178 lines) — highlight how THIN it is:
   - `sellStockWithSettlement()` just loads portfolio, gets price, calls `portfolio.sellWithSettlement()`, persists

**Run domain tests:**

```bash
./mvnw test -pl . -Dtest="SettlementAwareSellTest" -Dsurefire.useFile=false
```

Point out: 19 tests, pure Java, no Spring context, no mocks, no database. Runs in milliseconds.

**Run full suite:**

```bash
./mvnw clean verify
```

168 tests, 0 failures.

### Part 3 — Anemic Domain Model (10 min)

```bash
git checkout anemic-domain-model
```

**Show domain code** — same files, different behavior:

1. **`Lot.java`** — `isAvailableForSale()` only checks settlement, forgets `!reserved`:
   ```java
   // BUG: forgets reservation
   public boolean isAvailableForSale(LocalDateTime asOf) {
       return isSettled(asOf);  // Missing: && !reserved
   }
   ```

2. **`Portfolio.java`** — `sellWithSettlement()` computes profit without fee:
   ```java
   // BUG: profit ignores fee
   Money profit = grossProceeds.subtract(costBasis);
   // Should be: grossProceeds.subtract(fee).subtract(costBasis)
   ```

3. **`PortfolioStockOperationsService.java`** — **THIS IS THE KEY**:
   - The inline logic is CORRECT (checks both settlement AND reservation, fee handled properly)
   - But the domain convenience methods diverge from it

**Run domain tests:**

```bash
./mvnw test -Dtest="SettlementAwareSellTest" -Dsurefire.useFile=false
```

19 tests, 8 fail — due to flawed domain methods.

**Run integration tests:**

```bash
./mvnw test -Dtest="SettlementSellIntegrationTest" -Dsurefire.useFile=false
```

7 tests, 2 fail:

- **`eligibleSharesShouldExcludeReservedLots`**: Query endpoint says 15 shares eligible,
  but only 5 are truly available (reserved lot counted). Expected 5, got 15.
- **`aggregateSellShouldMaintainAccountingIdentity`**: Accounting identity broken —
  `costBasis + profit + fee = 1001 ≠ 1000`. Profit overstated by fee amount.

5 existing integration tests still pass because they use the service's correct inline path.

**Run full suite:**

```bash
./mvnw test
```

170 tests, 10 failures (8 domain + 2 integration).

### Part 4 — The Key Insight (5 min)

The same application has **two sell paths**:

| Path | Code | Reservation Check | Fee in Profit |
|------|------|-------------------|---------------|
| Original sell endpoint | Service inline logic | ✅ Correct | ✅ Correct |
| Eligible shares query | `Holding.getEligibleShares()` → `Lot.isAvailableForSale()` | ❌ Missing | N/A |
| Aggregate sell endpoint | `Portfolio.sellWithSettlement()` | ✅ Correct | ❌ Missing |

Two endpoints that should agree on "which lots are sellable" give different answers.
Two endpoints that should agree on "how much profit was made" give different numbers.

**This is the danger of an anemic model**: not that it crashes, but that it produces
inconsistent results across flows while each flow individually looks correct.

### Part 5 — Discussion (5 min)

Show the comparison table:

| Aspect | Rich Model | Anemic Model |
|--------|-----------|-------------|
| Domain tests | 19/19 pass | 11/19 pass, **8 fail** |
| Integration tests | 7/7 pass | 5/7 pass, **2 fail** |
| Full suite | 170/170 pass | 160/170 pass, **10 fail** |
| Invariant enforcement | Domain objects | Service (bypassable) |
| Rule consistency | Single source of truth | Duplicated, drifts silently |

Key discussion points:
- **Silent inconsistency**: Two endpoints that should agree on business state give different answers.
  Not a crash, not a stack trace — just wrong numbers that look plausible.
- **Logic duplication = drift**: Every time a new feature needs the same rule, someone reimplements it.
  Each copy can drift independently. In the rich model, there's only one copy.
- **Feature Envy**: The service's `sellStockWithSettlement()` method operates entirely on another
  object's data — a classic code smell.
- **When anemic is OK**: Simple CRUD, prototypes, or when business rules are genuinely trivial.

---

## Diff Commands

Compare specific files between branches:

```bash
# Compare Lot.java
git diff anemic-domain-model..rich-domain-model -- src/main/java/cat/gencat/agaur/hexastock/model/Lot.java

# Compare service
git diff anemic-domain-model..rich-domain-model -- src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java

# Compare Portfolio.java
git diff anemic-domain-model..rich-domain-model -- src/main/java/cat/gencat/agaur/hexastock/model/Portfolio.java

# Full diff between branches
git diff anemic-domain-model..rich-domain-model --stat
```

---

## Related Documentation

- [COMPARISON.md](COMPARISON.md) — Detailed side-by-side code comparison
- [FAILURES.md](FAILURES.md) — Full compilation failure analysis with line-by-line breakdown
- [DDD Portfolio and Transactions](../DDD%20Portfolio%20and%20Transactions.md) — Original DDD design
