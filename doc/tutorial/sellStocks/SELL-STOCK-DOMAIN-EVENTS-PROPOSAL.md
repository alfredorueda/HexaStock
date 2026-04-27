# Proposal — Domain Events for *Sell Stocks*

> **Status.** Design proposal, not yet implemented. Strictly experimental branch.
> **Branch.** `feature/modulith-watchlists-extraction` (HEAD `f60fc98`).
> **Scope.** Refactor `PortfolioStockOperationsService.sellStock(...)` to publish a domain event after the sale commits, and move the recording of the `SaleTransaction` to a Spring Modulith listener — *intra-module*, in-process, modular monolith. No Kafka, no broker, no microservice, no distributed anything.
> **Conceptual background.** Eric Evans, *Domain-Driven Design* — chapters cited inline by short name (e.g. *DDD §5*). The proposal is meant to be defensible against the rules in chapters 5 (model expression), 6 (life cycle / aggregates), 9 (implicit concepts), 14 (BOUNDED CONTEXTS / module integrity) and 15 (distillation).

---

## 0. TL;DR

- `StockSoldEvent` **is** a true domain event in Evans's sense (a past business fact: *"a sale happened"*), not a notification, not a command.
- The current `sellStock(...)` mixes **the sale** (mutates `Portfolio`) with **the audit projection** (writes `SaleTransaction`). DDD says these are two concerns living in different *consistency boundaries*. Moving the second one to a listener is sound.
- **Recommended Phase 1:** publish `StockSoldEvent` from the application service *after* `savePortfolio(...)`; a `@ApplicationModuleListener` (intra-module, AFTER_COMMIT, REQUIRES_NEW, async) consumes it and writes the `SaleTransaction` via the existing `TransactionPort`.
- **Caveat (this is the critical bit, not glossed over):** today's behaviour is *atomic*. If the listener fails after commit, the user sees the sale on the `Portfolio` but no row in `Transaction`. Acceptable **only if** Spring Modulith's persistent **Event Publication Registry** is enabled (so the publication is durable and re-delivered on restart) **and** the listener is idempotent. Without that, you trade atomicity for decoupling — a step backwards. Phase 1 must therefore include the persistent registry; otherwise stay synchronous in the same TX.
- The listener lives in the **same Modulith module** (`portfolios`) — `Transaction` is a `portfolios` aggregate, not somebody else's concern.
- Don't add `LotSoldEvent`, `Notification`, `TaxReporting`, `Cache invalidation` etc. on day one. They're *enabled* by this refactor; they're not part of it.

---

## 1. Current architecture analysis

### 1.1 The flow today (as of `f60fc98`)

