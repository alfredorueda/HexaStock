# Spring Modulith Notifications — Proof of Concept

> **Status**: experimental, scoped to branch `feature/experimental-spring-modulith-notifications`.
> **NOT** to be merged into `main` as-is. Built on top of the Watchlist / Market Sentinel feature.

## TL;DR

The Watchlist / Market Sentinel slice was originally coupled to a specific notification channel
(Telegram chat id was carried inside the `Watchlist` aggregate). This POC:

1. Removes the notification id from the **domain**.
2. Replaces the direct call from `MarketSentinelService` to a `NotificationPort` adapter with the
   in-process publication of a **domain event** (`WatchlistAlertTriggeredEvent`).
3. Introduces a dedicated **Notifications module** that listens to the event via a Spring Modulith
   `@ApplicationModuleListener` and dispatches to channel-specific senders.
4. Makes a structured **logging sender** the always-on default; the **Telegram sender** is gated
   behind the `telegram-notifications` Spring profile.
5. Stays inside the existing Maven multi-module build with no new transport (no Kafka, no RabbitMQ,
   no external broker) — the modulith runs **in-process**.

Spring Modulith adoption is intentionally **incremental**: only the two new packages
(`watchlists`, `notifications`) are subject to Modulith verification. The legacy
`model` / `application` / `adapter` / `config` packages keep using the existing hexagonal /
ArchUnit fitness rules.

---

## Why

### Problem statement

Before this POC the flow was roughly:

```
TelegramCommandHandler ──► WatchlistService.createWatchlist(owner, list, chatId)
                                              │
                                              ▼
                                   Watchlist (aggregate)
                                   ├── ownerName
                                   ├── listName
                                   └── userNotificationId  ← Telegram chat id baked in
                                              │
                       (Sentinel sweep)       │
MarketSentinelService ─► WatchlistQueryPort.findTriggeredAlerts()
                       └─► NotificationPort.notifyAlert(triggeredAlert)
                               └─► TelegramNotificationAdapter (only impl)
```

Issues:

- **Domain leakage**: the `Watchlist` aggregate, persisted in JPA *and* MongoDB, carried a
  Telegram-specific `userNotificationId`. The domain knew about a transport that has nothing to
  do with watchlist business rules.
- **Notification routing in the wrong layer**: `MarketSentinelService` (an application-layer
  service) directly invoked a transport-aware port. Adding email or Slack required either
  extending `NotificationPort` or branching inside the application service.
- **No clean module boundary**: the only outbound notification adapter (`TelegramNotificationAdapter`)
  lived alongside no-ops, with no in-process bus or named module to anchor cross-module behaviour
  on.

### Goals

- Decouple the **trigger** (sentinel sweep) from the **delivery** (Telegram, log, future channels).
- Make the new boundary **discoverable and verifiable** with Spring Modulith primitives.
- Keep everything **in-process** — this is a deployment-shape preserving refactor, not a
  microservices migration.
- Default behaviour out-of-the-box (no profile) must produce a **visible structured audit log**
  for every alert. Telegram delivery becomes opt-in via profile.

### Non-goals

- No Kafka, no Spring Cloud Stream, no `@Externalized`. The event stays JVM-local for now.
- No persistent recipient store. The default `CompositeNotificationRecipientResolver` aggregates
  one `NotificationDestinationProvider` per channel; replacing it (or any single provider)
  with a database-backed implementation is left as a future extension.
- No live registration of Telegram chat ids when a user contacts the bot. Naturally a follow-up
  POC, also via in-process domain events.

---

## Solution architecture

### High-level event flow

