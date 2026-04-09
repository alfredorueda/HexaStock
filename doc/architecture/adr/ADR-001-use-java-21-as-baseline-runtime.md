# ADR-001: Use Java 21 as the baseline language and runtime

## Status

Accepted

## Context

The project requires a modern, long-term-support (LTS) Java version that supports current language features (records, sealed classes, pattern matching) and aligns with the Spring Boot 3.x minimum requirement of Java 17+. The choice of Java version affects language expressiveness, runtime performance, and the available ecosystem of libraries and tools.

## Decision

Use Java 21 as the baseline language level and runtime for all modules.

## Alternatives considered

- **Java 17 (previous LTS):** Would satisfy Spring Boot 3.x requirements but lacks newer language features and performance improvements present in Java 21. Standard alternative, not discussed in the repository.
- **Java 23+ (latest non-LTS):** Would provide the newest features but lacks long-term support guarantees required for stable educational and production use. Standard alternative.

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
- `CONTRIBUTING.md`: "Java 21 (or later)" in prerequisites
- `README.md`: "JDK 21 or higher" in prerequisites
- Domain value objects implemented as Java records: `Money.java`, `Price.java`, `ShareQuantity.java`, `Ticker.java`, `PortfolioId.java`, `SellResult.java`, `HoldingPerformance.java`, `StockPrice.java`

## Relation to other specifications

- **Gherkin:** Not directly related. Gherkin specifies behaviour independently of language version.
- **OpenAPI:** Not directly related. The API contract is language-agnostic.
- **PlantUML:** Domain model diagrams reflect Java records as value objects, which is a Java 21 capability.

This ADR documents a technology decision that enables implementation choices visible in the source code but not expressed by behavioural or API specifications.
