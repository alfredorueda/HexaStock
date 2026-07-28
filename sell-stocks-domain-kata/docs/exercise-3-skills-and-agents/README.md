# Exercise 3 — Build it with skills and specialised agents

> **Student tool policy:** use GitHub Copilot in VS Code for this exercise. Codex and Claude
> adapters are maintained separately for instructor rehearsal and portability checks; they are not
> alternative student paths during the lab.

You will build exactly the same domain as Exercise 2 from exactly the same two specifications.
This time, the prompt is not the workflow. The workflow is versioned as project instructions, an
agent skill, specialised agents, reusable prompt files, a plan, and deterministic checks.

The experiment changes one variable:

| Exercise 2 | Exercise 3 |
| --- | --- |
| One specification-driven implementation prompt | The same specifications plus reusable context engineering |
| One general-purpose assistant conversation | Separate planner, implementer, and reviewer roles |
| Verification described in the prompt | Verification packaged in a skill and script |
| Conversation carries the process | Repository files carry the process |

Do not use the completed implementation elsewhere in this training repository. The point is to
regenerate it from the inputs and compare the working method with Exercise 2.

## Student quick start — GitHub Copilot

There are three AI steps: **plan, implement, review**. Only the second step generates Java code.

### 1. Create and open your workspace

From the `sell-stocks-domain-kata` directory, run:

```bash
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh \
  ~/workshops/sell-stocks-exercise-3
cd ~/workshops/sell-stocks-exercise-3
git init
git add .
git commit -m "chore: initialise exercise 3"
code .
```

The script creates the exercise inputs and AI configuration. It deliberately creates **no**
`pom.xml`, `src/main`, or `src/test` yet.

### 2. Ask Copilot to plan

Open Copilot Chat in VS Code and run:

```text
/create-plan
```

Copy the proposed plan into `plan.md`, review it, change at least one item yourself, and set both
the status and Human checkpoint decision to `approved`.

### 3. Generate the Java project

Run:

```text
/implement-approved-plan
```

This is the step that creates `pom.xml`, production code, and JUnit tests. Review the proposed
changes at each checkpoint instead of accepting every edit automatically.

### 4. Review and verify

Run the independent review:

```text
/verify-conformance
```

Then verify the result yourself:

```bash
mvn test
.agents/skills/implement-sell-stocks/scripts/verify-workspace.sh
```

You are done when both commands pass and Maven reports exactly **36 tests**.

> If a slash command is missing, do not improvise. Check **Chat: Open Customizations**, confirm
> that the generated folder is the VS Code workspace root, and ask the instructor for the
> compatibility fallback.

## Learning objectives

By the end of the exercise, you should be able to:

1. Explain the difference between always-on instructions, prompt files, skills, and custom agents.
2. Use a read-only planning phase before a multi-file implementation.
3. Apply least privilege to planner, implementer, and reviewer roles.
4. Use a skill with progressively loaded instructions, references, and a helper script.
5. Perform a visible human checkpoint between planning and implementation.
6. Separate implementation from independent conformance review.
7. Identify which parts of an agent workflow are portable and which are tool-specific.

## What you are given

Your generated student workspace contains:

- `docs/spec/sell-stocks-spec.md` — authoritative behaviour and AC-01 through AC-24;
- `docs/spec/domain-class-diagram.puml` — authoritative structure;
- `AGENTS.md` — shared project rules;
- `.github/` — GitHub Copilot instructions, prompt files, and custom agents;
- `.agents/skills/implement-sell-stocks/` — the canonical open-standard skill;
- `plan.md` — the durable planning and approval checkpoint; and
- `evidence/session-notes.md` — the observation log you will submit.

No implementation, build file, test code, Codex configuration, or Claude configuration is provided
in the student workspace.

## Prepare a student workspace

From the training repository, choose an empty folder outside the kata module:

```bash
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh \
  /path/to/your/sell-stocks-exercise-3
cd /path/to/your/sell-stocks-exercise-3
git init
git add .
git commit -m "chore: initialise skills and agents exercise"
```

Open that generated folder as the workspace root. Opening only one file prevents the tools from
discovering project customisations reliably.

## Primary path — GitHub Copilot in VS Code

### 0. Preflight

