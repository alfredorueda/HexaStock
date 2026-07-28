# Exercise 1 — Build it from a prompt

You are going to build a small piece of financial software using an AI assistant, working the
way most people work with one: by describing what you want in conversation.

Read [`brief.md`](brief.md). That is your whole input.

## The one rule

**Everything goes through the conversation.** You may not write a specification, a list of
acceptance criteria, a class diagram, a schema, or a test plan. If you want the assistant to know
something, you say it in the chat.

You may talk to the assistant as long as you like, correct it, argue with it, and ask it to
change things. That is all allowed. What you may not do is write any of it down as a durable
artifact.

## What you hand in

1. **The code**, building and running.
2. **The tests**, passing. If the assistant did not write tests, ask it to.
3. **The full transcript of your conversation**, saved to a file. All of it, including the parts
   where it went wrong.

Point 3 is not paperwork. You will need it in Phase 4, and so will somebody else.

## How the session runs

| Phase | Time | What happens |
| ----- | ---- | ------------ |
| **1. Build** | ~40 min | You and your assistant build the thing from the brief. |
| **2. Self-assessment** | ~10 min | Your instructor releases a conformance checklist. You run it against your own code and score yourself. |
| **3. Comparison** | ~15 min | The class compares results. |
| **4. The change request** | ~15 min | You swap code with another pair — code only, no transcript — and are asked to change something. |
| **5. Debrief** | ~10 min | What just happened, and what Exercise 2 does about it. |

Do not read [`conformance.md`](conformance.md) before Phase 2. Opening it early does not make you
look good; it just removes the only interesting thing that happens to you today.

## What is actually being measured

Not whether your code works. Everyone's code will work — the assistant will make sure of it, and
it will write tests that prove it.

What is being measured is **whether your code does the same thing as everybody else's**, and
whether the things it does are the things the business actually needed. Those are different
questions, and only one of them is answered by a green test run.

## A hint you are allowed to have

When you finish, before Phase 2, ask yourself one question and write the answer down:

> *What did I never tell the assistant, that it had to decide on its own?*

Keep that list. In Phase 3 you will find out how much it was worth.