```
                                                ┌──────────────────────────────────────────┐
                                                │  notifications  (Modulith module)        │
                                                │  ──────────────                          │
                                                │                                          │
 watchlists module                               │  WatchlistAlertNotificationListener     │
 ─────────────────                               │      @ApplicationModuleListener         │
                                                │            │                              │
 MarketSentinelService                          │            ▼                              │
   │                                            │  NotificationRecipientResolver           │
   │  builds                                    │  (default: CompositeNotificationRecipientResolver
   │  WatchlistAlertTriggeredEvent              │            │                              │
   │                                            │            ▼                              │
   │  publish(event)                            │  ┌────────────┬────────────┐              │
   ▼                                            │  │ Logging…   │ Telegram…  │              │
 DomainEventPublisher (port)                    │  │ Sender     │ Sender     │              │
   │                                            │  │ (default)  │ (@Profile) │              │
   ▼                                            │  └────────────┴────────────┘              │
 SpringDomainEventPublisher (adapter, bootstrap)│                                            │
   │                                            └──────────────────────────────────────────┘
   ▼
 ApplicationEventPublisher  ──── in-process ────►  @ApplicationModuleListener (after-commit, async)
```

### Module map

| Module                                    | Responsibility                                                                                                  |
| ----------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `cat.gencat.agaur.hexastock.watchlists`   | **Publisher**: declares the domain event (`WatchlistAlertTriggeredEvent`). Lives in the Maven `application` module so both the publisher (also `application`) and consumer (`adapters-outbound-notification`) can see it without adapter ↔ adapter coupling. |
| `cat.gencat.agaur.hexastock.notifications`| **Consumer**: listener + recipient resolution + channel-specific senders. Lives in the Maven `adapters-outbound-notification` module. |

Both packages are top-level under the application root package, which is what Spring Modulith
treats as a module by convention.

### Domain event

```java
public record WatchlistAlertTriggeredEvent(
        String watchlistId,
        String userId,        // business identity, NOT a transport id
        Ticker ticker,
        AlertType alertType,  // currently only PRICE_THRESHOLD_REACHED
        Money threshold,
        Money currentPrice,
        Instant occurredOn,
        String message
) { /* requireNonNulls in compact ctor */ }
```

Design notes:

- **Pure POJO record**, no Spring, no JPA, no Modulith API import.
- Lives in the `application` Maven module so it is reachable from both the publisher
  (`application/.../service/MarketSentinelService`) and the consumer
  (`adapters-outbound-notification/.../notifications/...`) without introducing any new Maven
  edge between adapters.
- Uses domain value objects (`Ticker`, `Money`) — no primitive obsession.
- `userId` is the **business** identity (matches `Watchlist.ownerName`). It is the resolver's
  responsibility to translate it into one or more `NotificationDestination`s.

### Publisher port — keeping `application` Spring-free

The `application` Maven module is intentionally Spring-free (enforced by ArchUnit). To publish
an event from there without depending on Spring, a tiny outbound port is introduced:

```java
// application/.../port/out/DomainEventPublisher.java
public interface DomainEventPublisher {
    void publish(Object event);
}
```

The Spring adapter lives in `bootstrap`:

```java
// bootstrap/.../config/events/SpringDomainEventPublisher.java
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher delegate;
    public SpringDomainEventPublisher(ApplicationEventPublisher delegate) { this.delegate = delegate; }
    @Override public void publish(Object event) { delegate.publishEvent(event); }
}
```

This is the same pattern the project already uses for every other side-effectful concern:
**ports owned by application, adapters owned by infrastructure**.

### Listener — `@ApplicationModuleListener`

```java
@Component
public class WatchlistAlertNotificationListener {
    @ApplicationModuleListener
    public void on(WatchlistAlertTriggeredEvent event) {
        var recipient = recipientResolver.resolve(event.userId());
        for (var destination : recipient.destinations()) {
            var sender = pickSender(destination);
            if (sender != null) sender.send(destination, event);
        }
    }
}
```

`@ApplicationModuleListener` is, by design, equivalent to:

```
@TransactionalEventListener(phase = AFTER_COMMIT)
@Async
@Transactional(propagation = REQUIRES_NEW)
```

This gives us three guarantees that are usually painful to wire by hand:

1. **After-commit**: notifications are dispatched only when the publishing transaction commits.
   No notification for rolled-back work.
2. **Async**: the listener runs on a separate thread, so dispatching to slow channels does not
   block the publisher.
