# ADR-002: Use Spring Boot 3 as the application framework

## Status

Accepted

## Context

The project needs an application framework that provides dependency injection, web serving, data access integration, caching, testing support, and profile-based configuration. The framework must support Java 21 and allow hexagonal architecture boundaries to be respected (i.e. the domain must remain free of framework annotations).

## Decision

Use Spring Boot 3.5.0 (with Spring Framework 6.x) as the application framework. Confine Spring-specific code to adapters, the bootstrap module, and a minimal `@Transactional` dependency in the application layer.

## Alternatives considered

- **Quarkus:** Provides similar capabilities with faster startup and native compilation. Standard alternative for cloud-native Java applications. Not discussed in the repository.
- **Micronaut:** Compile-time dependency injection with low memory footprint. Standard alternative. Not discussed in the repository.
- **Plain Java (no framework):** Would achieve maximum domain purity but would require reimplementing dependency injection, web serving, transaction management, and test infrastructure.

## Consequences

**Positive:**
- Mature ecosystem with extensive documentation and community support.
- Spring profiles enable swappable adapters (`jpa`, `finhub`, `mockfinhub`).
- `@SpringBootTest` with Testcontainers provides comprehensive integration testing.
- `spring-boot-starter-*` modules simplify dependency management.
- The application layer has no compile-time coupling to Spring. Transactional demarcation uses the standard Jakarta `@Transactional` annotation; Spring's transaction infrastructure recognises it at runtime.

**Negative:**
- Contributors must understand Spring Boot conventions (profiles, auto-configuration, component scanning).
- Runtime startup is slower than compile-time frameworks such as Quarkus or Micronaut (acceptable for this project's context).

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
