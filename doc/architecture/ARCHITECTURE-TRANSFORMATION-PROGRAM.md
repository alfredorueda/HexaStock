# HexaStock – Enterprise Architecture Transformation Program

> A production-grade engineering program designed to build evolvable systems using Domain-Driven Design and Hexagonal Architecture under real enterprise constraints.

---

# Strategic Engineering Background

This program is grounded in real enterprise software engineering practice applied in large-scale financial environments where correctness, reliability, and long-term evolvability are essential.

The architectural discipline presented here has been applied in:

- High-volume transactional systems
- Regulated financial domains
- Distributed production platforms
- Mission-critical enterprise software

This is not pattern-driven training.  
It is boundary-driven engineering shaped by production constraints.

The focus is on building systems that:

- Protect business logic from infrastructure volatility
- Enforce correct dependency direction
- Encapsulate invariants inside aggregates
- Allow infrastructure to evolve independently
- Remain testable, refactorable, and stable over time

The objective is clear:

**Design systems that grow stronger through change.**

These systems do not merely tolerate change.  
They leverage change as an opportunity to evolve safely, confidently, and predictably.

---

# Industry Validation: Netflix and Architectural Resilience

Netflix Engineering publicly documented its use of Hexagonal Architecture in:

👉 https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749

Netflix explains how isolating core business logic from infrastructure enabled:

- Safe technology evolution
- Reduced ripple effects across services
- Structural resilience in distributed systems
- Continuous change without destabilization

Hexagonal Architecture is not presented as theory.  
It is a structural strategy for adaptability at scale.

The same architectural philosophy underpins this program.

---

# HexaStock: A Production-Grade Architectural Laboratory

Repository:  
👉 https://github.com/alfredorueda/HexaStock

HexaStock is not a demo repository.  
It is a structured architectural laboratory designed to explore disciplined engineering under realistic enterprise conditions.

It includes:

- Gherkin-style behavioural specifications
- Contract-first OpenAPI definition
- A rich domain model
- Clean Hexagonal Architecture
- More than 100 automated tests
- Deterministic CI validation
- Multiple outbound integrations

The architecture is intentionally explicit, inspectable, and verifiable.

It is engineered to make architectural boundaries visible.

---

# Specification-Driven, Contract-First Engineering

HexaStock follows a disciplined engineering loop:

> **Specification → Contract → Tests → Implementation → Refactor Safely**

This sequence ensures that:

- Behaviour is defined before implementation
- Contracts are explicit and versioned
- Tests protect architectural integrity
- Refactoring remains safe

Architecture is not assumed.  
It is continuously validated.

---

## 4.1 Gherkin-Style Behavioural Specification

Functional Specification:

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/stock-portfolio-api-specification.md

Requirements are written using structured **Gherkin-style scenarios**:

- `Given`
- `When`
- `Then`

Each scenario defines:

- Preconditions
- Expected behaviour
- Error cases
- Edge cases
- Explicit outcomes

Behaviour drives design.

---

## 4.2 OpenAPI – Contract-First API Definition

OpenAPI Specification:

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/openapi.yaml

The API is formally defined before implementation details.

It specifies:

- Endpoints
- Request/response schemas
- Error contracts (RFC 7807 compliant)
- HTTP semantics

This guarantees alignment between:

- Specification
- Implementation
- Integration tests

---

## 4.3 Comprehensive Automated Test Suite (100+ Tests)

HexaStock includes **more than 100 automated tests**, covering:

- All happy paths
- Error scenarios
- Domain invariant violations
- Edge cases
- Adapter boundaries
- Contract consistency

Full test suite directory:

👉 https://github.com/alfredorueda/HexaStock/tree/main/src/test/java/cat/gencat/agaur/hexastock

### Domain Tests

- Pure unit tests
- No database
- No web server
- Millisecond execution
- Deterministic

They validate:

- Aggregate invariants
- Strategy behaviour
- Domain error handling
- State transitions

### Integration Tests

- Validate adapter wiring
- Confirm OpenAPI contract alignment
- Use Testcontainers for realistic persistence validation
- Validate end-to-end flows

### Architecture Fitness Tests

- Enforce hexagonal dependency rules using ArchUnit
- Domain must not depend on application, adapters, or Spring
- Application must not depend on adapters
- Inbound and outbound adapters must not depend on each other
- Complement Maven module boundaries with class-level verification

**Source:** `bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java`

CI Pipeline:

