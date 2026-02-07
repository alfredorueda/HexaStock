# Automated Market Monitoring & Watchlists

**Domain-Driven Design + Hexagonal Architecture + CQRS**

This document presents a comprehensive guide to implementing an automated market monitoring system within the HexaStock platform. The feature enables investors to create personalized watchlists with price thresholds, while a background "Market Sentinel" continuously monitors market conditions and generates buy signals when opportunities arise. This extension demonstrates how Command Query Responsibility Segregation (CQRS) naturally emerges when scaling read-heavy monitoring operations, while preserving the integrity of the domain model for write operations.

---

## Table of Contents

- [Overview](#overview)
  - [The Investor's Challenge](#the-investors-challenge)
  - [The Engineering Challenge: Scale](#the-engineering-challenge-scale)
  - [Why CQRS Emerges Naturally](#why-cqrs-emerges-naturally)
- [Student Assignment](#student-assignment)
  - [Domain Model Requirements](#domain-model-requirements)
  - [Use Cases — Write Side (Commands)](#use-cases--write-side-commands)
  - [Use Case — Read Side (Market Sentinel Query)](#use-case--read-side-market-sentinel-query)
  - [Notification Adapter](#notification-adapter)
  - [Important Clarifications](#important-clarifications)
- [Architecture & Design Guide](#architecture--design-guide)
  - [Why CQRS Is Necessary Here](#why-cqrs-is-necessary-here)
  - [Domain Model Design (Write Side)](#domain-model-design-write-side)
  - [Scalable Read-Side Algorithm (Critical Section)](#scalable-read-side-algorithm-critical-section)
    - [Step 1 — Find DISTINCT Tickers in Active Watchlists](#step-1--find-distinct-tickers-in-active-watchlists)
    - [Step 2 — Fetch Prices ONCE per Ticker](#step-2--fetch-prices-once-per-ticker)
    - [Step 3 — Query ONLY Triggered Entries](#step-3--query-only-triggered-entries)
    - [Step 4 — Notify](#step-4--notify)
  - [Duplicate Prevention](#duplicate-prevention)
  - [Testing Strategy](#testing-strategy)
  - [Sequence Diagrams](#sequence-diagrams)
- [How This Would Evolve in Production](#how-this-would-evolve-in-production)
  - [Event Publishing](#event-publishing)
  - [Alert State Tracking](#alert-state-tracking)
  - [Horizontal Scaling](#horizontal-scaling)
  - [Architectural Stability](#architectural-stability)

---

## Overview

### The Investor's Challenge

Imagine an investor tracking dozens of stocks, waiting for the right moment to buy. They have identified target prices for each stock—prices at which they believe the investment becomes attractive. Perhaps they want to buy Apple (AAPL) if it drops below $150, or Google (GOOGL) if it falls under $120.

The problem is clear: **investors cannot manually monitor market prices continuously**. Markets move fast, opportunities appear and disappear within minutes, and constantly checking prices is neither practical nor efficient.

This is where **watchlists** come in. A watchlist allows an investor to:

1. **Track potential buying opportunities** by listing stocks of interest
2. **Define price thresholds** that represent their target entry points
3. **Receive automated notifications** when market conditions match their criteria

But the investor doesn't want to run manual checks. They want the system to do it for them. They want a **Market Sentinel**—an automated guardian that continuously watches the market and alerts them when action is needed.

### The Engineering Challenge: Scale

For a single user with a handful of watchlists, implementing this feature is straightforward: load the watchlists, fetch the current prices, compare, and notify. Simple.

But what happens when the platform grows?

- **100 users** with 10 watchlists each = 1,000 watchlists
- **10,000 users** with 20 watchlists each = 200,000 watchlists
- **1 million users** = potentially millions of watchlist entries

The naive approach—loading all watchlist aggregates into memory, iterating through each entry, and fetching prices repeatedly—**does not scale**. Memory consumption explodes. Network calls to price providers multiply unnecessarily. The system becomes slow and expensive.

**The real engineering challenge is not evaluating a price condition. The real challenge is executing that evaluation efficiently at scale.**

### Why CQRS Emerges Naturally

This scalability challenge reveals a fundamental architectural insight: **write operations and read operations have fundamentally different characteristics**.

**Write operations** (creating watchlists, adding entries, updating thresholds):
- Modify state
- Must protect domain invariants
- Require loading the full aggregate
- Are relatively infrequent
- Affect one aggregate at a time

**Read operations** (Market Sentinel detection):
- Do not modify state
- Do not need aggregate invariants
- Can use optimized projections
- Are extremely frequent (running on a schedule)
- Must scan across all active watchlists efficiently

This asymmetry is the essence of **Command Query Responsibility Segregation (CQRS)**:

- **Commands** (writes) go through the rich domain model, loading aggregates and enforcing business rules
- **Queries** (reads) bypass the domain model entirely, using optimized database projections

By separating these concerns, we can:
- Preserve the integrity and expressiveness of our domain model for writes
- Achieve the scalability and performance required for reads
- Keep each path simple and focused on its specific requirements

The following sections guide you through implementing this pattern in HexaStock.

---

## Student Assignment

### Domain Model Requirements

Students must implement the following domain model for watchlists.

#### Watchlist (Aggregate Root)

| Field       | Type                          | Description                                      |
|-------------|-------------------------------|--------------------------------------------------|
| `id`        | `WatchlistId` (Value Object)  | Unique identifier for the watchlist              |
| `ownerName` | `String`                      | Name of the watchlist owner                      |
| `listName`  | `String`                      | Human-readable name for the watchlist            |
| `active`    | `boolean`                     | Whether the watchlist is active for monitoring   |
| `entries`   | `Map<Ticker, WatchlistEntry>` | Collection of entries indexed by ticker          |

#### WatchlistEntry (Value Object)

| Field            | Type     | Description                                           |
|------------------|----------|-------------------------------------------------------|
| `ticker`         | `Ticker` | Stock ticker symbol (existing Value Object)           |
| `thresholdPrice` | `Money`  | Target price threshold for buy signal (existing VO)   |

**Critical Design Point**: The threshold price is defined **per entry**, not globally on the watchlist. Each stock in a watchlist can have its own independent threshold. This is fundamental to how the Market Sentinel evaluates conditions.

### Use Cases — Write Side (Commands)

Students must implement the following use cases that modify watchlist state. These represent the **Command** side of CQRS.

#### 1. Create Watchlist

Creates a new watchlist for an owner.

**Input:**
- `ownerName`: String
- `listName`: String

**Output:**
- The created `Watchlist` with a generated ID, initially active with no entries

**Business Rules:**
- Watchlist name must not be blank
- Owner name must not be blank
- New watchlists are active by default

---

#### 2. Delete Watchlist

Removes a watchlist entirely.

**Input:**
- `watchlistId`: WatchlistId

**Output:**
- Confirmation of deletion

**Business Rules:**
- Watchlist must exist

---

#### 3. Add or Update WatchlistEntry

Adds a new entry or updates an existing one for a ticker.

**Input:**
- `watchlistId`: WatchlistId
- `ticker`: Ticker
- `thresholdPrice`: Money

**Output:**
- Updated `Watchlist`

**Business Rules:**
- Watchlist must exist
- Threshold price must be positive
- If ticker already exists, update the threshold; otherwise, add new entry

---

#### 4. Remove WatchlistEntry

Removes an entry from a watchlist.

**Input:**
- `watchlistId`: WatchlistId
- `ticker`: Ticker

**Output:**
- Updated `Watchlist`

**Business Rules:**
- Watchlist must exist
- Entry for ticker must exist

---

#### 5. Activate Watchlist

Enables a watchlist for Market Sentinel monitoring.

**Input:**
- `watchlistId`: WatchlistId

**Output:**
- Updated `Watchlist` with `active = true`

---

#### 6. Deactivate Watchlist

Disables a watchlist from Market Sentinel monitoring.

**Input:**
- `watchlistId`: WatchlistId

**Output:**
- Updated `Watchlist` with `active = false`

---

**Why Aggregates Are Loaded for Commands:**

When modifying state, we load the complete aggregate to protect invariants. The `Watchlist` aggregate ensures:
- Entry uniqueness by ticker (enforced via Map)
- Valid threshold prices
- Consistent state transitions

This is the **Write Model** in Domain-Driven Design: rich entities with behavior that enforce business rules.

### Use Case — Read Side (Market Sentinel Query)

The Market Sentinel is a background process that monitors market conditions and generates buy signals.

#### Behavior

- Runs on a configurable schedule using `@Scheduled`
- Detection interval is configurable via application properties
- Does **NOT** load aggregates
- Uses projection queries for efficiency
- Represents the **Read Model**

#### Algorithm (High-Level)

1. Query DISTINCT tickers from all **active** watchlists
2. Fetch current prices for those tickers (once per ticker)
3. For each ticker, query entries where `thresholdPrice >= currentPrice`
4. For each triggered entry, send a notification (buy signal)

#### Why Aggregates Are NOT Loaded

The Market Sentinel performs **read-only** operations. It does not modify any state. Loading aggregates would be:
- **Wasteful**: We don't need the full object graph
- **Slow**: Loading millions of aggregates is expensive
- **Unnecessary**: We're not enforcing invariants, just querying data

Instead, the Market Sentinel uses **projection queries** that return only the data needed for evaluation. This is the essence of the Read Model in CQRS.

### Notification Adapter

For this assignment, the notification adapter logs buy signals to the console.

```java
public void notifyBuySignal(BuySignal signal) {
    log.info("BUY SIGNAL: {} should consider buying {} (threshold: {}, current: {})",
        signal.ownerName(),
        signal.ticker(),
        signal.thresholdPrice(),
        signal.currentPrice()
    );
}
```

**Production Context:**

The console logging simulates a real notification mechanism. In a real system, this adapter could send emails, push notifications, publish Kafka events, or trigger webhooks. The hexagonal architecture ensures this infrastructure concern is isolated in an adapter, allowing the notification mechanism to evolve independently of the domain logic.

### Important Clarifications

#### 1. NO User Entity

There is intentionally **no JPA User entity** in this assignment. Authentication, authorization, and JWT handling are out of scope. The `Watchlist` aggregate simply contains:

```java
private final String ownerName;
```

This is a deliberate simplification to focus on the core architectural concepts (DDD, Hexagonal, CQRS) without the complexity of security infrastructure.

#### 2. No JPA Many-to-Many with Ticker

`Ticker` is a **Value Object**, not a JPA entity. Do NOT model a JPA many-to-many relationship between `Watchlist` and `Ticker`.

Instead, use the following persistence structure:

```
WatchlistJpaEntity (1) ──────> WatchlistEntryJpaEntity (many)
```

The `WatchlistEntryJpaEntity` should have:

| Column           | Type                          |
|------------------|-------------------------------|
| `id`             | Long (auto-generated)         |
| `watchlist_id`   | Long (FK to watchlist)        |
| `ticker`         | String                        |
| `threshold_price`| Persistence representation of Money |

The domain `Ticker` and `Money` value objects are mapped to/from these persistence primitives by the adapter.

#### 3. Threshold is PER ENTRY

This point is critical and worth emphasizing:

- The threshold price is stored **per WatchlistEntry**
- It is NOT a global property of the Watchlist
- The Market Sentinel evaluates conditions **row-by-row** at the database level
- Each entry is independently evaluated against the current market price

This design allows users to track multiple stocks with different target prices in a single watchlist.

---

## Architecture & Design Guide

This section provides detailed architectural guidance for implementing the watchlist feature following DDD, Hexagonal Architecture, and CQRS principles.

### Why CQRS Is Necessary Here

Consider the naive approach to implementing Market Sentinel:

```java
// NAIVE APPROACH - DO NOT USE
List<Watchlist> allWatchlists = watchlistRepository.findAllActive();
for (Watchlist watchlist : allWatchlists) {
    for (WatchlistEntry entry : watchlist.getEntries()) {
        StockPrice price = priceProvider.fetchStockPrice(entry.ticker());
        if (entry.thresholdPrice().compareTo(price) >= 0) {
            notify(watchlist.ownerName(), entry);
        }
    }
}
```

**Problems with this approach:**

1. **Memory explosion**: Loading millions of `Watchlist` aggregates into memory
2. **Redundant price fetches**: The same ticker might appear in thousands of watchlists, causing redundant API calls
3. **Slow iteration**: Nested loops over in-memory collections
4. **No database optimization**: The database cannot help filter results

**The CQRS Solution:**

| Aspect               | Command Use Cases              | Market Sentinel (Query)        |
|----------------------|--------------------------------|--------------------------------|
| Loads aggregate      | ✅ Yes                         | ❌ No                          |
| Modifies state       | ✅ Yes                         | ❌ No                          |
| Enforces invariants  | ✅ Yes                         | ❌ No                          |
| Uses projections     | ❌ No                          | ✅ Yes                         |
| Scales to millions   | Limited (single aggregate)     | ✅ Yes (database-optimized)    |

By recognizing that read and write operations have different requirements, we can optimize each path independently.

### Domain Model Design (Write Side)

The domain model for the write side should be rich and expressive.

#### WatchlistEntry Value Object

```java
public record WatchlistEntry(
    Ticker ticker,
    Money thresholdPrice
) {
    public WatchlistEntry {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(thresholdPrice, "Threshold price must not be null");
        if (thresholdPrice.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Threshold price must be positive");
        }
    }
}
```

#### Watchlist Aggregate

```java
public class Watchlist {
    private final WatchlistId id;
    private final String ownerName;
    private String listName;
    private boolean active;
    private final Map<Ticker, WatchlistEntry> entries = new HashMap<>();
    
    // Constructor, factory methods...
    
    public void addOrUpdateEntry(Ticker ticker, Money thresholdPrice) {
        WatchlistEntry entry = new WatchlistEntry(ticker, thresholdPrice);
        entries.put(ticker, entry);
    }
    
    public void removeEntry(Ticker ticker) {
        if (!entries.containsKey(ticker)) {
            throw new EntryNotFoundException(ticker);
        }
        entries.remove(ticker);
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public Collection<WatchlistEntry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }
}
```

**Design Notes:**

- **Map<Ticker, WatchlistEntry>**: Ensures uniqueness by ticker. Using a Map is clearer and more efficient than using `Map<Ticker, Money>` directly, as it allows the `WatchlistEntry` to potentially carry additional metadata in the future.
- **Internal mutability**: The Map is mutable internally for aggregate operations
- **Defensive copy externally**: `getEntries()` returns an unmodifiable view
- **Rich behavior**: The aggregate contains methods that enforce business rules

### Scalable Read-Side Algorithm (Critical Section)

This section describes the algorithm the Market Sentinel must use to achieve scalability. This is **the most important technical section** of this document.

#### Step 1 — Find DISTINCT Tickers in Active Watchlists

First, query the database for all unique tickers that appear in active watchlists:

```sql
SELECT DISTINCT e.ticker
FROM watchlist_entry e
JOIN watchlist w ON e.watchlist_id = w.id
WHERE w.active = true
```

**Key point**: No aggregates are loaded. This is a simple projection query that returns a set of ticker strings.

In the port interface:

```java
public interface WatchlistQueryPort {
    Set<Ticker> findDistinctTickersInActiveWatchlists();
    // ...
}
```

#### Step 2 — Fetch Prices ONCE per Ticker

Using the existing `StockPriceProviderPort`, fetch prices for all tickers in a single batch operation:

```java
Set<Ticker> tickers = watchlistQueryPort.findDistinctTickersInActiveWatchlists();

Map<Ticker, StockPrice> prices = stockPriceProviderPort.fetchStockPrice(tickers);
```

**Critical**: This project already contains:

```
cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort
```

The documentation MUST use this exact port. The interface provides:

```java
StockPrice fetchStockPrice(Ticker ticker);

default Map<Ticker, StockPrice> fetchStockPrice(Set<Ticker> tickers);
```

The default batch method demonstrates a clean extension point that allows adapters to optimize price retrieval without changing the application core.

**Why this matters:**
- An adapter might override the batch method to call a real batch API
- Even if individual caching exists, the algorithm must fetch once per ticker per detection cycle
- This prevents N+1 query problems at the price provider level

#### Step 3 — Query ONLY Triggered Entries

For each ticker, query the database for entries where the threshold condition is satisfied:

```sql
SELECT w.owner_name, w.list_name, e.ticker, e.threshold_price
FROM watchlist_entry e
JOIN watchlist w ON e.watchlist_id = w.id
WHERE w.active = true
  AND e.ticker = :ticker
  AND e.threshold_price >= :currentPrice
```

**Key points:**
- The threshold is evaluated **per row** by the database
- Only triggered entries are returned, not all entries
- The database does the filtering, not Java code

In the port interface:

```java
public interface WatchlistQueryPort {
    Set<Ticker> findDistinctTickersInActiveWatchlists();
    
    List<TriggeredEntryView> findTriggeredEntries(Ticker ticker, Money currentPrice);
}
```

Where `TriggeredEntryView` is a simple projection DTO:

```java
public record TriggeredEntryView(
    String ownerName,
    String listName,
    Ticker ticker,
    Money thresholdPrice
) {}
```

#### Step 4 — Notify

Putting it all together, the Market Sentinel use case:

```java
@Service
public class MarketSentinelService {
    
    private final WatchlistQueryPort queryPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final NotificationPort notificationPort;
    
    public void detectBuySignals() {
        Set<Ticker> tickers = queryPort.findDistinctTickersInActiveWatchlists();
        
        Map<Ticker, StockPrice> prices = stockPriceProviderPort.fetchStockPrice(tickers);
        
        for (Ticker ticker : tickers) {
            StockPrice stockPrice = prices.get(ticker);
            Money currentPrice = Money.of(
                Currency.getInstance(stockPrice.currency()),
                BigDecimal.valueOf(stockPrice.price())
            );
            
            queryPort.findTriggeredEntries(ticker, currentPrice)
                .forEach(view -> 
                    notificationPort.notifyBuySignal(
                        new BuySignal(
                            view.ownerName(),
                            view.listName(),
                            ticker,
                            view.thresholdPrice(),
                            stockPrice
                        )
                    )
                );
        }
    }
}
```

**Design Notes:**

- `StockPrice` may contain timestamp, currency, and other metadata
- Threshold comparison uses `Money` for type-safe monetary comparison
- The richer `StockPrice` object allows better logging and future evolution
- The notification includes both the threshold and current price for context

### Duplicate Prevention

In a real system, duplicate prevention could be implemented in the notification adapter or via a separate alert state store. This is out of scope for this assignment.

However, consider these production approaches:
- **Notification adapter with state**: Track which signals have been sent
- **Alert entity**: Create an `Alert` aggregate that records sent notifications
- **Time-based deduplication**: Don't re-notify for the same condition within X hours

### Testing Strategy

A comprehensive testing strategy should cover:

#### 1. Unit Tests for Aggregate Invariants

```java
@Test
void shouldRejectNegativeThresholdPrice() {
    Watchlist watchlist = createWatchlist();
    Money negativePrice = Money.of(Currency.getInstance("USD"), new BigDecimal("-10.00"));
    
    assertThrows(IllegalArgumentException.class, () ->
        watchlist.addOrUpdateEntry(Ticker.of("AAPL"), negativePrice)
    );
}

@Test
void shouldUpdateExistingEntryForSameTicker() {
    Watchlist watchlist = createWatchlist();
    Ticker aapl = Ticker.of("AAPL");
    
    watchlist.addOrUpdateEntry(aapl, Money.of(usd, new BigDecimal("150.00")));
    watchlist.addOrUpdateEntry(aapl, Money.of(usd, new BigDecimal("140.00")));
    
    assertEquals(1, watchlist.getEntries().size());
    assertEquals(new BigDecimal("140.00"), 
        watchlist.getEntries().iterator().next().thresholdPrice().amount());
}
```

#### 2. Integration Tests for Persistence

Test that the JPA adapter correctly persists and retrieves watchlists and entries.

#### 3. Query Port Tests

Test that projection queries return correct results:

```java
@Test
void shouldFindDistinctTickersFromActiveWatchlistsOnly() {
    // Given: active watchlist with AAPL, inactive watchlist with GOOGL
    
    Set<Ticker> tickers = queryPort.findDistinctTickersInActiveWatchlists();
    
    assertThat(tickers).containsExactly(Ticker.of("AAPL"));
}

@Test
void shouldFindTriggeredEntriesWhenPriceBelowThreshold() {
    // Given: entry with threshold $150, current price $140
    
    List<TriggeredEntryView> triggered = 
        queryPort.findTriggeredEntries(Ticker.of("AAPL"), Money.of(usd, "140.00"));
    
    assertThat(triggered).hasSize(1);
}
```

#### 4. Market Sentinel Service Tests

Test the service directly with mocked ports:

```java
@Test
void shouldNotifyWhenPriceBelowThreshold() {
    // Given
    when(queryPort.findDistinctTickersInActiveWatchlists())
        .thenReturn(Set.of(Ticker.of("AAPL")));
    when(stockPriceProviderPort.fetchStockPrice(anySet()))
        .thenReturn(Map.of(Ticker.of("AAPL"), 
            new StockPrice(Ticker.of("AAPL"), 140.0, Instant.now(), "USD")));
    when(queryPort.findTriggeredEntries(any(), any()))
        .thenReturn(List.of(new TriggeredEntryView("john", "Tech Stocks", 
            Ticker.of("AAPL"), Money.of(usd, "150.00"))));
    
    // When
    marketSentinelService.detectBuySignals();
    
    // Then
    verify(notificationPort).notifyBuySignal(any(BuySignal.class));
}
```

#### 5. Scheduler Stays Thin

The `@Scheduled` component should be a thin wrapper:

```java
@Component
public class MarketSentinelScheduler {
    
    private final MarketSentinelService marketSentinelService;
    
    @Scheduled(fixedRateString = "${market.sentinel.interval:60000}")
    public void runDetection() {
        marketSentinelService.detectBuySignals();
    }
}
```

Test the service, not the scheduler. The scheduler's only responsibility is timing.

### Sequence Diagrams

The following PlantUML diagrams illustrate the key flows:

1. **Command Flow (Write Side)**: [watchlist-command-flow.puml](./diagrams/watchlist-command-flow.puml)
2. **Market Sentinel Detection (Read Side)**: [market-sentinel-detection-flow.puml](./diagrams/market-sentinel-detection-flow.puml)
3. **CQRS Overview**: [cqrs-read-vs-write-overview.puml](./diagrams/cqrs-read-vs-write-overview.puml)

---

## How This Would Evolve in Production

The architecture presented in this assignment is intentionally simplified for pedagogical purposes. Here's how it would evolve in a production environment.

### Event Publishing

Instead of directly calling a notification adapter, a production system would publish domain events:

```java
public record BuySignalDetectedEvent(
    String ownerName,
    String listName,
    Ticker ticker,
    Money thresholdPrice,
    StockPrice currentPrice,
    Instant detectedAt
) {}
```

These events could be:
- Published to Apache Kafka for async processing
- Stored in an event store for audit and replay
- Consumed by multiple downstream services (email, push, SMS, analytics)

### Alert State Tracking

A production system would track alert state:

```java
public class Alert {
    private AlertId id;
    private WatchlistId watchlistId;
    private Ticker ticker;
    private AlertStatus status; // TRIGGERED, ACKNOWLEDGED, DISMISSED
    private Instant triggeredAt;
    private Instant acknowledgedAt;
}
```

This enables:
- Preventing duplicate notifications
- Showing alert history to users
- Tracking user engagement with signals

### Horizontal Scaling

For massive scale, consider:

#### Partitioning by Ticker

Distribute detection work across multiple instances:
- Instance 1 handles tickers A-M
- Instance 2 handles tickers N-Z
- Consistent hashing for dynamic partitioning

#### Distributed Scheduling

Use a distributed scheduler like:
- Quartz with JDBC job store
- Spring Cloud Kubernetes with leader election
- AWS EventBridge with Lambda

#### Idempotency

Ensure notifications are idempotent:
- Include a unique signal ID
- Notification adapter checks for duplicates
- Use exactly-once semantics where possible

#### Observability

Add comprehensive monitoring:
- Metrics: detection latency, signals per cycle, price fetch times
- Tracing: distributed traces across services
- Logging: structured logs for debugging

### Architectural Stability

Despite all these production enhancements, note what remains unchanged:

- **Domain model**: `Watchlist`, `WatchlistEntry` stay the same
- **Command use cases**: Still load aggregates, enforce invariants
- **Query use cases**: Still use projections, bypass aggregates
- **Port interfaces**: Same contracts, different implementations

This is the power of Hexagonal Architecture combined with DDD and CQRS:

> **The core architecture remains stable. Infrastructure evolves.**

Business logic is protected. Technical concerns are isolated in adapters. New requirements can be met by adding or replacing adapters, not by rewriting the core.

---

*This document is part of the HexaStock pedagogical project, demonstrating Domain-Driven Design, Hexagonal Architecture, and CQRS principles in a realistic financial domain context.*
