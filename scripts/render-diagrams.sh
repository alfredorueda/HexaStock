#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# render-diagrams.sh — Render all PlantUML diagrams to SVG and PNG using Docker.
#
# Usage:   ./scripts/render-diagrams.sh          (run from repo root)
#
# For each *.puml / *.plantuml file found under the repository:
#   • Creates a sibling  Rendered/  directory (if missing).
#   • Renders SVG and PNG with the same base name into that directory.
#   • Overwrites any existing outputs.
#
# Exits non-zero if any diagram fails to render.
# ---------------------------------------------------------------------------
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────
PLANTUML_IMAGE="plantuml/plantuml:latest"   # includes Graphviz
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# ── Counters ──────────────────────────────────────────────────────────────
TOTAL=0
SVG_OK=0
PNG_OK=0
FAIL=0
FAILED_FILES=""

while IFS= read -r src; do
  TOTAL=$((TOTAL + 1))

  src_dir="$(dirname "$src")"
  src_name="$(basename "$src")"
  base_name="${src_name%.*}"
  rendered_dir="${src_dir}/Rendered"

  mkdir -p "$rendered_dir"

  # Paths relative to REPO_ROOT (for display)
  rel_src="${src#"$REPO_ROOT"/}"
  rel_dir="${rendered_dir#"$REPO_ROOT"/}"

  echo "  ▸ Rendering: $rel_src"

  diagram_failed=false

  # ── SVG ──────────────────────────────────────────────────────────────
  echo "           → ${rel_dir}/${base_name}.svg"
  if docker run --rm \
       -v "${src_dir}:/data" \
       "$PLANTUML_IMAGE" \
       -tsvg \
       -o "/data/Rendered" \
       "/data/${src_name}" \
    ; then
    SVG_OK=$((SVG_OK + 1))
    echo "    ✅  SVG OK"
  else
    diagram_failed=true
    echo "    ❌  SVG FAILED"
  fi

  # ── PNG ──────────────────────────────────────────────────────────────
  echo "           → ${rel_dir}/${base_name}.png"
  if docker run --rm \
       -v "${src_dir}:/data" \
       "$PLANTUML_IMAGE" \
       -tpng \
       -o "/data/Rendered" \
       "/data/${src_name}" \
    ; then
    PNG_OK=$((PNG_OK + 1))
    echo "    ✅  PNG OK"
  else
    diagram_failed=true
    echo "    ❌  PNG FAILED"
  fi

  if [ "$diagram_failed" = true ]; then
    FAIL=$((FAIL + 1))
    FAILED_FILES="${FAILED_FILES}    • ${rel_src}"$'\n'
  fi
  echo ""
done < <(find "$REPO_ROOT" -type f \( -name '*.puml' -o -name '*.plantuml' \) | sort)

if [ "$TOTAL" -eq 0 ]; then
  echo "⚠  No PlantUML files found — nothing to render."
  exit 0
fi

# ── Summary ───────────────────────────────────────────────────────────────
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  PlantUML Render Summary"
echo "  Diagrams: $TOTAL  |  SVG: $SVG_OK ✅  |  PNG: $PNG_OK ✅  |  Failed: $FAIL ❌"
if [ "$FAIL" -gt 0 ]; then
  echo ""
  echo "  Failed diagrams:"
  printf "%s" "$FAILED_FILES"
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
