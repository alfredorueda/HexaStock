#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# generate-consulting-reading-edition.sh
#
# Builds a large-font reading edition of the consulting / training
# documentation set produced for the Monday session, suitable for
# comfortable reading on a reMarkable 2 tablet.
#
# Outputs (under doc/consultancy/monday-session/reading-edition/):
#   • HexaStock-Consulting-Guide-Large-Print.pdf
#   • HexaStock-Consulting-Guide-Large-Print.docx   (best-effort)
#
# Requirements:
#   • Docker (uses the pandoc/latex image, which includes xelatex +
#     the extsizes package needed for the 20 pt body type).
#   • Python 3 on the host (used only to rewrite image paths).
#
# Body text size: 20 pt   (extarticle class via extsizes).
# Headings scale automatically (KOMA-style headings, ~20–32 pt).
# Code blocks: \normalsize (i.e. 20 pt) with line-wrapping enabled.
#
# Reproducible regeneration:  ./scripts/generate-consulting-reading-edition.sh
# ---------------------------------------------------------------------------
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC_MS="${REPO_ROOT}/doc/consultancy/monday-session"
SRC_SS="${REPO_ROOT}/doc/tutorial/sellStocks"
OUT_DIR="${SRC_MS}/reading-edition"
BUILD_DIR="${OUT_DIR}/build"
PANDOC_IMAGE="${PANDOC_IMAGE:-pandoc/extra:latest}"

mkdir -p "${OUT_DIR}" "${BUILD_DIR}"

# Source files, in reading order. Only the consulting-specific docs.
FILES=(
  "${SRC_MS}/README.md"                                    # focused doc map
  "${SRC_MS}/00-ARCHITECTURE-OVERVIEW.md"
  "${SRC_MS}/01-FILESYSTEM-AND-MAVEN-STRUCTURE.md"
  "${SRC_MS}/02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md"
  "${SRC_MS}/03-LAYOUT-ALTERNATIVES.md"
  "${SRC_MS}/04-PRODUCTION-EVOLUTION.md"
  "${SRC_SS}/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md"
  "${SRC_MS}/05-INSTRUCTOR-GUIDE.md"
  "${SRC_MS}/06-SLIDE-DECK-SPEC.md"
)

COMBINED="${BUILD_DIR}/combined.md"
: > "${COMBINED}"

# ----------------------------------------------------------------------
# Cover page (raw LaTeX, accepted by pandoc with +raw_tex)
# ----------------------------------------------------------------------
cat >> "${COMBINED}" <<'EOF'
\thispagestyle{empty}
\vspace*{3cm}
\begin{center}
{\Huge\bfseries HexaStock Consulting Guide}\\[1.5cm]
{\LARGE Spring Modulith \textbullet{} Domain Events}\\[0.4cm]
{\LARGE Watchlists \textbullet{} Hexagonal Architecture}\\[2.5cm]
{\Large Large-format reading edition for reMarkable 2}\\[3cm]
EOF
printf '{\\large Branch: \\texttt{feature/modulith-watchlists-extraction}}\\\\[0.6cm]\n' >> "${COMBINED}"
printf '{\\large Generated: %s}\n' "$(date '+%Y-%m-%d')" >> "${COMBINED}"
cat >> "${COMBINED}" <<'EOF'
\end{center}
\newpage
\tableofcontents
\newpage

EOF

# ----------------------------------------------------------------------
# Per-file preprocessing:
#   • image src ![](path)  → ![](/data/<repo-relative-abs-path>)
#   • cross-doc .md links  → kept as plain text (no PDF anchor exists)
#   • leave http(s) links and #anchors alone
# ----------------------------------------------------------------------
for f in "${FILES[@]}"; do
  printf '\n\n\\newpage\n\n' >> "${COMBINED}"
  python3 - "$f" "$(dirname "$f")" "${REPO_ROOT}" >> "${COMBINED}" <<'PY'
import sys, re, os
src, base, repo_root = sys.argv[1], sys.argv[2], sys.argv[3]
with open(src) as fh:
    content = fh.read()

# Transliterate Unicode glyphs that are missing in Latin Modern (the
# default font in pandoc/extra) so xelatex does not warn or render
# blanks. We keep semantics by mapping to ASCII equivalents.
_translit = {
    '✅': '[OK] ', '❌': '[X] ', '⚠️': '[!] ', '⚠': '[!] ',
    '⇒': '=>', '→': '->', '←': '<-', '↔': '<->',
    '⇐': '<=', '⇔': '<=>',
    '≈': '~', '≡': '==', '≠': '!=', '≤': '<=', '≥': '>=',
    '─': '-', '━': '-', '│': '|', '┃': '|',
    '┌': '+', '┐': '+', '└': '+', '┘': '+',
    '├': '+', '┤': '+', '┬': '+', '┴': '+', '┼': '+',
    '►': '>', '◄': '<', '▶': '>', '◀': '<',
    '•': '*', '‣': '>', '◦': 'o',
    '️': '',
    '–': '-', '—': '--',
    '‘': "'", '’': "'", '“': '"', '”': '"',
    '…': '...',
}
for k, v in _translit.items():
    content = content.replace(k, v)
