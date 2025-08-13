# Domain Model Design Decision: Portfolio, Holdings, Lots, and Transactions

## 1. Purpose
This document formalizes the design decision for structuring the **Portfolio** domain and its related concepts (**Holdings**, **Lots**, and **Transactions**) in alignment with **Domain-Driven Design (DDD)** principles, as introduced by **Eric Evans** in *Domain-Driven Design: Tackling Complexity in the Heart of Software* (2003) and further developed by experts such as **Vaughn Vernon** (*Implementing Domain-Driven Design*, 2013) and **Jimmy Nilsson** (*Applying Domain-Driven Design and Patterns*, 2006).

The goal is to maintain a **clean, invariant-focused domain model** while ensuring **high performance and practical feasibility** in a Spring + JPA application.

---

## 2. Aggregate Structure

### 2.1 Portfolio as Aggregate Root
Following Evans’ definition of **aggregate roots** and Vernon’s recommendations for **transactional consistency boundaries**:
- **Role**: Central point for enforcing business invariants related to investment portfolios.
- **Contains**:
    - **Holdings**: Represent positions in specific assets.
    - **Lots**: Represent individual batches of an asset acquired at a certain time and price.
- **Reasoning**:
    - Holdings and Lots are part of the same **consistency boundary** and must be updated atomically to preserve business rules.
    - Their size remains bounded in practice, avoiding performance issues common with unbounded collections inside aggregates.

### 2.2 Transactions as a Separate Aggregate
Aligned with Vernon’s principle that aggregates should remain **small and focused on invariants**:
- **Role**: Immutable record of all operations that have affected the Portfolio (deposits, withdrawals, purchases, sales).
- **Reasoning**:
    - Transaction history grows without bound and is not needed for enforcing Portfolio invariants.
    - Placing Transactions in a separate aggregate prevents large data loads, reduces memory pressure, and allows independent optimization for querying and reporting.
    - This separation supports Nilsson’s emphasis on **decoupling persistence concerns from the domain model**.

---

## 3. Aggregate Boundaries and Relationships
- **Portfolio** is the aggregate root responsible for maintaining the **current state** of the investment account, including its Holdings and Lots.
- **Transactions** are modeled as a distinct aggregate, linked to the Portfolio via an identifier rather than a direct object reference, preserving loose coupling and reducing accidental eager loading.
- This aligns with Evans’ recommendation that aggregates should be designed to be loaded and modified atomically without excessive data.

---

## 4. Application Layer Coordination
Following DDD’s **layered architecture** and Vernon’s approach to **application services**:
- The application service coordinates operations that affect both aggregates.
- Typical workflow:
    1. Load the Portfolio aggregate.
    2. Apply domain logic to modify Holdings and Lots.
    3. Persist the updated Portfolio.
    4. Create and persist the corresponding Transaction entry.
- In single-database scenarios, all steps occur within an ACID transaction.
- In distributed scenarios, a domain event (e.g., *TradeExecuted*) is published and consumed to persist the Transaction asynchronously, ensuring eventual consistency.

---

## 5. Performance Considerations
- **Bounded Aggregate Size**: Transactions are excluded from the Portfolio to prevent performance degradation.
- **Lazy Loading Risks**: Even with lazy loading, large collections introduce dirty-checking overhead and accidental loads; separating aggregates mitigates this risk.
- **Optimized Queries**: Transactions can be retrieved independently with pagination and projections.
- **Summary Fields**: Derived values (e.g., cash balance, market value) can be stored directly in Portfolio to avoid recomputations from transaction history.

---

## 6. Advantages of This Design

### 6.1 Alignment with DDD Principles
- Aggregates are modeled according to business invariants and transactional boundaries (Evans, Vernon).
- The Portfolio aggregate enforces rules and maintains consistent state.
- The Transaction aggregate focuses on historical and analytical concerns.

### 6.2 Scalability and Maintainability
- Scales effectively as transaction volume grows.
- Maintains query performance through separation of concerns.
- Supports future adoption of patterns such as **CQRS** or **event sourcing** (as recommended by Vernon) without major restructuring.

---

## 7. Summary
This design:
- **Respects DDD tactical modeling** by keeping aggregates small, cohesive, and invariant-focused.
- **Preserves performance** by avoiding unbounded collections inside a single aggregate.
- **Aligns with guidance** from Evans, Vernon, and Nilsson on aggregate boundaries and performance.
- Fits naturally with Spring + JPA repository patterns and application service orchestration.

> **Key Decision**: The Portfolio aggregate contains Holdings and Lots but not the full list of Transactions. Transactions are modeled as a separate aggregate, linked by identifier and coordinated via the application layer. This approach maintains domain purity, scalability, and operational efficiency.