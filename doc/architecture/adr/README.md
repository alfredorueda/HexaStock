# Architecture Decision Records (ADRs) - Index

This directory contains the Architecture Decision Records for the HexaStock project. ADRs capture significant architectural and technological decisions with their context, rationale, alternatives, and consequences.

## What is an ADR?

An Architecture Decision Record is a short document that captures a single architectural decision. Each ADR describes the context that led to the decision, the decision itself, what alternatives were considered, and what consequences follow. ADRs are numbered sequentially and are never deleted - superseded decisions are marked with a new status.

## How ADRs relate to other specifications

The HexaStock project uses multiple specification artefacts that complement each other:

| Specification | What it defines | Example |
|--------------|----------------|---------|
| **Gherkin features** (`doc/features/`) | *What* the system does (behaviour) | "Shares are consumed from lots in FIFO order" |
| **OpenAPI contract** (`doc/openapi.yaml`) | *What* the API looks like (contract) | `POST /api/portfolios/{id}/sell` request/response schema |
| **PlantUML diagrams** (`doc/tutorial/*/diagrams/`) | *How* the structure looks (visualisation) | Domain model class diagram, hexagonal architecture layers |
| **ADRs** (this directory) | *Why* a decision was made (rationale) | Why FIFO over LIFO? Why pessimistic locking over optimistic? |
| **Master specification** (`doc/architecture/TECHNICAL-ARCHITECTURE-SPECIFICATION.md`) | *How* everything fits together (synthesis) | Technology stack, testing strategy, traceability matrix |

ADRs do not duplicate content from Gherkin, OpenAPI, or PlantUML. Instead, each ADR includes a "Relation to other specifications" section explaining how it connects to these artefacts.

## ADR index

| # | Title | Status | Summary |
|---|-------|--------|---------|
| [ADR-001](ADR-001-use-java-21-as-baseline-runtime.md) | Use Java 21 as the baseline runtime | Accepted | Java 21 LTS for modern language features (records, pattern matching) and Spring Boot 3.x compatibility |
| [ADR-002](ADR-002-use-spring-boot-3-as-application-framework.md) | Use Spring Boot 3 as the application framework | Accepted | Spring Boot 3.5.0 for DI, web, JPA, caching, testing, and profiles; Spring-specific code confined to adapters and bootstrap |
| [ADR-003](ADR-003-adopt-hexagonal-architecture.md) | Adopt Hexagonal Architecture | Accepted | Ports and Adapters pattern enforced by Maven modules, package conventions, and ArchUnit fitness tests |
| [ADR-004](ADR-004-rich-domain-model-with-ddd.md) | Rich domain model with DDD | Accepted | DDD tactical patterns with aggregate root (Portfolio), entities (Holding, Lot), value objects (Money, Ticker, etc.), and domain services |
| [ADR-005](ADR-005-rest-api-with-rfc7807-errors.md) | REST API with RFC 7807 errors | Accepted | REST primary adapter with separate DTO layer and RFC 7807 ProblemDetail for structured error responses |
| [ADR-006](ADR-006-multi-module-maven-build.md) | Multi-module Maven build | Accepted | Six Maven modules encoding architectural boundaries with compile-time dependency enforcement |
| [ADR-007](ADR-007-domain-module-zero-framework-dependencies.md) | Domain module zero framework dependencies | Accepted | Domain module has no external dependencies — no Spring, no JPA, no third-party libraries |
| [ADR-008](ADR-008-mysql-jpa-with-domain-persistence-mapping.md) | MySQL/JPA with domain-persistence mapping | Accepted | MySQL 8.0.32 via Spring Data JPA with separate JPA entity model and explicit static mappers |
| [ADR-009](ADR-009-testcontainers-for-integration-tests.md) | Testcontainers for integration tests | Accepted | Real MySQL 8.0.32 in Testcontainers instead of H2, for high-fidelity integration testing |
| [ADR-010](ADR-010-archunit-architecture-fitness-tests.md) | ArchUnit architecture fitness tests | Accepted | Six ArchUnit rules enforcing hexagonal dependency direction as part of the build |
| [ADR-011](ADR-011-fifo-lot-accounting-for-stock-sales.md) | FIFO lot accounting for stock sales | Accepted | First-In-First-Out lot consumption for sell operations, implemented as a domain rule in Holding.sell() |
| [ADR-012](ADR-012-pessimistic-locking-for-aggregate-consistency.md) | Pessimistic locking for aggregate consistency | Accepted | PESSIMISTIC_WRITE lock on portfolio reads-for-update to prevent lost updates in concurrent scenarios |
| [ADR-013](ADR-013-cache-stock-prices-with-caffeine.md) | Cache stock prices with Caffeine | Accepted | Local Caffeine cache with 5-minute TTL and 1,000 entry limit for external stock price API responses |
| [ADR-014](ADR-014-specification-ref-traceability-annotation.md) | @SpecificationRef traceability annotation | Accepted | Custom repeatable annotation linking JUnit tests to Gherkin scenario identifiers at DOMAIN and INTEGRATION levels |
| [ADR-015](ADR-015-explicit-bean-wiring-via-configuration.md) | Explicit bean wiring via @Configuration | Accepted | Application services wired via @Bean methods in SpringAppConfig, keeping services free of @Service annotations |

## ADR lifecycle

- **Accepted:** Decision is current and in effect.
- **Superseded:** Decision has been replaced by a newer ADR (the superseding ADR is linked).
- **Deprecated:** Decision is no longer relevant due to scope change.
- **Proposed:** Decision is under discussion and not yet accepted.

All current ADRs have status **Accepted**.

## Potential future ADRs

The following areas are identified as implicit decisions that could benefit from formal ADRs (see Section 13 of the master specification):

- Authentication and authorisation strategy
- API versioning strategy
- Database schema migration approach (Flyway/Liquibase)
- Settlement mechanics (T+2 rules, lot reservation)
- Resilience patterns for external API calls (circuit breaker, retry)
- Deployment strategy (containerisation, orchestration)
- Domain events and event-driven decoupling

## Contributing

When adding a new ADR:

1. Use the next sequential number.
2. Follow the existing template: Status, Context, Decision, Alternatives considered, Consequences, Repository evidence, Relation to other specifications.
3. Every statement must be grounded in repository evidence (file paths, code snippets, configuration).
4. Update this index.
5. If the ADR supersedes an existing one, update the status of both.
