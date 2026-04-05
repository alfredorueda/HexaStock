# Specification-Driven Development with AI

**How Structurally Precise Specifications Enable High-Quality AI-Assisted Implementation**

> *"The quality of AI-generated code is bounded by the quality of the specifications it receives."*

---

## About This Chapter

This chapter examines a specific, reproducible engineering practice: using structurally precise specifications — Gherkin scenarios, UML diagrams, OpenAPI contracts, Architecture Decision Records (ADRs), and stack constraints — to guide AI-assisted code generation. The method was validated in real consulting practice, where participants used it both to extend existing codebases and to generate projects from scratch. The model used in those engagements was Claude Opus 4.6, running via GitHub Copilot in VS Code.

This is not a chapter about AI tooling. It is a chapter about software engineering — specifically, about the conditions under which structurally precise specifications enable AI-assisted implementation that meets production engineering standards, whether in greenfield projects or in evolutionary development within an existing system.

---

## 1. Why This Topic Matters Now

Large Language Models (LLMs) integrated into development environments — GitHub Copilot, Cursor, Windsurf, and similar tools — have moved from experimental curiosity to daily engineering practice. Yet the results vary enormously. The same model that generates correct, idiomatic code in one context produces subtly broken, untestable, or architecturally incoherent code in another. The variable is not the model. The variable is the specification context available to the model at generation time.

Most discussions of AI-assisted development focus on the tool: which model, which IDE integration, which prompting technique. This chapter takes a different perspective. It asks: **what engineering practices must already be in place for AI-assisted implementation to produce results that meet the standards of a professional codebase?**

The answer, as demonstrated by the HexaStock project and by broader consulting practice, is that the same artefacts which make software engineering disciplined for humans — Gherkin specifications, UML models, OpenAPI contracts, ADRs, and well-defined architectural boundaries — are precisely the artefacts that make AI-assisted development reliable.

This is not a coincidence. It is a structural consequence of how specification-driven engineering works.

This chapter first demonstrates the method as practised: a specification-first workflow in which precise, multi-layered artefacts guide AI-assisted implementation within a hexagonal architecture. It then examines the method's limitations — particularly its treatment of specifications as initial design documents rather than evolving, living artefacts — and proposes a forward direction: iterative spec-driven engineering, where specifications evolve alongside the codebase, supported by emerging tooling, and where the closed loop between intent and implementation is maintained not only during first generation but across the project's lifetime.

### Scope of Validation

The method described in this chapter — specification-driven development with AI — was validated in a consulting practice, where participants generated projects from scratch using precise specification artefacts in Claude Opus 4.6. Those experiments demonstrated that, when the specification stack is structurally complete, greenfield generation is genuinely feasible: the AI produces architecturally coherent, testable code without an existing codebase to imitate.

The method is viable for both greenfield generation and extension within an established system. The HexaStock case was chosen for documentation because the repository is public, the specification artefacts are committed alongside the code, and the entire workflow is therefore independently reproducible.

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

### 2.2 UML Class Diagrams (Structural Model)

UML class diagrams define the domain model's entities, value objects, aggregates, and their relationships. In HexaStock, these are authored in PlantUML and rendered as SVG:

- **Class diagrams** show that `Portfolio` is the aggregate root, `Holding` and `Lot` are entities within the aggregate boundary, and `Money`, `Price`, `ShareQuantity` are value objects.

For an AI-generated implementation, a UML class diagram provides the type system — aggregate boundaries, entity relationships, value object types, and method signatures. This constrains the generation to an architecturally correct structure.

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
| OpenAPI contract | HTTP interface, request/response shapes, error semantics |
| ADRs | Architectural rationale and design intent |
| Stack constraints | Technology, framework idioms, and conventions |

When an AI operates within all these constraints simultaneously, the space of valid implementations becomes narrow enough that generation reliably produces correct, architecturally coherent code.

---

## 3. From Prompting to Engineering

The typical narrative around AI-assisted development emphasises *prompting* — crafting natural-language instructions that guide the model toward desired output. This framing has a fundamental limitation: it embeds the specification within the prompt, rendering it ephemeral, informal, and unrepeatable.

Specification-driven development with AI inverts this relationship. The specification is not in the prompt. The specification exists as a durable engineering artefact — a Gherkin file, a UML diagram, an OpenAPI contract — that is part of the project repository. The AI reads these artefacts as context and generates code that satisfies their constraints.

### The Difference in Practice

**Prompt-driven approach:**

> "Create a service that sells stocks from a portfolio. It should use FIFO order for lot consumption. Return the proceeds and cost basis."

This prompt is ambiguous. What is "a service"? An application service? A domain service? A static utility? What is the aggregate boundary? How are insufficient shares handled? What types represent money and quantities?

**Specification-driven approach:**

The AI reads:
1. The Gherkin scenario defining FIFO behaviour with concrete values
2. The UML class diagram showing `Portfolio` → `Holding` → `Lot` with `sell()` on `Holding`
3. The OpenAPI contract specifying the REST endpoint and response shape
4. The ADR explaining why `Portfolio` is the aggregate root
5. The existing codebase with its package structure, naming conventions, and value object types

The generation is no longer a creative act on the model's part. It is a *translation* from multiple formal specifications into implementation code. The degrees of freedom are minimal. The model's role is to synthesise the constraints into syntactically correct, idiomatically appropriate code — not to make design decisions.

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

