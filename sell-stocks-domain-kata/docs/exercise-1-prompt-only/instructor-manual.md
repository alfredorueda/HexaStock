# Instructor manual — Exercise 1

## What this exercise is for

Students arrive believing that a working program with passing tests is a finished program. This
exercise is designed so that belief survives Phase 1 completely intact, and does not survive
Phase 3.

The lesson is not "AI assistants are unreliable". They are not — they will produce clean,
defensible, well-tested code from the brief.

There are two lessons, and neither one is about the assistant. Both are about the *prompt*.

**1. A prompt is not something a team can work with.** Nobody can read it and object before the
code gets written. Nobody can see what changed between one version of the requirements and the
next. Nobody can pick it up in six months and find out what was agreed. It is a conversation,
and conversations disappear.

**2. The tests do not rescue you.** The assistant read the brief, wrote the code from its own
interpretation, and then wrote the tests from that same interpretation. Those tests were never
going to fail. They prove that the assistant agreed with itself. They say nothing about whether
the code does what the business actually needed.

Do not tell them any of that. They will tell you, in Phase 5, if the session works.

## The trap, and why it is fair

[`brief.md`](brief.md) says this:

> We buy the same stock more than once, at different times and at different prices. So the cost
> of the shares being sold depends on which shares you consider sold.

It states the problem precisely and never names the method. A competent assistant will pick one
and move on, usually without flagging the choice. With Alice's portfolio, selling 12 shares:

| Method | Cost basis | Profit |
| ------ | ---------- | ------ |
| FIFO | 1240.00 | 560.00 |
| Weighted average | 1280.00 | 520.00 |
| LIFO | 1300.00 | 500.00 |

Three defensible readings, three different numbers on a tax return. Nobody was careless. The
requirement was incomplete, and incomplete requirements do not announce themselves — they get
resolved silently and look finished.

Expect a spread across the room. If everyone happens to land on FIFO, you still have Parts B, C
and D, where the divergence is reliably wide.

## Timing

| Phase | Time | You are doing |
| ----- | ---- | ------------- |
| 1. Build | 40 min | Circulating. Answering nothing. |
| 2. Self-assessment | 10 min | Releasing `conformance.md`. |
| 3. Comparison | 15 min | Running the divergence table. |
| 4. Change request | 15 min | Swapping code between pairs. |
| 5. Debrief | 10 min | Bridging to Exercise 2. |

Ninety minutes. If you have sixty, drop Phase 4 and keep Phases 1–3; the core lesson survives.
If you have to cut further, cut Phase 1 to 25 minutes rather than cutting Phase 3.

## Phase 1 — Build (40 min)

Hand out [`brief.md`](brief.md) and [`README.md`](README.md). Nothing else.

**Answer no questions about the domain.** You will be asked "should it be FIFO?", "what if they
sell more than they have?", "should I validate the ticker?". Every one of those questions is a
student discovering a gap in the requirements, which is exactly the experience you want them to
have and remember. Say: *"The brief is what you have. Decide, and note that you decided."*

This is the hardest part of running the session. The temptation to help is strong and it destroys
the exercise.

Circulate and note, for Phase 3:
- which pairs asked you a domain question and which never noticed there was one to ask
- who used `double` for money
- who asked their assistant to write tests, and who was given tests unprompted

**Enforce the transcript.** Warn at 10 minutes remaining. Pairs who lose their transcript cannot
do Phase 4 properly, and Phase 4 is the best part.

## Phase 2 — Self-assessment (10 min)

Release [`conformance.md`](conformance.md).

Expect the room to go quiet, then loud. The usual sequence: satisfaction, then A1, then a hand
goes up to argue that weighted average is a perfectly standard accounting method.

**That student is right, and you should say so.** Weighted average is legitimate and is used in
several jurisdictions. That is the entire point: they did nothing wrong, the requirement was
underspecified, and the cost of the ambiguity landed on the tax return rather than in a
compiler error.

Insist on honest scoring. The value of the next phase depends on the numbers being real.

## Phase 3 — Comparison (15 min)

