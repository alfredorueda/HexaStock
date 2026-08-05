# Sell Stocks exercise operating agreement

## Objective

Build the Sell Stocks domain from the two authoritative files in `docs/spec/`. This workspace is
the skills-and-agents variant of the specification-driven exercise. Do not search for, copy, or
inspect a completed implementation elsewhere.

## Authoritative inputs

- `docs/spec/sell-stocks-spec.md` owns behaviour.
- `docs/spec/domain-class-diagram.puml` owns structure.
- If they conflict, stop and report the conflict. Never choose silently.

## Required workflow

1. Use the `implement-sell-stocks` skill.
2. Explore both specifications before proposing changes.
3. Use the specification-planner role to produce the proposed contents of `plan.md`. It must not
   edit files. The learner records and refines the proposal in `plan.md`.
4. Obtain explicit human approval of the recorded plan.
5. Use the domain-implementer role to implement the approved plan.
6. Use the conformance-reviewer role for an independent review. The reviewer must report findings
   before any fixes are made.
7. Keep `plan.md` and `evidence/session-notes.md` accurate throughout the session.

Do not collapse planning, implementation, and review into one unreviewed agent run.

## Technical constraints

- Java 21 and a standalone Maven project with no parent.
- JUnit 5 is the only dependency and is test-scoped.
- Package root: `com.neueda.portfolio.domain`.
- Use `BigDecimal` for every monetary amount; never use `double` or `float`.
- No Spring, REST, persistence, logging framework, DTOs, authentication, or unrelated layers.
- Do not add members absent from the class diagram or behaviour absent from the specification.
- Preserve rejected-operation state and assert state as well as return values.

## Verification

- Run `mvn test` and require exactly 36 passing tests.
- Make AC-01 through AC-24 traceable in test names or display names.
- Run `.agents/skills/implement-sell-stocks/scripts/verify-workspace.sh` when Bash is available.
  Otherwise complete the bundled review checklist and record that fallback in the evidence log.
- Review the full diff before declaring completion.

## Safety and scope

- Work only inside this generated student workspace.
- Do not add an MCP server: this local exercise needs no external data or actions.
- Do not install dependencies beyond the specified JUnit test dependency.
- Ask before destructive operations, dependency changes, or scope expansion.
