# Design Decision: Handling Lots with Zero Remaining Quantity in a Portfolio Aggregate

## 1. Purpose
This document formalizes the design decision regarding whether **Lots** with a remaining quantity of zero should be retained or removed from the **Holding** entity within a **Portfolio** aggregate.  
The decision is based on **Domain-Driven Design (DDD)** principles as introduced by **Eric Evans** (*Domain-Driven Design: Tackling Complexity in the Heart of Software*, 2003) and expanded by **Vaughn Vernon** (*Implementing Domain-Driven Design*, 2013) and **Jimmy Nilsson** (*Applying Domain-Driven Design and Patterns*, 2006).

---

## 2. Context
In the current model:
- **Portfolio** is the aggregate root.
- **Holding** represents a position in a specific asset within the Portfolio.
- **Lot** represents a batch of shares acquired at a specific time and price.
- The `remaining` attribute in Lot indicates how many units are still held after partial or full sales.

When a Lot reaches `remaining = 0`, there are two options:
1. **Retain the Lot** within the Holding.
2. **Remove the Lot** from the Holding.

---

## 3. Option Analysis

### 3.1 Retaining Lots with Zero Remaining Quantity
**Advantages:**
- Preserves historical context directly within the aggregate.
- Useful for regulatory, tax, or audit scenarios where the origin of a sale must be traceable from the current domain object.
- Simplifies reconstruction of historical positions without accessing other aggregates.

**Disadvantages:**
- The aggregate grows indefinitely, violating the DDD guideline that aggregates should be small enough to load and modify atomically (Evans).
- Larger collections increase memory footprint, dirty checking costs, and persistence overhead.
- Filtering out inactive Lots becomes necessary in most operations, adding complexity.

---

### 3.2 Removing Lots with Zero Remaining Quantity
**Advantages:**
- Keeps the aggregate small and efficient, in line with Vernon’s principle that aggregates should only contain state required to enforce invariants.
- Reduces the size of collections, improving performance when loading and updating the aggregate.
- Simplifies business logic for current state operations (only active Lots are processed).

**Disadvantages:**
- Historical details of Lots are no longer available directly from the Portfolio aggregate.
- Requires a separate mechanism (e.g., `Transaction` aggregate) to reconstruct past events.

---

## 4. Recommended Approach

Following DDD tactical modeling guidance from Evans and Vernon:

> “An aggregate is a cluster of associated objects that we treat as a unit for the purpose of data changes.  
> It should be small enough to be loaded and updated in one transaction.”  
> — *Eric Evans, Domain-Driven Design* (2003, p. 125)

**Decision:**
- Remove Lots from the Holding when their `remaining` quantity reaches zero.
- Maintain full historical information in a **separate aggregate** (`Transaction`), which serves as the append-only log of all operations affecting the Portfolio.
- The Portfolio aggregate will then represent **only the current active state** of investments.

This approach:
- Preserves **invariant enforcement** in the Portfolio aggregate.
- Keeps aggregates **bounded** and **transactionally efficient**.
- Delegates historical reconstruction to the `Transaction` aggregate, which can be queried independently.

---

## 5. Performance Considerations
- **Bounded collections** reduce the risk of performance degradation when aggregates are loaded into memory.
- Historical queries can be optimized via separate read models or projections without impacting transactional consistency boundaries.
- This separation aligns with Vernon’s recommendation to avoid coupling long histories to aggregates that need fast, atomic updates.

---

## 6. Alignment with DDD Authors

- **Eric Evans**: Emphasizes keeping aggregates small and focused on enforcing invariants, avoiding unbounded collections.
- **Vaughn Vernon**: Recommends separating long-lived or unbounded historical data into different aggregates or read models.
- **Jimmy Nilsson**: Advocates decoupling persistence concerns from domain object lifetime, supporting the use of separate aggregates for historical data.

---

## 7. Summary
The chosen approach is to **remove Lots with zero remaining quantity** from the Portfolio aggregate, while maintaining a complete historical record in the Transaction aggregate.

This decision:
- Aligns with established DDD literature and best practices.
- Preserves aggregate efficiency and scalability.
- Maintains a clean separation between **current state** (Portfolio) and **historical state** (Transaction).

> **Key takeaway**: The Portfolio aggregate should reflect only active positions; historical reconstruction is delegated to the Transaction aggregate, ensuring both domain purity and operational performance.