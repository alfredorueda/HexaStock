# Instructor manual — Exercise 1

> Students build working software from a vague brief, then discover that everyone in the room
> built something different — and that their passing tests could never have caught it.

**90 minutes.** Short on time? Keep Phases 1–3 and drop Phase 4.

---

## The idea

Students believe working code with passing tests is finished code. Phase 1 confirms that.
Phase 3 takes it apart.

Two lessons, neither about the assistant:

**1. A prompt records a request, not a behaviour.** You can commit a prompt to git — people do.
What you get is a record of what somebody asked for once, before the code existed. It never says
what the software does *today*, nothing checks it against the code, and re-running it produces
something different anyway.

**2. The tests prove nothing.** The assistant wrote the code from its own reading of the brief,
then wrote the tests from that same reading. They were never going to fail.

Keep this for Phase 5: repair what is weak in a prompt — completeness, checkability, staying true
as the code changes — and you do not get a better prompt. You get a specification.

Do not tell them any of it. Let Phase 3 do it.

---

## The trap

[`brief.md`](brief.md) states the problem exactly and never names the method:

> the cost of the shares being sold depends on which shares you consider sold

Selling 12 of Alice's shares:

| Method | Cost basis | Profit |
| ------ | ---------- | ------ |
| **FIFO** — the answer we want | **1240.00** | **560.00** |
| Weighted average | 1280.00 | 520.00 |
| LIFO | 1300.00 | 500.00 |

All three are recognised cost-basis methods; which are *permitted* depends on jurisdiction and
instrument. Nobody is careless here — the requirement was incomplete, and incomplete
requirements look finished once someone resolves them quietly.

---

## Phase 1 — Build · 40 min

**Hand out** [`brief.md`](brief.md) and [`README.md`](README.md). Nothing else.

**Answer no domain questions.** You will get "should it be FIFO?", "what if they sell more than
they have?". Each one is a student finding a gap in the requirements — that discovery *is* the
exercise.

> "The brief is what you have. Decide, and note that you decided."

This is the hard part of running the session. Helping ruins it.

**Note as you circulate:** who used `double`; who asked a domain question and who never noticed
there was one; who had to ask for tests.

**Warn about the transcript** with 10 minutes left. No transcript, no Phase 4.

---

## Phase 2 — Self-assessment · 10 min

Release [`conformance.md`](conformance.md).

Someone will argue that weighted average is a standard accounting method. **They are right — say
so out loud.** That is the point: they did nothing wrong, the requirement was incomplete, and the
cost landed on a tax return instead of in a compiler error.

Insist on honest scoring. Phase 3 is only as good as the numbers.

---

## Phase 3 — Comparison · 15 min

Fill this in live, one row per pair, **before discussing anything**:

| Pair | Cost method | Money type | Sell 0 | Sell −5 | Sell 16 of 15 | Score |
| ---- | ----------- | ---------- | ------ | ------- | ------------- | ----- |

The spread on the board does the teaching. Then three moves:

**1. The divergence.** Same brief, same hour, same room, no two implementations agree.

> "Which of these would you have shipped?"

**2. The circular test** — the heart of the session.

> "Your tests passed. Whose understanding of the problem were they testing?"

Let the silence sit, then show the tolerance from the flawed solution:

```java
assertEquals(346.67, profit, 0.01);
```

That tolerance is not laziness — it is *required* once money is a `double`. The defect and the
thing that hides it were written together.

**3. Reproducibility.** Find two pairs with near-identical prompts and different results.

---

## Phase 4 — The swap · 15 min

Pairs trade **code only**. No transcript, no notes, no talking to the authors.

> "The regulator now requires LIFO instead. Make the change. Ten minutes."

They cannot tell which method the code implements without reading all of it, cannot tell which
behaviours were deliberate, and do not know what they are allowed to break.

Several will ask for the original conversation. **Refuse.**

> "What would you do at work, if the person who wrote this had left?"

Close with:

> "You had the code — the thing everyone calls the source of truth. Why wasn't it enough?"

---

## Phase 5 — Debrief · 10 min

Ask what they wish they had been handed at the start of Phase 4. Steer until somebody describes
acceptance criteria, then put their words next to the real thing.

Hand out Exercise 2:

- the behaviour lives in a file reviewable **before** any code exists
- an argument about FIFO happens in a pull request, not on a tax return
- the prompt drops to ~30 lines, and what is left is the tech stack, not the domain

Phase 4 is the argument. Do not over-explain it.

---

## Troubleshooting

| If this happens | Do this |
| --------------- | ------- |
| **Everyone chose FIFO** | Pivot to Parts B and C. Zero and negative quantities diverge almost every time, and the money type in Part D rarely disappoints. |
| **A pair scored near-perfect** | Ask how — usually they made the assistant list its assumptions. Then ask: *"Where does that knowledge live now?"* In a chat log. They are one closed tab from where everyone else is. |
| **"A better prompt would have fixed this"** | Agree, then finish the thought: that prompt would run to hundreds of lines and say what a specification says. At which point it *is* one. Treat this as the argument, not an objection. |
| **"But you can commit the prompt to git"** | Also correct. Committing it records what was *asked for*, once, before the code existed. A specification says what the system *does*, stays true as the code changes, and can be checked against it case by case. Storage is not the difference; purpose is. |

---

## The flawed reference solution

[`flawed-reference-solution/`](flawed-reference-solution/) is a working implementation with
**8 passing tests** that scores **5 out of 20**. Built exactly as Phase 1 asks: from the brief,
through conversation, no specification.

Use it as a demo if you have no lab time, as spare code for Phase 4, or as a marking reference —
it looks entirely reasonable until you check it against the numbers. Its README traces every
defect to the line of the brief that failed to prevent it.

**Do not show it before Phase 3.** Students who see it first pattern-match against it instead of
making their own mistakes, and their own mistakes are the curriculum.