👉 https://github.com/alfredorueda/HexaStock/actions

---

## 4.4 Complete Use Case Walkthrough – Rich Domain Model in Action

Full Tutorial:

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md

This tutorial provides a complete end-to-end walkthrough of the **Sell Stocks** use case.

It demonstrates how:

- Business logic is encapsulated inside aggregates
- The domain model is behaviour-driven
- Invariants are enforced internally
- Application services orchestrate but do not own business rules
- Infrastructure is isolated through ports and adapters

The tutorial includes:

- Detailed execution traces
- Class diagrams
- Sequence diagrams
- Architectural flow explanations

The domain is expressive and protected.

---

# Core Architectural Anchors

## Sell Stocks – Domain Backbone

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md

---

## Dependency Inversion in Practice

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md

This tutorial reinforces strict dependency direction:

- Application services define ports
- Infrastructure implements adapters
- The domain remains completely isolated
- Application orchestrates and delegates behaviour to rich aggregates

---

# Engineering Exercises

All exercises follow:

> **Specification → Tests → Implementation → Refactor Safely**

---

## Exercise 1 – Architectural Boundary Tracing

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md#5-step-by-step-execution-trace-happy-path

### Purpose

Make the architecture observable through execution flow.

### What Engineering Teams Gain

- Deep understanding of dependency direction
- Ability to detect boundary violations
- Strong mental model of ports and adapters

---

## Exercise 2 – New Financial Data Provider – Outbound Adapter

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md#exercise-7-add-a-third-stock-price-provider-adapter-prove-the-hexagon-works

### Purpose

Implement integration with a new external provider.

### What Engineering Teams Gain

- Isolation of third-party volatility
- Port abstraction discipline
- Error mapping strategies
- Safe integration practices

---

## Exercise 3 – MongoDB Adapter – Outbound Persistence

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/DDD-Hexagonal-exercise.md#9-optional-infrastructure-extension--multiple-persistence-adapters-mysql--mongodb

### Purpose

Introduce a new persistence technology without modifying domain logic.

### What Engineering Teams Gain

- Experience implementing secondary adapters
- Infrastructure replaceability mindset
- Confidence evolving storage safely

---

## Exercise 4 – Domain Evolution – Extending Lot Strategies

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/DDD-Hexagonal-exercise.md

### Purpose

Extend business logic (LIFO, Highest Cost Basis) using specification-first evolution.

### What Engineering Teams Gain

- Controlled domain growth
- Evolve implementation inside aggregates
- Invariant preservation
- Safe refactoring discipline

---

## Exercise 5 – gRPC / Protocol Buffers – Inbound Adapter

### Purpose

Add an alternative entry point while preserving the domain core.

### What Engineering Teams Gain

- Primary adapter implementation skills
- Multi-interface architectural discipline

---

## Exercise 6 – Watchlists & Market Sentinel – CQRS in Practice

👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md

### Purpose

Introduce a realistic read-heavy monitoring scenario.

As monitoring scales:

- Aggregate rehydration becomes inefficient
- Read workloads dominate
- Performance constraints emerge

CQRS emerges naturally:

- Write model remains invariant-protected
- Read model is optimized
- Boundaries remain clean

### What Engineering Teams Gain

- Understanding of when CQRS is justified
- Balance between DDD purity and performance
- Read-model optimisation strategies
- Scalability awareness

---

# Engineering Quality Standards

Architecture is protected by:

- Gherkin specification
- OpenAPI contract
- 100+ automated tests
- CI enforcement

---

# Why Architecture Matters in the Age of AI

AI accelerates implementation.  
Architecture defines structure.

Without architecture, AI accelerates entropy.  
With architecture, AI amplifies engineering precision.

---

# Organizational Impact

Organizations adopting this discipline gain:

- Infrastructure independence
- Safer evolution
- Reduced technical debt
- Faster onboarding
- Structural clarity

---

# Repository References

Main Repository  
👉 https://github.com/alfredorueda/HexaStock

Sell Use Case  
👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md

Specification  
👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/stock-portfolio-api-specification.md

OpenAPI  
👉 https://github.com/alfredorueda/HexaStock/blob/main/doc/openapi.yaml

Test Suite  
👉 https://github.com/alfredorueda/HexaStock/tree/main/src/test/java/cat/gencat/agaur/hexastock

CI Pipeline  
👉 https://github.com/alfredorueda/HexaStock/actions  