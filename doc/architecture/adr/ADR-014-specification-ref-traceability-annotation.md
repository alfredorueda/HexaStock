# ADR-014: Use @SpecificationRef for specification-to-test traceability

## Status

Accepted

## Context

The project follows a specification-first workflow where Gherkin scenarios define expected behaviour before implementation. Tests verify these scenarios. Without an explicit link between scenarios and tests, it is difficult to determine which tests verify which specification, whether all scenarios have test coverage, or which scenario a failing test represents.

## Decision

Define a custom `@SpecificationRef` annotation in the domain module's test-jar that allows tests to declare which Gherkin scenario they verify:

```java
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@Repeatable(SpecificationRefs.class)
public @interface SpecificationRef {
    String value();                  // Scenario identifier (e.g. "US-07.FIFO-1")
    TestLevel level();               // DOMAIN or INTEGRATION
    String feature() default "";     // Feature file name (e.g. "sell-stocks.feature")
}
```

The annotation:
- Is `@Repeatable`, allowing a test to reference multiple scenarios.
- Lives in the `domain` module's test-jar so all modules can use it.
- Carries metadata (`level`, `feature`) enabling queries like "all DOMAIN-level tests for sell-stocks.feature".

Gherkin scenario identifiers follow the convention `US-XX.AC-N` or `US-XX.FIFO-N`, matching the user story numbering.

## Alternatives considered

- **Test names only:** Encoding scenario references in test method names (e.g. `testUS07_FIFO1()`). Fragile, hard to query, and loses structured metadata. Standard practice in many projects.
- **External traceability matrix (spreadsheet):** Decoupled from code, prone to drift. The project uses an annotation-based approach that keeps traceability in the code. Standard practice in regulated environments.
- **Cucumber JVM (step definitions):** Would directly link Gherkin steps to Java code. The project chose JUnit tests with `@SpecificationRef` annotations instead, allowing both domain-level and integration-level tests to reference the same scenario. Standard alternative.

## Consequences

**Positive:**
- Every test can declare its specification origin with structured metadata.
- The traceability chain is maintained in code, not in external documentation.
- IDE navigation: developers can search for `@SpecificationRef("US-07.FIFO-1")` to find all tests verifying that scenario.
- The same Gherkin scenario can be verified at multiple levels (DOMAIN and INTEGRATION).

**Negative:**
- Annotation compliance is voluntary - there is no automated check that all Gherkin scenarios have corresponding `@SpecificationRef` tests.
- Scenario identifiers must be manually synchronised between Gherkin files and test annotations.

## Repository evidence

- `domain/src/test/java/.../SpecificationRef.java`: annotation definition with `value`, `level`, `feature` elements
- `domain/src/test/java/.../SpecificationRefs.java`: container annotation for `@Repeatable`
- `domain/src/test/java/.../TestLevel.java`: enum `DOMAIN`, `INTEGRATION`
- `domain/pom.xml`: `maven-jar-plugin` with `test-jar` goal, making these annotations available to other modules
- `PortfolioTradingRestIntegrationTest.java`: `@SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.INTEGRATION, feature = "sell-stocks.feature")`
- `PortfolioLifecycleRestIntegrationTest.java`: Multiple `@SpecificationRef` annotations on test methods
- `PortfolioTest.java`: `@SpecificationRef(level = TestLevel.DOMAIN)` on domain tests
- `CONTRIBUTING.md`: "Every test method references the Gherkin scenario it verifies using the @SpecificationRef annotation"

## Relation to other specifications

- **Gherkin:** This ADR directly bridges code and Gherkin. The `@SpecificationRef` annotation's `value` element contains the scenario identifier from the feature file. The `feature` element names the file.
- **OpenAPI:** Not directly related. API contract verification uses conventional REST-Assured assertions.
- **PlantUML:** Not directly related. Structural verification uses ArchUnit (ADR-010).