As noted in Section 1, the specification-driven approach documented here was validated in consulting practice in both greenfield and existing-codebase scenarios. The HexaStock system serves as the public, repository-backed case study: a production-grade financial domain application built using AI-assisted development guided by structurally precise specifications. The project was not a proof-of-concept or a toy example. It is a multi-module Maven project with:

- 10 use cases (create portfolio, deposit/withdraw funds, buy/sell stocks, list portfolios, view holdings performance, get stock price, get transaction history)
- A complete domain model with aggregates, entities, value objects, and domain events
- Hexagonal architecture with separate Maven modules for each layer
- Comprehensive testing: unit tests, integration tests (Testcontainers with MySQL), architecture tests (ArchUnit), and executable specifications
- JPA persistence mapping with bidirectional relationships, cascading, and orphan removal
- External API integration (Finnhub stock price provider) with caching
- Global error handling with RFC 7807 problem details
- Docker Compose configuration for local development

### 5.2 The Engineering Workflow

The workflow followed a consistent sequence for each use case, though in practice the steps are iterative: verification (Step 5) frequently surfaces specification gaps that loop back to specification authoring (Step 2), and refinement (Step 6) may generate requirements that restart discovery (Step 1). The steps are numbered for clarity of exposition, not to imply a strictly linear process. Every step is human-directed, AI-assisted where appropriate, and human-validated before the team proceeds. Different stakeholder groups contribute at different stages, depending on their expertise.

> **Terminological note.** In the descriptions that follow, *"business-side stakeholders"* refers to domain experts, product owners, business analysts, and anyone whose primary concern is business correctness. *"Engineering-side stakeholders"* refers to architects, developers, QA engineers, and anyone whose primary concern is technical correctness, architectural fitness, and production readiness.

#### Step 1 — Domain Discovery and Requirements Shaping

**Primary ownership:** Joint \
**Main participants:** Domain experts, product owner, architect, lead engineer \
**Purpose:** Establish the business goals, behavioural expectations, acceptance criteria, edge cases, domain terminology, and any policy or compliance constraints that govern the use case.

**Human role.** Domain experts articulate business rules, priorities, and boundary conditions. Engineers ask probing questions that surface unstated invariants, identify ambiguity, and test the completeness of the requirements. Together, both sides agree on the ubiquitous language that will appear in all downstream artefacts.

**AI assistance.** AI may assist in structuring raw requirements notes into a normalised format, suggesting potential edge cases derived from similar domains, or drafting initial scenario outlines from workshop notes. Any AI-produced draft is a starting point for discussion, not a finished artefact.

**Human review requirement.** Domain experts validate that captured requirements reflect actual business intent and priorities. Engineers validate that requirements are specific enough to be formally specified and subsequently implemented. Disagreements are resolved in joint discussion — not by defaulting to either party.

**Why this matters.** Requirements that are vague, incomplete, or expressed in inconsistent vocabulary propagate downstream into every subsequent artefact. This is the highest-leverage point for business-side participation because it directly shapes what gets built.

#### Step 2 — Specification Authoring

**Primary ownership:** Joint during behavioural specification; engineering-led during architectural formalisation \
**Main participants:** Architect, domain experts (for Gherkin review and acceptance criteria validation), lead engineer \
**Purpose:** Produce the formal specification stack — Gherkin scenarios with concrete values, UML class diagrams, OpenAPI 3.0 contract definitions, and Architecture Decision Records.

**Human role.** The architect makes fundamental design decisions — aggregate boundaries, module structure, port definitions, API shape — that require deep understanding of both the domain and the target architecture. Domain experts review Gherkin scenarios to confirm they express the correct business behaviour, and both sides refine the ubiquitous language as captured in scenario vocabulary.

**AI assistance.** AI may assist in drafting initial Gherkin syntax from structured requirements notes, normalising scenario format for consistency, generating UML diagram skeletons from verbal descriptions, proposing additional edge-case scenarios for human review, or improving OpenAPI schema completeness. Specification authoring is intellectually demanding work — AI assistance improves throughput and consistency without displacing the judgment that drives it.

**Human review requirement.** The architect validates all architectural decisions (aggregate boundaries, layer responsibilities, dependency direction). Domain experts validate that every Gherkin scenario accurately reflects business expectations. Both validate that the ubiquitous language is consistent across artefacts. No specification artefact enters the workflow without explicit human approval from the appropriate stakeholder.

**Why this matters.** Specifications are the single most consequential artefact in this workflow — they directly constrain all downstream generation. A Gherkin scenario with an incorrect `Then` clause or a UML diagram with a misplaced aggregate boundary will produce code that is structurally wrong regardless of how capable the AI is.

#### Step 3 — Context Assembly

**Primary ownership:** Engineering-led \
**Main participants:** Architect or lead engineer \
**Purpose:** Select and assemble the specification artefacts, relevant ADRs, and existing code samples that will serve as context for implementation generation. In VS Code with GitHub Copilot, this context is provided through the conversation window and file references.

**Human role.** The engineer decides which specifications, existing code files, and architectural references are relevant to the current generation task. This requires judgment about scope, interrelationships, and what the AI needs to "see" to produce architecturally correct output. The engineer also determines the generation sequence — which layer is generated first, and what scope is covered per step — based on the project's hexagonal structure.

**AI assistance.** Minimal. Context selection is a judgment-intensive activity that depends on architectural awareness. AI may help surface related files through search or identify references between artefacts, but the selection and scoping decisions remain human.

**Human review requirement.** The engineer verifies that the assembled context is complete and correctly scoped before proceeding. An incomplete context package produces incomplete or inaccurate generation; an overly broad package introduces noise.

