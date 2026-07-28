# US-07 — Sell Stocks (domain specification)

> **Scope.** Domain only. The original spec describes an HTTP API; failure
> outcomes are restated here as **domain exceptions** instead of HTTP status codes.

This file describes **what selling does**. Two companion files describe the rest:

* [`domain-model.md`](domain-model.md) — the classes, fields and value objects this behaviour
  relies on (also drawn in [`domain-class-diagram.puml`](domain-class-diagram.puml)).
* [`error-contract.md`](error-contract.md) — what each failure raises, and its message.

---

## 1. User story

**As an** investor with existing stock holdings
**I want to** sell shares of a specific stock by providing the ticker symbol and quantity
**So that** I can realize profits, cut losses, or rebalance my portfolio

---

## 2. Preconditions

1. The portfolio must exist.
2. The ticker must be valid: 1–5 uppercase letters. *(format validation itself is an open
   question — see section 6)*
3. The quantity must be positive (> 0).
4. The portfolio must hold the specified ticker.
5. The portfolio must hold at least the requested quantity of shares for that ticker.
6. The sale price must be positive (> 0) — from the value-object rules in
   [`domain-model.md`](domain-model.md).

---

## 3. FIFO accounting rule

Sales apply **First-In, First-Out (FIFO)** lot consumption. When shares are sold, the system
iterates through the holding's lots in chronological order (oldest first) and consumes
shares from each lot until the requested quantity is fulfilled:

1. Start with the **oldest lot** (the one purchased earliest).
2. Take the **minimum** of the lot's remaining shares and the shares still to sell.
3. Reduce the lot's remaining shares by that amount.
4. Accumulate the **cost basis** contribution: shares taken from that lot × that lot's unit
   purchase price.
5. If the lot reaches **zero remaining shares**, it becomes empty and is removed.
6. Move to the **next oldest lot** and repeat until all requested shares are sold.

---

## 4. Financial definitions

| Term           | Formula                                             | Description                                                                                   |
| -------------- | --------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| **Sale Price** | *(market price at sell time)*                       | The current market price obtained when executing the sell order                               |
| **Proceeds**   | `quantitySold × salePrice`                          | Total revenue from the sale                                                                   |
| **Cost Basis** | `Σ (sharesFromLotᵢ × purchasePriceᵢ)` applying FIFO | The original acquisition cost of the sold shares, computed by summing each lot's contribution |
| **Profit**     | `proceeds − costBasis`                              | Realized gain (positive) or loss (negative) from the sale                                     |

All monetary values are `BigDecimal`, scale 2, `HALF_UP`. Never `double` or `float`.

---

## 5. Acceptance criteria

### 5.1 Shared starting context (used unless a criterion says otherwise)

A portfolio exists for owner "Alice", with a cash balance of 0.00. It holds AAPL with the
following lots, in purchase order:

| Lot # | Shares | Purchase price |
| ----- | ------ | -------------- |
| 1     | 10     | 100.00         |
| 2     | 5      | 120.00         |

Total position: **15 shares**. The current market price for AAPL is **150.00**.

This starting context is referred to below as **the baseline holding**.

### 5.2 Worked FIFO examples

