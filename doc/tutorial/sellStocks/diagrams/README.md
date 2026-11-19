# Sell Stock Learning Path Diagrams

The [learning path](../SELL-STOCK-TUTORIAL.md) uses ten diagrams. Their editable PlantUML sources share `sell-style.puml`: navy text, blue adapter/orchestration blocks, teal domain blocks, amber outcomes and red exception arrows. Labels also describe each role, so colour is not the only distinction.

| Stage | Source |
| --- | --- |
| 2 — Domain model | `sell-domain-model.puml` |
| 3 — FIFO | `sell-domain-fifo.puml` |
| 4 — Architecture | `sell-architecture-overview.puml` |
| 4 — HTTP | `sell-http-to-port.puml` |
| 5 — Orchestration | `sell-application-service.puml` |
| 5 — Responsibilities | `sell-orchestrator-vs-aggregate.puml` |
| 6 — Persistence | `sell-persistence-adapter.puml` |
| 8 — Invalid quantity | `sell-error-invalid-quantity.puml` |
| 8 — Missing portfolio | `sell-error-portfolio-not-found.puml` |
| 8 — Overselling | `sell-error-sell-more-than-owned.puml` |

## Update a diagram

Edit the source, render both formats, inspect the result, then synchronize the PNG used by GitBook. From the repository root, with Docker running:

```bash
./scripts/render-diagram.sh \
  doc/tutorial/sellStocks/diagrams/sell-domain-fifo.puml \
  doc/tutorial/sellStocks/diagrams/Rendered
cp doc/tutorial/sellStocks/diagrams/Rendered/sell-domain-fifo.png \
  .gitbook/assets/sell-domain-fifo.png
```

Alternatively, use a local PlantUML installation. The staged edition was rendered with PlantUML 1.2025.2 and Graphviz, with 180 DPI PNG output specified in the shared style. Keep the include file beside the source when rendering. A shared-style edit requires rerendering all ten diagrams.

The book embeds PNGs from `.gitbook/assets` using relative paths and descriptive alt text. Each figure links to its SVG for zooming and its PlantUML source for maintenance. Commit the source, both rendered formats and the GitBook PNG together; the two PNG copies must be identical.

## Check semantics as well as layout

The successful sale uses 10 shares bought at 100.00, then 5 at 120.00, selling 12 at 150.00. Proceeds are 1800.00, cost basis 1240.00 and profit 560.00; 3 shares remain. The overselling diagram explicitly uses a separate error fixture: 10 available, 100 requested.

The architecture diagram shows source dependencies; the sequence diagrams show runtime interactions. A port and its implementation are not separate network calls. The persistence diagram distinguishes `save` from later flush/commit, and the error diagrams preserve the order of validation in `main`.

Other files in this directory support older companion chapters. They are not part of the staged path; update their consumers deliberately rather than assuming every diagram describes the same scenario.