**Why this matters.** Context quality directly determines generation quality. The engineer's ability to curate the right context — providing enough for the AI to follow conventions and constraints without overwhelming it with irrelevant material — is a critical skill in this workflow.

#### Step 4 — Implementation Generation

**Primary ownership:** Engineering-led, AI-assisted \
**Main participants:** Architect or lead engineer, AI coding assistant \
**Purpose:** Generate implementation code — domain model classes, application services, REST controllers, JPA entities, mapper classes, test classes — from the assembled specification context.

**Human role.** The engineer directs the generation process: which architectural layer to address first, what conventions to enforce, when to pause and verify, and when to reject and redirect. The engineer provides course corrections during generation, identifying when the AI drifts from architectural intent or naming conventions. This is an active, iterative collaboration — not a hands-off delegation.

**AI assistance.** The AI generates code that translates the formal specifications into syntactically correct, idiomatically appropriate implementation. Given sufficiently precise specifications and existing code as context, the AI's role is primarily translation and synthesis — not design. The AI produces candidate artefacts that satisfy the specification constraints within the conventions established by the existing codebase.

**Human review requirement.** All generated code is reviewed by a qualified engineer for architectural correctness, layer placement, naming consistency with the ubiquitous language, dependency direction, and design intent alignment. Code that compiles is not necessarily acceptable — it must be reviewed against both functional specifications and architectural constraints.

**Why this matters.** AI-generated code can be structurally plausible but architecturally wrong — correct logic in the wrong layer, valid implementations of the wrong design pattern, or subtly broken business rules that compile and look reasonable. Active engineering review is the primary safeguard against these failure modes.

#### Step 5 — Verification and Validation

**Primary ownership:** Engineering-led, with business-side validation of observable behaviour \
**Main participants:** Engineer (test verification, architectural validation), domain experts (acceptance validation) \
**Purpose:** Confirm that generated code satisfies the specification stack and architectural constraints. Validate that observable system behaviour matches business expectations.

**Human role.** Engineers compile, execute tests, and analyse results. They verify that test expected values derive from specifications — not from implementation output — and that architectural boundaries are respected. Domain experts review observable outcomes (response shapes, status codes, edge-case behaviour) against their understanding of business requirements.

**AI assistance.** AI may assist in generating additional test cases for uncovered paths, interpreting test failure messages, and proposing implementation corrections based on precise error output. The engineer decides which corrections to accept.

**Human review requirement.** Engineers confirm that all test assertions trace to specification values, not to implementation artefacts. Domain experts confirm that acceptance criteria are met. Failures are analysed for root cause — not merely corrected until tests pass — because a passing test suite built on incorrect specifications provides false assurance.

**Why this matters.** Verification without validation is insufficient. Code can pass every automated test and still not do what the business intended, if the specifications themselves were incomplete or incorrect. This step closes the loop between engineering correctness and business correctness.

#### Step 6 — Refinement and Hardening

**Primary ownership:** Engineering-led \
**Main participants:** Architect, senior engineers \
**Purpose:** Address edge cases, error handling, cross-cutting concerns (transactionality, caching, concurrency, observability), and production readiness.

**Human role.** Engineers make judgment calls about concurrency strategy, transaction isolation levels, cache invalidation policies, error response semantics, and performance trade-offs. These decisions require an understanding of production operating conditions, failure modes, and non-functional requirements that are rarely captured fully in behavioural specifications. The engineer also identifies and specifies previously unstated invariants surfaced during implementation.

**AI assistance.** Once engineering decisions are made, AI may assist in implementing the chosen patterns — applying a selected concurrency strategy, generating error-handling code following established project conventions, and drafting a configuration for caching or retry policies. The AI executes decisions; it does not make them.

**Human review requirement.** All refinements are reviewed for architectural consistency, production suitability, and alignment with non-functional requirements. The architect confirms that cross-cutting decisions do not introduce unintended coupling or violate the hexagonal structure.

**Why this matters.** Production-grade software requires engineering decisions that transcend functional correctness. Concurrency, resilience, observability, and security are domains where experienced human judgment is irreplaceable. AI can accelerate the implementation of selected patterns, but the selection of patterns remains an engineering responsibility.

#### Stakeholder Participation Across the Workflow

The table below summarises the realistic distribution of stakeholder involvement. The early phases are the most collaborative; later phases become progressively more engineering-led, with business stakeholders returning to validate outcomes.

| Step | Primary Ownership | Business-Side Role | Engineering-Side Role |
|------|------------------|-------------------|-----------------------|
| 1. Domain discovery | **Joint** | Lead: articulate goals, rules, edge cases, terminology | Facilitate, probe for completeness, identify ambiguity |
| 2. Specification authoring | **Joint → Engineering-led** | Validate Gherkin scenarios and acceptance criteria | Author formal specifications, make architectural decisions |
| 3. Context assembly | **Engineering-led** | — | Select, scope, and sequence context for generation |
| 4. Implementation generation | **Engineering-led** | — | Direct generation, provide course corrections, review output |
| 5. Verification & validation | **Engineering-led + Business validation** | Validate observable behaviour against business expectations | Verify test correctness, architectural fitness, non-functional alignment |
| 6. Refinement & hardening | **Engineering-led** | Available for edge-case business clarification | Make cross-cutting engineering decisions, implement and review |

