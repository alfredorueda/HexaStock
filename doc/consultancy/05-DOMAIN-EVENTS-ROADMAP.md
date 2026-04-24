# 5. Domain Events — Forward Roadmap

> **Purpose.** This chapter catalogues the additional domain events whose introduction would extend the same pattern documented in Chapter 4 to the rest of the platform. Each event is presented with: business motivation, payload schema, publication site, plausible consumers, transactional semantics and the architectural verifications that would have to be added or amended.
>
> No code in this chapter exists in the repository today. The chapter is forward-looking and intended to support the consultancy session's *"where does this go next?"* discussion.

## 5.1 Selection criteria

A new event is justified when *all* of the following hold:

1. **Business significance.** The event represents a fact that a domain expert would name and care about — not a technical artefact such as "row inserted".
2. **Multiple plausible reactions.** At least two distinct cross-cutting concerns would consume the fact (audit, projection, notification, integration). A fact with a single, tightly-coupled consumer is better expressed as a synchronous call.
3. **Logical autonomy of producer and consumer.** The producer must remain meaningful even if no consumer ever reacts.

Each event below satisfies the three criteria.

## 5.2 `LotSoldEvent` — *the keystone of the roadmap*

### 5.2.1 Business motivation

When a user sells a quantity of shares, FIFO accounting consumes one or more `Lot` entities — possibly partially. Every individual lot consumption is a *fact* with downstream business significance:

- **Realised gain reporting.** A tax-aware reporting module needs the per-lot realised gain (proceeds − cost basis) on the day the lot was depleted, *not* on the day a periodic report is generated.
- **Position monitoring.** A risk-monitoring module may want to know the moment a holding's remaining quantity falls below a configured threshold.
- **External brokerage reconciliation.** A future integration with a brokerage API would benefit from a per-lot trace.
- **Audit and regulatory compliance.** Financial regulations frequently require an immutable per-trade ledger; a `LotSoldEvent` stream is the natural source for it.

A single `Portfolio.sell(...)` invocation may emit multiple `LotSoldEvent`s — one per lot consumed — plus one *aggregate-level* `StockSoldEvent` (Section 5.3) summarising the operation.

### 5.2.2 Proposed payload

```java
package cat.gencat.agaur.hexastock.portfolios;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;

import java.time.Instant;
import java.time.LocalDateTime;

public record LotSoldEvent(
        String portfolioId,
        String userId,
        Ticker ticker,
        String lotId,
        ShareQuantity quantitySold,
        Price unitCostBasis,         // price the lot was bought at
        Price unitSalePrice,         // price the sale was executed at
        Money realisedGain,          // (unitSalePrice − unitCostBasis) × quantitySold
        boolean lotFullyDepleted,    // true if remainingShares == 0 after this sale
        LocalDateTime lotPurchasedAt,
        Instant occurredOn
) { /* compact constructor with Objects.requireNonNull on all required fields */ }
```

### 5.2.3 Publication site

The publication site is the application service [PortfolioStockOperationsService.sellStock(...)](../../application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/PortfolioStockOperationsService.java).

The aggregate's `Portfolio.sell(...)` method already returns a `SellResult` summarising the operation. To preserve the *aggregates emit events as a side effect of behaviour* discipline (Vernon, IDDD), `Portfolio.sell(...)` should be modified to *also* return — or *register internally* — the per-lot consumption records. The application service then publishes them through `DomainEventPublisher` after `portfolioPort.save(portfolio)` succeeds.

Two stylistic options exist:

- **Option A — `SellResult` carries an event list.** The aggregate returns `SellResult { Money proceeds, Money profit, List<LotConsumption> consumptions }`, and the application service iterates the consumptions and publishes one `LotSoldEvent` each.
- **Option B — Aggregate-internal event collection.** The aggregate accumulates raised events in a private list and the application service drains and publishes them. This is closer to Vernon's "register events as a side effect" idiom, at the cost of mutable state inside the aggregate.

HexaStock's preference is **Option A**, because the existing aggregate is already a value-returning style and `SellResult` is the natural extension point.

### 5.2.4 Plausible consumers

| Module (current or future) | Reaction |
|---|---|
| **`reporting`** *(future, §5 of the BC inventory)* | Append the realised gain to a per-user, per-tax-year ledger projection. |
| **`watchlists`** *(existing)* | If the user has a *position-size alert* on the same ticker, re-evaluate it. |
| **`audit`** *(future)* | Append to an immutable trade-history ledger backed by a write-once store. |
| **`integrations.brokerage`** *(future)* | Translate to the brokerage's "trade confirmation" message and dispatch. |

