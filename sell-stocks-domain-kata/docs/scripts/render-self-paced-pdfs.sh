#!/usr/bin/env bash
# Render every self-paced PDF (and rebuild the distribution zip) from their
# Markdown sources, with a consistent, reviewed visual design.
#
# First run installs a local virtualenv (docs/.venv-pdf, gitignored) with
# WeasyPrint — takes ~15-20s once, instant after that.
#
# Usage:
#   docs/scripts/render-self-paced-pdfs.sh                  # render everything, rebuild the zip
#   docs/scripts/render-self-paced-pdfs.sh --no-zip          # render everything, skip the zip
#   docs/scripts/render-self-paced-pdfs.sh exercise-1 spec   # render only these documents
#
# Requires: python3, pandoc (brew install pandoc if missing).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VENV="$DOCS_DIR/.venv-pdf"

if ! command -v pandoc >/dev/null 2>&1; then
  echo "pandoc is required (brew install pandoc) and was not found on PATH." >&2
  exit 1
fi

if [[ ! -x "$VENV/bin/python" ]]; then
  echo "Setting up $VENV (first run only) ..."
  python3 -m venv "$VENV"
  "$VENV/bin/pip" install --quiet --upgrade pip
  "$VENV/bin/pip" install --quiet weasyprint
fi

exec "$VENV/bin/python" "$SCRIPT_DIR/render_pdfs.py" "$@"