# Strip remaining astral-plane emoji (U+1F300..U+1FAFF, etc.)
content = re.sub(r'[\U0001F000-\U0001FFFF]', '', content)

# Flatten the clickable-image idiom  [![alt](png)](svg)  →  ![alt](png)
# (the linked SVG cannot be followed inside a PDF on a reMarkable.)
content = re.sub(
    r'\[(!\[[^\]]*\]\([^)]+\))\]\([^)]+\)',
    r'\1',
    content,
)

def to_container_abs(path):
    """Resolve `path` (relative to base) to an absolute /data/... path
       that pandoc-in-docker can read. Returns None if the file does
       not exist on disk."""
    path_only = path.split('#', 1)[0].split('?', 1)[0]
    abs_path = os.path.normpath(os.path.join(base, path_only))
    if not os.path.exists(abs_path):
        return None
    rel = os.path.relpath(abs_path, repo_root)
    return '/data/' + rel.replace(os.sep, '/')

def fix_image(m):
    alt, path = m.group(1), m.group(2)
    if path.startswith(('http://', 'https://', '/data/', 'data:')):
        return m.group(0)
    new = to_container_abs(path)
    if new is None:
        return m.group(0)
    return f'![{alt}]({new})'

# Markdown images
content = re.sub(r'!\[([^\]]*)\]\(([^)]+)\)', fix_image, content)

# Convert cross-document links to .md files into plain text (the PDF
# does not have anchors back into other source files). Use a negative
# lookbehind for `!` so we don't strip image syntax.
content = re.sub(
    r'(?<!\!)\[([^\]]+)\]\(([^)]*\.md(?:#[^)]*)?)\)',
    r'\1',
    content,
)

# Convert links to .puml / Rendered/* (which point to source/asset files
# the PDF reader cannot follow) to plain text as well. Same lookbehind.
content = re.sub(
    r'(?<!\!)\[([^\]]+)\]\(([^)]*\.(?:puml|svg|png)(?:#[^)]*)?)\)',
    r'\1',
    content,
)

sys.stdout.write(content)
PY
done

# ----------------------------------------------------------------------
# Pandoc metadata: large-print configuration
#   • extarticle (extsizes) → supports 14/17/20 pt body
#   • 20 pt body, 25 mm margins, sans-serif default
#   • code blocks set with breaklines so wide lines do not overflow
# ----------------------------------------------------------------------
cat > "${BUILD_DIR}/metadata.yaml" <<'EOF'
---
title: "HexaStock Consulting Guide"
documentclass: scrartcl
classoption:
  - fontsize=20pt
  - DIV=14
  - parskip=half
  - oneside
geometry:
  - paperwidth=210mm
  - paperheight=297mm
  - top=22mm
  - bottom=22mm
  - left=22mm
  - right=22mm
linestretch: 1.15
header-includes:
  - \usepackage{graphicx}
  - \setkeys{Gin}{width=0.92\linewidth,keepaspectratio}
  - \usepackage{fvextra}
  - \DefineVerbatimEnvironment{Highlighting}{Verbatim}{breaklines,breakanywhere,breaksymbolleft={},commandchars=\\\{\}}
  - \usepackage{microtype}
  - \setlength{\emergencystretch}{3em}
  - \widowpenalty=10000
  - \clubpenalty=10000
toc: false
links-as-notes: false
colorlinks: false
---
EOF

# ----------------------------------------------------------------------
# Render PDF (xelatex)
# ----------------------------------------------------------------------
echo "▸ Generating PDF (xelatex, 20 pt body) ..."
docker run --rm --platform linux/amd64 -v "${REPO_ROOT}:/data" -w /data "${PANDOC_IMAGE}" \
  --from=markdown+raw_tex+yaml_metadata_block \
  --pdf-engine=xelatex \
  --metadata-file=/data/doc/consultancy/monday-session/reading-edition/build/metadata.yaml \
  -o "doc/consultancy/monday-session/reading-edition/HexaStock-Consulting-Guide-Large-Print.pdf" \
  "doc/consultancy/monday-session/reading-edition/build/combined.md"

# ----------------------------------------------------------------------
# Render DOCX (best-effort; DOCX font size must come from a reference doc
# or be set in the editor — the body uses pandoc's default 11 pt unless
# a reference.docx is supplied. The PDF is the primary deliverable.)
# ----------------------------------------------------------------------
echo "▸ Generating DOCX (default styling — PDF is the primary deliverable) ..."
docker run --rm --platform linux/amd64 -v "${REPO_ROOT}:/data" -w /data "${PANDOC_IMAGE}" \
  --from=markdown+raw_tex+yaml_metadata_block \
  -o "doc/consultancy/monday-session/reading-edition/HexaStock-Consulting-Guide-Large-Print.docx" \
  "doc/consultancy/monday-session/reading-edition/build/combined.md" \
  || echo "  (DOCX generation skipped or failed — PDF is the canonical output)"

echo ""
echo "✅ Output:"
ls -lh "${OUT_DIR}"/*.pdf "${OUT_DIR}"/*.docx 2>/dev/null || true
echo ""
echo "Move the PDF to your reMarkable 2:"
echo "  ${OUT_DIR}/HexaStock-Consulting-Guide-Large-Print.pdf"
