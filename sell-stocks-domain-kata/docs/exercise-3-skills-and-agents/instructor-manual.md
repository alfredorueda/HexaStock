# Instructor manual — Exercise 3

> Students regenerate the Exercise 2 domain from the same specifications, but the working method
> is now encoded as skills, specialised agents, permissions, checkpoints, and durable evidence.

> **Delivery boundary:** students use GitHub Copilot in VS Code only. Codex and Claude adapters
> are instructor tools for rehearsal while Copilot is unavailable and for optional portability
> checks. Do not distribute those adapters as alternative student workflows.

**Recommended duration: 120 minutes.** Run Exercise 2 first. Exercise 3 is a controlled comparison,
not an introduction to the domain.

> Running the tool-agnostic self-paced version instead of the live Copilot session? See
> [`exercise-3-self-paced-instructor-notes.md`](exercise-3-self-paced-instructor-notes.md) —
> a separate, short set of notes for that document; this manual is about the live classroom flow.

## Teaching intent

Exercise 1 showed that a vague prompt produces plausible divergence. Exercise 2 moved behaviour
and structure into versioned specifications. Exercise 3 asks the next question:

> Once the requirements are durable, can the team's way of using an agent become durable too?

The lesson is not "more files make AI better." The lesson is to assign each kind of context to the
smallest appropriate layer:

- universal project facts in instructions;
- a repeatable multi-step method in a skill;
- a focused responsibility and tool boundary in an agent;
- a single reusable invocation in a prompt file;
- approval and progress in a plan; and
- mechanical evidence in a script.

Students should also discover the maintenance cost. Some configuration is portable, some is an
adapter, and some is not worth adding at all.

## Learning objectives

At the end, students should be able to:

1. select instructions, prompt files, skills, or agents for a concrete need;
2. explain progressive disclosure and context budget in their own words;
3. review and refine an AI-generated plan before implementation;
4. enforce a human checkpoint between read-only planning and writing;
5. explain least privilege using the three exercise roles;
6. distinguish self-correction from independent review;
7. show traceable evidence for AC-01 through AC-24; and
8. identify portable and product-specific configuration.

## Instructor preflight — complete before class

### 1. Licensing and organisation policy

- Activate the GitHub Copilot licence for the instructor account.
- Confirm student accounts have the required Copilot entitlement.
- Confirm organisation policy enables Copilot Chat, agent mode, workspace customisations, skills,
  and custom agents as needed.
- Confirm whether model choice is managed centrally. Use Auto or an approved model; do not require
  a model unavailable to learners.
- Confirm terminal approvals and workspace trust behave as expected on managed machines.
- Record any enterprise feature disabled by policy and choose the documented fallback before the
  session.

Do not ask students to bypass Citibank controls. A policy-limited fallback is itself a useful
enterprise lesson.

### 2. Local toolchain

Verify:

```bash
java -version
mvn -version
git --version
```

Use Java 21. Ensure Maven can resolve JUnit before the class if the training network uses a proxy
or an internal artefact mirror.

### 3. Validate the training module

From `sell-stocks-domain-kata`:

```bash
./docs/exercise-3-skills-and-agents/scripts/check-workspace-template.sh
(cd docs/exercise-2-specification-driven/reference-solution && mvn test)
(cd docs/exercise-1-prompt-only/flawed-reference-solution && mvn test)
```

The canonical skill is also validated during authoring with the available Agent Skills validator;
that validator is not an instructor or student prerequisite. The expected module results are 36
passing reference tests and 8 passing tests in the deliberately flawed Exercise 1 solution.

### 4. Generate a clean pilot workspace

```bash
PILOT_DIR="$(mktemp -d)"
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh "$PILOT_DIR/workspace"
```

Open `$PILOT_DIR/workspace` as the VS Code workspace root. Use Chat customisation diagnostics and
confirm:

