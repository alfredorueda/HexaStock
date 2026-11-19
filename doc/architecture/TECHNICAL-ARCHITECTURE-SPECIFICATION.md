# Technical architecture: a guided reference

HexaStock is an educational portfolio application. It demonstrates a rich domain model, ports and adapters, two persistence implementations and automated checks. It is not a complete brokerage platform: authentication, settlement, fees and production operations are outside the implemented baseline.

This reference describes **`main`**. Design proposals elsewhere in the book are exercises, not additional runtime capabilities. For a first encounter, start with [one sale in nine stages](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md).

## Read in three passes

| Pass | Question | Read | Check your understanding |
|---|---|---|---|
| 1 | Where does a business rule belong? | [Architecture map](ARCHITECTURE-MAP.md) | Locate FIFO without opening a controller or repository. |
| 2 | What makes an operation atomic? | [Runtime and verification](RUNTIME-AND-VERIFICATION.md) | Separate a transaction, a lock and a retry. |
| 3 | Why were these choices made? | [Decision records](adr/README.md) | Explain one benefit and one cost of a chosen design. |

Stop after each pass. No database setup is necessary to understand the first map or run domain tests.

## Different artefacts answer different questions

| Artefact | Role | Limit |
|---|---|---|
| [Behavioral scenarios](../tutorial/README.md) | Examples of required behavior | Feature files are documents, not Cucumber executions. Some describe proposed extensions. |
| [API reference](../stock-portfolio-api-specification.md) and [OpenAPI](https://github.com/alfredorueda/HexaStock/blob/main/doc/openapi.yaml) | HTTP requests, responses and errors | Maintained separately from generated SpringDoc output; drift is possible. |
| [Diagrams](../tutorial/sellStocks/diagrams/README.md) | Explain structure and execution | Diagrams do not execute or enforce the architecture. |
| [ADRs](adr/README.md) | Record decisions and consequences | Alternatives are analysis, not necessarily measured experiments. |
| [Tests](../tutorial/TESTING-STRATEGY.md) | Check selected properties | Passing tests do not prove every possible input, race or architectural rule. |

`@SpecificationRef` links a test to a scenario identifier. Review its assertions as well as the annotation: metadata alone does not establish coverage.

## Domain scope and limits

The baseline creates portfolios, changes cash, buys and sells whole shares, records transactions, looks up quotes and reports holdings performance. FIFO consumes the oldest lots, deleting depleted lots while retaining an empty holding. Historical transactions are stored separately from the live portfolio graph.

Money is interpreted as USD; there is no currency-conversion model. Amounts use decimal arithmetic, with operation-specific validation and rounding. Public access to mutable child entities means the intended aggregate boundary still depends on caller discipline. See [the consistency boundary](aggregates/01-CONSISTENCY-BOUNDARY.md).

The API currently accepts a transaction `type` parameter without applying it. Reporting reads the full transaction history. Provider failures are not silently converted into successful zero-price reports. These are material behaviors, described in [the operation pages](../stock-portfolio-api-specification.md).

## Extension backlog

Useful future decisions include authorization, API versioning, migration-managed schemas, request idempotency, explicit event delivery, bounded reporting queries and deployment operations. Settlement and watchlists are separate exercises. A proposed change should identify its scenario, affected port, tests and operational consequences before being treated as part of the baseline.

For setup use [the project README](../../README.md); for contribution checks use [CONTRIBUTING](../../CONTRIBUTING.md). Those guides are the maintained source for commands, rather than duplicated setup instructions here.
