# Traceability to the six course decks

The six source decks in the instructor's course materials are design constraints for Exercise 3.
This map lets an instructor show where each concept becomes observable practice.

## 02 — GitHub Copilot for Developers

| Slide concept | Exercise evidence |
| --- | --- |
| Copilot surfaces and mode choice | Student guide separates planning, agent execution, and review |
| Workspace setup and trust | Preflight requires opening the generated folder as workspace root |
| Custom instructions and prompt files | `.github/copilot-instructions.md`, `.github/instructions/`, `.github/prompts/` |
| Tool results feed the loop | Maven and verifier output return to the implementer |
| Safety and review | Supervised permissions, checkpoints, full diff review |

## 05 — Context Engineering

| Slide concept | Exercise evidence |
| --- | --- |
| Context is instructions, files, tools, and history | Specifications, instructions, skill, agents, plan, evidence log |
| Finite context and signal/noise | Skill uses progressive disclosure and does not duplicate specifications |
| Always-on instructions | `AGENTS.md` and `.github/copilot-instructions.md` |
| Path-specific instructions | Domain and test `.instructions.md` files |
| Skills with scripts and references | Canonical `implement-sell-stocks` skill |
| Choose the smallest layer | Student and compatibility guides explain ownership boundaries |

## 06 — Planning Mode

| Slide concept | Exercise evidence |
| --- | --- |
| Explore → Plan → Implement → Review | Required workflow in `AGENTS.md` and `SKILL.md` |
| No code during planning | Planner has read/search only; learner records proposal |
| Refine the plan | Student must change at least one plan item |
| Acceptance criteria in the plan | AC-01 through AC-24 matrix in `plan.md` |
| Todo lists and checkpoints | Ordered tasks, checkboxes, and Human checkpoint |
| Version plans | `plan.md` is committed before implementation |

## 07 — Agent Mode

| Slide concept | Exercise evidence |
| --- | --- |
| Multi-step edits and self-correction | Domain Implementer can read, edit, execute, and rerun tests |
| Configure model and permissions | Model remains policy-selected; permissions are supervised |
| Review output safely | Diff checkpoints and separate no-edit reviewer |
| Clean branch | Generated workspace is committed before work |
| Least external access | No browser or MCP is required |
| Full test suite | Manual and scripted final verification |

## 08 — Agent Skills and Custom Agents

| Slide concept | Exercise evidence |
| --- | --- |
| Skill folder with `SKILL.md` | `.agents/skills/implement-sell-stocks/` |
| Supporting scripts and references | Verifier and independent-review checklist |
| Specific trigger description | Skill frontmatter names implementation, AC traceability, review, and repair |
| Focused custom agents | Planner, implementer, reviewer |
| Least privilege | Different tool lists and sandboxes per role |
| Handoffs | Copilot guided buttons; durable file handoffs elsewhere |
| Test the skill | Template check, skill validator, pilot run, verifier |

## 10 — Prompting for Enterprise

| Slide concept | Exercise evidence |
| --- | --- |
| Role, context, task, output contract | Agent role files and prompt files split these concerns explicitly |
| Acceptance criteria and constraints | Specifications plus plan traceability and completion contract |
| Shared prompt templates | Three `.prompt.md` files under version control |
| Governance and consistency | Instructor preflight, fixed inputs, evidence log, assessment rubric |
| Coding standards as context | Shared and path-specific instructions |
| Iterative refinement | Plan review, implementation checkpoints, independent review, selected fixes |

## Deliberate exclusions

- **MCP:** no external system is needed, so adding a server would violate least privilege.
- **Pinned model:** enterprise availability changes; the exercise records model policy instead.
- **Automatic reviewer fixes:** they would erase the distinction between review and implementation.
- **One universal agent manifest:** Copilot, Codex, and Claude currently use different formats and
  permission semantics.

These exclusions are discussion material, not missing work.
