#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXERCISE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TEMPLATE="$EXERCISE_DIR/workspace-template"
FAILURES=0
CHECK_DIR="$(mktemp -d)"
trap 'rm -rf "$CHECK_DIR"' EXIT

check() {
  if [[ -f "$TEMPLATE/$1" ]]; then
    printf 'PASS  %s\n' "$1"
  else
    printf 'FAIL  %s\n' "$1" >&2
    FAILURES=$((FAILURES + 1))
  fi
}

check "AGENTS.md"
check "CLAUDE.md"
check "plan.md"
check ".github/copilot-instructions.md"
check ".github/instructions/domain.instructions.md"
check ".github/instructions/tests.instructions.md"
check ".github/prompts/create-plan.prompt.md"
check ".github/prompts/implement-approved-plan.prompt.md"
check ".github/prompts/verify-conformance.prompt.md"
check ".github/agents/specification-planner.agent.md"
check ".github/agents/domain-implementer.agent.md"
check ".github/agents/conformance-reviewer.agent.md"
check ".agents/skills/implement-sell-stocks/SKILL.md"
check ".agents/skills/implement-sell-stocks/agents/openai.yaml"
check ".agents/skills/implement-sell-stocks/references/review-checklist.md"
check ".agents/skills/implement-sell-stocks/scripts/verify-workspace.sh"
check ".claude/skills/implement-sell-stocks/SKILL.md"
check ".claude/agents/specification-planner.md"
check ".claude/agents/domain-implementer.md"
check ".claude/agents/conformance-reviewer.md"
check ".codex/agents/specification-planner.toml"
check ".codex/agents/domain-implementer.toml"
check ".codex/agents/conformance-reviewer.toml"

if grep -R -n '\[TODO' "$TEMPLATE" --exclude-dir=.git; then
  printf 'FAIL  unresolved skill or template TODO found\n' >&2
  FAILURES=$((FAILURES + 1))
else
  printf 'PASS  no unresolved scaffold TODOs\n'
fi

"$SCRIPT_DIR/prepare-student-workspace.sh" "$CHECK_DIR/student" >/dev/null
if [[ -d "$CHECK_DIR/student/.github" ]] \
  && [[ -d "$CHECK_DIR/student/.agents/skills/implement-sell-stocks" ]] \
  && [[ ! -e "$CHECK_DIR/student/.codex" ]] \
  && [[ ! -e "$CHECK_DIR/student/.claude" ]] \
  && [[ ! -e "$CHECK_DIR/student/CLAUDE.md" ]] \
  && [[ ! -e "$CHECK_DIR/student/.agents/skills/implement-sell-stocks/agents/openai.yaml" ]]; then
  printf 'PASS  default workspace is Copilot-only\n'
else
  printf 'FAIL  default workspace leaked instructor adapters or missed Copilot files\n' >&2
  FAILURES=$((FAILURES + 1))
fi

"$SCRIPT_DIR/prepare-student-workspace.sh" --include-instructor-adapters \
  "$CHECK_DIR/instructor" >/dev/null
if [[ -d "$CHECK_DIR/instructor/.github" ]] \
  && [[ -d "$CHECK_DIR/instructor/.codex" ]] \
  && [[ -d "$CHECK_DIR/instructor/.claude" ]] \
  && [[ -f "$CHECK_DIR/instructor/CLAUDE.md" ]] \
  && [[ -f "$CHECK_DIR/instructor/.agents/skills/implement-sell-stocks/agents/openai.yaml" ]]; then
  printf 'PASS  instructor workspace includes portability adapters\n'
else
  printf 'FAIL  instructor workspace is missing portability adapters\n' >&2
  FAILURES=$((FAILURES + 1))
fi

if [[ "$FAILURES" -gt 0 ]]; then
  printf '%s template check(s) failed.\n' "$FAILURES" >&2
  exit 1
fi

printf 'Workspace template checks passed.\n'
