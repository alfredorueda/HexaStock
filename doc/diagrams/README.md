# Maintain diagrams for GitHub and GitBook

Editable PlantUML sources and their SVG/PNG renders should describe the same behavior. The book uses PNG copies under `.gitbook/assets`; SVG links provide a full-size view. Preserve each image's aspect ratio: do not force both width and height.

## Sources and exceptions

For a source `name.puml`, keep `Rendered/name.svg` and `Rendered/name.png` beside it. Shared styles may be included from another directory. The original hand-drawn hexagonal overview and attributed community images are separate source images, not generated PlantUML artifacts. Preserve their attribution and natural proportions.

Presentation materials have their own canonical files and maintenance instructions in [AGENTS.md](../presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/AGENTS.md); do not bulk-regenerate them as a book maintenance step.

## Render only the diagrams you changed

With Java, Graphviz, and a local PlantUML JAR available, run from the repository root. Replace `/path/to/plantuml.jar` with the actual installation:

```bash
java -jar /path/to/plantuml.jar -failfast2 -tsvg -o Rendered \
  doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml
java -jar /path/to/plantuml.jar -failfast2 -tpng -o Rendered \
  doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml
cp doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.png \
  .gitbook/assets/sell-domain-fifo.png
```

The staged diagrams were rendered with PlantUML 1.2025.2 and Graphviz. Rendering with another version can change layout; inspect the output. Keep relative includes resolvable. The legacy bulk Docker script mounts each source folder independently, so cross-directory includes need additional handling; it also scans presentation resources. It is not the default workflow for these staged chapters.

There is no automatic diagram-rendering workflow in the current `.github/workflows` directory. Commit the source, both renders, and the GitBook PNG together after inspection.

## Publish a figure, not a linked thumbnail

Put the Markdown image on its own line and put its full-size link in a separate paragraph. Wrapping the image in a link (`[![...](...)](...)`) can make GitBook import it as an inline image, limited to 300 pixels, instead of a figure that fills the reading column.

Use a direct `raw.githubusercontent.com/alfredorueda/HexaStock/main/` link to the SVG, or to the original PNG when no vector source exists. This opens the image itself so readers can zoom without navigating GitHub's file interface. Keep the embedded image path relative to the page.

Check the GitBook preview after synchronization, then the published page after merging. Inspect desktop and narrow mobile widths: the whole diagram should fit proportionally, and its full-size link should load an image. Check long tables and code blocks by scrolling within them; the whole page should not overflow sideways. A successful synchronization check does not establish visual readability.

## Verify meaning and appearance

* Distinguish source dependency arrows from runtime interactions. An adapter's implementation dependency points toward its port.
* Check class names, cardinalities, amounts, error order, and proposed/current labels against the relevant contract.
* Inspect at book-column width as well as full size. Reduce participants or split a diagram when labels become too small.
* Avoid overlapping labels and relying on color alone. Give every image useful alternative text.
* Confirm the displayed PNG matches its `Rendered` copy and that full-size/source links resolve.

See [the sell-stock diagram guide](../tutorial/sellStocks/diagrams/README.md) for its scenario and shared style.