**Key pattern.** Business-side stakeholders contribute most intensively in Steps 1–2, where the specifications that drive everything downstream are shaped and validated. Their role in Steps 5–6 is lighter but still meaningful — they validate that observable outcomes match business intent and clarify edge cases that surface during implementation. Steps 3–4 are primarily engineering activities where business-side participation would add little value.

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
- The UML class diagrams
- The OpenAPI contract
- The architectural decisions (aggregate boundaries, module structure, port definitions)
- The `@SpecificationRef` traceability annotations (added by the architect to link tests to specifications)
- The ArchUnit architecture tests (which verify the hexagonal structure)
- The Docker Compose configuration
- The concurrency control strategy (pessimistic locking, isolation levels)

The pattern is consistent: **humans directed, decided, and validated; AI assisted in implementation within human-defined boundaries.** The architect's role was not reduced — it was refocused on specification quality, architectural judgment, and verification that generated artefacts meet production engineering standards. At no point did AI operate autonomously; at no point did humans write every line manually. The process was human-led, AI-assisted, and human-accountable throughout.

---

## 6. What Made It Work: Analysis of Success Conditions

The HexaStock experience identified six conditions that were necessary for specification-driven AI-assisted development to produce reliable results.

### 6.1 Formal, Structured Specifications

Natural-language requirements alone were insufficient. The AI produced consistently better code when given Gherkin scenarios with concrete values, UML diagrams with explicit relationships, and OpenAPI contracts with typed schemas. The more formal and structured the specification, the less room for misinterpretation.

### 6.2 Existing Codebase as Context

In the HexaStock case documented here, the AI generated new use cases within an existing codebase that already adhered to the target architecture's conventions, patterns, and naming standards. The existing code served as a powerful form of specification-by-example: when the AI needed to create a new value object, it could reference existing value objects (`Money`, `Price`, `ShareQuantity`) and follow the established pattern.

This should not be misread as a precondition of the method. As noted in Section 1, the same approach was validated in consulting practice in true greenfield scenarios, where no prior codebase existed. In those cases, the specification artefacts alone — Gherkin scenarios, UML models, OpenAPI contracts, ADRs, and stack constraints — provided sufficient context for the AI to generate architecturally coherent initial implementations. An existing codebase accelerates and stabilises generation, but it is not required for the method to work.

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

Deciding that `Portfolio` is the aggregate root — and that `Holding` and `Lot` are entities within its boundary rather than separate aggregates — is a design decision that requires understanding of consistency requirements, transaction boundaries, and domain invariants. The AI can implement an aggregate root once the boundary is defined, but it cannot determine the boundary solely from business requirements.

### 7.2 Choosing Concurrency Strategies

Deciding whether to use pessimistic, optimistic, or eventual consistency depends on usage patterns, performance requirements, and failure tolerance. The HexaStock concurrency chapter demonstrates three approaches with real race-condition tests—but the decision on which to use in production was made by the architect, not by the AI.

### 7.3 Balancing Completeness and Simplicity

The AI, when given specifications, tends to generate complete implementations. But engineering judgment often requires *not* implementing something — deferring a feature, choosing a simpler approach, or deliberately accepting a limitation. The AI lacks the context of project timelines, team capabilities, and business priorities that inform these decisions.

### 7.4 Naming and Ubiquitous Language

While the AI can follow naming conventions from existing code, establishing the ubiquitous language itself — deciding that the domain should speak of "lots" rather than "positions," "proceeds" rather than "revenue," "cost basis" rather than "purchase total" — requires domain expertise and stakeholder collaboration.

### 7.5 Identifying Unstated Invariants

Specifications capture stated requirements. But experienced engineers also protect against unstated invariants — conditions that "obviously" should hold but were never formally specified. For example: a sell operation should never produce a negative number of remaining shares. This invariant might not appear in the Gherkin scenarios (because no one thought to test "selling more shares than you own in a single lot that gets partially consumed"). The architect identifies and specifies these invariants; the AI cannot discover them independently.

### 7.6 Architecture Evolution Decisions

When the codebase grows, architectural decisions arise that cross use-case boundaries: Should we introduce domain events? Should we add a CQRS read model? Should we extract a bounded context into a separate service? These strategic decisions require an understanding of the project's trajectory and organisational context that falls outside any single specification artefact.

---

## 8. Risks, Limitations, and Failure Modes

Specification-driven AI-assisted development is not a universal solution. The HexaStock experience also identified failure modes that practitioners should anticipate.

### 8.1 Specification Gaps Produce Implementation Gaps

The AI generates code that satisfies the specifications it receives. If the specifications are incomplete — missing error cases, unspecified edge behaviours, implicit business rules — the generated code will be correspondingly incomplete. Unlike an experienced human developer who might raise questions about missing scenarios, a current-generation AI typically generates a "best guess" that may be plausible but wrong.

**Mitigation:** Invest heavily in specification completeness. Use scenario-outline tables in Gherkin to cover boundary cases. Review specifications with domain experts before using them for generation.

### 8.2 Confident Incorrectness

LLMs generate plausible-looking code with high confidence, even when the code is subtly wrong. During HexaStock development, the AI occasionally generated FIFO logic that consumed lots in the correct order but incorrectly calculated cost basis. The code compiled, the structure looked right, and a casual review would not catch the error. Only the executable specification — with its concrete numeric values — caught the discrepancy.

**Mitigation:** Never trust AI-generated code without executable verification. This is not a "nice to have" — it is the primary safety mechanism.

### 8.3 Context Window Limitations

Current LLMs have finite context windows. For a small-to-medium project like HexaStock, the relevant specifications and existing code fit within the context. For larger projects, not all relevant context can be provided simultaneously. This can lead to:

