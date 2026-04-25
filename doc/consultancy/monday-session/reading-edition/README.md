# Reading Edition — Large-Format PDF for reMarkable 2

This folder contains a single-document, large-font reading edition of the
**Monday session consulting documentation** (Spring Modulith, Domain Events,
Watchlists, Hexagonal Architecture).

| File | Purpose |
|---|---|
| `HexaStock-Consulting-Guide-Large-Print.pdf` | Primary deliverable. A4, **20 pt body**, KOMA-Script `scrartcl`, ~144 pages. Move this to your reMarkable 2. |
| `HexaStock-Consulting-Guide-Large-Print.docx` | Companion best-effort DOCX (default Word styling — adjust manually if needed). |

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

Requires Docker. Uses the `pandoc/extra:latest` image (linux/amd64; runs under
emulation on Apple Silicon — first run is slow, subsequent runs are fast).

## Design choices

- **Document class:** KOMA-Script `scrartcl` with `fontsize=20pt`, `DIV=14`,
  `parskip=half`, `oneside`. KOMA supports arbitrary body sizes natively;
  the `extsizes` package is not bundled in `pandoc/extra`.
- **Geometry:** A4 portrait with 22 mm margins on all sides — comfortable on
  a 6.8" eink screen at native zoom.
- **Body font:** Latin Modern (default in pandoc/extra). Unicode glyphs that
  Latin Modern lacks (emojis, box-drawing, arrows, tick marks) are
  transliterated to ASCII equivalents during preprocessing.
- **Code blocks:** `\normalsize` (i.e. 20 pt) with `fvextra` line-wrapping so
  long lines do not overflow.
- **Cross-document `.md` links** are flattened to plain text — the PDF has no
  anchor back into the original markdown.
- **Clickable-image idiom** `[![alt](png)](svg)` is flattened to plain
  `![alt](png)` — SVG cannot be embedded directly, and the PDF reader on a
  reMarkable cannot follow external links anyway.

## Build artefacts

The `build/` folder (gitignored) contains the assembled `combined.md` and the
pandoc YAML metadata used to render the PDF. Inspect those files when
debugging the pipeline.
