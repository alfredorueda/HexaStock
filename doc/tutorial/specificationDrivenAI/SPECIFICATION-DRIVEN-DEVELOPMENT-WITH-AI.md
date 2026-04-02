# Specification-Driven Development with AI

**How Structurally Precise Specifications Enable High-Quality AI-Assisted Implementation**

> *"The quality of AI-generated code is bounded by the quality of the specifications it receives."*

---

## About This Chapter

This chapter examines a specific, reproducible engineering practice: using structurally precise specifications — Gherkin scenarios, UML diagrams, OpenAPI contracts, Architecture Decision Records (ADRs), and stack constraints — to guide AI-assisted code generation within a disciplined software architecture. The analysis is grounded in a real consulting engagement where the entire HexaStock implementation was produced through this method using GitHub Copilot with Claude as the underlying model, operating within VS Code.

This is not a chapter about AI tooling. It is a chapter about software engineering — specifically, about what happens when the specification artefacts that already exist in a well-engineered project are used as the primary input to an AI coding assistant, and about the conditions under which that approach produces results that meet production engineering standards.

---

## Table of Contents

1. [Why This Topic Matters Now](#1-why-this-topic-matters-now)
2. [The Specification Stack](#2-the-specification-stack)
3. [From Prompting to Engineering](#3-from-prompting-to-engineering)
4. [How BDD, TDD, DDD, and Hexagonal Architecture Reinforce AI-Assisted Delivery](#4-how-bdd-tdd-ddd-and-hexagonal-architecture-reinforce-ai-assisted-delivery)
5. [The Consulting Case: End-to-End Implementation with AI](#5-the-consulting-case-end-to-end-implementation-with-ai)
6. [What Made It Work: Analysis of Success Conditions](#6-what-made-it-work-analysis-of-success-conditions)
7. [What Still Requires Human Engineering Judgment](#7-what-still-requires-human-engineering-judgment)
8. [Risks, Limitations, and Failure Modes](#8-risks-limitations-and-failure-modes)
9. [What Changes for Software Architects and Technical Leads](#9-what-changes-for-software-architects-and-technical-leads)
10. [Practical Guidance for Teams](#10-practical-guidance-for-teams)
11. [Relationship with the Sell Stock Tutorial](#11-relationship-with-the-sell-stock-tutorial)
12. [Conclusion](#12-conclusion)
13. [References](#13-references)

---

## 1. Why This Topic Matters Now

Large Language Models (LLMs) integrated into development environments — GitHub Copilot, Cursor, Windsurf, and similar tools — have moved from experimental curiosity to daily engineering practice. Yet the results vary enormously. The same model that generates correct, idiomatic code in one context produces subtly broken, untestable, or architecturally incoherent code in another. The variable is not the model. The variable is the specification context available to the model at generation time.

Most discussions of AI-assisted development focus on the tool: which model, which IDE integration, which prompting technique. This chapter takes a different perspective. It asks: **what engineering practices must already be in place for AI-assisted implementation to produce results that meet the standards of a professional codebase?**

The answer, as demonstrated by the HexaStock project, is that the same artefacts which make software engineering disciplined for humans — Gherkin specifications, UML models, OpenAPI contracts, ADRs, and well-defined architectural boundaries — are precisely the artefacts that make AI-assisted development reliable.

This is not a coincidence. It is a structural consequence of how specification-driven engineering works.

---

## 2. The Specification Stack

HexaStock's engineering workflow produces a layered set of specification artefacts before any implementation begins. These artefacts serve as the primary context for both human comprehension and AI generation.

### 2.1 Gherkin Scenarios (Behavioural Specification)

Gherkin captures *what the system should do* in business language. Each scenario defines a precondition (`Given`), an action (`When`), and an observable outcome (`Then`). For the sell-stocks use case:

```gherkin
Scenario: Sell stocks with FIFO lot consumption
  Given a portfolio "P-001" with:
    | ticker | shares | costBasis |
    | AAPL   | 50     | 7500.00   |
  And the portfolio has lots:
    | ticker | shares | purchasePrice | purchasedAt         |
    | AAPL   | 30     | 150.00        | 2024-01-15T10:00:00 |
    | AAPL   | 20     | 150.00        | 2024-02-20T14:30:00 |
  When the investor sells 40 shares of "AAPL" at 180.00
  Then the sell result should have:
    | sharesSold | proceeds | totalCostBasis |
    | 40         | 7200.00  | 6000.00        |
  And the remaining lots should be:
    | ticker | shares | purchasePrice |
    | AAPL   | 10     | 150.00        |
```

For an AI, this scenario is not prose — it is a structured, parseable specification of expected behaviour, with concrete values that can be directly translated into test assertions.

### 2.2 UML Diagrams (Structural and Behavioural Models)

UML class diagrams define the domain model's entities, value objects, aggregates, and their relationships. UML sequence diagrams trace the execution flow through architectural layers. In HexaStock, these are authored in PlantUML and rendered as SVG:

- **Class diagrams** show that `Portfolio` is the aggregate root, `Holding` and `Lot` are entities within the aggregate boundary, and `Money`, `Price`, `ShareQuantity` are value objects.
- **Sequence diagrams** show the message flow: `PortfolioRestController` → `PortfolioStockOperationsUseCase` → `PortfolioStockOperationsService` → `Portfolio.sell()` → `Holding.sell()`.

For an AI generating implementation code, a UML class diagram provides the type system. A UML sequence diagram provides the call graph. Together, they constrain generation to architecturally correct structure.

### 2.3 OpenAPI 3.0 Contract (Interface Specification)

The OpenAPI specification defines every endpoint, request body, response schema, status code, and error format. For the sell endpoint:

```yaml
/api/portfolios/{portfolioId}/sales:
  post:
    summary: Sell stocks from a portfolio
    requestBody:
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/SellRequest'
    responses:
      '200':
        description: Sell result
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SellResult'
      '409':
        description: Insufficient shares
```

An AI reading this contract knows the exact HTTP method, path, request shape, response shape, status codes, and error semantics — without any ambiguity.

### 2.4 Architecture Decision Records (ADRs)

ADRs document *why* specific design decisions were made. For example: why `Portfolio` is the aggregate root (consistency boundary for balance and holdings), why FIFO is implemented in `Holding.sell()` and not in the application service (domain logic belongs in the domain model), why lots with zero remaining quantity are retained (audit trail requirement). These records provide the *rationale* that prevents an AI from making structurally correct but architecturally wrong decisions.

### 2.5 Stack and Framework Constraints

Technology choices — Java 21, Spring Boot 3, Maven multi-module, JPA/Hibernate, JUnit 5, Testcontainers — are documented in the project configuration. These constraints narrow the generation space: the AI produces Spring `@Service` classes rather than generic service implementations, JPA `@Entity` annotations rather than raw JDBC, and `@SpringBootTest` integration tests rather than framework-agnostic scaffolding.

### The Combined Effect

No single artefact is sufficient. The combined specification stack creates a multi-dimensional constraint surface:

| Artefact | What It Constrains |
|----------|--------------------|
| Gherkin scenarios | Observable behaviour and business rules |
| UML class diagrams | Type system, aggregate boundaries, relationships |
| UML sequence diagrams | Call graph and layer responsibilities |
| OpenAPI contract | HTTP interface, request/response shapes, error semantics |
| ADRs | Architectural rationale and design intent |
| Stack constraints | Technology, framework idioms, and conventions |

When an AI operates within all these constraints simultaneously, the space of valid implementations becomes narrow enough that generation reliably produces correct, architecturally coherent code.

---

## 3. From Prompting to Engineering

The typical narrative around AI-assisted development emphasises *prompting* — crafting natural-language instructions that guide the model toward desired output. This framing has a fundamental limitation: it places the specification inside the prompt, making it ephemeral, informal, and unrepeatable.

Specification-driven development with AI inverts this relationship. The specification is not in the prompt. The specification exists as a durable engineering artefact — a Gherkin file, a UML diagram, an OpenAPI contract — that is part of the project repository. The AI reads these artefacts as context and generates code that satisfies their constraints.

### The Difference in Practice

**Prompt-driven approach:**

> "Create a service that sells stocks from a portfolio. It should use FIFO order for lot consumption. Return the proceeds and cost basis."

This prompt is ambiguous. What is "a service"? An application service? A domain service? A static utility? What is the aggregate boundary? How are insufficient shares handled? What types represent money and quantities?

**Specification-driven approach:**

The AI reads:
1. The Gherkin scenario defining FIFO behaviour with concrete values
2. The UML class diagram showing `Portfolio` → `Holding` → `Lot` with `sell()` on `Holding`
3. The UML sequence diagram showing the call chain from controller through service to aggregate
4. The OpenAPI contract specifying the REST endpoint and response shape
5. The ADR explaining why `Portfolio` is the aggregate root
6. The existing codebase with its package structure, naming conventions, and value object types

The generation is no longer a creative act by the model. It is a *translation* from multiple formal specifications into implementation code. The degrees of freedom are minimal. The model's role is to synthesise the constraints into syntactically correct, idiomatically appropriate code — not to make design decisions.

### Why This Distinction Matters

Prompt-driven development scales poorly. Each prompt must re-specify context that already exists elsewhere. The specifications are informal and vary between prompts. There is no way to verify that the prompt was complete or consistent.

Specification-driven development leverages artefacts that already exist in a well-engineered project. The specifications are formal, versioned, and testable. The AI's output can be verified against the same specifications that guided its generation — creating a closed verification loop.

---

## 4. How BDD, TDD, DDD, and Hexagonal Architecture Reinforce AI-Assisted Delivery

Each engineering discipline that HexaStock employs creates a specific advantage for AI-assisted implementation. These advantages compound.

### 4.1 Behaviour-Driven Development (BDD)

BDD's contribution is **executable acceptance criteria**. Gherkin scenarios define the system's expected behaviour in a structured, parseable format. For an AI:

- The `Given` clause specifies the test fixture — the initial state the AI must construct.
- The `When` clause specifies the action — the method call or HTTP request the AI must invoke.
- The `Then` clause specifies the assertion — the concrete values the AI must verify.

This is not vague guidance. It is a formal specification of input-output behaviour with concrete values. An AI can generate a JUnit test method that is *provably correct with respect to the Gherkin scenario* because the scenario's structure maps directly to test structure.

### 4.2 Test-Driven Development (TDD)

TDD's contribution is **immediate feedback**. When AI generates code that fails a test, the failure message identifies the exact discrepancy. The AI can read the failure, understand which specification constraint was violated, and correct the implementation. This creates a tight feedback loop:

1. AI generates implementation from specifications
2. Tests run (automatically or manually)
3. Failures identify specific constraint violations
4. AI reads failures and corrects implementation
5. Tests pass — implementation satisfies specifications

This loop is mechanistic. It does not require the AI to "understand" the domain. It requires the AI to satisfy formal constraints, which is precisely what LLMs are effective at when sufficient context is provided.

### 4.3 Domain-Driven Design (DDD)

DDD's contribution is **bounded context and aggregate boundaries**. For AI-assisted development, these boundaries serve as hard constraints on where code belongs:

- Business logic belongs in aggregates, not in services.
- All mutations to entities within an aggregate pass through the aggregate root.
- Value objects are immutable, self-validating types.
- Domain events represent facts about state changes.

These rules are not guidelines — they are structural constraints that an AI can be instructed to follow. When the UML class diagram shows `Portfolio` as the aggregate root with `sell()` as a method on `Holding` (accessible only through `Portfolio`), the AI knows that selling logic must not appear in `PortfolioStockOperationsService`. The architectural rule prevents the most common category of AI-generated code smell: logic in the wrong layer.

### 4.4 Hexagonal Architecture

Hexagonal Architecture's contribution is **dependency direction and layer isolation**. The ports-and-adapters pattern creates hard boundaries that constrain AI generation:

- Inbound adapters (REST controllers) can depend on primary ports (use case interfaces) but not on domain internals.
- Application services implement primary ports and depend on secondary ports (outbound interfaces) but not on adapter implementations.
- Domain code depends on nothing outside itself.

When an AI generates a REST controller, it can only call methods on a use case interface — it cannot access the repository directly. When it generates an application service, it can only access the database through a port — it cannot import JPA classes. These constraints are enforced by the module structure (`adapters-inbound-rest`, `application`, `domain`, `adapters-outbound-persistence-jpa`) and verified by ArchUnit tests.

For AI generation, hexagonal architecture transforms a single large codebase into multiple small, constrained generation problems. Generating a driven adapter is a different, isolated problem from generating domain logic. Each problem has fewer degrees of freedom, which increases generation accuracy.

### 4.5 The Compound Effect

| Discipline | Constrains | Effect on AI Generation |
|------------|-----------|------------------------|
| BDD | What the system does | Concrete input-output pairs for test generation |
| TDD | Whether it works | Immediate verification of generated code |
| DDD | Where logic lives | Prevents logic placement errors |
| Hexagonal | What depends on what | Prevents dependency direction violations |

When all four disciplines converge, the AI is operating within a highly constrained space where the "correct" implementation is largely determined by the specifications. The model's strength — pattern matching and synthesis across large context windows — is applied to translation rather than invention.

---

## 5. The Consulting Case: End-to-End Implementation with AI

### 5.1 Context

The HexaStock system was developed to demonstrate that a production-grade financial domain application could be built using specification-driven AI-assisted development. The project was not a proof of concept or a toy example. It is a multi-module Maven project with:

- 10 use cases (create portfolio, deposit/withdraw funds, buy/sell stocks, list portfolios, view holdings performance, get stock price, get transaction history)
- A complete domain model with aggregates, entities, value objects, and domain events
- Hexagonal architecture with separate Maven modules for each layer
- Comprehensive testing: unit tests, integration tests (Testcontainers with MySQL), architecture tests (ArchUnit), and executable specifications
- JPA persistence mapping with bidirectional relationships, cascading, and orphan removal
- External API integration (Finnhub stock price provider) with caching
- Global error handling with RFC 7807 problem details
- Docker Compose configuration for local development

### 5.2 The Engineering Workflow

The workflow followed a consistent sequence for each use case:

**Step 1 — Specification authoring (human).** The architect wrote Gherkin scenarios, UML diagrams, and OpenAPI contract definitions for the use case. This step required deep domain understanding and design judgment. It could not be delegated to AI.

**Step 2 — Context assembly (human).** The relevant specification artefacts were assembled as context: the Gherkin file, the UML diagrams, the OpenAPI snippet, any relevant ADRs, and the existing codebase. In VS Code with GitHub Copilot, this context was provided through the conversation window and file references.

**Step 3 — Implementation generation (AI).** GitHub Copilot, using the specification context, generated the implementation code: domain model classes, application services, REST controllers, JPA entities, mapper classes, and test classes. The generation was guided by the specifications, not by free-form prompts.

**Step 4 — Verification (human + AI).** The generated code was compiled and tested. Test failures were fed back to the AI for correction. The architect reviewed the generated code for architectural correctness, naming consistency, and design intent alignment.

**Step 5 — Refinement (human).** Edge cases, error flows, and cross-cutting concerns (transactionality, caching, concurrency) were refined through iterative dialogue between the architect and the AI, always grounded in specifications and test results.

### 5.3 What the AI Generated

For the sell-stocks use case specifically, the AI generated:

- `Portfolio.sell()` — aggregate root method coordinating the sell operation
- `Holding.sell()` — FIFO lot consumption algorithm
- `SellResult` — value object representing the sell outcome
- `PortfolioStockOperationsService.sellStock()` — application service orchestration
- `PortfolioRestController.sellStocks()` — REST endpoint
- `SellRequest` / `SellResponse` — API DTOs
- `PortfolioStockOperationsServiceTest` — unit tests with mock dependencies
- `SellStockIntegrationTest` — Testcontainers-based integration tests
- JPA entity mappings and repository implementations

### 5.4 What the AI Did Not Generate

- The Gherkin scenarios
- The UML class and sequence diagrams
- The OpenAPI contract
- The architectural decisions (aggregate boundaries, module structure, port definitions)
- The `@SpecificationRef` traceability annotations (added by the architect to link tests to specifications)
- The ArchUnit architecture tests (which verify the hexagonal structure)
- The Docker Compose configuration
- The concurrency control strategy (pessimistic locking, isolation levels)

The division is clear: **the architect specified; the AI implemented.** The architect's role was not reduced — it was refocused on specification, verification, and architectural judgment.

---

## 6. What Made It Work: Analysis of Success Conditions

The HexaStock experience identified six conditions that were necessary for specification-driven AI-assisted development to produce reliable results.

### 6.1 Formal, Structured Specifications

Natural-language requirements alone were insufficient. The AI produced consistently better code when given Gherkin scenarios with concrete values, UML diagrams with explicit relationships, and OpenAPI contracts with typed schemas. The more formal and structured the specification, the less room for misinterpretation.

### 6.2 Existing Codebase as Context

The AI did not generate the project from scratch. It generated new use cases within an existing codebase that already demonstrated the conventions, patterns, and naming standards. The existing code served as a powerful form of specification-by-example. When the AI needed to create a new value object, it could reference existing value objects (`Money`, `Price`, `ShareQuantity`) and follow the established pattern.

### 6.3 Hard Architectural Boundaries

The Maven multi-module structure physically prevented dependency violations. The AI could not import a JPA class in the domain module because the domain module had no JPA dependency. This is a stronger constraint than any prompt instruction — it is compiler-enforced.

### 6.4 Executable Tests as Verification

Every AI-generated method had a corresponding test (or multiple tests) that could be executed immediately. Test failures provided precise, actionable feedback. The AI could read a `java.lang.AssertionError: expected: <1800.00> but was: <1500.00>` and understand exactly what needed to change.

### 6.5 Small, Incremental Generation Steps

The AI was not asked to generate the entire application at once. It generated one layer at a time, one use case at a time. Each generation step was small enough to verify completely before proceeding. This mirrors the TDD red-green-refactor cycle, applied to AI generation.

### 6.6 Active Human Review

The architect reviewed every generated artefact. This review was not perfunctory. It caught:

- Correct code in the wrong architectural layer
- Naming inconsistencies with the ubiquitous language
- Over-engineering (unnecessary abstractions, excessive error handling)
- Subtle domain logic errors that passed tests but violated unstated invariants
- Framework anti-patterns (e.g., `@Transactional` on private methods, which bypasses Spring AOP proxying)

The AI is a powerful generator. It is not a reliable reviewer of its own output.

---

## 7. What Still Requires Human Engineering Judgment

AI-assisted development shifts the architect's role but does not eliminate it. The following activities required human judgment that the AI could not replicate, even with comprehensive specifications:

### 7.1 Defining Aggregate Boundaries

Deciding that `Portfolio` is the aggregate root — and that `Holding` and `Lot` are entities within its boundary rather than separate aggregates — is a design decision that requires understanding of consistency requirements, transaction boundaries, and domain invariants. The AI can implement an aggregate root once the boundary is defined, but it cannot determine the boundary from business requirements alone.

### 7.2 Choosing Concurrency Strategies

Whether to use pessimistic locking, optimistic locking, or eventual consistency is a decision that depends on usage patterns, performance requirements, and failure tolerance. The HexaStock concurrency chapter demonstrates three approaches with real race-condition tests — but the decision of which approach to use in production was made by the architect, not by the AI.

### 7.3 Balancing Completeness and Simplicity

The AI, when given specifications, tends to generate complete implementations. But engineering judgment often requires *not* implementing something — deferring a feature, choosing a simpler approach, or deliberately accepting a limitation. The AI does not have the context of project timelines, team capabilities, or business priorities that inform these decisions.

### 7.4 Naming and Ubiquitous Language

While the AI can follow naming conventions from existing code, establishing the ubiquitous language itself — deciding that the domain should speak of "lots" rather than "positions," "proceeds" rather than "revenue," "cost basis" rather than "purchase total" — requires domain expertise and stakeholder collaboration.

### 7.5 Identifying Unstated Invariants

Specifications capture stated requirements. But experienced engineers also protect against unstated invariants — conditions that "obviously" should hold but were never formally specified. For example: a sell operation should never produce a negative number of remaining shares. This invariant might not appear in the Gherkin scenarios (because no one thought to test "selling more shares than you own in a single lot that gets partially consumed"). The architect identifies and specifies these invariants; the AI cannot discover them independently.

### 7.6 Architecture Evolution Decisions

When the codebase grows, architectural decisions arise that cross use-case boundaries: Should we introduce domain events? Should we add a CQRS read model? Should we extract a bounded context into a separate service? These strategic decisions require understanding of the project's trajectory and organisational context that falls outside any single specification artefact.

---

## 8. Risks, Limitations, and Failure Modes

Specification-driven AI-assisted development is not a universal solution. The HexaStock experience also identified failure modes that practitioners should anticipate.

### 8.1 Specification Gaps Produce Implementation Gaps

The AI generates code that satisfies the specifications it receives. If the specifications are incomplete — missing error cases, unspecified edge behaviours, implicit business rules — the generated code will be correspondingly incomplete. Unlike an experienced human developer who might raise questions about missing scenarios, a current-generation AI typically generates a "best guess" that may be plausible but wrong.

**Mitigation:** Invest heavily in specification completeness. Use scenario-outline tables in Gherkin to cover boundary cases. Review specifications with domain experts before using them for generation.

### 8.2 Confident Incorrectness

LLMs generate plausible-looking code with high confidence, even when the code is subtly wrong. In the HexaStock development, the AI occasionally generated FIFO logic that consumed lots in the correct order but calculated cost basis incorrectly. The code compiled, the structure looked right, and a casual review would not catch the error. Only the executable specification — with its concrete numeric values — caught the discrepancy.

**Mitigation:** Never trust AI-generated code without executable verification. This is not a "nice to have" — it is the primary safety mechanism.

### 8.3 Context Window Limitations

Current LLMs have finite context windows. For a small-to-medium project like HexaStock, the relevant specifications and existing code fit within the context. For larger projects, not all relevant context can be provided simultaneously. This can lead to:

- Inconsistent naming across modules generated in different sessions
- Duplicate utility methods or value objects
- Architectural drift when the AI cannot "see" the full hexagonal structure

**Mitigation:** Use consistent file and package naming conventions. Maintain a living architecture document (like the HexaStock Architecture Map) that can be included in every generation session. Break generation into small, well-defined tasks where the relevant context fits within the window.

### 8.4 Over-Reliance on Generated Tests

If the AI generates both implementation and tests, the tests may reflect the implementation's bugs rather than the specification's intent. A test that asserts `assertEquals(1500.00, result.proceeds())` is only valuable if `1500.00` is the correct value per the specification — not merely the value that the implementation happens to produce.

**Mitigation:** Derive test values from specifications (Gherkin scenarios), not from implementation output. The `@SpecificationRef` traceability annotation in HexaStock exists precisely for this purpose — it links each test to the specification it verifies, making disconnected tests immediately visible.

### 8.5 The Specification Authoring Bottleneck

If AI-assisted development requires comprehensive specifications, and specification authoring remains a human activity, then the bottleneck shifts from implementation to specification. This is not necessarily a problem — specification is arguably where the most important engineering decisions are made — but it does mean that the total effort may not decrease as much as expected.

**Mitigation:** Recognise that the value proposition is not primarily speed. It is quality, consistency, and the ability to maintain a traceable chain from requirements to code. Specification-driven development with AI produces a codebase that is more verifiable and more maintainable than ad-hoc implementation, whether performed by a human or an AI.

### 8.6 Model Capability Degradation

LLM capabilities vary across model versions, providers, and even between sessions. A workflow that produces excellent results with one model version may produce inferior results with the next. This is a practical concern for teams that depend on AI-assisted development for sustained productivity.

**Mitigation:** Design the workflow to be model-agnostic. The specification stack does not depend on any particular AI model. If the model changes, the specifications remain. The feedback loop (generate → test → correct) works with any model that can read code and specifications.

---

## 9. What Changes for Software Architects and Technical Leads

Specification-driven AI-assisted development does not eliminate the architect's role. It redefines the activities where the architect's time is most productive.

### 9.1 Specification Authoring Becomes the Primary Deliverable

In traditional development, the architect's primary deliverable is often an architecture document or a set of design decisions that developers interpret into code. In specification-driven development with AI, the architect's primary deliverable is the specification stack itself — Gherkin scenarios, UML models, OpenAPI contracts, ADRs — because these artefacts directly drive implementation.

This raises the quality bar for specifications. A Gherkin scenario with vague `Then` clauses or a UML diagram with ambiguous relationships will produce ambiguous code. The architect must write specifications that are precise enough to constrain generation to correct implementations.

### 9.2 Code Review Shifts to Architectural Verification

The architect spends less time reviewing whether the code "works" (tests verify this) and more time reviewing whether the code is *architecturally appropriate*:

- Is the logic in the correct layer?
- Does the naming align with the ubiquitous language?
- Are the dependency directions correct?
- Are the aggregate boundaries respected?
- Is the code unnecessarily complex?

### 9.3 Testing Strategy Becomes Infrastructure

Tests are no longer just verification — they are the feedback mechanism that drives iterative correction of AI-generated code. The testing infrastructure (Testcontainers setup, base test classes, test data builders) becomes critical engineering infrastructure, not an afterthought.

### 9.4 The Team Composition Question

If AI handles a significant portion of implementation, the team's skill distribution may shift. One possible model observed in HexaStock development:

- **Architect/Specifier** (human): Writes specifications, defines boundaries, reviews generated code, makes strategic decisions
- **Implementation engine** (AI): Generates code from specifications within defined boundaries
- **Verification engineer** (human + AI): Ensures generated code satisfies specifications and architectural constraints

This is not a prediction about the future of software teams. It is a description of the workflow that emerged naturally during the HexaStock consulting engagement.

---

## 10. Practical Guidance for Teams

For teams considering specification-driven AI-assisted development, the HexaStock experience suggests the following practical steps.

### 10.1 Start with an Existing Specification Practice

AI-assisted development amplifies existing engineering practices. If your team already writes Gherkin scenarios, maintains UML models, and uses contract-first API design, you can begin using these artefacts as AI context immediately. If your team does not have these practices, introducing AI-assisted development simultaneously with specification-driven design will conflate two significant workflow changes and make it difficult to diagnose problems.

### 10.2 Define Architectural Boundaries Before Generating Code

Ensure that module structure, package conventions, port interfaces, and aggregate boundaries are defined before the AI generates implementation code. These boundaries are constraints that improve generation quality. Defining them afterward means retroactively refactoring AI-generated code, which loses most of the productivity benefit.

### 10.3 Generate One Layer at a Time

Do not ask the AI to generate an entire use case across all layers simultaneously. Generate the domain model first, then the application service, then the REST controller, then the persistence adapter. Verify each layer before proceeding. This mirrors the hexagonal architecture's layer separation and keeps each generation step small enough to review thoroughly.

### 10.4 Derive Test Values from Specifications, Not from Implementation

When the AI generates tests, verify that the expected values in assertions come from the Gherkin scenarios or business requirements — not from running the implementation and recording its output. Tests that merely record implementation behaviour are tautological and provide no safety.

### 10.5 Keep a Living Architecture Document

Maintain a concise document that summarises the project's architectural decisions, conventions, and boundaries. Include this document as context in every AI generation session. In HexaStock, the Architecture Map table in the sell-stock tutorial serves this purpose.

### 10.6 Review AI Output for Architectural Correctness, Not Just Functional Correctness

Code that passes all tests may still be architecturally inappropriate. Look for:

- Business logic in application services instead of aggregates
- Direct repository access from controllers
- Value objects with mutable state
- Framework annotations in domain classes
- Unnecessary abstractions or premature optimisations

### 10.7 Document What the AI Cannot See

Some constraints are not captured in specifications: performance requirements, regulatory compliance, team conventions, deployment constraints. Maintain explicit documentation for these constraints and review AI-generated code against them.

---

## 11. Relationship with the Sell Stock Tutorial

The **[Sell Stock Deep Dive](../sellStocks/SELL-STOCK-TUTORIAL.md)** serves as the primary worked example for this chapter's principles. The entire specification stack described in Section 2 — Gherkin scenarios, UML diagrams, OpenAPI contracts — exists in the HexaStock repository, and the sell-stock tutorial traces how those specifications translate into implemented, tested code.

Specific connections:

| This Chapter | Sell Stock Tutorial |
|-------------|-------------------|
| Section 2.1 — Gherkin Scenarios | Section 3 — Functional Specification (Gherkin) |
| Section 2.2 — UML Diagrams | Section 6 — Domain Context (class diagrams), Section 9 — Execution Trace (sequence diagrams) |
| Section 2.3 — OpenAPI Contract | Section 7 — REST Entry Point |
| Section 3 — From Prompting to Engineering | Section 16 — Requirements Traceability (`@SpecificationRef`) |
| Section 4.3 — DDD and AI | Section 10 — Services vs Aggregates (anti-pattern demonstration) |
| Section 4.4 — Hexagonal Architecture and AI | Section 8 — Architecture Map, Section 1 — Architecture Overview |

The sell-stock tutorial was not written as an illustration of AI-assisted development. It was authored first as a specification-driven engineering tutorial. The fact that the same artefacts serve both pedagogical and AI-context purposes is not a design choice — it is a natural consequence of specification-driven engineering.

---

## 12. Conclusion

Specification-driven development with AI is not a new methodology. It is the application of established engineering practices — BDD, TDD, DDD, Hexagonal Architecture — in a context where AI serves as an implementation engine rather than a design authority. The quality of AI-generated code is determined not by the sophistication of the model or the cleverness of the prompt, but by the precision and completeness of the specifications provided as context.

The HexaStock case demonstrates that when a project maintains formal specifications (Gherkin, UML, OpenAPI), enforces hard architectural boundaries (hexagonal modules, aggregate roots, port interfaces), and verifies output through executable tests (JUnit, Testcontainers, ArchUnit), AI-assisted implementation can produce code that meets production engineering standards.

The architect's role does not diminish. It shifts — from writing implementation code to writing specifications, defining boundaries, and reviewing generated artefacts for architectural fitness. This shift places a higher premium on specification quality and architectural judgment, which are the activities where human engineering expertise is least replaceable.

The specifications that make AI-assisted development reliable are the same specifications that make any software project well-engineered. There is no separate "AI-ready" practice. There is only disciplined engineering, which happens to be the optimal input for both human developers and AI assistants.

---

## 13. References

### Books and Papers

- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013.
- Cockburn, Alistair. *Hexagonal Architecture.* alistair.cockburn.us, 2005.
- North, Dan. "Introducing BDD." *dannorth.net*, 2006.
- Martin, Robert C. *Clean Architecture: A Craftsman's Guide to Software Structure and Design.* Prentice Hall, 2017.
- Beck, Kent. *Test Driven Development: By Example.* Addison-Wesley, 2002.

### HexaStock Project

- [HexaStock Repository](https://github.com/alfredorueda/HexaStock) — Full source code, specifications, and documentation
- [Sell Stock Deep Dive](../sellStocks/SELL-STOCK-TUTORIAL.md) — End-to-end architectural tutorial tracing the sell use case
- [Concurrency Control with Pessimistic and Optimistic Locking](../CONCURRENCY-PESSIMISTIC-LOCKING.md) — Companion study on concurrency strategies
- [Rich vs Anemic Domain Model](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) — Comparative architectural study

### Tools

- [GitHub Copilot](https://github.com/features/copilot) — AI-assisted development tool integrated with VS Code
- [VS Code](https://code.visualstudio.com/) — Development environment
- [JUnit 5](https://junit.org/junit5/) — Testing framework
- [Testcontainers](https://testcontainers.com/) — Integration testing with real databases
- [ArchUnit](https://www.archunit.org/) — Architecture verification through unit tests
- [PlantUML](https://plantuml.com/) — Diagram authoring

---

## Acknowledgements

This chapter reflects insights gained during a consulting engagement focused on specification-driven AI-assisted software development. The HexaStock project served as the case study vehicle, and the engineering workflow described here emerged from real development practice, not from theoretical projection.

The author acknowledges the AI tools used during HexaStock development — specifically GitHub Copilot operating within VS Code — as implementation instruments whose output quality was directly determined by the specification artefacts described in this chapter.

---

*This chapter is part of the [HexaStock technical book](../../BOOK-HOME.md). Return to the [book home page](../../BOOK-HOME.md) or continue to the [Sell Stock Deep Dive](../sellStocks/SELL-STOCK-TUTORIAL.md).*
