# 4. Test the comparison fairly

**Outcome:** support a design argument with observations that could disprove it.

## Begin with the implemented baseline

From the repository root with JDK 21:

```bash
./mvnw -pl domain test
```

This needs no database. Inspect `PortfolioTest`, `HoldingTest`, `LotTest` and `SellResult` behavior rather than expecting settlement classes or a fixed historical test count. Database-backed checks require the setup described in [testing strategy](../TESTING-STRATEGY.md).

## Plan tests for the proposed extension

| Property | Smallest useful example |
|---|---|
| Settlement cutoff | Just before, exactly at and after the specified eligibility boundary |
| Calendar | Friday purchase crosses a weekend under the exercise calendar |
| Reservation | A settled reserved lot is excluded; unreserving restores eligibility |
| FIFO | Skip ineligible older lots and partially consume the first eligible lot |
| Rejection before mutation | Request exceeds eligible quantity; compare all state before and after |
| Fees | Nonzero fee, rounding tie and fee absorbing the entire gain |
| Identity | Cost basis + profit + fee equals gross proceeds |
| Cross-path agreement | Query and command use the same eligibility decision for the same snapshot |
| Persistence | New fields survive save/reload and deterministic ordering is preserved |
| Atomicity | Failure during persistence rolls back both portfolio and transaction record |
| Concurrency | Competing operations cannot consume the same eligible shares twice |

These are test obligations, not claims of current coverage. A pure domain test cannot prove database rollback, and a single-request integration test cannot prove behavior under two concurrent writers.

## Compare implementations without changing the question

Give both implementations the same fixtures, clock, fees and expected outcomes. If their APIs differ, use a small test adapter to express the same operation; do not silently omit a scenario for one model. Report the revision, commands, environment and observed results if you run an experiment.

A deliberately seeded missing reservation check should make a focused test fail. That establishes that the test detects that defect. It does not establish a universal failure rate for anemic models or make rich models immune to bugs.

Check the assertions themselves. A test that only checks response status or list size may pass while profit, lot selection or transaction filtering is wrong. `@SpecificationRef` helps navigation but does not execute the feature file or validate the assertion's meaning.

## Suggested classroom session

1. Calculate the baseline FIFO example by hand.
2. Trace the current domain operation and run its tests.
3. Compare the two illustrative predicates from lesson 2.
4. Add a failing counterexample for a reserved lot in an isolated exercise implementation.
5. Centralize the rule and explain which callers now share it.
6. Review what remains unproved: encapsulation, persistence and concurrency.

Keep the exercise implementation separate from the reference repository according to course rules. No experimental repository branch is required for this lesson.

## Questions for the review

Where is the rule defined? Can any constructor or mutation path bypass it? Does a query promise a snapshot that can become stale before a command? Which boundary changed when a new rule was added? Would a simpler transaction script be easier to maintain for the actual behavior?

A useful conclusion names a concrete benefit and cost of the chosen design. Counting methods or comparing deliberately unequal test suites cannot replace that analysis.

[Chapter index](RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md).
