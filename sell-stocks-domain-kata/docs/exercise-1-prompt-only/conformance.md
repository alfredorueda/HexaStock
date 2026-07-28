# Conformance checklist — open in Phase 2

> Stop. If you are still in Phase 1, close this file.

Run each check against your own implementation and record what actually happens — not what you
believe should happen. Where your code cannot answer the question at all, that is itself an
answer: write "cannot tell".

Throughout, **Alice's portfolio** is the one from the brief: 10 shares of AAPL bought at 100.00,
then 5 more at 120.00. Fifteen shares, cash balance 0.00. Market price 150.00 unless stated.

## Part A — The money

| # | Check | Required result | Yours |
| - | ----- | ---------------- | ----- |
| A1 | Sell 12 shares at 150.00 | proceeds 1800.00, cost basis **1240.00**, profit **560.00** | |
| A2 | Sell 8 shares at 150.00 | proceeds 1200.00, cost basis **800.00**, profit **400.00** | |
| A3 | Sell 1 share at 150.00 | proceeds 150.00, cost basis 100.00, profit 50.00 | |
| A4 | Sell 10 shares at 150.00 | proceeds 1500.00, cost basis 1000.00, profit 500.00 | |
| A5 | Sell all 15 shares at 150.00 | proceeds 2250.00, cost basis 1600.00, profit 650.00 | |
| A6 | Sell 8 shares at 90.00 | proceeds 720.00, cost basis 800.00, profit **−80.00** — a loss is a valid outcome, not an error | |
| A7 | After selling 8, what is left | one lot of **2** shares at 100.00 and one of **5** at 120.00 | |
| A8 | After selling 12, what is left | **3** shares at 120.00; the first purchase is gone entirely | |

**A1 is the one that matters.** The required method is **FIFO**: the shares sold are the
oldest ones you still hold. If your cost basis for 12 shares came out as anything else, your
assistant chose a different accounting method — and it chose it without telling you.

| What your code did | Cost basis on 12 | Profit |
| ------------------ | ---------------- | ------ |
| FIFO — oldest shares first | **1240.00** | **560.00** |
| Weighted average cost | 1280.00 | 520.00 |
| LIFO — newest shares first | 1300.00 | 500.00 |

All three are respectable accounting methods. Two of them are the wrong answer here, and the
brief never said which was which.

## Part B — The refusals

| # | Check | Required result | Yours |
| - | ----- | ---------------- | ----- |
| B1 | Sell 0 shares | Refused. Not "a sale of nothing for nothing". | |
| B2 | Sell −5 shares | Refused. Check carefully what yours does — many implementations quietly *increase* the position and *reduce* the cash balance. | |
| B3 | Sell 16 shares of the 15 held | Refused, with an error that says how many were available and how many were asked for | |
| B4 | Sell 5 shares of MSFT, which Alice does not hold | Refused, and distinguishably from B3 — "you don't hold this" is not the same problem as "you don't hold enough" | |
| B5 | Sell at a price of 0.00 or a negative price | Refused | |
| B6 | Ticker `"aapl"`, `"TOOLONG"`, `"123"`, `""` | Refused. A ticker is 1–5 uppercase letters. | |

## Part C — What happens after a refusal

| # | Check | Required result | Yours |
| - | ----- | ---------------- | ----- |
| C1 | After the refused sale of 16 shares, inspect the position | Untouched: 15 shares, still 10 at 100.00 and 5 at 120.00 | |
| C2 | After that same refusal, inspect the cash balance | Untouched: 0.00 | |

If your implementation consumed part of the oldest lot before discovering it could not finish,
it failed halfway through and left the portfolio in a state that never should have existed.

## Part D — The parts nobody mentions

| # | Check | Required result | Yours |
| - | ----- | ---------------- | ----- |
| D1 | What type holds money in your code? | `BigDecimal`. If it is `double` or `float`, you have a defect — see below. | |
| D2 | Sell 8 shares, then print the profit exactly as your code stores it | An exact 400.00 | |
| D3 | Sell all 15 shares, then try to sell 1 more | Refused, and the position no longer exists | |
| D4 | Sell all 15, then buy 4 more at 130.00 | A fresh position of exactly 4 shares — no ghosts of the old one | |

### On D1 and D2

An implementation that uses `double` and weighted-average cost reports the profit on an 8-share
sale as:

```
346.66666666666663
```

That is what an investor sees on their tax statement.

Now look at the test your assistant wrote for that number. It almost certainly reads something
like `assertEquals(346.67, profit, 0.01)` — with a tolerance. The tolerance is there because
floating-point money does not come out exact, and it is the reason the test passed and you never
found out.

## Scoring

Count Part A as one point per row, Parts B, C and D as one point per row. Twenty points in total.

Write your score down. In Phase 3 you will put it next to everyone else's, and the interesting
number is not the average — it is the spread.
