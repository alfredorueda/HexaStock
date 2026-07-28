---
name: implement-sell-stocks
description: Plan, implement, and independently verify the Java 21 Sell Stocks FIFO domain from the authoritative behaviour specification and class diagram. Use when building or reviewing this kata, mapping AC-01 through AC-24 to JUnit 5 tests, checking specification conformance, or repairing a generated solution without inventing infrastructure or domain behaviour.
---

# Implement Sell Stocks

Build the domain from repository specifications through a gated Explore, Plan, Implement,
Review workflow. Keep the specifications authoritative and keep the human in control at each
handoff.

## 1. Explore

1. Read `../../../docs/spec/sell-stocks-spec.md` completely.
2. Read `../../../docs/spec/domain-class-diagram.puml` completely.
3. Read the repository-level instructions (`AGENTS.md`, plus any tool-specific adapter).
4. Inspect the current workspace without looking for an existing solution outside it.
5. Report missing inputs or conflicts. Do not resolve specification conflicts silently.

## 2. Plan

1. Produce a complete proposal for `../../../plan.md` from the supplied template. If the planner
   is read-only, return the proposal for the learner or parent agent to record.
2. Map every planned production type to the class diagram.
3. Map AC-01 through AC-24 to named tests and record expected test multiplicity.
4. Record the build, conformance, and state-mutation checks.
5. Stop before editing any file and ask the learner to record, refine, and approve the plan.

If an approved plan already exists, confirm that it still matches the specifications before
continuing.

## 3. Implement

1. Follow the approved plan in dependency order: build configuration, value objects, entities,
   exceptions, aggregate behaviour, then acceptance tests.
2. Implement only members present in the class diagram and behaviour required by the
   specification.
3. Keep the domain pure Java. Do not add Spring, REST, persistence, DTOs, logging, or unrelated
   abstractions.
4. Use Java 21, Maven, JUnit 5 in test scope, and `BigDecimal` for every monetary value.
5. Run focused tests while working, then run the full suite.
6. Update `plan.md` as tasks complete; do not rewrite the plan to hide deviations.

## 4. Review

Use an independent reviewer role or agent. The reviewer must not edit the solution during the
review.

1. Read `references/review-checklist.md`.
2. Trace every acceptance criterion to executable evidence.
3. Compare the implemented structure with the class diagram.
4. Inspect rejected operations for partial mutation and verify FIFO state, not only return values.
5. Run `scripts/verify-workspace.sh` from this skill directory.
6. Report findings before fixes. Return to the implementer only after the learner chooses which
   findings to address.

## Completion contract

Finish only when:

- `mvn test` passes with 36 tests and no failures or errors;
- AC-01 through AC-24 are visibly traceable in the test source;
- the implementation matches the class diagram and contains no infrastructure;
- the verification script passes;
- `plan.md` records completed tasks and any deliberate deviations; and
- the final response lists changed files, verification performed, and unresolved concerns.

Do not claim that a green test run alone proves conformance.
