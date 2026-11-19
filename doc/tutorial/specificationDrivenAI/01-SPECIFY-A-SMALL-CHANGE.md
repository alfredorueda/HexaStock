# 1. Specify a small change

**Goal:** turn a requirement into a reviewable implementation task. Allow 25–35 minutes. First complete [one sale by hand](../sellStocks/SELL-STOCK-01-BEHAVIOUR.md).

A specification states an intended behavior. An implementation is one attempt to realize it. An AI assistant can help draft either, but neither becomes correct merely because the other agrees with it: both can share the same mistake.

## Begin with an independent example

Consider two purchases, 10 shares at 100 and then 5 at 120. Selling 12 at 130 under FIFO consumes all 10 older shares and 2 newer shares. Independently calculate:

| Outcome | Calculation | Value |
|---|---|---:|
| Proceeds | 12 × 130 | 1560 |
| Cost basis | 10 × 100 + 2 × 120 | 1240 |
| Realized profit | 1560 − 1240 | 320 |
| Remaining position | 3 shares at 120 | Cost basis 360 |

These expected values come from the rule, not from running the implementation. Use them to review generated tests. In current HexaStock, depleted lots are removed; transaction records preserve trading history separately. Fees and settlement are proposed extensions, not hidden assumptions in this example.

## Assemble a small context packet

| Question | Repository evidence |
|---|---|
| What behavior is intended? | [US-07 contract](../../api/US-07-SELL-STOCKS.md) and its linked Gherkin scenarios |
| Who owns the rule? | [FIFO lesson](../sellStocks/SELL-STOCK-03-FIFO.md) and current domain classes |
| How is it reached? | [HTTP and ports](../sellStocks/SELL-STOCK-04-HTTP-AND-PORTS.md) |
| How is it persisted? | [Persistence lesson](../sellStocks/SELL-STOCK-06-PERSISTENCE.md) |
| What must be verified? | [Testing strategy](../TESTING-STRATEGY.md) |

Supply the relevant files and the checked-out revision, rather than an entire repository dump. Explicitly distinguish existing behavior from the requested change. For example, a maximum deposit amount would be a new rule; it is not part of the current deposit contract.

An illustrative task instruction is:

> Review the current sell implementation and its tests against the FIFO example above. Identify mismatches before editing. Keep domain rules in the aggregate and retain the current port signatures unless a contract change is justified. Show the changed files, checks performed, and unresolved assumptions.

A prompt is a work instruction, not a security boundary or proof of correctness. Repository access and tool permissions remain separate controls.

## Work in reviewable increments

1. Agree on one behavior and its failure cases. Resolve ambiguous rounding, dates, and ordering before encoding expected results.
2. Write or inspect a focused domain test. In a TDD exercise, observe the intended failure before changing production code.
3. Implement the smallest relevant change and run that test.
4. Check adjacent failure cases and the relevant adapter or integration contract.
5. Review the diff, update specifications, and commit the coherent change.

Generating implementation and then tests can still be useful, but it is not the red–green–refactor sequence of TDD. A generated passing test may only confirm the implementation's assumptions. Ask a reviewer to recompute at least one expected result independently.

**Checkpoint:** explain why `profit == proceeds − cost basis` alone is an insufficient FIFO test. A wrong lot order can satisfy that equation while selecting the wrong cost basis.

Next: [reconcile the specifications](02-RECONCILE-SPECIFICATIONS.md).
