# Extending Lot Selection Strategies in HexaStock
**Domain-Driven Design + Hexagonal (Clean) Architecture**

---

## Table of Contents

- [1. Business Context](#1-business-context)
- [2. Objective of the Assignment](#2-objective-of-the-assignment)
- [3. Fundamental Design Principle (Key Learning Objective)](#3-fundamental-design-principle-key-learning-objective)
- [4. Lot Selection Policy (Key Domain Decision)](#4-lot-selection-policy-key-domain-decision)
- [5. Strategies to Implement](#5-strategies-to-implement)
    - [5.1 Mandatory New Strategies](#51-mandatory-new-strategies)
    - [5.2 Advanced Extra — Specific Lot Identification (Optional)](#52-advanced-extra--specific-lot-identification-optional)
- [6. Expected Design (DDD + Strategy Pattern)](#6-expected-design-ddd--strategy-pattern)
- [7. Hexagonal Architecture](#7-hexagonal-architecture)
- [8. Evaluation Criteria](#8-evaluation-criteria)
- [Pedagogical Closing](#pedagogical-closing)
- [9. Optional Infrastructure Extension — Multiple Persistence Adapters (MySQL & MongoDB)](#9-optional-infrastructure-extension--multiple-persistence-adapters-mysql--mongodb)

---

## 1. Business Context

The **HexaStock** platform was originally designed for the Spanish market.  
In this context, tax regulations require the use of the **FIFO (First-In, First-Out)** criterion when selling stocks. For this reason, the system was initially implemented with FIFO as its default and only lot selection strategy.

At this point, **FIFO is already implemented and working correctly in the system**.

Over recent months, several clients and potential international partners have highlighted an important limitation of the product:  
working exclusively with FIFO is insufficient for operating in other markets and for advanced users who require greater flexibility and analytical capabilities.

As part of the company’s growth strategy, the product is now expected to **expand to international markets**, where:

- different regulations apply,
- alternative lot selection strategies are commonly used,
- and advanced portfolio functionality is expected by expert users.

This creates a clear business opportunity:  
**to extend HexaStock so it supports additional lot selection strategies beyond FIFO**, while preserving architectural quality and minimizing maintenance costs.

---

## 2. Objective of the Assignment

The goal of this assignment is to **evolve the existing system** to support **new lot selection strategies**, beyond FIFO, while strictly respecting:

- **Domain-Driven Design (DDD)** principles
- **Hexagonal (Clean) Architecture**
- strong encapsulation of business rules within the **domain**
- minimal and controlled impact on infrastructure

This assignment is not about rewriting the system, but about **evolving it correctly**.

---

## 3. Fundamental Design Principle (Key Learning Objective)

This assignment is intentionally designed as a deep learning opportunity about software design and architecture.

HexaStock follows the following guiding principle:

> **Business changes should primarily impact the domain.**  
> **Infrastructure changes should impact infrastructure code only.**  
> **The domain must not depend on frameworks, databases, or technical details.**

Introducing new lot selection strategies is a **business rule change**.  
Therefore, the main impact of this extension must be concentrated in the **domain model** and its algorithms.

This exercise aims to demonstrate, in a concrete and practical way, the power of combining **DDD with Hexagonal Architecture** to reduce the cost of change and protect the core of the system.

---

## 4. Lot Selection Policy (Key Domain Decision)

Each **Portfolio** has an associated **Lot Selection Policy** (`LotSelectionPolicy`) that determines how lots are consumed when selling stocks.

### Mandatory Business Rules

1. The policy is defined **when the portfolio is created**.
2. The policy is persisted as part of the aggregate state.
3. **The policy cannot be changed after creation.**
4. All sell operations automatically apply the configured policy.
5. FIFO remains available as the default policy and **must not be removed or broken**.

---

## 5. Strategies to Implement

### 5.1 Mandatory New Strategies

Students must implement **two new strategies**, in addition to the existing FIFO strategy.

#### LIFO — Last In, First Out

- The most recently acquired shares are sold first.
- Clearly illustrates behavioral differences compared to FIFO.
- Algorithmically simple, ideal for introducing the Strategy pattern.

---

#### HIFO — Highest In, First Out

- Shares purchased at the **highest price** are sold first.
- Uses a non-temporal selection criterion.
- Commonly used in fiscal analysis and advanced simulations.
- Provides higher algorithmic and pedagogical value.

Both strategies must be **fully encapsulated within the domain** and designed to be easily extensible.

---

### 5.2 Advanced Extra — Specific Lot Identification (Optional)

As an **advanced optional extension**, students may implement an additional policy.

#### Specific Lot Identification

- Allows selling **explicitly selected lots**.
- The client specifies exactly which lots and how many shares from each lot should be sold.
- Common in advanced brokers and markets such as the United States.

##### Consistency Rules

- `SPECIFIC_ID` is modeled as an additional **portfolio policy**.
- If a portfolio is created with this policy:
    - the sell request **must include explicit lot selection**.
- If the portfolio uses FIFO, LIFO, or HIFO:
    - the sell request **must not include lot selection**.

This extension is optional but will be **highly valued** for advanced students.

---

## 6. Expected Design (DDD + Strategy Pattern)

Lot selection strategies must be modeled using the **Strategy pattern**:

- A common abstraction for lot selection behavior.
- Concrete implementations for FIFO (existing), LIFO, HIFO, and (optional) SPECIFIC_ID.
- Strategy selection depends on the **state of the Portfolio**, not on controllers or application services.

### Explicitly forbidden

- Business logic in REST controllers
- Algorithm selection in the web layer
- Leakage of domain rules into infrastructure code

---

## 7. Hexagonal Architecture

The extension must respect the existing architecture:

- **Domain**  
  Main focus of the change.

- **Application layer**  
  Orchestrates use cases, no algorithmic logic.

- **Inbound adapters (REST)**  
  Input validation and DTO translation.

- **Outbound adapters (persistence)**  
  Persist the policy, no business logic.

---

## 8. Evaluation Criteria

The following aspects will be especially valued:

- Clear separation of responsibilities
- Clean and correct use of the Strategy pattern
- Strong encapsulation of business rules in the domain
- Strict respect for architectural boundaries
- Clear and expressive domain tests
- *(Extra)* Correct implementation of Specific Lot Identification

---

## Pedagogical Closing

> *This assignment is not about adding features, but about demonstrating how a well-designed system can evolve with new business rules without compromising its architecture.*

---

## 9. Optional Infrastructure Extension — Multiple Persistence Adapters (MySQL & MongoDB)

### Context

The current HexaStock system uses a **relational database (MySQL)** via JPA as its persistence mechanism.  
This persistence layer is already implemented following **hexagonal architecture principles**: the domain defines outbound ports (repository interfaces), and the infrastructure provides concrete adapters that implement those ports using JPA.

This design decision has successfully isolated the domain from persistence concerns.

---

### Optional Advanced Requirement

As an **optional and advanced extension**, students may extend the system to support **MongoDB** as an alternative persistence mechanism, in addition to the existing MySQL/JPA implementation.

The choice of persistence technology must be controlled using **Spring Profiles**:

- **Profile `jpa`**: activates the MySQL/JPA adapter
- **Profile `mongodb`**: activates the MongoDB adapter

Switching between MySQL and MongoDB must be possible **without changing domain code**, simply by activating a different Spring profile in the application configuration.

Both adapters must coexist in the codebase, and the application must work correctly with either technology depending on the active profile.

---

### Architectural Constraints (Very Important)

This extension is **optional** and intended for **advanced students** who wish to explore infrastructure flexibility in depth.

If you choose to implement this extension, the following constraints are **mandatory**:

1. **The domain layer must not be modified** to support MongoDB.
   - No MongoDB-specific annotations, types, or logic in domain classes.
   - Domain classes remain persistence-agnostic.

2. **All existing domain tests must remain unchanged** and must pass regardless of the active persistence technology.
   - Domain tests should not know or care whether data is stored in MySQL or MongoDB.

3. **All MongoDB-related code must live exclusively in infrastructure adapters.**
   - Document models, mappers, and repository implementations belong to the adapter layer.

4. **The existing repository ports must be reused.**
   - The MongoDB adapter must implement the same outbound port interfaces already used by the JPA adapter.
   - No new ports should be created for MongoDB.

5. **Profile-based activation must be clean and explicit.**
   - Each adapter should be activated only when its corresponding profile is active.
   - Use Spring's `@Profile` annotation or equivalent mechanisms.

---

### Explicit Pedagogical Objective

This optional extension exists to demonstrate a fundamental architectural principle:

> **Changes in infrastructure impact only infrastructure code.**  
> **A stable and well-designed domain remains unchanged even when the persistence technology changes completely.**

By implementing this extension, you will experience firsthand:

- That **infrastructure is replaceable** without touching business logic.
- That **ports and adapters** provide genuine protection and flexibility.
- The practical value of combining **Domain-Driven Design** with **Hexagonal Architecture** in real-world scenarios.

This is the counterpart to the mandatory assignment:  
- The **mandatory work** (lot selection strategies) demonstrates how **business changes** are isolated in the domain.
- This **optional extension** demonstrates how **infrastructure changes** are isolated in adapters.

Together, they illustrate the complete architectural story.

---

### Implementation Hints (High-Level)

The HexaStock project already demonstrates profile-based adapter selection in another area: **stock price providers** can be switched via Spring profiles. Study that implementation as a reference pattern.

For the MongoDB adapter, consider the following approach:

- Use **Spring Data MongoDB** as the persistence framework.
- Create separate **MongoDB document models** (e.g., `PortfolioDocument`) in the adapter layer.
  - These documents may have a different structure optimized for MongoDB (embedded lots, denormalized data, etc.).
- Implement **dedicated mappers** to translate between domain entities and MongoDB documents.
- Ensure the MongoDB repository implementation satisfies the same port contract as the JPA adapter.
- Both JPA and MongoDB adapters should **coexist in the codebase**, activated conditionally via profiles.

You are free to design the MongoDB document structure as you see fit, as long as:
- It correctly represents the domain state.
- The adapter correctly translates between documents and domain entities.
- All domain rules and invariants are preserved.

---

### Evaluation Notes

This optional extension will be evaluated **separately** from the mandatory assignment and will **not penalize** students who choose not to implement it.

**If you do not implement this extension**, your grade will be based entirely on the mandatory requirements (lot selection strategies, domain design, and tests).

**If you do implement this extension**, it will be assessed based on:

- **Clean separation** between domain and infrastructure (domain remains unchanged).
- **Correct use of Spring profiles** to switch between adapters.
- **Absence of persistence-specific code in the domain** (no JPA or MongoDB leaks).
- **Ability to run the application with either database** by changing configuration only.
- **Quality of the MongoDB adapter design** (document modeling, mapping, error handling).
- **All tests passing** regardless of the active persistence technology.

Successful implementation of this extension will demonstrate **advanced understanding** of hexagonal architecture and will be rewarded accordingly.

---

### Pedagogical Closing

This optional infrastructure extension reinforces the core learning objective of the assignment:

- The **core assignment** focuses on a **business change**: extending lot selection strategies.  
  This change is isolated in the **domain layer**.

- This **optional extension** focuses on an **infrastructure change**: replacing the persistence technology.  
  This change is isolated in the **adapter layer**.

Each type of change is intentionally confined to its proper architectural layer.

By working through both challenges, you will gain deep, practical understanding of how **Domain-Driven Design** and **Hexagonal Architecture** work together to create systems that are resilient to change—whether that change comes from evolving business requirements or from evolving technical infrastructure.

---
