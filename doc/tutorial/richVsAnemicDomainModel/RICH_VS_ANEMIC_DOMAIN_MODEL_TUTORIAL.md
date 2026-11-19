# Rich and anemic models: where should the rules live?

A rich domain model places behavior alongside the data it governs. An anemic model mainly carries data while other code performs the operations. The useful design question is where a rule has one clear owner and how every relevant caller uses it.

This tutorial starts from **`main`** and then develops a clearly marked extension exercise. Settlement, reservations, fees and their proposed endpoints are not implemented in the baseline. No experimental branch checkout or branch test-result comparison is needed.

## A gradual route

| Lesson | Question | Output |
|---|---|---|
| [1. Inspect the current sale](01-CURRENT-MODEL.md) | Which object makes each decision? | A responsibility map |
| [2. Diagnose duplicated rules](02-RULE-DRIFT.md) | How can two callers disagree? | A counterexample and a shared rule |
| [3. Specify a richer extension](03-SETTLEMENT-EXERCISE.md) | How do time, reservation and fees interact? | Acceptance examples |
| [4. Test the comparison fairly](04-VERIFICATION.md) | What evidence supports the design claim? | A test plan and review argument |

For beginners, complete lessons 1 and 2 first. Lessons 3 and 4 are optional advanced work after understanding the baseline FIFO algorithm.

## What a comparison can establish

A deliberately incorrect formula demonstrates a defect, not that every service-based model must fail. Rich models can also duplicate rules, expose mutable state or validate incompletely. Conversely, a service-oriented design can centralize a policy and test it consistently.

Keep the same requirements and meaningful observations when comparing designs. Different endpoints or different test suites do not support an uncontrolled claim that one architectural label caused every observed failure.

## Further reading

- Martin Fowler, [Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html), and *Patterns of Enterprise Application Architecture*, 2002, for the distinction between Domain Model and Transaction Script.
- Eric Evans, *Domain-Driven Design*, 2003, and Vaughn Vernon, *Implementing Domain-Driven Design*, 2013, for invariants and aggregate boundaries.
- [HexaStock architecture comparison](../../architecture/DDD-HEXAGONAL-ONION-CLEAN-COMPARATIVE-STUDY.md), for separating modeling choices from dependency structure.
- [Diagram sources](https://github.com/alfredorueda/HexaStock/tree/main/doc/tutorial/richVsAnemicDomainModel/diagrams). Captions distinguish current code from hypothetical designs.
