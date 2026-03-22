# Contributing to HexaStock

Thank you for your interest in contributing to HexaStock. This project exists at the intersection of production engineering and technical education — it is a working financial portfolio system built with Java 21, Spring Boot 3, Domain-Driven Design, and Hexagonal Architecture. Every contribution, whether code, documentation, or specification, strengthens both the system and the body of knowledge around it.

This document explains how to contribute effectively and what we expect from contributions.

---

## Types of Contributions Welcome

- **Code** — New features, bug fixes, refactorings that respect the architectural boundaries.
- **Tests** — Unit tests, integration tests, and specification-linked test cases.
- **Specifications** — Gherkin scenarios, OpenAPI contract updates, and behavioural definitions.
- **Documentation** — Architectural studies, reading map entries, diagram updates, and editorial improvements to existing documents.
- **Exercises** — Practice problems and worked examples that deepen understanding of the codebase.
- **Bug reports and design discussions** — Filed as GitHub Issues with enough context to reproduce or reason about the problem.

---

## Core Architectural Principles to Preserve

HexaStock is structured around a small set of non-negotiable architectural principles. Every contribution must respect them.

**Domain-Driven Design.** Business logic lives in the domain layer — inside aggregates, entities, and value objects. Application services orchestrate; they do not decide. If a rule belongs to the business, it belongs in the domain model.

**Hexagonal Architecture (Ports and Adapters).** All dependencies point inward toward the domain. The domain has zero knowledge of frameworks, databases, HTTP, or any infrastructure concern. Communication between the core and the outside world flows exclusively through port interfaces.

**The Inward Dependency Rule.** Adapters depend on ports. Ports are defined by the core. The domain depends on nothing external. Violating this rule — even for convenience — is not acceptable.

**Value Objects over Primitives.** The domain uses `Money`, `Price`, `ShareQuantity`, `Ticker`, `PortfolioId`, and similar types instead of raw `BigDecimal`, `int`, or `String`. New domain concepts should follow this pattern.

**Aggregate Root as Consistency Boundary.** All state changes to entities within an aggregate pass through the aggregate root. Direct manipulation of child entities from outside the aggregate is a design error.

---

## Before You Start

1. **Read the architecture.** Familiarise yourself with the project structure and the sell-stocks book (`doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md`). It traces a single use case through every layer and explains why each boundary exists.
2. **Check existing issues and discussions.** Your idea may already be in progress or previously discussed.
3. **For non-trivial changes, open an issue first.** Describe what you want to do and why. This avoids duplicate effort and gives maintainers a chance to offer guidance before you invest time.

---

## Development Setup

**Prerequisites:**

- Java 21 (or later)
- Maven 3.9+ (or use the included `mvnw` wrapper)
- Docker and Docker Compose (for the MySQL database)
- A Java IDE with Lombok support (IntelliJ IDEA recommended)

**Getting started:**

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
docker-compose up -d
./mvnw clean verify
```

The `verify` phase runs both unit tests (Surefire) and integration tests (Failsafe). All tests must pass before you submit a pull request.

---

## Contribution Workflow

1. **Fork** the repository and create a feature branch from `main`.
2. **Make your changes** in small, focused commits.
3. **Run the full test suite** — `./mvnw clean verify`. Do not submit with failing tests.
4. **Push** your branch and open a **pull request** against `main`.

Keep pull requests focused on a single concern. A PR that fixes a bug should not also refactor unrelated code or add a new feature.

---

## Specification, Tests, Code, and Documentation Alignment

HexaStock follows a specification-first engineering discipline:

> **Specification → Contract → Tests → Implementation → Documentation**

This means:

- **Behavioural changes start with Gherkin.** If you change what the system does, update or add the corresponding `.feature` file in `doc/features/` first.
- **Tests link to specifications.** Use `@SpecificationRef` annotations to trace each test back to its Gherkin scenario or API specification clause.
- **API changes start with OpenAPI.** If your change affects the REST contract, update `doc/openapi.yaml` before changing controller code.
- **Documentation reflects reality.** If your code change makes an existing document inaccurate, update the document in the same PR. Do not leave documentation out of sync.

This alignment is not bureaucracy — it is an engineering constraint that keeps the project honest. Specifications, tests, code, and documentation must tell the same story.

---

## Code Quality Expectations

- **All tests pass.** Unit tests via Surefire and integration tests via Failsafe.
- **Coverage does not regress.** The project maintains >90% line coverage (JaCoCo). New code should be tested proportionally.
- **No SonarQube issues introduced.** The project targets a Sonar AAA maintainability rating.
- **Domain logic stays in the domain.** Do not put business rules in controllers, services, or adapters.
- **Use value objects.** Do not pass raw primitives across layer boundaries where a domain type exists.
- **Follow existing code conventions.** Match the naming, formatting, and structural patterns already in the codebase. Consistency matters more than personal preference.
- **Keep it simple.** Solve the problem at hand. Do not add abstractions, configuration options, or extension points for hypothetical future requirements.

---

## Documentation Contribution Guidance

HexaStock is also a pedagogical project. The documentation is not an afterthought — it is a first-class deliverable. Contributions to the Markdown documents under `doc/` are valued as highly as code contributions.

When writing or editing documentation:

- **Write clearly and precisely.** The documents are read by engineers learning DDD and Hexagonal Architecture. Ambiguity costs them time.
- **Use the existing voice.** The project's documents are written in a declarative, professional tone — not conversational, not corporate. Match it.
- **Reference real code.** Link to actual source files, test classes, and Gherkin scenarios. Do not describe code that does not exist in the repository.
- **Keep diagrams current.** If your change affects a PlantUML diagram, update the `.puml` source and re-render the PNG/SVG outputs.
- **Internal links should be relative.** The documentation is designed to work inside GitBook. Use relative Markdown links, not absolute URLs or inline-code file paths.
- **Update the Reading Map.** If you add a new companion document under `doc/`, add a corresponding entry in the Reading Map section of the sell-stocks book.

---

## Pull Request Checklist

Before requesting review, confirm:

- [ ] All tests pass (`./mvnw clean verify`)
- [ ] New behaviour has corresponding Gherkin scenarios
- [ ] New tests include `@SpecificationRef` annotations linking to specifications
- [ ] Domain logic resides in the domain layer, not in services or adapters
- [ ] Value objects are used instead of raw primitives at domain boundaries
- [ ] No outward dependencies introduced in the domain layer
- [ ] Documentation updated to reflect any behavioural or structural changes
- [ ] Commit messages are clear and descriptive

---

## Commit and PR Guidance

**Commits:**

- Write commit messages in imperative mood: "Add FIFO lot selection test", not "Added" or "Adds".
- Keep the subject line under 72 characters. Use the body for context when needed.
- Each commit should represent a single logical change that compiles and passes tests.

**Pull requests:**

- Give the PR a clear title that describes the change, not the ticket number.
- In the PR description, explain *what* changed and *why*. If the change is architectural, explain the reasoning.
- Reference related issues with `Closes #N` or `Relates to #N`.

---

## Questions and Discussion

If you have questions about architecture, design decisions, or how to approach a contribution:

- **Open a GitHub Discussion** for general questions or design explorations.
- **Open a GitHub Issue** for concrete bugs or feature proposals.
- **Read the documentation first.** Many architectural decisions are explained in detail in the companion documents listed in the Reading Map.

---

## Final Note

HexaStock is built to be read as carefully as it is built to run. Contributions that maintain this standard — clean architecture, honest tests, precise documentation — make the project better for everyone who learns from it and everyone who builds on it.

We look forward to your contribution.
