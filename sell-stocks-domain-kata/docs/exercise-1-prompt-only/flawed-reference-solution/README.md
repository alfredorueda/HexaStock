# Flawed reference solution — DO NOT COPY

> **This code is wrong on purpose.** It is a teaching artifact: what a prompt-only session
> typically produces from [`../brief.md`](../brief.md). It compiles, it runs, and all of its
> tests pass. It scores **5 out of 20** on [`../conformance.md`](../conformance.md).
>
> The correct implementation is Exercise 2, in
> [`../../exercise-2-specification-driven/`](../../exercise-2-specification-driven/).

```bash
cd docs/exercise-1-prompt-only/flawed-reference-solution
mvn test          # 8 tests, all green
```

## Why it looks fine

Read [`Portfolio.java`](src/main/java/com/example/portfolio/Portfolio.java) before reading any
further. It is short, it is clearly named, it has javadoc, it validates its inputs, and it comes
with eight passing tests covering the happy path, a loss, the cash balance, and two error cases.

At a code review, with the brief in hand, most people would approve it.

## What is actually wrong

Each defect is followed by the line of the brief that failed to prevent it.

### 1. It uses weighted average cost, not FIFO

Selling 12 of Alice's shares reports a cost basis of **1280.00** and a profit of **520.00**.
The correct answers are **1240.00** and **560.00**.

> *"the cost of the shares being sold depends on which shares you consider sold"*

The brief states the problem exactly and never says which method. The assistant picked one,
implemented it well, and never mentioned that there was a choice. Nobody was careless.

### 2. Money is `double`

Selling 8 shares, the profit is reported as:

```
346.66666666666663
```

That is the number an investor would see on a tax statement.

> *(the brief says nothing about money types at all)*

Now look at the test that covers it, in
[`PortfolioTest.java`](src/test/java/com/example/portfolio/PortfolioTest.java):

```java
assertEquals(346.67, result.getProfit(), 0.01);
```

The tolerance is not laziness — with `double` money it is *required*, because the value is never
exact. The defect and the thing that hides it were generated together, in the same breath.

### 3. Selling a negative quantity buys shares

The guard is `quantity > position.getShares()`, which `-5` passes happily. Observed:

```
sell("AAPL", -5, 150.00)
  -> Sold -5 AAPL for -750.0 (cost -533.33, profit -216.67)
  -> shares now 20, cash now -750.0
```

Alice asked to sell. She ended up owning **five more shares** and **750.00 poorer**.

> *(no line of the brief covers it — nobody thinks to say "quantities are positive")*

### 4. Selling zero shares succeeds

```
sell("AAPL", 0, 150.00)  ->  Sold 0 AAPL for 0.0 (cost 0.0, profit 0.0)
```

A transaction that did nothing, reported as a successful sale.

### 5. A negative price is accepted

```
sell("AAPL", 5, -150.00)  ->  proceeds -750.0
```

### 6. Ticker format is never checked

`buy("aapl", 10, 100.00)` creates a position under the key `"aapl"`, which is a different
position from `"AAPL"`. The same company, held twice, and neither one knows about the other.

### 7. Lots do not exist, so the remaining position cannot be described

After selling 8 shares the correct answer is "2 shares at 100.00 and 5 at 120.00". This
implementation can only say "7 shares at an average of 106.67". The information needed to compute
FIFO later was destroyed at purchase time — `Position.add` folds every buy into a running total.

Switching to FIFO later is not a change to `sell()` — it is a change to how purchases are
stored. Within this program the per-purchase detail is gone for good; a real system could
rebuild it from a trade log, if one happens to exist elsewhere.

### 8. A liquidated position lingers

Sell all 15 shares, then try to sell 1 more:

```
java.lang.IllegalArgumentException: Not enough shares
```

The position object is still there with zero shares, so the error reports the wrong problem —
"not enough" rather than "you do not hold this stock".

### 9. Every failure is `IllegalArgumentException`

"You do not hold MSFT" and "you only have 15 shares" are different problems with different
answers — one is a lookup failure, the other a conflict. Here they are the same exception type,
distinguishable only by matching on the message text.

## What it gets right

Worth saying, because a strawman teaches nothing:

* **A failed sale changes nothing.** Validation runs before any mutation, so the refused sale of
  16 shares leaves 15 shares and a 0.00 balance — conformance C1 and C2, passed.
* **Selling the whole position** gives the right numbers (2250.00 / 1600.00 / 650.00), because
  when you sell everything, average cost and FIFO agree by definition.
* **Buying again after liquidation** starts from a clean cost basis (D4, passed).

## Scoring, for marking

| Part | Score | Passes |
| ---- | ----- | ------ |
| A — the money | 1 / 8 | A5 only: selling the entire position, where the method makes no difference |
| B — the refusals | 1 / 6 | B4 only. B3 refuses, but without reporting available versus requested |
| C — state after a refusal | 2 / 2 | both |
| D — the unmentioned parts | 1 / 4 | D4 only |
| **Total** | **5 / 20** | |

B3 is the one judgement call: it does refuse the oversized sale, but the message is
`"Not enough shares"` with no numbers, which is not what the check asks for. Score it either way
— just score every student's the same way.

## The point

Nine defects. Not one of them is a mistake in the sense of "the assistant did the task badly" —
each is a decision the brief left open, resolved silently and plausibly, and then locked in by a
test that agreed with it.

A specification would not have made the assistant smarter. It would have made the disagreement
visible before any code existed.
