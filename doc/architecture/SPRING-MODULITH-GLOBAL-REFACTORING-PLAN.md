# Spring Modulith Global Refactoring Plan

> **Status**: Proposal — read-only architectural plan. No production code changes are part of this document.
> **Branch**: `feature/experimental-spring-modulith-notifications`
> **Baseline**: post-POC state, including the experimental `watchlists` (publisher) and `notifications` (consumer) Modulith modules introduced by the Watchlist Alert Notifications POC.
> **Author**: Architecture working group.
> **Audience**: Maintainers, students, contributors.

---

## Table of contents

1. [Executive summary](#1-executive-summary)
2. [Current architecture assessment](#2-current-architecture-assessment)
3. [Candidate Bounded Contexts](#3-candidate-bounded-contexts)
4. [Spring Modulith vs Hexagonal vs Maven vs DDD — the relationship](#4-spring-modulith-vs-hexagonal-vs-maven-vs-ddd--the-relationship)
5. [Recommended Spring Modulith module map](#5-recommended-spring-modulith-module-map)
6. [File and package structure options](#6-file-and-package-structure-options)
7. [Incremental migration roadmap](#7-incremental-migration-roadmap)
8. [Concrete first safe implementation step](#8-concrete-first-safe-implementation-step)
9. [Testing strategy](#9-testing-strategy)
10. [Documentation strategy](#10-documentation-strategy)
11. [Risks, trade-offs, and constraints honoured](#11-risks-trade-offs-and-constraints-honoured)
12. [Open questions and final recommendation](#12-open-questions-and-final-recommendation)

---

## 1. Executive summary

Spring Modulith is adopted **selectively and incrementally** on top of the existing hexagonal Maven structure — not as a wholesale repackaging. The recommended end state:

1. The current **Maven multi-module hexagonal layout** (`domain` / `application` / `adapters-*` / `bootstrap`) remains the **physical** dependency-enforcement boundary.
2. Spring Modulith is layered on top as a **logical** boundary: one top-level Java package per Bounded Context, mirroring the pattern already proven by the POC (`watchlists`, `notifications`).
3. Bounded Contexts are migrated **one at a time**. Legacy `model.*`, `application.*`, `adapter.*` packages stay until each BC is fully extracted, preserving `git blame`, books, and tutorial references.
4. Modulith verification (`ApplicationModules.verify()`) is scoped to the migrated BC packages only, never to the legacy hexagonal layered ones (the POC empirically demonstrated that mechanically applying global verification to the legacy packages produces noise and false positives).
5. Inter-module communication uses **in-process domain events** — no Kafka, no microservices, no external broker.

The Notifications POC is the canonical template. Replicate its discipline (events as the only inter-module contract, scoped Modulith verification, full test coverage including event-flow integration tests) for every subsequent Bounded Context.

**Recommended approach**: Option C (hybrid incremental). **Recommended first step**: Phase 0 — publish this plan and render baseline Modulith documenter snapshots. No code moves.

---

## 2. Current architecture assessment

### Maven modules (post-POC)

| Module | Role | Spring? |
|---|---|---|
| `domain` | Pure POJO aggregates, value objects, domain exceptions. Includes `Watchlist` aggregate after the POC. | No |
| `application` | Ports (`port.in`, `port.out`), use case services, the new `watchlists` package containing `WatchlistAlertTriggeredEvent` + `package-info.java`. | No |
| `adapters-inbound-rest` | Spring MVC controllers + DTOs + RFC 7807 problem details. | Yes |
| `adapters-inbound-telegram` | Telegram bot inbound handler (introduced with the watchlist feature). | Yes |
| `adapters-outbound-persistence-jpa` | JPA persistence for `PortfolioPort`, `TransactionPort`, `WatchlistPort`, `WatchlistQueryPort`. | Yes |
| `adapters-outbound-persistence-mongodb` | Mongo equivalents under a different profile. | Yes |
| `adapters-outbound-market` | `StockPriceProviderPort` implementations (Finnhub / AlphaVantage / mock). | Yes |
| `adapters-outbound-notification` | POC notifications module: listener, recipient resolver, channel adapters (Telegram, Logging). | Yes |
| `bootstrap` | Composition root, `SpringAppConfig`, `RetryOnWriteConflictAspect`, `SpringDomainEventPublisher`, `@Modulithic` declaration, integration + ArchUnit + Modulith verification tests. | Yes |

### Top-level Java packages under `cat.gencat.agaur.hexastock`

- **Legacy "technical layer" packages** (predate Modulith adoption):
  - `model.*` — domain aggregates, value objects, exceptions.
  - `application.*` — split into `port.in`, `port.out`, `service`, `annotation`, `exception`.
  - `adapter.in.*`, `adapter.out.*` — adapter implementations.
  - `config.*` — Spring configuration.
- **New Bounded Context packages** (introduced by the POC):
  - `watchlists` — publisher side: `WatchlistAlertTriggeredEvent`, `package-info.java`.
  - `notifications` — consumer side: listener, senders, recipient resolver, adapters.
- **Modulith metadata**: `@Modulithic(systemName = "HexaStock")` on `HexaStockApplication`. **No `@ApplicationModule` annotations declared yet** — modules are inferred from top-level packages.

### Where the architecture is already clean

- The `domain` module is pristine and framework-free; ArchUnit enforces it.
- Ports/adapters direction is enforced both at Maven (compile-time) and ArchUnit (runtime test) levels.
- The `application` module is Spring-free; the Spring adapter for events lives in `bootstrap` (`SpringDomainEventPublisher`).
- Profile-gated outbound adapters (JPA vs Mongo, Finnhub vs AlphaVantage vs mock, Telegram vs Logging) provide a clean substitution pattern.

### Where Modulith would clearly help

- Crystallizing the boundary between Watchlists/Market Sentinel (publisher) and Notifications (consumer) — already demonstrated by the POC.
- Future split of Reporting (CQRS read side) and Market Data refresh / caching as their own Modulith modules.
- Making event listener semantics (after-commit + asynchronous) declarative via `@ApplicationModuleListener`.

### Where Modulith would HURT if applied mechanically

The POC empirically validated these failure modes:

- A global `ApplicationModules.verify()` on the legacy `model` / `application` / `adapter` / `config` packages produces dozens of "non-exposed type" errors because those packages do not follow Modulith's named-interface convention and were never designed as cross-module APIs.
- Forcing every Maven module to be a Modulith module would **invert the abstraction**: Maven modules currently express **technical layers**, while Modulith modules should express **business capabilities**. The two axes must remain orthogonal.

---

## 3. Candidate Bounded Contexts

Pragmatic short list with explicit "now vs later" recommendations:

| BC | Aggregates / Concepts | Modulith module now? | Notes |
|---|---|---|---|
| **Portfolio Management** | `Portfolio`, `Holding`, `Lot`, `HoldingPerformance`; `PortfolioPort`; lifecycle/cash/stock-operation use cases | **Later (Phase 3)** | Currently lives under `model.portfolio` + `application.service`. Premature extraction risks unstable boundary. |
| **Trading / Order Execution** | Buy/Sell orchestration, `SellResult`, FIFO lot consumption, `@RetryOnWriteConflict` | **Later (Phase 3, with Portfolio)** | Currently inseparable from the Portfolio aggregate. Likely stays inside the Portfolio module unless true asynchronous orders are introduced. |
| **Transactions / Ledger** | `Transaction` sealed hierarchy, `TransactionPort` | **Later (Phase 3, with Portfolio)** | Append-only ledger driven by Portfolio events. Could later become its own module subscribing to `MoneyDeposited`, `StockBought`, `StockSold`. |
| **Market Data** | `StockPrice`, `Ticker`, `StockPriceProviderPort`, Caffeine cache | **Phase 4** | Extract as a `marketdata` module exposing a single port. Isolates external API volatility. |
| **Watchlists / Market Sentinel** | `Watchlist`, `AlertEntry`, `WatchlistQueryPort`, `MarketSentinelService`, `WatchlistAlertTriggeredEvent` | **Already a Modulith module (POC)** | Stabilize the package surface in Phase 1. |
| **Notifications** | `WatchlistAlertNotificationListener`, `NotificationSender`, recipient resolver, channel adapters | **Already a Modulith module (POC)** | Promote to first-class in Phase 1. |
| **Reporting / Performance Analysis** | `HoldingPerformanceCalculator`, `ReportingService` | **Phase 5 (optional)** | Becomes its own read-side module fed by Portfolio events only if read load justifies CQRS. |
| **User / Investor Preferences** | not yet modelled | **Not now** | Premature. The POC will eventually justify a `notification preferences` slice driven by inbound events; out of scope here. |

---

## 4. Spring Modulith vs Hexagonal vs Maven vs DDD — the relationship

These four axes are **orthogonal** and must remain so. The plan never collapses one into another.

- **Maven module** = physical compile-time separation of **technical layers** (domain, application, adapters, bootstrap).
- **Hexagonal layer** = a technical role inside the application (domain, application, adapter), enforced by Maven boundaries and ArchUnit.
- **Spring Modulith application module** = a top-level Java package under the application's base package, grouping Spring beans by **business capability**. Modulith only enforces dependencies between modules at the type level and only on Spring-managed code paths it can see.
- **Bounded Context (DDD)** = a linguistic and lifecycle boundary around aggregates, ports, and services that share a ubiquitous language.

Key clarifications:

- A Modulith module is **NOT** equivalent to a Maven module. A single Modulith module (e.g. `watchlists`) may have its publisher code in `application` and its consumer code in `adapters-outbound-notification`.
- A Modulith module is **NOT** a hexagonal layer. Hexagonal layers slice by technical role; Modulith modules slice by business capability.
- A Modulith module **≈** a (pragmatic) Bounded Context. The strong CTM connotation ("separate database, separate team, separate deploy") is deliberately avoided because this is still a modular monolith.
- **Ports** remain owned by `application`; their concrete implementations remain in `adapters-outbound-*` Maven modules. **Each implementation moves into the BC top-level package** (e.g. `adapters-outbound-persistence-jpa/.../portfolios/` once Portfolio becomes a Modulith module). We do **not** create one Maven module per BC.
- **Dependency direction is enforced on three levels**:
  1. Maven (compile-time).
  2. ArchUnit (the existing layer + zero-deps rules).
  3. Spring Modulith (`ApplicationModules.verify()` scoped to BC packages, plus `@ApplicationModule(allowedDependencies = …)` once declared).
- **Avoiding anaemic technical packages**: each BC package contains its own `port.in`, `port.out`, `service`, and `internal` subpackages — not just a `dto` or `util` namespace. Modulith named interfaces (`@NamedInterface`) export only the intentional API surface.

---

## 5. Recommended Spring Modulith module map

End-state target (after several phases — not now):

```
cat.gencat.agaur.hexastock
├── shared/         (Money, Ticker, ShareQuantity, common exceptions — Modulith "open" module)
├── portfolios/     (Portfolio aggregate, ports, services, JPA + Mongo adapters slices)
├── marketdata/     (StockPrice, provider port, adapters, caching)
├── watchlists/     (Watchlist aggregate, sentinel service, query port, event publisher)
├── notifications/  (listener, senders, recipient resolver, telegram + logging adapters)
├── reporting/      (HoldingPerformanceCalculator, reporting use case — optional, Phase 5)
└── platform/       (RetryOnWriteConflictAspect, SpringDomainEventPublisher, SpringAppConfig)
```

Each business package contains:

- `port.in` — driving ports (use case interfaces).
- `port.out` — driven ports (SPI for adapters).
- `internal` — Modulith default-internal subpackage for implementation details.
- `events` — published domain events, the only inter-module API surface.

The `shared` module is declared as a Modulith **open module** (any module may depend on it). The `platform` module collects technical infrastructure that crosses business boundaries (retry aspect, event publisher, Spring configuration). Both are exceptions to the "one BC per top-level package" rule and are documented as such.

---

## 6. File and package structure options

### Option A — Conservative

Keep Maven layout untouched. Use Spring Modulith **only at package level inside `application`** (BC top-level packages) and inside specific outbound adapter modules. Legacy `model`, `adapter` packages remain.

- ✅ Lowest churn; teaches Modulith without uprooting hexagonal.
- ✅ Easy to revert.
- ❌ Leaves a confusing dual taxonomy (technical packages vs BC packages) indefinitely.
- ❌ Modulith never reaches its full expressive power.

### Option B — Big-bang business-first repackage

Repackage the entire codebase by Bounded Context inside each Maven module, dropping the technical `model` / `application` / `adapter` package roots in a single PR.

- ✅ Cleanest end state.
- ❌ Massive churn; breaks every `git blame`, every reference in books and tutorials, every external link.
- ❌ Unshippable in increments.
- ❌ High risk of introducing regressions undetectable by the existing test suite.

### Option C — Hybrid, incremental ⭐ **RECOMMENDED**

Preserve the Maven multi-module structure. Introduce BC top-level packages **one at a time**, **alongside** the legacy ones. Each migration phase moves the classes of one BC into its new package and removes the corresponding shells from the legacy packages. Modulith verification is scoped to the migrated BC packages only.

- ✅ Keeps tutorials and books readable (legacy packages stay until each BC migrates).
- ✅ Each phase is independently shippable and rollback-able as a single feature branch.
- ✅ Aligns with the POC pattern (already proven).
- ⚠️ Dual taxonomy lasts a few phases; documented in this plan and in the architecture spec.

---

## 7. Incremental migration roadmap

| Phase | Goal | Scope | Notes |
|---|---|---|---|
| **Phase 0 — Baseline & docs** | Snapshot the current architecture. | Add this document. Update existing architecture docs to reference the Modulith POC. Render `Documenter` snapshots into `doc/architecture/diagrams/modulith/`. | No code change. |
| **Phase 1 — Stabilize Watchlists / Notifications boundary** | Promote the POC modules from "experimental" to first-class. | Add `package-info.java` with `@ApplicationModule(allowedDependencies = …)`. Add `@NamedInterface` for events. Strengthen Modulith verification tests. | Tiny PR per package. |
| **Phase 2 — Modulith documentation in build** | Make the module canvas part of the build. | Add a `Documenter` test that fails the build if rendering fails. Commit the rendered diagrams via a script under `scripts/`. | Educational value. |
| **Phase 3 — Portfolio BC extraction** | Introduce the `portfolios` Modulith module. | Move `Portfolio`, `Holding`, `Lot`, `Transaction`, related ports, services, mappers, JPA + Mongo adapters into `portfolios.*` subpackages **inside their existing Maven modules**. Rename `StockPriceProviderPort` references to a forward-declared `MarketDataPort`. | High value, medium risk. ArchUnit rules updated to recognize new package roots. |
| **Phase 4 — Market Data BC extraction** | Carve out `marketdata`. | Move `StockPrice`, `Ticker`, provider port + adapters + cache config into `marketdata.*`. Portfolio depends on Market Data only via its named interface. | Medium effort. |
| **Phase 5 — Reporting (optional)** | Introduce `reporting` if and only if the read side justifies CQRS. | Move `HoldingPerformanceCalculator` and reporting use case into `reporting.*`. Subscribe to Portfolio events. | Optional; do not do prematurely. |
| **Phase 6 — Global Modulith verification** | Replace package-by-package scoped verification with a global one. | `ApplicationModules.of(HexaStockApplication.class).verify()` should pass. `@ApplicationModule(allowedDependencies = …)` declared everywhere. | Marks "Modulith adoption complete". |
| **Phase 7 — Documentation & ADRs** | Update README, technical spec, GitBook. | Add module diagrams. Add ADR-016 (Spring Modulith adoption) and ADR-017 (in-process inter-BC events). | Pedagogical milestone. |
| **Phase 8 — Maven module consolidation (deferred)** | Decide whether to fold each adapter Maven module into per-BC submodules. **Default: do nothing.** | Out of scope unless independent deployability becomes a real requirement. | Explicitly NOT recommended now. |

---

## 8. Concrete first safe implementation step

The first PR after this proposal is approved should be **Phase 0 only**:

1. Add this document at `doc/architecture/SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md` (already done by this PR).
2. Cross-link it from `README.md` and `doc/architecture/TECHNICAL-ARCHITECTURE-SPECIFICATION.md`.
3. Cross-link it from the existing POC document (`SPRING-MODULITH-NOTIFICATIONS-POC.md` or its equivalent under `doc/`).
4. **Do NOT move any existing class.** No Java code is repackaged. No `package-info.java` is edited. No Documenter wiring yet (deferred to Phase 2).

This step is reversible by deleting the new files; no production behaviour changes.

Phase 1 (stabilizing the POC packages) and Phase 2 (Documenter wiring) follow in separate PRs once Phase 0 is merged and reviewed.

---

## 9. Testing strategy

To minimize brittleness during refactoring:

- **Domain unit tests** stay where they are. They continue to import from the old package paths until each BC moves.
- **Application service tests** continue to use Mockito + plain JUnit; no Spring context.
- **Adapter contract tests** (`AbstractPortfolioPortContractTest`, etc.) remain unchanged. They are the safety net during repackaging.
- **Modulith verification tests** are added per BC, scoped to the BC packages only (the pattern already used in `ModulithVerificationTest`). A single global verification test is introduced **only at Phase 6**.
- **`@ApplicationModuleTest`** is reserved for genuine module-slice tests once the legacy hexagonal packages have been migrated. Until then, full `@SpringBootTest` is used (as in the POC).
- **ArchUnit** rules are updated **per phase**, never preemptively, so they always match the current code shape.
- **Event publication tests** use Spring's `ApplicationEvents` recorder (Modulith-friendly) where possible, falling back to Mockito spies on listeners (the POC pattern) when full async-after-commit semantics need to be exercised.
- **Coverage** is measured per Maven module to ensure no module silently regresses during the move. JaCoCo aggregates already exist per module.

---

## 10. Documentation strategy

- **README.md** — add a short "Modulith POC + roadmap" section linking to the POC doc and this plan.
- **TECHNICAL-ARCHITECTURE-SPECIFICATION.md** — add a "Modulith adoption" section explaining the four orthogonal axes (Maven, hexagonal, Modulith, DDD).
- **SPRING-MODULITH-NOTIFICATIONS-POC.md** — cross-link from this global plan.
- **SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md** — this document.
- **Diagrams** — render Modulith canvas + per-BC components under `doc/architecture/diagrams/modulith/`. Use the existing render scripts under `scripts/`.
- **GitBook structure** — add a new chapter "Spring Modulith inside HexaStock" with three sub-pages:
  - "DDD perspective" — Bounded Contexts and aggregates.
  - "Hexagonal perspective" — ports and adapters across modules.
  - "Modulith perspective" — packages, named interfaces, events, verification tests.
- **Tutorial pages** — a step-by-step walkthrough mirroring the POC, suitable for students.
- **ADRs** — ADR-016 ("Adopt Spring Modulith for cross-module communication") and ADR-017 ("Use in-process domain events for inter-BC notification"). Both deferred to Phase 7.

Explicit framing for students:

- This is **still a modular monolith**, not microservices.
- **Kafka is not introduced** because all events are in-process.
- **Spring Modulith reinforces** rather than replaces hexagonal architecture.

---

## 11. Risks, trade-offs, and constraints honoured

### Risks and trade-offs

- **Dual taxonomy** (legacy `model`/`application`/`adapter` packages alongside new BC packages) for several phases. *Mitigation*: explicit document, time-boxed phases, ArchUnit guard against new code in legacy packages once a BC has been migrated.
- **ArchUnit rules drift** — rules must be edited on every phase. *Mitigation*: treat ArchUnit edits as part of each phase's PR.
- **Modulith named-interface enforcement** can be aggressive once enabled globally. *Mitigation*: introduce `@NamedInterface` only when a BC is fully extracted; until then, scope verification per package.
- **Risk of overengineering** by promoting too many BCs (e.g. extracting Reporting before its read-side load justifies CQRS). *Mitigation*: explicit phase ordering with optional phases marked as such.
- **Maven module proliferation** (one per BC) is explicitly out of scope.
- **Spring Modulith version**: keep at 1.4.0 (already adopted by the POC). No Spring Boot upgrade needed.

### Constraints honoured

- No Spring Boot upgrade.
- No Kafka, no microservices, no external broker.
- No merge into `main`.
- Existing Maven multi-module structure preserved.
- `application` Maven module remains Spring-free.
- No reduction in test coverage; all existing tests keep passing.
- Pedagogical clarity preserved (legacy packages stay until migrated; book/tutorial references stay valid).
- ADRs ADR-003 (hexagonal), ADR-004 (rich DDD), ADR-006 (multi-module Maven), ADR-007 (domain zero deps), ADR-010 (ArchUnit), ADR-015 (explicit bean wiring) all remain valid; this plan complements them.

---

## 12. Open questions and final recommendation

### Open questions for review

1. Should Phase 1 ("stabilize Watchlists/Notifications") happen on the same experimental branch or be split into a new branch derived from it?
2. Should the Reporting BC (Phase 5) be elevated to mandatory, or kept as optional?
3. Confirm: no Maven module consolidation (Phase 8 stays explicitly deferred).
4. Should the `shared` and `platform` modules be introduced in Phase 1 (as empty placeholders for documentation purposes) or only when they have content?

### Final recommendation

Adopt **Option C** (hybrid incremental). Execute Phases 0–2 immediately (low risk, high clarity). Defer Phase 3+ until each Bounded Context is independently understood and explicitly approved. The Notifications POC is the template — replicate its discipline (events as the only inter-module contract, Modulith verification scoped per BC, full test coverage including event-flow integration tests) for every subsequent BC.

This plan deliberately avoids commitment to the more aggressive Phases 5, 6, and 8. They remain on the roadmap as conditional milestones, to be revisited only when the prerequisite phases have been delivered and observed in production-like conditions.
