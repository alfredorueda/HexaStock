# ADR-009: Use Testcontainers for integration tests instead of H2

## Status

Accepted

## Context

Integration tests must verify that the full application stack works correctly, including database interactions. Using an in-memory database (H2) for tests diverges from the production database (MySQL) in SQL dialect, type system, locking behaviour, and constraint handling. This can lead to tests that pass with H2 but fail against MySQL - or worse, tests that miss bugs that only manifest with MySQL.

## Decision

Use Testcontainers to run integration tests against a real MySQL 8.0.32 instance (matching the production database version). Connect via the Testcontainers JDBC URL convention:

```
jdbc:tc:mysql:8.0.32:///testdb
```

This URL triggers automatic container lifecycle management: Testcontainers starts a MySQL container before the test suite and tears it down afterward.

Additionally, use `spring.jpa.hibernate.ddl-auto=create` (not `create-drop`) in test properties to avoid schema conflicts when multiple Spring contexts share the same container instance in a test run.

## Alternatives considered

- **H2 in MySQL compatibility mode:** Cheaper to run (no Docker required) but imperfect MySQL compatibility. Differences in stored procedure support, character sets, locking, and edge-case SQL behaviour reduce test fidelity. Standard alternative.
- **Embedded MySQL (e.g. MariaDB4j):** Provides closer compatibility than H2 but is not identical to MySQL 8.0.32. Testcontainers offers the exact target version. Standard alternative.
- **Shared MySQL test server:** Would provide the correct database but introduces external dependency, mutable state, and test isolation issues. Avoided.

## Consequences

**Positive:**
- Tests run against the exact same MySQL version used in production/development.
- SQL dialect, type coercion, locking behaviour, and constraint enforcement are tested faithfully.
- No need for H2-specific workarounds or compatibility mode configuration.
- Container lifecycle is automatic and transparent to test code.

**Negative:**
- Requires Docker to be available on the developer machine and CI server.
- Container startup adds ~5-10 seconds to test suite execution.
- CI pipeline must have Docker access (verified in `.github/workflows/build.yml` with Docker diagnostics step).

## Repository evidence

- `bootstrap/pom.xml`: `org.testcontainers:testcontainers` (BOM version `1.21.4`), `org.testcontainers:mysql`, `org.testcontainers:junit-jupiter`
- `pom.xml` (root): `org.testcontainers:testcontainers-bom` version `1.21.4` in `<dependencyManagement>`
- `application-test.properties`: `spring.datasource.url=jdbc:tc:mysql:8.0.32:///testdb` with detailed comment explaining the `create` vs `create-drop` choice
- `AbstractPortfolioRestIntegrationTest.java`: `@Testcontainers`, `@ActiveProfiles({"test", "jpa", "mockfinhub"})`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `.github/workflows/build.yml`: Docker diagnostics step (`docker version`, `docker info`) verifying container runtime availability
- `README.md`: Docker listed as prerequisite
- `CI_SETUP.md`: Notes on Docker availability in GitHub Actions runners

## Relation to other specifications

- **Gherkin:** Gherkin scenarios are verified through integration tests that use Testcontainers. This ADR ensures the tests exercise the real database, giving Gherkin scenarios high-fidelity verification.
- **OpenAPI:** Integration tests validate the API contract endpoints against the real database.
- **PlantUML:** Not directly related.