### 5.2.5 Transactional semantics

`@ApplicationModuleListener` on every consumer; the publishing transaction commits the `Portfolio` mutation atomically with the in-memory event publication record (Spring Modulith's *event publication registry*); each listener fires after commit, on its own thread, in its own `REQUIRES_NEW` transaction. A failing audit append does not rollback the sale; it is registered for re-delivery instead.

### 5.2.6 Modulith additions

The `portfolios` `@ApplicationModule` would be amended to expose a named interface for the event package — for instance `@NamedInterface("events")` on `cat.gencat.agaur.hexastock.portfolios.events` — and consumer modules would declare `allowedDependencies = {..., "portfolios::events"}`. A new ArchUnit assertion (`portfoliosEventsAreImmutableRecords`) would verify that every type in `portfolios.events` is a `record` with no setters.

### 5.2.7 Why this is the *first* event to introduce next

Because it is the densest in business value: it underpins reporting, audit, brokerage integration *and* exhibits the canonical "one aggregate operation, multiple emitted events" pattern that is otherwise absent from HexaStock today.

## 5.3 `StockSoldEvent` and `StockBoughtEvent`

### 5.3.1 Business motivation

In addition to the per-lot facts of Section 5.2, the *aggregate-level* outcome of a buy or sell — "user X bought 50 AAPL at $150" or "user X sold 30 GOOG for $9,000 with $1,200 realised gain" — has independent significance: it is what populates a user's transaction feed, what a trading-volume monitor reacts to, and what an *integration* with an analytics warehouse consumes.

### 5.3.2 Proposed payloads

```java
public record StockBoughtEvent(
        String portfolioId, String userId,
        Ticker ticker,
        ShareQuantity quantity,
        Price unitPrice,
        Money totalCost,
        Instant occurredOn
) { }

public record StockSoldEvent(
        String portfolioId, String userId,
        Ticker ticker,
        ShareQuantity quantity,
        Price unitPrice,
        Money proceeds,
        Money realisedGain,
        Instant occurredOn
) { }
```

### 5.3.3 Publication

The application service publishes one `StockBoughtEvent` (resp. `StockSoldEvent`) immediately after the corresponding aggregate operation succeeds and before any per-lot events. The order matters when an external system reconstructs the timeline.

### 5.3.4 Plausible consumers

| Module | Reaction |
|---|---|
| **`watchlists`** | Trigger a *position-opened* or *position-closed* alert family (currently HexaStock supports only price thresholds; this would extend the catalogue). |
| **`reporting`** | Maintain per-day, per-user trading-volume aggregates. |
| **`notifications`** | Send a transaction confirmation if the user has opted in (currently only watchlist alerts produce notifications). |

### 5.3.5 Why bought *and* sold?

Because the asymmetry between buy and sell — buys consume cash, sells produce realised gains — is significant for downstream consumers; coalescing them into a single `TradeExecutedEvent` would force every consumer to switch on a `direction` enum and discard half the payload. Two events is cheaper.

## 5.4 `PortfolioOpenedEvent` and `PortfolioClosedEvent`

### 5.4.1 Business motivation

Portfolio creation and closure are infrequent but high-significance events:

- A *welcome notification* should be sent on opening.
- A *seed projection* in the reporting module should be initialised on opening.
- A *closure audit record* should be written on closure.
- A future *retention analytics* module would track the median lifetime of a portfolio.

### 5.4.2 Payloads

```java
public record PortfolioOpenedEvent(
        String portfolioId, String userId, String ownerName, Instant occurredOn) { }

public record PortfolioClosedEvent(
        String portfolioId, String userId,
        Money finalBalance, ShareQuantity finalDistinctHoldings, Instant occurredOn) { }
```

### 5.4.3 Publication

`PortfolioLifecycleService.openPortfolio(...)` publishes `PortfolioOpenedEvent` after `portfolioPort.save(portfolio)` succeeds; the (yet-to-be-introduced) `closePortfolio(...)` publishes `PortfolioClosedEvent` likewise.

## 5.5 `CashDepositedEvent` and `CashWithdrawnEvent`

### 5.5.1 Business motivation

Cash movements that happen outside a buy or sell — explicit deposits and withdrawals — are the simplest form of financial event and the natural feeder for:

- A *cash-balance alert* in Watchlists (e.g., "warn me when balance drops below $1,000"), which currently does not exist.
- An *AML / KYC* monitor that flags large or unusual movements (a future regulatory module).
- A *daily-balance-snapshot* projection in reporting.

### 5.5.2 Payloads

```java
public record CashDepositedEvent(
        String portfolioId, String userId, Money amount,
        Money newBalance, Instant occurredOn) { }

public record CashWithdrawnEvent(
        String portfolioId, String userId, Money amount,
        Money newBalance, Instant occurredOn) { }
```

### 5.5.3 Publication

The publication site is the application service that wraps `Portfolio.deposit(...)` and `Portfolio.withdraw(...)`. As with `LotSoldEvent`, the aggregate operation is unchanged in semantics; the application service publishes after `save`.

## 5.6 `WatchlistAlertSilencedEvent`

### 5.6.1 Business motivation

The current Watchlists module emits one `WatchlistAlertTriggeredEvent` *every time* the price condition holds, with no notion of "this alert has already fired in the last hour, do not repeat it". Operationally this is a defect: a high-frequency price oscillation around the threshold produces a notification storm.

A `WatchlistAlertSilencedEvent`, paired with a future `silencedUntil` field on `AlertEntry`, would let the Watchlists module inform the Notifications module — and a future *alert-storm dashboard* — that an alert has been suppressed and why.

### 5.6.2 Payload

```java
public record WatchlistAlertSilencedEvent(
        String watchlistId, String userId, Ticker ticker,
        Instant silencedFrom, Instant silencedUntil, String reason) { }
```

### 5.6.3 Publication

Inside `MarketSentinelService.detectBuySignals()`, alongside the existing publication of `WatchlistAlertTriggeredEvent`. The two events would be siblings in the same `watchlists` published-events package.

## 5.7 Cross-cutting observations

Several invariants apply to *every* event in the roadmap and should be codified as architecture tests as soon as the second event is introduced:

1. **Records, not classes.** All events are Java `record`s. An ArchUnit rule (`onlyRecordsLiveInTheEventsPackage`) makes the rule explicit.
2. **No infrastructure imports.** No event imports anything from `org.springframework.*`, `com.fasterxml.jackson.*` or `jakarta.persistence.*`. ADR-007 generalised.
3. **Business identity, not transport identity.** No event carries a `chatId`, `email`, `phoneNumber`, `slackWebhookUrl` or comparable transport identifier. The recipient resolution is the consumer's responsibility.
4. **Past tense.** Event names are past tense (`LotSoldEvent`, not `SellLotCommand`). This distinguishes events from commands at first glance.
5. **`Instant occurredOn`.** Every event carries the moment it was raised, in UTC. Listeners that reorder or replay events depend on this.

## 5.8 Suggested introduction order

1. **`LotSoldEvent` + `StockSoldEvent` + `StockBoughtEvent`** — high business value, exercises the "one aggregate operation, multiple emitted events" pattern, and unlocks the `reporting` and `audit` modules.
2. **`CashDepositedEvent` + `CashWithdrawnEvent`** — straightforward, and unlocks balance-monitoring features in Watchlists.
3. **`PortfolioOpenedEvent` + `PortfolioClosedEvent`** — useful for analytics and welcome flows; lowest urgency.
4. **`WatchlistAlertSilencedEvent`** — accompanies the introduction of alert silencing in Watchlists; pure capability addition.

Each of the four steps can be delivered on a dedicated experimental branch, in the same incremental style used for the bounded-context extractions.

## 5.9 Closing observation

The single in-flight event in HexaStock — `WatchlistAlertTriggeredEvent` — is the proof of concept. The roadmap above is the *productionisation* of the same pattern across the rest of the platform. None of the proposed events requires a message broker, a new infrastructure component or a change in the hexagonal layout. They each require:

- one Java `record` in an `events` sub-package of the publishing module,
- one or two new lines in an existing application service to call `eventPublisher.publish(...)`,
- one or more `@ApplicationModuleListener` consumers in the reacting modules,
- updated `allowedDependencies` and (where appropriate) `@NamedInterface` declarations,
- one new ArchUnit / Modulith assertion per event family.

This is, by design, a low-friction extension path — and it is exactly the property that justifies the upfront investment in the hexagonal Maven topology and the Spring Modulith verifications.
