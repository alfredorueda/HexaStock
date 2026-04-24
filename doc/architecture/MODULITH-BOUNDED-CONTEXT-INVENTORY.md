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

## 1. Portfolio Management *(pending — Phase 3)*

**Move target package**: `cat.gencat.agaur.hexastock.portfolios`

**Why a single Modulith module rather than three**: the Portfolio aggregate, its FIFO Trading orchestration, and the append-only Transaction ledger are mutually inseparable in the current model. Splitting them now would either duplicate the FIFO invariants across modules or force synchronous cross-module calls. They are recorded here as one Bounded Context for the Modulith refactoring; the DDD discussion of whether Trading or Transactions could ever become independent BCs is documented in `doc/architecture/DDD-HEXAGONAL-ONION-CLEAN-COMPARATIVE-STUDY.md` and is out of scope for the Modulith POC.

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

- **Reads** stock prices via `StockPriceProviderPort` (Market Data BC). After Phase 4 this becomes `MarketDataPort`.
- **Publishes** no Modulith events yet. Future evolution (out of scope for this POC) could publish `MoneyDeposited`, `StockBought`, `StockSold` events consumed by Reporting.

### Constraints when extracting (Phase 3)

- Domain module remains zero-dependency (ADR-007).
- ArchUnit `HexagonalArchitectureTest` rules will need to recognize `portfolios.*` as a valid root for domain classes alongside `model.portfolio.*`.
- All existing `JpaPortfolioRepositoryContractTest`, `JpaTransactionRepositoryContractTest`, and Mongo equivalents must keep passing without modification.

---

## 2. Market Data *(pending — Phase 4)*

**Move target package**: `cat.gencat.agaur.hexastock.marketdata`

**Why a Modulith module**: Market Data is a clearly delimited supporting capability. Its only responsibility is to convert a `Ticker` into a `StockPrice`, hiding the volatility of upstream providers (Finnhub, AlphaVantage, mock). Both Portfolio Management (for buy/sell pricing and reporting) and Watchlists / Market Sentinel (for threshold polling) depend on it via a single outbound port. Promoting it to a Modulith module makes that single dependency direction explicit and lets the Caffeine cache and provider switching live behind a stable named interface.

### Domain (Maven module: `domain`)

| Current package | Classes |
|---|---|
| `model.market` | `StockPrice`, `Ticker`, `InvalidTickerException` |

### Application (Maven module: `application`)

| Current package | Classes |
|---|---|
| `application.port.in` | `GetStockPriceUseCase` |
| `application.port.out` | `StockPriceProviderPort` (rename to `MarketDataPort` deferred to Phase 4 PR) |
| `application.service` | `GetStockPriceService` |

### Adapters (Maven module: `adapters-outbound-market`)

| Current package | Classes |
|---|---|
| `adapter.out.rest` | `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter`, `MockFinhubStockPriceAdapter` |

### Inbound (Maven module: `adapters-inbound-rest`)

| Current package | Classes |
|---|---|
| `adapter.in.controller` | `StockPriceController` (and DTOs) |

### Cross-cutting infrastructure

- Caffeine cache configuration in `bootstrap` (Spring `@EnableCaching` + `CacheManager` bean) — will remain in `platform/` because it is a generic infrastructure concern.
- Spring profiles: `finnhub`, `alphavantage`, `mockfinhub` — profile activation logic stays in `bootstrap`.

### What Market Data depends on

- Nothing internal to HexaStock. Pure outbound to external HTTP APIs (or to a fake when `mockfinhub` is active).

### What depends on Market Data (current consumers)

- `application.service.MarketSentinelService` (Watchlists / Market Sentinel BC) — calls `StockPriceProviderPort.fetchStockPrice(Set<Ticker>)`.
- `application.service.PortfolioStockOperationsService` and `ReportingService` (Portfolio Management BC) — call `StockPriceProviderPort.fetchStockPrice(Ticker)`.
- The `WatchlistAlertTriggeredEvent` published by Watchlists also imports `model.market.Ticker`. After Phase 4, this means `watchlists` will have a documented Modulith dependency on `marketdata::events` (or on `shared::market` if `Ticker` is promoted to a shared kernel).

### Constraints when extracting (Phase 4)

- Adapters under `adapters-outbound-market` must remain isolated — they are the only place where Finnhub / AlphaVantage SDK or HTTP code may live.
- Existing WireMock-based adapter tests must keep passing without changes.
- The Caffeine caching annotation-based wiring (`@Cacheable` on `GetStockPriceService` or on the port adapters) must be preserved verbatim during the move.
- A renaming of `StockPriceProviderPort` to `MarketDataPort` is recommended but optional and reversible; if performed, all call sites in `application.service.MarketSentinelService`, `PortfolioStockOperationsService`, and `ReportingService` must be updated in the same PR.

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
