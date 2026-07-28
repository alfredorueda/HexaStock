# Instructor guide — Copilot, Codex, and Claude Code compatibility

> **Instructor-only material.** Students use GitHub Copilot in VS Code exclusively. Codex and
> Claude support exists so the instructor can rehearse and validate the exercise without Copilot;
> it is not a student choice or a fallback during the timed lab.

Generate a workspace containing all instructor adapters with:

```bash
./docs/exercise-3-skills-and-agents/scripts/prepare-student-workspace.sh \
  --include-instructor-adapters /path/to/instructor-workspace
```

The domain inputs and most context-engineering artefacts are portable. Agent manifests, tool
identifiers, permissions, prompt commands, and handoffs are product-specific.

This guide records the differences instead of hiding them behind a false common denominator.

## Compatibility summary

| Concern | GitHub Copilot in VS Code | Codex | Claude Code |
| --- | --- | --- | --- |
| Shared project rules | `.github/copilot-instructions.md`, path instructions, and `AGENTS.md` | `AGENTS.md` | `CLAUDE.md` imports `AGENTS.md` |
| Canonical skill | `.agents/skills/implement-sell-stocks/` | `.agents/skills/implement-sell-stocks/` | Thin adapter in `.claude/skills/` loads the canonical skill |
| Planner/implementer/reviewer | `.github/agents/*.agent.md` | `.codex/agents/*.toml` | `.claude/agents/*.md` |
| Reusable prompt commands | `.github/prompts/*.prompt.md` | Invoke the skill or agent in the prompt | Invoke the skill or subagent in the prompt |
| Guided handoff buttons | Yes, in supported VS Code versions | No shared button format; parent agent coordinates | No Copilot handoff format; main conversation coordinates |
| Plan protection | Planner has only read/search tools | Planner sandbox is read-only | Planner uses plan permission mode and read tools |
| Implementation permissions | Read/search/edit/execute, with supervised approvals | Workspace-write sandbox with normal approvals | Read/edit/write/Bash with normal approvals |
| Review protection | No edit tool; execute is allowed for tests | Read-only sandbox; parent reruns mutating build commands | No Edit/Write tools; Bash limited by instructions |
| MCP | Deliberately absent | Deliberately absent | Deliberately absent |

## Deliberate updates to literal slide configuration

The learning concepts in the decks are preserved, but a few literal examples are adapted to the
current tools and the portability goal:

- The decks show repository skills under `.github/skills`. Current Copilot also supports the open
  `.agents/skills` project location, and Codex discovers that location natively. The exercise uses
  `.agents/skills` as the single canonical copy and verifies discovery during preflight.
- One deck describes custom agents through chat-participant configuration or the VS Code agent API.
  Current VS Code supports checked-in `.github/agents/*.agent.md` files, which are easier to review
  and distribute in a workshop.
- Model names in the decks are examples, not requirements. The exercise leaves `model` unset so
  enterprise policy and the current model picker decide what is available.
- The MCP example is not instantiated. The task needs no external system, and least privilege is
  one of the stated learning objectives.
- Copilot handoffs remain in the Copilot adapter. Codex and Claude preserve the same gates through
  explicit parent-agent coordination and durable files because they do not share that manifest.

If a managed Copilot version does not discover `.agents/skills`, use the documented fallback:
copy the canonical skill folder to `.github/skills/implement-sell-stocks` for that delivery and add
a preflight drift check. Do not maintain two hand-edited copies.

## The portable core

### `AGENTS.md`

`AGENTS.md` is the shared operating agreement: authoritative inputs, workflow gates, technical
constraints, verification, and safety. Codex loads it natively. Claude imports it from
`CLAUDE.md`. Current VS Code versions can also recognise it, while the Copilot-specific
instructions repeat only the critical baseline for course consistency.

Do not put the complete workflow into every tool adapter. Update `AGENTS.md` first when a shared
rule changes.

### The open-standard skill

The canonical skill is:

```text
.agents/skills/implement-sell-stocks/
├── SKILL.md
├── agents/openai.yaml
├── references/review-checklist.md
└── scripts/verify-workspace.sh
```

GitHub Copilot and Codex discover `.agents/skills`. Claude Code normally discovers project skills
under `.claude/skills`, so its tiny adapter tells Claude to load the canonical file. All paths
from both locations have the same workspace depth, and scripts remain single-source.

The `agents/openai.yaml` file is optional Codex/ChatGPT UI metadata. Other tools ignore it safely.

### Durable handoff artefacts

`plan.md`, test output, review findings, and `evidence/session-notes.md` are more portable than a
conversation handoff. Every tool uses those files even though the UI transition differs.

## GitHub Copilot path

1. Open the generated student folder as the VS Code workspace root.
2. Use **Chat: Open Customizations** or Chat diagnostics to confirm loaded artefacts.
3. Select **Specification Planner** or run `/create-plan`.
4. Copy its read-only proposal into `plan.md`, refine it, and record approval.
5. Use the handoff button or `/implement-approved-plan`.
6. Keep permissions supervised and review each diff/checkpoint.
7. Use the handoff button or `/verify-conformance`.
8. Accept findings yourself before returning any to the implementer.

