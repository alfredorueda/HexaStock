# Monday Session — Documentation Map

> **Scope.** This folder is a *focused* documentation set produced for the upcoming
> consultancy / training session that uses HexaStock as a teaching asset for
> **DDD + Hexagonal Architecture + Spring Modulith + Domain Events**. It complements,
> but does not replace, the broader [`doc/consultancy/`](../) briefing pack and the
> [`doc/architecture/`](../../architecture/) reference material.
>
> **Branch.** All material lives on `feature/modulith-watchlists-extraction`. Nothing
> here ships to `main`.

---

## File index

| # | File | Type | Purpose |
|---|---|---|---|
| 0 | [README.md](README.md) | Index | This map and reading order. |
| 1 | [00-ARCHITECTURE-OVERVIEW.md](00-ARCHITECTURE-OVERVIEW.md) | Main doc | What HexaStock is, business capabilities, the four architectural styles in play, why the combination makes sense here. |
| 2 | [01-FILESYSTEM-AND-MAVEN-STRUCTURE.md](01-FILESYSTEM-AND-MAVEN-STRUCTURE.md) | Main doc | Filesystem layout, Maven multi-module, package structure, how Maven modules / packages / Modulith modules relate. |
| 3 | [02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md](02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md) | Main doc | End-to-end walk-through of `WatchlistAlertTriggeredEvent`, why it is the canonical teaching example. |
| 4 | [03-LAYOUT-ALTERNATIVES.md](03-LAYOUT-ALTERNATIVES.md) | Main doc | Rigorous comparison of the current layout against four common alternatives. |
| 5 | [04-PRODUCTION-EVOLUTION.md](04-PRODUCTION-EVOLUTION.md) | Main doc | Realistic evolution paths beyond the current in-process domain events: outbox, externalisation, eventual extraction. |
| 6 | [05-INSTRUCTOR-GUIDE.md](05-INSTRUCTOR-GUIDE.md) | Teaching | Practical guide for running the Monday session: agenda, talking points, demo sequence, discussion prompts, exercise flow. |
| 7 | [06-SLIDE-DECK-SPEC.md](06-SLIDE-DECK-SPEC.md) | Teaching | Structured, AI-feedable slide-by-slide specification. |
| 8 | [diagrams/](diagrams/) | Sources | 11 PlantUML sources for this folder, each rendered to PNG and SVG under `diagrams/Rendered/`. |

The Sell Stocks event-driven exercise also gets three new diagrams — they live with the
existing tutorial under [`doc/tutorial/sellStocks/diagrams/`](../../tutorial/sellStocks/diagrams/),
following the convention already established in that folder:

| File | Purpose |
|---|---|
| [diagrams/sell-events-current.puml](../../tutorial/sellStocks/diagrams/sell-events-current.puml) | Sequence — current synchronous sell flow. |
| [diagrams/sell-events-target.puml](../../tutorial/sellStocks/diagrams/sell-events-target.puml) | Sequence — target event-driven sell flow. |
| [diagrams/sell-events-conceptual.puml](../../tutorial/sellStocks/diagrams/sell-events-conceptual.puml) | Conceptual — one business fact, many reactions. |

The exercise document itself, [SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](../../tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md),
is updated in-place to embed the three rendered diagrams.

---

## Diagram index

All diagram sources are PlantUML. Each `.puml` has a sibling `Rendered/<name>.svg` and
`Rendered/<name>.png` produced by [`scripts/render-diagrams.sh`](../../../scripts/render-diagrams.sh)
(Docker image `plantuml/plantuml:latest`).

