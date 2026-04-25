# 05 — Instructor Guide for the Monday Session

> **Audience.** Yourself, on Monday morning, ten minutes before the room fills up.
> **Reading time.** ~10 minutes; refer back during the session.

---

## 1. Session objective

By the end of the session, every participant should be able to:

1. Explain how DDD, Hexagonal Architecture, and Spring Modulith coexist in
   HexaStock and what each style enforces.
2. Walk the Watchlists / Market Sentinel domain event flow end-to-end and
   defend the design choices in it.
3. Apply the same pattern themselves to the Sell Stocks use case during the
   hands-on exercise.
4. Articulate at least three realistic evolution paths beyond the current
   in-process design, and the trade-offs of each.

---

## 2. Suggested agenda (half-day, ~3.5 h with breaks)

| Time | Block | Material |
|---|---|---|
| 0:00 – 0:05 | Welcome, framing, what we will *not* cover | Slide 1 |
| 0:05 – 0:30 | HexaStock and the four architectural styles | [00-ARCHITECTURE-OVERVIEW.md](00-ARCHITECTURE-OVERVIEW.md) |
| 0:30 – 1:00 | Filesystem, Maven and Modulith structure (live tour) | [01-FILESYSTEM-AND-MAVEN-STRUCTURE.md](01-FILESYSTEM-AND-MAVEN-STRUCTURE.md) |
| 1:00 – 1:15 | **Break** | — |
| 1:15 – 1:55 | Watchlists / Market Sentinel deep dive (live demo) | [02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md](02-WATCHLISTS-EVENT-FLOW-DEEP-DIVE.md) |
| 1:55 – 2:20 | Layout alternatives & trade-offs (whiteboard discussion) | [03-LAYOUT-ALTERNATIVES.md](03-LAYOUT-ALTERNATIVES.md) |
| 2:20 – 2:40 | Production evolution path | [04-PRODUCTION-EVOLUTION.md](04-PRODUCTION-EVOLUTION.md) |
| 2:40 – 2:50 | **Break** | — |
| 2:50 – 3:25 | Hands-on exercise: Sell Stocks with Domain Events | [SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](../../tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md) |
| 3:25 – 3:35 | Group debrief on exercise | — |
| 3:35 – 3:50 | Wrap-up, Q&A, next steps | [CHEATSHEET.md](../CHEATSHEET.md) |

If pressed for time, drop the production-evolution block to 10 minutes and
keep everything else.

---

## 3. Recommended flow

### 3.1 Open with the *thesis*, not with the project

Spend the first five minutes stating what the project is *not*: not
microservices, not event-sourced, not CQRS-everywhere, not asynchronous
everything. The thesis: a *deliberately conservative* architectural shape
that gives compile-time enforcement of two orthogonal things — *layer
purity* and *bounded-context isolation* — without paying the operational
cost of distribution.

### 3.2 Tour the filesystem live, with the IDE projector

Open the IDE on the projector and walk through:

```
domain/src/main/java/cat/gencat/agaur/hexastock/watchlists/model/watchlist/Watchlist.java
application/src/main/java/cat/gencat/agaur/hexastock/watchlists/WatchlistAlertTriggeredEvent.java
application/src/main/java/cat/gencat/agaur/hexastock/watchlists/application/service/MarketSentinelService.java
bootstrap/src/main/java/cat/gencat/agaur/hexastock/watchlists/package-info.java
adapters-outbound-notification/src/main/java/cat/gencat/agaur/hexastock/notifications/WatchlistAlertNotificationListener.java
```

Each file is a different point on the same vertical slice. Make the audience
*see* that the BC name is the second package segment in every Maven module.

### 3.3 Run the verification tests live

```bash
./mvnw -pl bootstrap test -Dtest=ModulithVerificationTest
./mvnw -pl bootstrap test -Dtest=HexagonalArchitectureTest
```

Then, for theatre value, **deliberately break the rule**: in
`MarketSentinelService` add `import org.springframework.stereotype.Service;`
and re-run `HexagonalArchitectureTest` so it fails publicly. Revert. The
visceral demonstration of *the build refusing the architectural error* is
the single most memorable moment of the session.

### 3.4 Run the integration test as the demo

`NotificationsEventFlowIntegrationTest` exercises the whole chain in a
single JVM. Run it on the projector and walk through the log output as the
event flows:

```bash
./mvnw -pl bootstrap test -Dtest=NotificationsEventFlowIntegrationTest
```

Then read the test method top to bottom to convince the audience that what
they just saw is indeed a real end-to-end exercise.

### 3.5 Do the layout-alternatives discussion as a discussion, not a lecture

Show the side-by-side diagram, then ask the room: *"which of A, B, C, D
would you pick for a project of this size, and why?"*. Let the answers come.
Steer toward the two main pivots: (i) what each layout enforces vs. what it
asks you to trust, and (ii) at what team size each layout starts to pay off.

### 3.6 Set up the exercise *before* the break

Push participants to clone the repository (or pull the branch) during the
break so the hands-on time is spent on the architecture, not on git.

---

## 4. Key talking points (memorise three of these, improvise the rest)

1. **"The build is the architecture's bouncer."** Maven module dependencies
   enforce hexagonal layering; `MODULES.verify()` enforces bounded-context
   isolation. Neither relies on review discipline.
