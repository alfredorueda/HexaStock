@AGENTS.md

# Claude Code adapter

- A thin `implement-sell-stocks` adapter under `.claude/skills/` points to the canonical skill for
  Claude Code discovery without duplicating the workflow.
- Project subagents are under `.claude/agents/`.
- Run the workflow sequentially: specification-planner, then human approval, then
  domain-implementer, then conformance-reviewer.
- Claude Code does not use the Copilot handoff buttons. Invoke the next subagent explicitly and
  carry forward `plan.md` or the review findings as the durable handoff artifact.
