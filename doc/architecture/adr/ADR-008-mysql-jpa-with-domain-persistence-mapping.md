# ADR-008: Use MySQL with Spring Data JPA and explicit domain-persistence mapping

## Status

Accepted

## Context

The system manages financial portfolio data (portfolios, holdings, lots, transactions) that must be durably persisted. The persistence mechanism must support transactional consistency for financial operations, handle aggregate-level persistence, and maintain an anti-corruption layer that prevents JPA annotations from leaking into the domain model.

## Decision

Use MySQL 8.0.32 as the relational database, accessed via Spring Data JPA with Hibernate. Maintain a separate JPA entity model with explicit mapper classes that convert between domain objects and persistence entities.

Key implementation decisions:

- **Profile-gated activation:** The JPA repository adapter is activated only when the `jpa` Spring profile is active (`@Profile("jpa")`).
- **Separate entity model:** `PortfolioJpaEntity`, `HoldingJpaEntity`, `LotJpaEntity`, `TransactionJpaEntity` mirror domain types but carry JPA annotations.
- **Static mappers:** `PortfolioMapper`, `HoldingMapper`, `LotMapper`, `TransactionMapper` perform bidirectional mapping.
- **Aggregate-level persistence:** `cascade = ALL, orphanRemoval = true` on `@OneToMany` relationships ensures the full aggregate is saved/loaded as a unit.
- **Docker Compose for local development:** MySQL container defined in `docker-compose.yml`.

## Alternatives considered

- **PostgreSQL:** Functionally equivalent for this use case. MySQL was chosen as the target database. Standard alternative.
- **H2 (in-memory for tests):** Many Spring projects use H2 for tests. The project uses Testcontainers with real MySQL instead, for higher-fidelity testing. See ADR-009.
- **JPA annotations directly on domain entities:** Would eliminate the mapper layer but would introduce infrastructure coupling in the domain (violating ADR-007).
- **jOOQ or JDBC Template:** Would give more SQL control but loses automatic dirty-checking and cascade management that simplifies aggregate persistence.

## Consequences

**Positive:**
- Domain model remains free of JPA annotations.
- Aggregate-level `cascade = ALL` ensures consistency: saving a Portfolio also saves its Holdings and Lots.
- Profile-gated activation allows swapping the persistence implementation.
- Mapper classes serve as an anti-corruption layer, preventing persistence concerns from leaking into the domain.
- `orphanRemoval = true` ensures that depleted lots (removed during FIFO sell) are deleted from the database.

**Negative:**
- Maintaining two parallel object models (domain and JPA) creates duplication.
- Mapper code is boilerplate that must be manually updated when domain types change.
- Hibernate's merge/attach semantics can introduce subtle persistence bugs.

## Repository evidence

- `docker-compose.yml`: `image: mysql:8.0.32`, port `3307:3306`, database `hexastock_db`
- `application.properties`: `spring.datasource.url=jdbc:mysql://localhost:3307/hexastock_db`, `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect`
- `adapters-outbound-persistence-jpa/pom.xml`: `spring-boot-starter-data-jpa`, `mysql-connector-j` version `8.2.0`
- `JpaPortfolioRepository.java`: implements `PortfolioPort`, annotated `@Repository @Profile("jpa")`
- `PortfolioJpaEntity.java`: `@Entity @Table(name = "portfolios")`, `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`
- `PortfolioMapper.java`: static `toDomain()` and `toJpaEntity()` methods
- `JpaPortfolioSpringDataRepository.java`: extends `JpaRepository<PortfolioJpaEntity, String>`

## Relation to other specifications

- **Gherkin:** Gherkin scenarios describe behaviour in persistence-agnostic terms. Integration tests exercise the full persistence path, verifying that Gherkin-specified behaviour survives round-trip persistence.
- **OpenAPI:** No direct relation. The API contract is persistence-agnostic.
- **PlantUML:** Persistence class diagrams (if present under `doc/tutorial/`) show the JPA entity model. This ADR explains why a separate entity model exists.
