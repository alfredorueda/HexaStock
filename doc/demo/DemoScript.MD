# Meeting Purpose

To introduce **Domain-Driven Design (DDD)** and **Hexagonal Architecture**.

# Key Takeaways

## 1. DDD restores OOP

DDD counters *anemic* entities, which are data-only objects with no real behavior, by embedding business logic directly into domain objects. This makes them **rich domain objects** and creates a **single source of truth** for business behavior.

## 2. Hexagonal Architecture protects the core

Hexagonal Architecture isolates the domain from infrastructure concerns such as databases and external APIs through **Input Ports** and **Output Ports**. This prevents technical dependencies from leaking into the domain and keeps the core **technology-agnostic**.

## 3. Decoupling enables rapid testing

Because the domain is isolated, it can be tested without a database or network. This enables **very fast unit tests** (around **150 ms**) and greatly improves the development feedback loop.

## 4. Ports require explicit mapping

A critical step in this architecture is mapping infrastructure-specific objects, such as JPA entities, to pure domain objects when they cross a port boundary. This preserves the independence and purity of the domain core.

# Topics

## The Problem: Anemic Entities and Infrastructure Coupling

Traditional layered architectures such as:

`Controller → Service → DAO`

often lead to **anemic entities**, which are simple data containers with no behavior.

This has several consequences:

- Business logic ends up concentrated in service classes.
- Services become a single point of failure.
- The system becomes fragile and harder to maintain.
- Testing becomes more difficult.
- The business core becomes tightly coupled to infrastructure concerns, such as a specific database.

As a result, changing technology later becomes expensive and risky.

## The Solution: DDD and Hexagonal Architecture

### Domain-Driven Design (DDD)

DDD focuses on modeling the business domain through **rich entities** that contain both:

- data
- behavior

This helps create a **single source of truth** for business rules and makes the code more robust, expressive, and easier to understand.

### Hexagonal Architecture (Ports and Adapters)

Hexagonal Architecture is a structural pattern that protects the domain by isolating it from external systems and technical concerns.

#### Ports

Ports define **contracts**, usually as Java interfaces, for how the domain interacts with the outside world.

##### Input Ports

Input Ports define the functionality that the domain exposes to external actors.

Example:

- `sellStocks(...)`

##### Output Ports

Output Ports define the functionality that the domain needs from external systems.

Example:

- `fetchStockPrice(...)`

#### Adapters

Adapters implement the ports using concrete technologies.

Examples:

- a **REST controller** implementing access through an Input Port
- a **JPA repository adapter** implementing a Persistence Output Port
- an **external API adapter** implementing a stock price provider

# Demo: Selling Stocks with FIFO Logic

The demo presented a financial portfolio application built using **DDD** and **Hexagonal Architecture**.

## Use Case

Selling **15 Apple shares** using a **First-In, First-Out (FIFO)** strategy.

## Execution Flow

### 1. Web Adapter (`RestController`)

The web adapter performs basic input validation, for example:

- quantity must be greater than zero

This allows the system to **fail fast** and avoids wasting domain resources on invalid requests.

### 2. Input Port (`PortfolioStockOperationUseCase`)

The controller invokes the domain through this interface.

### 3. Application Service

The application service orchestrates the use case.

Its responsibilities include:

- retrieving the `Portfolio` from the database through a **Persistence Output Port**
- retrieving the current stock price from an external API through a **StockPriceProvider Output Port**
- delegating the actual business logic to the domain model

### 4. Domain Entities (`Portfolio`, `Holding`, `Lot`)

The complex FIFO selling logic is delegated to the domain itself, specifically to the `sell(...)` method on `Holding`.

This follows the **Tell, Don’t Ask** principle:

- instead of extracting data and making decisions outside the entity,
- the application tells the entity what business operation to perform.

The `Holding` then:

- consumes shares from its `Lot` objects in purchase order
- updates `remainingShares`
- removes empty lots when necessary

# Testing Strategy: Rapid Feedback Loop

## Domain Tests

Domain tests run in complete isolation, without infrastructure.

This means:

- no database
- no network
- no web server

### Benefits

- extremely fast execution
- immediate feedback during development
- easier validation of business rules

In this case, domain tests run in about **150 ms**.

They are often written in **Given-When-Then** style and may map directly to **Gherkin feature files**, which improves clarity and communication with business stakeholders.

## Integration Tests

Integration tests validate the full stack working together, including:

- database
- web server
- adapters
- application services

These tests are slower, for example **4+ seconds**, but they are essential to verify that all components collaborate correctly in a real execution flow.

# Summary

DDD and Hexagonal Architecture work especially well together:

- **DDD** gives us a rich, expressive, behavior-centered domain model.
- **Hexagonal Architecture** protects that model from infrastructure concerns.
- The result is a system that is:
    - easier to understand
    - easier to test
    - easier to maintain
    - easier to evolve over time

The stock-selling demo illustrated this clearly by showing how FIFO business logic can remain inside the domain while the surrounding infrastructure is kept separate through ports and adapters.