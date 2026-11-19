# 2. Reconcile specifications before trusting output

**Goal:** identify what each artifact establishes, and what still needs review. Allow 25 minutes.

HexaStock keeps several descriptions of the same system. Their overlap helps reviewers find contradictions; it does not make them automatically consistent.

| Artifact | Useful evidence | Limit |
|---|---|---|
| Gherkin | Business examples and scenario identifiers | The `.feature` files are not executed by a Cucumber runner here |
| JUnit tests | Executed assertions for specific inputs | Passing examples do not prove every case |
| `@SpecificationRef` | A link from a test to a scenario ID | Metadata does not enforce completeness or assertion quality |
| OpenAPI | Intended HTTP shapes and documented responses | It does not establish that every runtime error follows the schema |
| UML | Structure or interactions selected by the author | It can omit behavior or become stale |
| ADRs | A decision, its rationale, and consequences | A decision record is not runtime verification |
| ArchUnit | Seven specific dependency rules | It does not prove every architectural property |

Use the [API reference](../../stock-portfolio-api-specification.md), [architecture map](../../architecture/ARCHITECTURE-MAP.md), and [traceability guide](../README.md) as entry points. Read the source and tests when a claim depends on implementation details.

## A concrete consistency review

For a sell operation, inspect these relationships in order:

1. **Behavior:** quantities must be positive, holdings sufficient, and lot consumption FIFO. Calculate proceeds, cost basis, profit, and remaining lots independently.
2. **Transport:** the controller constructs value objects and maps the domain result into HTTP DTOs. DTO names and fields must match the current source and OpenAPI, not an invented `SellResponse` type.
3. **Coordination:** the application service loads the portfolio, obtains the quote, invokes the aggregate, and saves portfolio and transaction within its transaction boundary.
4. **Persistence:** JPA uses a pessimistic lookup; MongoDB uses version checking and retry infrastructure. Their conflict protocols differ even though both implement core-owned ports.
5. **Verification:** map each claim to a relevant assertion. Identify missing coverage rather than inferring it from a scenario annotation.

In `main`, application services use Jakarta transaction annotations and are constructed through bootstrap configuration; they are not Spring `@Service` components. Inbound adapters may use domain types. Domain events and settlement workflows are not baseline capabilities. These distinctions matter when generated examples are copied into a real project.

## Keep review responsibilities explicit

A domain expert reviews business meaning and boundary examples. An engineer reviews dependencies, transaction behavior, persistence mappings, error handling, and test adequacy. One person may perform both roles, but the questions remain distinct.

Read code even when tests pass. Check for mutable state escaping an aggregate, accidental framework dependencies, changed exception order, broad retries, and test fixtures that bypass the rule being demonstrated. Add performance or operational checks only when the change requires them; a test suite is not a production-readiness certificate.

**Checkpoint:** a new scenario is linked from a passing test, but that test asserts only that an HTTP response is successful. What evidence is still missing? At minimum, the scenario's business outcomes and relevant persisted state must be asserted.

Next: [maintain evidence across changes](03-EVOLVE-AND-EVALUATE.md).
