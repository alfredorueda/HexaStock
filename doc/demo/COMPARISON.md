# Rich vs Anemic Domain Model — Side-by-Side Comparison

## Feature: Settlement-Aware FIFO Selling with Reserved Lots and Fees

Both branches implement identical external behavior: a `POST /{id}/settlement-sales` endpoint
that sells shares respecting T+2 settlement, lot reservation, FIFO ordering, and 0.1% fee
deduction. The difference is **where** the business logic lives.

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
│  Domain objects are DATA BAGS. Service contains ALL logic.          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Lines of Code

| File | Rich Model | Anemic Model | Delta |
|------|-----------|-------------|-------|
| `Lot.java` | 190 | 144 | -46 (no behavioral methods) |
| `Holding.java` | 200 | 135 | -65 (no settlement queries) |
| `Portfolio.java` | 376 | 280 | -96 (no sell orchestration) |
| **Domain total** | **766** | **559** | **-207** |
| `PortfolioStockOperationsService.java` | 178 | 247 | **+69** (all logic moved here) |
| **Net change** | **944** | **806** | **-138** |

The anemic model has fewer total lines, but the **distribution** is the problem: domain objects
are hollow data structures while the service is bloated with business logic.

---

## Code Comparison: Selling with Settlement

### Rich Model — `Portfolio.sellWithSettlement()`

```java
// Portfolio.java (rich)
public SellResult sellWithSettlement(Ticker ticker, ShareQuantity qty,
                                      Price currentPrice, Money currentCash,
                                      LocalDateTime asOf) {
    Holding h = findHolding(ticker);
    ShareQuantity eligible = h.getEligibleShares(asOf);  // domain query
    if (eligible.compareTo(qty) < 0)
        throw new InsufficientEligibleSharesException(ticker, qty, eligible);

    SellResult result = h.sellSettled(qty, currentPrice, asOf);  // domain op
    Money fee = result.proceeds().multiply(FEE_RATE);
    SellResult withFee = SellResult.withFee(result.proceeds(), result.costBasis(),
                                             result.profit(), fee);
    Money netProceeds = withFee.netProceeds();
    if (currentCash.compareTo(fee) < 0)
        throw new InsufficientCashForFeeException(fee, currentCash);

    deposit(netProceeds);
    transactions.add(Transaction.createSaleWithFee(...));
    return withFee;
}
```

### Anemic Model — `PortfolioStockOperationsService.sellStockWithSettlement()`

```java
// PortfolioStockOperationsService.java (anemic)
public SaleResponseDTO sellStockWithSettlement(PortfolioId id, Ticker ticker,
                                                ShareQuantity qty, LocalDateTime asOf) {
    Portfolio portfolio = loadPortfolio(id);
    Price price = stockPricePort.getStockPrice(ticker).price();
    Holding holding = findHolding(portfolio, ticker);

    // Eligibility check — logic that should be in domain
    int eligible = 0;
    for (Lot lot : holding.getLots()) {
        if (!asOf.isBefore(lot.getSettlementDate()) && !lot.isReserved()) {
            eligible += lot.getRemainingShares().value();
        }
    }
    if (eligible < qty.value())
        throw new InsufficientEligibleSharesException(ticker, qty,
                                                       new ShareQuantity(eligible));

    // FIFO sell — logic that should be in domain
    int remaining = qty.value();
    Money costBasis = Money.ZERO;
    for (Lot lot : holding.getLots()) {
        if (remaining <= 0) break;
        if (asOf.isBefore(lot.getSettlementDate()) || lot.isReserved()) continue;
        int sellable = Math.min(lot.getRemainingShares().value(), remaining);
        costBasis = costBasis.add(lot.getUnitPrice().toMoney().multiply(
                        new BigDecimal(sellable)));
        lot.reduce(new ShareQuantity(sellable));
        remaining -= sellable;
    }

    // Fee calculation — logic that should be in domain
    Money proceeds = price.toMoney().multiply(new BigDecimal(qty.value()));
    Money fee = proceeds.multiply(FEE_RATE);
    Money netProceeds = proceeds.subtract(fee);

    if (portfolio.getBalance().compareTo(fee) < 0)
        throw new InsufficientCashForFeeException(fee, portfolio.getBalance());

    portfolio.deposit(netProceeds);
    // ... create transaction, persist, return DTO
}
```

---

## Method-Level Comparison

### `Lot` — Settlement & Reservation

| Capability | Rich Model | Anemic Model |
|-----------|-----------|-------------|
| Is lot settled? | `lot.isSettled(asOf)` | `!asOf.isBefore(lot.getSettlementDate())` — inline in service |
| Is lot available? | `lot.isAvailableForSale(asOf)` | `!asOf.isBefore(lot.getSettlementDate()) && !lot.isReserved()` — inline in service |
| Available shares | `lot.availableShares(asOf)` | `lot.isAvailableForSale(asOf) ? lot.getRemainingShares() : 0` — inline in service |
| Reserve lot | `lot.reserve()` — with guard | `lot.setReserved(true)` — no guard |
| Unreserve lot | `lot.unreserve()` | `lot.setReserved(false)` |

### `Holding` — Eligibility Queries

| Capability | Rich Model | Anemic Model |
|-----------|-----------|-------------|
| Eligible shares | `holding.getEligibleShares(asOf)` | Manual loop in service iterating lots |
| Settled FIFO sell | `holding.sellSettled(qty, price, asOf)` | Manual loop + reduce in service |
| Find lot by ID | `holding.findLotById(lotId)` | Manual stream filter in service |

### `Portfolio` — Aggregate Operations

| Capability | Rich Model | Anemic Model |
|-----------|-----------|-------------|
| Sell with settlement | `portfolio.sellWithSettlement(...)` | `service.sellStockWithSettlement(...)` |
| Reserve lot | `portfolio.reserveLot(ticker, lotId)` | `service` does `lot.setReserved(true)` |
| Unreserve lot | `portfolio.unreserveLot(ticker, lotId)` | `service` does `lot.setReserved(false)` |

---

## Invariant Protection

| Invariant | Rich Model | Anemic Model |
|-----------|-----------|-------------|
| Cannot sell unsettled lots | Enforced in `Holding.sellSettled()` | Checked in service — can be bypassed |
| Cannot sell reserved lots | Enforced in `Lot.isAvailableForSale()` | Checked in service — can be bypassed |
| Cannot double-reserve a lot | `lot.reserve()` throws if already reserved | `lot.setReserved(true)` — silently overwrites |
| Fee must not exceed cash | Checked in `Portfolio.sellWithSettlement()` | Checked in service |
| FIFO ordering | Enforced in `Holding.sellSettled()` | Manually coded in service loop |

**Key insight**: In the anemic model, any code with access to the domain objects can bypass
invariants by calling setters directly. The rich model makes invalid states unrepresentable
by not exposing setters.

---

## Testability Impact

| Aspect | Rich Model | Anemic Model |
|--------|-----------|-------------|
| Domain unit tests | ✅ 19 tests, pure, fast, no mocks | ❌ 33 compilation errors |
| Test settlement logic | `lot.isSettled(asOf)` — direct | Must test through service (needs mocks) |
| Test FIFO ordering | `holding.sellSettled(...)` — direct | Must test through service (needs DB/mocks) |
| Test fee calculation | `portfolio.sellWithSettlement(...)` — direct | Must test through service |
| Test invariant violations | Direct exception assertions | Need full service + repo setup |
| Integration tests | ✅ 5 tests pass | ✅ 5 tests pass (same behavior) |
| Test execution time | Milliseconds (no I/O) | Seconds (needs Spring context or mocks) |

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
