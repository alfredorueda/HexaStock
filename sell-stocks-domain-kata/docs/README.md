# Exercises

Four steps that progressively make behaviour, workflow, and architecture explicit. The first
three build the same domain so their working methods can be compared; the fourth puts application
infrastructure around that domain.

## [Exercise 1 — Build it from a prompt](exercise-1-prompt-only/README.md)

You describe the whole problem to an assistant in one conversation. Whatever you forget to say,
it decides for you — plausibly, silently, and with passing tests. Then you try to change
something, and discover that the only record of what you asked for was a chat log.

* [`brief.md`](exercise-1-prompt-only/brief.md) — all the student gets
* [`conformance.md`](exercise-1-prompt-only/conformance.md) — released in Phase 2
* [`instructor-manual.md`](exercise-1-prompt-only/instructor-manual.md) — timings, what to
  expect, how to run the debrief
* [`flawed-reference-solution/`](exercise-1-prompt-only/flawed-reference-solution/) — a working,
  fully green, six-out-of-twenty implementation, with every defect traced to the line of the
  brief that failed to prevent it

## [Exercise 2 — Build it from specifications](exercise-2-specification-driven/README.md)

The same domain, but the behaviour lives in versioned files instead of in a conversation:

* [`spec/sell-stocks-spec.md`](exercise-2-specification-driven/spec/sell-stocks-spec.md) — the
  behaviour, as acceptance criteria AC-01 … AC-24
* [`spec/domain-class-diagram.puml`](exercise-2-specification-driven/spec/domain-class-diagram.puml)
  — the structure
* [`reference-solution/`](exercise-2-specification-driven/reference-solution/) — the standalone
  Maven result; keep it hidden until students have completed their own implementation

The prompt shrinks to about thirty lines, and most of what remains is the technology stack. The
specifications carry everything else — which means they can be reviewed, diffed, corrected and
reused without anyone reading the conversation that produced the code.

## [Exercise 3 — Build it with skills and specialised agents](exercise-3-skills-and-agents/README.md)

Repeat Exercise 2 from the same two authoritative specifications, but replace the one-off
implementation conversation with versioned context engineering:

* project and path-specific instructions
* an open-standard skill with a reference and verification script
* separate planner, implementer and conformance-reviewer agents
* a human-approved `plan.md` and durable evidence log
* GitHub Copilot configuration for students, plus separate Codex and Claude Code adapters for
  instructor rehearsal

The student guide is supported by an
[`instructor manual`](exercise-3-skills-and-agents/instructor-manual.md), a
[`compatibility guide`](exercise-3-skills-and-agents/compatibility.md), and explicit
[`slide traceability`](exercise-3-skills-and-agents/slide-alignment.md).

## [Exercise 4 — REST API and persistence](exercise-4-rest-and-persistence/README.md)

**Planned, not built.** The same domain, unchanged, wrapped in Spring Boot, a small REST API and
a database through JPA — no authentication, still specification-driven. It is where the error
contract and the SQL schema stop being documentation and become tested behaviour, and where the
domain gets to prove it survives infrastructure without being edited.

## Running them together

Run them in order:

1. Do not preview Exercise 2 before Exercise 1. The argument for specifications is learned through
   the divergence and code-swap experience.
2. Use Exercise 2 to establish a specification-driven baseline.
3. Use Exercise 3 with the same specifications and completion criteria. Change only the AI
   collaboration workflow so the comparison remains meaningful.
4. Use Exercise 4 after the domain is stable, when the class is ready to test whether it survives
   contact with infrastructure.

For the planned Copilot delivery, activate and verify the licences before the course, then run all
exercises with the approved Copilot setup and record tool versions, policy constraints, and any
compatibility adjustments.
