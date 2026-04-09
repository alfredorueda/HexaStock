# ADR-003: Adopt Hexagonal Architecture with strict dependency inversion

## Status

Accepted

## Context

The project models a financial domain with multiple external dependencies (REST API, relational database, external stock price APIs). Business rules must remain isolated from infrastructure concerns to enable independent testing, adapter interchangeability, and long-term evolvability. A structural approach is needed to enforce this isolation at build time and test time, not just by convention.

## Decision

Adopt Hexagonal Architecture (Ports and Adapters) as the primary architectural style. Enforce the dependency rule at three levels:

1. **Maven modules** - separate modules for domain, application, inbound adapters, outbound adapters, and bootstrap.
2. **Package conventions** - `..model..` for domain, `..application..` for application, `..adapter.in..` and `..adapter.out..` for adapters.
3. **ArchUnit fitness tests** - automated tests that fail the build if dependency rules are violated.

Dependencies flow strictly inward: adapters depend on ports, ports are defined by the application layer, the application layer depends on the domain, and the domain depends on nothing external.

## Alternatives considered

- **Layered architecture (traditional 3-tier):** Simpler to set up but allows infrastructure leakage into business logic. Does not enforce inward dependency direction. Standard alternative.
- **Clean Architecture (Uncle Bob):** Similar intent with concentric circles. The difference is largely terminological; the project uses Hexagonal Architecture terminology (ports, adapters, driving, driven). Standard alternative.
- **Modular monolith without explicit ports:** Would still benefit from module separation but without the formal port/adapter contract. Loses the ability to swap adapters cleanly.

## Consequences

**Positive:**
- Domain model is fully portable: zero framework dependencies, testable with plain JUnit.
- Adapters are interchangeable: multiple stock price providers (Finnhub, AlphaVantage, mock) implement the same port.
- Architectural rules are enforced automatically: ArchUnit tests prevent regressions.
- Module boundaries make dependency violations visible at build time (Maven will fail if a module declares an undeclared dependency).

**Negative:**
- More modules and more files than a flat structure.
- Mapping between domain objects and adapter objects (DTOs, JPA entities) requires explicit mappers.
- Contributors must understand the architectural constraints before contributing.

## Repository evidence

- Root `pom.xml`: 6 modules (`domain`, `application`, `adapters-inbound-rest`, `adapters-outbound-persistence-jpa`, `adapters-outbound-market`, `bootstrap`)
- `domain/pom.xml`: no dependencies on Spring, JPA, or any adapter module
- `application/pom.xml`: depends only on `hexastock-domain` (plus minimal `spring-tx`)
- `HexagonalArchitectureTest.java`: 6 ArchUnit rules enforcing:
  - Domain does not depend on application, adapters, or Spring
  - Application does not depend on adapters
  - Inbound adapters do not depend on outbound adapters (and vice versa)
- `CONTRIBUTING.md`: "The Inward Dependency Rule. Adapters depend on ports. Ports are defined by the core. The domain depends on nothing external. Violating this rule - even for convenience - is not acceptable."
- Input ports: `PortfolioManagementUseCase`, `PortfolioStockOperationsUseCase`, `ReportingUseCase`, `GetStockPriceUseCase`, `TransactionUseCase`
- Output ports: `PortfolioPort`, `TransactionPort`, `StockPriceProviderPort`
- `SpringAppConfig.java`: explicit wiring of services to ports (composition root pattern)

## Relation to other specifications

- **Gherkin:** Behavioural specifications are independent of architecture. However, the architectural separation enables domain-level tests and integration-level tests to verify the same Gherkin scenario at different layers.
- **OpenAPI:** The API contract is implemented by the inbound REST adapter, which is explicitly separated from the domain by the port boundary.
- **PlantUML:** Architecture diagrams under `doc/tutorial/sellStocks/diagrams/` (e.g. `hexastock-hexagonal-architecture.puml`) visualise the same port-and-adapter structure this ADR formalises. The ADR documents the decision and enforcement mechanism; PlantUML visualises the result.
