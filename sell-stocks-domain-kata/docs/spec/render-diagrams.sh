#!/usr/bin/env bash
#
# Renders the Mermaid diagrams in domain-diagrams.md to PNGs under docs/spec/png/.
#
# domain-diagrams.md is the single source: the fenced ```mermaid blocks in it are
# extracted in order and rendered. Edit the Markdown, then re-run this script —
# never edit the images.
#
# Requires Node and, on first run, network access (npx fetches mermaid-cli).
# This is a convenience for instructors; `mvn test` never depends on it.
#
set -euo pipefail

SPEC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$SPEC_DIR/domain-diagrams.md"
OUT_DIR="$SPEC_DIR/png"

# Output names, in the order the diagrams appear in domain-diagrams.md.
NAMES=(domain-class-diagram domain-er-diagram)

mkdir -p "$OUT_DIR"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Pull each fenced mermaid block into its own .mmd file.
awk -v dir="$TMP" '
  /^```mermaid$/ { n++; inblock = 1; next }
  /^```$/ && inblock { inblock = 0; next }
  inblock { print > (dir "/block" n ".mmd") }
' "$SRC"

# Chromium needs --no-sandbox in many CI and container environments.
cat > "$TMP/puppeteer.json" <<'JSON'
{ "args": ["--no-sandbox", "--disable-setuid-sandbox"] }
JSON

for i in "${!NAMES[@]}"; do
  block="$TMP/block$((i + 1)).mmd"
  target="$OUT_DIR/${NAMES[$i]}.png"

  if [[ ! -f "$block" ]]; then
    echo "error: no mermaid block $((i + 1)) found in $SRC" >&2
    exit 1
  fi

  echo "rendering ${NAMES[$i]}.png"
  npx -y @mermaid-js/mermaid-cli@11 \
    --input "$block" \
    --output "$target" \
    --backgroundColor white \
    --scale 3 \
    --puppeteerConfigFile "$TMP/puppeteer.json"
done

echo "done — images in $OUT_DIR"
