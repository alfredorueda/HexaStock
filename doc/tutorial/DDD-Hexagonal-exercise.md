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
- [10. Deliverables](#10-deliverables)
    - [1. Source Code (GitHub Repository)](#1-source-code-github-repository)
        - [Contribution Requirements](#contribution-requirements)
    - [2. Video Presentation](#2-video-presentation)
        - [Mandatory General Requirements](#mandatory-general-requirements)
        - [Central Requirement: Complete End-to-End Use Case Execution](#central-requirement-complete-end-to-end-use-case-execution)
        - [Architectural Components to Explain (Mandatory)](#architectural-components-to-explain-mandatory)
        - [Domain-Driven Design Understanding (Critical Evaluation Point)](#domain-driven-design-understanding-critical-evaluation-point)
        - [Live Execution Demonstration (Mandatory)](#live-execution-demonstration-mandatory)
        - [Diagrams (Mandatory — PlantUML)](#diagrams-mandatory--plantuml)
        - [Code Demonstration (Mandatory)](#code-demonstration-mandatory)
        - [Test Execution and Explanation (Mandatory)](#test-execution-and-explanation-mandatory)
        - [Content to Cover (Beyond the Core Use Case)](#content-to-cover-beyond-the-core-use-case)
        - [Pedagogical Goal of the Video](#pedagogical-goal-of-the-video)
        - [Understanding and Ownership (Critical)](#understanding-and-ownership-critical)

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

## 10. Deliverables

### 1. Source Code (GitHub Repository)

Students must submit the source code of their implementation through a **GitHub repository**.

The repository must comply with all technical and organizational instructions provided by the instructors in the virtual campus.

#### Contribution Requirements

- **All group members** must contribute proportionally to the repository.
- Contributions must be **clearly reflected in the Git commit history**.
- Even if collaborative tools such as **IntelliJ IDEA Code With Me** are used for pair or group programming, the commit history must demonstrate **equitable participation** from all members.
- Commit messages should be **meaningful** and reflect real development work, not placeholder or trivial commits.

The Git history is part of the evaluation and must demonstrate genuine collaborative software development.

---

### 2. Video Presentation

Students must submit a **video presentation** that explains and demonstrates their solution.

The delivery format (platform, upload method, etc.) will be specified by the instructors in the virtual campus.

---

#### Mandatory General Requirements

The video must comply with the following requirements:

- **Language**: The video must be recorded in **English**.
- **Team participation**: The video must include **all group members**, actively participating in the explanation.
- **Visual support**: Use **diagrams** as visual support throughout the presentation.

---

#### Central Requirement: Complete End-to-End Use Case Execution

The **core focus** of the video must be to explain and demonstrate **the complete execution flow of the stock selling use case using the HIFO strategy (Highest In, First Out)**.

**HIFO is mandatory** for this demonstration. FIFO already exists in the system and must **not** be the focus of the architectural explanation.

Students must demonstrate **deep architectural understanding** by explaining the full execution path, from the incoming HTTP request, through all hexagonal layers, until data is persisted and retrieved, and back to the HTTP response.

This is **not a high-level overview**. Students must walk through the actual implementation, step by step, showing how each architectural component participates in the use case.

---

#### Architectural Components to Explain (Mandatory)

The video must explain **all architectural components involved in the use case**, including:

1. **Inbound Adapter (REST Controller)**
   - How the HTTP request is received
   - Input validation
   - DTO-to-domain translation

2. **Input Port (Use Case Interface)**
   - The contract between the adapter and the application layer
   - Port definition and purpose

3. **Application Service (Use Case Service)**
   - **Critical**: Explain the role of the application service as an **orchestrator**
   - Demonstrate that the service **does not contain business logic**
   - Show how the service **delegates business rules to rich domain entities**
   - Explain how the service coordinates the flow and calls domain methods

4. **Rich Domain Entities and Domain Logic**
   - Show the domain model (e.g., Portfolio, Holding, Lot)
   - Explain how business invariants and policies are enforced **within the domain**
   - Demonstrate that the domain is **rich, not anemic**
   - Show where the actual business logic lives

5. **Domain Strategy Implementation (HIFO)**
   - Explain the Strategy pattern implementation
   - Show how the HIFO strategy is selected and applied
   - Demonstrate the algorithm that selects lots based on highest purchase price
   - Explain how the strategy is encapsulated in the domain

6. **Output Ports (Repository Interfaces)**
   - Explain the outbound port contracts
   - Show how the domain defines its persistence needs without depending on infrastructure

7. **Outbound Adapters (Persistence, Repositories, etc.)**
   - Show the concrete implementation of the outbound ports
   - Explain how the adapter translates domain entities to persistence models (JPA entities, documents, etc.)
   - Demonstrate the separation between domain and infrastructure

8. **Error Handling Paths and Exceptions**
   - Explain how domain exceptions are defined and thrown
   - Show how errors propagate through the layers
   - Demonstrate how adapters translate domain exceptions to HTTP responses

9. **Response Generation and DTO Mapping**
   - Show how the domain result is mapped back to DTOs
   - Explain how the HTTP response is constructed

---

#### Domain-Driven Design Understanding (Critical Evaluation Point)

The video must **explicitly and clearly demonstrate** understanding of Domain-Driven Design principles:

1. **Application Service as Orchestrator**
   - Students must explain that the application service **orchestrates the use case flow** but does **not** contain business logic
   - The service is a thin coordination layer

2. **Delegation to Rich Domain Entities**
   - Students must show how the service **delegates business decisions to domain entities**
   - Business rules, invariants, and algorithms must live in the domain, not in the service

3. **Avoiding Anemic Domain Model**
   - The domain model must be **rich**: entities have behavior, not just data
   - Students must demonstrate that domain entities enforce their own invariants and encapsulate business logic

4. **Strategy Pattern in the Domain**
   - The lot selection strategy (HIFO) must be a **domain concept**, not an infrastructure detail
   - The Portfolio entity must use the strategy to make business decisions

**Failure to demonstrate this understanding will significantly impact the evaluation**, even if the code works correctly.

---

#### Live Execution Demonstration (Mandatory)

The video must demonstrate a **real, live execution** of the use case, not just slides or static diagrams.

Required demonstrations:

1. **Perform a real HTTP call** to the sell endpoint
   - Use an HTTP client (e.g., IntelliJ's `.http` file, Postman, curl, etc.)
   - Show the actual request being sent
   - Show the response received

2. **Show the application running**
   - Start the HexaStock application
   - Demonstrate that the system is actually executing, not simulated

3. **Optional but highly recommended: Use the debugger**
   - Set breakpoints in key components (controller, service, domain entity, strategy, adapter)
   - Step through the execution flow
   - Show how control passes from layer to layer
   - Demonstrate the delegation from the application service to the domain entity
   - Show the strategy being invoked within the domain
   - Illustrate how the Portfolio entity uses the HIFO strategy to select lots

This live demonstration will provide **concrete evidence** of architectural understanding and correct implementation.

---

#### Diagrams (Mandatory — PlantUML)

The video must include and explain **architectural and design diagrams**, created using **PlantUML**.

**Minimum required diagrams**:

1. **Class Diagram**
   - Show the domain model (Portfolio, Holding, Lot, etc.)
   - Show the strategy pattern structure (LotSelectionStrategy interface, HIFO implementation, etc.)
   - Show the ports (input and output port interfaces)
   - Show the adapters (REST controller, repository adapter)
   - The diagram must reflect the **actual implementation**

2. **Sequence Diagram**
   - Show the complete execution flow for the sell use case with HIFO
   - **Critical**: The sequence diagram must clearly illustrate:
     - The REST controller receiving the HTTP request
     - The controller calling the input port (application service)
     - **The application service as orchestrator**: coordinating the flow without business logic
     - **The delegation of business logic to domain entities**: the service calling methods on Portfolio
     - The Portfolio entity using the HIFO strategy to select lots
     - The application service calling the output port (repository) to persist the updated portfolio
     - The adapter translating and persisting the data
     - The response being returned back through the layers
   - The sequence diagram must demonstrate the **separation of concerns** and the **flow of control** through the hexagonal architecture

**Presentation of diagrams**:

- The **rendered results** of the PlantUML diagrams must be shown (PNG, SVG, or rendered in IDE)
- Diagrams must be **clearly explained in English**
- Students must explain each component and interaction shown in the diagrams
- Diagrams must be **consistent** with the actual code shown

---

#### Code Demonstration (Mandatory)

The video must show **relevant source code** directly in **IntelliJ IDEA**.

Required code demonstrations:

1. **Show the key classes involved in the use case**:
   - REST controller
   - Application service
   - Domain entities (Portfolio, Holding, Lot)
   - HIFO strategy implementation
   - Repository adapter

2. **Explain key methods and logic**:
   - Walk through the code that implements the HIFO algorithm
   - Show how the Portfolio entity delegates to the strategy
   - Explain how the application service orchestrates the use case

3. **Highlight architectural boundaries**:
   - Show how the domain does not depend on infrastructure
   - Show how adapters depend on ports, not the other way around

---

#### Test Execution and Explanation (Mandatory)

The video must include **execution and explanation of tests**.

Required test demonstrations:

1. **Execute domain tests related to HIFO**
   - Run unit tests that validate the HIFO strategy algorithm
   - Run domain tests that validate the Portfolio behavior with HIFO
   - Explain what each test validates

2. **Execute integration tests**
   - Run integration tests that validate the full use case with HIFO
   - Explain how the integration tests validate the end-to-end flow

3. **Demonstrate that all previously existing tests still pass**
   - Run the full test suite
   - Show that FIFO tests and other existing tests are still passing
   - This demonstrates that the extension did not break existing functionality

4. **Explain any tests that had to be adapted or fixed**
   - If any existing tests needed to be modified, explain why
   - Justify the changes based on the architectural evolution

---

#### Content to Cover (Beyond the Core Use Case)

In addition to the core end-to-end execution demonstration, the video should briefly cover:

1. **Core Idea and Business Value**
   - Explain the business context and the need for multiple lot selection strategies
   - Describe the solution approach

2. **Architectural Decisions and Trade-offs**
   - Explain key design decisions (e.g., why Strategy pattern, where to place the strategy)
   - Discuss trade-offs and alternative approaches considered

3. **Testing Strategy**
   - Describe the overall testing approach (unit, integration, domain tests)
   - Explain how tests validate domain behavior and architectural boundaries

4. **Optional Extensions (if implemented)**
   - If LIFO, Specific Lot Identification, or MongoDB extension were implemented, provide a brief demonstration

---

#### Pedagogical Goal of the Video

The video must demonstrate that students have achieved the **core learning objective** of the assignment:

- **Deep understanding** of Domain-Driven Design principles
- **Deep understanding** of Hexagonal Architecture
- **Practical ability** to implement and explain a business rule extension that respects architectural boundaries
- **Demonstrated proficiency** of the Strategy pattern in a DDD context
- **Ability to articulate** how the application service orchestrates while the domain encapsulates business logic

The objective is **not merely to show that the system works**, but to **prove that the system has been correctly designed and correctly extended** according to DDD and Hexagonal Architecture principles.

---

#### Understanding and Ownership (Critical)

Students must demonstrate **real, deep understanding** of the code, architecture, and design decisions presented, regardless of whether AI tools or IDE assistance were used during development.

It must be evident that students **understand everything that has been generated or written**, not merely that it exists in the codebase.

**Superficial explanations**, reading from generated documentation without comprehension, or inability to explain design decisions will **not be accepted** and will negatively impact the evaluation.

The video should reflect **genuine mastery** of:
- The architectural design
- The domain model
- The implementation details
- The design patterns used
- The flow of control through the system

Students should be able to answer the question: **"Why did you design it this way?"** for every architectural decision shown.