- Inconsistent naming across modules generated in different sessions
- Duplicate utility methods or value objects
- Architectural drift when the AI cannot "see" the full hexagonal structure

**Mitigation:** Use consistent file and package naming conventions. Maintain a living architecture document (like the HexaStock Architecture Map) that can be included in every generation session. Break generation into small, well-defined tasks, with the relevant context fitting within the window.

### 8.4 Over-Reliance on Generated Tests

If the AI generates both implementation and tests, the tests may reflect the implementation's bugs rather than the specification's intent. A test that asserts `assertEquals(1500.00, result.proceeds())` is only valuable if `1500.00` is the correct value per the specification — not merely the value that the implementation happens to produce.

**Mitigation:** Derive test values from specifications (Gherkin scenarios), not from implementation output. The `@SpecificationRef` traceability annotation in HexaStock exists precisely for this purpose — it links each test to the specification it verifies, making disconnected tests immediately visible.

### 8.5 The Specification Authoring Bottleneck

If AI-assisted development requires comprehensive specifications, and specification authoring remains a human activity, then the bottleneck shifts from implementation to specification. This is not necessarily a problem — specification is arguably where the most important engineering decisions are made — but it does mean that the total effort may not decrease as much as expected.

**Mitigation:** Recognise that the value proposition is not primarily speed. It is quality, consistency, and the ability to maintain a traceable chain from requirements to code. Specification-driven development produces a codebase that is more verifiable and maintainable than ad hoc implementation, whether performed by a human or an AI.

### 8.6 Model Capability Degradation

LLM capabilities vary across model versions, providers, and even between sessions. A workflow that produces excellent results with one model version may produce inferior results with the next. This is a practical concern for teams that depend on AI-assisted development for sustained productivity.

**Mitigation:** Design the workflow to be model-agnostic. The specification stack does not depend on any particular AI model. If the model changes, the specifications remain the same. The feedback loop (generate → test → correct) works with any model that can read code and specifications.

---

## 9. From Specification-First Generation to Iterative Spec-Driven Engineering

The HexaStock workflow described in Section 5 follows a clear sequence: author specifications, assemble context, generate implementation, verify, refine. For pedagogic clarity, this sequence was presented as a series of numbered steps. In practice, the steps are not strictly sequential — Step 5 (verification) frequently surfaces specification gaps that loop back to Step 2 (specification authoring), and Step 6 (refinement) routinely generates new requirements that restart the cycle at Step 1.

This iterative character is not a deviation from the method. It is the method's natural operating mode. Yet the chapter's emphasis to this point has been on the *first pass* — the initial translation from specification to implementation — because that is where AI assistance produces its most visible and measurable gains. The next question is whether the method extends gracefully to ongoing development, where specifications evolve, new requirements emerge, and the codebase accumulates a history of design decisions that constrain future changes.

The answer is conditioned on how specifications are managed over time. If specifications are treated as *initial design documents* — authored once, referenced during generation, and then filed away — the method degrades. Specifications drift from the implementation. New team members encounter code that no longer matches the Gherkin scenarios in the repository. AI-assisted generation in subsequent iterations uses stale context, producing code that contradicts current system behaviour.

If, instead, specifications are treated as *living artefacts* — updated when requirements change, reconciled with implementation after refactoring, and versioned alongside the code they describe — then the method becomes sustainable across the project's lifetime. Specification-driven development evolves from a generation technique into an engineering discipline: not a way to produce the first version of the code, but a way to maintain a traceable, verifiable relationship between intent and implementation throughout the system's evolution.

This distinction — between specification-first generation and iterative spec-driven engineering — is the central insight of this section. HexaStock, as documented in the preceding sections, demonstrates the former. Making the transition to the latter is the next engineering challenge.

---

## 10. Living Specifications as the Missing Agile Layer

Agile methodologies introduced iterative development but left a gap in the specification layer. User stories capture intent in natural language; acceptance criteria are often informal; the relationship between requirements and implementation is maintained in team memory and retrospectives rather than in formal, machine-readable artefacts. When teams adopt AI-assisted development on top of agile workflows, this gap becomes a practical problem: the AI has no durable specification to read, because the specifications were never formalised in the first place, or were formalised once and never updated.

Living specifications address this gap. A living specification is a formal artefact — a Gherkin scenario, an OpenAPI contract, a UML diagram — that is:

1. **Co-located with the code** it describes, in the same repository and subject to the same version control.
2. **Updated when requirements change**, with the specification diff reviewable alongside the code diff.
3. **Executable or verifiable**, so that divergence between specification and implementation is detectible by automated means.
4. **Reviewable by non-engineering stakeholders**, so that domain experts can validate specification changes without reading code.

HexaStock already satisfies the first and third criteria: its Gherkin scenarios are committed to the repository and serve as executable specifications through JUnit tests. The second criterion — systematic updating upon requirement change — is currently a manual discipline. The fourth is partially addressed by Gherkin's business-readable syntax, though no formal review workflow currently formalises domain-expert participation in specification changes.

The specification-driven workflow described in this chapter is therefore not complete in its present form. It demonstrates that precise specifications enable high-quality AI-assisted generation, but it does not yet demonstrate a closed-loop process in which specifications evolve alongside the implementation across multiple development iterations, with changes to either side propagated and reconciled systematically.

This is not a theoretical concern. Any project that practises specification-driven development for more than a few iterations will face the question: *when the requirements change, what happens to the specifications?* If the answer is "they are manually updated by whoever remembers to do so," the method depends on human diligence in a way that is fragile and unverifiable. If the answer is "the tooling detects specification–implementation drift and surfaces it for review," the method becomes robust.

