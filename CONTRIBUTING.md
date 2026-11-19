# Contributing to HexaStock

HexaStock is a Java 21 learning project. Contributions should improve both the working example and a student's ability to understand it. Bug reports, clearer explanations, tests and small exercises are as useful as new features.

If you are new to the project, begin with [Start Here](doc/learning/START-HERE.md) and follow one sale through the [staged tutorial](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md). You do not need to read the entire book before fixing a typo or improving an example.

## Make a small, reviewable change

1. Check existing issues. For a substantial feature or architectural change, describe the problem in an issue before implementing it.
2. Start from `main` in your fork or an authorized contribution branch.
3. Make focused commits: one coherent improvement per commit.
4. Run the checks appropriate to the change, and describe the result in the pull request.
5. Open a pull request against `main`. The protected branch requires the build check and the repository's review rules to be satisfied.

Explain the resulting behavior and why it matters. A useful bug report includes a reproducible input, the expected result and the actual result. A useful documentation change identifies the misconception it resolves.

## Set up and verify

Use **JDK 21** and the included Maven wrapper. Follow the [README](README.md) for application startup; Docker is needed for container-backed tests, but not for the first domain tests.

```bash
# Fast feedback on business rules; no Docker needed
./mvnw -pl domain test

# Complete reactor verification; Docker must be running
./mvnw clean verify
```

Testcontainers starts its own databases. Starting Compose separately is unnecessary for the test suite.

Maven test phases depend on naming and plugin configuration. Surefire includes `*Test` and `*Tests`, so it also runs classes named `*IntegrationTest`. Failsafe is configured for `*IT`. Do not infer that a test is a unit test merely because it runs during `test`.

For a prose-only change, check links, examples and rendering. For changes to behavior, run the relevant tests locally and the full verification before merging. State any check that could not be run; do not describe an unexecuted check as successful.

## Preserve the design's responsibilities

| Concern | Where it belongs | Example |
|---|---|---|
| Business invariant | Domain model | A sale cannot consume more shares than are held. |
| Use-case coordination | Application service | Load the portfolio, obtain a quote, invoke the sale and persist results. |
| External protocol | Adapter | Translate JSON, JPA entities or provider responses. |
| Object assembly | Bootstrap | Select an implementation for an application port. |

The domain has no production dependency on Spring or persistence frameworks. Its tests inherit shared test libraries from the parent POM. Application services may coordinate transactions through Jakarta annotations; avoid introducing Spring or adapter dependencies into that module.

Use existing value objects such as `Money`, `Price` and `ShareQuantity` when their semantics fit. Keep business updates behind `Portfolio`, the aggregate root. Some current methods expose mutable children, so encapsulation is a design responsibility as well as a property to review; it is not guaranteed by returning an unmodifiable collection alone.

The seven ArchUnit rules check selected dependency directions. Passing them is useful evidence, not proof that all architectural or domain requirements are satisfied.

## Keep requirements and implementation connected

For a behavioral change, update the relevant artifacts together:

- Describe observable examples in `doc/features/`.
- Update `doc/openapi.yaml` if the public HTTP contract changes.
- Add tests at the narrowest level that can demonstrate the behavior.
- Use `@SpecificationRef` where applicable to identify the requirement being tested.
- Explain changed behavior in the relevant chapter.

The `.feature` files are specifications; this repository does not run them through Cucumber. `@SpecificationRef` supplies traceability metadata, not an automatic assertion that the scenario is fully tested. Review the test's assertions as well as its reference.

Clearly identify whether a document describes the current implementation, a proposed extension or an exercise. Settlement, fees and watchlists must not be presented as implemented baseline behavior without corresponding code and tests.

## Write for a learner

Introduce a concrete example before its architectural name. Define unfamiliar terms on first use. A chapter should have a clear goal, prerequisites and a point where the reader can stop and check their understanding.

Keep advanced trade-offs available, but move them into separate linked sections or pages when they interrupt the main learning path. Prefer short, connected explanations over repeated summaries and long inventories of patterns. Qualify performance estimates and distinguish intended guarantees from guarantees actually enforced by code.

For GitBook compatibility:

- Use relative Markdown links for book pages and images, and register new chapters in [SUMMARY.md](SUMMARY.md).
- Link source code to the corresponding path on GitHub `main`; GitBook does not publish Java source files as book pages.
- Use ordinary Markdown images so the renderer preserves their aspect ratio. Keep captions and source attribution.
- Update diagram sources together with their rendered assets. Follow the [diagram maintenance guide](doc/tutorial/sellStocks/diagrams/README.md).
- Preserve existing entry-page paths when splitting a chapter, and update incoming section links.

Do not change license terms or remove attribution as an editorial cleanup.

## Before requesting review

- Does the change solve one identifiable problem?
- Do examples agree with the code on `main`, or explicitly state their proposed scope?
- Do tests demonstrate the changed behavior, including a relevant failure case?
- Are links and diagrams readable in the book as well as on GitHub?
- Does the PR description report the checks actually performed?

Report measured coverage or quality-gate results with their context. Avoid unsupported project-wide percentages or ratings. Clear evidence is more useful than a general claim of quality.