[`PortfolioStockOperationsService.sellStock(...)`](application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/PortfolioStockOperationsService.java#L137-L154):

```java
@Override
@RetryOnWriteConflict
public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
    Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

    StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
    Price price = stockPrice.price();

    SellResult sellResult = portfolio.sell(ticker, quantity, price);   // ① domain operation
    portfolioPort.savePortfolio(portfolio);                            // ② persist aggregate

    Transaction transaction = Transaction.createSale(                  // ③ build audit record
            portfolioId, ticker, quantity, price, sellResult.proceeds(), sellResult.profit());
    transactionPort.save(transaction);                                 // ④ persist audit record

    return sellResult;
}
```

The whole method runs inside a single JTA `@Transactional` boundary, with `@RetryOnWriteConflict` on top (relevant later — it implies **the entire body can run more than once**).

### 1.2 Inventory of responsibilities

| # | Step | Owns it today | Layer | Module | Aggregate |
|---|---|---|---|---|---|
| ① | `portfolio.sell(...)` — FIFO lot consumption, share quantity invariant, cash credit | `Portfolio` | domain | `portfolios` | **Portfolio** (root) |
| ② | `portfolioPort.savePortfolio(...)` | service | application | `portfolios` | Portfolio |
| ③ | `Transaction.createSale(...)` factory | service (calls factory on `Transaction` interface) | domain factory + application | `portfolios` | **Transaction** (separate) |
| ④ | `transactionPort.save(tx)` | service | application | `portfolios` | Transaction |
| return | `SellResult` to caller | service | — | — | — |

### 1.3 Aggregates and consistency boundaries

There are **two aggregates** in `portfolios`:

- **`Portfolio`** (root) — owns `Holding`, `Lot`. Invariants: cash sufficiency, share availability, FIFO ordering. See [`Portfolio.java`](domain/src/main/java/cat/gencat/agaur/hexastock/portfolios/model/portfolio/Portfolio.java).
- **`Transaction`** (root) — sealed hierarchy, append-only ledger. The Javadoc on [`Transaction`](domain/src/main/java/cat/gencat/agaur/hexastock/portfolios/model/transaction/Transaction.java) is explicit about *why* it is its own aggregate:
  > *"Transaction history is append-only and unbounded. Including it in the Portfolio aggregate would force loading all historical transactions for every operation. Transactions do not participate in the invariants that the Portfolio aggregate protects."*

This is straight out of *DDD §6* (AGGREGATES): when invariants do not bind two clusters, they should be separate aggregates. The current code **already acknowledges** that `Portfolio` and `Transaction` are separate aggregates — but then breaks the rule of one-aggregate-per-transaction by writing both atomically inside the same DB transaction. That's a pragmatic compromise, not a logical necessity.

### 1.4 Could the audit record be decoupled?

Yes. Three independent observations:

1. **No invariant ties them.** Nothing in `Portfolio` reads or constrains `Transaction`. The `SaleTransaction` is *derived* from the outcome of `portfolio.sell(...)` — it carries a snapshot, not a constraint.
2. **Write directions are independent.** `Transaction` is append-only; `Portfolio` is mutable. Crashing between ② and ④ today corrupts neither aggregate's invariants — it just leaves the audit log incomplete.
3. **Consumers of `Transaction` are read-only and tolerant of slight delay.** Reporting, tax history, "list my transactions" UI — none of them need the `Transaction` row to be visible *at the same instant* as the `Portfolio` mutation. *DDD §14* would call this a relaxed eventual consistency relationship between aggregates.

So the refactor is well-grounded: **the only thing keeping ④ inside the same transaction as ② is convenience and the (real) atomicity guarantee it currently provides**. Convenience is negotiable. Atomicity is not — it must be replaced by *durable publication + idempotent re-delivery*.

### 1.5 Module ownership today

All four steps live in the `portfolios` Spring Modulith module:

- domain types in [`domain/.../portfolios/model/...`](domain/src/main/java/cat/gencat/agaur/hexastock/portfolios/model/)
- ports + service in [`application/.../portfolios/application/...`](application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/)
- adapters in `adapters-outbound-persistence-jpa` and `adapters-outbound-persistence-mongodb`

So the proposed change is **intra-module** (compare with the watchlists refactor, which was *inter-module*: `watchlists` → `notifications`). That's important: this refactor does not introduce any new cross-module dependency, only a temporal decoupling within `portfolios`.

---

## 2. DDD analysis of the event

### 2.1 Is `StockSoldEvent` a domain event?

Test it against Evans's criteria (*DDD §9 — Making Implicit Concepts Explicit*, and the broader event-storming literature):

| Criterion | Verdict |
|---|---|
| Past tense, business fact | ✅ "Sold" — fact, not request |
| Stated in the **ubiquitous language** | ✅ "the user sold X shares of AAPL at $283.06" — every `02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md`, every `DDD Portfolio and Transactions.md`, every conversation with a domain expert uses the verb *sell*. |
| Triggered by a real-world or business decision | ✅ User instruction → service execution → completed sale |
| Multiple consumers plausibly interested | ✅ audit, analytics, reporting, notifications, watchlists ("position-size" alerts) |
| Carries enough context for consumers without leaking aggregates | ✅ as long as we ship VOs + IDs, not entity references |

It passes. It is **not**:

- a notification ("tell the user…")
- a command ("record a transaction") — even though that's its first consumer
- a precondition for the sale itself

### 2.2 Where does the event come from? Aggregate vs application service

Two camps in the literature:

- **(A) Aggregate registers the event** (Vernon, IDDD ch. 8 — *"register events as a side effect of behaviour"*). `Portfolio.sell(...)` adds a `StockSoldEvent` to an internal collection; the application service flushes them after `save`.
- **(B) Application service publishes the event** after `save`. The aggregate stays a pure value-returning function (returns `SellResult`), and the service composes the event from the result.

**Recommendation for HexaStock: option (B), application service publishes.** Three reasons:

1. `Portfolio` is currently **value-returning** (`sell(...) → SellResult`). Going to (A) would force a setter-style API or a `pendingEvents()` flush method on the aggregate. That's a cross-cutting change to a stable, well-loved aggregate, in service of one feature.
2. (B) keeps the **domain Maven module Spring-free** trivially — no need for the aggregate to know about an event registry.
3. (B) preserves ADR-007 — `application` stays Spring-free; the `DomainEventPublisher` port is already in place exactly for this.

Trade-off acknowledged: with (B), the discipline is on the service. If a developer adds another `sellStock`-like method and forgets to publish, no one will know. Mitigation: a tiny architecture test that says *"any application method named `sell*Stock` must call `eventPublisher.publish(StockSoldEvent.class)`"* — only worth doing if we ever get more than one such method.

### 2.3 What should the event carry?

Guiding principles (*DDD §5*, §9, plus the existing `WatchlistAlertTriggeredEvent` precedent):

- **VOs over primitives** — `Ticker`, `Money`, `Price`, `ShareQuantity`, `PortfolioId`. Already in the domain Maven module. No mapping pain. The `WatchlistAlertTriggeredEvent` already does this — keep precedent.
- **IDs, not entity references** — `PortfolioId`, **not** `Portfolio`. Aggregates must not leak across event boundaries (they may not even exist anymore by the time the listener runs).
- **Self-contained** — listener should not need to call back into `PortfolioPort` to do its job. If we ever externalise the event, callbacks would be impossible.
- **Past-tense snapshot** — values frozen at sale time. If the price later changes, the event is unaffected.
- **Carry the result, not the request** — `proceeds`, `realisedProfit`, not "user wanted to sell N at market price".
- **Stable `eventId`** for idempotency (see §3.4).

### 2.4 What should the event NOT carry?

- ❌ `Portfolio` aggregate (entity reference)
- ❌ Lots consumed (FIFO internals) — *unless* we deliberately add `LotSoldEvent` later (Phase 4, optional). Don't pollute the aggregate-level event with per-lot detail. *DDD §15 (Distillation)* — keep events minimal.
- ❌ User name / e-mail / chat-id — that's notification routing, resolved by `notifications` from `ownerName`, exactly as for watchlists.
- ❌ Recommended actions ("you should rebalance…") — that's a *consumer's* concern.
- ❌ A `Transaction` object — the listener creates it; including it would invert the dependency.

### 2.5 Naming

Candidates:

| Name | Pros | Cons |
|---|---|---|
| **`StockSoldEvent`** | Past tense, matches `WatchlistAlertTriggeredEvent` cadence, ubiquitous language ("the customer sold stock") | Slightly generic |
| `StocksSoldEvent` | Plural reflects "shares" | But one *event* = one *sale* (one ticker, one quantity). Plural is misleading. |
| `SaleExecutedEvent` | "Executed" reads as past | Mixes with the *commands* vocabulary (execute) |
| `PortfolioStockSoldEvent` | Disambiguates module | Verbose; module is implicit in package |
| `StockSaleCompletedEvent` | Very explicit | Wordy. "Completed" adds nothing — events are by definition past. |

**Pick: `StockSoldEvent`.** Singular, past tense, matches the aggregate operation `portfolio.sell(...)`, matches `SaleTransaction`, fits the existing event naming style.

### 2.6 Proposed event content

| Field | Type | Why |
|---|---|---|
| `eventId` | `UUID` | Idempotency key for the persistent registry + listener dedup |
| `portfolioId` | `PortfolioId` (VO) | Aggregate identity |
| `ownerName` | `String` | Already used by `WatchlistAlertTriggeredEvent`; lets future cross-module consumers route per-user without coupling to the user model |
| `ticker` | `Ticker` (VO) | Domain VO |
| `quantity` | `ShareQuantity` (VO) | Number of shares sold |
| `salePrice` | `Price` (VO) | Unit price at sale time |
| `proceeds` | `Money` (VO) | `quantity × salePrice` (matches `SellResult.proceeds()`) |
| `costBasis` | `Money` (VO) | From `SellResult.costBasis()` — useful for tax/reporting consumers |
| `realisedProfit` | `Money` (VO) | From `SellResult.profit()` — signed |
| `occurredOn` | `Instant` | Standard for events; `LocalDateTime.now()` in `Transaction` is a project bug to fix later |

All VOs already live in the `domain` Maven module and are already used by `WatchlistAlertTriggeredEvent`. Zero new domain types.

---

## 3. Transaction registration as a listener — careful analysis

This is the section where I challenge the proposal hardest, as requested.

### 3.1 Is the `SaleTransaction` part of `Portfolio`'s consistency boundary?

**No.** §1.3 already established this. The Javadoc on `Transaction` itself says so. The proposal is consistent with the *existing* model.

### 3.2 Is it an audit / accounting projection, or a side effect?

**It is an accounting projection of an already-completed sale.** That's the textbook DDD rationale for moving it out (*DDD §11 — "Accounting Models in Analysis Patterns"*). Specifically:

- It is **derived** from the sale (no information in `SaleTransaction` is independent of what `Portfolio.sell` returns).
- It is **append-only**.
- It is **read by reporting**, not by the sale path.

So it is a projection. It is *also* a side effect, in the sense that it's a write that follows the primary write — but the word "side effect" tends to imply "could be skipped"; this one cannot.

### 3.3 Should it run in the same DB transaction as the sale?

This is the real question. Two opposing pressures:

- **Atomicity (today):** crash between `savePortfolio` and `transactionPort.save` is impossible because they share a TX. Audit log is always consistent with portfolio state. Auditors and accountants love this.
- **Decoupling (proposed):** the sale should not have to wait for, or be aborted by, an audit write. More importantly, the sale should not be coupled to the *future* arrival of new consumers (notifications, analytics, watchlists position-size alerts).

The honest answer:

> **There is no free lunch.** Going from "same TX" to "AFTER_COMMIT listener" trades atomicity for decoupling. That trade is acceptable **only if** the publication is durable and the listener is idempotent. Otherwise we're regressing.

Concrete options:

1. **Same TX listener** (`@TransactionalEventListener(BEFORE_COMMIT)` or just synchronous publish + sync listener inside the TX). Keeps atomicity, gains expressiveness, allows future listeners. *No durability problem.* Loses async / fault-isolation.
2. **AFTER_COMMIT, in-memory only** (`@ApplicationModuleListener` without persistent registry). Best decoupling, **worst durability** — listener crash → silent data loss.
3. **AFTER_COMMIT + Spring Modulith Event Publication Registry** (JPA-backed table `event_publication`). Publication row written *in the same TX as the sale*; listener acks completion in the registry; on restart, incomplete publications are re-delivered. Atomicity restored, decoupling preserved. **This is the production-grade option.**

### 3.4 Risks if the listener fails

| Failure mode | Option 1 (sync TX) | Option 2 (in-mem AFTER_COMMIT) | Option 3 (registry AFTER_COMMIT) |
|---|---|---|---|
| Listener throws synchronously | Sale is rolled back | Sale already committed; listener exception logged; **`Transaction` never persisted** | Sale committed; publication row stays incomplete; retried on next restart / explicit resubmit |
| JVM crash mid-listener | N/A (sale not committed yet) | **Silent loss** | Publication row incomplete; retried on restart |
| Listener succeeds twice (`@RetryOnWriteConflict` outer retry, or registry resubmit) | Single TX → not possible | Possible | Possible — *requires idempotency* (dedupe on `eventId`) |
| DB unavailable for `Transaction` table only | Sale rolled back | Sale committed; `Transaction` lost | Sale committed; retried |

`@RetryOnWriteConflict` deserves special mention: it can re-execute `sellStock(...)` end-to-end on optimistic-locking failures. With **Option 1** that means the publication is part of the retried unit — fine. With **Option 2/3** we must publish the event *after* the retry succeeds (which is what publishing from inside the `@Transactional` block already does — the Spring TX synchronisation only fires AFTER the *successful* commit, not after each attempt). Worth a unit test, though.

### 3.5 Should this listener be in the same Modulith module?

**Yes — same module: `portfolios`.**

Reasoning (*DDD §14*):

- `SaleTransaction` is a `portfolios` aggregate. It lives in `domain/.../portfolios/model/transaction`. `TransactionPort` is in `application/.../portfolios/application/port/out`. Putting the listener in `portfolios` keeps the writer of the aggregate inside the module that owns it.
- Cross-module event consumption (like `notifications` consuming `WatchlistAlertTriggeredEvent`) should be reserved for cases where the consumer is *conceptually different* from the producer. Audit-recording is the same bounded context as the sale.
- If we put the listener in `notifications` or `audit` as a separate module, then the `portfolios::events::StockSoldEvent` becomes part of the published API of `portfolios` *immediately*, and we add a cross-module dependency just to write a row in our own database. That's a step too far for Phase 1.

Future-proofing: when (if) we extract a separate `audit` module, we move the listener there. The event API doesn't change.

### 3.6 Sync or async?

Recommend `@ApplicationModuleListener` semantics: **async, AFTER_COMMIT, REQUIRES_NEW**. Same as `WatchlistAlertNotificationListener`. The audit write is small and fast, but:

- Async means the user-facing REST response returns as soon as the sale is committed. Better latency.
- REQUIRES_NEW isolates the audit-write transaction. An audit failure can't poison anything else.
- AFTER_COMMIT is what makes the persistent registry pattern work.

**But:** for Phase 1, if the persistent registry is not yet enabled, fall back to **synchronous, same-TX** (`@TransactionalEventListener` with default phase `AFTER_COMMIT` is *still* outside the TX — that's the trap). If we want sync-in-TX, just call the listener synchronously from the service via the publisher (Spring publishes synchronously by default; only `@Async` makes it async). See §6 for the exact recipe.

### 3.7 Recommendation (this section)

> **Phase 1: keep it boring and safe.** Sync publication, in-TX listener (option 1). One commit covers `Portfolio` + `Transaction`. Atomicity preserved. Refactor delivers value (separation of concerns, supple design, declarative service body) **without** taking on the durability/idempotency risk yet.
>
> **Phase 2: enable Spring Modulith persistent event registry.** Switch to `@ApplicationModuleListener` (async, AFTER_COMMIT, REQUIRES_NEW). Add idempotency via `eventId`.
>
> Phases 3+ (other consumers) become safe and cheap once Phase 2 is in.

This is the opposite order from what some examples in the *Domain Events Roadmap* suggest. That's deliberate: in production code, **never trade away atomicity until you've replaced it with durable publication**.

---

## 4. Phased implementation plan

### Phase 1 — Synchronous in-TX listener (the safe refactor)

**Goal.** Move the `Transaction.createSale + transactionPort.save` lines out of `sellStock(...)` into a listener, *without* changing the atomicity guarantee.

**Changes.**

| File | Change |
|---|---|
| `application/.../portfolios/events/StockSoldEvent.java` | **NEW** — `record` (see §5) |
| `bootstrap/.../portfolios/events/package-info.java` | **NEW** — `@NamedInterface("events")` |
| `application/.../portfolios/application/service/PortfolioStockOperationsService.java` | Remove `Transaction.createSale + transactionPort.save`. Inject `DomainEventPublisher`. Publish `StockSoldEvent` after `savePortfolio`. Inject `Clock`. |
| `application/.../portfolios/application/service/SaleTransactionRecordingListener.java` | **NEW** — `@TransactionalEventListener(phase=BEFORE_COMMIT)` *or* sync `@EventListener` (see §6) |
| `bootstrap/.../config/SpringAppConfig.java` (or wherever the wiring is) | Wire the listener as a `@Component` |

**New tests.**

| Test | Purpose |
|---|---|
| `PortfolioStockOperationsServiceTest.sellStock_publishes_StockSoldEvent` | Verify the service publishes once, no `transactionPort.save` directly |
| `SaleTransactionRecordingListenerTest` (unit, no Spring) | Stub `TransactionPort`, hand-build a `StockSoldEvent`, assert one `SaleTransaction` saved with correct fields |
| `SellStockEventFlowIntegrationTest` (`@SpringBootTest`, `@DataJpaTest`-style) | Full flow: REST POST `/sales` → assert one row in `Portfolio` and one in `Transaction`, both rolled back together if the listener throws |

**Tests to preserve untouched.**

- `PortfolioTradingRestIntegrationTest$WhenPortfolioExists$SellingShares` — still goes green
- All `TransactionService*` tests — `TransactionPort` interface unchanged
- `ModulithVerificationTest` — no new module dependency
- `HexagonalArchitectureTest` — `application` stays Spring-free except for the existing publisher port

**Risks.**
- BEFORE_COMMIT semantics differ subtly between Spring versions. Keep the test that simulates listener failure rolling back the sale.
- Two `@Transactional` boundaries (the service and the listener) merging under propagation defaults. Use `Propagation.MANDATORY` on the listener to make the merge explicit.

**Rollback strategy.** Single revert commit. The event class and listener are pure additions; the service line removal is the only destructive change. `git revert <hash>` brings the previous behaviour back.

**Acceptance criteria.**
- All existing tests green.
- New listener test green.
- `Transaction` row appears in DB on every successful sale, vanishes on rollback.
- `sellStock(...)` does not import `Transaction` or `TransactionPort` anymore.

### Phase 2 — Persistent Event Publication Registry + async listener

**Goal.** Decouple the listener from the sale's TX without losing durability.

**Changes.**

| File | Change |
|---|---|
| `bootstrap/pom.xml` | Add `spring-modulith-starter-jpa` (and `-mongodb` if mongo profile) |
| `application-jpa.properties` | `spring.modulith.events.jdbc.schema-initialization.enabled=true` |
| `SaleTransactionRecordingListener` | Replace `@TransactionalEventListener` with `@ApplicationModuleListener` |
| `SaleTransactionRecordingListener.on(...)` | Add idempotency: `if (transactionPort.existsByEventId(e.eventId())) return;` |
| `Transaction` / `SaleTransaction` | Add `UUID sourceEventId` to the record + persistence schema migration |
| `TransactionPort` | Add `boolean existsByEventId(UUID)` |

**New tests.**
- `SellStockEventRegistryIntegrationTest` — with `Awaitility`, `kill -9` simulation (test profile that throws inside listener once), assert recovery on restart resubmit.
- `SaleTransactionRecordingListener.idempotency` — call twice with same event, assert single row.

**Risks.**
- Schema change on `transactions` table — needs Flyway/Liquibase migration script for existing data (or accept that pre-migration sales have `null` `sourceEventId`).
- `event_publication` table grows unbounded; needs a periodic completion-reaper (Spring Modulith provides one; just enable it).

**Rollback.** Revert the registry config; listener falls back to sync mode. Keep `sourceEventId` column nullable so old code keeps working.

**Acceptance criteria.**
- Forced listener crash → restart → row appears in `Transaction`.
- Two deliveries of the same event → one row.
- `event_publication` table populated atomically with the sale.

### Phase 3 — Same shape applied to `BuyStock` (optional, but completes the symmetry)

**Goal.** Mirror everything in `buyStock(...)`. Don't do it on day one; do it when Phase 2 is stable, otherwise the audit table half-decoupled is confusing.

### Phase 4 — Per-lot events `LotSoldEvent` (optional, advanced)

**Goal.** Demonstrate the *one operation, many emitted events* pattern (*DDD §11, ROADMAP §5.2*).
**Cost.** Touches the aggregate (`SellResult` must carry `List<LotConsumption>`).
**Benefit.** Per-lot tax/holding-period reporting.
**Recommendation.** *Educational stretch goal only.* Not part of the Phase 1 PR.

### Phase 5 — Cross-module consumers (`notifications`, `analytics`, `watchlists`)

Phase 5 is not one phase, it's an *open door*. Each consumer is its own small refactor: declare `allowedDependencies = {..., "portfolios::events"}` in its `@ApplicationModule`, add a listener. None of them require touching `portfolios` again.

---

## 5. Suggested event design (Java sketch)

### 5.1 Location

```
application/src/main/java/cat/gencat/agaur/hexastock/portfolios/events/
    StockSoldEvent.java                                     ← the record

bootstrap/src/main/java/cat/gencat/agaur/hexastock/portfolios/events/
    package-info.java                                       ← @NamedInterface("events")
```

Same split as the watchlists precedent: pure domain event lives in `application` (Spring-free, ADR-007); the `@NamedInterface` marker lives in `bootstrap` next to the `@ApplicationModule` annotation. Allowed cross-module dependency declared on the *consumer* side, never on the producer.

### 5.2 The record

```java
package cat.gencat.agaur.hexastock.portfolios.events;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published by the Portfolios module after a stock sale has been
 * successfully executed and persisted. Past-tense business fact, immutable, no
 * framework dependencies.
 *
 * <p>The event is consumed by the {@code SaleTransactionRecordingListener}
 * (intra-module) which writes the corresponding {@code SaleTransaction}.
 * Future cross-module consumers (analytics, notifications) can subscribe by
 * declaring {@code allowedDependencies = {"portfolios::events"}} in their
 * {@code @ApplicationModule}.
 */
public record StockSoldEvent(
        UUID eventId,
        PortfolioId portfolioId,
        String ownerName,
        Ticker ticker,
        ShareQuantity quantity,
        Price salePrice,
        Money proceeds,
        Money costBasis,
        Money realisedProfit,
        Instant occurredOn
) {
    public StockSoldEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(portfolioId, "portfolioId");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(salePrice, "salePrice");
        Objects.requireNonNull(proceeds, "proceeds");
        Objects.requireNonNull(costBasis, "costBasis");
        Objects.requireNonNull(realisedProfit, "realisedProfit");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }
}
```

`package-info.java` (bootstrap side):

```java
@NamedInterface("events")
package cat.gencat.agaur.hexastock.portfolios.events;

import org.springframework.modulith.NamedInterface;
```

### 5.3 The new service body

```java
SellResult sellResult = portfolio.sell(ticker, quantity, price);
portfolioPort.savePortfolio(portfolio);

eventPublisher.publish(new StockSoldEvent(
        UUID.randomUUID(),
        portfolioId,
        portfolio.getOwnerName(),
        ticker,
        quantity,
        price,
        sellResult.proceeds(),
        sellResult.costBasis(),
        sellResult.profit(),
        clock.instant()));

return sellResult;
```

The `Transaction.createSale` and `transactionPort.save` lines are gone. `Clock` is injected (precedent: `MarketSentinelService`).

---

## 6. Suggested listener design (Java sketch)

### 6.1 Location

```
application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/
    SaleTransactionRecordingListener.java
```

Same Maven module as the service (`application`). Same Spring Modulith module (`portfolios`). Listener is wired as a `@Component` in `bootstrap/.../config/SpringAppConfig.java`, exactly like the existing services (this preserves ADR-007: no Spring annotations in `application` source).

> ⚠️ **Subtle point.** `@TransactionalEventListener` and `@ApplicationModuleListener` are Spring annotations — they cannot be on a class in the `application` Maven module per ADR-007. Two clean options:
>
> 1. Place the **interface** in `application` and the **annotated implementation** in `bootstrap` (matches how `DomainEventPublisher` ↔ Spring publisher is wired).
> 2. Relax ADR-007 specifically for `@ApplicationModuleListener` (the watchlists precedent already did this for `WatchlistAlertNotificationListener`, but that listener is in `adapters-outbound-notification`, not `application`).
>
> **Recommendation: option (1).** The listener interface in `application` declares `void on(StockSoldEvent e)`; the annotated implementation lives in `bootstrap` and delegates. It's a tiny extra class but it preserves the ADR cleanly.

### 6.2 Phase 1 — synchronous, in-TX

```java
// bootstrap side (annotated)
@Component
public class SaleTransactionRecordingListenerImpl
        implements SaleTransactionRecordingListener {

    private final TransactionPort transactionPort;

    public SaleTransactionRecordingListenerImpl(TransactionPort p) {
        this.transactionPort = p;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void on(StockSoldEvent e) {
        transactionPort.save(SaleTransaction.from(e));   // small static factory on SaleTransaction
    }
}
```

`BEFORE_COMMIT` + `MANDATORY` keeps the audit write inside the sale's TX. If the listener throws, the sale is rolled back. Atomicity preserved.

### 6.3 Phase 2 — async, AFTER_COMMIT, idempotent

```java
@Component
public class SaleTransactionRecordingListenerImpl
        implements SaleTransactionRecordingListener {

    private final TransactionPort transactionPort;

    @ApplicationModuleListener   // = @TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)
    @Override
    public void on(StockSoldEvent e) {
        if (transactionPort.existsByEventId(e.eventId())) {
            return;                                       // dedupe (at-least-once delivery)
        }
        transactionPort.save(SaleTransaction.from(e));
    }
}
```

Persistent publication is automatic once `spring-modulith-starter-jpa` is on the classpath.

### 6.4 Hexagonal-cleanness checklist

- ✅ Listener uses **only the existing `TransactionPort`** — no JPA imports
- ✅ Listener does no domain logic of its own (it's an adapter from "sale-as-event" to "sale-as-audit-row")
- ✅ Domain layer (`Transaction.createSale` / `SaleTransaction.from(event)`) still owns the construction rule
- ✅ The event is a record; the event class itself has zero Spring/JPA imports
- ✅ The cross-cutting Spring annotation (`@ApplicationModuleListener`) lives in `bootstrap`, not `application`

---

## 7. Other meaningful listeners for `StockSoldEvent`

Now that we have an event, what *should* and *should not* attach to it?

| Idea | Conceptually a listener? | Module | Sync/async | Implement when? | Notes |
|---|---|---|---|---|---|
| **Record `SaleTransaction`** (this proposal) | ✅ — accounting projection | `portfolios` (intra-module) | Phase 1: sync; Phase 2: async | **Phase 1** | The driver of the whole refactor |
| **Update realised P&L projection per portfolio** | ✅ — read-model projection | `analytics` (new) or stay in `portfolios` | Async, AFTER_COMMIT | Phase 5 | Educational: read-model for "Year-to-date realised gain" view. Pure derivation from `realisedProfit`. |
| **Notify the user** ("Your sale of 30 AAPL completed: +$420 realised") | ✅ — but only if business actually wants it | `notifications` (existing) | Async | Phase 5 — only if the product team asks | Re-uses the `notifications` module already built for watchlists. Pedagogically nice: same module, two events, two listeners. |
| **Update tax-year report** | ✅ — projection, like accounting | `tax-reporting` (would be a new module) | Async | Later. Don't speculate. | Only worth it when there's actual tax-reporting code. *DDD §15*: don't generalise prematurely. |
| **Re-evaluate position-size watchlist alerts** | ✅ — interesting cross-module case | `watchlists` (existing) | Async | Phase 5 | The user might have an alert "if I hold less than 10 AAPL warn me"; selling could trigger it. Concrete cross-module value. |
| **Trigger rebalancing recommendation** | 🟡 — careful, this is closer to a *command* than a fact reaction | `analytics` | Async | Later | Only OK if it's a *suggestion*, never auto-execution. |
| **Invalidate cached portfolio summary** | ✅ — but probably not via domain event | infrastructure | N/A | N/A | Cache eviction is an *implementation* concern, not a domain reaction. Use cache-aside / Spring `@CacheEvict`, don't pollute the domain event with infra concerns. |
| **Educational trace listener** ("log every event for the demo") | ✅ — for teaching only | `bootstrap` (demo profile) | Async | Phase 1 (cheap) | Tiny `@ApplicationModuleListener` that logs `STOCK_SOLD_EVENT user=… proceeds=…` — exactly like the existing `WATCHLIST_ALERT_LISTENER_RECEIVED` log line. Helps the live demo. **Free educational value.** |
| **Outbox / external event publisher** | ✅ when externalisation is real | future external module | Async | Way later | Out of scope: no Kafka, no broker. The Spring Modulith persistent registry already *is* a kind of in-process outbox. |

**Pedagogical short-list (recommended for the open-source project):**

1. `SaleTransactionRecordingListener` — the canonical example, intra-module
2. The educational trace listener — one-liner, proves "events fire" in the demo log
3. Eventually, a cross-module read-model projection in `analytics` — proves "the same event can have multiple consumers, they only depend on `portfolios::events`"

Resist the rest until there's real demand. *DDD §15:* the supporting subdomains shouldn't be invented to demonstrate techniques.

---

## 8. Spring Modulith compatibility

### 8.1 Module ownership

| Element | Module |
|---|---|
| `StockSoldEvent` (record) | `portfolios` |
| `@NamedInterface("events")` declaration | `portfolios` (bootstrap-side `package-info.java`) |
| `SaleTransactionRecordingListener` (Phase 1 & 2) | `portfolios` (intra-module) |
| Future cross-module listeners | `notifications`, `analytics`, `watchlists` — each with `allowedDependencies = {"portfolios::events"}` |

### 8.2 Public-API status

The event package becomes part of the *published API* of `portfolios`:

```java
// bootstrap/.../portfolios/package-info.java   (existing, edit)
@org.springframework.modulith.ApplicationModule(
        displayName = "Portfolios",
        allowedDependencies = { "marketdata::model" }
)
package cat.gencat.agaur.hexastock.portfolios;

// bootstrap/.../portfolios/events/package-info.java   (NEW)
@NamedInterface("events")
package cat.gencat.agaur.hexastock.portfolios.events;
```

Treat `StockSoldEvent`'s shape as a *contract*: backward-incompatible changes break consumers. Use `@Deprecated` for transitions, never delete fields.

### 8.3 Avoiding illegal module dependencies

- `portfolios` does **not** depend on `notifications`, `analytics`, or anything downstream — events are published, never called.
- Future consumers depend on `portfolios::events` *only*, never on `portfolios` internals.
- The persistent event registry is infrastructure (`spring-modulith-starter-jpa`), wired in `bootstrap` — does not appear in `domain` or `application`.

### 8.4 Verifying boundaries

- `ModulithVerificationTest.verify()` (already present) catches illegal dependencies at build time.
- `ModulithVerificationTest.documentation()` produces a Modulith arc42-style docs snapshot — generate it after Phase 1 to update `doc/architecture/modulith/`.
- A targeted test: `ApplicationModules.of(HexaStockApplication.class).verify()` — already in `ModulithVerificationTest`, will fail if any consumer accidentally imports a non-`events` package from `portfolios`.

### 8.5 Documenting the flow

Add a Modulith C4 / sequence diagram (`.puml`) to [`doc/consultancy/monday-session/diagrams/`](doc/consultancy/monday-session/diagrams/) showing:

1. REST `POST /sales` → `PortfolioStockOperationsService`
2. `Portfolio.sell()` → `PortfolioPort.save()`
3. `DomainEventPublisher.publish(StockSoldEvent)` → `event_publication` row inserted (Phase 2)
4. TX commit
5. `[task-N]` thread picks up → `SaleTransactionRecordingListener.on()` → `TransactionPort.save()`
6. `event_publication.completion_date` set

---

## 9. Hexagonal architecture compatibility

| Layer | Element | Compliant? |
|---|---|---|
| **Domain** | `Portfolio.sell()` returns `SellResult` (unchanged); `SaleTransaction.from(StockSoldEvent)` factory (new, in `domain`) | ✅ |
| **Domain** | `StockSoldEvent`? **No** — events live in `application`, mirroring `WatchlistAlertTriggeredEvent`. The reason: the event is a *cross-aggregate* concept that may be consumed by other modules, while the `domain` Maven module is per-aggregate-cluster. Putting events in `application` matches the existing convention. | ✅ |
| **Application** | `StockSoldEvent` record, `DomainEventPublisher` (existing port), `SaleTransactionRecordingListener` interface | ✅ Spring-free |
| **Application** | `PortfolioStockOperationsService.sellStock` calls `eventPublisher.publish(...)` instead of `transactionPort.save(...)` | ✅ uses existing port |
| **Inbound adapter** | `adapters-inbound-rest` — unchanged | ✅ |
| **Outbound adapter (persistence)** | `adapters-outbound-persistence-jpa/-mongodb` — unchanged interface; eventually adds `existsByEventId` | ✅ |
| **Bootstrap** | `SaleTransactionRecordingListenerImpl` (annotated), Spring publisher wiring (existing), Modulith registry config (Phase 2) | ✅ all infra concerns isolated here |

Result: **zero new dependencies in the domain Maven module, zero new Spring imports in the application Maven module.**

---

## 10. Testing strategy

### 10.1 Layered tests

| Layer | Test | What it proves |
|---|---|---|
| **Domain** | `PortfolioTest.sell_…` (existing) | `portfolio.sell(...)` invariants unchanged |
| **Domain (new)** | `SaleTransactionTest.from_event` | The factory builds a `SaleTransaction` whose fields match the event |
| **Application** | `PortfolioStockOperationsServiceTest.sellStock_publishes_event_and_does_not_call_transactionPort` | Use a recording-fake `DomainEventPublisher`. `transactionPort` is a strict mock that fails if `save` is called. |
| **Application** | `SaleTransactionRecordingListenerTest.on_persists_transaction` | Pure unit, hand-built event, stub port |
| **Application** | `SaleTransactionRecordingListenerTest.on_is_idempotent` (Phase 2) | Same event twice → one save |
| **Spring Modulith** | `SellStockEventPublicationIntegrationTest extends @ApplicationModuleTest` | Use Modulith's `PublishedEvents` to assert exactly one `StockSoldEvent` was published per `sellStock` |
| **Integration (Phase 1)** | `SellStockBookingFlowIntegrationTest` | Full REST → DB; assert `Portfolio` row + `Transaction` row + invariants on amounts |
| **Integration (Phase 1, failure)** | `SellStockListenerFailureRollsBackSale` | Listener throws → no `Portfolio` row, no `Transaction` row. Atomicity test. |
| **Integration (Phase 2)** | `SellStockEventRegistryRecoveryTest` | Listener crashes once, restart → `Transaction` appears on resubmit |
| **Architecture** | `HexagonalArchitectureTest` (existing) + new rule: *"no class in `application/.../portfolios/events/` may import `org.springframework.*` or `jakarta.persistence.*`"* | Enforces ADR-007 mechanically |
| **Module boundaries** | `ModulithVerificationTest` (existing) | No illegal dependencies introduced |

### 10.2 The flow test, concretely

```java
@SpringBootTest
@ActiveProfiles({"test","jpa","mockfinhub"})
class SellStockBookingFlowIT {

    @Test
    void sellStock_persists_portfolio_and_transaction_atomically() {
        var portfolioId = givenAPortfolioWith100Shares("AAPL");

        sellStockUseCase.sellStock(portfolioId, Ticker.of("AAPL"), ShareQuantity.of(30));

        var portfolio = portfolioPort.getPortfolioById(portfolioId).orElseThrow();
        assertThat(portfolio.holdingOf("AAPL").totalShares()).isEqualTo(70);

        var txs = transactionPort.findByPortfolioId(portfolioId);
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0)).isInstanceOf(SaleTransaction.class);
    }

    @Test
    void listener_failure_rolls_back_sale() {
        // override the listener bean with one that throws
        // run the sale
        // assert: portfolio unchanged, no transaction row
    }
}
```

### 10.3 Listener-isolation tests

For Phase 2 idempotency, Spring Modulith provides `@ApplicationModuleTest` and `IncompleteEventPublications` — use them. Don't roll your own polling loop.

### 10.4 Regression fence

The existing `PortfolioTradingRestIntegrationTest$WhenPortfolioExists$SellingShares` suite (currently passing) is the regression fence. It must stay green at every commit of Phase 1.

---

## 11. Documentation updates

| Document | Update |
|---|---|
| [doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md) | Add a forward-link to this proposal as the *implementation track* (the exercise stays a paper exercise; this doc is the actual plan) |
| [doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-QUICKSTART.md](doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-QUICKSTART.md) | Already aligned with this proposal (Iteration 1 = Phase 1, Iteration 2 = Phase 2). Cross-link both ways. |
| [doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md](doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md) | Mark `StockSoldEvent` as *being implemented* (Phase 1 in progress) instead of *planned* |
| [doc/architecture/](doc/architecture/) | New ADR: *"ADR-NNN: Sale audit recording moves to domain-event listener"* — context, decision, consequences, atomicity trade-off |
| [doc/consultancy/monday-session/diagrams/](doc/consultancy/monday-session/diagrams/) | New `.puml`: sequence `Sell Stock → Event → Listener → Transaction`. Suggested file: `13-sell-stocks-event-flow.puml` |
| [doc/consultancy/monday-session/02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md](doc/consultancy/monday-session/02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md) | Add a closing section *"…and the same shape for sales"* pointing to the new diagram and ADR |
| [doc/consultancy/monday-session/DEMO-WATCHLIST-NOTIFICATION-FLOW.md](doc/consultancy/monday-session/DEMO-WATCHLIST-NOTIFICATION-FLOW.md) | Add a §10 *"Next: same demo for `StockSoldEvent`"* with a curl recipe |
| `README.md` (root) | One paragraph: "the project demonstrates Spring Modulith domain events both *intra-module* (sales → audit) and *inter-module* (watchlists → notifications)." |
| [doc/architecture/modulith/](doc/architecture/) | Regenerate Modulith documentation (`ApplicationModules.documentation()`) after Phase 1 commit |

Diagrams to add (PlantUML):

1. `13-sell-stocks-event-flow-current.puml` — today's monolithic `sellStock`
2. `14-sell-stocks-event-flow-phase1.puml` — sync listener, BEFORE_COMMIT
3. `15-sell-stocks-event-flow-phase2.puml` — async listener with persistent registry, recovery on restart

---

## 12. Final recommendation

| Question | Answer |
|---|---|
| Should we implement `StockSoldEvent`? | **Yes.** It's a real domain event by every DDD criterion, and the project already has the publisher port + the watchlists precedent. |
| Should `Transaction` recording move to a listener? | **Yes — but in two steps.** Phase 1 keeps it synchronous + same TX (no atomicity loss); Phase 2 goes async + persistent registry + idempotency. |
| Same module or another? | **Same Modulith module (`portfolios`) — intra-module.** The audit projection is part of the same bounded context as the sale. Cross-module listeners (`notifications`, `analytics`) come later, on the same event, without touching `portfolios` again. |
| Aggregate-raised or service-published? | **Service-published.** Application service composes the event from the `SellResult`. Keeps `Portfolio` value-returning, keeps `domain` Spring-free, matches the existing `WatchlistAlertTriggeredEvent` pattern. |
| What first? | **Phase 1** — the smallest possible refactor that demonstrates the pattern without losing atomicity: add the event, add the in-TX listener, all green tests. |
| What postponed? | Phase 2 (registry) — *next sprint*. Phases 3–5 — only when there's real demand. `LotSoldEvent`, tax reporting, rebalancing — not now. |

### What this proposal explicitly refuses to do

- Build a generic event framework on top of Spring's. Use Spring Modulith as-is.
- Pull events into the `domain` Maven module. They live in `application`, like `WatchlistAlertTriggeredEvent`.
- Make `Portfolio` an event accumulator (Vernon-style). `SellResult` is enough.
- Add Kafka, RabbitMQ, an outbox table of our own, or any external broker.
- Implement listeners for which there is no current business demand.
- Trade away atomicity in Phase 1.

### Style and educational fit

This is an open-source project for teaching DDD + Hexagonal + Spring Modulith. The proposal is *deliberately* ordered for that:

1. Phase 1 is small enough for a 90-minute classroom exercise.
2. Phase 2 introduces the *production-grade* concern (durability + idempotency) as a separate, named lesson — not buried in Phase 1.
3. The `LotSoldEvent` stretch goal is positioned as *advanced material*, not as a requirement.
4. Every step is reversible (`git revert`).
5. Every step preserves the existing test suite.

That's the proposal. Comments, push-back and corrections welcome before any code is written.

---

## Appendix A — References

- **Eric Evans**, *Domain-Driven Design: Tackling Complexity in the Heart of Software* (2003) — chapters 5 (Software Expression of Model), 6 (Aggregates / Factories / Repositories), 9 (Implicit Concepts / Specifications), 14 (Maintaining Model Integrity), 15 (Distillation).
- **Vaughn Vernon**, *Implementing Domain-Driven Design* (2013) — chapter 8 (Domain Events).
- **Spring Modulith Reference** — *Event Publication Registry* and `@ApplicationModuleListener`.
- **HexaStock — internal**:
  - [doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md) — the participant exercise
  - [doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-QUICKSTART.md](doc/tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-QUICKSTART.md) — the 5-min recipe
  - [doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md](doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md) — forward-looking event catalogue
  - [doc/consultancy/04-DOMAIN-EVENTS-DEEP-DIVE.md](doc/consultancy/04-DOMAIN-EVENTS-DEEP-DIVE.md) — watchlists case study
  - [`WatchlistAlertTriggeredEvent`](application/src/main/java/cat/gencat/agaur/hexastock/watchlists/WatchlistAlertTriggeredEvent.java) — the implementation precedent
  - [`WatchlistAlertNotificationListener`](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/WatchlistAlertNotificationListener.java) — the listener precedent
  - [`PortfolioStockOperationsService`](application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/PortfolioStockOperationsService.java) — the target of the refactor
