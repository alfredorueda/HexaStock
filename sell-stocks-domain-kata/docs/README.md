# Exercises

Two ways of building the same thing. The point is the contrast between them.

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
  behaviour, as acceptance criteria AC-01 … AC-23
* [`spec/domain-class-diagram.puml`](exercise-2-specification-driven/spec/domain-class-diagram.puml)
  — the structure

The prompt shrinks to about thirty lines, and most of what remains is the technology stack. The
specifications carry everything else — which means they can be reviewed, diffed, corrected and
reused without anyone reading the conversation that produced the code.

## Running them together

Exercise 1 first, and do not preview Exercise 2. The argument for specifications is not
persuasive as a claim; it is obvious as an experience, and only after Phase 4 of Exercise 1,
when a student is holding somebody else's code with no idea what it was supposed to do.
