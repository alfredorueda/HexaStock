# 3. Spring Modulith in HexaStock

> **Reading prerequisites.** Familiarity with the Spring Modulith reference documentation (Drotbohm, *Spring Modulith Reference Guide*) is assumed. This chapter focuses on how the project applies the technology, not on re-stating the documentation.

## 3.1 The problem Modulith solves

The hexagonal Maven topology (Chapter 2) prevents framework leakage but does not prevent *bounded-context leakage*. Two services that both live in `application/src/main/java/...` can reach into each other's packages and create accidental coupling that no Maven dependency graph will catch. As the platform grows from two to four to eight bounded contexts, the cost of this kind of leakage rises super-linearly.

Spring Modulith addresses this by treating each bounded context as a *runtime-verified* application module: a top-level package under the application's base package, optionally annotated with `@ApplicationModule(allowedDependencies = {...})` and `@NamedInterface("...")` declarations on its sub-packages. A test that calls `ApplicationModules.of(Application.class).verify()` then proves that no class in any module reaches into another module's *internal* packages and that all explicit `allowedDependencies` are respected.

In HexaStock the substrate is sound — the hexagon already exists — so Modulith adds the *cross-context dimension* on top of it.

## 3.2 The four application modules

The current promoted set is:

| Module | Base package | Allowed dependencies | Role |
|---|---|---|---|
| `portfolios` | `cat.gencat.agaur.hexastock.portfolios` | `marketdata::model`, `marketdata::port-in`, `marketdata::port-out` | Core: portfolio aggregate, lifecycle and trading use cases |
| `marketdata` | `cat.gencat.agaur.hexastock.marketdata` | *(none — leaf)* | Supporting: price look-ups, ticker model |
| `watchlists` | `cat.gencat.agaur.hexastock.watchlists` | `marketdata::model` | Supporting: watchlist aggregate, alert detection, event publisher |
| `notifications` | `cat.gencat.agaur.hexastock.notifications` | `watchlists`, `marketdata::model` | Generic: event-driven notification dispatch |

Each of these is declared in a `package-info.java` whose location respects ADR-007 (Spring annotations stay out of `application/`). The `marketdata` declaration, for example, lives in [bootstrap/.../marketdata/package-info.java](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/marketdata/package-info.java) rather than in the `application` Maven module.

## 3.3 `@NamedInterface` and the published-API discipline

Spring Modulith's default rule is *the base package is the published API; everything in sub-packages is internal*. That default does not fit a hexagonal codebase, where the published API is naturally split across `model`, `port.in` and (in carefully justified cases) `port.out`.

The Market Data module solves this by declaring three named interfaces:

- [marketdata.model.market](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/marketdata/model/market/package-info.java) → `@NamedInterface("model")` exposing `Ticker`, `StockPrice`, `InvalidTickerException`.
- [marketdata.application.port.in](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/marketdata/application/port/in/package-info.java) → `@NamedInterface("port-in")` exposing `GetStockPriceUseCase`.
- [marketdata.application.port.out](../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/marketdata/application/port/out/package-info.java) → `@NamedInterface("port-out")` exposing `MarketDataPort`. This last one is a deliberate, documented exception — secondary ports are not normally exposed across module boundaries, but `MarketDataPort` is consumed by Portfolio Management for batched price look-ups that the primary port does not yet offer. The named interface keeps `MODULES.verify()` green; a future refactor will promote a richer primary port and remove this exposure.

Other modules then refer to the slice they consume: Portfolio Management's `allowedDependencies = {"marketdata::model", "marketdata::port-in", "marketdata::port-out"}` makes it explicit what it depends on.

## 3.4 Cross-module communication: synchronous or event-based

There are exactly two legitimate ways for one Modulith module to communicate with another in HexaStock:

1. **Synchronous call through a published primary port.** Portfolio Management calls `GetStockPriceUseCase` from Market Data when it needs an up-to-date price for a buy or sell. The call is type-safe, in-process, and verified by `MODULES.verify()`.
2. **Asynchronous in-process domain event.** Watchlists publishes `WatchlistAlertTriggeredEvent`; Notifications consumes it via `@ApplicationModuleListener`. The publisher knows nothing of the consumer; the consumer knows only of the event type and the publishing module name.

The asymmetry is intentional. Synchronous calls are appropriate for *queries* (read-only requests for information that would not otherwise be available). Events are appropriate for *facts* (something happened that other modules may wish to react to). The two patterns map cleanly onto Vernon's distinction between *queries* and *events* in *Implementing Domain-Driven Design*.

## 3.5 The verification suite

[ModulithVerificationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/ModulithVerificationTest.java) runs three classes of assertions:

1. **Generic verification.** A single call to `MODULES.verify()` proves that no internal package is referenced from outside its module, no module declares an undeclared dependency, and the module graph is acyclic.
2. **Per-module shape assertions.** Each promoted module is individually checked to ensure its base package ends with the expected name and its dependency set matches the documented contract — for instance, `marketdataHasNoOutgoingModuleDependencies` proves that Market Data is a leaf, and `notificationsOnlyDependsOnWatchlists` proves that Notifications depends only on `watchlists` and `marketdata::model`.
3. **Documentation generation.** `ApplicationModules.of(Application.class).createDocumentation().writeDocumentation()` produces, on every CI build, a fresh AsciiDoc + PlantUML rendering of the module graph. This is the single source of truth for cross-cutting architectural diagrams in the project.

## 3.6 Why the hexagonal Maven layout is preserved under Modulith

Section 2.4 made the case that the Maven topology and the Modulith topology are orthogonal. From the Modulith side, the same conclusion is reached for two additional reasons:

1. **Modulith does not enforce ADR-007.** Without the `application` Maven module's framework-free classpath, application services would gradually accumulate `@Service`, `@Transactional` and direct `ApplicationEventPublisher` injections. The hexagon prevents this.
2. **Modulith verification is a positive guarantee, not a test of completeness.** It proves that the dependency arrows that exist are legal; it cannot prove that the right *layers* of a context are exposed at the right boundaries. The hexagonal layering supplies that complementary structure.

The two technologies are therefore best deployed together, and HexaStock is a small but credible reference for the combination.

## 3.7 The current state of the verification

Running the full suite — `./mvnw clean verify -DskipITs` — completes in approximately 70 seconds and produces:

- 61 unit tests across 10 reactor modules,
- a passing `MODULES.verify()` for all four application modules,
- an updated module-graph diagram under `bootstrap/target/spring-modulith-docs/`,
- an aggregated JaCoCo report covering domain, application and adapter layers.

This is the baseline against which any new bounded-context extraction or domain-event introduction must continue to hold.
