# GitHub Copilot repository instructions

- Treat `AGENTS.md` as the project operating agreement and follow it for every task.
- Treat `docs/spec/sell-stocks-spec.md` as authoritative for behaviour.
- Treat `docs/spec/domain-class-diagram.puml` as authoritative for structure.
- Use the `implement-sell-stocks` skill from `.agents/skills/` for this kata.
- Keep Explore, Plan, Implement, and Review as separate, visible phases.
- Do not edit code until `plan.md` has explicit human approval.
- Use Java 21, standalone Maven, and JUnit 5 as the only test-scoped dependency.
- Keep the project domain-only and use `BigDecimal` for all money.
- Run the full test suite before completion. Run the bundled verifier when Bash is available;
  otherwise use its documented review-checklist fallback.
- Do not add MCP servers or access data outside this workspace.
