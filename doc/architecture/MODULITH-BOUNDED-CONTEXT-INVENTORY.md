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

## 3. Watchlists / Market Sentinel *(promoted, partial — Phase 5 hardened)*

**Promoted package**: `cat.gencat.agaur.hexastock.watchlists` (publisher side only — currently only `WatchlistAlertTriggeredEvent`).

**Phase 5 verification (this branch)**:

- The `Watchlist` aggregate (`domain/.../model/watchlist/Watchlist.java`) is channel-agnostic. The class javadoc explicitly documents that notification routing concerns (Telegram chat ids, emails, phone numbers) are NOT modeled here.
- The published event `WatchlistAlertTriggeredEvent` carries only business data (`watchlistId`, `userId`, `ticker`, `alertType`, `threshold`, `currentPrice`, `occurredOn`, `message`). No transport identifiers.
- The Modulith verification test asserts:
  - `watchlists` has zero outgoing cross-module dependencies (`watchlistsHasNoOutgoingModuleDependencies`).
  - `notifications` only depends on `watchlists` for the published event type (`notificationsOnlyDependsOnWatchlists`).

### Pending classes (move target: same `watchlists` package, future phase)

| Maven module | Current package | Classes |
|---|---|---|
| `domain` | `model.watchlist` | `Watchlist`, `WatchlistId`, `AlertEntry`, `AlertNotFoundException`, `DuplicateAlertException` |
| `application` | `application.port.in` | `WatchlistUseCase`, `MarketSentinelUseCase` |
| `application` | `application.port.out` | `WatchlistPort`, `WatchlistQueryPort`, `TriggeredAlertView` |
| `application` | `application.service` | `WatchlistService`, `MarketSentinelService` |
| `adapters-inbound-rest` | `adapter.in.controller` | watchlist-related controllers + DTOs |
| `adapters-inbound-telegram` | (entire module) | Telegram bot inbound handler that creates watchlists |
| `adapters-outbound-persistence-jpa` | `adapter.out.persistence.jpa.*` | watchlist entity / mapper / repository / port adapter |
| `adapters-outbound-persistence-mongodb` | `adapter.out.persistence.mongodb.*` | Mongo equivalents |

These will be moved into `watchlists.adapter.in.*` / `watchlists.adapter.out.*` subpackages of their respective Maven modules in a future phase. Phase 5 does not move them.

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
