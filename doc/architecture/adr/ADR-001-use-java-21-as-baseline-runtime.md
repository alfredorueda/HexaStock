# ADR-001: Use Java 21 as the baseline language and runtime

## Status

Accepted

## Context

The project requires a modern, long-term-support (LTS) Java version that supports current language features (records, sealed classes, pattern matching) and aligns with the Spring Boot 3.x minimum requirement of Java 17+. The choice of Java version affects language expressiveness, runtime performance, and the available ecosystem of libraries and tools.

## Decision

Use Java 21 as the baseline language level and runtime for all modules.

## Alternatives considered

- **Java 17:** Would satisfy Spring Boot 3.x requirements but lacks newer language features and performance improvements present in Java 21. Standard alternative, not discussed in the repository.
- **Another JDK baseline:** Would require checking source compatibility, dependencies and CI together. This record describes the repository baseline, not a claim about the latest Java release or every vendor’s support policy.

## Consequences

**Positive:**
- Access to modern language features: records (used extensively for value objects), pattern matching, virtual threads (available but not currently used).
- Long-term support from major vendors ensures security patches and stability.
- Alignment with Spring Boot 3.5.0 ecosystem.

**Negative:**
- Requires contributors to have JDK 21 installed.
- Some older enterprise environments may not yet support Java 21 deployment.

## Repository evidence

- `pom.xml`: `<java.version>21</java.version>`
- `.github/workflows/build.yml`: `java-version: '21'`, `distribution: 'temurin'`
- `CONTRIBUTING.md`: JDK 21 in prerequisites
- `README.md`: JDK 21 setup
- Domain value objects implemented as Java records: `Money.java`, `Price.java`, `ShareQuantity.java`, `Ticker.java`, `PortfolioId.java`, `SellResult.java`, `HoldingPerformance.java`, `StockPrice.java`

## Relation to other specifications

- **Gherkin:** Not directly related. Gherkin specifies behaviour independently of language version.
- **OpenAPI:** Not directly related. The API contract is language-agnostic.
- **PlantUML:** Domain model diagrams reflect Java records as value objects, using records, a feature available before Java 21.

This ADR documents a technology decision that enables implementation choices visible in the source code but not expressed by behavioural or API specifications.
