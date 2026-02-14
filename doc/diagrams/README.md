# Diagram Pipeline — PlantUML → SVG & PNG

## Source of Truth

**PlantUML files (`.puml`) are the single source of truth** for all architectural and sequence diagrams in this project. SVG and PNG files are **generated artifacts** and must never be edited by hand.

## Folder Convention

For every PlantUML source file, the rendered outputs are placed in a sibling `Rendered/` directory with the same base name:

```
<dir>/<name>.puml          ← source (edit this)
<dir>/Rendered/<name>.svg  ← generated output (do NOT edit)
<dir>/Rendered/<name>.png  ← generated output (do NOT edit)
```

Example:

```
doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml
doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.svg
doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.png
```

## Embedding in Markdown

Use the canonical clickable pattern — PNG for display, SVG for zoom:

```markdown
[![Alt Text](path/to/Rendered/diagram.png)](path/to/Rendered/diagram.svg)
```

## Rendering Locally

Prerequisites: **Docker** must be running (the script uses the `plantuml/plantuml` image, which includes Graphviz).

From the repository root:

```bash
./scripts/render-diagrams.sh
```

The script will:

1. Recursively find all `*.puml` and `*.plantuml` files in the repo.
2. Create the `Rendered/` directory next to each source if it doesn't exist.
3. Render each diagram to both SVG and PNG, overwriting any previous version.
4. Print a summary and exit non-zero if any diagram fails.

## CI — Automatic Rendering

A GitHub Actions workflow (`.github/workflows/plantuml-svg-autocommit.yml`) runs on every push to `main`:

1. Checks out the repository.
2. Runs `scripts/render-diagrams.sh`.
3. If any outputs changed, auto-commits **only** the files inside `**/Rendered/*.svg` and `**/Rendered/*.png`.
4. The commit message includes `[skip ci]` to prevent infinite loops.

**No manual action is needed** — just edit a `.puml` file, push to `main`, and the workflow will regenerate and commit the updated diagrams automatically.
