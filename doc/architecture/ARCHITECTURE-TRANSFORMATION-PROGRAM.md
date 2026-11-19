# Architecture learning program

This program uses HexaStock to teach how business rules, application use cases and infrastructure work together. It is suitable for a university sequence or an engineering workshop; session length can be adapted to participants' prior Java and testing experience.

## Prerequisites and first success

Know Java methods, interfaces, collections and basic assertions. Start with [the beginner route](../learning/START-HERE.md), which explains the vocabulary and runs domain tests without a database. Introduce Maven modules, HTTP and transactions only when the corresponding session needs them.

Use `main` as the reference implementation. Carry out student changes in a personal fork or teaching copy, following the course's submission rules. Existing JPA and MongoDB adapters are examples to study, not unfinished features to introduce again.

## Session sequence

| Session | Activity | Evidence to submit |
|---|---|---|
| 1. Describe a rule | Work through a FIFO sale on paper | Quantities, cost basis, proceeds and profit with calculations |
| 2. Test the rule | Follow [the domain lessons](../tutorial/sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md) | A focused test and an explanation of its assertions |
| 3. Trace a use case | Follow [the HTTP and service stages](../tutorial/sellStocks/SELL-STOCK-04-HTTP-AND-PORTS.md) | A dependency map distinguishing calls from source dependencies |
| 4. Compare persistence | Inspect the two port implementations and shared contract tests | One shared behavior and one adapter-specific concurrency mechanism |
| 5. Extend behavior | Select one exercise below | Scenarios, a small implementation, relevant tests and a short ADR |
| 6. Review a trade-off | Measure or analyse reporting behavior | An explicit assumption, observed evidence and a justified design choice |

Finish each session by explaining the result to another reader. If the learner cannot explain why a test fails, adding another architectural pattern is unlikely to help.

## Extension choices

### A. Add a quote provider

Implement `StockPriceProviderPort` for a new provider or a deterministic local fixture. Specify ticker mapping, error handling, freshness and test behavior before changing configuration. See [the sell exercises](../tutorial/sellStocks/SELL-STOCK-EXERCISES.md).

**Acceptance evidence:** existing business rules still pass; adapter-specific response and failure mappings are verified; the domain does not import the provider client.

### B. Compare storage adapters

JPA/MySQL and MongoDB already exist. Trace one shared contract test through both, then explain why locking and version conflict detection need separate tests. See [the concurrency route](../tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md).

**Acceptance evidence:** identify the transaction boundary, a real concurrency failure and the retry behavior. Do not claim that interchangeable interfaces make all operational semantics identical.

### C. Extend lot selection

Use [the lot-strategy exercise](../tutorial/DDD-Hexagonal-exercise.md) to specify a policy other than FIFO. State which policies are supported in the assignment and retain the baseline behavior by default.

**Acceptance evidence:** deterministic ordering, partial-lot arithmetic, insufficient-quantity rejection and a justified home for the policy. This is an educational accounting exercise, not tax guidance.

### D. Add another inbound interface

As an advanced optional task, design a gRPC or command-line adapter calling an existing input port. No such adapter is implemented in the baseline.

**Acceptance evidence:** transport mapping, error semantics and unchanged domain tests. A new transport must not copy FIFO into its handler.

### E. Explore watchlists or reporting projections

[Watchlists](../tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) are a proposed extension. [Reporting lessons](../tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) begin with current behavior and then explore query alternatives.

**Acceptance evidence:** distinguish implemented behavior from the proposal, state a measurable requirement, and analyse consistency and recovery before adding asynchronous processing.

## A repeatable engineering workflow

1. State a behavior with concrete inputs, outputs and failures.
2. Identify the responsible domain operation and affected ports.
3. Write or refine tests that could detect an incorrect implementation.
4. Implement a small change and run the checks appropriate to its scope.
5. Review the diff, update the relevant contract and record any trade-off.

Feature files, OpenAPI and diagrams are maintained documents. They do not automatically enforce alignment with code. `@SpecificationRef` links tests to scenarios; assertions and actual execution establish what was checked. See [testing strategy](../tutorial/TESTING-STRATEGY.md) and [CI guidance](../../CI_DISCIPLINE_WALKTHROUGH.md).

## Assessment rubric

| Criterion | Evidence of understanding |
|---|---|
| Behavior | Calculates an example and names the rejected states |
| Design | Places rules and infrastructure responsibilities deliberately |
| Verification | Selects tests that exercise the claimed guarantee |
| Communication | Uses precise vocabulary and makes assumptions visible |
| Trade-offs | Explains a cost as well as a benefit; avoids unsupported performance claims |

Students can use development tools according to their course rules. Responsibility for explaining the design and validating the submitted result remains with the participant.

## Reference material

[Technical architecture](TECHNICAL-ARCHITECTURE-SPECIFICATION.md), [architecture comparison](DDD-HEXAGONAL-ONION-CLEAN-COMPARATIVE-STUDY.md), [API reference](../stock-portfolio-api-specification.md), [contribution checks](../../CONTRIBUTING.md), and [decision records](adr/README.md).
