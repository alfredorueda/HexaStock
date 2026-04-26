# Demo: Watchlist → Domain Event → Notification (Spring Modulith)

> Practical, local, no-external-services walkthrough of the Watchlist /
> Market Sentinel notification flow. Telegram is intentionally **disabled**;
> the default `LoggingNotificationSenderAdapter` is the notification
> mechanism, so the whole flow can be demonstrated from a single laptop.

**Companion files for this demo:**
- IntelliJ HTTP Client **setup** script (one-click "Run All" prepares the demo data, captures `portfolioId` / `watchlistId` automatically): [doc/consultancy/demo/watchlist-market-sentinel-setup.http](doc/consultancy/demo/watchlist-market-sentinel-setup.http)
- IntelliJ HTTP Client **control / cleanup** script (deactivate, reactivate, remove alerts, delete watchlist — reuses the IDs captured by the setup script): [doc/consultancy/demo/watchlist-market-sentinel-control.http](doc/consultancy/demo/watchlist-market-sentinel-control.http)
- This guide (narrative + verification + troubleshooting): you are here.

**Reading order during the live demo:** 0 → 1 → 8 (pre-flight tests) → 2 → 3 → 4 → 5 → 6 → 7 → (9 only if anything goes sideways).

---

## 0. Architecture in 30 seconds (whiteboard slide)

```
 ┌────────────────────────┐                            ┌──────────────────────────┐
 │  watchlists module     │                            │  notifications module    │
 │                        │                            │                          │
 │  MarketSentinelService │── publish ───► domain ───► │ WatchlistAlertNotifi-    │
 │  (every 60s, scheduled)│   event bus                │ cationListener           │
 │                        │ (Spring Modulith,          │  @ApplicationModule-     │
 │  emits                 │  AFTER_COMMIT,             │  Listener                │
 │  WatchlistAlert-       │  REQUIRES_NEW,             │                          │
 │  TriggeredEvent)       │  @Async)                   │ ─► LoggingNotification-  │
 └────────────────────────┘                            │    SenderAdapter (LOG)   │
                                                       └──────────────────────────┘
```

