# Numbered-Seat Concert Booking: A Technical Architecture Report

## Coordinating Seat Allocation and Payment in a Microservices System with Relational Databases

**Audience:** Principal architects, senior backend engineers, technical leads  
**Technology context:** Java · Spring Boot · Relational databases (MySQL / PostgreSQL) · JPA/Hibernate considered but not mandated  
**Date:** March 2026

---

## Executive Summary

This report analyses how to design and implement a system for booking numbered concert seats when seat selection and payment are handled by separate microservices, each backed by its own relational database. The central engineering challenge is not the payment flow per se, but the safe coordination of two cross-service concerns — seat availability and monetary settlement — under concurrent contention for the same finite, non-fungible resources.

Numbered-seat booking is fundamentally harder than generic inventory decrement. Each seat is a unique, individually addressable resource. Two customers may request overlapping subsets of seats simultaneously. The system must prevent double-booking without sacrificing throughput on high-demand events, and it must remain consistent even when payment succeeds but confirmation is delayed, or when a temporary hold expires between seat selection and payment completion.

The report evaluates multiple coordination models (temporary holds, reservation intents, payment-authorisation-first, allocation-after-payment, and hybrid patterns), multiple concurrency-control strategies (optimistic locking, pessimistic locking, constraint-based collision detection, insert-first approaches, queue-based serialisation), and multiple aggregate-root choices under Domain-Driven Design (Booking, Seat, ConcertInventory, SeatHold). It provides concrete relational schemas, analyses JPA/Hibernate trade-offs versus SQL-centric approaches, and details transaction boundaries, saga-style coordination, failure semantics, and performance characteristics.

The final recommendation proposes a pragmatic default architecture: a `SeatHold` aggregate root backed by a dedicated `seat_hold` / `seat_hold_item` schema with a database-enforced unique constraint on (seat, active hold), insert-first collision detection for the hot path, selective pessimistic locking only where partitioned contention justifies it, and saga-style coordination with the payment microservice using idempotent confirmations and hold expiration as the safety net.

---

## Table of Contents

