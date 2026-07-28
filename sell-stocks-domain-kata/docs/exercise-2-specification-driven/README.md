# Exercise — build this project from its specifications

This project can be regenerated from two files. Your job is to do exactly that, then judge the
result against the specification rather than against your own memory of what you asked for.

## What you are given

| File | What it settles |
| ---- | ---------------- |
| [`spec/sell-stocks-spec.md`](spec/sell-stocks-spec.md) | **Behaviour** — preconditions, the FIFO rule, the money definitions, and acceptance criteria AC-01 … AC-24 |
| [`spec/domain-class-diagram.puml`](spec/domain-class-diagram.puml) | **Structure** — classes, fields, methods, visibility, relationships |

Everything the program must do is in those two files. Nothing else is needed.

> **Student warning:** `reference-solution/` contains the completed result for instructors and
> later comparison. Do not inspect it before finishing your own implementation; doing so removes
> the regeneration experiment this exercise is designed to run.

## The prompt

Start an empty folder, put the two specification files in `docs/spec/`, and give your assistant
this:

```text
Implement the domain model described by the two specifications in this project:

  docs/spec/sell-stocks-spec.md      the behaviour: preconditions, the FIFO
                                     rule, the money definitions, and the
                                     acceptance criteria AC-01 to AC-24
  docs/spec/domain-class-diagram.puml the structure: classes, fields, methods,
                                     visibility and relationships

The specifications are authoritative. Implement what they say and nothing more:
do not add members that are absent from the class diagram, and do not invent
behaviour the acceptance criteria do not describe. If the two ever disagree,
follow the diagram for structure and the specification for behaviour, and tell
me about the conflict rather than choosing silently.

Technical constraints, which the specifications deliberately do not cover:

  - Java 21 and Maven. A standalone project with its own pom.xml and no parent.
  - JUnit 5 is the only dependency, in test scope. No Spring, no persistence,
    no REST layer, no logging library, no DTOs.
  - Package root: com.neueda.portfolio.domain
  - Every monetary amount is BigDecimal. Never double or float.

Tests:

  - One JUnit 5 test per acceptance criterion, named so that the criterion it
    covers is obvious from the test name.
  - Use the exact numbers given in the specification.
  - Compare BigDecimal amounts with compareTo, so that 1200 and 1200.00 count
    as equal.
  - Assert state, not only return values: the remaining lots and their share
    counts, the cash balance, and the fact that a rejected sale changes
    nothing at all.

Done when `mvn test` runs green and every acceptance criterion is covered.
```

That is the whole prompt. It is about thirty lines, and most of it is the technical stack —
because the hard part, the behaviour, already lives in the specifications.

## Checking what comes back

Do not accept a green build as proof. Check these, in this order:

1. **`mvn test` is green.** It should report **36 tests** from 24 criteria — two criteria are
   checked against a list of inputs each, and one has an extra case for the converse.
2. **Every criterion has a test.** Walk AC-01 to AC-24 and find the test for each one. A missing
   criterion is the most common failure, and a green build will not reveal it.
3. **The numbers are right.** Selling 12 shares of the baseline holding must give proceeds
   1800.00, cost basis 1240.00, profit 560.00 — and must leave one lot of 3 shares at 120.00.
   The classic wrong answer is a cost basis of 1280.00, which is what you get from a blended
   average price (1600.00 / 15 = 106.67 per share) instead of consuming lots oldest-first.
4. **A rejected sale changes nothing.** Try to sell 16 of 15 shares and confirm the lots and the
   cash balance are untouched. Validation must happen before any mutation, not during it.
5. **Nothing extra was invented.** Compare the classes against the diagram. Extra fields, a
   `Transaction` class, a repository interface, a `profit` field cached on the holding — none of
   that is in the specification, and none of it should appear.

## Instructor reference solution

After the student implementation has been assessed, run the worked result with:

```bash
cd reference-solution
mvn test
```

It is a standalone Maven project containing `pom.xml` and `src/`. It should execute exactly 36
tests. Its location is deliberately inside Exercise 2 so the code, prompt, specifications, and
worked result have one clear owner.

## Things worth trying afterwards

* **Delete the class diagram and regenerate from the behaviour spec alone.** What does the model
  lose? Which decisions does the assistant now make for you, silently?
* **Delete the acceptance criteria and keep only sections 1 to 4.** The prose describes FIFO
  perfectly well. Does the implementation still come out right? Do the tests?
* **Change one number in the specification** — say, the second lot's price — and regenerate.
  Everything downstream should follow. If it does not, the specification was not the source of
  truth you thought it was.

## Why the prompt is so short

A long prompt is written once, reviewed by nobody, and thrown away after use. The behaviour it
describes cannot be diffed, cannot be reviewed in a pull request, and is gone the moment the
conversation ends.

A specification is a file. It sits in the repository, it has a history, someone can object to a
line of it before the code exists, and the next person can regenerate the project from it without
ever seeing the conversation that produced it. Moving detail from the prompt into the
specification is what makes the work repeatable.

Notice which details stayed in the prompt: the language, the build tool, the test framework, the
package name, the ban on frameworks. Those are technology decisions, not domain rules — and they
belong in a versioned decision record rather than in a prompt, for exactly the same reason. That
is the next step described under Roadmap B in the [README](../../README.md).

## Next: make the working method reusable

[Exercise 3](../exercise-3-skills-and-agents/README.md) rebuilds the same result from the same two
specifications, but packages the repeated Explore → Plan → Implement → Review method as project
instructions, a skill, specialised agents, a durable plan, and deterministic verification. Keep
this Exercise 2 result as the baseline for that comparison.
