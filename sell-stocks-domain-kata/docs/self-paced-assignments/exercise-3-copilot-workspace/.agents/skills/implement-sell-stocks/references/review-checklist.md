# Independent conformance review

Use this checklist after implementation. Review the specifications directly; this file describes
the review process and deliberately does not restate the domain answers.

## Input integrity

- Confirm that `docs/spec/sell-stocks-spec.md` and `docs/spec/domain-class-diagram.puml` are the
  only domain inputs used.
- Confirm that no reference solution or code from an earlier exercise was copied.
- Record any conflict between behaviour and structure instead of choosing silently.

## Behaviour traceability

- Locate executable evidence for every criterion from AC-01 through AC-24.
- Confirm that parameterised inputs account for the expected 36 executed tests.
- Check exact financial results against the specification.
- Check state after successful operations, not only returned values.
- Check state after rejected operations and look specifically for partial mutation.
- Check validation order where more than one precondition fails.

## Structural conformance

- Compare every production class, field, method, visibility, and relationship with the class
  diagram.
- Flag missing members and extra members separately.
- Flag invented domain concepts even when their tests pass.
- Confirm that the project remains domain-only and has no framework or infrastructure layer.

## Technical constraints

- Confirm Java 21 and a standalone Maven build.
- Confirm JUnit 5 is the only dependency and is test-scoped.
- Confirm the required package root.
- Confirm all monetary values use `BigDecimal`, never `double` or `float`.
- Confirm tests compare monetary value rather than relying on scale-sensitive equality.

## Evidence and reporting

- Run the full test suite from a clean workspace.
- Run the bundled verification script when Bash is available; otherwise complete every item in
  this checklist and record the environment limitation in `evidence/session-notes.md`.
- Report findings with specification identifiers and file locations.
- Do not edit during the review. Hand findings back to the learner and implementer.
- Distinguish verified facts, suspected risks, and optional improvements.