1. [Problem Statement and Domain Constraints](#1-problem-statement-and-domain-constraints)
2. [Domain Model Options](#2-domain-model-options)
3. [Aggregate Design Under DDD](#3-aggregate-design-under-ddd)
4. [Data Model in a Relational Database](#4-data-model-in-a-relational-database)
5. [Concurrency-Control Strategies](#5-concurrency-control-strategies)
6. [Optimistic vs Pessimistic Locking — Detailed Analysis](#6-optimistic-vs-pessimistic-locking--detailed-analysis)
7. [Why Seat Rows May or May Not Need to Be Read First](#7-why-seat-rows-may-or-may-not-need-to-be-read-first)
8. [Transaction Boundaries and Failure Semantics](#8-transaction-boundaries-and-failure-semantics)
9. [Microservice Interaction Patterns](#9-microservice-interaction-patterns)
10. [JPA/Hibernate Approach vs SQL-Centric Approach](#10-jpahibernate-approach-vs-sql-centric-approach)
11. [Performance and Scalability](#11-performance-and-scalability)
12. [Industry Realism](#12-industry-realism)
13. [Final Recommendation](#13-final-recommendation)
14. [Conclusion](#14-conclusion)

---

## 1. Problem Statement and Domain Constraints

### 1.1 The Business Problem

A concert-ticketing platform sells numbered seats. Every seat in a venue has a fixed identity — row A seat 3 is a distinct, non-fungible resource. A customer selects one or more specific seats and purchases them as a unit. The system must guarantee that no seat is ever sold to two different customers, that payment and seat allocation are coordinated so that neither money nor seats are lost, and that the system remains responsive under high concurrency when a popular concert goes on sale.

### 1.2 Why Numbered Seats Are Harder Than Generic Inventory

In a generic inventory system (e.g. "100 units of Widget-X in stock"), the resources are fungible. Decrementing a counter atomically — `UPDATE inventory SET count = count - 4 WHERE product_id = ? AND count >= 4` — is sufficient. Two customers wanting 4 widgets each never conflict on a specific widget; they conflict only on the aggregate count.

Numbered seats are fundamentally different:

- **Each seat is individually addressable.** Customer A wants seats [A1, A2, A3, A4]; customer B wants [A3, A4, A5, A6]. The requests overlap on A3 and A4 specifically.
- **Multi-seat atomicity is required.** If A1 is available but A3 is not, the entire 4-seat request may need to fail (or the customer must be informed of partial availability). A customer who asked for 4 adjacent seats rarely wants just 2.
- **The conflict surface is fine-grained and unpredictable.** Any subset of seats may overlap with any other. Unlike a counter, you cannot simply serialise on a single row.
- **Seat identity carries business meaning.** Row, section, view quality, price tier, accessibility status — all are attributes of the specific seat. There is no "any equivalent seat will do" simplification in the strict numbered-seat model.

This means that concurrency control must operate at the individual-seat level, yet multi-seat operations must succeed or fail atomically. This tension is at the heart of the architectural challenge.

### 1.3 Coordination Models Between Seat Allocation and Payment

There are several conceptually distinct ways to coordinate seat selection with payment. None is universally correct; each carries different trade-offs for user experience, consistency risk, and implementation complexity.

**Model A: Reserve-then-pay (temporary hold before payment).**  
The seat service places the requested seats into a temporary held state. The customer has a time window (e.g. 10 minutes) to complete payment. If payment succeeds, the hold is confirmed into a sale. If payment fails or the timer expires, the hold is released. This is the most common model in the ticketing industry.

**Model B: Reservation intent recorded separately (no seat-row mutation before payment).**  
Instead of changing seat status directly, a separate "reservation intent" or "booking request" record is created. Seats are not marked as held in the seat table itself. Conflict detection happens when the intent is evaluated — either immediately via constraint checks, or asynchronously. This model decouples intent capture from seat-state mutation.

**Model C: Payment-authorisation-first.**  
The payment service authorises (but does not capture) the customer's payment method before any seat allocation occurs. Once authorisation is confirmed, the seat service attempts to allocate the seats. If allocation fails, the authorisation is voided. This inverts the usual flow: money is secured first, seats second.

**Model D: Allocation-after-payment (optimistic, risky).**  
Payment is fully captured, and only then are seats allocated. If seats are no longer available, the payment must be refunded. This model is generally unsuitable for numbered seats because the conflict window is wide and refund-based recovery is a poor customer experience, but it is worth mentioning as an anti-pattern to explain why the other models exist.

**Model E: Hybrid — hold + payment authorisation in parallel.**  
A temporary hold is created simultaneously with a payment authorisation. Confirmation happens only when both succeed. Expiration or cancellation of either leg triggers cleanup of the other. This is a tighter coordination pattern, sometimes seen in premium ticketing.

The report will analyse Model A (reserve-then-pay with explicit holds) in the greatest depth because it is the dominant industry pattern for numbered seats, but Models B, C, and E will also be explored as legitimate alternatives.

### 1.4 Implications of Overlapping Seat Requests

When User A requests [A1, A2, A3, A4] and User B concurrently requests [A3, A4, A5, A6], the system must resolve the overlap. The design must answer:

- Should both requests be attempted fully, with the loser discovering failure at the overlap point?
- Should one request be serialised ahead of the other, blocking it until the first completes?
- Should the overlap be detected eagerly (by reading seat availability) or lazily (by attempting writes and catching constraint violations)?
- If one request partially succeeds (A1, A2 are free but A3 is not), should the system attempt a partial allocation, or fail the entire multi-seat batch atomically?

These questions drive the choice of concurrency-control strategy and transaction design.

### 1.5 The Seat Service as Source of Truth

Regardless of coordination model, the seat-booking microservice must remain the source of truth for seat availability. The payment service should never unilaterally decide that a seat is available or sold. Payment success is a necessary condition for confirming a sale, but it is not sufficient — the seat service must independently verify and record the allocation. This separation is fundamental to maintaining consistency in a microservices architecture where each service owns its domain.

---

## 2. Domain Model Options

### 2.1 Core Domain Concepts

Regardless of implementation strategy, the following concepts appear in virtually every numbered-seat booking system:

| Concept | Description |
|---|---|
| **Concert** | An event at a venue on a specific date. May have sections, price tiers, and capacity. |
| **Seat** | A uniquely identified position in a venue, associated with a concert. Has attributes: section, row, number, price tier, accessibility. |
| **SeatHold** (or ReservationHold) | A temporary claim on one or more seats for a specific customer, with an expiration time. |
| **SeatHoldItem** | The join between a hold and an individual seat. One hold contains one or more items. |
| **Booking** (or ConfirmedReservation) | A finalised, paid allocation of seats to a customer. Represents the end-state after successful payment. |
| **Payment** | A record of monetary settlement, owned by the payment service. The seat service may hold a reference (payment ID) but does not own the payment lifecycle. |

### 2.2 State Machines — Where They Apply

A seat in the system may transition through states. The most common state machine for a hold-based design is:

```
AVAILABLE ──▶ HELD ──▶ SOLD
                │
                ├──▶ EXPIRED (hold timed out)
                └──▶ CANCELLED (customer or system cancelled)
```

However, this state machine can be represented in different ways:

**Option 1: Explicit status column on the seat row.**  
The `seat` table has a `status` column (`AVAILABLE`, `HELD`, `SOLD`). Every state transition mutates the seat row. This is simple to query ("show me all available seats") but means the seat row is a write hotspot whenever holds are created, confirmed, or expired.

**Option 2: Derived status from active holds.**  
The `seat` table has no explicit status column. A seat is "held" if there exists an active, non-expired record in a `seat_hold_item` table pointing to it. A seat is "sold" if a confirmed booking references it. A seat is "available" if neither condition holds. This avoids mutating the seat row during the hold lifecycle, which can reduce contention, but makes availability queries more expensive (they require joins or NOT EXISTS subqueries).

**Option 3: No hold concept at all.**  
In a reservation-intent or payment-first model, there may be no explicit "held" state. The seat transitions directly from AVAILABLE to SOLD upon confirmed allocation, or there is only a short, synchronous window during which the seat status is in flight inside a single transaction.

Each option has legitimate uses. Option 2 is particularly interesting for insert-first concurrency models, which we will explore in depth.

### 2.3 Whether HOLD Should Be an Explicit Domain Concept

In Model A (reserve-then-pay), a hold is undeniably a first-class domain concept. It has a lifecycle (creation, expiration, confirmation, cancellation), it participates in business rules (maximum hold duration, maximum seats per customer), and it is the locus of concurrency control.

In Model B (reservation intent), the hold may be implicit — a "booking request" record exists, but it is not called a "hold" and may not carry the same lifecycle semantics.

In Model C (payment-first), a hold may not exist at all from the seat service's perspective. Seats are allocated in a short synchronous transaction after payment authorisation. The "hold" window is the duration of the allocation transaction itself.

The report will treat explicit holds as the primary design for detailed analysis, but the reader should understand that this is a design choice, not a requirement.

---

## 3. Aggregate Design Under DDD

Choosing the right aggregate root is one of the most consequential design decisions. In DDD, an aggregate defines a transactional consistency boundary: all invariants within the aggregate are guaranteed by a single transaction. Changes to the aggregate are atomic. Invariants that span multiple aggregates can only be enforced eventually, or by external mechanisms (such as database constraints that operate below the aggregate layer).

### 3.1 Candidate: Booking as Aggregate Root

A `Booking` aggregate represents a customer's intent to purchase specific seats. It might contain a list of seat references, a customer reference, a status, and a payment reference.

**Advantages:**
- Natural representation of customer intent.
- Groups all seats in a single operation into one transactional unit.
- Easy to model the lifecycle: REQUESTED → CONFIRMED → CANCELLED.

**Disadvantages:**
- A Booking does not own seat availability. Two Bookings for overlapping seats are separate aggregates. They cannot see each other's changes within their own transactional boundaries.
- The critical invariant — "the same seat must not be double-booked" — is a cross-aggregate invariant. It cannot be enforced by the Booking aggregate alone.
- To protect availability, you would need either a database constraint external to the aggregate, or a higher-level coordination mechanism.

**Assessment:** Booking is a useful concept for representing customer-facing state, but it is a poor choice as the aggregate root for protecting seat-availability invariants. It works well as a read model or as a secondary aggregate that references confirmed allocations.

### 3.2 Candidate: Seat as Aggregate Root

Each `Seat` is its own aggregate root. The invariant "this seat can only be allocated to one active hold/booking at a time" is perfectly localised. A single seat's state is changed in a single transaction.

**Advantages:**
- The per-seat invariant is trivially protected: each aggregate manages its own state.
- Fine-grained: no unnecessary contention between unrelated seats.
- Scales horizontally in theory.

**Disadvantages:**
- A multi-seat booking operation (e.g. 4 seats) spans 4 aggregates. In strict DDD, this means 4 separate transactions, which breaks atomicity. If seats A1, A2, A3 are successfully held but A4 fails, the first three must be compensated.
- Compensating transactions add significant complexity.
- The user expectation is that a multi-seat request either succeeds entirely or fails entirely, which fits poorly with per-seat aggregates unless you relax transactional boundaries (e.g. process all 4 seats in one database transaction, which is technically a cross-aggregate transaction).

**Assessment:** Seat as aggregate root is theoretically clean but operationally awkward for multi-seat operations. It can work if you accept that the database transaction will span multiple aggregates (a pragmatic compromise), or if you implement saga-style compensation for partial failures (complex and error-prone for a synchronous user-facing flow).

### 3.3 Candidate: ConcertInventory / SectionInventory as Aggregate Root

A single aggregate encompasses all seats for a concert (or a section of a concert). All availability checks and hold creations happen within this aggregate's transaction.

**Advantages:**
- All invariants are local to the aggregate. Multi-seat atomicity is trivial.
- No cross-aggregate coordination needed.

**Disadvantages:**
- The aggregate is extremely large. A 20,000-seat venue means loading 20,000 seat entities into memory for every operation.
- Every concurrent request for any seat in the same concert contends on the same aggregate. If 500 customers simultaneously try to book seats for a popular concert, they all serialise on the same aggregate root, destroying throughput.
- Even partitioning by section (e.g. 2,000 seats per section) only partially alleviates the problem. Hot sections remain bottlenecks.
- ORM-level optimistic locking on the aggregate root's version column will cause massive retry storms under contention.

**Assessment:** ConcertInventory as aggregate root is a textbook example of an aggregate that is too large. It centralises invariants at the cost of all practical scalability. It should be avoided for any system that expects non-trivial concurrency.

### 3.4 Candidate: SeatHold / ReservationHold as Aggregate Root

A `SeatHold` aggregate represents one customer's temporary claim on a specific set of seats. It contains `SeatHoldItem` entities (one per seat). The aggregate manages its own lifecycle: creation, expiration, confirmation, cancellation.

**Advantages:**
- Natural unit of work: one hold per customer request, containing all requested seats.
- The aggregate's internal invariants are simple: the hold has an expiration, a status, and a list of claimed seats.
- Multi-seat atomicity is local to the aggregate: creating the hold with 4 items is a single transaction.
- Different holds are separate aggregates, so concurrent requests for non-overlapping seats do not contend at the aggregate level.

**Disadvantages:**
- The global invariant — "the same seat must not belong to two active holds simultaneously" — is a cross-aggregate invariant. Two `SeatHold` aggregates for overlapping seats cannot see each other within their own boundaries.
- This invariant must be enforced externally, typically by the database via a unique constraint or equivalent mechanism.

**Assessment:** SeatHold is often the most pragmatic aggregate root for this domain. It cleanly models the unit of work, provides multi-seat atomicity within the aggregate, and delegates the global uniqueness invariant to the database — which is precisely where relational databases excel. The "impurity" of relying on the database for a cross-aggregate invariant is not a failure of the design; it is a principled use of the strongest tool available for that specific guarantee.

### 3.5 The Pragmatic Reality

In practice, many seat-booking systems use a design where:

- The **SeatHold** is the operational aggregate for the reserve-then-pay workflow.
- The **database** enforces the cross-aggregate invariant (no duplicate active holds per seat) via constraints.
- The **Booking** or **ConfirmedReservation** is a separate aggregate that comes into existence after payment confirmation. It represents the finalised sale.
- The **Seat** is a reference entity, queried for display and validation, but not the primary aggregate through which holds are managed.

This is not a pure DDD design in the Evans sense, where every invariant is protected by exactly one aggregate. It is a mature, engineering-informed compromise that acknowledges the relational database as a first-class participant in invariant enforcement. The report will use this SeatHold-centric design as the primary model for the remaining analysis, while noting alternatives where relevant.

---

## 4. Data Model in a Relational Database

### 4.1 Candidate Schema: Hold-Based Design

```sql
-- The physical seat in a venue for a specific concert
CREATE TABLE seat (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    concert_id  BIGINT       NOT NULL,
    section     VARCHAR(20)  NOT NULL,
    row_label   VARCHAR(10)  NOT NULL,
    seat_number INT          NOT NULL,
    price_tier  VARCHAR(20)  NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    UNIQUE KEY uq_seat_identity (concert_id, section, row_label, seat_number),
    INDEX idx_seat_concert (concert_id)
);

-- A temporary hold on one or more seats
CREATE TABLE seat_hold (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    concert_id    BIGINT       NOT NULL,
    customer_id   BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, CONFIRMED, EXPIRED, CANCELLED
    expires_at    DATETIME     NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at  DATETIME     NULL,
    payment_id    VARCHAR(100) NULL,
    version       INT          NOT NULL DEFAULT 0,
    INDEX idx_hold_expires (status, expires_at),
    INDEX idx_hold_customer (customer_id, concert_id)
);

-- Individual seats within a hold
CREATE TABLE seat_hold_item (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_hold_id  BIGINT NOT NULL,
    seat_id       BIGINT NOT NULL,
    CONSTRAINT fk_item_hold FOREIGN KEY (seat_hold_id) REFERENCES seat_hold(id),
    CONSTRAINT fk_item_seat FOREIGN KEY (seat_id)      REFERENCES seat(id),
    UNIQUE KEY uq_active_seat_hold (seat_id)  -- see discussion below
);

-- Confirmed bookings (post-payment)
CREATE TABLE booking (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_hold_id  BIGINT       NOT NULL,
    concert_id    BIGINT       NOT NULL,
    customer_id   BIGINT       NOT NULL,
    payment_id    VARCHAR(100) NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    booked_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_hold FOREIGN KEY (seat_hold_id) REFERENCES seat_hold(id),
    INDEX idx_booking_concert (concert_id),
    INDEX idx_booking_customer (customer_id)
);
```

### 4.2 The Critical Constraint: One Active Hold Per Seat

The most important constraint in this schema is ensuring that no seat appears in two active holds simultaneously. The `UNIQUE KEY uq_active_seat_hold (seat_id)` on `seat_hold_item` is a simplification that requires careful handling.

**The problem:** If we simply make `seat_id` unique in `seat_hold_item`, then once a hold is expired or cancelled, its items still physically exist in the table. A new hold for the same seat will violate the unique constraint even though the old hold is no longer active.

**Possible solutions:**

**Solution A: Delete hold items when the hold expires or is cancelled.**  
When a hold transitions to EXPIRED or CANCELLED, its `seat_hold_item` rows are deleted. The unique constraint on `seat_id` then works correctly: only items from active or confirmed holds remain. This is operationally simple but means that expired holds lose their item history unless archived elsewhere.

**Solution B: Composite unique constraint with status.**  
Change the constraint to be on `(seat_id, hold_status)` where `hold_status` is denormalised into the item table. But this is semantically wrong — you do not want uniqueness per status; you want uniqueness across all active statuses.

**Solution C: Partial unique index (PostgreSQL).**  
PostgreSQL supports partial indexes:

```sql
CREATE UNIQUE INDEX uq_active_seat_hold 
    ON seat_hold_item (seat_id) 
    WHERE hold_status = 'ACTIVE';
```

This is the cleanest relational solution: the uniqueness constraint applies only to rows where the hold is active. MySQL does not support partial indexes natively.

**Solution D: Filtered approach via an active-flag column.**  
Add an `is_active` boolean column to `seat_hold_item`. Create a unique index on `(seat_id, is_active)` where `is_active = TRUE`. On MySQL, you can approximate this with a trick: set `is_active = NULL` for inactive rows and use the fact that NULLs are not considered equal in unique indexes:

```sql
ALTER TABLE seat_hold_item ADD COLUMN active_seat_id BIGINT NULL;
-- active_seat_id = seat_id when active, NULL when expired/cancelled
CREATE UNIQUE INDEX uq_active_seat ON seat_hold_item (active_seat_id);
```

When a hold expires, set `active_seat_id = NULL`, which removes it from the unique constraint.

**Solution E: Separate active-holds table.**  
Maintain a `seat_active_hold` table that contains only currently active hold items. When a hold is created, rows are inserted into both `seat_hold_item` (permanent history) and `seat_active_hold` (live contention table). When a hold expires, confirmed, or is cancelled, rows are removed from `seat_active_hold`. The unique constraint lives only on `seat_active_hold`.

```sql
CREATE TABLE seat_active_hold (
    seat_id       BIGINT NOT NULL,
    seat_hold_id  BIGINT NOT NULL,
    PRIMARY KEY (seat_id),  -- implicitly unique on seat_id
    CONSTRAINT fk_active_hold FOREIGN KEY (seat_hold_id) REFERENCES seat_hold(id)
);
```

This is arguably the most robust cross-database approach. The PRIMARY KEY on `seat_id` guarantees that no two active holds can claim the same seat. Insert failures are cheap and informative. Deletion is straightforward.

**Recommendation:** For MySQL, Solution A (delete expired items) or Solution E (separate active-holds table) are the most practical. For PostgreSQL, Solution C (partial unique index) is elegant and performant. The report will proceed with Solution E as the reference design because it is explicit, engine-agnostic, and maps cleanly to the insert-first concurrency model.

### 4.3 Indexes and Query Patterns

Key queries the system must support efficiently:

| Query | Supporting Index |
|---|---|
| Show available seats for a concert | `idx_seat_concert` + LEFT JOIN to `seat_active_hold` |
| Check if specific seats are available | Point lookups on `seat_active_hold(seat_id)` |
| Find expired holds for cleanup | `idx_hold_expires(status, expires_at)` |
| Confirm a hold (by ID) | PK on `seat_hold(id)` |
| Customer's active holds | `idx_hold_customer(customer_id, concert_id)` |

### 4.4 Hold Expiration Modelling

If the hold-based design is chosen, every hold has a finite lifetime. The `seat_hold.expires_at` column records the deadline. But expired holds do not clean themselves up; the system must actively expire them.

Options for expiration enforcement:

- **Eager cleanup (scheduled job):** A background task runs every N seconds, finds holds where `status = 'ACTIVE' AND expires_at < NOW()`, transitions them to EXPIRED, and deletes their `seat_active_hold` rows. Simple, but introduces a lag: a seat may appear unavailable for up to N seconds after a hold expires.
- **Lazy cleanup (on-access):** When a new hold is attempted and the insert into `seat_active_hold` fails, the system checks whether the conflicting hold has expired. If it has, it cleans it up and retries. This is more responsive but adds complexity to the hot path.
- **Hybrid:** Scheduled cleanup for bulk expiration, plus lazy cleanup for individual contention resolution. This is the most common production approach.

---

## 5. Concurrency-Control Strategies

This section is the technical core of the report. Each strategy is analysed in depth.

### 5.1 Strategy 1: Read-Check-Then-Insert

**How it works:**  
Before creating a hold, the service reads the relevant seat rows (or the `seat_active_hold` table) to check whether the requested seats are already held. If all seats appear available, it inserts a new hold. If any seat appears held, it rejects the request.

**Where the consistency boundary is:**  
The read and the subsequent insert must happen within the same database transaction with an appropriate isolation level. At READ_COMMITTED (the default for most databases), a classic TOCTOU (time-of-check-to-time-of-use) race occurs: between the SELECT and the INSERT, another transaction may have inserted a conflicting hold. The read must use `SELECT ... FOR UPDATE` (pessimistic locking) or the insert must still rely on a unique constraint as a safety net.

**Performance characteristics:**  
The read adds a round trip. Under low contention this is negligible. Under high contention, the `SELECT ... FOR UPDATE` variant serialises requests for the same seats, which provides correctness but reduces throughput. Without the lock, the constraint-based safety net catches conflicts, making the preceding read somewhat redundant for conflict detection (though still useful for user-facing error messages).

**When to use:**  
When you want richer business validation before attempting the insert (e.g. checking seat existence, price tier, accessibility requirements, concert status). The read is valuable for these purposes even if it is not strictly necessary for collision detection.

### 5.2 Strategy 2: Insert-First, Database-Decides

**How it works:**  
The service does not read seat availability first. It constructs the hold and its items in memory and issues INSERTs directly into `seat_hold`, `seat_hold_item`, and `seat_active_hold`. If any INSERT into `seat_active_hold` violates the unique constraint (because another transaction has already claimed that seat), the database returns a constraint-violation error, and the transaction is rolled back.

**Where the consistency boundary is:**  
The unique constraint on `seat_active_hold(seat_id)` is the single point of truth. Conflict detection happens inside the database engine during the INSERT operation, which is atomic with respect to concurrent transactions.

**Whether seat rows must be read first:**  
No. This is the defining characteristic of this strategy. The database constraint provides the collision-detection guarantee. Seat rows need not be read for concurrency correctness. They may still be read for other business reasons (price calculation, seat existence verification, display purposes), but those reads are not part of the concurrency-control mechanism.

**What happens under overlap conflicts:**  
If User A and User B concurrently attempt holds on overlapping seats:
1. Both transactions begin.
2. Both attempt to INSERT into `seat_active_hold` for their respective seat lists.
3. The first transaction to write the row for the contested seat ID succeeds (the row is provisionally committed or locked by that transaction).
4. The second transaction's INSERT for the same seat ID blocks (if the first hasn't committed yet) or fails with a duplicate-key error (if the first has committed).
5. The second transaction receives a `DataIntegrityViolationException` (Spring) or equivalent, and is rolled back.

**Error handling model:**  
The calling code catches the constraint violation and translates it into a domain-level "seats not available" error. In Spring with JPA, this surfaces as a `PersistenceException` wrapping a `ConstraintViolationException` during flush or commit.

```java
@Transactional
public SeatHold createHold(CreateHoldCommand cmd) {
    SeatHold hold = SeatHold.create(cmd.customerId(), cmd.concertId(),
                                     cmd.seatIds(), holdDuration);
    try {
        seatHoldRepository.save(hold);  // inserts hold + items + active-hold rows
        return hold;
    } catch (DataIntegrityViolationException e) {
        throw new SeatsNotAvailableException(cmd.seatIds(), e);
    }
}
```

**Performance characteristics:**  
Excellent. No preliminary read. One write operation (batched inserts). The constraint check is a B-tree lookup, which the database engine performs at near-index speed. Under no contention, the happy path is a single round trip of inserts. Under contention, the losing transaction fails fast.

**Deadlock / contention risks:**  
If the 4 seat inserts for User A and the 4 for User B are interleaved in different orders, a deadlock can occur. Specifically, if A inserts seat 3 first and B inserts seat 4 first, then A tries seat 4 (blocked by B) while B tries seat 3 (blocked by A). This is a real risk.

Mitigation: **always insert `seat_active_hold` rows in a deterministic order** (e.g. sorted by `seat_id`). This ensures that two transactions covering the same set of seats will attempt inserts in the same order, which prevents circular-wait deadlocks.

```java
List<Long> sortedSeatIds = new ArrayList<>(cmd.seatIds());
Collections.sort(sortedSeatIds);
for (Long seatId : sortedSeatIds) {
    // insert into seat_active_hold in seat_id order
}
```

**Suitability:**  
This is the recommended strategy for the hot booking path when the schema is designed with a strong uniqueness constraint on active holds. It minimises I/O, latency, and lock duration. It works especially well with the `seat_active_hold` table design.

### 5.3 Strategy 3: Optimistic Locking with @Version

**How it works:**  
Entities carry a `@Version` column. When JPA flushes an update, the generated SQL includes `WHERE version = ?`. If the version has changed since the entity was loaded, the update affects zero rows, and Hibernate throws `OptimisticLockException`.

**Applicability to this domain:**  
Optimistic locking on the `Seat` entity would detect concurrent modifications to individual seats. However, if the design uses a separate `seat_active_hold` table and the primary concurrency mechanism is insert-based constraint detection, optimistic locking on seats is unnecessary for collision detection.

Optimistic locking on the `SeatHold` entity itself (via its `version` column) protects against concurrent modifications to the same hold — e.g. two competing "confirm" or "cancel" operations. This is valuable for the hold lifecycle but irrelevant for the initial collision-detection problem.

**When still useful:**  
- On `SeatHold.version`: protects the confirm/cancel/expire lifecycle against races. A scheduled expiration task and a concurrent confirmation request cannot both succeed because one will see a stale version.
- On `Seat` entities: relevant only if the design mutates seat rows directly (Option 1 in Section 2.2). If the design uses derived status (Option 2), seat rows are not mutated during hold operations, and `@Version` on seats adds no value.

**When unnecessary:**  
If the primary concurrency guarantee is the unique constraint on `seat_active_hold.seat_id`, optimistic locking is not needed for collision detection during hold creation. Adding it introduces ORM overhead without additional safety.

### 5.4 Strategy 4: Pessimistic Locking with SELECT FOR UPDATE

**How it works:**  
Before inserting a hold, the transaction acquires row-level locks on the contested resources. In JPA, this is expressed as:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
List<Seat> findSeatsForUpdate(@Param("seatIds") List<Long> seatIds);
```

Or directly:

```sql
SELECT * FROM seat WHERE id IN (101, 102, 103, 104) FOR UPDATE;
```

The locked rows are held until the transaction commits or rolls back. Other transactions attempting to lock the same rows will block.

**Where the consistency boundary is:**  
The database row locks themselves. While the lock is held, no other transaction can modify or lock those rows. The inserting transaction has exclusive access.

**Whether seat rows must be read first:**  
Yes, by definition. Pessimistic locking operates by reading and locking rows. The read is inherent to the mechanism.

**What happens under overlap conflicts:**  
The first transaction locks A3 and A4; the second transaction's `SELECT ... FOR UPDATE` for A3 blocks until the first commits. Then the second transaction sees the current state and can decide whether the seats are still available.

**Deadlock / contention risks:**  
Same as Strategy 2: if rows are locked in different orders, deadlocks occur. The mitigation is the same: **always lock seats in sorted order**.

```sql
SELECT * FROM seat WHERE id IN (101, 102, 103, 104) ORDER BY id FOR UPDATE;
```

**Performance characteristics:**  
Higher latency than insert-first: the lock acquisition is a blocking read. Under high contention, many transactions queue up waiting for locks on popular seats. Lock hold time includes the entire transaction (including any JPA flush, constraint checks, and network latency to the client).

**When to use:**  
When you need a read-before-write pattern for business validation and the same transaction will also perform the insert. When you want to provide the customer with a precise, immediate "this seat is unavailable" response rather than an insert failure that must be translated. When the contention level is moderate enough that lock-queue latency is acceptable.

**When to avoid:**  
For very high-demand events where hundreds of concurrent requests hit the same section simultaneously. The lock queue becomes the bottleneck. Insert-first with constraint detection fails faster and frees resources sooner.

### 5.5 Strategy 5: Constraint-Based Collision Detection in Hold Tables

This is a refinement of Strategy 2, made explicit. The core idea: the `seat_active_hold` table's unique constraint on `seat_id` is the collision detector. No other mechanism is needed for concurrency correctness.

**Critical insight:** If we have a dedicated hold-related table with a strong uniqueness constraint, the question "is this seat available?" is answered by the database's ability to accept or reject an INSERT. We do not need to read the seat, check a status column, or acquire an explicit lock. The INSERT itself is the check.

This is analogous to how a relational database's unique index on `(email)` prevents duplicate user registrations — you do not need to SELECT first; you just INSERT and catch the violation.

**Combined with the `seat_active_hold` table design, this is the primary recommended concurrency-control mechanism for hold creation.**

### 5.6 Strategy 6: Single-Writer / Queue / Partition-by-Concert

**How it works:**  
All booking requests for a given concert (or section) are routed to a single processing unit — either a dedicated thread, a message queue consumed by a single consumer, or an exclusive database advisory lock that serialises operations.

**Advantages:**
- All contention is eliminated. Requests are processed sequentially, so no locking or constraint-violation handling is needed.
- The logic is simple: check availability → allocate → respond.

**Disadvantages:**
- Throughput is limited to the processing speed of the single writer. For a high-demand event with thousands of concurrent requests, this may be unacceptable.
- The single writer is a single point of failure. If it crashes, processing stops.
- Queue depth during peak load introduces latency. Customers may wait seconds or more for their turn.

**When to use:**  
For VIP pre-sales or limited-seat events where the total request rate is manageable (e.g. fewer than 100 requests per second per concert). For systems where simplicity and correctness are valued over raw throughput at the peak moment.

**Partition-by-section variant:**  
Route requests by concert section rather than entire concert. Each section has its own single-writer consumer. This multiplies throughput by the number of sections while retaining per-section serialisation. Requests spanning multiple sections are more complex but rare.

### 5.7 Strategy 7: SQL-Centric Approaches Without Heavy ORM Reliance

**How it works:**  
Rather than relying on JPA entity lifecycle (persist → flush → constraint exception), the booking path uses direct SQL or a lightweight SQL DSL (jOOQ, Spring JDBC, or native queries).

Example using Spring JDBC:

```java
@Transactional
public SeatHold createHold(CreateHoldCommand cmd) {
    String holdId = idGenerator.next();
    
    jdbcTemplate.update(
        "INSERT INTO seat_hold (id, concert_id, customer_id, status, expires_at) VALUES (?, ?, ?, 'ACTIVE', ?)",
        holdId, cmd.concertId(), cmd.customerId(), cmd.expiresAt());

    List<Long> sortedSeatIds = new ArrayList<>(cmd.seatIds());
    Collections.sort(sortedSeatIds);
    
    for (Long seatId : sortedSeatIds) {
        jdbcTemplate.update(
            "INSERT INTO seat_hold_item (seat_hold_id, seat_id) VALUES (?, ?)",
            holdId, seatId);
        jdbcTemplate.update(
            "INSERT INTO seat_active_hold (seat_id, seat_hold_id) VALUES (?, ?)",
            seatId, holdId);
    }
    
    return loadHold(holdId);
}
```

**Advantages:**
- Full control over SQL execution order.
- No ORM flush-timing surprises. When the INSERT executes, it executes immediately, and the constraint check happens at that exact point.
- Easier to reason about deadlocks because the developer controls the precise order of row-level operations.
- No entity graph management overhead.

**Disadvantages:**
- More boilerplate.
- No automatic dirty checking or cascade.
- Domain model and persistence model may diverge; the developer must maintain the mapping manually.
- Less idiomatic in a Spring Boot / JPA-heavy project.

**When to use:**  
For the hot booking path where concurrency control, lock ordering, and exception semantics must be precise. Many high-throughput booking systems use a hybrid approach: JPA for CRUD-heavy, low-contention paths (customer management, event administration), and direct SQL for the critical booking/hold path.

### 5.8 Strategy 8: Distributed Locks External to the Database

**How it works:**  
A distributed lock (Redis, ZooKeeper, etcd) is acquired on a resource key (e.g. "concert:123:seat:A3") before the database transaction begins.

**Why this is usually not the primary mechanism:**  
The relational database already provides ACID transactions and unique constraints. Adding an external lock introduces:
- An additional failure mode (what if Redis is unreachable?).
- A clock-dependence issue (distributed lock TTLs depend on clock synchronisation).
- A coordination boundary that is harder to reason about than database transactions.
- The need to keep the distributed lock and the database state consistent, which is itself a distributed-consistency problem.

Distributed locks can be useful as a coarse pre-filter: if a lock cannot be acquired, reject the request before even starting a database transaction. This reduces database load under extreme contention. But the database constraint must remain the source of truth. The distributed lock is a performance optimisation, not a correctness mechanism.

### 5.9 Strategy 9: Payment-Authorisation-First Coordination

**How it works:**  
Before creating a seat hold, the system requests a payment authorisation from the payment service. If the customer's card can be charged, the authorisation ID is returned (no funds are captured yet). The seat service then creates the hold, associating it with the authorisation. Upon hold confirmation, the payment is captured. If the hold fails or expires, the authorisation is voided.

**Implications:**  
This inverts the default flow. Instead of "hold seats → pay → confirm", it becomes "authorise payment → hold seats → confirm → capture". This can reduce the incidence of held seats that never result in payment (because payment capability is verified upfront), but it introduces a window where the customer's payment method has an authorisation but no seats have been reserved yet.

**Trade-offs:**  
- Reduces wasted holds from customers who cannot pay.
- Introduces a more complex failure matrix: authorisation may succeed but hold may fail (void the auth); hold may succeed but capture may fail (cancel the hold and void the auth).
- Payment authorisations have their own expiration windows (typically 5-7 days), so this is rarely a bottleneck.

### 5.10 Strategy 10: Hybrid Approaches

In practice, production systems often combine several of the above strategies. A typical hybrid:

1. **Insert-first for hold creation** (Strategy 5): The hot path uses constraint-based collision detection. No seat reads for concurrency purposes.
2. **Pessimistic locking for hold confirmation** (Strategy 4): When converting a hold to a confirmed booking, the service acquires a `FOR UPDATE` lock on the `seat_hold` row to prevent concurrent confirm/cancel/expire races.
3. **Optimistic locking on `SeatHold.version`** (Strategy 3): As an additional safety net for lifecycle transitions.
4. **Scheduled + lazy cleanup** for expired holds: Background job for bulk cleanup; lazy check on contention.
5. **Queue-based serialisation** (Strategy 6): Only for specific ultra-high-demand events, enabled by configuration, not used globally.

### 5.11 Comparison Table

| Strategy | Reads Seats First? | Primary Consistency Mechanism | Conflict Detection Point | Deadlock Risk | Throughput Under Contention | Complexity |
|---|---|---|---|---|---|---|
| Read-check-then-insert | Yes | SELECT + constraint | INSERT (constraint as safety net) | Low (if ordered) | Moderate | Low |
| Insert-first | No | Unique constraint | INSERT | Moderate (mitigated by ordering) | High | Low |
| Optimistic @Version | Yes (implicit) | Version check at flush | UPDATE/flush | N/A (retry-based) | Low under high contention (retry storms) | Moderate |
| Pessimistic FOR UPDATE | Yes | Row locks | Lock acquisition | Moderate (mitigated by ordering) | Moderate (serialised on contention) | Moderate |
| Constraint on hold table | No | Unique constraint | INSERT | Same as insert-first | High | Low |
| Single-writer queue | N/A | Serial processing | Before execution | None | Low (single-threaded) | Low to moderate |
| SQL-centric | Varies | Constraint + explicit SQL | INSERT | Low (developer-controlled) | High | Moderate (more code) |
| Distributed lock | No (lock is external) | External lock + constraint | Lock acquisition | Low | Moderate | High |

---

## 6. Optimistic vs Pessimistic Locking — Detailed Analysis

### 6.1 Optimistic Locking in JPA/Hibernate

Optimistic locking in JPA is implemented via the `@Version` annotation on a numeric or timestamp column:

```java
@Entity
public class SeatHold {
    @Id
    private Long id;
    
    @Version
    private int version;
    
    // ... other fields
}
```

When Hibernate flushes a dirty entity, the generated UPDATE includes a version check:

```sql
UPDATE seat_hold 
SET status = 'CONFIRMED', version = 2, confirmed_at = '2026-03-14 12:00:00' 
WHERE id = 42 AND version = 1;
```

If the row's current version is no longer 1 (because another transaction modified it), the UPDATE affects zero rows. Hibernate detects this and throws `OptimisticLockException`, which typically causes the `@Transactional` method to roll back.

**When conflicts are detected:**  
Not at the point where the entity is modified in Java memory. The conflict is detected at flush time — which, in a `@Transactional` Spring service method, may be at the end of the method (just before commit) or earlier if `entityManager.flush()` is called explicitly, or if a query triggers auto-flush. This late detection means that significant work may be performed before the failure is discovered.

**Multi-seat implications:**  
If optimistic locking is applied to individual `Seat` entities, and a multi-seat operation modifies 4 seats, a conflict on any one seat at flush time rolls back the entire transaction. The user must retry. Under high contention, many retries may be needed — retry storms.

**When optimistic locking is unnecessary:**  
If the concurrency-control mechanism is insert-based (Strategy 5), optimistic locking on seat or hold entities provides no additional safety for hold creation. It remains useful for hold lifecycle transitions (confirm, cancel, expire) where the same hold row may be concurrently modified.

### 6.2 Pessimistic Locking in JPA/Hibernate

Pessimistic locking in JPA is expressed via lock modes on queries:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT h FROM SeatHold h WHERE h.id = :id")
Optional<SeatHold> findHoldForUpdate(@Param("id") Long id);
```

Or programmatically:

```java
entityManager.find(SeatHold.class, holdId, LockModeType.PESSIMISTIC_WRITE);
```

This translates to:

```sql
SELECT * FROM seat_hold WHERE id = 42 FOR UPDATE;
```

The row is locked for the duration of the enclosing transaction. Other transactions that attempt to lock the same row will block until the first transaction commits or rolls back.

**When locks block:**  
Immediately at the `SELECT ... FOR UPDATE` statement. This is "early" in the sense that the conflict is known as soon as the lock cannot be acquired. The blocking duration is the remainder of the first transaction's lifetime.

**Where exceptions occur:**  
If a lock wait timeout is configured (e.g. `innodb_lock_wait_timeout = 5` in MySQL), and the lock cannot be acquired within that time, a `LockTimeoutException` is thrown. In JPA, this surfaces as `PessimisticLockException` or `LockTimeoutException`.

**Multi-seat implications:**  
Locking 4 seat rows (or 4 `seat_active_hold` rows) requires acquiring all 4 locks. If another transaction holds a conflicting lock, blocking occurs. Deadlocks are possible if lock ordering is inconsistent. Mitigation: always acquire locks in a canonical order.

**When pessimistic locking is useful in this domain:**
- When confirming a hold: lock the `seat_hold` row to prevent concurrent confirm and expire from racing.
- When performing cleanup of expired holds: lock the hold to prevent a late-arriving confirmation from succeeding on an expired hold that is being cleaned up.
- When the insert-first strategy is not applicable (e.g. updating an existing hold rather than creating a new one).

**When pessimistic locking is unnecessary:**  
For initial hold creation via insert-first into `seat_active_hold`. The INSERT with unique constraint already provides the necessary collision detection without requiring any prior lock.

### 6.3 Rollback Semantics

In a Spring `@Transactional` method:

- **Unique constraint violation:** The INSERT fails. Spring's `PersistenceExceptionTranslator` wraps it as `DataIntegrityViolationException`. If the exception propagates uncaught, the transaction is rolled back. All changes in the transaction are undone.
- **Optimistic lock failure:** Hibernate throws `OptimisticLockException`. The transaction is rolled back. The service layer can catch this and retry.
- **Pessimistic lock timeout:** The database throws a lock-wait-timeout error. The transaction is rolled back.
- **Deadlock:** The database detects the deadlock and rolls back one of the victim transactions. The application receives a `DeadlockLoserDataAccessException` (Spring) and can retry.

In all cases, the atomicity guarantee of the local database transaction ensures that either all inserts/updates succeed, or none do. There is no partial state.

### 6.4 Are These Mechanisms Necessary When Collision Detection Is on Insert?

If the primary concurrency guarantee for hold creation is the unique constraint on `seat_active_hold.seat_id`, and the hold lifecycle (confirm/cancel/expire) is managed by a separate operation:

- **For hold creation:** Neither optimistic nor pessimistic locking is needed. The constraint is sufficient.
- **For hold lifecycle transitions:** Optimistic locking (via `@Version` on `SeatHold`) is a good default to detect concurrent modifications. Pessimistic locking is a stronger alternative when contention on the same hold is expected (rare in practice, since each hold is specific to one customer).
- **On Seat entities:** If the design uses derived status (no status column on `seat`), seat entities are never mutated during booking operations, and neither locking mechanism applies to them.

**Summary:** Optimistic and pessimistic locking are not the centre of the concurrency design when insert-based constraint detection is the primary mechanism. They serve a supporting role for lifecycle transitions.

---

## 7. Why Seat Rows May or May Not Need to Be Read First

This section addresses a critical design question that is often conflated: the difference between reading for concurrency correctness and reading for business validation.

### 7.1 Reading Is Not Necessary for Collision Detection

If the schema includes a `seat_active_hold` table with a unique constraint on `seat_id`, insert-first collision detection does not require reading seat rows at all. The INSERT into `seat_active_hold` will succeed or fail based solely on whether a conflicting row already exists. The database's B-tree index lookup during the INSERT is the concurrency check.

This is analogous to an `INSERT INTO users (email, name) VALUES ('alice@example.com', 'Alice')` with a unique constraint on `email`. You do not need to `SELECT * FROM users WHERE email = 'alice@example.com'` first to check for duplicates. The constraint handles it.

### 7.2 Reading Is Still Useful for Business Validation

While not needed for collision detection, reading seat rows before creating a hold is useful for:

| Purpose | Example | Necessary for Correctness? |
|---|---|---|
| **Seat existence** | Verify that seat ID 999 actually exists for this concert | Yes — prevents foreign-key violations or nonsensical holds |
| **Concert status** | Verify the concert is on sale, not cancelled or past | Yes — business rule |
| **Price calculation** | Load seat price tier for total amount computation | Yes — the hold or booking needs a price |
| **Seat-class validation** | Verify the customer is eligible for VIP seats | Yes — business rule |
| **Richer error messages** | "Seat A3 is already held by another customer" vs "constraint violation on seat_active_hold" | No, but improves UX |
| **Availability display** | "Show available seats in section A" | Display concern, not hold-creation concern |

### 7.3 Separating the Concerns

The cleanest design separates these two concerns:

1. **Business validation reads** happen first. Load seat data for existence, pricing, eligibility, and error enrichment. These reads do not require locks (they run at normal READ_COMMITTED isolation).
2. **Collision detection** happens during the INSERT into `seat_active_hold`. In the rare case where a seat was available during the read but became held between the read and the insert, the unique constraint catches the conflict and the transaction rolls back. The business validation read is not wasted — it was useful for pricing and error context — but it is not the collision-detection mechanism.

This separation is sometimes called "validate-then-insert with constraint as safety net." It combines the UX benefits of read-check-then-insert with the correctness benefits of insert-first:

```java
@Transactional
public SeatHold createHold(CreateHoldCommand cmd) {
    // 1. Business validation (read, no locks)
    List<Seat> seats = seatRepository.findByIdIn(cmd.seatIds());
    validateSeatsExist(seats, cmd.seatIds());
    validateConcertOnSale(seats.getFirst().getConcertId());
    validatePriceTier(seats, cmd.customerId());
    BigDecimal totalPrice = calculateTotal(seats);
    
    // 2. Create hold (insert-first, constraint detects collisions)
    SeatHold hold = SeatHold.create(cmd.customerId(), cmd.concertId(),
                                     seats, totalPrice, holdDuration);
    try {
        seatHoldRepository.save(hold);  // triggers INSERTs into seat_active_hold
        return hold;
    } catch (DataIntegrityViolationException e) {
        throw new SeatsNotAvailableException(cmd.seatIds(), e);
    }
}
```

### 7.4 When Reading First Is Strictly Unnecessary

If the system can tolerate a generic "seats not available" error without specifying which seat caused the conflict, and if pricing is pre-loaded or unnecessary (e.g. free events), and if seat existence is guaranteed by schema design (pre-populated seat rows with foreign keys), then the entire read step can be skipped. The hold is constructed from the command's seat IDs alone, inserted, and the constraint provides the only validation. This is maximally fast but minimally informative.

---

## 8. Transaction Boundaries and Failure Semantics

### 8.1 Local Transaction Inside the Seat Service

The hold-creation operation — inserting into `seat_hold`, `seat_hold_item`, and `seat_active_hold` — must be a single local database transaction. If any insert fails (constraint violation, database error), the entire operation is rolled back. No partial hold is created. This is the fundamental atomicity guarantee of a relational transaction.

In Spring Boot:

```java
@Transactional
public SeatHold createHold(CreateHoldCommand cmd) { ... }
```

The `@Transactional` annotation ensures that the method runs within a single database transaction. All JPA operations within the method share the same transaction. On normal return, the transaction commits. On unchecked exception, the transaction rolls back.

**Subtle JPA behaviour:** In JPA with Hibernate, changes to managed entities are held in the persistence context (first-level cache). INSERT statements may be batched and delayed until flush. Flush happens:
- Explicitly, when `entityManager.flush()` is called.
- Implicitly, before a JPQL/native query that targets the same tables (auto-flush).
- At commit time.

This means that a unique-constraint violation may surface not at the `repository.save()` call, but at the flush that happens just before the transaction commits. The exception then triggers a rollback. The practical implication: code that catches `DataIntegrityViolationException` must account for the fact that the exception may surface at a different point than where the save was called:

```java
try {
    seatHoldRepository.save(hold);
    entityManager.flush();  // force immediate write to detect constraint violations now
} catch (DataIntegrityViolationException e) {
    throw new SeatsNotAvailableException(cmd.seatIds(), e);
}
```

Calling `flush()` explicitly makes the timing deterministic.

### 8.2 Why the Seat Service and Payment Service Must Not Share a Distributed Transaction

In a microservices architecture, each service owns its own database. A distributed ACID transaction (2PC — two-phase commit) across the seat database and the payment database is technically possible but strongly discouraged:

- **Availability cost:** 2PC requires all participants to be available. If the payment service is partially degraded, the seat service cannot commit its transaction. This couples their availability.
- **Latency cost:** 2PC adds multiple coordination round trips. For a user-facing booking flow, this is unacceptable at scale.
- **Operational complexity:** Most cloud-managed relational databases do not support XA transactions natively or efficiently.
- **Vendor coupling:** XA requires a transaction manager (e.g. Atomikos, Narayana) that all participants agree on.

The industry consensus is to use **saga-style coordination** instead: each service performs its own local transaction, and cross-service consistency is maintained through compensating actions and idempotent event handling.

### 8.3 Saga-Style Coordination: The Hold-Based Flow

The standard saga for the hold-based design:

```
Customer → Seat Service: "Hold seats [A1, A2, A3, A4]"
Seat Service: local tx → creates hold → responds with hold_id and total_price
Customer → Payment Service: "Pay $400 for hold_id=42"
Payment Service: processes payment → succeeds → responds with payment_id
Customer → Seat Service: "Confirm hold_id=42 with payment_id=XYZ"
Seat Service: local tx → confirms hold, records payment_id → responds: "Booking confirmed"
```

### 8.4 Failure Matrix

| Failure Scenario | Outcome | Recovery Mechanism |
|---|---|---|
| **Hold creation fails** (seats unavailable) | No hold created, no payment attempted | Customer informed immediately; no cleanup needed |
| **Hold succeeds, payment fails** | Hold exists but is unconfirmed | Hold expires after timeout; customer can retry payment within the hold window |
| **Hold succeeds, payment succeeds, confirmation message is lost** | Hold exists, payment captured, but hold is not confirmed | Idempotent confirmation retry by customer or payment service; hold must not expire before retry window; alternatively, payment service publishes a "payment succeeded" event that the seat service consumes |
| **Hold expires before payment confirmation arrives** | Hold expired, but payment was captured | The seat service detects on confirmation that the hold has expired. It rejects the confirmation. The payment must be refunded. This is a known edge case that every hold-based system must handle. |
| **Duplicate payment confirmation arrives** | The hold is already confirmed from the first message | Idempotency: the confirmation endpoint checks `seat_hold.status`. If already CONFIRMED with the same `payment_id`, it returns success without modification. |
| **Customer cancels after hold, before payment** | Hold is cancelled | Seat service cancels the hold; `seat_active_hold` rows are deleted; seats become available |

### 8.5 Idempotency Requirements

The confirmation endpoint must be idempotent. Regardless of how many times `CONFIRM hold_id=42 with payment_id=XYZ` is received, the outcome is the same: the hold is confirmed and the payment ID is recorded. Subsequent calls are no-ops that return success.

Implementation:

```java
@Transactional
public Booking confirmHold(ConfirmHoldCommand cmd) {
    SeatHold hold = seatHoldRepository.findByIdForUpdate(cmd.holdId())
            .orElseThrow(HoldNotFoundException::new);
    
    if (hold.isConfirmed() && hold.getPaymentId().equals(cmd.paymentId())) {
        return bookingRepository.findByHoldId(hold.getId()).orElseThrow();
    }
    
    if (hold.isExpired()) {
        throw new HoldExpiredException(hold.getId());
    }
    
    hold.confirm(cmd.paymentId());
    seatHoldRepository.save(hold);
    
    // seat_active_hold rows remain (or are moved to a confirmed_seat table)
    Booking booking = Booking.from(hold);
    return bookingRepository.save(booking);
}
```

### 8.6 Outbox-Style Event Publishing

If the seat service needs to notify other systems (e.g. an email service, an analytics pipeline) after a hold is confirmed, the event must be published reliably. Publishing to a message broker inside the `@Transactional` method is unsafe: if the transaction commits but the broker publish fails, the event is lost. If the broker publish succeeds but the transaction rolls back, the event is spurious.

The **transactional outbox pattern** addresses this:

1. Within the same local transaction, insert an event record into an `outbox` table.
2. A separate process (polling or CDC-based) reads the outbox and publishes events to the message broker.
3. The event is guaranteed to be published if and only if the transaction committed.

```sql
CREATE TABLE outbox (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSON         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published   BOOLEAN      NOT NULL DEFAULT FALSE
);
```

---

## 9. Microservice Interaction Patterns

### 9.1 Orchestration vs Choreography

**Orchestration:** A central coordinator (e.g. the customer's session or a dedicated orchestrator service) drives the saga step by step. The coordinator calls the seat service to create a hold, then calls the payment service, then calls the seat service to confirm. This is simpler to reason about, easier to debug, and the default recommendation for a two-service saga.

**Choreography:** Each service reacts to events from the other. The seat service publishes "HoldCreated"; the payment service listens, processes payment, and publishes "PaymentSucceeded"; the seat service listens and confirms. This is more decoupled but harder to trace and debug, and introduces eventual-consistency windows that are more complex to manage.

**Recommendation for this scenario:** Orchestration. With only two services, the coordination logic is simple enough for a client-driven or API-gateway-driven orchestration approach. Choreography adds unnecessary indirection.

### 9.2 Command/Event Flow: Hold-Based Orchestration

```
┌─────────┐       ┌──────────────┐       ┌─────────────────┐
│ Customer │       │ Seat Service │       │ Payment Service  │
└────┬─────┘       └──────┬───────┘       └────────┬─────────┘
     │  POST /holds       │                        │
     │ {concertId, seats} │                        │
     │───────────────────▶│                        │
     │                    │ local tx:              │
     │                    │ insert hold +          │
     │                    │ seat_active_hold       │
     │  201 {holdId,      │                        │
     │   expiresAt, total}│                        │
     │◀───────────────────│                        │
     │                    │                        │
     │  POST /payments    │                        │
     │ {holdId, amount,   │                        │
     │  paymentMethod}    │                        │
     │────────────────────┼───────────────────────▶│
     │                    │                        │ process payment
     │  200 {paymentId}   │                        │
     │◀───────────────────┼────────────────────────│
     │                    │                        │
     │  POST /holds/{id}  │                        │
     │    /confirm        │                        │
     │ {paymentId}        │                        │
     │───────────────────▶│                        │
     │                    │ local tx:              │
     │                    │ confirm hold,          │
     │                    │ create booking         │
     │  200 {bookingId}   │                        │
     │◀───────────────────│                        │
```

### 9.3 Alternative Flow: Payment-Authorisation-First

```
Customer → Payment Service: "Authorise $400 for concert=7"
Payment Service → Customer: {authId=ABC, status=AUTHORISED}
Customer → Seat Service: "Hold seats [A1..A4], authId=ABC"
Seat Service: local tx → creates hold (with authId reference) → responds 201
Customer → Seat Service: "Confirm hold_id=42"
Seat Service → Payment Service: "Capture authId=ABC"
Payment Service → Seat Service: {paymentId=XYZ, status=CAPTURED}
Seat Service: local tx → confirms hold, creates booking
Seat Service → Customer: {bookingId=99}
```

In this alternative, the seat service calls the payment service to capture funds during confirmation. If capture fails, the hold is cancelled. If the hold was already expired, the authorisation is voided.

### 9.4 Why the Seat Service Owns Availability Truth

The payment service should never make seat-availability decisions. Even in the authorisation-first flow, the payment service only knows about money. The seat service alone decides:
- Whether specific seats exist.
- Whether they are available.
- Whether a hold can be created.
- Whether a hold can be confirmed.

This separation prevents a class of inconsistencies where payment success is mistaken for seat availability.

---

## 10. JPA/Hibernate Approach vs SQL-Centric Approach

### 10.1 A Richer JPA Domain Model

```java
@Entity
@Table(name = "seat_hold")
public class SeatHold {
    @Id @GeneratedValue
    private Long id;
    
    @Version
    private int version;
    
    private Long concertId;
    private Long customerId;
    
    @Enumerated(EnumType.STRING)
    private HoldStatus status;
    
    private Instant expiresAt;
    private Instant confirmedAt;
    private String paymentId;
    
    @OneToMany(mappedBy = "hold", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatHoldItem> items = new ArrayList<>();
    
    // domain methods: confirm(), cancel(), isExpired(), etc.
}

@Entity
@Table(name = "seat_hold_item")
public class SeatHoldItem {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_hold_id")
    private SeatHold hold;
    
    private Long seatId;
}

@Entity
@Table(name = "seat_active_hold")
public class SeatActiveHold {
    @Id
    private Long seatId;  // PK is the unique constraint
    
    private Long seatHoldId;
}
```

**Advantages:**
- Rich domain model with behaviour (DDD-friendly).
- Cascade persistence: saving a `SeatHold` automatically persists its items.
- Familiar to Spring/JPA developers.

**Disadvantages:**
- Flush timing is opaque. Cascade inserts may be batched unpredictably.
- The `SeatActiveHold` entity is awkward in a JPA model — it's a concurrency-control artifact, not a true domain object.
- Constraint violations surface as `PersistenceException` wrapping vendor-specific errors. Distinguishing "seats already held" from "database connectivity error" requires inspecting exception chains.
- The ORM may generate suboptimal SQL for the insert-first pattern (e.g. doing a SELECT before INSERT for `@GeneratedValue` strategies other than `IDENTITY`).

### 10.2 A SQL-Centric Approach

The hot path uses `JdbcTemplate` or jOOQ, with the domain model being plain Java objects (not JPA entities):

```java
@Repository
public class SeatHoldJdbcRepository {
    private final JdbcTemplate jdbc;

    @Transactional
    public SeatHold createHold(Long concertId, Long customerId,
                                List<Long> seatIds, Instant expiresAt) {
        String holdId = UUID.randomUUID().toString();
        
        jdbc.update("INSERT INTO seat_hold (id, concert_id, customer_id, status, expires_at) " +
                     "VALUES (?, ?, ?, 'ACTIVE', ?)",
                     holdId, concertId, customerId, Timestamp.from(expiresAt));
        
        List<Long> sorted = new ArrayList<>(seatIds);
        Collections.sort(sorted);
        
        for (Long seatId : sorted) {
            jdbc.update("INSERT INTO seat_hold_item (seat_hold_id, seat_id) VALUES (?, ?)",
                        holdId, seatId);
            jdbc.update("INSERT INTO seat_active_hold (seat_id, seat_hold_id) VALUES (?, ?)",
                        seatId, holdId);
        }
        
        return new SeatHold(holdId, concertId, customerId, "ACTIVE",
                            expiresAt, seatIds);
    }
}
```

**Advantages:**
- Total control over SQL execution order and timing.
- Immediate constraint-violation feedback (no flush delay).
- Clearer deadlock-prevention via explicit ordered inserts.
- No ORM overhead or entity-graph management.

**Disadvantages:**
- More verbose.
- Domain model may not benefit from JPA lifecycle callbacks, cascade, or dirty checking.
- Developer must manually maintain consistency between the Java domain objects and the database.

### 10.3 Pragmatic Hybrid

Use JPA for the 90% of the application that benefits from it (CRUD operations, queries, administrative endpoints). Use direct SQL for the critical hot path (hold creation) where precise control over INSERT ordering, flush timing, and constraint-violation handling is essential.

This is not unusual in production systems. The key is to ensure that both code paths share the same transactional context (`DataSource` and `TransactionManager`), which Spring handles seamlessly when both `JdbcTemplate` and `EntityManager` use the same `DataSource`.

---

## 11. Performance and Scalability

### 11.1 Hot Concerts and Peak Release Times

When tickets for a major concert go on sale, the system may receive thousands of concurrent booking requests within seconds. Characteristics of this traffic pattern:

- **Burst traffic:** A massive spike at the on-sale time, decaying over minutes.
- **High overlap:** Many requests for the same "best" seats (front row, centre section).
- **Short tolerance for latency:** Customers expect a response within 1-3 seconds.
- **High emotional stakes:** Slow or failed responses generate enormous customer frustration.

### 11.2 Contention Hotspots

The primary hotspot is the `seat_active_hold` table (or its equivalent). For a popular concert:
- Many transactions attempt INSERT into `seat_active_hold` for seats in the same section.
- Inserts for the same `seat_id` serialise at the database level due to the unique constraint.
- Winner takes the seat; losers fail fast (constraint violation). This is acceptable if the failure rate is manageable and the customer can quickly retry with different seats.

Secondary hotspots:
- The `seat_hold` table's auto-increment ID column: heavy insert load may cause contention on the ID generator. UUIDs or pre-allocated ID ranges can mitigate this.
- Index maintenance on `seat_active_hold` and `seat_hold_item`: every insert updates the B-tree. Under extreme load, this is CPU and I/O intensive.

### 11.3 Deadlock Avoidance

As discussed in Section 5.2, inserting `seat_active_hold` rows in sorted `seat_id` order prevents circular-wait deadlocks. This is critical. Without it, two overlapping requests can deadlock, requiring one to be rolled back and retried.

### 11.4 Hold Expiration Cleanup

Expired holds leave rows in `seat_active_hold` that block new holds for those seats. Cleanup latency directly affects re-availability. Options:

- **Aggressive scheduled cleanup** (every 5-10 seconds): fast re-availability, but the scheduled job itself may contend with the hot booking path if it acquires locks on the same rows.
- **Lazy cleanup on contention:** When an INSERT into `seat_active_hold` fails, check whether the conflicting hold has expired. If so, delete the expired rows and retry the insert within the same transaction. This is responsive but adds complexity to the critical path.
- **Hybrid with staggered cleanup:** The scheduled job cleans up holds for concerts with moderate traffic; lazy cleanup handles the hot concert.

### 11.5 Partitioning by Concert or Section

If each concert's data is in a separate partition (or separate database shard), contention is isolated. Two concerts never interfere with each other. Within a concert, sections can further partition contention.

For most systems, logical partitioning via indexed queries (e.g. `WHERE concert_id = ?`) on a single database is sufficient. Physical partitioning (separate databases or table partitions) is warranted only at very large scale.

### 11.6 When Single-Writer Makes Sense

For an ultra-premium concert with only 50 VIP seats and expected demand of 10,000 simultaneous requests, a single-writer queue consuming requests for that specific section can provide perfect fairness, zero deadlocks, and guaranteed correctness, at the cost of serialised throughput. The queue drains in seconds for 50 seats — acceptable latency. This is a niche but legitimate optimisation.

### 11.7 Indexing Recommendations

```sql
-- Fast availability check: which seats in this concert have no active hold?
-- The LEFT JOIN to seat_active_hold is the key query
CREATE INDEX idx_seat_concert_section ON seat (concert_id, section, row_label, seat_number);

-- PK on seat_active_hold.seat_id already serves as the unique constraint and lookup index

-- Cleanup: find expired holds
CREATE INDEX idx_hold_expire ON seat_hold (status, expires_at) WHERE status = 'ACTIVE';
-- (partial index for PostgreSQL; standard composite index for MySQL)

-- Booking history lookup
CREATE INDEX idx_booking_customer_concert ON booking (customer_id, concert_id);
```

---

## 12. Industry Realism

This section grounds the analysis in what actually happens in production ticketing systems.

### 12.1 No Perfect Pattern Exists

Every production ticketing system the author has studied or worked with uses some variant of temporary holds, but the implementation details vary enormously. Some use explicit hold tables; others toggle status columns on seat rows. Some use pessimistic locking for the hot path; others rely entirely on constraint-based inserts. Some use Redis for the hold layer and only persist to the relational database upon confirmation. None uses distributed ACID transactions between seat and payment services.

### 12.2 What Is Common in Production Systems

- **Strong local consistency** inside the seat-ownership service. The relational database — not the application — is trusted as the ultimate arbiter of seat availability.
- **No distributed transactions** with payment. The saga pattern (hold → pay → confirm) is universal.
- **Temporary holds with expiration** are the dominant pattern for numbered seats. The hold window varies from 2 minutes to 15 minutes depending on the platform.
- **Idempotent confirmation endpoints.** Every system must handle duplicate or delayed payment confirmations. The confirmation logic checks current hold status before acting.
- **Relational constraints doing safety work.** Application-level checks (reading availability before writing) are treated as optimisations for error reporting, not as the primary consistency mechanism. The database constraint is the safety net that must never be removed.
- **Selective pessimistic locking.** Some systems use `SELECT ... FOR UPDATE` on the hot path; others rely on constraint-based detection. The choice depends on the contention profile, which is measured, not assumed.
- **Measurement-based optimisation.** No responsible team chooses pessimistic vs optimistic vs insert-first based on theory alone. They instrument the system, measure contention rates and latency percentiles under realistic load, and adjust. A system with 0.1% conflict rate has different needs than one with 30% conflict rate.

### 12.3 What Is Less Common (Despite Appearing in Textbooks)

- Event sourcing for seat booking. The operational complexity and eventual-consistency characteristics of event-sourced systems are rarely justified for a domain where strong consistency on individual seat ownership is the primary requirement.
- CQRS with separate read and write databases. For the seat service, a single relational database with well-designed indexes provides both consistent writes and fast reads. Splitting read and write storage adds eventual-consistency concerns without proportional benefit, unless the read-side query patterns are radically different from the write-side access patterns.
- Distributed locks (Redis, ZooKeeper) as the primary consistency mechanism. These are supplementary, not primary. The relational constraint is always the ground truth.

---

## 13. Final Recommendation

This section provides a concrete, engineering-oriented recommendation for the specified scenario: numbered concert seats, a seat service and a payment service, relational databases, Java/Spring Boot, with JPA available but not mandated for the hottest path.

### 13.1 The Default Architecture

**Domain model:**
- `SeatHold` as the primary aggregate root for the booking flow. It contains `SeatHoldItem` entities (one per seat) and manages its lifecycle: ACTIVE → CONFIRMED / EXPIRED / CANCELLED.
- `Booking` as a secondary aggregate, created from a confirmed hold. Represents the finalised sale.
- `Seat` as a reference entity. Queried for display, validation, and pricing. Not mutated during the hold/booking lifecycle.

**Concurrency model:**
- **Insert-first into `seat_active_hold`** with a PRIMARY KEY on `seat_id` as the collision-detection mechanism. This is the single point of truth for seat availability during the hold lifecycle.
- **No pessimistic locking on the hold-creation path.** The constraint is sufficient.
- **Deterministic insert ordering** (sorted by `seat_id`) to prevent deadlocks.
- **Optimistic locking** (`@Version`) on `SeatHold` for lifecycle transitions (confirm, cancel, expire). This prevents concurrent confirm and expire from racing.

**Relational schema:**
- `seat`, `seat_hold`, `seat_hold_item`, `seat_active_hold`, `booking` as described in Section 4.
- `seat_active_hold` with PRIMARY KEY on `seat_id` is the constraint that enforces one-active-hold-per-seat.
- Expired/cancelled holds are cleaned from `seat_active_hold` by a combination of scheduled cleanup and lazy contention-triggered cleanup.

**Hold expiration:**
- 10-minute default hold duration, configurable per concert or event type.
- Scheduled cleanup every 10 seconds for bulk expiration.
- Lazy cleanup on single-seat contention (when an insert fails, check if the conflicting hold is expired and clean it up inline).

**Business validation before insert:**
- Read seat rows (without locks) for existence verification, pricing, concert-status check, and error enrichment.
- The read is not part of the concurrency-control mechanism. It is a business-validation step only.

**Persistence approach:**
- **SQL-centric (JDBC or jOOQ)** for the hold-creation path. This gives precise control over insert ordering, flush timing, and exception handling.
- **JPA** for the rest of the application: hold confirmation, booking creation, seat catalogue management, customer management, queries.
- Both share the same `DataSource` and `PlatformTransactionManager`.

### 13.2 Coordination with the Payment Service

- **Orchestration-based saga:** Customer (or API gateway) drives the flow: create hold → process payment → confirm hold.
- **No distributed transactions.** Each service performs local transactions only.
- **Hold expiration as safety net:** If payment takes longer than the hold window, the hold expires and the seats are released. The customer must start over.
- **Idempotent confirmation:** The confirm endpoint checks hold status and payment ID before acting. Duplicate calls are safe.
- **Payment-expired-hold edge case:** If a confirmation arrives for an expired hold, the seat service rejects it. The orchestrating layer must trigger a refund on the payment service.
- **Transactional outbox** for event publication (e.g. "BookingConfirmed") to downstream systems (email, analytics). Events are published exactly-once via a polling outbox reader.

### 13.3 When to Deviate from the Default

| Condition | Deviation |
|---|---|
| **Ultra-high-demand event** (>1,000 req/sec for same section) | Consider single-writer queue per section. The throughput ceiling of constraint-based inserts may be reached. |
| **Very simple system, low concurrency** | Skip `seat_active_hold` table; use a status column on `seat` rows with optimistic locking. Simpler schema, sufficient for moderate traffic. |
| **Payment-authorisation-first required by business** | Add an authorisation step before hold creation. The seat service receives an `authId` and includes it in the hold. Capture happens at confirmation. |
| **PostgreSQL is the database** | Use partial unique index instead of the separate `seat_active_hold` table. Simpler to maintain. |
| **Event sourcing is mandated** | The analysis in this report still applies conceptually, but the implementation shifts from CRUD to event-append plus projections. The unique-constraint mechanism changes to a projection-based check. |

### 13.4 Summary Table of Recommended Choices

| Design Decision | Recommendation | Alternative |
|---|---|---|
| Aggregate root for holds | `SeatHold` | Per-`Seat` aggregate with saga compensation |
| Collision detection | Insert-first + unique constraint on `seat_active_hold` | `SELECT ... FOR UPDATE` on seat rows |
| Locking for hold creation | None (constraint is sufficient) | Pessimistic if validation reads must be locked |
| Locking for hold lifecycle | Optimistic (`@Version` on `SeatHold`) | Pessimistic (`FOR UPDATE` on hold row) |
| Seat status model | Derived from active holds (no status column on `seat`) | Explicit status column on `seat` |
| Persistence for hot path | SQL-centric (JDBC/jOOQ) | JPA with explicit `flush()` |
| Cross-service coordination | Orchestrated saga | Choreographed events |
| Hold expiration | Scheduled + lazy cleanup | Scheduled only |
| Event publishing | Transactional outbox | Direct broker publish (weaker guarantee) |
| Deadlock prevention | Sorted insert order by `seat_id` | Database-level deadlock detection + retry |

---

## 14. Conclusion

Designing a numbered-seat booking system that coordinates with a payment microservice is a problem that sits at the intersection of concurrent data access, distributed-system coordination, and domain modelling. There is no single pattern that resolves all tensions. The fundamental insight is that the relational database — through its unique constraints, transactional guarantees, and row-level locking capabilities — is the strongest tool available for protecting seat-level invariants, and the system design should lean into this rather than trying to replicate those guarantees in application-layer abstractions alone.

The `SeatHold` aggregate with a constraint-backed `seat_active_hold` table provides a pragmatic, performant, and correct design for the majority of concert-ticketing scenarios. It achieves multi-seat atomicity within a single local transaction, delegates the global uniqueness invariant to the database, and coordinates with the payment service through a simple, idempotent saga.

The design is not dogmatic. It does not insist on DDD aggregate purity where a relational constraint is more effective. It does not insist on JPA where direct SQL gives better control on the hot path. It does not insist on pessimistic locking where insert-first collision detection is faster. And it acknowledges that measurement under realistic load is always the final arbiter of whether the chosen strategy is correct for a given system's contention profile.

---

*End of report.*
