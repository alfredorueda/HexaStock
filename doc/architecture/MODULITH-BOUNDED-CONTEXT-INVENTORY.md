# Spring Modulith — Bounded Context Inventory

> **Status**: living inventory document. Updated phase by phase as part of the Spring Modulith global refactoring (see [SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md](SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md)).
> **Branch**: `feature/experimental-spring-modulith-notifications`
> **Purpose**: maps each candidate Bounded Context to the legacy classes that currently implement it, so future Phase 3+ extractions can be performed mechanically and reviewably without surprise.

This document is **read-only inventory**. It does not move any code. It is the input for any future package-level extraction PR.

---

## Legend

- **Promoted**: this BC already lives in its own top-level Spring Modulith package and is verified by `ModulithVerificationTest`.
- **Pending**: this BC has been catalogued but its classes are still under the legacy hexagonal package roots (`model.*`, `application.*`, `adapter.*`, `config.*`).
- **Move target**: the future top-level package this BC will move to when extracted (Phase 3+).

---

## 1. Portfolio Management *(promoted, full extraction — completed on `feature/modulith-portfolio-extraction`)*

**Promoted package**: `cat.gencat.agaur.hexastock.portfolios`

**Why a single Modulith module rather than three**: the Portfolio aggregate, its FIFO Trading orchestration, and the append-only Transaction ledger are mutually inseparable in the current model. Splitting them now would either duplicate the FIFO invariants across modules or force synchronous cross-module calls. They are recorded here as one Bounded Context for the Modulith refactoring; the DDD discussion of whether Trading or Transactions could ever become independent BCs is documented in `doc/architecture/DDD-HEXAGONAL-ONION-CLEAN-COMPARATIVE-STUDY.md` and is out of scope for the Modulith POC.

**Extraction status (this branch)**:

- All 5 layers (domain, application, REST adapter, JPA adapter, Mongo adapter) moved from their legacy packages into `cat.gencat.agaur.hexastock.portfolios.*` over five small commits, each ending in a green `./mvnw clean test`.
- `bootstrap/.../portfolios/package-info.java` declares `@ApplicationModule(displayName = "Portfolio Management", allowedDependencies = {})`.
- `MODULE_PACKAGES` in `ModulithVerificationTest` now contains `portfolios`. New tests assert it is detected and has no outgoing dependencies on other promoted modules.
- Test infrastructure classes `TestWebApplication`, `TestJpaApplication`, and `TestMongoApplication` were hoisted to `cat.gencat.agaur.hexastock` (the common parent) and made `public` so `@SpringBootConfiguration` auto-discovery still works for tests living under both legacy and `portfolios.*` package roots.

### Domain (Maven module: `domain`)

| Current package | Classes |
|---|---|
| `model.portfolio` | `Portfolio`, `PortfolioId`, `Holding`, `HoldingId`, `HoldingPerformance`, `HoldingPerformanceCalculator`, `Lot`, `LotId`, `SellResult`, `EntityExistsException`, `HoldingNotFoundException`, `InsufficientFundsException`, `ConflictQuantityException` |
| `model.transaction` | `Transaction` (sealed), `DepositTransaction`, `WithdrawalTransaction`, `PurchaseTransaction`, `SaleTransaction`, `TransactionId`, `TransactionType` |

### Application (Maven module: `application`)

| Current package | Classes |
|---|---|
| `application.port.in` | `PortfolioLifecycleUseCase`, `PortfolioStockOperationsUseCase`, `CashManagementUseCase`, `TransactionUseCase`, `ReportingUseCase` |
| `application.port.out` | `PortfolioPort`, `TransactionPort` |
| `application.service` | `PortfolioLifecycleService`, `PortfolioStockOperationsService`, `CashManagementService`, `TransactionService`, `ReportingService` |
| `application.annotation` | `RetryOnWriteConflict` (Phase 3 will keep this in `platform/`, not in `portfolios`) |

### Adapters (Maven modules: `adapters-inbound-rest`, `adapters-outbound-persistence-jpa`, `adapters-outbound-persistence-mongodb`)