The emerging generation of spec-driven development tools — discussed in the following section — represents the first serious effort to provide that tooling.

---

## 11. Next Step: Tool-Supported Spec Evolution in HexaStock

The argument so far can be summarised as follows: precise specifications enable reliable AI-assisted generation (Sections 2–6); human judgment remains essential for architectural decisions, boundary definition, and unstated invariants (Section 7); the method has identifiable risks and failure modes (Section 8); and the next evolution is to make specifications living, iterative artefacts rather than one-pass design documents (Sections 9–10). The practical question is: what tooling exists to support this evolution?

Two open-source projects merit evaluation as potential complements to the HexaStock workflow: **OpenSpec** and **GitHub Spec Kit**. Neither was used during HexaStock development. They are presented here not as endorsements but as tools whose design philosophies align with the iterative, specification-driven approach advocated in this chapter, and whose suitability for HexaStock's architecture and workflow warrants investigation.

### 11.1 OpenSpec

[OpenSpec](https://github.com/Fission-AI/OpenSpec) (Fission-AI, MIT licence) is a spec-driven development framework for AI coding assistants that emphasises fluidity and iteration over rigid phase gates. Its stated philosophy — *"fluid not rigid, iterative not waterfall, built for brownfield not just greenfield"* — directly addresses the limitation identified in Section 9: that specification-first workflows can become brittle if specifications are treated as immutable design documents rather than evolving artefacts.

OpenSpec organises work around *changes*, each of which receives its own folder containing a proposal, specifications, a design document, and a task list. When a change is implemented and verified, it is archived, and the project specifications are updated to reflect the new state. This change-based workflow naturally supports specification evolution: each modification to the system is accompanied by a specification delta that is reviewable, traceable, and archivable.

For HexaStock, the relevant question is whether OpenSpec's change-based structure could complement the existing Gherkin + UML + OpenAPI specification stack, providing a systematic process for updating specifications when requirements evolve — rather than relying on the current manual discipline of keeping specification artefacts in sync.

### 11.2 GitHub Spec Kit

[GitHub Spec Kit](https://github.com/github/spec-kit) (GitHub, MIT licence) provides a CLI-oriented toolkit that structures the spec-driven development lifecycle into explicit, composable phases: constitution (project principles), specification (what to build), planning (technical approach), task breakdown, and implementation. The workflow is formalised through the `specify` CLI and a set of slash commands that integrate with over twenty AI coding assistants, including GitHub Copilot.

Spec Kit's extensibility is notable. A modular extension and preset system allows teams to customise templates, add domain-specific commands, enforce organisational standards, and integrate with external tools — without modifying the core workflow. Community walkthroughs include brownfield Java projects (extending a 420,000-line Jakarta EE runtime) and Spring Boot applications, demonstrating that the approach is not limited to greenfield scenarios.

For HexaStock, Spec Kit's interest lies in its formalised operating model for moving from specification to implementation in a structured, repeatable way. The existing HexaStock workflow (Section 5.2) is conceptually similar — specification authoring, context assembly, generation, verification — but is currently orchestrated manually by the architect. Spec Kit's CLI-driven approach could provide guardrails and traceability that the current workflow achieves through discipline alone.

### 11.3 Evaluation Criteria, Not Adoption Recommendations

Neither OpenSpec nor Spec Kit has been evaluated against HexaStock's specific requirements: a hexagonal Java 21 / Spring Boot 3 project with DDD aggregates, JPA persistence, and an existing Gherkin + OpenAPI specification stack. This chapter does not recommend adopting either tool. It identifies them as promising candidates for a structured evaluation, which the following section outlines.

The broader point is methodological, not tool-specific. The specification-driven approach described in this chapter requires tooling support to scale beyond manually orchestrated single-iteration workflows. Whether that support comes from OpenSpec, Spec Kit, a different tool, or a bespoke integration matters less than the principle: *the specifications that drive AI-assisted development must be maintainable, evolvable, and integrated into the team's development workflow as first-class artefacts.*

---

## 12. Concrete Experiment Scenarios for a Future Iteration

The following experiments are designed to test whether the specification-driven approach, as practised in HexaStock, can be extended with tool support to achieve the iterative, living-specification workflow described in Sections 9–10. Each experiment is scoped to a specific, testable hypothesis.

### 12.1 Specification-to-Implementation Traceability Under Change

**Hypothesis:** Tooling can maintain automatic traceability between Gherkin scenarios and generated implementation code across requirement changes.

**Experiment:** Select a HexaStock use case (e.g., deposit funds). Modify the Gherkin scenarios to add a new business rule (e.g., a maximum deposit limit). Use tool-assisted generation to update the implementation. Evaluate whether the tool correctly identifies all code locations affected by the specification change and whether the resulting implementation satisfies the modified specification without introducing regressions.

### 12.2 Specification Drift Detection

**Hypothesis:** Automated tooling can detect when implementation diverges from its governing specifications after manual code changes.

**Experiment:** Manually modify a HexaStock domain class (e.g., alter the FIFO algorithm in `Holding.sell()`) without updating the corresponding Gherkin scenario. Run tool-assisted drift detection. Evaluate whether the tool identifies the divergence, reports which specification constraints are violated, and proposes corrective actions — either to update the specification or revert the code.

### 12.3 Brownfield Specification Bootstrap

**Hypothesis:** Spec-driven development tools can generate initial specifications from an existing, well-structured codebase.

**Experiment:** Apply OpenSpec or Spec Kit to the existing HexaStock repository without providing any pre-authored specifications. Evaluate whether the tool generates meaningful specification artefacts from the codebase's structure, tests, and documentation — and how those generated artefacts compare in quality and completeness to the hand-authored specifications that currently exist in the repository.

### 12.4 Multi-Iteration Specification Evolution

**Hypothesis:** Specification-driven workflows can maintain coherence across multiple development iterations without manual reconciliation.

**Experiment:** Implement three consecutive feature additions to HexaStock (e.g., watchlists, portfolio reporting, dividend tracking) using a tool-supported specification workflow. After each iteration, assess whether the specification stack remains internally consistent, whether traceability is maintained, and whether AI-assisted generation in later iterations benefits from the specification history accumulated in earlier ones.

### 12.5 Cross-Stakeholder Specification Review

**Hypothesis:** Tool-supported specification workflows can provide domain experts with a reviewable, non-technical view of specification changes.

**Experiment:** Introduce a requirement change that affects both Gherkin scenarios and OpenAPI contracts. Use tooling to generate a change summary suitable for domain-expert review. Evaluate whether a non-technical stakeholder can assess the proposed change without reading implementation code, and whether their feedback can be incorporated into the specification before generation proceeds.

### 12.6 Specification-Driven Refactoring Guidance

**Hypothesis:** When architectural refactoring is needed, specifications can guide the refactoring and verify that the refactored implementation remains consistent with the original specification intent.

**Experiment:** Select a refactoring candidate in HexaStock (e.g., extracting a bounded context, introducing domain events for sell operations). Use the existing specification stack to guide AI-assisted refactoring. Evaluate whether the specifications provide sufficient constraints to prevent the refactoring from altering observable behaviour, and whether the tests derived from those specifications catch any regressions introduced during the refactoring process.

---

## 13. What Changes for Software Architects and Technical Leads

Specification-driven AI-assisted development does not eliminate the architect's role. It redefines the activities where the architect's time is most productive.

### 13.1 Specification Authoring Becomes the Primary Deliverable

In traditional development, the architect's primary deliverable is often an architecture document or a set of design decisions that developers interpret into code. In specification-driven development with AI, the architect's primary deliverable is the specification stack itself — Gherkin scenarios, UML models, OpenAPI contracts, ADRs — because these artefacts directly drive implementation.

This raises the bar on specification quality. A Gherkin scenario with vague `Then` clauses or a UML diagram with ambiguous relationships will produce ambiguous code. The architect must write specifications that are precise enough to constrain generation to correct implementations.

### 13.2 Code Review Shifts to Architectural Verification

The architect spends less time reviewing whether the code "works" (tests verify this) and more time reviewing whether the code is *architecturally appropriate*:

- Is the logic in the correct layer?
- Does the naming align with the ubiquitous language?
- Are the dependency directions correct?
- Are the aggregate boundaries respected?
- Is the code unnecessarily complex?

### 13.3 Testing Strategy Becomes Infrastructure

Tests are no longer just verification — they are the feedback mechanism that drives iterative correction of AI-generated code. The testing infrastructure (Testcontainers setup, base test classes, test data builders) becomes critical engineering infrastructure rather than an afterthought.

### 13.4 The Team Composition Question

If AI handles a significant portion of implementation, the team's skill distribution may shift. One possible model observed in HexaStock development:

- **Architect/Specifier** (human): Writes specifications, defines boundaries, reviews generated code, makes strategic decisions
- **Implementation engine** (AI): Generates code from specifications within defined boundaries
- **Verification engineer** (human + AI): Ensures generated code satisfies specifications and architectural constraints

This is not a prediction about the future of software teams. It is a description of the workflow that emerged naturally during the HexaStock consulting engagement.

---

## 14. Practical Guidance for Teams

For teams considering specification-driven AI-assisted development, the HexaStock experience suggests the following practical steps.

### 14.1 Start with an Existing Specification Practice

AI-assisted development amplifies existing engineering practices. If your team already writes Gherkin scenarios, maintains UML models, and uses contract-first API design, you can begin using these artefacts as AI context immediately. If your team does not have these practices, introducing AI-assisted development alongside specification-driven design will conflate two significant workflow changes, making it difficult to diagnose problems.

### 14.2 Define Architectural Boundaries Before Generating Code

Ensure that module structure, package conventions, port interfaces, and aggregate boundaries are defined before the AI generates implementation code. These boundaries are constraints that improve the quality of generation. Defining them afterwards means retroactively refactoring AI-generated code, which loses most of the productivity benefit.

### 14.3 Generate One Layer at a Time

Do not ask the AI to generate an entire use case across all layers simultaneously. Generate the domain model first, then the application service, then the REST controller, then the persistence adapter. Verify each layer before proceeding. This mirrors the hexagonal architecture's layer separation and keeps each generation step small enough to review thoroughly.

### 14.4 Derive Test Values from Specifications, Not from Implementation

When the AI generates tests, verify that the expected values in assertions come from the Gherkin scenarios or business requirements — not from running the implementation and recording its output. Tests that merely record implementation behaviour are tautological and provide no safety.

### 14.5 Keep a Living Architecture Document

Maintain a concise document that summarises the project's architectural decisions, conventions, and boundaries. Include this document as context in every AI generation session. In HexaStock, the Architecture Map table in the sell-stock tutorial serves this purpose.

### 14.6 Review AI Output for Architectural Correctness, Not Just Functional Correctness

Code that passes all tests may still be architecturally inappropriate. Look for:

- Business logic in application services instead of aggregates
- Direct repository access from controllers
- Value objects with mutable state
- Framework annotations in domain classes
- Unnecessary abstractions or premature optimisations

### 14.7 Document What the AI Cannot See

Some constraints are not captured in specifications: performance requirements, regulatory compliance, team conventions, deployment constraints. Maintain explicit documentation for these constraints and review AI-generated code against them.

---

## 15. Relationship with the Sell Stock Tutorial

The **[Sell Stock Deep Dive](../sellStocks/SELL-STOCK-TUTORIAL.md)** serves as the primary worked example for this chapter's principles. The entire specification stack described in Section 2 — Gherkin scenarios, UML diagrams, OpenAPI contracts — is available in the HexaStock repository, and the sell-stock tutorial shows how those specifications translate into implemented, tested code.

Specific connections:

| This Chapter | Sell Stock Tutorial |
|-------------|-------------------|
| Section 2.1 — Gherkin Scenarios | Section 3 — Functional Specification (Gherkin) |
| Section 2.2 — UML Class Diagrams | Section 6 — Domain Context (class diagrams) |
| Section 2.3 — OpenAPI Contract | Section 7 — REST Entry Point |
| Section 3 — From Prompting to Engineering | Section 16 — Requirements Traceability (`@SpecificationRef`) |
| Section 4.3 — DDD and AI | Section 10 — Services vs Aggregates (anti-pattern demonstration) |
| Section 4.4 — Hexagonal Architecture and AI | Section 8 — Architecture Map, Section 1 — Architecture Overview |

The sell-stock tutorial was not written as an illustration of AI-assisted development. It was authored first as a specification-driven engineering tutorial. The fact that the same artefacts serve both pedagogical and AI-context purposes is not a design choice — it is a natural consequence of specification-driven engineering.

---

## 16. Conclusion

Specification-driven development with AI is not a new methodology. It is the application of established engineering practices — BDD, TDD, DDD, Hexagonal Architecture — in a context where AI serves as an implementation engine rather than a design authority. The quality of AI-generated code is determined not by the sophistication of the model or the cleverness of the prompt, but by the precision and completeness of the specifications provided as context.

The HexaStock case demonstrates this principle concretely. When a project maintains formal specifications (Gherkin, UML, OpenAPI), enforces hard architectural boundaries (hexagonal modules, aggregate roots, port interfaces), and verifies output through executable tests (JUnit, Testcontainers, ArchUnit), AI-assisted implementation produces code that meets production engineering standards. The broader consulting experience confirms that the same method works in true greenfield scenarios — generating projects from scratch from precise specification artefacts — without requiring a pre-existing codebase as scaffolding.

Yet HexaStock also reveals the method's current boundary. The workflow documented in this chapter treats specifications as durable artefacts that guide generation — but it does not yet demonstrate a fully iterative process in which specifications evolve systematically alongside the implementation across multiple development cycles. The specification updating that occurred during development relied on human discipline rather than tooling support. This is adequate for a demonstration project; it is insufficient for sustained production development.

The forward direction is clear. Well-structured engineering specifications remain the foundation, and the next major advance is to make those specifications iterative, living, reviewable, and better integrated into agile development workflows. Emerging tools such as OpenSpec and GitHub Spec Kit represent early efforts to formalise this integration. Whether through these tools or through bespoke process discipline, the principle is the same: specifications that drive AI-assisted development must be maintained as first-class, evolving artefacts — not archived design documents.

HexaStock, as a public repository with committed specification artefacts, provides a credible demonstration of the first-generation approach and a concrete platform for evaluating the next. The experiments outlined in Section 12 offer a path from demonstration to validated practice. The aspiration is engineering discipline without rigidity, agility without vagueness, and iteration without architectural drift.

The architect's role does not diminish. It evolves — from writing implementation code to writing and maintaining specifications, defining boundaries, reviewing generated artefacts for architectural fitness, and increasingly, orchestrating the tools and processes that keep specifications and implementation in sync. This shift places a higher premium on specification quality and architectural judgment: the activities where human engineering expertise is least replaceable.

---

## 17. References

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

### Spec-Driven Development Tools (Referenced for Evaluation)

- [OpenSpec](https://github.com/Fission-AI/OpenSpec) — Spec-driven development framework for AI coding assistants, emphasising iterative, change-based workflows
- [GitHub Spec Kit](https://github.com/github/spec-kit) — CLI-oriented toolkit for structured spec-driven development with composable phases and extensibility

---

## Acknowledgements

This chapter reflects insights gained during consulting engagements focused on specification-driven AI-assisted software development. The HexaStock project serves as the public, repository-backed case study, and the engineering workflow described here emerged from real development practice — both in evolutionary development within an existing codebase and in greenfield generation from scratch — not from theoretical projection. The forward-looking sections on iterative spec-driven engineering, living specifications, and tool-supported spec evolution (Sections 9–12) represent the author's analysis of natural next steps, informed by the practical experience documented in the earlier sections and by publicly available information about the tools referenced.

The author acknowledges the AI tools used — specifically GitHub Copilot with Claude Opus 4.6 operating within VS Code — as implementation instruments whose output quality was directly determined by the specification artefacts described in this chapter.

---

*This chapter is part of the [HexaStock technical book](../../BOOK-HOME.md). Return to the [book home page](../../BOOK-HOME.md) or continue to the [Sell Stock Deep Dive](../sellStocks/SELL-STOCK-TUTORIAL.md).*