| Sell qty | Sale price | Lots consumed (FIFO)              | Proceeds | Cost basis | Profit  | Lots remaining afterwards |
| -------- | ---------- | --------------------------------- | -------- | ---------- | ------- | ------------------------- |
| 1        | 150.00     | 1 from Lot #1 @ 100.00            | 150.00   | 100.00     | 50.00   | 9 @ 100.00, 5 @ 120.00    |
| 8        | 150.00     | 8 from Lot #1 @ 100.00            | 1200.00  | 800.00     | 400.00  | 2 @ 100.00, 5 @ 120.00    |
| 10       | 150.00     | 10 from Lot #1 @ 100.00           | 1500.00  | 1000.00    | 500.00  | 5 @ 120.00 (Lot #1 removed) |
| 12       | 150.00     | 10 @ 100.00, then 2 @ 120.00      | 1800.00  | 1240.00    | 560.00  | 3 @ 120.00 (Lot #1 removed) |
| 15       | 150.00     | 10 @ 100.00, then 5 @ 120.00      | 2250.00  | 1600.00    | 650.00  | none — 0 shares held      |
| 8        | 90.00      | 8 from Lot #1 @ 100.00            | 720.00   | 800.00     | −80.00  | 2 @ 100.00, 5 @ 120.00    |

Calculation breakdown for the two scenarios the original spec spelled out:

*Selling 8 shares.* Lot #1 has 10 remaining, so take `min(10, 8) = 8` shares; cost-basis
contribution `8 × 100.00 = 800.00`; Lot #1 remaining `10 − 8 = 2`. The request is fulfilled.
`proceeds = 8 × 150.00 = 1200.00`; `costBasis = 800.00`; `profit = 1200.00 − 800.00 = 400.00`.

*Selling 12 shares.* Lot #1 has 10 remaining, so take `min(10, 12) = 10`; contribution
`10 × 100.00 = 1000.00`; Lot #1 remaining `10 − 10 = 0`, so the lot is empty and is removed;
still to sell `12 − 10 = 2`. Lot #2 has 5 remaining, so take `min(5, 2) = 2`; contribution
`2 × 120.00 = 240.00`; Lot #2 remaining `5 − 2 = 3`; still to sell `0`. The request is
fulfilled. `proceeds = 12 × 150.00 = 1800.00`;
`costBasis = 1000.00 + 240.00 = 1240.00`; `profit = 1800.00 − 1240.00 = 560.00`.

### 5.3 Criteria

| ID | Title | Starting context / preconditions | Action | Expected result |
| -- | ----- | -------------------------------- | ------ | --------------- |
| **AC-01** | Sale consumed entirely from a single lot | The baseline holding | Sell 8 shares of AAPL at 150.00 | The sale reports ticker AAPL, quantity 8, proceeds 1200.00, cost basis 800.00 and profit 400.00. FIFO took all 8 shares from Lot #1 at 100.00. The lots are now Lot #1 with 2 of its 10 initial shares remaining at 100.00, and Lot #2 untouched with 5 of 5 at 120.00. |
| **AC-02** | Sale consumed across multiple lots, emptied lot removed | The baseline holding | Sell 12 shares of AAPL at 150.00 | The sale reports quantity 12, proceeds 1800.00, cost basis 1240.00 and profit 560.00. FIFO took 10 shares from Lot #1 at 100.00 and then 2 shares from Lot #2 at 120.00. Lot #1 is fully depleted and removed from the holding. One lot remains: Lot #2 with 3 of its 5 initial shares at 120.00. |
| **AC-03** | Smallest possible sale | The baseline holding | Sell 1 share of AAPL at 150.00 | Proceeds 150.00, cost basis 100.00, profit 50.00. Both lots remain: 9 @ 100.00 and 5 @ 120.00. |
| **AC-04** | Boundary — the sale exactly exhausts the oldest lot | The baseline holding | Sell 10 shares of AAPL at 150.00 | Proceeds 1500.00, cost basis 1000.00, profit 500.00. Lot #1 is emptied and removed; exactly one lot remains, 5 @ 120.00. No shares are taken from Lot #2. |
| **AC-05** | Boundary — the sale liquidates the entire position | The baseline holding | Sell all 15 shares of AAPL at 150.00 | Proceeds 2250.00, cost basis 1600.00, profit 650.00. The holding's total shares become 0. *(What happens to the holding object itself is an open question — see section 6. This criterion asserts the amounts and the zero share count only.)* |
| **AC-06** | Loss — sale price below the purchase price | The baseline holding | Sell 8 shares of AAPL at 90.00 | Proceeds 720.00, cost basis 800.00, profit −80.00. A negative profit is a valid, expected outcome; the sale succeeds. |
| **AC-07** | FIFO consumes the oldest lot first | The baseline holding | Sell 8 shares of AAPL at 150.00 | The shares are taken from the oldest lot (purchased earliest) before any newer lot is touched: the cost basis is 800.00, computed at 100.00 per share, not at 120.00 and not at a blended average. The newer lot still has all 5 of its shares. |
| **AC-08** | Proceeds are credited to the portfolio's cash balance | The baseline holding, cash balance 0.00 | Sell 8 shares of AAPL at 150.00 | The cash balance increases by the proceeds, from 0.00 to 1200.00. |
| **AC-09** | Profit is always proceeds minus cost basis | The baseline holding | Sell any valid quantity | The reported profit equals the reported proceeds minus the reported cost basis. |
| **AC-10** | Rejected — quantity of zero | The baseline holding | Sell 0 shares of AAPL | The sale is rejected with the invalid-quantity exception (`InvalidQuantityException`), whose message states that the quantity must be positive. Nothing is sold. |
| **AC-11** | Rejected — negative quantity | The baseline holding | Sell a negative number of shares (e.g. −5) of AAPL | The sale is rejected with the invalid-quantity exception (`InvalidQuantityException`). A negative share quantity cannot exist: it is refused when the quantity value is created, before any sale is attempted. |
| **AC-12** | Rejected — selling more shares than are held | The baseline holding (15 shares) | Sell 16 shares of AAPL at 150.00 | The sale is rejected with the conflict/insufficient-shares exception (`ConflictQuantityException`), whose message reports the available and requested amounts ("Not enough shares to sell. Available: 15, Requested: 16"). |
| **AC-13** | Rejected — the portfolio does not hold that ticker | The baseline holding (AAPL only; MSFT is not held) | Sell 5 shares of MSFT at 150.00 | The sale is rejected with the holding-not-found exception (`HoldingNotFoundException`). |
| **AC-14** | Rejected — non-positive sale price | The baseline holding | Attempt a sale at a price of 0.00 (or a negative price) | The price is refused with the invalid-amount exception (`InvalidAmountException`), whose message states that the price must be positive. |
| **AC-15** | Validation order — quantity is checked before the holding is looked up | The baseline holding (MSFT is not held) | Sell 0 shares of MSFT | The invalid-quantity exception (`InvalidQuantityException`) is raised, not the holding-not-found exception. The quantity precondition is evaluated first. |
| **AC-16** | A rejected sale leaves the holding untouched | The baseline holding, cash balance 0.00 | Attempt to sell 16 shares of AAPL at 150.00 (rejected per AC-12) | Nothing is mutated: the holding still has 15 shares in two lots, 10 @ 100.00 and 5 @ 120.00, and the cash balance is still 0.00. There is no partial consumption of the oldest lot. |
| **AC-17** | A rejected sale of an unheld ticker leaves the portfolio untouched | The baseline holding, cash balance 0.00 | Attempt to sell 5 shares of MSFT (rejected per AC-13) | The cash balance is still 0.00 and the AAPL holding is unchanged, with 15 shares in two lots. |
| **AC-18** | A lot cannot be reduced below zero | A single lot of 10 shares at 100.00 | Reduce the lot by 11 shares | The reduction is rejected with the conflict/insufficient-shares exception (`ConflictQuantityException`), and the lot still has 10 remaining shares. |
| **AC-19** | A lot cannot be created with a non-positive share count | — | Create a lot with 0 shares (or a negative number) at 100.00 | Creation is rejected with the invalid-quantity exception (`InvalidQuantityException`). |

---

## 6. Open questions (not yet specified)

These two points are gaps in the contract itself, carried over from section 9 of the original
spec. They are **not** acceptance criteria, they are **not** implemented, and they have **no**
tests. They are marked `// TODO (open spec question)` in the code.

### 6.1 Invalid ticker format is not covered by any acceptance criterion

Precondition 2 states that a ticker must be 1–5 uppercase letters, yet none of the original
acceptance criteria exercise a violation of it — not `"aapl"`, not `"TOOLONG"`, not `"123"`,
not `""`. By contrast the quantity preconditions each have an explicit criterion with a
defined outcome. The equivalent for ticker format is simply missing, so the failure mode is
undefined: which exception, and validated where — at the request boundary or inside
`Portfolio.sell()`, before or after the portfolio is located?

### 6.2 Holding lifecycle when fully liquidated is ambiguous

The FIFO rule says an emptied **lot** is removed. It says nothing about the **holding** once
its last lot is gone. Two designs are equally consistent with the stated preconditions:

* **(a)** The holding is removed from the portfolio once it has zero lots — so a subsequent
  sell of the same ticker violates precondition 4 and raises `HoldingNotFoundException`, even
  though the portfolio legitimately held that stock moments earlier.
* **(b)** The holding remains with an empty lot list and zero total shares — so a subsequent
  sell instead violates precondition 5 and raises `ConflictQuantityException`.

The choice also affects reporting: after full liquidation, would a holdings listing still show
the ticker with quantity 0, or would it disappear? And would buying that ticker again create a
new holding or reuse the empty one? The spec does not say. AC-05 therefore stops at asserting
the amounts and the zero share count, and says nothing about a *subsequent* sale.