| Maven module | Current package roots | Notes |
|---|---|---|
| `adapters-inbound-rest` | `adapter.in.controller.*`, `adapter.in.dto.*` (portfolio/transaction-related controllers) | Phase 3 splits these by BC into `portfolios/adapter/in/...`. |
| `adapters-outbound-persistence-jpa` | `adapter.out.persistence.jpa.*` (entities, mappers, Spring Data repositories, port adapters for `PortfolioPort` and `TransactionPort`) | Move into `portfolios/adapter/out/persistence/jpa/...`. |
| `adapters-outbound-persistence-mongodb` | `adapter.out.persistence.mongodb.*` (Mongo equivalents) | Move into `portfolios/adapter/out/persistence/mongodb/...`. |

### Cross-cutting infrastructure used by Portfolio

- `config.RetryOnWriteConflictAspect` (will remain in `platform/`, not in `portfolios/`).
- `bootstrap.SpringAppConfig` portfolio-related bean wiring.

### What Portfolio depends on

- **Reads** stock prices via `MarketDataPort` (Market Data BC, exposed as the `marketdata::port-out` named interface).
- **Publishes** no Modulith events yet. Future evolution (out of scope for this POC) could publish `MoneyDeposited`, `StockBought`, `StockSold` events consumed by Reporting.

### Constraints when extracting (Phase 3)

- Domain module remains zero-dependency (ADR-007).
- ArchUnit `HexagonalArchitectureTest` rules will need to recognize `portfolios.*` as a valid root for domain classes alongside `model.portfolio.*`.
- All existing `JpaPortfolioRepositoryContractTest`, `JpaTransactionRepositoryContractTest`, and Mongo equivalents must keep passing without modification.

---

## 2. Market Data *(promoted, full extraction — completed on `feature/modulith-marketdata-extraction`)*

**Final package**: `cat.gencat.agaur.hexastock.marketdata`

**Status**: Promoted to a Spring Modulith application module. `StockPriceProviderPort` was renamed to `MarketDataPort` in the same PR. All five layers moved under `marketdata.*`. Three `@NamedInterface` declarations expose the cross-module API surface (`marketdata::model`, `marketdata::port-in`, `marketdata::port-out`). `MODULES.verify()` enforces the boundary.

### Extraction outcome

| Layer | Final package |
|---|---|
| Domain | `marketdata.model.market` (`Ticker`, `StockPrice`, `InvalidTickerException`) — exposed as `marketdata::model` named interface |
| Application — primary port | `marketdata.application.port.in.GetStockPriceUseCase` — exposed as `marketdata::port-in` |
| Application — secondary port | `marketdata.application.port.out.MarketDataPort` (renamed from `StockPriceProviderPort`) — exposed as `marketdata::port-out` |
| Application — service | `marketdata.application.service.GetStockPriceService` |
| Inbound REST | `marketdata.adapter.in.{StockRestController,StockPriceDTO}` |
| Outbound REST | `marketdata.adapter.out.rest.{Finhub,AlphaVantage,MockFinhub}StockPriceAdapter` |

### Cross-cutting infrastructure (intentionally NOT moved)

- Caffeine cache configuration in `bootstrap` (`@EnableCaching`, `CacheManager` bean) — remains a generic infrastructure concern.
- Spring profiles `finnhub`, `alphavantage`, `mockfinhub` — profile activation logic stays in `bootstrap`.

### Final dependency graph

- **Market Data dependencies (outgoing)**: none — leaf module.
- **Consumers**:
  - `portfolios` declares `allowedDependencies = {"marketdata::model", "marketdata::port-out"}` (`PortfolioStockOperationsService`, `ReportingService`, REST/persistence mappers).
  - `watchlists` declares `allowedDependencies = {"marketdata::model"}` (`WatchlistAlertTriggeredEvent` carries a `Ticker`).
  - `notifications` declares `allowedDependencies = {"watchlists", "marketdata::model"}` (renders `Ticker` into outbound messages).

### Note on `MarketDataPort` exposure

`MarketDataPort` is a *secondary* port, normally an internal concern. It is exposed as a named interface because Portfolio services inject it directly for batch price lookups (`fetchStockPrice(Set<Ticker>)`). A future refactor may promote that batch operation to a primary use case and remove this named interface.

---

## 3. Watchlists / Market Sentinel *(promoted, full extraction — completed on `feature/modulith-watchlists-extraction`)*

**Promoted package**: `cat.gencat.agaur.hexastock.watchlists` (full module — aggregate, ports, services, REST + JPA + Mongo adapters, plus the published `WatchlistAlertTriggeredEvent`).

