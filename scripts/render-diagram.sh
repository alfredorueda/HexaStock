#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# render-diagram.sh — Render a single PlantUML file to SVG and PNG using Docker.
#
# Usage:   ./scripts/render-diagram.sh <INPUT_FILE> <OUTPUT_DIR>
#
# Example: ./scripts/render-diagram.sh diagrams/domain/model.puml diagrams/output
#
# Generates model.svg and model.png inside the specified output directory.
# Exits non-zero if any rendering step fails.
# ---------------------------------------------------------------------------
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────
PLANTUML_IMAGE="plantuml/plantuml:latest"

# ── Argument validation ───────────────────────────────────────────────────
if [ $# -ne 2 ]; then
  echo "Usage: $0 <INPUT_FILE> <OUTPUT_DIR>"
  echo "  INPUT_FILE  Path to a .puml or .plantuml file"
  echo "  OUTPUT_DIR  Directory where SVG and PNG will be saved"
  exit 1
fi

INPUT_FILE="$1"
OUTPUT_DIR="$2"

if [ ! -f "$INPUT_FILE" ]; then
  echo "Error: input file not found: $INPUT_FILE"
  exit 1
fi

# ── Resolve absolute paths (works on both Linux and macOS) ────────────────
INPUT_ABS="$(cd "$(dirname "$INPUT_FILE")" && pwd)/$(basename "$INPUT_FILE")"
INPUT_DIR="$(dirname "$INPUT_ABS")"
INPUT_NAME="$(basename "$INPUT_ABS")"
BASE_NAME="${INPUT_NAME%.*}"

# Create output directory if it does not exist
mkdir -p "$OUTPUT_DIR"
OUTPUT_ABS="$(cd "$OUTPUT_DIR" && pwd)"

# ── Friendly log ──────────────────────────────────────────────────────────
echo "Rendering: $INPUT_NAME"
echo "  → ${BASE_NAME}.svg"
echo "  → ${BASE_NAME}.png"

# ── SVG rendering ─────────────────────────────────────────────────────────
if docker run --rm \
     -v "${INPUT_DIR}:/input:ro" \
     -v "${OUTPUT_ABS}:/output" \
     "$PLANTUML_IMAGE" \
     -tsvg \
     -o "/output" \
     "/input/${INPUT_NAME}"; then
  echo "SVG OK"
else
  echo "SVG FAILED"
  exit 1
fi

# ── PNG rendering ─────────────────────────────────────────────────────────
if docker run --rm \
     -v "${INPUT_DIR}:/input:ro" \
     -v "${OUTPUT_ABS}:/output" \
     "$PLANTUML_IMAGE" \
     -tpng \
     -o "/output" \
     "/input/${INPUT_NAME}"; then
  echo "PNG OK"
else
  echo "PNG FAILED"
  exit 1
fi
