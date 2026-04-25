# 06 — Slide Deck Specification (AI-feedable)

> **Purpose.** A structured, slide-by-slide specification suitable for direct
> ingestion by an AI slide-generation tool (Beautiful.ai, Gamma, Tome,
> Microsoft Designer, Google's NotebookLM slide export, etc.).
>
> **Audience of the resulting deck.** Senior Java engineers and architects.
> Tone: precise, technical, free of marketing language.
>
> **Visual style.** Clean, low-chrome, technical. White backgrounds. One
> diagram per architecture slide. Diagram references point to the rendered
> SVGs in [`diagrams/Rendered/`](diagrams/Rendered/) and (for the exercise)
> in [`../../tutorial/sellStocks/diagrams/Rendered/`](../../tutorial/sellStocks/diagrams/Rendered/).
> Use SVG when supported.
>
> **Suggested total length.** 28 slides, ~45 minutes of speaking time.

---

## How to feed this to a slide generator

Each slide block below contains the same fields:

- **#** — slide number.
- **Title** — slide title.
- **Objective** — the one thing the audience must take away.
- **Bullets** — concise content (3 – 5 bullets).
- **Visual** — what to put on the slide; a path to a rendered diagram or a
  prose description of a chart.
- **Speaker notes** — what to actually say.
- **Timing** — suggested speaking time.

You may pass the document verbatim, or extract the fields you need.

---

## Slide 1

- **Title:** HexaStock — DDD, Hexagonal, Spring Modulith and Domain Events
- **Objective:** Frame the session and set expectations.
- **Bullets:**
  - A small, real, financial-portfolio backend used as a teaching asset
  - Combines four architectural styles deliberately
  - Half-day session; theory + live demo + hands-on exercise
- **Visual:** Title card. No diagram.
- **Speaker notes:** Welcome. State explicitly what we will *not* cover:
  microservices, event sourcing, CQRS-as-religion, message brokers in
  production. Emphasise: the architecture is *deliberately conservative*.
- **Timing:** 2 min.

## Slide 2

- **Title:** What HexaStock is
- **Objective:** Anchor the business model in two sentences.
- **Bullets:**
  - Open a portfolio, deposit cash, buy and sell stocks (FIFO)
  - Maintain watchlists with price-threshold alerts → notifications
  - Single Spring Boot deployable; JPA *or* MongoDB; Telegram or logging
- **Visual:** None (text card) or a thumbnail of the architecture overview.
- **Speaker notes:** Keep it brief. The point is to establish the *domain*
  vocabulary before introducing any architectural term.
- **Timing:** 2 min.

## Slide 3

- **Title:** Four architectural styles, one codebase
- **Objective:** Introduce the thesis.
- **Bullets:**
  - DDD — *where* the boundaries lie
  - Hexagonal — *between which layers* the boundaries lie
  - Spring Modulith — *enforces* both at build time
  - Domain Events — let modules *react* without coupling
- **Visual:** [`Rendered/01-architecture-overview.svg`](diagrams/Rendered/01-architecture-overview.svg)
- **Speaker notes:** Four styles, four jobs. The combination is what
  matters; any one of them on its own would not do.
- **Timing:** 3 min.

## Slide 4

- **Title:** The four bounded contexts
- **Objective:** Make the BCs concrete.
- **Bullets:**
  - `portfolios` — Portfolio aggregate, FIFO trading, Transaction ledger
  - `marketdata` — Ticker, StockPrice, MarketDataPort
  - `watchlists` — Watchlist aggregate, Market Sentinel scheduler
  - `notifications` — channels, destinations, senders
- **Visual:** [`Rendered/05-bounded-context-map.svg`](diagrams/Rendered/05-bounded-context-map.svg)
- **Speaker notes:** Note the asymmetry: `notifications` depends on
  `watchlists` (because it consumes the event); the inverse never holds.
- **Timing:** 2 min.

## Slide 5

- **Title:** Maven multi-module layout
- **Objective:** Show the physical shape of the codebase.
- **Bullets:**
  - 9 Maven modules: domain, application, adapters-*, bootstrap
  - `application` does *not* depend on Spring (ADR-007)
  - `bootstrap` is the only Spring Boot module
- **Visual:** [`Rendered/02-maven-multimodule.svg`](diagrams/Rendered/02-maven-multimodule.svg)
- **Speaker notes:** The Maven graph is the layer-purity *enforcement
  mechanism*. A leak does not compile.
- **Timing:** 3 min.

## Slide 6

- **Title:** Filesystem layout — three orthogonal axes
- **Objective:** Resolve the "why is the same package in many trees?"
  confusion.
- **Bullets:**
  - Hex layer → Maven module
  - Bounded context → top-level Java package
  - Adapter tech → distinct adapter Maven module
- **Visual:** [`Rendered/03-filesystem-layout.svg`](diagrams/Rendered/03-filesystem-layout.svg)
- **Speaker notes:** Internalise this slide and the rest of the session is
  a walk in the park.
- **Timing:** 3 min.

## Slide 7

- **Title:** Spring Modulith application modules
- **Objective:** Show what Modulith adds on top.
- **Bullets:**
  - 4 modules detected automatically by package
  - `@ApplicationModule` declares allowed dependencies
  - `MODULES.verify()` fails the build on cycles or undeclared imports
- **Visual:** [`Rendered/04-modulith-modules.svg`](diagrams/Rendered/04-modulith-modules.svg)
- **Speaker notes:** This is the BC-isolation enforcement mechanism.
  Hexagonal enforces layers; Modulith enforces contexts.
- **Timing:** 3 min.

## Slide 8

- **Title:** Hexagonal — per bounded context, not global
- **Objective:** Correct the most common architectural misreading.
- **Bullets:**
  - One hexagon per BC (today: 4 hexagons)
  - Primary ports: `…UseCase` interfaces
  - Secondary ports: `…Port` interfaces (repository, market data, event publisher)
  - Adapters live in their own Maven modules
- **Visual:** [`Rendered/06-hexagonal-view.svg`](diagrams/Rendered/06-hexagonal-view.svg)
- **Speaker notes:** "Hexagonal" is a layering rule, not a single hexagon
  drawn for the whole app.
- **Timing:** 3 min.

## Slide 9

- **Title:** Watchlists / Market Sentinel — what it does
- **Objective:** Set up the demo.
- **Bullets:**
  - Scheduler scans active watchlists
  - Reads current prices for distinct tickers
  - Emits `WatchlistAlertTriggeredEvent` per triggered alert
  - Notifications module dispatches per channel
- **Visual:** [`Rendered/07-watchlist-sentinel-sequence.svg`](diagrams/Rendered/07-watchlist-sentinel-sequence.svg)
- **Speaker notes:** The flow is short; walk it verbally before showing the
  code.
- **Timing:** 3 min.

## Slide 10

- **Title:** The event itself — a Java record
- **Objective:** Show what a *good* domain event looks like.
- **Bullets:**
  - `record WatchlistAlertTriggeredEvent(...)` — immutable, value-equal
  - Carries business identity (`userId`), not infrastructure identity
  - Validates non-null at construction
  - Timestamped at the source via injected `Clock`
  - Enum-typed `AlertType`, evolvable
- **Visual:** Code snippet of the record (~10 lines, monospace).
- **Speaker notes:** The five properties on the slide are the recurring
  pattern; if the audience remembers only one slide, this is it.
- **Timing:** 3 min.

## Slide 11

- **Title:** Where the event lives
- **Objective:** Trace the publication path through the codebase.
- **Bullets:**
  - Event in `application/.../watchlists/`
  - Port: `DomainEventPublisher` (Spring-free)
  - Adapter: `SpringDomainEventPublisher` in `bootstrap/`
  - Consumer: `WatchlistAlertNotificationListener` in `notifications`
- **Visual:** [`Rendered/08-watchlist-event-flow.svg`](diagrams/Rendered/08-watchlist-event-flow.svg)
- **Speaker notes:** The application layer never imports Spring. Only the
  adapter does.
- **Timing:** 2 min.

## Slide 12

- **Title:** `@ApplicationModuleListener` = three annotations in a trench coat
- **Objective:** Demystify the listener annotation.
- **Bullets:**
  - `@TransactionalEventListener(AFTER_COMMIT)` — only fires on commit
  - `@Async` — off the publisher's thread
  - `@Transactional(REQUIRES_NEW)` — own transaction, own failure budget
  - This is what makes in-process events *safe*, not just *convenient*
- **Visual:** Three concentric annotation pills with the listener method
  inside; or just a code block.
- **Speaker notes:** This is the slide where senior engineers stop being
  sceptical. Take your time.
- **Timing:** 3 min.

## Slide 13

- **Title:** Notification dispatch — pluggable senders
- **Objective:** Show how the consumer side handles channels.
- **Bullets:**
  - `NotificationRecipientResolver` aggregates destinations per user
  - `NotificationSender.supports(destination)` selects the sender
  - Telegram sender is profile-gated
  - Adding SMS = three new classes, zero changes upstream
- **Visual:** [`Rendered/09-notification-flow.svg`](diagrams/Rendered/09-notification-flow.svg)
- **Speaker notes:** This is where the value of the asymmetric coupling
  becomes obvious.
- **Timing:** 3 min.

## Slide 14

- **Title:** Live demo — the build refusing the rule
- **Objective:** Make the architectural enforcement *visceral*.
- **Bullets:**
  - Run `ModulithVerificationTest`
  - Run `HexagonalArchitectureTest`
  - Add an illegal Spring import → watch the build fail
  - Revert
- **Visual:** A still of an IDE test runner showing red bars; or a terminal
  screenshot of the failing test.
- **Speaker notes:** Do not skip this. It is the most memorable five
  minutes of the session.
- **Timing:** 5 min.

## Slide 15

- **Title:** Layout alternatives at a glance
- **Objective:** Set up the discussion.
- **Bullets:**
  - A. Layered, single Maven module
  - B. Package-by-feature, single Maven module
  - C. Per-BC mini-hexagons
  - D. Modulith-first (top-level package per module)
  - E. HexaStock today (Hex × BC, by Maven *and* by package)
- **Visual:** [`Rendered/10-layout-alternatives.svg`](diagrams/Rendered/10-layout-alternatives.svg)
- **Speaker notes:** Show the slide, then *ask the room which they would
  pick*. Let the discussion happen.
- **Timing:** 4 min.

## Slide 16

- **Title:** Why the current shape, in four lines
- **Objective:** Give the audience a quotable summary.
- **Bullets:**
  - Layer purity → enforced by Maven modules
  - BC isolation → enforced by Spring Modulith
  - Cost: one extra `package-info.java` per module + a denser Maven graph
  - Benefit: leaks fail to compile
- **Visual:** None or a quote card.
- **Speaker notes:** This is the *quotable* version of the thesis.
- **Timing:** 2 min.

## Slide 17

- **Title:** What the in-process event bus actually guarantees
- **Objective:** Be honest about the limits.
- **Bullets:**
  - ✅ Delivered iff publisher's transaction commits
  - ✅ Listener does not slow down the publisher
  - ✅ Listener failure does not roll back the publisher
  - ❌ Survives a JVM crash
  - ❌ Has retries
- **Visual:** A two-column table (✅ / ❌).
- **Speaker notes:** This is the slide that buys credibility with the
  ops/SRE-leaning members of the audience.
- **Timing:** 3 min.

## Slide 18

- **Title:** Evolution path 1 — durability inside the monolith
- **Objective:** Show that the monolith can become *durable* without being
  rewritten.
- **Bullets:**
  - Add `spring-modulith-starter-jpa`
  - Events persisted to outbox in same transaction
  - Listener completion tracked, retried until success
  - Idempotency becomes a real concern
- **Visual:** Left half of [`Rendered/11-current-vs-future-events.svg`](diagrams/Rendered/11-current-vs-future-events.svg)
- **Speaker notes:** Single dependency change.
- **Timing:** 3 min.

## Slide 19

- **Title:** Evolution path 2 — externalise to a broker
- **Objective:** Show the seam to async cross-process.
- **Bullets:**
  - `@Externalized("watchlist.alert.triggered.v1")`
  - Event becomes a *published contract* — schema discipline begins
  - Out-of-process consumers become possible
- **Visual:** Middle of [`Rendered/11-current-vs-future-events.svg`](diagrams/Rendered/11-current-vs-future-events.svg)
- **Speaker notes:** The architecture *preserves* this option. It does
  not commit to it.
- **Timing:** 3 min.

## Slide 20

- **Title:** Evolution path 3 — physical extraction
- **Objective:** Demystify the path to "microservices, when justified".
- **Bullets:**
  - Notifications can be its own deployable
  - Shared `Ticker` becomes a question (vendor / library / replace with String)
  - Operational concerns enter; modelling stays the same
  - This is a *capability you preserve*, not a *target you commit to*
- **Visual:** Right of [`Rendered/11-current-vs-future-events.svg`](diagrams/Rendered/11-current-vs-future-events.svg)
- **Speaker notes:** This is the slide where someone will say *"so why
  not just start with microservices?"* — be ready with: "because we'd be
  paying the cost without having validated that we need it".
- **Timing:** 3 min.

## Slide 21

- **Title:** Hands-on exercise — Sell Stocks with Domain Events
- **Objective:** Set up the exercise.
- **Bullets:**
  - Today: `sellStock(...)` synchronously writes the `Transaction`
  - Goal: emit a `StockSoldEvent` and let a listener write the transaction
  - Reuse `DomainEventPublisher` and `@ApplicationModuleListener`
  - Same pattern as Watchlists, applied to a *write-side* consumer
- **Visual:** [`sell-events-conceptual.svg`](../../tutorial/sellStocks/diagrams/Rendered/sell-events-conceptual.svg)
- **Speaker notes:** Set the framing: this is the same pattern, with a
  twist (the consumer writes).
- **Timing:** 3 min.

## Slide 22

- **Title:** Sell Stocks — current synchronous flow
- **Objective:** Show what we are refactoring.
- **Bullets:**
  - Controller → service → portfolio repo → market data → portfolio repo → transaction repo
  - Two persistence side effects in one method
  - Adding any new reaction means modifying `sellStock(...)`
- **Visual:** [`sell-events-current.svg`](../../tutorial/sellStocks/diagrams/Rendered/sell-events-current.svg)
- **Speaker notes:** Read the current method top to bottom.
- **Timing:** 3 min.

## Slide 23

- **Title:** Sell Stocks — target event-driven flow
- **Objective:** Show the destination.
- **Bullets:**
  - `sellStock(...)` writes the portfolio and publishes the event
  - `SaleTransactionRecordingListener` writes the transaction (AFTER_COMMIT)
  - New reactions = new listeners, not edits to `sellStock(...)`
- **Visual:** [`sell-events-target.svg`](../../tutorial/sellStocks/diagrams/Rendered/sell-events-target.svg)
- **Speaker notes:** Emphasise that the change is *additive*. Future
  reactions will not touch the method again.
- **Timing:** 3 min.

## Slide 24

- **Title:** Exercise tasks (35 minutes)
- **Objective:** Hand the work to the room.
- **Bullets:**
  - Define `StockSoldEvent` (decide payload — *defend* it)
  - Replace the synchronous transaction write with a publication
  - Add `SaleTransactionRecordingListener`
  - Add an integration test mirroring `NotificationsEventFlowIntegrationTest`
- **Visual:** None or a checklist card.
- **Speaker notes:** Pair them up. Pre-warn: the exercise grade is *the
  decisions they defend*, not the line count.
- **Timing:** 1 min.

## Slide 25

- **Title:** Common mistakes during the exercise
- **Objective:** Pre-empt the predictable.
- **Bullets:**
  - Putting persistence ids of the *Transaction* in the event payload
  - Calling the listener from inside the publishing transaction
  - Letting the listener throw (forgetting REQUIRES_NEW semantics)
  - Adding `@Component` on the application service
- **Visual:** None.
- **Speaker notes:** Walk the room. Catch these before they spread.
- **Timing:** 2 min.

## Slide 26

- **Title:** Stretch — `LotSoldEvent` per FIFO consumption
- **Objective:** Tease the deeper pattern.
- **Bullets:**
  - One aggregate operation, multiple emitted events
  - Per-lot event enables tax-lot reporting downstream
  - Discussed in `doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md`
- **Visual:** None or a small fan-out diagram.
- **Speaker notes:** Optional content; mention only if time allows.
- **Timing:** 2 min.

## Slide 27

- **Title:** What to take away
- **Objective:** Close the loop.
- **Bullets:**
  - The build enforces the architecture; reviews don't
  - Domain events are an *internal coordination mechanism* before they are
    an *integration mechanism*
  - In-process today, durable + brokered tomorrow, extractable the day
    after — same domain code
  - Bounded contexts are modelling decisions, not deployment decisions
- **Visual:** A short bullet card.
- **Speaker notes:** This is the slide they will photograph.
- **Timing:** 3 min.

## Slide 28

- **Title:** Q&A and references
- **Objective:** Hand off to the room.
- **Bullets:**
  - [`doc/consultancy/monday-session/`](README.md) — this folder
  - [`doc/consultancy/`](../) — full briefing pack
  - [`doc/architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md`](../../architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md)
  - [`doc/architecture/adr/`](../../architecture/adr/) — ADRs (especially ADR-007)
  - [Spring Modulith reference](https://docs.spring.io/spring-modulith/reference/)
- **Visual:** A reference card with QR codes if printed.
- **Speaker notes:** Keep this slide up during Q&A.
- **Timing:** 5+ min Q&A.

---

## Notes for the slide generator

- Prefer **SVG** over PNG when the target tool supports it (better scaling).
- Keep slide titles short. Bullet count: 3 – 5 lines, monospace for code.
- For diagram slides, the diagram is the slide; bullets go in speaker notes.
- For text slides, prefer a single quote-card layout.
- Footer on every slide: *HexaStock — DDD · Hexagonal · Spring Modulith ·
  Domain Events*.
