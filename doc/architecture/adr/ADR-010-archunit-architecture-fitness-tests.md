# ADR-010: Enforce architecture rules with ArchUnit fitness tests

## Status

Accepted

## Context

Hexagonal architecture dependency rules (ADR-003) must be continuously enforced, not just at code review time. Without automated enforcement, architectural erosion occurs as developers add convenient but rule-violating imports. The project needs a mechanism that fails the build when architectural constraints are violated.

## Decision

Use ArchUnit to express and enforce architectural rules as JUnit tests. These tests run as part of the standard `mvn verify` cycle and fail the build on any violation.

Seven rules are currently enforced, organised into three groups:

**Domain independence:**
1. Domain does not depend on Application
2. Domain does not depend on Adapters
3. Domain does not depend on Spring

**Application independence:**
4. Application does not depend on Adapters
5. Application does not depend on Spring

**Adapter isolation:**
6. Inbound adapters do not depend on outbound adapters
7. Outbound adapters do not depend on inbound adapters

All rules operate on package patterns matching the project's naming convention (e.g. `..model..` for domain, `..adapter.in..` for inbound).

## Alternatives considered

- **Code review only:** Human-mediated enforcement that does not scale and is error-prone. Insufficient for a team project. Standard practice, but not sufficient alone.
- **Java Platform Module System (JPMS):** Provides compile-time and runtime encapsulation. More powerful but harder to configure with Spring Boot, which relies on classpath scanning. The Maven multi-module structure provides build-level separation; ArchUnit adds package-level static analysis. Standard alternative.
- **Custom Maven Enforcer rules:** Would provide build-time checks but requires writing custom plugins. ArchUnit provides a more expressive, JUnit-integrated DSL. Standard alternative.

## Consequences

**Positive:**
- Architectural violations are detected immediately and automatically in every build.
- Rules are expressed alongside other tests, making them visible and maintainable.
- The JUnit-based API is familiar to developers.
- New rules can be added incrementally (e.g. "no `System.out` in production code", "all entities must have a no-arg constructor").

**Negative:**
- ArchUnit adds a test-scope dependency.
- Rules must be updated if package naming conventions change.
- Static analysis cannot catch all architectural violations (e.g. runtime reflection). These seven rules also do not prohibit dependencies between every pair of outbound adapters.

## Repository evidence

- `bootstrap/pom.xml`: `com.tngtech.archunit:archunit-junit5` version `1.3.0` (test scope)
- `HexagonalArchitectureTest.java`: Full test class with:
  - `@Nested class DomainLayer` (3 tests)
  - `@Nested class ApplicationLayer` (2 tests)
  - `@Nested class AdapterIsolation` (2 tests)
  - Class import scoped to `cat.gencat.agaur.hexastock`
- `CONTRIBUTING.md`: References architecture tests as a mandatory quality gate

## Relation to other specifications

- **Gherkin:** Not directly related. ArchUnit tests enforce structural rules, not behavioural ones.
- **OpenAPI:** Not directly related.
- **PlantUML:** Architecture diagrams visualise the same dependency structure that ArchUnit tests enforce. The tests check the implemented rules against imported bytecode. They do not parse diagrams or enforce every dependency a diagram might suggest. The ADR and PlantUML diagrams express the same intent through different mechanisms.
