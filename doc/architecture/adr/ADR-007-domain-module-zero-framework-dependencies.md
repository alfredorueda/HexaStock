# ADR-007: Keep domain production code free of frameworks

## Status

Accepted; dependency scope clarified against `main`.

## Context

A FIFO rule should be testable without a web server or database. Importing JPA or Spring into the domain would couple that rule to an infrastructure choice.

## Decision

Domain **production code** uses JDK types and domain types, with no Spring, JPA or validation-framework dependency. Constructor and operation checks express domain constraints. Transaction demarcation belongs to application services, using Jakarta Transactions.

This does not mean that the module has no dependencies of any kind. Its effective Maven configuration inherits `spring-boot-starter-test` in test scope from the parent. JUnit tests and the shared test-jar are intentionally outside the production dependency policy.

## Alternatives considered

- Put persistence or validation annotations on domain classes: fewer mappings, but infrastructure coupling in business types.
- Use Lombok: less handwritten code, but an additional build dependency. The model uses records where appropriate.
- Put transaction annotations on aggregate methods: would make infrastructure part of a rule that can run independently in memory.

## Consequences

Domain tests can construct ordinary objects and run without Spring or Docker. Explicit validation and persistence mappers require maintenance. This policy does not establish complete validation: each constructor and operation must still be reviewed.

Maven controls the resolved classpath; it is not an automatic prohibition on future dependency edits. ArchUnit checks domain dependencies on application, adapter and Spring packages. It does not scan every possible external library or guarantee the absence of reflection-based coupling.

## Repository evidence

- Root `pom.xml`: shared test-scope starter.
- `domain/pom.xml`: module configuration and test-jar publication.
- `HexagonalArchitectureTest.java`: three domain dependency rules.
- `Money`, `Price`, `Ticker`, `ShareQuantity`: explicit validation with different permitted values.

## Relation to other specifications

[Domain-only lessons](../../tutorial/sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md) demonstrate the practical benefit. [Testing scopes](../../tutorial/TESTING-STRATEGY.md) explain what an isolated test proves and what still requires integration tests.