2. **"`@ApplicationModuleListener` is just three annotations in a trench
   coat."** `@TransactionalEventListener(AFTER_COMMIT) + @Async +
   @Transactional(REQUIRES_NEW)`. Once you understand each piece, the
   transactional safety of the in-process event bus stops feeling magical.
3. **"The event carries business identity, not infrastructure identity."**
   The single most common rookie mistake is putting Telegram chat ids in the
   event. Don't. Resolve channels in the consumer.
4. **"The publisher does not know the consumer exists."** That is the
   property you are paying for, and the property the exercise tests.
5. **"In-process domain events are the *first* step of an evolution, not the
   *last* one."** Outbox, externalisation, extraction are all preserved
   options.
6. **"Bounded contexts are not microservices."** They are *modelling
   decisions*. Microservices are *deployment decisions*. Conflating them is
   the textbook error this project avoids.

---

## 5. Demo sequence (cut-and-keep card)

1. Show the Maven dependency graph in the IDE.
2. Show two `package-info.java` files for the same `watchlists` package.
3. Show `MarketSentinelService` and walk the `detectBuySignals()` method.
4. Show `WatchlistAlertTriggeredEvent` (the record).
5. Show `WatchlistAlertNotificationListener` (the consumer).
6. Run `ModulithVerificationTest`.
7. Run `HexagonalArchitectureTest`.
8. Run `NotificationsEventFlowIntegrationTest`.
9. (Optional theatre) Break a rule, watch the build fail, revert.

---

## 6. Discussion prompts

Use these to draw the audience out:

- *Where in this codebase would you put a "user signed up" event, and why?*
- *Suppose Marketing wants every stock sale to trigger an email. Where do
  you change code? How does your answer differ between the current sell
  flow and the target sell flow?*
- *Should `notifications` know about `marketdata`? Today it does, because
  `Ticker` is in the event payload. Is that a leak?*
- *If you had to remove Spring Modulith tomorrow, what would you lose?
  What would you keep?*
- *What would you measure on Monday morning to know whether the in-process
  event bus is starting to cost you?*
- *At what team size does Layout C (per-BC mini-hexagons) start to be
  worth it?*

---

## 7. Common misconceptions to address proactively

| Misconception | Correction |
|---|---|
| "Spring Modulith is a microservices framework." | It is a build-time module verifier and an in-process event bus with transactional semantics. |
| "Hexagonal means three concentric circles in a slide." | Hexagonal means *the application defines ports, the adapters implement them*. The visual is incidental. |
| "DDD requires aggregates everywhere." | DDD requires *thinking about* aggregates. Most code is not in an aggregate. The Watchlists aggregate is small for a reason. |
| "Domain events should be published from the aggregate." | They *can* be. Here they are published from the application service because the trigger condition (price-threshold) is *not* an aggregate invariant — it is a cross-aggregate read-side observation. |
| "If we add `@Async`, we have made it scalable." | We have made it *off-thread*. Scalability requires bounded executors, back-pressure, and idempotent consumers. |
| "The outbox is what gives us exactly-once." | The outbox gives at-least-once with idempotent consumers. Exactly-once is a marketing term outside of constrained transactional contexts. |

---

## 8. Architecture trade-offs to highlight (when asked)

- **Two `package-info.java` per Modulith package.** Yes, it is friction. It
  is the price of `application` staying Spring-free (ADR-007). The trade
  could be reversed if the team accepted a Spring import in the application
  layer.
- **Per-BC vertical slicing across many Maven modules.** Yes, the IDE
  experience is denser. The pay-off is a build that refuses to compile
  layer violations.
- **In-process event bus.** Yes, it loses events on JVM crash. The
  evolution path to durable / brokered delivery is a configuration change,
  not a redesign — that is the point.

---

## 9. Suggested exercise flow (35 minutes)

| Time | What |
|---|---|
| 0:00 – 0:05 | Read [§3-§4 of the exercise document](../../tutorial/sellStocks/SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md). |
| 0:05 – 0:10 | Discuss in pairs: *what is the business fact?* |
| 0:10 – 0:25 | Implement: define the event, publish it, write the listener. |
| 0:25 – 0:30 | Add the integration test. Make it green. |
| 0:30 – 0:35 | One pair presents their event payload to the room; group critiques. |

Pre-warn participants that the exercise is *not* a code-completeness test —
it is a discussion vehicle. The interesting outputs are *what they put in
the payload, what they leave out, and where they put the listener*.

---

## 10. Wrap-up points (last five minutes)

1. The architecture is **conservative by design** — it favours enforcement
   over ergonomics.
2. The Watchlists flow is the **smallest correct example** of every concept
   covered.
3. The Sell Stocks exercise is the **same pattern, applied with one twist**
   (the consumer writes to the database).
4. Every "production-grade" property the audience asked about is a
   **preserved option**, not an architectural debt.
5. **Send the participants the doc map**:
   [`doc/consultancy/monday-session/README.md`](README.md).

Optional homework: read
[`doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md`](../05-DOMAIN-EVENTS-ROADMAP.md)
for a forward-looking catalogue of additional domain events the project
will benefit from.
