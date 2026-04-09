# ADR-006: Use multi-module Maven build to enforce architectural boundaries

## Status

Accepted

## Context

Hexagonal architecture requires strict dependency direction between layers. Convention-based enforcement (package naming, code reviews) is fragile and prone to drift. A build-level mechanism is needed to prevent accidental dependency violations at compile time, complementing the runtime checks provided by ArchUnit.

## Decision

Structure the project as a multi-module Maven build with six modules, each representing an architectural layer or adapter:

| Module | Role |
|--------|------|
| `domain` | Domain model. No framework dependencies. |
| `application` | Ports and application services. Depends on `domain` only. |
| `adapters-inbound-rest` | REST adapter. Depends on `application`. |
| `adapters-outbound-persistence-jpa` | JPA adapter. Depends on `application`. |
| `adapters-outbound-market` | Market adapter. Depends on `application`. |
| `bootstrap` | Composition root. Depends on all modules. |

The root POM defines shared properties, dependency management, and plugin configuration. Each module declares only its own dependencies.

Additionally, the `domain` module publishes a `test-jar` containing shared test annotations (`@SpecificationRef`, `TestLevel`) that other modules reuse.

## Alternatives considered

- **Single module with package-only separation:** Simpler build but no compile-time enforcement of dependency rules. A developer could accidentally import a persistence class in the domain. Standard alternative.
- **Gradle with feature variants or project isolation:** Gradle offers more flexible dependency configuration but Maven is the standard in the Java enterprise ecosystem and integrates naturally with Spring Boot. Standard alternative.
- **Java Platform Module System (JPMS):** Would provide compile-time encapsulation but adds complexity to Spring Boot applications and is not widely adopted in Spring projects. Standard alternative.

## Consequences

**Positive:**
- Maven enforces at compile time that `domain` cannot depend on `application` or adapters.
- Each module has a clear, documented purpose (see `<description>` tags in each `pom.xml`).
- Modules can be built, tested, and versioned independently.
- Adapter modules can be swapped or removed without affecting the core.

**Negative:**
- More POM files to maintain (7 total).
- Inter-module dependency management requires the `dependencyManagement` section in the root POM.
- Building the full project requires `mvnw install` before running individual modules (documented in README).

## Repository evidence

- `pom.xml`: `<packaging>pom</packaging>`, `<modules>` block listing 6 modules
- `domain/pom.xml`: zero dependencies on other modules or frameworks; `maven-jar-plugin` with `test-jar` execution
- `application/pom.xml`: depends only on `hexastock-domain` and `spring-tx`; `<description>` states "Application layer: input/output ports (use cases) and application services"
- `adapters-inbound-rest/pom.xml`: depends on `hexastock-application`; `<description>` states "Inbound REST adapter"
- `adapters-outbound-persistence-jpa/pom.xml`: depends on `hexastock-application`; `<description>` states "Outbound JPA persistence adapter"
- `adapters-outbound-market/pom.xml`: depends on `hexastock-application`; `<description>` states "Outbound market adapter"
- `bootstrap/pom.xml`: depends on all 5 other modules; `<description>` states "Composition root"
- `README.md`: explains the two-command build process (`mvnw install` then `spring-boot:run`)

## Relation to other specifications

- **Gherkin:** Module structure is transparent to behavioural specifications.
- **OpenAPI:** The REST adapter module is the one that implements the OpenAPI contract.
- **PlantUML:** Architecture diagrams show logical layers. Maven modules are the physical enforcement of those layers. This ADR bridges the conceptual (PlantUML) and physical (Maven) views.
