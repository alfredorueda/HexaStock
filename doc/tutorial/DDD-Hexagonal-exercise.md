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