### Extraction outcome

| Maven module | New package |
|---|---|
| `domain` | `watchlists.model.watchlist` (Watchlist, WatchlistId, AlertEntry, AlertNotFoundException, DuplicateAlertException) |
| `application` | `watchlists.application.port.in` (WatchlistUseCase, MarketSentinelUseCase), `watchlists.application.port.out` (WatchlistPort, WatchlistQueryPort, TriggeredAlertView), `watchlists.application.service` (WatchlistService, MarketSentinelService), and the published event `watchlists.WatchlistAlertTriggeredEvent` |
| `adapters-inbound-rest` | `watchlists.adapter.in.*` (WatchlistRestController, WatchlistDTOs) |
| `adapters-outbound-persistence-jpa` | `watchlists.adapter.out.persistence.jpa.*` (entities, mapper, repositories, spring-data repositories, contract tests) |
| `adapters-outbound-persistence-mongodb` | `watchlists.adapter.out.persistence.mongodb.*` (documents, mapper, repositories, spring-data repository, contract tests) |

The Telegram inbound adapter (`adapters-inbound-telegram`) lives in its own Modulith module and consumes `watchlists.application.port.in.WatchlistUseCase` from outside; it is not part of the `watchlists` package tree.

### Cross-cutting infrastructure (intentionally NOT moved)

- `application.port.out.DomainEventPublisher` — generic event-publishing port shared by every BC. Stays in the legacy `application.port.out` namespace as a platform abstraction.

### Modulith verification (still green after the move)

- The `Watchlist` aggregate is channel-agnostic. Its class javadoc documents that notification routing concerns (Telegram chat ids, emails, phone numbers) are NOT modeled here.
- The published event `WatchlistAlertTriggeredEvent` carries only business data (`watchlistId`, `userId`, `ticker`, `alertType`, `threshold`, `currentPrice`, `occurredOn`, `message`). No transport identifiers.
- `watchlistsHasNoOutgoingModuleDependencies` asserts watchlists' only cross-module dependency is `marketdata` (the `Ticker` value object embedded in the published event).
- `notificationsOnlyDependsOnWatchlists` asserts notifications' cross-module dependencies are exactly `{watchlists, marketdata}`.

### Final dependency graph (with watchlists fully extracted)

- `marketdata` — leaf, zero outgoing dependencies.
- `portfolios` — depends on `marketdata` (via `MarketDataPort` and the `Ticker` / `StockPrice` value objects).
- `watchlists` — depends on `marketdata::model` (`Ticker` carried in `WatchlistAlertTriggeredEvent`).
- `notifications` — depends on `watchlists` (consumes `WatchlistAlertTriggeredEvent`) and on `marketdata::model` (`Ticker` rendering inside outbound notification messages).

---

## 4. Notifications *(promoted, hardened — Phase 5)*

**Promoted package**: `cat.gencat.agaur.hexastock.notifications` (Maven module `adapters-outbound-notification`).

**Phase 5 hardening (this branch)**:

- Replaced the channel-aware `InMemoryNotificationRecipientResolver` with a channel-agnostic `CompositeNotificationRecipientResolver`.
- Introduced `NotificationDestinationProvider` SPI: one implementation per channel, profile-gated.
  - `LoggingNotificationDestinationProvider` — always active.
  - `TelegramNotificationDestinationProvider` — only under `@Profile("telegram-notifications")`.
- Telegram configuration leakage eliminated: when the Telegram profile is OFF, the resolver no longer reads `notifications.telegram.chat-ids` and the listener no longer warns about absent Telegram senders.
- Module package annotated with `@ApplicationModule(allowedDependencies = {"watchlists"})`.
- Channel-specific senders live under internal subpackages (`adapter/logging`, `adapter/telegram`) which are not part of any Modulith named interface.

See [SPRING-MODULITH-NOTIFICATIONS-POC.md](SPRING-MODULITH-NOTIFICATIONS-POC.md) for the full design discussion.

---

## 5. Reporting / Performance Analysis *(deferred — optional Phase 5 of the global plan)*

> *Currently lives inside Portfolio (`HoldingPerformanceCalculator`, `ReportingService`). Will only be extracted if the read side justifies CQRS.*
