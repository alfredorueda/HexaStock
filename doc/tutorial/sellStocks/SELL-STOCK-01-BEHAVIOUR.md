# 1. Specify the sale

**Goal:** Calculate the result before reading implementation code.

**Study time:** 10–15 minutes, including the checkpoint. **Before starting:** read the [learning path](SELL-STOCK-TUTORIAL.md).

## Start with observable behaviour

A sale changes both inventory and cash. Before choosing controllers or repositories, specify what a successful sale must produce. The canonical source is [sell-stocks.feature](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/sell-stocks.feature), linked to [US-07 in the API specification](../../stock-portfolio-api-specification.md#27-us-07--sell-stocks).

The feature file is a readable specification. **Cucumber does not execute it in this repository.** JUnit tests are the executable layer; `@SpecificationRef` records which scenario a test is intended to cover.

## Use one vocabulary

| Term | Meaning in this use case |
| --- | --- |
| Portfolio | An investor's cash and stock holdings; the aggregate root |
| Holding | All shares of one ticker within a portfolio |
| Lot | A purchase batch with its price, time and remaining shares |
| FIFO | Consume the oldest available purchase lots first |
| Proceeds | Sale price multiplied by shares sold |
| Cost basis | Original purchase cost of the shares consumed |
| Profit | Proceeds minus cost basis; a negative result is a loss |

These terms appear in the specification, Java methods and response fields. Keeping the meanings stable is the practical purpose of a ubiquitous language.

## Work the example by hand

Both scenarios start with these lots and a sale price of **150.00**:

| Purchase order | Shares | Purchase price |
| --- | ---: | ---: |
| Lot 1 — oldest | 10 | 100.00 |
| Lot 2 — newest | 5 | 120.00 |

| Scenario | Shares sold | Proceeds | Cost basis | Profit | Shares left |
| --- | ---: | ---: | ---: | ---: | ---: |
| `US-07.FIFO-1` | 8 | 1200.00 | 800.00 | 400.00 | 7 |
| `US-07.FIFO-2` | 12 | 1800.00 | 1240.00 | 560.00 | 3 |

For 12 shares, FIFO takes `min(10, 12) = 10` from Lot 1, then `min(5, 2) = 2` from Lot 2. Therefore:

```text
proceeds   = 12 × 150.00                  = 1800.00
cost basis = 10 × 100.00 + 2 × 120.00      = 1240.00
profit     = 1800.00 − 1240.00             =  560.00
```

Cash increases by **proceeds**, not profit. Cost basis describes money spent at purchase time; subtracting it from cash again would charge for the same shares twice.

## Read the scenario

This is an abbreviated Gherkin reading of `US-07.FIFO-2`; the canonical file includes the full background and response table:

```gherkin
Scenario: Selling shares consumed across multiple lots
  When I sell 12 shares of AAPL
  Then the sale response contains:
    | Field     | Value   |
    | proceeds  | 1800.00 |
    | costBasis | 1240.00 |
    | profit    |  560.00 |
  And FIFO consumed 10 shares from Lot #1 at 100.00 and 2 shares from Lot #2 at 120.00
  And Lot #1 is fully depleted and removed
  And the portfolio cash balance has increased by 1800.00
```

The acceptance criteria also cover failures. Keep those in view, but leave their HTTP mappings until [Stage 8](SELL-STOCK-08-ERRORS.md).

## Checkpoint

From a fresh starting state, sell 11 shares at 150.00. Calculate proceeds, cost basis, profit and the remaining lots. Explain which amount increases cash.

**Check your answer:** Proceeds are **1650.00**, cost basis **1120.00**, and profit **530.00**. Lot 1 is removed; Lot 2 retains **4 shares**. Cash increases by **1650.00**.

**Stop here** when you can explain the answer without looking at the code. You can resume at the next stage later.

---

[Previous](SELL-STOCK-TUTORIAL.md) · [Learning path](SELL-STOCK-TUTORIAL.md) · [Next: Stage 2](SELL-STOCK-02-DOMAIN-TEST.md)