Key files to point at:
- Event: [WatchlistAlertTriggeredEvent.java](application/src/main/java/cat/gencat/agaur/hexastock/watchlists/WatchlistAlertTriggeredEvent.java)
- Publisher: [MarketSentinelService.java](application/src/main/java/cat/gencat/agaur/hexastock/watchlists/application/service/MarketSentinelService.java#L51-L66)
- Scheduler (driving adapter through the inbound port): [MarketSentinelScheduler.java](bootstrap/src/main/java/cat/gencat/agaur/hexastock/scheduler/MarketSentinelScheduler.java)
- Listener (the magic line): [WatchlistAlertNotificationListener.java](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/WatchlistAlertNotificationListener.java#L36)
- Default LOG sender (`@Component`, no profile required): [LoggingNotificationSenderAdapter.java](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationSenderAdapter.java#L31-L40)
- Default LOG destination provider: [LoggingNotificationDestinationProvider.java](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationDestinationProvider.java)

> Telegram is opt-in via `@Profile("telegram-notifications")`. We do **not** activate that profile, so the LOG channel is the only sender registered.

---

## 1. Start the application

### 1.1 Start MySQL (only external dependency)

The `jpa` profile points at `jdbc:mysql://localhost:3307/hexaStock`.

```bash
cd /Users/alfre/IdeaProjects/DSI2025-2026/HexaStock
docker compose up -d mysql
docker compose ps mysql      # wait until status is "healthy" / "Up"
```

### 1.2 Build once (skip tests for speed during the demo)

```bash
./mvnw -q -DskipTests install
```

### 1.3 Run the bootstrap module from a terminal (recommended)

Launching from a terminal (instead of from IntelliJ) is the recommended
workflow for the live demo: it keeps the run reproducible, lets us pipe
the output through `tee` for a follower terminal, and avoids accidental
IDE-injected JVM flags or stale active profiles.

For the demo, lower the sentinel interval from 60 s to 10 s so we don’t
have to wait. We do **not** activate `telegram-notifications`, so the
LOG sender is the only registered `NotificationSender`.

**Terminal 1 — launch + capture full log to a file:**

```bash
cd /Users/alfre/IdeaProjects/DSI2025-2026/HexaStock && \
  MARKET_SENTINEL_INTERVAL=10000 \
  ./mvnw -pl bootstrap spring-boot:run \
         -Dspring-boot.run.profiles=jpa,mockfinhub \
  2>&1 | tee /tmp/hexastock-demo.log
```

**Terminal 2 — follow only the demo log chain (color-highlighted):**

```bash
tail -F /tmp/hexastock-demo.log \
  | grep --line-buffered --color=always -E \
      "MARKET_SENTINEL_TICK|DOMAIN_EVENT_PUBLISHED|WATCHLIST_ALERT_LISTENER|WATCHLIST_ALERT "
```

This second terminal is the one to project on screen during the demo:
it shows the entire publisher → modulith → listener → sender chain
without the noise from Hibernate / Tomcat / Spring boot. (`tail -F`
with capital F keeps following even if the file is rotated;
`--line-buffered` flushes each line immediately.)

Profiles in use:
- `jpa` — MySQL persistence (matches running container).
- `mockfinhub` — [MockFinhubStockPriceAdapter](adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/marketdata/adapter/out/rest/MockFinhubStockPriceAdapter.java) returns random prices in roughly `[10.00, 1000.00]`. No API key, no network call.
- *(no `telegram-notifications`)* — only the LOG sender is wired.
- *(no `alphaVantage`)* — important: if `alphaVantage` is active you’ll see intermittent `ExternalApiException` from the live API.

App URL: `http://localhost:8081` · Swagger UI: `http://localhost:8081/swagger-ui.html`

Verify in Terminal 1 that Spring picked up the right profiles:

```
The following 2 profiles are active: "jpa", "mockfinhub"
```

> Alternative — launching from IntelliJ: in *Run/Debug Configurations*
> set **Active profiles = `jpa,mockfinhub`** (no spaces around the
> comma) and **Environment variable `MARKET_SENTINEL_INTERVAL=10000`**.
> Optionally enable *Modify options → Save console output to file* and
> point it at `/tmp/hexastock-demo.log` so the Terminal 2 follower
> command above keeps working unchanged.

App URL: `http://localhost:8081` · Swagger UI: `http://localhost:8081/swagger-ui.html`

You should see at startup:

```
Detected module: marketdata
Detected module: portfolios
Detected module: watchlists
Detected module: notifications
... Started HexaStockApplication in X.XXX seconds
```

That confirms Spring Modulith has discovered all four modules.

---

## 2. Create and configure a watchlist

The endpoints below are served by [WatchlistRestController](adapters-inbound-rest/src/main/java/cat/gencat/agaur/hexastock/watchlists/adapter/in/WatchlistRestController.java). Use any HTTP client; here we use `curl` and capture the IDs into shell variables.

### 2.1 Create the watchlist (owner = `alice`)

```bash
WID=$(curl -s -X POST http://localhost:8081/api/watchlists \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"alice","listName":"Tech"}' \
  | tee /dev/stderr | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
echo "Watchlist id = $WID"
```

### 2.2 Add a price alert that is **guaranteed to trigger**

The query that detects triggered alerts uses
`thresholdPrice >= currentPrice` — i.e. the watchlist owner is interested
when the price drops to (or below) the threshold (a "buy signal").
Mock prices are in `[10, 1000]`, so a threshold of `9999.00` makes
**every** sentinel cycle fire the alert.

```bash
curl -s -X POST "http://localhost:8081/api/watchlists/$WID/alerts" \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"AAPL","thresholdPrice":"9999.00"}'
echo
```

### 2.3 Activate the watchlist

A watchlist must be `active=true` for the sentinel to consider it (see the SQL `where w.active = true` in [JpaWatchlistQuerySpringDataRepository](adapters-outbound-persistence-jpa/src/main/java/cat/gencat/agaur/hexastock/watchlists/adapter/out/persistence/jpa/springdatarepository/JpaWatchlistQuerySpringDataRepository.java#L22-L43)).

```bash
curl -s -X POST "http://localhost:8081/api/watchlists/$WID/activation"
echo
```

> Tip: two IntelliJ IDEA HTTP Client files automate every call in this
> section. Run them in order:
>
> 1. [watchlist-market-sentinel-setup.http](../demo/watchlist-market-sentinel-setup.http) —
>    open in IntelliJ and click **"Run All Requests in File"**. It
>    creates the portfolio + watchlist + alert, activates the watchlist,
>    samples the mock price, and captures `portfolioId` / `watchlistId`
>    into the HTTP Client global environment so nothing has to be
>    copy-pasted.
> 2. [watchlist-market-sentinel-control.http](../demo/watchlist-market-sentinel-control.http) —
>    operational and cleanup requests (deactivate, reactivate, remove
>    alerts, delete watchlist). Reuses the globals from step 1.
>
> A Postman collection with portfolio calls also exists at
> [HexaStock.postman_collection.json](../HexaStock.postman_collection.json).

---

## 3. Trigger the market alert

Nothing else to do. The [MarketSentinelScheduler](bootstrap/src/main/java/cat/gencat/agaur/hexastock/scheduler/MarketSentinelScheduler.java) ticks every `MARKET_SENTINEL_INTERVAL` ms (10 s in this demo) and calls `MarketSentinelUseCase::detectBuySignals()` through the inbound port.

Within ~10 seconds you will see, in order, in the application console:

1. (DEBUG/INFO) the scheduler log line — depending on the configured level.
2. The mock fetch of `AAPL`’s price.
3. The `WATCHLIST_ALERT` log line emitted by the LOG sender (see §6).

If you want to make the demo even tighter, restart with
`MARKET_SENTINEL_INTERVAL=3000`.

---

## 4. Verify the Domain Event is being published

There are three complementary signals.

### 4.1 (Recommended live) — Tail the application log for the listener

The `@ApplicationModuleListener` log entry only appears when the event is
both *published* and *received*, so seeing the alert log in §6 is your
proof. To narrow the scope, in a second terminal:

```bash
# If you redirect the app to a file:
tail -f /tmp/hexastock.log | grep -E "WATCHLIST_ALERT|WatchlistAlert"
```

### 4.2 Inspect the publisher in code

Open [MarketSentinelService.java#L51-L66](application/src/main/java/cat/gencat/agaur/hexastock/watchlists/application/service/MarketSentinelService.java#L51-L66):

```java
queryPort.findTriggeredAlerts(ticker, currentPrice)
    .forEach(view -> eventPublisher.publish(toEvent(view, currentPrice)));
```

The publisher is the application-level `DomainEventPublisher` port whose
runtime adapter delegates to Spring's `ApplicationEventPublisher`, which
in turn feeds Spring Modulith's event infrastructure.

### 4.3 (Important clarification) Where does the event actually live?

**With the current configuration, events are processed entirely
in-memory. Nothing is written to MySQL.**

The project's POMs only declare `spring-modulith-starter-core` and
`spring-modulith-events-api`. Spring Modulith therefore boots its
*Event Publication Registry* in **in-memory mode**
(`InMemoryEventPublicationRegistry`). The flow at runtime is:

1. The publisher's transaction commits.
2. Spring Modulith hands the event to the `@ApplicationModuleListener`
   on a background thread (the registry tracks the in-flight delivery in
   a `Map` inside the JVM).
3. The listener's own `REQUIRES_NEW` transaction runs and commits.
4. The registry entry is discarded from memory.

Consequence: if the JVM dies between steps 1 and 3, **the event is
lost** — there is no durable outbox.

If we wanted at-least-once delivery (the typical production hardening),
we would add ONE dependency:

```xml
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>
```

That alone makes Modulith create an `event_publication` table in MySQL
and persist every publication transactionally with the publisher's
commit, then mark each row complete when the listener finishes. The
publisher and listener code do **not** change. This is the
"Evolution 1" step described in
[04-PRODUCTION-EVOLUTION.md](04-PRODUCTION-EVOLUTION.md).

Until that dependency is added, the `event_publication` table simply
does not exist — querying it in MySQL returns `Table 'hexaStock.event_publication' doesn't exist`.
For the demo we therefore **prove event delivery from the application
log only** (§6), which is the authoritative source with the in-memory
registry.

---

## 5. Verify Spring Modulith is handling the event correctly

### 5.1 Module structure (no source-level violations)

```bash
./mvnw -pl bootstrap test -Dtest=ModulithVerificationTest
```

This is the [bootstrap/.../architecture/ModulithVerificationTest.java](bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/ModulithVerificationTest.java) suite. It asserts the four promoted modules (`watchlists`, `notifications`, `portfolios`, `marketdata`) only depend on each other through their published API packages.

### 5.2 Generated module documentation

The same test class has a `writeDocumentation()` test that emits PlantUML
+ AsciiDoc under `bootstrap/target/spring-modulith-docs/`. Open one of
the generated `.puml` files and render it (or just open the
auto-generated PNG if your toolchain produces one) to *visually* show
the audience:

- the four modules,
- the `WatchlistAlertTriggeredEvent` arrow from `watchlists` to
  `notifications`,
- and that `notifications` only knows about `watchlists` through the
  published event type — no bypass references.

### 5.3 Listener wiring

`WatchlistAlertNotificationListener::on` is annotated
`@ApplicationModuleListener`, which is the Spring Modulith equivalent of
`@TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)`.
Practically that means:

- the listener fires only **after** the publisher's transaction commits,
- it runs on a separate thread (so the sentinel cycle is not blocked),
- it has its own transaction (so a listener failure cannot roll back the publisher).

> Important consequence: the publisher must run inside an active
> transaction, otherwise Spring's `AFTER_COMMIT` listeners are **silently
> dropped**. That is why
> [MarketSentinelScheduler.runDetection()](bootstrap/src/main/java/cat/gencat/agaur/hexastock/scheduler/MarketSentinelScheduler.java)
> is annotated `@Transactional` — without it the sentinel ticks would
> publish events that no listener would ever receive.

---

## 6. Verify the notification was processed by the LOG adapter

When the listener picks up the event it asks the
`NotificationRecipientResolver` for the recipient's destinations. With
no telegram profile active, the only registered
`NotificationDestinationProvider` is
[LoggingNotificationDestinationProvider](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationDestinationProvider.java#L19-L24), which always returns a single
`LoggingNotificationDestination(userId)`. The listener then picks the
sender that `supports(destination)` — the [LoggingNotificationSenderAdapter](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationSenderAdapter.java) — and calls `send`.

For the live demo we instrumented every link in the chain with a
structured INFO line, so Terminal 2 (§1.3) will display the complete
flow per triggered alert:

| Marker | Source | What it proves |
|---|---|---|
| `MARKET_SENTINEL_TICK started` / `... finished durationMs=N` | [MarketSentinelScheduler](bootstrap/src/main/java/cat/gencat/agaur/hexastock/scheduler/MarketSentinelScheduler.java) | The scheduler is alive and producing ticks at the configured interval, even on cycles with zero alerts. |
| `DOMAIN_EVENT_PUBLISHED type=... payload=...` | [SpringDomainEventPublisher](bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/events/SpringDomainEventPublisher.java) | An event was handed to Spring's bus by the publisher port. Captures **every** domain event in the system, not only watchlist alerts. |
| `WATCHLIST_ALERT_LISTENER_RECEIVED ...` | [WatchlistAlertNotificationListener](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/WatchlistAlertNotificationListener.java) | The Notifications module *received* the event — i.e. Spring Modulith routed it across the module boundary, AFTER_COMMIT, on the async executor. |
| `WATCHLIST_ALERT_LISTENER_DISPATCH user=... destinations=N` | same | The recipient resolver returned N destinations to fan out to. |
| `WATCHLIST_ALERT user=... ticker=... ...` | [LoggingNotificationSenderAdapter](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationSenderAdapter.java) | The end of the chain — the actual notification dispatched to the LOG channel. |

If you started the app from Terminal 1 with `tee`, Terminal 2 already
shows these lines filtered and color-highlighted. If not:

```bash
tail -F /tmp/hexastock-demo.log \
  | grep --line-buffered --color=always -E \
      "MARKET_SENTINEL_TICK|DOMAIN_EVENT_PUBLISHED|WATCHLIST_ALERT_LISTENER|WATCHLIST_ALERT "
```

---

## 7. Expected log output

A single triggered alert produces this five-line chain in Terminal 2
(IDs, prices and durations will differ on each run):

```
... INFO ... [scheduling-1] c.g.a.h.s.MarketSentinelScheduler        : MARKET_SENTINEL_TICK started
... INFO ... [scheduling-1] c.g.a.h.c.e.SpringDomainEventPublisher   : DOMAIN_EVENT_PUBLISHED type=cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent payload=WatchlistAlertTriggeredEvent[watchlistId=..., userId=alice, ticker=Ticker[value=AAPL], alertType=PRICE_THRESHOLD_REACHED, threshold=9999.00, currentPrice=437.21, occurredOn=..., message=Threshold 9999.00 reached for AAPL on watchlist 'Tech' (current=437.21)]
... INFO ... [scheduling-1] c.g.a.h.s.MarketSentinelScheduler        : MARKET_SENTINEL_TICK finished durationMs=18
... INFO ... [task-1]       c.g.a.h.n.WatchlistAlertNotificationListener : WATCHLIST_ALERT_LISTENER_RECEIVED user=alice watchlist=... ticker=AAPL threshold=9999.00 current=437.21 message=Threshold 9999.00 reached for AAPL on watchlist 'Tech' (current=437.21)
... INFO ... [task-1]       c.g.a.h.n.WatchlistAlertNotificationListener : WATCHLIST_ALERT_LISTENER_DISPATCH user=alice destinations=1
... INFO ... [task-1]       h.n.a.l.LoggingNotificationSenderAdapter     : WATCHLIST_ALERT user=alice watchlist=... ticker=AAPL type=PRICE_THRESHOLD_REACHED threshold=9999.00 current=437.21 occurredOn=... message=Threshold 9999.00 reached for AAPL on watchlist 'Tech' (current=437.21)
```

The LOG adapter's final line follows exactly this format
([source](adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/adapter/logging/LoggingNotificationSenderAdapter.java#L31-L40)):

```
WATCHLIST_ALERT user={userId} watchlist={watchlistId} ticker={ticker} type={alertType} threshold={threshold} current={currentPrice} occurredOn={instant} message={message}
```

The **thread name change** from `[scheduling-1]` (the publisher /
scheduler thread) to `[task-N]` (the Spring async executor) on the last
three lines is the visual proof that the `@ApplicationModuleListener`
ran AFTER_COMMIT and asynchronously. This is one of the strongest
talking points of the demo — point at the thread name in Terminal 2.

What to point at on screen, line by line:
- `MARKET_SENTINEL_TICK started/finished` ⇒ the scheduler is alive (visible even on quiet cycles).
- `DOMAIN_EVENT_PUBLISHED` ⇒ the publisher port handed an event to Spring's bus.
- `WATCHLIST_ALERT_LISTENER_RECEIVED` ⇒ the Notifications module received the event across the module boundary.
- `WATCHLIST_ALERT_LISTENER_DISPATCH ... destinations=N` ⇒ fan-out count — with no telegram profile this is always `1`.
- `WATCHLIST_ALERT user=...` ⇒ the LOG sender produced the final notification.
- `[task-N]` thread name on the last three lines ⇒ async, after-commit delivery.

---

## 8. Pre-demo sanity tests

Run these the morning of the demo. None of them needs MySQL — they
either use Testcontainers or pure unit tests.

```bash
# (a) Whole reactor — green is the only acceptable state
./mvnw -q -T 1C clean verify

# (b) The end-to-end event flow (publisher → modulith → listener → LOG sender)
./mvnw -q -pl bootstrap test \
  -Dtest=NotificationsEventFlowIntegrationTest

# (c) Modulith structure + documentation generation
./mvnw -q -pl bootstrap test \
  -Dtest=ModulithVerificationTest

# (d) Watchlist domain rules
./mvnw -q -pl domain test -Dtest=WatchlistTest

# (e) Sentinel application service (publishes the event)
./mvnw -q -pl application test \
  -Dtest=MarketSentinelServiceTest

# (f) JPA query that finds triggered alerts (threshold >= current)
./mvnw -q -pl adapters-outbound-persistence-jpa test \
  -Dtest=JpaWatchlistQueryRepositoryContractTest
```

Expected outcome of `./mvnw clean verify`: `BUILD SUCCESS`,
`Tests run: <total>, Failures: 0, Errors: 0, Skipped: 0`.

---

## 9. Quick troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| App fails to start with `Communications link failure` to `localhost:3307` | MySQL container not running / not yet ready. | `docker compose up -d mysql` and wait until `docker compose ps mysql` shows `Up`/`healthy`. |
| Startup banner says `The following ... profiles are active: "jpa", "alphaVantage"` (or anything other than `jpa, mockfinhub`) | Wrong active profiles — typically a stale IntelliJ Run Configuration. | Stop the app and relaunch with the Terminal 1 command in §1.3 (or in IntelliJ set Active profiles to exactly `jpa,mockfinhub` with no extra spaces). |
| `MARKET_SENTINEL_TICK ... failed` with `ExternalApiException: Alpha Vantage` | The `alphaVantage` profile is active instead of `mockfinhub`. | Same fix as the row above. |
| `MARKET_SENTINEL_TICK started` and `... finished` appear but never `DOMAIN_EVENT_PUBLISHED` | No active watchlists, or the threshold is below the mocked price. | Re-run the setup .http (§2) and confirm the watchlist is active with `threshold=9999.00`. |
| `DOMAIN_EVENT_PUBLISHED` appears but never `WATCHLIST_ALERT_LISTENER_RECEIVED` | The publisher ran outside a transaction — Spring's `AFTER_COMMIT` listener silently drops the event. | Confirm `@Transactional` is on `MarketSentinelScheduler.runDetection()`. This is the root cause behind "events publish but the notifications module never reacts". |
| `WATCHLIST_ALERT_LISTENER_RECEIVED` appears but no `WATCHLIST_ALERT user=...` line | The listener says `No NotificationSender available for channel TELEGRAM`. | The `telegram-notifications` profile got activated by accident. Restart with the exact command in §1.3. |
| Log line appears in `[scheduling-1]` thread instead of `[task-N]` | Spring async executor not configured / `@EnableAsync` missing. | The demo still works but loses the AFTER_COMMIT/async talking point. Verify `@EnableAsync` on the bootstrap configuration. |
| `BUILD FAILURE` mentioning `cannot find symbol class WatchlistId` (or similar) under `domain/.../model/watchlist/` | Stale orphan files from the Watchlists Bounded Context extraction. | These are not on `origin`; if they reappear locally, delete them: `rm -rf domain/src/main/java/cat/gencat/agaur/hexastock/model/watchlist`. |
| Someone in the audience asks to see the `event_publication` table in MySQL | The current build uses the in-memory registry (no `spring-modulith-starter-jpa`), so that table does not exist. | Acknowledge it explicitly and point to §4.3: the durable registry is the first production-hardening step (Evolution 1). |
| `WATCHLIST_ALERT` log line never appears even with `threshold=9999.00` | Either the watchlist is inactive (§2.3), or the listener threw and — because the registry is in-memory — the event is gone with no retry. | Re-check activation; inspect the app log for a stack trace from `WatchlistAlertNotificationListener` or `LoggingNotificationSenderAdapter` immediately around the expected delivery. |
| Demo computer offline | Nothing here needs the internet. The market data adapter is `mockfinhub`. The only network call is to the local MySQL container. | None — go ahead. |

---

### Cleanup after the demo

```bash
# Stop the app: Ctrl+C in the spring-boot:run terminal.
docker compose down
```