- repository instructions are loaded;
- three prompt files are available;
- Specification Planner, Domain Implementer, and Conformance Reviewer appear;
- `implement-sell-stocks` is discoverable; and
- there is no MCP server supplied by the exercise.

Run at least the planning phase before class. Run a complete pilot whenever the Copilot extension
or organisation policy changes.

If the instructor Copilot licence is not active yet, an instructor-only rehearsal can be generated
with:

```bash
PILOT_DIR="$(mktemp -d)"
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh \
  --include-instructor-adapters "$PILOT_DIR/workspace"
```

Use Codex or Claude according to [`compatibility.md`](compatibility.md). This checks the portable
workflow, specifications, and verification contract. It does **not** validate Copilot discovery,
slash commands, permissions, or handoffs. Complete the Copilot pilot after activating the licence
and before teaching the course.

### 5. Prepare distribution

Prefer one pre-created starter repository per pair, or a repository template that each pair can
clone. Use an archive only after testing that its extracted folder has the expected files and a
working Git baseline. Test the exact distributed artefact on a managed learner machine. Do not ask
students to run the preparation script, and do not give them access to the module root while they
work: it contains the Exercise 2 result.

## What students receive

Release only:

- the generated Exercise 3 workspace;
- the concise [`student-guide.md`](student-guide.md), copied into that workspace as `README.md`.

The default generator intentionally excludes `.codex/`, `.claude/`, `CLAUDE.md`, and Codex UI
metadata. Keep [`compatibility.md`](compatibility.md) with the instructor materials.

The learner's only mandatory terminal command is `mvn test`. The Bash verifier is a convenience
for the reviewer or instructor. If Bash is unavailable, use the skill's review checklist and
record the limitation; do not troubleshoot shells during the lab.

Do not release:

- the completed implementation in the kata root;
- Exercise 1 conformance material as a shortcut;
- this instructor manual during the timed run; or
- another pair's completed plan.

## Recommended 120-minute schedule

| Phase | Time | Student activity | Instructor focus |
| --- | ---: | --- | --- |
| 0. Frame the comparison | 10 min | Review what stays constant from Exercise 2 | Establish the controlled variable |
| 1. Inspect the context layers | 15 min | Locate instructions, skill, agents, prompts, plan, script | Ask why each artefact exists |
| 2. Read-only planning | 20 min | Run planner, copy proposal, refine and approve plan | Enforce no code changes |
| 3. Agent implementation | 35 min | Implement with checkpoints and diff review | Observe permissions and divergence |
| 4. Independent review | 20 min | Reviewer reports findings; learner selects fixes | Prevent reviewer edits |
| 5. Verify and compare | 10 min | Run tests/script; complete evidence log | Collect comparable measures |
| 6. Debrief | 10 min | Discuss value, portability, and maintenance cost | Connect back to the slides |

If time is short, stop after independent review and use the reference implementation only for the
arithmetic demonstration. Do not remove the planning or review checkpoint; they are the exercise.

## Phase-by-phase facilitation

### Phase 0 — frame the experiment

Ask the room:

> What is allowed to change between Exercise 2 and Exercise 3?

Required answer: the AI collaboration workflow. The behaviour specification, class diagram,
language, dependency constraint, package root, and completion criteria do not change.

Write these controlled variables visibly. If a pair changes its spec or copies code, its outcome
cannot be compared.

### Phase 1 — inspect before invoking

Have pairs fill this verbally or on a board:

| Artefact | Loads when | Owns |
| --- | --- | --- |
| `AGENTS.md` / Copilot instructions | Always | Stable project rules |
| Prompt file | Explicit command | One focused invocation |
| `SKILL.md` | Relevant task or explicit use | Reusable workflow |
| Agent file | Selected/delegated role | Persona, responsibility, tools |
| `plan.md` | Human-reviewed checkpoint | Scope, order, approval, progress |
| Verification script | Explicit execution | Deterministic mechanical checks |