The agent files do not pin a model. Use Auto or a model approved and available under the
organisation's Copilot policy. This avoids turning a transient model catalogue into a repository
requirement.

Copilot prompt files and handoff buttons are convenience layers. The skill, plan, and verification
remain the source of the workflow.

## Codex path

Start Codex from the generated workspace root so it discovers `AGENTS.md`, `.agents/skills`, and
`.codex/agents`.

### Plan

```text
Use the specification_planner custom agent and the $implement-sell-stocks skill.
Return complete proposed contents for plan.md. Do not edit any file.
```

The parent or learner records the returned plan, refines it, and adds explicit approval.

### Implement

```text
Use the domain_implementer custom agent and $implement-sell-stocks to execute the approved
plan.md. Wait for it to finish, then summarize its changes and verification evidence.
```

### Review

```text
Use the conformance_reviewer custom agent for an independent read-only review against both
specifications and plan.md. Report findings before proposing fixes.
```

The Codex reviewer is truly read-only, so it may inspect existing Surefire output but cannot run a
fresh Maven build that writes `target/`. After receiving the review, the parent session or learner
runs the verifier. That separation is intentional and should be discussed in the debrief.

Codex project guidance and skill discovery are documented in the current Codex manual under
[AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md.md),
[skills](https://learn.chatgpt.com/docs/build-skills), and
[subagents](https://learn.chatgpt.com/docs/agent-configuration/subagents.md).

## Claude Code path

Start Claude Code from the generated workspace root. `CLAUDE.md` imports the shared agreement,
`.claude/skills` exposes the adapter, and `.claude/agents` provides the three subagents.

### Plan

Use `/agents` to inspect the project agents, then ask:

```text
Use the specification-planner subagent and the implement-sell-stocks skill. Return a complete
proposal for plan.md without editing files.
```

Record, refine, and approve the plan yourself.

### Implement

```text
Use the domain-implementer subagent to execute the approved plan with the
implement-sell-stocks skill. Return its verification evidence to this conversation.
```

### Review

```text
Use the conformance-reviewer subagent for an independent no-edit review. Report findings before
any fixes.
```

Claude subagents begin with isolated context. The checked-in skill and durable files prevent the
handoff from depending on hidden chat history. Claude may automatically delegate based on agent
descriptions, but explicit invocation makes the classroom run easier to observe.

Anthropic documents project instructions in
[`CLAUDE.md`](https://code.claude.com/docs/en/memory), open-standard
[skills](https://code.claude.com/docs/en/slash-commands), and project
[subagents](https://code.claude.com/docs/en/sub-agents).

## What is not portable

### Prompt files

`.github/prompts/*.prompt.md` and their slash-command UX are Copilot-specific. Codex now favours
skills for shared reusable workflows. Claude also exposes skills as commands. The prompt files are
there to match the Copilot course material, not to become a second source of truth.

### Agent definitions

Copilot agents use Markdown frontmatter, VS Code tool sets, and optional handoffs. Codex project
agents are TOML configuration layers. Claude subagents are Markdown with Claude tool names. The
three sets express the same responsibilities but cannot be byte-for-byte identical.

### Handoffs

VS Code handoffs provide suggested buttons and carry conversation context. Codex and Claude use a
parent conversation to coordinate custom agents/subagents. `plan.md` and review findings are the
cross-tool handoff contract.

### Permission semantics

"Read-only", "supervised", and "plan" are not identical implementations. Shell access can often
write even when an agent lacks an editor tool. Always combine tool restrictions with human
approval and diff review.

## Why there is no MCP configuration

MCP is valuable when an agent needs live data or actions from GitHub, a database, a browser, or an
internal system. This kata needs only local files and Maven. Adding `.vscode/mcp.json`, a Codex MCP
server, or `.mcp.json` for Claude would expand permissions without adding learning value.

The absence of MCP is a least-privilege decision, not an unfinished configuration.

## Fair-comparison rule

For the timed classroom comparison with Exercise 2:

- require GitHub Copilot in VS Code for every student pair;
- use fresh generated workspaces;
- do not switch tools after seeing a result;
- record tool/version/permission information;
- preserve plan and review evidence; and
- compare outcomes against specifications, not against model reputation.

Codex and Claude replays are instructor preparation activities. They help validate the portable
core but do not prove that Copilot-specific discovery, permissions, prompts, or handoffs work.

## Maintenance rule

Agent products evolve quickly. Before delivering the course:

1. use current stable tool versions approved by the organisation;
2. run each product's customisation diagnostics or discovery command;
3. verify skill and agent names appear;
4. confirm tool permissions behave as documented;
5. update only the relevant adapter when a product format changes; and
6. leave the canonical skill and domain specifications unchanged unless the workflow itself
   changes.
