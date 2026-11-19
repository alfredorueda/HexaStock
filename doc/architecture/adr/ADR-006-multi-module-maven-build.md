# ADR-006: Use multi-module Maven build to enforce architectural boundaries

## Status

Accepted

## Context

Hexagonal architecture requires strict dependency direction between layers. Convention-based enforcement (package naming, code reviews) is fragile and prone to drift. A build-level mechanism is needed to prevent accidental dependency violations at compile time, complementing the static dependency checks executed by ArchUnit tests.

## Decision

Structure the project as a multi-module Maven build with seven child modules, each representing an architectural layer or adapter:

| Module | Role |
|--------|------|
| `domain` | Domain model. No framework dependencies. |
| `application` | Ports and services; production dependencies include `domain` and Jakarta Transactions. |
| `adapters-inbound-rest` | REST adapter. Depends on `application`. |
| `adapters-outbound-persistence-jpa` | JPA adapter. Depends on `application`. |
| `adapters-outbound-persistence-mongodb` | MongoDB adapter. Depends on `application`. |
| `adapters-outbound-market` | Market adapter. Depends on `application`. |
| `bootstrap` | Composition root. Depends on all modules. |

The root POM defines shared properties, dependency management, and plugin configuration. Children also inherit dependencies declared in the parent, including the test starter.

Additionally, the `domain` module publishes a `test-jar` containing shared test annotations (`@SpecificationRef`, `TestLevel`) that other modules reuse.

## Alternatives considered

- **Single module with package-only separation:** Simpler build but no compile-time enforcement of dependency rules. A developer could accidentally import a persistence class in the domain. Standard alternative.
- **Gradle with feature variants or project isolation:** Gradle offers more flexible dependency configuration but Maven is the standard in the Java enterprise ecosystem and integrates naturally with Spring Boot. Standard alternative.
- **Java Platform Module System (JPMS):** Would provide compile-time encapsulation but adds complexity to Spring Boot applications and is not widely adopted in Spring projects. Standard alternative.

## Consequences

**Positive:**
- The declared classpath prevents domain source from importing unavailable application or adapter types. Changing dependency declarations can weaken that boundary; review and ArchUnit complement it.
- Each module has a clear, documented purpose (see `<description>` tags in each `pom.xml`).
- Modules can be targeted with Maven selectors. This repository versions them together in one reactor.
- Adapter modules can be swapped or removed without affecting the core.

**Negative:**
- More POM files to maintain (8 total: parent plus seven children).
- Inter-module dependency management requires the `dependencyManagement` section in the root POM.
- Running a module outside the reactor may require `mvnw install` for sibling artefacts first (documented in README).

## Repository evidence

- `pom.xml`: `<packaging>pom</packaging>`, `<modules>` block listing 7 modules
- `domain/pom.xml`: no production framework dependencies; test dependencies inherited from the parent; `maven-jar-plugin` with `test-jar` execution
- `application/pom.xml`: depends only on `hexastock-domain` and standard `jakarta.transaction-api`; `<description>` states "Application layer: input/output ports (use cases) and application services"
- `adapters-inbound-rest/pom.xml`: depends on `hexastock-application`; `<description>` states "Inbound REST adapter"
- `adapters-outbound-persistence-jpa/pom.xml`: depends on `hexastock-application`; `<description>` states "Outbound JPA persistence adapter"
- `adapters-outbound-market/pom.xml`: depends on `hexastock-application`; `<description>` states "Outbound market adapter"
- `bootstrap/pom.xml`: depends on all 6 other modules; `<description>` states "Composition root"
- `README.md`: explains the two-command build process (`mvnw install` then `spring-boot:run`)

## Relation to other specifications

- **Gherkin:** Module structure is transparent to behavioural specifications.
- **OpenAPI:** The REST adapter module is the one that implements the OpenAPI contract.
- **PlantUML:** Architecture diagrams show logical layers. Maven modules are the physical enforcement of those layers. This ADR bridges the conceptual (PlantUML) and physical (Maven) views.
