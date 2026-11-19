# ADR-002: Use Spring Boot 3 as the application framework

## Status

Accepted

## Context

The project needs an application framework that provides dependency injection, web serving, data access integration, caching, testing support, and profile-based configuration. The framework must support Java 21 and allow hexagonal architecture boundaries to be respected (i.e. the domain must remain free of framework annotations).

## Decision

Use Spring Boot 3.5.0 (with Spring Framework 6.x) as the application framework. Confine Spring-specific production code to adapters and bootstrap. The application module uses Jakarta `@Transactional`, a separate standard API interpreted by Spring at runtime.

## Alternatives considered

- **Quarkus:** Offers an alternative runtime and native compilation model. Standard alternative for cloud-native Java applications. Not discussed in the repository.
- **Micronaut:** Compile-time dependency injection. Standard alternative. Not discussed in the repository.
- **Plain Java (no framework):** Would achieve maximum domain purity but would require selecting and integrating those facilities separately.

## Consequences

**Positive:**
- Mature ecosystem with extensive documentation and community support.
- Spring profiles enable swappable adapters (`jpa` or `mongodb`, and `finhub`, `alphaVantage` or `mockfinhub`).
- `@SpringBootTest` with Testcontainers provides comprehensive integration testing.
- `spring-boot-starter-*` modules simplify dependency management.
- The application layer has no compile-time coupling to Spring. Transactional demarcation uses the standard Jakarta `@Transactional` annotation; Spring's transaction infrastructure recognises it at runtime.

**Negative:**
- Contributors must understand Spring Boot conventions (profiles, auto-configuration, component scanning).
- Startup and memory costs must be measured on the intended workload; this repository does not establish a comparative benchmark.

## Repository evidence

- `pom.xml`: `spring-boot-starter-parent` version `3.5.0`
- `adapters-inbound-rest/pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-validation`
- `adapters-outbound-persistence-jpa/pom.xml`: `spring-boot-starter-data-jpa`
- `adapters-outbound-market/pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-cache`
- `bootstrap/pom.xml`: `spring-boot-starter`, `spring-boot-maven-plugin`
- `HexaStockApplication.java`: `@SpringBootApplication(scanBasePackages = "cat.gencat.agaur.hexastock")`
- `application/pom.xml`: `jakarta.transaction-api` dependency (standard API for `@Transactional`; zero Spring compile-time dependencies)
- `AbstractPortfolioRestIntegrationTest.java`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`

## Relation to other specifications

- **Gherkin:** Framework choice is invisible to behavioural specifications. Gherkin scenarios describe system behaviour without referencing Spring.
- **OpenAPI:** The API contract is framework-agnostic. Spring MVC implements the contract defined in `openapi.yaml`.
- **PlantUML:** Architecture diagrams show adapters and ports; the framework is the mechanism that wires them, but the diagrams focus on logical boundaries.

This ADR documents the foundational framework decision that enables the port/adapter wiring visible in `SpringAppConfig.java` and the profile-based adapter activation visible in `@Profile("jpa")`.
