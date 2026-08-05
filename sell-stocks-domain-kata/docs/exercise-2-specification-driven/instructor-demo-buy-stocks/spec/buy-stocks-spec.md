# US-08 — Buy Stocks (domain specification)

> **Scope.** This specification defines the **domain** behaviour of buying stocks, extending the
> same `Portfolio` aggregate that [`sell-stocks-spec.md`](../../spec/sell-stocks-spec.md)
> describes. Failure outcomes are domain exceptions, named throughout the acceptance criteria
> below.
>
> **Provenance.** This file is a model answer for Exercise 2, Part Two, written the way a
> diligent student would after "interviewing" the product owner (see the stakeholder decisions
> in `exercise-2-self-paced-assignment.md`). It exists so instructors can demonstrate the full
> spec → tests → implementation loop, not as an official addition to the student-facing spec
> folder.

This file describes **what buying does**. It relies on the same structure as the sell spec —
`Portfolio`, `Holding`, `Lot`, and the value objects — extended by exactly the members drawn in
[`domain-class-diagram-with-buy.puml`](domain-class-diagram-with-buy.puml).

---

## 1. User story

**As an** investor
**I want to** deposit cash and buy shares of a specific stock by providing the ticker, quantity,
and price
**So that** I can build a position to sell later, using money I actually have

---

## 2. Preconditions

1. The portfolio must exist.
2. **To deposit:** the amount must be positive (> 0).
3. **To buy:** the ticker must be valid: 1–5 uppercase letters, matching `^[A-Z]{1,5}$`.
4. **To buy:** the quantity must be positive (> 0).
5. **To buy:** the price must be positive (> 0) — from the value-object rules in
   `domain-model.md`.
6. **To buy:** the portfolio's cash balance must be greater than or equal to `quantity × price`.

---

## 3. The buying rule

Buying never merges into an existing lot, even when the ticker and the price both match an
existing lot exactly. Every successful purchase is its own event, its own row in history:

1. Compute the cost: `cost = quantity × price`.
2. Check the cost against the portfolio's cash balance. If `cost` exceeds the balance, reject the
   purchase — nothing below happens.
3. Reduce the cash balance by `cost`.
4. If the portfolio does not yet hold this ticker, create a new, empty holding for it.
5. Append a **new lot** — this quantity, this price, now — to the **end** of the holding's lot
   list.
6. The new lot never merges with an existing lot, and existing lots are never reordered. This is
   what keeps the list in strict purchase order, which is what makes FIFO possible when the
   holding is later sold.

Depositing is a separate, simpler operation: it only ever increases the cash balance, and only
when the amount is positive.

---

## 4. Financial definitions

| Term | Formula | Description |
| ---- | ------- | ----------- |
| **Cost** | `quantity × price` | What a purchase deducts from the cash balance |
| **Balance after deposit** | `balance + amount` | The cash balance after a successful deposit |
| **Balance after purchase** | `balance − cost` | The cash balance after a successful purchase |

All monetary values are `BigDecimal`, scale 2, `HALF_UP`. Never `double` or `float` — the same
rule as the sell specification, because it is the same `Money` value object.

---

## 5. Acceptance criteria

### 5.1 Shared starting context

Unless a criterion says otherwise, the starting context is a portfolio for owner "Alice" that
holds nothing and has a cash balance of **0.00**. This is referred to below as **the empty
portfolio**.

Criteria that need money already deposited say so explicitly and build on the immediately
preceding step (for example, "given the empty portfolio, after depositing 1000.00 …").

> Acceptance-criterion identifiers in this file (AC-01 … AC-12) are scoped to
> `buy-stocks-spec.md` — they are independent of, and do not continue, the AC-01…AC-24 numbering
> in `sell-stocks-spec.md`.

### 5.2 Worked example

*Depositing, then buying twice.* Start from the empty portfolio. Deposit 1000.00 → balance
1000.00. Buy 5 shares of AAPL at 100.00 → `cost = 5 × 100.00 = 500.00`; balance
`1000.00 − 500.00 = 500.00`; a new holding for AAPL is created with one lot, 5 shares at 100.00.
Buy 3 more shares of AAPL at 110.00 → `cost = 3 × 110.00 = 330.00`; balance
`500.00 − 330.00 = 170.00`; the AAPL holding now has **two** lots, in order: 5 @ 100.00, then
3 @ 110.00 — the same shape as the sell specification's baseline holding, which is exactly the
point: it was built by two purchases just like these.