| # | Source | Rendered |
|---|---|---|
| 1 | [01-architecture-overview.puml](diagrams/01-architecture-overview.puml) | [PNG](diagrams/Rendered/01-architecture-overview.png) · [SVG](diagrams/Rendered/01-architecture-overview.svg) |
| 2 | [02-maven-multimodule.puml](diagrams/02-maven-multimodule.puml) | [PNG](diagrams/Rendered/02-maven-multimodule.png) · [SVG](diagrams/Rendered/02-maven-multimodule.svg) |
| 3 | [03-filesystem-layout.puml](diagrams/03-filesystem-layout.puml) | [PNG](diagrams/Rendered/03-filesystem-layout.png) · [SVG](diagrams/Rendered/03-filesystem-layout.svg) |
| 4 | [04-modulith-modules.puml](diagrams/04-modulith-modules.puml) | [PNG](diagrams/Rendered/04-modulith-modules.png) · [SVG](diagrams/Rendered/04-modulith-modules.svg) |
| 5 | [05-bounded-context-map.puml](diagrams/05-bounded-context-map.puml) | [PNG](diagrams/Rendered/05-bounded-context-map.png) · [SVG](diagrams/Rendered/05-bounded-context-map.svg) |
| 6 | [06-hexagonal-view.puml](diagrams/06-hexagonal-view.puml) | [PNG](diagrams/Rendered/06-hexagonal-view.png) · [SVG](diagrams/Rendered/06-hexagonal-view.svg) |
| 7 | [07-watchlist-sentinel-sequence.puml](diagrams/07-watchlist-sentinel-sequence.puml) | [PNG](diagrams/Rendered/07-watchlist-sentinel-sequence.png) · [SVG](diagrams/Rendered/07-watchlist-sentinel-sequence.svg) |
| 8 | [08-watchlist-event-flow.puml](diagrams/08-watchlist-event-flow.puml) | [PNG](diagrams/Rendered/08-watchlist-event-flow.png) · [SVG](diagrams/Rendered/08-watchlist-event-flow.svg) |
| 9 | [09-notification-flow.puml](diagrams/09-notification-flow.puml) | [PNG](diagrams/Rendered/09-notification-flow.png) · [SVG](diagrams/Rendered/09-notification-flow.svg) |
| 10 | [10-layout-alternatives.puml](diagrams/10-layout-alternatives.puml) | [PNG](diagrams/Rendered/10-layout-alternatives.png) · [SVG](diagrams/Rendered/10-layout-alternatives.svg) |
| 11 | [11-current-vs-future-events.puml](diagrams/11-current-vs-future-events.puml) | [PNG](diagrams/Rendered/11-current-vs-future-events.png) · [SVG](diagrams/Rendered/11-current-vs-future-events.svg) |
| S1 | [sell-events-current.puml](../../tutorial/sellStocks/diagrams/sell-events-current.puml) | [PNG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-current.png) · [SVG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-current.svg) |
| S2 | [sell-events-target.puml](../../tutorial/sellStocks/diagrams/sell-events-target.puml) | [PNG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-target.png) · [SVG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-target.svg) |
| S3 | [sell-events-conceptual.puml](../../tutorial/sellStocks/diagrams/sell-events-conceptual.puml) | [PNG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-conceptual.png) · [SVG](../../tutorial/sellStocks/diagrams/Rendered/sell-events-conceptual.svg) |

To regenerate everything from the repository root:

```bash
./scripts/render-diagrams.sh
```

---

## Recommended reading order (for the instructor)

Read in this order to prepare for Monday — total approx. 90 minutes:

1. **[00-ARCHITECTURE-OVERVIEW.md](00-ARCHITECTURE-OVERVIEW.md)** — anchor the mental model.
2. **[01-FILESYSTEM-AND-MAVEN-STRUCTURE.md](01-FILESYSTEM-AND-MAVEN-STRUCTURE.md)** — be ready to explain *where things live and why*.
3. **[02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md](02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md)** — the demo case.
4. **[03-LAYOUT-ALTERNATIVES.md](03-LAYOUT-ALTERNATIVES.md)** — the part where senior engineers will push back; have the trade-offs internalised.
5. **[04-PRODUCTION-EVOLUTION.md](04-PRODUCTION-EVOLUTION.md)** — the “what would we do at real scale?” conversation.
6. **[../../tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](../../tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md)** — the participant exercise.
7. **[05-INSTRUCTOR-GUIDE.md](05-INSTRUCTOR-GUIDE.md)** — agenda and timing.
8. **[06-SLIDE-DECK-SPEC.md](06-SLIDE-DECK-SPEC.md)** — feed into your slide tool of choice.

---

## Cross-references to existing material

- Higher-level briefing chapters: [`doc/consultancy/`](../) (DDD, Hexagonal, Modulith, Domain Events deep dive, Domain Events roadmap, cheatsheet).
- Bounded-context inventory and migration history: [`doc/architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md`](../../architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md).
- Architectural decision records: [`doc/architecture/adr/`](../../architecture/adr/).
- Hexagonal layering reference diagrams: [`doc/tutorial/sellStocks/HEXAGONAL-ARCHITECTURE-REFERENCE-DIAGRAMS.md`](../../tutorial/sellStocks/HEXAGONAL-ARCHITECTURE-REFERENCE-DIAGRAMS.md).
- Ubiquitous language: [`doc/tutorial/sellStocks/UBIQUITOUS-LANGUAGE.md`](../../tutorial/sellStocks/UBIQUITOUS-LANGUAGE.md).
