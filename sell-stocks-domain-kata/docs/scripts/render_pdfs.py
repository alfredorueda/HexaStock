#!/usr/bin/env python3
"""Render the Sell Stocks Domain Kata self-paced PDFs from their Markdown sources.

Single source of truth for the visual design (fonts, colours, tables, code blocks,
PDF bookmarks) used across all self-paced handouts. Edit the CSS in this file once;
every PDF picks it up the next time it's rendered.

Usage (always through the wrapper, which manages the virtualenv):
    docs/scripts/render-self-paced-pdfs.sh                # render everything + rebuild the zip
    docs/scripts/render-self-paced-pdfs.sh --no-zip        # render everything, skip the zip
    docs/scripts/render-self-paced-pdfs.sh exercise-1 spec # render only the named documents

Run directly only if DOCS/.venv-pdf already has weasyprint installed:
    docs/.venv-pdf/bin/python docs/scripts/render_pdfs.py [--no-zip] [doc-key ...]
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

from weasyprint import HTML

SCRIPT_DIR = Path(__file__).resolve().parent
DOCS_ROOT = SCRIPT_DIR.parent  # sell-stocks-domain-kata/docs
KATA_ROOT = DOCS_ROOT.parent  # sell-stocks-domain-kata

# ---------------------------------------------------------------------------
# Shared design tokens
# ---------------------------------------------------------------------------

SERIF = "'Iowan Old Style', 'Palatino Linotype', Palatino, 'Book Antiqua', Georgia, 'Times New Roman', serif"
SANS = "'Helvetica Neue', Arial, sans-serif"
MONO = "'SFMono-Regular', Menlo, Consolas, monospace"


def base_css(*, pre_label: str, table_font_size: str = "9.5pt", extra: str = "",
             page_margin: str = "18mm 20mm 15mm", body_font_size: str = "11.3pt",
             accent: str = "#b08d57", accent_dark: str = "#7a4a1f") -> str:
    """The warm gold/navy theme used by the four student-facing exercise handouts."""
    return f"""
  @page {{ size: A4; margin: {page_margin}; }}
  * {{ box-sizing: border-box; }}
  html, body {{ margin: 0; padding: 0; }}
  body {{
    background: #fffdf8; color: #1a1a1a; font-family: {SERIF};
    font-size: {body_font_size}; line-height: 1.55; -webkit-font-smoothing: antialiased;
  }}
  .page {{ max-width: 720px; margin: 0 auto; }}
  .kicker {{
    text-transform: uppercase; letter-spacing: .16em; font-size: 9pt; color: #8a6d3b;
    text-align: center; margin: 0 0 6mm; font-family: {SANS}; font-weight: 600;
  }}
  h1 {{
    font-size: 21.5pt; text-align: center; margin: 0 0 3mm; color: #16233c;
    font-weight: 600; letter-spacing: .005em; line-height: 1.28;
  }}
  .subtitle {{ text-align: center; font-style: italic; color: #54606f; font-size: 10.6pt; margin: 0 0 6mm; }}
  .hr {{ border: none; border-top: 1.4pt solid {accent}; width: 54px; margin: 0 auto 11mm; }}
  hr:not(.hr) {{ border: none; border-top: 0.6pt solid #ddd6c5; margin: 5mm 0; }}
  h2 {{
    font-size: 13.3pt; color: #16233c; margin: 6.5mm 0 3mm; padding-bottom: 1.5mm;
    border-bottom: 0.8pt solid #ddd6c5; font-weight: 600; letter-spacing: .015em; page-break-after: avoid;
  }}
  .page > h2:first-of-type {{ margin-top: 0; }}
  h3 {{
    font-size: 11.6pt; color: {accent_dark}; margin: 5.5mm 0 2.4mm; font-weight: 600;
    letter-spacing: .01em; page-break-after: avoid;
  }}
  p {{ margin: 0 0 2.9mm; text-align: justify; hyphens: auto; orphans: 3; widows: 3; }}
  strong {{ color: #16233c; }}
  a {{ color: {accent_dark}; text-decoration: none; border-bottom: 0.6pt solid #cdb98a; }}
  blockquote {{
    margin: 3.5mm 0; padding: 3mm 6mm; background: #f8f3e7; border-left: 2.6pt solid {accent};
    border-radius: 2px; font-style: italic; color: #2c2c2c; page-break-inside: avoid;
  }}
  blockquote p {{ text-align: left; margin: 0; }}
  blockquote p + p {{ margin-top: 2mm; }}
  ul, ol {{ margin: 0 0 3.4mm; padding-left: 6mm; }}
  li {{ margin-bottom: 1.3mm; orphans: 3; widows: 3; }}
  code {{ font-family: {MONO}; font-size: 9.5pt; background: #f1ece0; padding: 0.5pt 1.8pt; border-radius: 2px; }}
  pre {{
    background: #16233c; color: #eef1f6; padding: 3.8mm 6mm; border-radius: 3px;
    font-size: 9.1pt; line-height: 1.48; white-space: pre-wrap; overflow-wrap: break-word;
    page-break-inside: avoid; margin: 3mm 0 4mm;
  }}
  pre code {{ background: none; color: inherit; padding: 0; }}
  pre::before {{
    content: "{pre_label}"; display: block; font-family: {SANS}; font-size: 7.6pt;
    letter-spacing: .16em; text-transform: uppercase; color: {accent}; margin-bottom: 2.8mm;
  }}
  table {{ width: 100%; border-collapse: collapse; margin: 2.5mm 0 4.5mm; font-size: {table_font_size}; line-height: 1.38; }}
  thead {{ display: table-header-group; }}
  tr {{ page-break-inside: avoid; }}
  th, td {{ border: 0.6pt solid #ddd6c5; padding: 1.8mm 2.6mm; text-align: left; vertical-align: top; }}
  th {{
    background: #f1ece0; color: #16233c; font-family: {SANS}; font-size: 7.8pt;
    text-transform: uppercase; letter-spacing: .05em; font-weight: 600;
  }}
  figure {{ margin: 4mm 0; text-align: center; page-break-inside: avoid; }}
  figure img {{ max-width: 100%; border: 0.6pt solid #ddd6c5; border-radius: 3px; }}
  figcaption {{ font-size: 8.6pt; color: #8a8a8a; font-style: italic; margin-top: 2mm; font-family: {SANS}; }}
  .footnote {{
    margin-top: 13mm; padding-top: 3.2mm; border-top: 0.6pt solid #ddd6c5; font-size: 8.6pt;
    color: #8a8a8a; text-align: center; font-family: {SANS};
  }}
  h1 {{ bookmark-level: 1; bookmark-label: content(); }}
  h2 {{ bookmark-level: 2; bookmark-label: content(); }}
  h3 {{ bookmark-level: 3; bookmark-label: content(); }}
  {extra}
"""


def instructor_css() -> str:
    """The cooler blue-grey theme for instructor-only companion notes."""
    return f"""
  @page {{ size: A4; margin: 18mm 20mm 15mm; }}
  * {{ box-sizing: border-box; }}
  html, body {{ margin: 0; padding: 0; }}
  body {{
    background: #fbfbfd; color: #1a1a1a; font-family: {SERIF};
    font-size: 11.3pt; line-height: 1.55; -webkit-font-smoothing: antialiased;
  }}
  .page {{ max-width: 720px; margin: 0 auto; }}
  .kicker {{
    text-transform: uppercase; letter-spacing: .16em; font-size: 9pt; color: #45557a;
    text-align: center; margin: 0 0 6mm; font-family: {SANS}; font-weight: 600;
  }}
  h1 {{
    font-size: 20pt; text-align: center; margin: 0 0 3mm; color: #16233c;
    font-weight: 600; letter-spacing: .005em; line-height: 1.28;
  }}
  .subtitle {{ text-align: center; font-style: italic; color: #54606f; font-size: 10.4pt; margin: 0 0 6mm; }}
  .hr {{ border: none; border-top: 1.4pt solid #45557a; width: 54px; margin: 0 auto 11mm; }}
  p {{ margin: 0 0 2.9mm; text-align: justify; hyphens: auto; orphans: 3; widows: 3; }}
  strong {{ color: #16233c; }}
  a {{ color: #45557a; text-decoration: none; border-bottom: 0.6pt solid #b8c2d4; }}
  blockquote {{
    margin: 3.5mm 0; padding: 3mm 6mm; background: #f3f0e6; border-left: 2.6pt solid #b08d57;
    border-radius: 2px; font-style: italic; color: #2c2c2c; page-break-inside: avoid;
  }}
  blockquote p {{ text-align: left; margin: 0; }}
  ul {{ margin: 0 0 3.4mm; padding-left: 6mm; }}
  li {{ margin-bottom: 1.3mm; orphans: 3; widows: 3; }}
  code {{ font-family: {MONO}; font-size: 9.5pt; background: #eef1f6; padding: 0.5pt 1.8pt; border-radius: 2px; }}
  .note-box {{
    margin: 0 0 6mm; padding: 4mm 6mm; background: #eef1f6; border: 0.7pt solid #c3ccdb;
    border-left: 3pt solid #16233c; border-radius: 2px; page-break-inside: avoid;
  }}
  .note-box h2 {{
    margin: 0 0 3mm; font-size: 12.6pt; color: #16233c; font-weight: 600; letter-spacing: .01em;
    bookmark-level: 2; bookmark-label: content();
  }}
  .note-box blockquote {{ background: #fbfbfd; }}
  .footnote {{
    margin-top: 13mm; padding-top: 3.2mm; border-top: 0.6pt solid #ddd6c5; font-size: 8.6pt;
    color: #8a8a8a; text-align: center; font-family: {SANS};
  }}
  h1 {{ bookmark-level: 1; bookmark-label: content(); }}
"""


# ---------------------------------------------------------------------------
# Markdown -> HTML body
# ---------------------------------------------------------------------------

def markdown_to_body(md_path: Path) -> str:
    """Convert Markdown to an HTML fragment via pandoc and drop pandoc's own <h1>
    (the page template renders its own title/kicker/subtitle block instead)."""
    result = subprocess.run(
        ["pandoc", str(md_path), "-f", "markdown", "-t", "html", "--wrap=none"],
        capture_output=True, text=True, check=True,
    )
    html = result.stdout
    lines = html.split("\n")
    if lines and lines[0].startswith("<h1"):
        lines = lines[1:]
    return "\n".join(lines)


def wrap_note_boxes(body_html: str) -> str:
    """Wrap each top-level <h2>...</h2> section (up to the next <h2> or end) in a
    .note-box div, for the instructor-notes variant."""
    parts = re.split(r"(?=<h2)", body_html)
    out = []
    for part in parts:
        if part.startswith("<h2"):
            out.append(f'<div class="note-box">\n{part.rstrip()}\n</div>')
        else:
            out.append(part)
    return "\n".join(p for p in out if p.strip())


# ---------------------------------------------------------------------------
# Page assembly + render
# ---------------------------------------------------------------------------

PAGE_TEMPLATE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>{title}</title>
<style>
{css}
</style>
</head>
<body>
<div class="page">
  <div class="kicker">{kicker}</div>
  <h1>{title}</h1>
  <p class="subtitle">{subtitle}</p>
  <hr class="hr">

{body}

  <div class="footnote">{footnote}</div>
</div>
</body>
</html>
"""


def render_one(key: str, cfg: dict) -> None:
    md_path: Path = cfg["md"]
    pdf_path: Path = cfg["pdf"]
    variant = cfg.get("variant", "standard")

    body = markdown_to_body(md_path)
    if variant == "instructor":
        body = wrap_note_boxes(body)
        css = instructor_css()
    elif variant == "dense":
        css = base_css(
            pre_label=cfg.get("pre_label", "Copy this block"),
            table_font_size=cfg.get("table_font_size", "8.6pt"),
            page_margin="16mm 16mm 14mm",
            body_font_size="10.6pt",
            extra=cfg.get("extra_css", ""),
        )
    else:
        css = base_css(
            pre_label=cfg.get("pre_label", "Copy this block"),
            table_font_size=cfg.get("table_font_size", "9.5pt"),
            extra=cfg.get("extra_css", ""),
        )

    html = PAGE_TEMPLATE.format(
        title=cfg["title"], kicker=cfg["kicker"], subtitle=cfg["subtitle"],
        footnote=cfg["footnote"], body=body, css=css,
    )

    pdf_path.parent.mkdir(parents=True, exist_ok=True)
    # base_url = the source directory, so relative image/link paths in the
    # Markdown (e.g. spec/png/diagram.png) resolve without any manual fix-up.
    HTML(string=html, base_url=str(md_path.parent)).write_pdf(str(pdf_path))
    print(f"  {key}: wrote {pdf_path.relative_to(KATA_ROOT)}")


# ---------------------------------------------------------------------------
# Document registry — the single place that knows about every handout
# ---------------------------------------------------------------------------

EX1 = DOCS_ROOT / "exercise-1-prompt-only"
EX2 = DOCS_ROOT / "exercise-2-specification-driven"
EX3 = DOCS_ROOT / "exercise-3-skills-and-agents"
EX4 = DOCS_ROOT / "exercise-4-rest-and-persistence"
SPEC = EX2 / "spec"

DOCUMENTS = {
    "exercise-1": dict(
        md=EX1 / "exercise-1-self-paced-assignment.md",
        pdf=EX1 / "exercise-1-self-paced-assignment.pdf",
        title="Exercise 1 — Build it from a prompt",
        subtitle="Build working software from a deliberately incomplete brief",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Self-paced track",
        footnote="Sell Stocks Domain Kata — Exercise 1",
        pre_label="Suggested prompt",
        table_font_size="9.6pt",
        extra_css="td:last-child { background: #fffefb; }",
    ),
    "exercise-2": dict(
        md=EX2 / "exercise-2-self-paced-assignment.md",
        pdf=EX2 / "exercise-2-self-paced-assignment.pdf",
        title="Exercise 2 — Build it from specifications",
        subtitle="A self-paced companion for building from durable specifications",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Self-paced track",
        footnote="Sell Stocks Domain Kata — Exercise 2",
        pre_label="Copy this block",
        table_font_size="9.5pt",
    ),
    "exercise-3": dict(
        md=EX3 / "exercise-3-self-paced-assignment.md",
        pdf=EX3 / "exercise-3-self-paced-assignment.pdf",
        title="Exercise 3 — Build it with a durable working method",
        subtitle="A self-paced companion using real GitHub Copilot custom agents",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Self-paced track",
        footnote="Sell Stocks Domain Kata — Exercise 3",
        pre_label="Copy this block",
        table_font_size="9.5pt",
    ),
    "exercise-3-instructor-notes": dict(
        md=EX3 / "exercise-3-self-paced-instructor-notes.md",
        pdf=EX3 / "exercise-3-self-paced-instructor-notes.pdf",
        title="Exercise 3 (self-paced) — instructor notes",
        subtitle="Pairs with exercise-3-self-paced-assignment.pdf — keep this one out of the student packet",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Instructor notes &nbsp;&middot;&nbsp; not for students",
        footnote="Sell Stocks Domain Kata — instructor notes for the self-paced Exercise 3 handout",
        variant="instructor",
    ),
    "exercise-4": dict(
        md=EX4 / "exercise-4-self-paced-assignment.md",
        pdf=EX4 / "exercise-4-self-paced-assignment.pdf",
        title="Exercise 4 — REST API and persistence",
        subtitle="A self-paced companion for wrapping the domain in Spring Boot and JPA",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Self-paced track",
        footnote="Sell Stocks Domain Kata — Exercise 4",
        pre_label="Copy this block",
        table_font_size="9.3pt",
    ),
    "spec": dict(
        md=SPEC / "sell-stocks-spec.md",
        pdf=SPEC / "sell-stocks-spec.pdf",
        title="US-07 — Sell Stocks (domain specification)",
        subtitle="Behaviour: preconditions, the FIFO rule, money definitions, and acceptance criteria AC-01–AC-24",
        kicker="Sell Stocks Domain Kata &nbsp;&middot;&nbsp; Specification",
        footnote="Sell Stocks Domain Kata — sell-stocks-spec.md, rendered for distribution",
        variant="dense",
    ),
}

# sell-stocks-spec.md links to error-contract.md / domain-model.md, which are not
# part of the student bundle — rewritten at render time so the PDF never points to
# a file the reader won't have. Keep in sync with the source by hand if that intro
# paragraph changes shape.
SPEC_LINK_FIXUPS = [
    (
        re.compile(
            r'Failure outcomes are domain exceptions — see '
            r'<a href="error-contract\.md"><code>error-contract\.md</code></a>\.'
        ),
        "Failure outcomes are domain exceptions, named throughout the acceptance criteria below.",
    ),
    (
        re.compile(
            r'This file describes <strong>what selling does</strong>\. Two companion files describe the rest:'
            r'</p>\s*<ul>\s*'
            r'<li><a href="domain-model\.md"><code>domain-model\.md</code></a> — the classes, fields and value '
            r'objects this behaviour relies on \(also drawn in '
            r'<a href="domain-class-diagram\.puml"><code>domain-class-diagram\.puml</code></a>\)\.</li>\s*'
            r'<li><a href="error-contract\.md"><code>error-contract\.md</code></a> — what each failure raises, '
            r'and its message\.</li>\s*</ul>',
            re.DOTALL,
        ),
        "This file describes <strong>what selling does</strong>. The structure it relies on — classes, fields, "
        "and value objects — is drawn in the class diagram bundled alongside this document (PlantUML, Mermaid, "
        "and a rendered image).</p>",
    ),
    (
        re.compile(
            r'The sale price must be positive \(&gt; 0\) — from the value-object rules in '
            r'<a href="domain-model\.md"><code>domain-model\.md</code></a>\.'
        ),
        "The sale price must be positive (&gt; 0) — enforced by the <code>Price</code> value object at construction.",
    ),
]


def render_one_with_fixups(key: str, cfg: dict) -> None:
    if key != "spec":
        render_one(key, cfg)
        return
    # sell-stocks-spec.md is also the working spec other exercises link to, so we
    # don't want to edit the source just for PDF distribution — patch the HTML
    # after pandoc conversion instead.
    original_markdown_to_body = markdown_to_body

    def patched(md_path: Path) -> str:
        html = original_markdown_to_body(md_path)
        for pattern, replacement in SPEC_LINK_FIXUPS:
            html, n = pattern.subn(replacement, html)
            if n == 0:
                print(f"  warning: a spec link fix-up matched nothing — check "
                      f"{md_path.name} hasn't changed shape", file=sys.stderr)
        return html

    globals()["markdown_to_body"] = patched
    try:
        render_one(key, cfg)
    finally:
        globals()["markdown_to_body"] = original_markdown_to_body


# ---------------------------------------------------------------------------
# Zip bundle
# ---------------------------------------------------------------------------

ZIP_PATH = DOCS_ROOT / "self-paced-assignments.zip"
PREP_SCRIPT = EX3 / "scripts" / "prepare-student-workspace.sh"


def build_zip() -> None:
    print("Rebuilding self-paced-assignments.zip ...")
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        workspace = tmp_path / "exercise-3-copilot-workspace"
        subprocess.run(
            ["bash", str(PREP_SCRIPT), str(workspace)],
            check=True, cwd=str(KATA_ROOT), capture_output=True, text=True,
        )

        if ZIP_PATH.exists():
            ZIP_PATH.unlink()
        with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as z:
            for key in ("exercise-1", "exercise-2", "exercise-3", "exercise-4"):
                pdf = DOCUMENTS[key]["pdf"]
                z.write(pdf, pdf.name)
            for name in ("sell-stocks-spec.md", "sell-stocks-spec.pdf",
                          "domain-class-diagram.puml", "domain-class-diagram.mmd"):
                z.write(SPEC / name, f"spec/{name}")
            z.write(SPEC / "png" / "domain-class-diagram.png", "spec/domain-class-diagram.png")
            for f in workspace.rglob("*"):
                if f.is_file():
                    z.write(f, f"exercise-3-copilot-workspace/{f.relative_to(workspace)}")
    print(f"  wrote {ZIP_PATH.relative_to(KATA_ROOT)}")


# ---------------------------------------------------------------------------

def main() -> None:
    args = sys.argv[1:]
    do_zip = True
    if "--no-zip" in args:
        do_zip = False
        args = [a for a in args if a != "--no-zip"]

    keys = args or list(DOCUMENTS)
    unknown = [k for k in keys if k not in DOCUMENTS]
    if unknown:
        print(f"Unknown document key(s): {', '.join(unknown)}", file=sys.stderr)
        print(f"Known keys: {', '.join(DOCUMENTS)}", file=sys.stderr)
        sys.exit(2)

    print(f"Rendering {len(keys)} document(s) with WeasyPrint ...")
    for key in keys:
        render_one_with_fixups(key, DOCUMENTS[key])

    if do_zip:
        build_zip()
    else:
        print("Skipped zip rebuild (--no-zip).")

    print("Done.")


if __name__ == "__main__":
    main()
