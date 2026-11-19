# Testing HexaStock: From One Rule to the Running Application

**Goal:** choose the smallest useful test for a question, then add evidence at the boundaries it cannot exercise.

**Prerequisites:** basic JUnit assertions and the [sale example](sellStocks/SELL-STOCK-01-BEHAVIOUR.md).

## Start with one question

If 12 shares are sold from lots of 10 at 100.00 and 5 at 120.00, is the cost basis 1240.00? A domain test can answer this without starting a database or web server:

```bash
./mvnw -pl domain test
```

That test cannot tell us whether JSON fields are correct, the database mapper preserves lot order, or Spring selects the intended adapter. Those are different questions for other tests.

## A three-session learning path

| Session | Focus | Checkpoint |
|---|---|---|
| [1. Test rules and orchestration](testing/01-RULES-AND-ORCHESTRATION.md) | Domain, application and HTTP boundaries | Distinguish a business assertion from an interaction assertion. |
| [2. Test adapters deterministically](testing/02-ADAPTER-CONTRACTS.md) | WireMock, Testcontainers and shared port contracts | Explain which external behavior is real and which is controlled. |
| [3. Verify the assembled system](testing/03-INTEGRATION-AND-ARCHITECTURE.md) | Integration, architecture and traceability | State what a passing check actually demonstrates. |

Allow about 10 minutes per session, plus time to run the examples. The sessions refer to existing test classes instead of maintaining duplicate copies of their entire setup.

## The test map

| Boundary | Typical tools | Main evidence |
|---|---|---|
| Domain | JUnit, AssertJ | Arithmetic, state changes and invalid operations. |
| Application | JUnit, Mockito | Coordination of domain objects and ports. |
| Inbound REST | `@WebMvcTest`, MockMvc | Input conversion, response JSON and error translation. |
| Market adapter | WireMock | Requests and parsing against controlled HTTP responses. |
| Persistence adapters | Mapper tests, Testcontainers | Representation round trips and selected database behavior. |
| Assembled application | `@SpringBootTest`, RestAssured | Wiring and complete request flows in a configured environment. |
| Architecture | ArchUnit | Seven explicit package dependency rules. |

These scopes overlap deliberately. An integration test may detect an arithmetic error too; a domain test usually makes that error easier to isolate. No layer has exclusive ownership of every defect category.

## Run a focused test or the complete build

Use JDK 21. For modules that depend on siblings, install the reactor artifacts first:

```bash
./mvnw install -DskipTests
./mvnw -pl application test
./mvnw -pl adapters-inbound-rest test
./mvnw -pl adapters-outbound-market test
```

For database-backed tests, start Docker. Testcontainers creates the databases; separate Compose startup is unnecessary.

```bash
./mvnw -pl adapters-outbound-persistence-jpa test
./mvnw -pl adapters-outbound-persistence-mongodb test
./mvnw -pl bootstrap test

# Full verification, as used by CI
./mvnw clean verify
```

Surefire runs names matching `*Test` and `*Tests`, including `*IntegrationTest`. Failsafe is configured for `*IT`. The Maven phase name alone does not describe a test's infrastructure needs.

Reports are produced under each module's `target/surefire-reports` or `target/failsafe-reports`. The aggregate JaCoCo report is under `bootstrap/target/site/jacoco-aggregate` after verification. Obtain current counts from those reports: test-method counts, parameterized invocations and nested classes are different measures.

## References

### Foundational Works

- Beck, Kent. *Test-Driven Development: By Example.* Addison-Wesley, 2003.
- Cockburn, Alistair. \"Hexagonal Architecture.\" 2005. https://alistair.cockburn.us/hexagonal-architecture/
- Cohn, Mike. *Succeeding with Agile: Software Development Using Scrum.* Addison-Wesley, 2009. (The test pyramid.)
- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003.
- Fowler, Martin. \"TestPyramid.\" *martinfowler.com*, 2012. https://martinfowler.com/bliki/TestPyramid.html
- Fowler, Martin. \"TestDouble.\" *martinfowler.com*, 2006. https://martinfowler.com/bliki/TestDouble.html
- Meszaros, Gerard. *xUnit Test Patterns: Refactoring Test Code.* Addison-Wesley, 2007.
- North, Dan. \"Introducing BDD.\" *Better Software*, March 2006. https://dannorth.net/introducing-bdd/
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013.

### Testing Framework Documentation

- **JUnit 5:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito:** https://site.mockito.org/
- **AssertJ:** https://assertj.github.io/doc/
- **Testcontainers:** https://testcontainers.com/
- **WireMock:** https://wiremock.org/docs/
- **ArchUnit:** https://www.archunit.org/userguide/html/000_Index.html
- **REST Assured:** https://rest-assured.io/

### Spring Testing Documentation

- **`@WebMvcTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests
- **`@DataJpaTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa
- **`@SpringBootTest`:** https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html

### Project Documents

- [Sell Stock Tutorial](sellStocks/SELL-STOCK-TUTORIAL.md) — end-to-end use case traced through every code layer
- [Tutorial README — Traceability Chain](README.md) — `@SpecificationRef` architecture and identifier conventions
- [Stock Portfolio API Specification](../stock-portfolio-api-specification.md) — all 10 use cases with acceptance criteria
- [Gherkin Feature Files](../features/) — 15 `.feature` files describing behavior; executed indirectly through separately authored JUnit tests
- [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](CONCURRENCY-PESSIMISTIC-LOCKING.md) — race-condition demonstrations with real tests

---

*This document is part of the [HexaStock documentation ecosystem](sellStocks/SELL-STOCK-REFERENCE.md).*
