# ADR-007: Keep the domain module free of all framework dependencies

## Status

Accepted

## Context

In hexagonal architecture, the domain layer should be the most stable and reusable part of the system. Framework dependencies (Spring annotations, JPA annotations, validation frameworks) create coupling that reduces portability, complicates testing, and blurs the boundary between domain logic and infrastructure concerns.

The project needs a clear policy on what dependencies are permitted in the domain module.

## Decision

The `domain` module has **zero external dependencies**. No Spring, no JPA, no Hibernate annotations, no Jakarta validation, no third-party libraries. The only dependency is the JDK itself.

This is enforced at three levels:

1. **`domain/pom.xml`** declares no dependencies (only the parent POM for module coordinates).
2. **ArchUnit test** `domainDoesNotDependOnSpring()` verifies that domain classes do not reference Spring packages.
3. **Maven module boundary** prevents any transitive framework dependency from reaching the domain.

## Alternatives considered

- **Allow `@Transactional` in the domain:** Some DDD practitioners place transaction boundaries on aggregate root methods. The project explicitly chose to keep `@Transactional` in the application layer instead. The `application/pom.xml` contains a comment justifying this: "minimal Spring dependency justified to preserve existing behavior".
- **Allow Jakarta Validation annotations (`@NotNull`, `@Pattern`) in domain types:** Would provide declarative validation but introduces a framework dependency. The project uses constructor-based self-validation instead (e.g. `Ticker` validates against a regex in its constructor).
- **Allow Lombok in the domain:** Would reduce boilerplate but adds a compile-time dependency and hides generated code. The project uses Java records instead.

## Consequences

**Positive:**
- Domain classes are testable with plain JUnit, no Spring context needed.
- Domain model is portable across frameworks.
- The zero-dependency policy is a strong, unambiguous rule that is easy to verify and communicate.
- Value objects as Java records provide the conciseness that Lombok would offer, without the dependency.

**Negative:**
- Self-validating constructors require manual null-checks and assertions.
- No declarative validation (e.g. JSR 380 annotations) in the domain.
- No use of `@Transactional` in the domain, requiring transaction boundaries to be managed at the application layer.

## Repository evidence

- `domain/pom.xml`: no `<dependencies>` section (only parent POM coordinates)
- `HexagonalArchitectureTest.java`:
  - `domainDoesNotDependOnSpring()`: `noClasses().that().resideInAPackage("..model..").should().dependOnClassesThat().resideInAPackage("org.springframework..")`
  - `domainDoesNotDependOnApplication()`: domain packages must not reference application packages
  - `domainDoesNotDependOnAdapters()`: domain packages must not reference adapter packages
- `Money.java`: manual null-check in compact constructor, `Objects.requireNonNull(amount, "amount cannot be null")`
- `Ticker.java`: regex validation in constructor, `if (!symbol.matches(PATTERN)) throw new InvalidTickerException(...)`
- `CONTRIBUTING.md`: "The domain depends on nothing external. Violating this rule - even for convenience - is not acceptable."

## Relation to other specifications

- **Gherkin:** This decision enables domain-level tests annotated with `@SpecificationRef(level = TestLevel.DOMAIN)` that verify Gherkin scenarios without any infrastructure.
- **OpenAPI:** No relation. The API layer is in a separate module.
- **PlantUML:** Domain model diagrams show pure domain classes. This ADR guarantees that those classes will never acquire framework annotations that would complicate the diagrams.
