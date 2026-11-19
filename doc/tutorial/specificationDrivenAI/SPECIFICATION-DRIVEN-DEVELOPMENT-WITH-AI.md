# Specification-driven development with AI

Specifications can make an implementation task easier to explain, review, and verify. An AI assistant can use that context to propose code and tests. The resulting artifacts still require independent checks: precise inputs reduce some ambiguity, but do not guarantee correct output.

This chapter is a practical learning route, using HexaStock's current `main` as its public example. Start after the [sell-stock tutorial](../sellStocks/SELL-STOCK-TUTORIAL.md); no particular AI product is required.

| Stage | You will produce | Suggested time |
|---|---|---:|
| [1. Specify a small change](01-SPECIFY-A-SMALL-CHANGE.md) | A bounded task with independently calculated expectations | 25–35 min |
| [2. Reconcile specifications](02-RECONCILE-SPECIFICATIONS.md) | A map from claims to source and test evidence | 25 min |
| [3. Evolve and evaluate](03-EVOLVE-AND-EVALUATE.md) | An experiment that measures benefits and limitations | 25–35 min |

Stop after any stage and keep its deliverable. Students can complete the same work manually before comparing an assisted attempt.

## What this case establishes

The original chapter described consulting experience with specification-led implementation. That experience motivates the method; it is not a controlled benchmark. The public repository permits inspection of artifacts and reproduction of tests, but does not establish which tool authored each file, reproduce private consulting sessions, or prove production suitability.

The practical proposition is modest: use concrete requirements, clear boundaries, and executable checks to make candidate implementations easier to assess. Human and generated implementations both benefit from that discipline. Model capabilities, engineering judgment, and operational context also affect the outcome.

## Further reading

* Kent Beck, *Test Driven Development: By Example* (2002): small failing tests and incremental implementation.
* Eric Evans, *Domain-Driven Design* (2003): domain language and modeling boundaries.
* [Architecture decisions](../../architecture/adr/README.md): decisions actually represented in this project.
* [Testing strategy](../TESTING-STRATEGY.md): what the current checks verify.
* [Rich and anemic models](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md): a separate exercise in rule ownership.

[Book home](../../BOOK-HOME.md)