Do not explain the table first. Let students classify the files and correct the model together.

Ask why `sell-stocks-spec.md` is not copied into `SKILL.md`. Expected answer: the specification is
already the source of truth; duplication creates drift and wastes context.

### Phase 2 — read-only planning

The planner cannot edit. Students must copy its proposal into `plan.md`, refine it, and explicitly
approve it. This inconvenience is intentional: it makes authorship and approval visible.

Require at least one learner-authored refinement. Good examples include:

- splitting a vague task;
- adding missing state assertions;
- correcting dependency order;
- adding an omitted acceptance criterion; or
- removing invented infrastructure.

Do not accept "the plan looked fine" without evidence that it covers AC-01 through AC-24.

Useful instructor response:

> Show me where the plan proves that rejected operations leave state unchanged.

### Phase 3 — implementation

Students use the Domain Implementer only after approval. Have them pause after each checkpoint and
read the diff.

Observe, but do not immediately correct:

- whether the agent reads the skill or merely claims to;
- whether it adds extra classes absent from the diagram;
- whether it writes tests from the implementation rather than the specification;
- whether it runs focused tests and the full suite;
- whether failed tests lead to narrow corrections or broad rewrites; and
- whether `plan.md` remains truthful.

If a pair grants unrestricted permissions, ask:

> What capability did this task require that supervised mode did not provide?

The aim is a reasoned permission decision, not punishment.

### Phase 4 — independent review

The reviewer must report before fixing. If students let it edit, pause them and restore the role
boundary. Self-repair is useful, but it is not independent review.

Ask students to classify every finding:

- verified defect;
- suspected risk needing evidence;
- optional improvement outside the specification; or
- false positive.

Only learner-accepted defects return to the implementer. This preserves human ownership and keeps
the reviewer from silently expanding scope.

### Phase 5 — verification and evidence

Students run the commands themselves. A passing script is necessary but not sufficient: it checks
file presence, dependency shape, AC markers, prohibited numeric types, Maven success, and test
count. It cannot prove that each test faithfully expresses the criterion or that every class member
matches the diagram. That remains a semantic review.

Collect:

- exact test count;
- plan refinements made by humans;
- number and type of reviewer findings;
- number of agent self-corrections;
- permission mode;
- tool and version; and
- any configuration that failed to load.

## Observation sheet

Use one row per pair:

| Pair | Plan refinement | Scope drift | 36 tests | Reviewer defects | False positives | Permission mode | Config issue |
| --- | --- | --- | ---: | ---: | ---: | --- | --- |
| | | | | | | | |

Then compare with Exercise 2:

| Measure | Exercise 2 | Exercise 3 |
| --- | --- | --- |
| Domain inputs | Two specifications | Same two specifications |
| Prompt/process size | One long implementation prompt | Short invocation plus versioned workflow |
| Planning evidence | Conversation-dependent | `plan.md` with approval |
| Role separation | General assistant | Planner / implementer / reviewer |
| Mechanical verification | Prompt instruction | Skill script |
| Reuse | Copy prompt | Skill and adapters in repository |

Do not claim causality from one classroom run. Treat results as structured observations and
discussion evidence.

## Assessment rubric — 20 points

| Area | Points | Evidence |
| --- | ---: | --- |
| Specification fidelity | 5 | Correct behaviour and structure; no invented scope |
| Acceptance-test traceability | 4 | AC-01 through AC-24 and 36 passing tests |
| Planning quality | 3 | Concrete plan, learner refinement, explicit approval |
| Agent workflow | 3 | Separate roles, controlled handoffs, truthful checkpoints |
| Verification and review | 3 | Script, diff review, independent findings before fixes |
| Reflection and portability | 2 | Evidence log and accurate comparison across tools |

A green build cannot earn full marks without plan, traceability, and review evidence.

## Common failures and interventions