1. Use the latest stable VS Code and GitHub Copilot extensions allowed by your organisation.
2. Sign in and confirm that your Copilot licence is active.
3. Trust the generated workspace.
4. Confirm Java 21, Maven, and Git are available.
5. Open Copilot Chat and inspect **Chat: Open Customizations** or Chat diagnostics.
6. Confirm that Copilot sees the repository instructions, three agents, three prompt files, and
   `implement-sell-stocks` skill.
7. Select supervised permissions. Do not enable unrestricted terminal access.

If organisation policy hides a feature, record that fact and use the fallback in
[`compatibility.md`](compatibility.md). Do not spend the lab bypassing enterprise controls.

### 1. Explore and plan — no code changes

Select **Specification Planner** from the agent picker or run:

```text
/create-plan
```

The planner must read both specifications and return complete proposed contents for `plan.md`.
Copy that proposal into `plan.md`, then refine it yourself. Check that the plan:

- names concrete files or types;
- maps AC-01 through AC-24 to test evidence;
- preserves domain-only scope;
- includes state verification and the exact 36-test completion condition;
- records conflicts or says explicitly that none were found; and
- contains no production-code edits.

Refine at least one plan item yourself. Planning is a review activity, not a button to click past.

When satisfied, update the Human checkpoint in `plan.md` to `approved`, add your name and the
date/time, and commit the plan.

### 2. Implement the approved plan

Use the planner's **Start approved implementation** handoff or run:

```text
/implement-approved-plan
```

Monitor the Domain Implementer. Pause at these checkpoints:

1. Maven structure and dependency choice;
2. value objects and their validation;
3. entity relationships and FIFO mutation;
4. acceptance-test traceability; and
5. final full-suite verification.

Review diffs rather than approving all edits automatically. If the implementation departs from
the plan, make the agent record the deviation in `plan.md` before continuing.

### 3. Independent conformance review

Use the implementer's **Request independent review** handoff or run:

```text
/verify-conformance
```

The Conformance Reviewer may read and run verification commands but must not edit. Require it to:

- report findings before fixes;
- cite acceptance criteria and file locations;
- distinguish actual defects from optional suggestions;
- compare structure with the class diagram; and
- inspect state after both successful and rejected operations.

Choose which findings to accept. Only then hand selected findings back to the Domain Implementer.

### 4. Close the loop

Run these yourself:

```bash
mvn test
.agents/skills/implement-sell-stocks/scripts/verify-workspace.sh
git diff --check
git status --short
```

Complete `evidence/session-notes.md` and review the entire diff.

## Instructor-only Codex and Claude support

Students do not use Codex or Claude during this exercise. The repository retains adapters for the
instructor to rehearse and validate the workflow without changing the student tool policy. Those
adapters are included only when the instructor generates a portability workspace with:

```bash
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh \
  --include-instructor-adapters /path/to/instructor-workspace
```

The instructor follows [`compatibility.md`](compatibility.md). An instructor replay is not part of
the student lab and does not replace the required Copilot preflight before course delivery.

## Deliverables

Submit:

1. the complete generated workspace with code and tests;
2. the approved and completed `plan.md`;
3. `evidence/session-notes.md`;
4. the final test and verification output;
5. the final diff or commit history; and
6. a short comparison with Exercise 2.

Do not submit secrets, access tokens, private customer data, or proprietary prompts copied from
outside the approved training materials.

## Done means

- `mvn test` reports exactly 36 tests, all passing;
- every AC from AC-01 through AC-24 is traceable in tests;
- rejected operations leave state untouched where specified;
- the class structure matches the diagram;
- no infrastructure or extra domain concepts were invented;
- the bundled verification script passes;
- an independent reviewer completed a no-edit review; and
- the human, not an agent, accepted the final result.

## Debrief questions

1. What knowledge moved out of the prompt and into versioned context?
2. Which skill instruction changed the agent's behaviour materially?
3. What did the planner catch before implementation?
4. Did the reviewer find anything the implementer and its tests missed?
5. Which agent boundary was genuine, and which was only a persona change?
6. What would you keep for a production repository? What would you simplify?
7. Which artefacts are Copilot-specific, and which appear portable in principle?

The goal is not to prove that more configuration is always better. It is to learn which reusable
context earns its maintenance cost.
