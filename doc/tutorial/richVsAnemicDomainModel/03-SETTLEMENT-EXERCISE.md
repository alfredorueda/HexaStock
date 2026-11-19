# 3. Specify a settlement-constrained extension

**Status:** proposed teaching exercise, absent from `main`. **Outcome:** define interacting rules precisely before proposing code or endpoints.

## Explicit teaching assumptions

The exercise chooses a two-business-day settlement delay, reservation flags and a fee policy. These are hypothetical rules, not a universal description of market practice. The earlier assertion that the SEC currently mandates T+2 was incorrect: the SEC's [settlement-cycle FAQ](https://www.sec.gov/exams/educationhelpguidesfaqs/t1-faq) describes the move to T+1 for covered transactions from May 28, 2024. Market-specific trading eligibility requires its own requirements; a settlement date alone does not justify a universal ban on resale.

For reproducible examples, freeze the valuation date at **2026-03-18** and use an exercise calendar containing Monday to Friday with no holidays. T+2 means two business dates after the purchase date. A Friday purchase on March 13 settles on Tuesday March 17; simply adding two calendar days would be wrong. A real calendar, timezone and cutoff policy would require a separate design.

The baseline `Lot` has no settlement date or reservation flag. The methods below are proposed names, not existing source APIs.

## Define the rules in order

1. A lot is settled when the valuation date is at or after its settlement date.
2. An eligible lot is settled, unreserved and has positive remaining quantity.
3. Before mutation, the sale must have enough eligible shares; otherwise reject without changing cash or lots.
4. Consume eligible lots in purchase order, with a deterministic tie-breaker specified for equal timestamps.
5. Calculate gross proceeds, subtract the fee and derive profit from net proceeds minus cost basis.
6. Persist portfolio and sale history atomically under the selected adapter's transaction and concurrency mechanism.

A default fee of 0.1% can be used for this exercise, rounded once to cents with `HALF_UP`. Scenarios supplying an explicit fee override that policy to isolate arithmetic. Define what happens for negative fees or a fee greater than proceeds; do not leave it implicit in a factory name.

## Work the combined example

| Lot | Purchase | Quantity | State on valuation date |
|---|---|---|---|
| A | $100 | 10 | Settled, reserved |
| B | $120 | 5 | Settled, unreserved |
| C | $130 | 3 | Unsettled, unreserved |

Eligible quantity is five. Selling those five at $140 with a $0.70 fee yields:

- Gross proceeds: $700.00.
- Cost basis: $600.00 from lot B.
- Net proceeds: $699.30.
- Profit: $99.30.

Lots A and C remain untouched. The identity is `600 + 99.30 + 0.70 = 700`. A request for six shares must fail before partial lot consumption.

## Assign responsibilities deliberately

`Lot` could own eligibility, `Holding` eligible quantity and FIFO consumption, and `Portfolio` cash coordination. A fee policy or result calculator can own the monetary identity. Alternatively, a shared domain policy can evaluate immutable lot data. Compare the designs against the same examples.

Capture the operation time once and pass it into calculations, or inject a clock at the appropriate boundary. Calling the system clock separately in each predicate can produce different answers around a cutoff and makes tests harder to reproduce.

A decision about an HTTP eligibility or reservation endpoint is separate from deciding where the rule lives. Define authentication, error mapping, IDs and concurrency requirements before adding a public contract.

## Scenario sources

The five extension feature files are proposed specifications, not executable Cucumber tests or proof of existing Java coverage: [settlement](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/settlement-aware-selling.feature), [reservations](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/reserved-lot-handling.feature), [fees](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/settlement-fees.feature), [FIFO eligibility](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/fifo-settlement-selling.feature), and [cross-path consistency](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/rule-consistency.feature).

**Checkpoint:** list the decisions still needed to turn this teaching calendar and fee assumption into a real product requirement.

Next: [verification](04-VERIFICATION.md). [Chapter index](RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md).
