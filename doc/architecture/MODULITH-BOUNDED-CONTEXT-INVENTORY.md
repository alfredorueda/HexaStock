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

> *To be filled in during Phase 4 of the global refactoring plan.*

---

## 3. Watchlists / Market Sentinel *(promoted, partial — Phase 5 hardening)*

**Promoted package**: `cat.gencat.agaur.hexastock.watchlists` (publisher side only — currently only `WatchlistAlertTriggeredEvent`).

> *Pending classes still under `model.watchlist`, `application.port.*`, `application.service.MarketSentinelService` will be inventoried during Phase 5.*

---

## 4. Notifications *(promoted)*

**Promoted package**: `cat.gencat.agaur.hexastock.notifications` (Maven module `adapters-outbound-notification`).

Already complete; see [SPRING-MODULITH-NOTIFICATIONS-POC.md](SPRING-MODULITH-NOTIFICATIONS-POC.md) for details.

---

## 5. Reporting / Performance Analysis *(deferred — optional Phase 5 of the global plan)*

> *Currently lives inside Portfolio (`HoldingPerformanceCalculator`, `ReportingService`). Will only be extracted if the read side justifies CQRS.*
