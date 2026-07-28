#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXERCISE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MODULE_DIR="$(cd "$EXERCISE_DIR/../.." && pwd)"
TEMPLATE_DIR="$EXERCISE_DIR/workspace-template"
SPEC_DIR="$MODULE_DIR/docs/exercise-2-specification-driven/spec"

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <empty-target-directory>\n' "$0" >&2
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
cp -R "$TEMPLATE_DIR"/. "$TARGET"/
mkdir -p "$TARGET/docs/spec"
cp "$SPEC_DIR/sell-stocks-spec.md" "$TARGET/docs/spec/"
cp "$SPEC_DIR/domain-class-diagram.puml" "$TARGET/docs/spec/"

printf 'Student workspace prepared at %s\n' "$TARGET"
printf '\nNext steps:\n'
printf '  1. cd %s\n' "$TARGET"
printf '  2. git init && git add . && git commit -m "chore: initialise exercise 3"\n'
printf '  3. code .\n'
printf '  4. In Copilot Chat, run /create-plan\n'
printf '  5. Review and approve plan.md\n'
printf '  6. In Copilot Chat, run /implement-approved-plan\n'
printf '  7. In Copilot Chat, run /verify-conformance\n'
printf '\nThe Java code is generated in step 6, not by this script.\n'