Draw this on the board and fill it in live, one row per pair:

| Pair | Cost method | Money type | Sell 0 | Sell −5 | Sell 16 of 15 | State after refusal | Classes | Score |
| ---- | ----------- | ---------- | ------ | ------- | ------------- | ------------------- | ------- | ----- |

Fill the columns before you discuss anything. The visual spread does the teaching; your
commentary adds very little to it.

Three things to draw out, in this order:

**1. The divergence.** Same brief, same assistant, one room, one hour — and no two
implementations agree. Ask: *which of these would you have shipped?* They all pass their tests.

**2. The circular validation.** This is the heart of the session. Ask the room:

> Your tests passed. Whose understanding of the problem were they testing?

Let the silence sit. The assistant wrote the code from its interpretation, then wrote tests
from the same interpretation. The tests could not have failed. They confirmed the assistant
agreed with itself.

Then point at Part D's tolerance: `assertEquals(346.67, profit, 0.01)`. The tolerance was not
carelessness — it is *necessary* once money is a `double`. A defect and its own cover story,
generated together.

**3. Reproducibility.** Find two pairs whose prompts were nearly the same and whose results were
not. If you have time, have one pair re-run their exact first prompt in a fresh conversation and
compare against their own earlier output.

## Phase 4 — The change request (15 min)

Pairs swap **code only**. No transcript, no notes, no talking to the authors.

Then: *"The regulator has ruled that this must use LIFO instead. Make the change. You have ten
minutes."*

What happens:

- They cannot tell which method the code implements without reading it carefully.
- They cannot tell which behaviours were deliberate and which the original assistant invented.
- They do not know what must not break, because nothing recorded what it was supposed to do.
- Several will ask to see the original conversation. **Refuse.** Ask them what they would do at
  work when the person who wrote it has left.

Close with: *"You had the code, which is the artifact everyone says is the source of truth. Why
was that not enough?"*

## Phase 5 — Debrief (10 min)

Ask what they would want to have been given at the start of Phase 4. Steer until somebody
describes acceptance criteria, then write their words on the board next to the real thing.

Then hand out Exercise 2, and make the contrast concrete:

- The behaviour lives in a file that can be reviewed *before* any code exists.
- Disagreement about FIFO happens in a pull request, not in a tax return.
- The prompt shrinks to about thirty lines, and what is left is the technology stack — not
  the domain.
- Anyone can regenerate the project without ever seeing the conversation that produced it.

Their Phase 4 experience is the argument. Do not over-explain it.

## The flawed reference solution

[`flawed-reference-solution/`](flawed-reference-solution/) is a complete, working, fully green
implementation built exactly as Phase 1 asks — from the brief, through conversation, with no
specification. Use it for whichever of these fits your session:

- **A demonstration** if you have no lab time and are teaching the point in a lecture.
- **A pre-supplied "another pair's code"** for Phase 4, so nobody is blocked by a partner who
  did not finish.
- **A calibration reference** while marking: it scores **6 out of 20** on `conformance.md` and
  looks completely reasonable until you check it against the numbers.

Its README lists every defect and, more usefully, which line of the brief failed to prevent it.

Do not show it before Phase 3. Students who see it first will pattern-match against it instead
of making their own mistakes, and their own mistakes are the curriculum.

## If the session is not landing

**Everyone chose FIFO.** It happens with a strong model. Pivot to Parts B and C, where divergence
is near-guaranteed — the zero-quantity and negative-quantity cases in particular. And the money
type in Part D rarely disappoints.

**A pair got a near-perfect score.** Ask how. Almost always they interrogated the brief hard and
made their assistant enumerate assumptions. Have them say so to the room, then ask the question
that matters: *"Where does that knowledge live now?"* It lives in a chat log. They are one
closed tab from being back where everyone else is. That pair has just made your Phase 5 argument
better than you could.

**Someone objects that a better prompt would have fixed everything.** Agree, completely — and
follow it: yes, and that prompt would be several hundred lines, and it would say exactly what a
specification says. The difference is not what it contains. The difference is that one of them is
a file with a history, and the other is a message in a chat window.