| Symptom | Likely cause | Instructor intervention |
| --- | --- | --- |
| Copilot does not show agents | Wrong workspace root or unsupported/disabled feature | Open generated root; inspect customisation diagnostics; use generic agents with role prompts |
| Skill does not trigger | Description not matched or discovery path unavailable | Invoke `implement-sell-stocks` explicitly and record the failure |
| Planner edits code | Wrong agent selected or tool restriction ignored | Stop, discard only unauthorised edits safely, restart planner read-only |
| Plan is generic | Insufficient spec reading | Require file/type mapping and AC traceability before approval |
| Implementer starts without approval | Handoff clicked without checking plan | Stop and return to the Human checkpoint |
| Reviewer edits | Agent role or permissions too broad | Reject edits; rerun review with edit disabled |
| 36 tests pass but structure drifts | Tests do not enforce diagram completeness | Perform class-diagram comparison manually |
| Maven cannot download JUnit | Proxy or cache issue | Use approved internal mirror or pre-warmed cache; do not add dependencies |
| Model named in slides is unavailable | Model catalogue or enterprise policy changed | Use Auto/approved model and record it; never block learning on a model brand |
| Students want MCP | Confusing extensibility with necessity | Ask which external system the domain task actually needs |

## Tool fallbacks

### Copilot custom agents unavailable

Use the built-in Plan and Agent modes. Paste the corresponding role file into the session prompt,
keep the same permission boundary, and retain `plan.md`. Record the missing feature.

### Prompt files unavailable

Copy the prompt body into Chat. Prompt files improve reuse; they are not a correctness dependency.

### Skill discovery unavailable

Attach or open the canonical `SKILL.md` and explicitly ask the agent to follow it. Do not merge its
contents into the domain specification.

### Codex or Claude replay

Generate a separate workspace with `--include-instructor-adapters` and use
[`compatibility.md`](compatibility.md). Record that the replay is an instructor portability check,
not part of the student exercise and not evidence that Copilot-specific integration works.

## Citibank-oriented safety discussion

Use the exercise to reinforce enterprise habits:

- never place credentials, customer information, card data, or internal tokens in prompts;
- follow organisation policy for model availability, retention, and code suggestions;
- review every generated diff before commit;
- keep permissions least-privileged and time-bounded;
- use version-controlled instructions and workflows so changes are reviewable;
- treat scripts, skills, hooks, and MCP servers as executable supply-chain inputs; and
- distinguish an AI-generated test from independent evidence of business correctness.

The kata contains no external data and requires no MCP server. That makes it a safe place to learn
the boundary before introducing real enterprise systems.

## Debrief guide

Ask in this order:

1. **What improved?** Seek concrete evidence, not preference.
2. **What became more visible?** Plans, permissions, handoffs, findings, and verification.
3. **What became more expensive?** Configuration, adapters, maintenance, and review time.
4. **What was actually portable?** Specifications, `AGENTS.md` intent, skill body, scripts, plan.
5. **What was tool-specific?** Prompt commands, agent manifests, tool names, handoff UI.
6. **What still required a human?** Plan approval, conflict resolution, scope decisions, finding
   acceptance, and final accountability.
7. **Would you keep all of this for every task?** Expected answer: no; use the smallest layer that
   earns its cost.

Close with:

> Specifications make the required behaviour durable. Skills make a repeated method durable.
> Agents make responsibility and capability boundaries visible. None of them removes the need for
> judgement.

## Instructor completion checklist

- [ ] Licence and organisation policies checked
- [ ] Toolchain verified
- [ ] Workspace template and canonical skill validated
- [ ] Fresh student workspaces generated
- [ ] Copilot customisation diagnostics checked
- [ ] Primary model/permission policy announced
- [ ] Exercise 2 results available for comparison
- [ ] Reference implementation withheld
- [ ] Observation sheet prepared
- [ ] Fallback path chosen
- [ ] Debrief questions prepared