### 5.3 Criteria

| ID | Title | Starting context | Action | Expected result |
| -- | ----- | ----------------- | ------ | ---------------- |
| **AC-01** | A deposit increases the cash balance | The empty portfolio | Deposit 1000.00 | Cash balance is exactly 1000.00. Nothing else changes. |
| **AC-02** | Rejected — non-positive deposit | The empty portfolio | Deposit 0.00, then separately deposit −50.00 | Each attempt is rejected with the invalid-amount exception (`InvalidAmountException`), whose message states that the amount must be positive. The cash balance stays 0.00 after both. |
| **AC-03** | First purchase of a ticker creates a new holding | The empty portfolio, after depositing 1000.00 | Buy 5 shares of AAPL at 100.00 | Balance becomes 500.00. A new holding for AAPL exists with exactly one lot: 5 shares at 100.00. |
| **AC-04** | Purchase deducts the exact cost from the balance | The empty portfolio, after depositing 1000.00 | Buy 5 shares of AAPL at 100.00 (cost 500.00) | Balance is exactly `1000.00 − 500.00 = 500.00`, not an approximation. |
| **AC-05** | A second purchase appends a second lot, in order | Balance 500.00, one lot of 5 @ 100.00 (per AC-03) | Buy 3 more shares of AAPL at 110.00 | Balance becomes 170.00. The AAPL holding now has two lots, in this order: 5 @ 100.00, then 3 @ 110.00. |
| **AC-06** | Buying never merges lots, even at an identical price | Balance 500.00, one lot of 5 @ 100.00 | Buy 5 more shares of AAPL at 100.00 (same price) | The holding has **two** separate lots of 5 @ 100.00 each — not one merged lot of 10 @ 100.00. Lot identity and purchase order are preserved. |
| **AC-07** | Rejected — insufficient funds | Balance 500.00, one lot of 5 @ 100.00 | Attempt to buy 4 shares of AAPL at 130.00 (cost 520.00) | Rejected with the insufficient-funds exception (`InsufficientFundsException`), whose message reports the available and required amounts ("Available: 500.00, Required: 520.00"). Balance stays 500.00; no lot is added; the existing lot is untouched. |
| **AC-08** | Rejected — non-positive quantity | The empty portfolio, after depositing 1000.00 | Attempt to buy 0 shares of AAPL at 100.00, then −3 shares | Each attempt is rejected with the invalid-quantity exception (`InvalidQuantityException`). Balance stays 1000.00; no holding is created. |
| **AC-09** | Rejected — malformed ticker | The empty portfolio, after depositing 1000.00 | Attempt to buy shares of `"aapl"`, `"TOOLONG"`, or `""` | Each attempt is rejected with the invalid-ticker exception (`InvalidTickerException`). Balance stays 1000.00; no holding is created for any of them. |
| **AC-10** | Rejected — non-positive price | The empty portfolio, after depositing 1000.00 | Attempt to buy 5 shares of AAPL at 0.00, then at −10.00 | Each attempt is rejected with the invalid-amount exception (`InvalidAmountException`). Balance stays 1000.00. |
| **AC-11** | A rejected purchase leaves no partial state behind | The empty portfolio, after depositing 1000.00 (no AAPL holding exists) | Attempt to buy 0 shares of AAPL at 100.00 (rejected per AC-08) | No holding is created for AAPL at all — not an empty one, not a partial one. A later sale of AAPL would still be a holding-not-found failure, exactly like `sell-stocks-spec.md` AC-24 for the equivalent case. |
| **AC-12** | Buying again after a position was fully sold starts a fresh holding | A portfolio whose AAPL position was fully sold (see `sell-stocks-spec.md` AC-22), cash balance 2250.00 | Buy 4 shares of AAPL at 130.00 | A new holding is created — not the emptied one revived. It has exactly one lot: 4 shares at 130.00, with no leftover lots from the previous position. (This criterion is shared verbatim with `sell-stocks-spec.md` AC-23; it is repeated here because it is as much a buying-side guarantee as a selling-side one.) |
