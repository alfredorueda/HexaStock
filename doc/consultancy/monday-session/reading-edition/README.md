# Reading Edition — Large-Format PDF for reMarkable 2

This folder contains a single-document, large-font reading edition of the
**Monday session consulting documentation** (Spring Modulith, Domain Events,
Watchlists, Hexagonal Architecture).

| File | Purpose |
|---|---|
| `HexaStock-Consulting-Guide-Large-Print.pdf` | Primary deliverable. A4, **20 pt body**, KOMA-Script `scrartcl`, ~144 pages. Move this to your reMarkable 2. |
| `HexaStock-Consulting-Guide-Large-Print.docx` | Companion best-effort DOCX (default Word styling — adjust manually if needed). |
| `HexaStock-Consulting-Guide-Large-Print.md` | The single assembled Markdown source that produced the PDF and the DOCX. Useful for editing in Typora / iA Writer / VS Code, importing into Word with custom styling, or feeding any other tool. |

## Contents (in reading order)

1. Documentation map (`monday-session/README.md`)
2. `00 — Architecture Overview`
3. `01 — Filesystem & Maven Structure`
4. `02 — Watchlists Event-Flow Deep Dive`
5. `03 — Layout Alternatives`
6. `04 — Production Evolution`
7. `Sell Stocks — Domain Events Exercise`
8. `05 — Instructor Guide`
9. `06 — Slide Deck Spec`

All 12 inline diagrams (architecture overview, Maven multimodule, filesystem
layout, modulith modules, BC map, hexagonal view, watchlist sentinel sequence,
watchlist event flow, notification flow, layout alternatives, current-vs-future
events, plus the three Sell-Stocks event-flow diagrams) are embedded as PNGs.

## Regenerating

From the repo root:

```bash
./scripts/generate-consulting-reading-edition.sh
```

Requires Docker. The script builds (and reuses) a thin derived image
`hexastock/reading-edition:latest` based on `pandoc/extra:latest` plus two
extra LaTeX packages (`seqsplit`, `hyphenat`). The Dockerfile lives at
`scripts/reading-edition/Dockerfile`. First run is slow (~30 s build);
subsequent runs are fast.

## Design choices

- **Document class:** KOMA-Script `scrartcl` with `fontsize=20pt`, `DIV=14`,
  `parskip=half`, `oneside`. KOMA supports arbitrary body sizes natively;
  the `extsizes` package is not bundled in `pandoc/extra`.
- **Geometry:** A4 portrait with 22 mm margins on all sides — comfortable on
  a 6.8" eink screen at native zoom.
- **Body font:** Latin Modern (default in pandoc/extra). Unicode glyphs that
  Latin Modern lacks (emojis, box-drawing, arrows, tick marks) are
  transliterated to ASCII equivalents during preprocessing.
- **Code blocks:** rendered with `fvextra` at `\small` (i.e. ~16 pt at the
  20 pt base) with `breaklines,breakanywhere` so long lines wrap.
- **Inline code** (Markdown backticks, e.g. fully-qualified Java class
  names) goes through a redefined `\passthrough` macro that wraps the
  argument in `seqsplit`, allowing breaks at any character. Combined
  with `hyphenat[htt]`, `xurl`, and a generous `\emergencystretch=6em`,
  this eliminates overfull-hbox warnings (right-margin overflow) that
  Word silently absorbs but xelatex cannot.
- **Cross-document `.md` links** are flattened to plain text — the PDF has no
  anchor back into the original markdown.
- **Clickable-image idiom** `[![alt](png)](svg)` is flattened to plain
  `![alt](png)` — SVG cannot be embedded directly, and the PDF reader on a
  reMarkable cannot follow external links anyway.

## Build artefacts

The `build/` folder (gitignored) contains the assembled `combined.md` and the
pandoc YAML metadata used to render the PDF. Inspect those files when
debugging the pipeline.
