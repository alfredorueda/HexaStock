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

# 2. Run ALL tests (168 tests — 19 domain + 5 integration + 144 existing)
./mvnw clean verify

# 3. Switch to anemic branch
git checkout anemic-domain-model

# 4. Attempt to compile tests — observe 33 compilation errors
./mvnw clean test-compile
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

1. **`Lot.java`** (144 lines) — only data:
   - `getSettlementDate()`, `isReserved()`, `setReserved(boolean)` — getters/setters
   - **No** `isSettled()`, `isAvailableForSale()`, `availableShares()`, `reserve()`
   - Ask audience: *"Where did the settlement logic go?"*

2. **`Holding.java`** (135 lines) — no new methods at all

3. **`Portfolio.java`** (280 lines) — no new methods at all

4. **`PortfolioStockOperationsService.java`** (247 lines) — **THIS IS THE KEY**:
   - Show the `sellStockWithSettlement()` method — all 70+ lines of business logic
   - Manual lot iteration, settlement date comparison, reserved flag check
   - Manual FIFO sell loop with quantity tracking
   - Fee calculation, balance check — all procedural

**Attempt to compile tests:**

```bash
./mvnw clean test-compile 2>&1 | grep "cannot find symbol" | head -15
```

**33 compilation errors.** The SAME test file from the rich branch cannot compile
because it calls methods (`sellWithSettlement`, `isSettled`, `reserve`, etc.)
that don't exist on anemic domain objects.

**Show that integration tests DO work:**

```bash
# Integration tests go through REST -> Service, so they compile and pass
./mvnw clean verify -Dtest="SettlementSellIntegrationTest" \
  -DfailIfNoTests=false -Dmaven.test.failure.ignore=true \
  -Dfailsafe.skip=false
```

### Part 4 — Discussion (5 min)

Show the comparison table:

| Aspect | Rich Model | Anemic Model |
|--------|-----------|-------------|
| Domain LOC | 766 | 559 |
| Service LOC | 178 | 247 |
| Domain tests | 19/19 pass | 33 compile errors |
| Integration tests | 5/5 pass | 5/5 pass |
| Invariant enforcement | Domain objects | Service (bypassable) |
| Double-reserve guard | `reserve()` throws | `setReserved(true)` — silent |
| Test speed | Milliseconds | Seconds |

Key discussion points:
- **Invariant leakage**: The anemic model's `setReserved(true)` can be called from anywhere.
  The rich model's `reserve()` throws on double-reserve.
- **Test pyramid**: Rich model enables fast domain tests at the base. Anemic model pushes
  all testing to integration level.
- **Feature Envy**: The service's `sellStockWithSettlement()` method is a classic code smell —
  it operates entirely on another object's data.
- **When anemic is OK**: Simple CRUD, prototypes, or when the team isn't comfortable with OOP.

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
