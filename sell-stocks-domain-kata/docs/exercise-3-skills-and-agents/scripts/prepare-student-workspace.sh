#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXERCISE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MODULE_DIR="$(cd "$EXERCISE_DIR/../.." && pwd)"
TEMPLATE_DIR="$EXERCISE_DIR/workspace-template"
SPEC_DIR="$MODULE_DIR/docs/exercise-2-specification-driven/spec"
INCLUDE_INSTRUCTOR_ADAPTERS=false

if [[ $# -eq 2 ]] && [[ "$1" == "--include-instructor-adapters" ]]; then
  INCLUDE_INSTRUCTOR_ADAPTERS=true
  shift
elif [[ $# -ne 1 ]]; then
  printf 'Usage: %s [--include-instructor-adapters] <empty-target-directory>\n' "$0" >&2
  exit 2
fi

TARGET="$1"

if [[ -e "$TARGET" ]] && [[ ! -d "$TARGET" ]]; then
  printf 'Refusing to replace a non-directory target: %s\n' "$TARGET" >&2
  exit 1
fi

if [[ -e "$TARGET" ]] && [[ -n "$(find "$TARGET" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
  printf 'Refusing to overwrite non-empty target: %s\n' "$TARGET" >&2
  exit 1
fi

mkdir -p "$TARGET"
cp "$TEMPLATE_DIR/AGENTS.md" "$TEMPLATE_DIR/plan.md" "$TEMPLATE_DIR/.gitignore" "$TARGET"/
cp -R "$TEMPLATE_DIR/.github" "$TARGET/"
cp -R "$TEMPLATE_DIR/evidence" "$TARGET/"

SKILL_TARGET="$TARGET/.agents/skills/implement-sell-stocks"
mkdir -p "$SKILL_TARGET"
cp "$TEMPLATE_DIR/.agents/skills/implement-sell-stocks/SKILL.md" "$SKILL_TARGET/"
cp -R "$TEMPLATE_DIR/.agents/skills/implement-sell-stocks/references" "$SKILL_TARGET/"
cp -R "$TEMPLATE_DIR/.agents/skills/implement-sell-stocks/scripts" "$SKILL_TARGET/"

if [[ "$INCLUDE_INSTRUCTOR_ADAPTERS" == true ]]; then
  cp "$TEMPLATE_DIR/CLAUDE.md" "$TARGET/"
  cp -R "$TEMPLATE_DIR/.claude" "$TEMPLATE_DIR/.codex" "$TARGET/"
  mkdir -p "$SKILL_TARGET/agents"
  cp "$TEMPLATE_DIR/.agents/skills/implement-sell-stocks/agents/openai.yaml" \
    "$SKILL_TARGET/agents/"
fi

mkdir -p "$TARGET/docs/spec"
cp "$SPEC_DIR/sell-stocks-spec.md" "$TARGET/docs/spec/"
cp "$SPEC_DIR/domain-class-diagram.puml" "$TARGET/docs/spec/"

printf 'Student workspace prepared at %s\n' "$TARGET"
if [[ "$INCLUDE_INSTRUCTOR_ADAPTERS" == true ]]; then
  printf 'Mode: instructor portability workspace (Copilot, Codex, and Claude adapters included)\n'
  printf '\nNext steps:\n'
  printf '  1. cd %s\n' "$TARGET"
  printf '  2. git init && git add . && git commit -m "chore: initialise exercise 3"\n'
  printf '  3. Start Codex or Claude from this workspace root\n'
  printf '  4. Follow the Plan, Implement, and Review prompts in compatibility.md\n'
  printf '\nThis rehearsal validates the portable workflow, not the Copilot integration.\n'
else
  printf 'Mode: student workspace (GitHub Copilot only)\n'
  printf '\nNext steps:\n'
  printf '  1. cd %s\n' "$TARGET"
  printf '  2. git init && git add . && git commit -m "chore: initialise exercise 3"\n'
  printf '  3. code .\n'
  printf '  4. In Copilot Chat, run /create-plan\n'
  printf '  5. Review and approve plan.md\n'
  printf '  6. In Copilot Chat, run /implement-approved-plan\n'
  printf '  7. In Copilot Chat, run /verify-conformance\n'
  printf '\nThe Java code is generated in step 6, not by this script.\n'
fi
