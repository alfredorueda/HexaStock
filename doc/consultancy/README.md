# HexaStock — Consultancy Briefing Pack

> **Audience.** Engineering leadership and senior practitioners attending the upcoming consultancy session on the HexaStock financial-portfolio platform.
>
> **Scope.** This briefing pack distils the architectural rationale behind HexaStock — Domain-Driven Design (DDD), Hexagonal Architecture, Spring Modulith, and in-process domain events — into a single, internally consistent narrative grounded in the production code of the project.
>
> **Branch policy.** All consultancy material lives on the experimental branch
> `feature/modulith-watchlists-extraction`. None of these documents touches `main`.

---

## How to read this pack

The pack is organised as five substantive chapters and one condensed cheat-sheet. The chapters build on each other; readers familiar with DDD and Hexagonal Architecture may skim Chapters 1–2 and concentrate on the Modulith and Domain Events material in Chapters 3–5.

| # | Document | Purpose | Suggested reading time |
|---|---|---|---|
| 1 | [01-DDD-IN-HEXASTOCK.md](01-DDD-IN-HEXASTOCK.md) | Strategic and tactical DDD as instantiated in HexaStock: the four bounded contexts, the aggregates, the ubiquitous language. | 25 min |
| 2 | [02-HEXAGONAL-ARCHITECTURE.md](02-HEXAGONAL-ARCHITECTURE.md) | Ports-and-adapters at the Maven-module level: why the hexagon survives Modulith and why it is the *enabler* of Modulith. | 20 min |
| 3 | [03-SPRING-MODULITH.md](03-SPRING-MODULITH.md) | Spring Modulith as the runtime enforcement layer for bounded contexts: `@ApplicationModule`, `@NamedInterface`, `MODULES.verify()`, the dependency graph. | 30 min |
| 4 | [04-DOMAIN-EVENTS-DEEP-DIVE.md](04-DOMAIN-EVENTS-DEEP-DIVE.md) | The end-to-end anatomy of the only domain event currently in production — `WatchlistAlertTriggeredEvent` — covering authoring, publication, transactional semantics and consumption. | 30 min |
| 5 | [05-DOMAIN-EVENTS-ROADMAP.md](05-DOMAIN-EVENTS-ROADMAP.md) | A forward-looking catalogue of further domain events the platform will benefit from — including `LotSoldEvent`, `PortfolioOpenedEvent`, `CashWithdrawnEvent` — with use cases, payload schemas and consumer sketches. | 30 min |
| C | [CHEATSHEET.md](CHEATSHEET.md) | A one-page revision aid for the day of the consultancy. | 10 min |

A small number of cross-cutting diagrams referenced from these documents live alongside the existing architecture material under [`doc/architecture/`](../architecture/) and [`doc/diagrams/`](../diagrams/).

---

## Provenance and authority

Every claim in the pack is traceable to a concrete artefact in the repository:

- The four bounded contexts are declared as Spring Modulith application modules in
  [bootstrap/.../portfolios/package-info.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/portfolios/package-info.java),
  [bootstrap/.../marketdata/package-info.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/marketdata/package-info.java),
  [bootstrap/.../watchlists/package-info.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/watchlists/package-info.java) and
  [adapters-outbound-notification/.../notifications/package-info.java](../../adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/package-info.java).
- The single in-flight domain event is
  [WatchlistAlertTriggeredEvent.java](../../application/src/main/java/cat/gencat/agaur/hexastock/watchlists/WatchlistAlertTriggeredEvent.java).
- Its publisher abstraction is
  [DomainEventPublisher.java](../../application/src/main/java/cat/gencat/agaur/hexastock/application/port/out/DomainEventPublisher.java); its in-process Spring adapter is `SpringDomainEventPublisher`.
- Its transactional consumer is
  [WatchlistAlertNotificationListener.java](../../adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/WatchlistAlertNotificationListener.java).
- Modulith boundaries are enforced by [ModulithVerificationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/ModulithVerificationTest.java); hexagonal layering by [HexagonalArchitectureTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java).
- The historical, sequenced extraction record is in [MODULITH-BOUNDED-CONTEXT-INVENTORY.md](../architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md).

When in doubt, the source code is the source of truth; the documents in this pack only summarise and explain it.
