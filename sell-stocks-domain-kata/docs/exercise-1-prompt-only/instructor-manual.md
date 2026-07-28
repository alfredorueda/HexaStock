# Instructor manual — Exercise 1

> **In one line:** students build working software from a vague brief, then discover that
> everyone in the room built something different — and that their passing tests could never
> have caught it.

**90 minutes.** Short on time? Keep Phases 1–3 and drop Phase 4.

---

## The idea

Students believe that working code with passing tests is finished code.

Phase 1 confirms that belief. Phase 3 takes it apart.

Two lessons, and neither is about the assistant:

**1. A prompt records a request, not a behaviour.** You can commit a prompt to git — people do.
What you get is a versioned record of what somebody asked for one afternoon, before the code
existed. It never says what the software does *today*, nothing checks it against the code, and
running it again produces something different anyway. It is not even what built the code: that
came from the whole conversation, corrections included.

**2. The tests prove nothing.** The assistant wrote the code from its own reading of the brief,
then wrote the tests from that same reading. They were never going to fail.

The first lesson has a useful consequence, and it is worth saving for Phase 5. Start repairing
what is weak in a prompt — make it complete, make it checkable case by case, keep it true after
the code changes — and you do not end up with a better prompt. You end up with a specification.
That is Exercise 2.

Do not tell them any of this. Let Phase 3 do it.

---

## The trap

[`brief.md`](brief.md) says:

> We buy the same stock more than once, at different times and at different prices. So the cost
> of the shares being sold depends on which shares you consider sold.

It states the problem exactly and never names the method. Selling 12 of Alice's shares:

| Method | Cost basis | Profit |
| ------ | ---------- | ------ |
| **FIFO** — the required answer | **1240.00** | **560.00** |
| Weighted average | 1280.00 | 520.00 |
| LIFO | 1300.00 | 500.00 |

All three are legitimate accounting methods. The brief just never said which one. Nobody is
careless — the requirement was incomplete, and incomplete requirements look finished once
somebody resolves them quietly.

---

## The session

| Phase | Time | Your job |
| ----- | ---- | -------- |
| 1. Build | 40 min | Circulate. Answer nothing. |
| 2. Self-assessment | 10 min | Release `conformance.md`. |
| 3. Comparison | 15 min | Fill the divergence table. |
| 4. The swap | 15 min | Trade code between pairs. |
| 5. Debrief | 10 min | Bridge to Exercise 2. |

---

## Phase 1 — Build · 40 min

**Hand out** [`brief.md`](brief.md) and [`README.md`](README.md). Nothing else.

**Answer no questions about the domain.** You will get "should it be FIFO?", "what if they sell
more than they have?", "should I check the ticker?". Every one of those is a student discovering
a gap in the requirements. That discovery is the exercise.

> "The brief is what you have. Decide, and note that you decided."

This is the hardest part of running the session. The urge to help is strong, and helping ruins it.

**Note for Phase 3** as you circulate:

- who used `double` for money
- who asked you a domain question, and who never noticed there was one to ask
- who had to request tests, and who was given them unprompted

**Warn about the transcript** with 10 minutes left. No transcript, no Phase 4.

---

## Phase 2 — Self-assessment · 10 min

Release [`conformance.md`](conformance.md). Expect quiet, then noise.

Someone will argue that weighted average is a perfectly standard accounting method.

**They are right. Say so out loud.** That is the whole point: they did nothing wrong, the
requirement was incomplete, and the cost of that landed on a tax return instead of in a compiler
error.

Insist on honest scoring — Phase 3 is only as good as the numbers.

---

## Phase 3 — Comparison · 15 min

Fill this in live, one row per pair, **before discussing anything**:

| Pair | Cost method | Money type | Sell 0 | Sell −5 | Sell 16 of 15 | Score |
| ---- | ----------- | ---------- | ------ | ------- | ------------- | ----- |

The spread on the board does the teaching. Then three moves, in order:

**1. The divergence.** Same brief, same hour, same room — no two implementations agree.

> "Which of these would you have shipped?"

**2. The circular test.** This is the heart of the session.

> "Your tests passed. Whose understanding of the problem were they testing?"

Let the silence sit. Then show them the tolerance from the flawed solution:

```java
assertEquals(346.67, profit, 0.01);
```

That tolerance is not laziness — it is *required* once money is a `double`, because the value is
never exact. The defect and the thing that hides it were written together.

**3. Reproducibility.** Find two pairs with near-identical prompts and different results. If
there is time, have one pair re-run their first prompt in a fresh chat and compare against
themselves.

---

## Phase 4 — The swap · 15 min

Pairs trade **code only**. No transcript, no notes, no talking to the authors.

> "The regulator now requires LIFO instead of what you have. Make the change. Ten minutes."

What you will see:

- they cannot tell which method the code implements without reading all of it
- they cannot tell which behaviours were deliberate and which were invented
- they do not know what they are allowed to break

Several will ask for the original conversation. **Refuse.**

> "What would you do at work, if the person who wrote this had left?"

Close the phase with:

> "You had the code — the thing everyone calls the source of truth. Why wasn't it enough?"

---

## Phase 5 — Debrief · 10 min

Ask what they wish they had been handed at the start of Phase 4. Steer until somebody describes
acceptance criteria, then write their words on the board next to the real thing.

Hand out Exercise 2 and make the contrast concrete:

- the behaviour lives in a file that can be reviewed **before** any code exists
- an argument about FIFO happens in a pull request, not on a tax return
- the prompt drops to about thirty lines, and what is left is the tech stack, not the domain
- anyone can rebuild the project without seeing the conversation that produced it

Phase 4 is the argument. Do not over-explain it.

---

## Troubleshooting

| If this happens | Do this |
| --------------- | ------- |
| **Everyone chose FIFO** (strong model) | Pivot to Parts B and C of the checklist. Zero and negative quantities diverge almost every time, and the money type in Part D rarely disappoints. |
| **A pair scored near-perfect** | Ask how they did it — usually they made the assistant list its assumptions. Have them tell the room, then ask: *"Where does that knowledge live now?"* In a chat log. They are one closed tab from where everyone else is, and they have just made your Phase 5 argument for you. |
| **"A better prompt would have fixed this"** | Agree completely — and follow it through. That prompt would run to several hundred lines and would say exactly what a specification says. At which point it *is* one. This is the best question you will get; treat it as the argument, not an objection. |
| **"But you can put the prompt in git"** | Also correct, and worth ten seconds on the board. Committing it gives you a record of what was *asked for*, once, before the code existed. A specification says what the system *does*, stays true as the code changes, and can be checked against it case by case. Storage is not the difference; what the document is for is the difference. |

---

## The flawed reference solution

[`flawed-reference-solution/`](flawed-reference-solution/) is a complete, working implementation
with **8 passing tests** that scores **6 out of 20** on the checklist. Built exactly as Phase 1
asks: from the brief, through conversation, with no specification.

Three ways to use it:

- **A demo** if you have no lab time and are making the point in a lecture.
- **Spare code for Phase 4**, so nobody is blocked by a partner who did not finish.
- **A marking reference** — it looks entirely reasonable until you check it against the numbers.

Its README traces every defect back to the line of the brief that failed to prevent it.

**Do not show it before Phase 3.** Students who see it first will pattern-match against it
instead of making their own mistakes — and their own mistakes are the curriculum.
