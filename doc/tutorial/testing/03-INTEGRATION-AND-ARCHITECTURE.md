# 3. Verify the Assembled System

**Goal:** connect test results to claims that are neither too weak nor too broad.

## Integration: cross the configured boundaries

The REST integration tests start Spring Boot, send HTTP requests with RestAssured and use a database created by Testcontainers. The trading scenarios check sale amounts and remaining holdings after purchases and sales.

The price provider is controlled rather than live. A full application test therefore exercises the selected assembly; it is not a test of every production service or every profile combination. Separate MongoDB concurrency integration tests exercise that adapter's transactional behavior.

When a domain test passes and an integration test fails, investigate mapping, setup, transactions and wiring. Do not assume wiring is the only possible cause: the tests may use different inputs or expectations.

## Architecture: read the seven rules

`HexagonalArchitectureTest` imports compiled production classes and checks these forbidden dependencies:

| From | Must not depend on |
|---|---|
| Domain (`model`) | Application |
| Domain (`model`) | Adapters |
| Domain (`model`) | Spring |
| Application | Adapters |
| Application | Spring |
| Inbound adapters | Outbound adapters |
| Outbound adapters | Inbound adapters |

These are specific static dependency rules. They do not prohibit every outbound-to-outbound dependency, inspect arbitrary reflection strings or prove runtime behavior. Maven's declared production dependencies provide another boundary, but those declarations can themselves change.

After installing sibling modules, run the architecture checks without a database:

```bash
./mvnw -pl bootstrap -Dtest=HexagonalArchitectureTest test
```

## Traceability: inspect the assertions

`@SpecificationRef` associates a test with a requirement identifier. It is metadata, not a runner or coverage guarantee. The Gherkin files are not executed by Cucumber.

There are ten baseline feature files and five advanced extension files. Settlement, reservations and fees described in the latter are not implemented baseline features merely because a specification exists.

A concrete gap illustrates why references need review: the transaction-history integration test supplies a `type` parameter and asserts that at least one record returns. It does not assert that all returned records have the requested type. `TransactionService` currently ignores that parameter. A passing annotated test therefore does not establish filtering support.

When reviewing a scenario, compare its preconditions and every observable outcome with the test. Check values and state, not only that a response exists.

## Report the limits of evidence

A successful suite means its executed assertions passed in that environment. It does not establish complete requirement coverage, deployment readiness or absence of defects. Coverage measures execution, not assertion quality; a high percentage alone cannot establish correctness.

For a useful test report, record the revision, command, environment, failures and skipped tests. Cite counts from the actual test reports instead of copying a stale total into a chapter.

## Checkpoint

A test carries `@SpecificationRef("US-08.AC-2")` but only checks that a list is nonempty. What should a reviewer do?

**Answer:** compare the requirement with the assertions. If the requirement is filtering, add checks for inclusion and exclusion of transaction types when implementing that behavior. The annotation alone is insufficient evidence.

[Previous: adapters](02-ADAPTER-CONTRACTS.md) · [Back to the test map](../TESTING-STRATEGY.md) · [Traceability guide](../README.md)