3. **Own transaction**: the listener can read/write its own state if needed without entangling
   it with the publisher's transaction.

### Recipient resolution

```java
public interface NotificationRecipientResolver {
    NotificationRecipient resolve(String userId);
}
```

Default implementation `CompositeNotificationRecipientResolver` (post-Phase-5):

- **Channel-agnostic**: aggregates every active `NotificationDestinationProvider` bean. The
  resolver itself contains zero references to Telegram, logging, or any other channel.
- One provider per channel:
  - `LoggingNotificationDestinationProvider` — always active, contributes a
    `LoggingNotificationDestination` for every user (the safe-by-default audit channel).
  - `TelegramNotificationDestinationProvider` — only loaded under the
    `telegram-notifications` profile. Reads Telegram chat ids from the SpEL-bound property
    `notifications.telegram.chat-ids={alice:'123', bob:'456'}` and contributes a
    `TelegramNotificationDestination(chatId)` when one is configured.
- Adding a new channel is a zero-change task on the resolver: ship one
  `NotificationDestinationProvider` + one `NotificationSender`, both gated by the same profile.

To replace it with a persistent or service-backed resolver, declare another
`NotificationRecipientResolver` bean annotated with `@Primary`.

### Senders

Two senders ship with this POC:

| Sender                                    | Bean condition                       | Behaviour                                                         |
| ----------------------------------------- | ------------------------------------ | ----------------------------------------------------------------- |
| `LoggingNotificationSenderAdapter`        | always (default `@Component`)        | Emits one structured SLF4J line per alert (`WATCHLIST_ALERT user=… ticker=… …`) |
| `TelegramNotificationSenderAdapter`       | `@Profile("telegram-notifications")` | Calls the Telegram Bot API via `RestClient` using the bot token configured in properties |

This explicitly inverts the previous "Telegram or no-op" model: **structured logging is the
guaranteed channel**, Telegram is opt-in.

---

## What changed in the codebase

### Removed

- `userNotificationId` field, getter, constructor parameter and persistence column from the
  `Watchlist` aggregate (JPA + MongoDB).
- `application/.../port/out/NotificationPort.java` and the obsolete `BuySignal.java`.
- `NoopNotificationAdapter` and the previous `TelegramNotificationAdapter` (the new one is
  channel-specific, not domain-coupled).

### Added

- `application/.../watchlists/WatchlistAlertTriggeredEvent.java` and `package-info.java`.
- `application/.../port/out/DomainEventPublisher.java`.
- `bootstrap/.../config/events/SpringDomainEventPublisher.java`.
- New `notifications` package under `adapters-outbound-notification`:
  - `NotificationChannel` enum, `NotificationDestination` sealed interface and concrete records
    (`LoggingNotificationDestination`, `TelegramNotificationDestination`).
  - `NotificationRecipient`, `UserNotificationPreference`, `NotificationRecipientResolver`,
    `CompositeNotificationRecipientResolver`, `NotificationDestinationProvider`.
  - `NotificationSender` interface + `LoggingNotificationSenderAdapter` (default) +
    `TelegramNotificationSenderAdapter` (`@Profile("telegram-notifications")`).
  - `WatchlistAlertNotificationListener` using `@ApplicationModuleListener`.
- Spring Modulith infrastructure:
  - `<spring-modulith.version>1.4.0</spring-modulith.version>` + BOM import in the root pom.
  - `spring-modulith-starter-core` (compile) and `spring-modulith-starter-test` (test) on the
    `bootstrap` module.
  - `spring-modulith-events-api` on the notification adapter (for `@ApplicationModuleListener`).
  - `@Modulithic(systemName = "HexaStock")` on `HexaStockApplication`.
- Tests:
  - [bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/ModulithVerificationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/ModulithVerificationTest.java)
    — verifies module structure scoped to the new `watchlists` and `notifications` packages and
    renders Modulith documentation under `target/spring-modulith-docs`.
  - [bootstrap/src/test/java/cat/gencat/agaur/hexastock/notifications/NotificationsEventFlowIntegrationTest.java](../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/notifications/NotificationsEventFlowIntegrationTest.java)
    — boots the full Spring context, publishes a `WatchlistAlertTriggeredEvent` inside a
    transaction via the application port, and asserts (with Awaitility + Mockito spy) that the
    `LoggingNotificationSenderAdapter` was invoked.

### Updated

- `MarketSentinelService` constructor takes `DomainEventPublisher` (and `Clock`); for each
  triggered alert it builds a `WatchlistAlertTriggeredEvent` and publishes it.
- `WatchlistService` and `WatchlistUseCase` no longer accept a notification id when creating
  watchlists. REST DTOs and the Telegram inbound handler reflect this.
- The Telegram inbound bot still uses `chatId` to send replies, but it is no longer pushed
  into the `Watchlist` aggregate.

---

## How to run

```bash
# unit + integration tests, including Modulith verification + event-flow IT
./mvnw -q clean test
```

Expected: `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`.

The Modulith documentation diagrams (PlantUML / module canvas) are written under
`bootstrap/target/spring-modulith-docs/` after tests run.

### Enabling Telegram delivery

```properties
spring.profiles.active=...,telegram-notifications
notifications.telegram.bot-token=<bot-token>
notifications.telegram.chat-ids.alice=123456789
notifications.telegram.chat-ids.bob=987654321
```

Without the `telegram-notifications` profile the Telegram sender bean is **not** registered
and only the logging sender runs.

---

## Trade-offs and caveats

- **Modulith verification is scoped, not global.** The legacy hexagonal layout
  (`model`/`application`/`adapter`/`config`) does not match Modulith's named-interface convention,
  so a global `ApplicationModules.of(HexaStockApplication.class).verify()` would surface dozens
  of pre-existing inter-package edges. The verification test deliberately limits inspection to
  the new POC packages. Migrating the legacy modules to a Modulith-friendly structure is a
  separate, much larger effort.
- **No event externalisation.** The event stays JVM-local. If we ever need to fan out to other
  services we can layer Spring Modulith's `@Externalized` or `spring-modulith-events-kafka`
  on top **without touching the domain**.
- **No persistent recipient store.** Replacing `CompositeNotificationRecipientResolver` (or
  swapping a single `NotificationDestinationProvider`) is the
  obvious next step.
- **`@SpringBootTest` for the event-flow IT.** The event-flow test uses a full `@SpringBootTest`
  rather than `@ApplicationModuleTest` because the latter triggers global Modulith verification
  (see first bullet) and would fail on the legacy module structure.
- **No live Telegram chat-id registration.** When a real user contacts the bot we currently rely
  on a static property map. A future POC could add an `OnboardingRequestedEvent` flowing from
  the Telegram inbound to the Notifications module.

---

## Future extensions

- **Event-driven Telegram chat-id self-registration**: the inbound bot publishes
  `UserContactedBotEvent`, the Notifications module persists the resulting
  `UserNotificationPreference`.
- **Persistent `NotificationRecipientResolver`** backed by JPA / MongoDB.
- **Outbox + externalisation**: enable `spring-modulith-events-jpa` for at-least-once delivery
  and `spring-modulith-events-kafka` for fan-out, all while keeping the same listener API.
- **Additional alert types**: extend `AlertType` (e.g. `MOVING_AVERAGE_CROSS`,
  `VOLUME_SPIKE`) without changing the listener signature.
- **Migrate legacy hexagonal packages to Modulith named interfaces** so global Modulith
  verification can replace (or complement) the existing ArchUnit fitness rules.

---

## Constraints honoured

- Done on `feature/experimental-spring-modulith-notifications`, branched from
  `feat/watchlists-market-sentinel-l1-l5`. **Not** merged.
- `main` and existing feature branches untouched.
- `application` Maven module remains Spring-free.
- Existing hexagonal architecture, ArchUnit fitness rules, and CQRS read-side for Market
  Sentinel are preserved.
- No external broker, no new deployment unit, no breaking change to the public REST API
  beyond removing the now-unused `userNotificationId` field from request/response DTOs.
