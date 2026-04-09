# ADR-004: Model the core business using Domain-Driven Design with a rich domain model

## Status

Accepted

## Context

The financial portfolio domain has non-trivial business rules: FIFO lot accounting, insufficient funds validation, positive quantity enforcement, cost basis calculation, and aggregate consistency between portfolios, holdings, and lots. These rules must be protected from scattering across services and adapters. A modelling approach is needed that keeps business logic co-located with the data it governs.

## Decision

Apply Domain-Driven Design (DDD) tactical patterns with a rich domain model:

- **Aggregate root:** `Portfolio` is the sole entry point for all state changes within its boundary.
- **Entities:** `Holding` and `Lot` have identity and lifecycle within their aggregate.
- **Value objects:** `Money`, `Price`, `ShareQuantity`, `Ticker`, `PortfolioId`, `HoldingId`, `LotId`, `SellResult`, `HoldingPerformance`, `StockPrice` are immutable, self-validating types.
- **Domain service:** `HoldingPerformanceCalculator` encapsulates computation that spans aggregate and transaction data.
- **Domain exceptions:** Typed exceptions (`InsufficientFundsException`, `InvalidAmountException`, etc.) express business rule violations.
- **Application services:** Orchestrate use cases by coordinating ports and delegating to the domain; they do not contain business logic.

## Alternatives considered

- **Anemic domain model:** Entities as data carriers with business logic in application services. The project explicitly documents and rejects this approach in `doc/tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md`, which provides a side-by-side comparison.
- **Transaction Script:** Procedural services operating on raw data. Simpler for trivial domains but leads to scattered business rules in complex domains. Standard alternative.
- **Event-sourced model:** Store domain events rather than current state. Would support auditability but adds complexity. Not discussed as an alternative in the repository.

## Consequences

**Positive:**
- Business rules (FIFO sell, funds validation, quantity checks) live inside the aggregate, making them testable without infrastructure.
- Value objects eliminate primitive obsession and enforce constraints at construction time.
- The rich domain model is explicit about what operations are valid and what states are reachable.
- Domain tests verify business logic in isolation.

**Negative:**
- More classes than an anemic model (separate value objects for each concept).
- Persistence requires explicit mapping between domain objects and JPA entities.
- Contributors must understand DDD patterns to contribute effectively.

## Repository evidence

- `Portfolio.java`: Javadoc explicitly states "In DDD terms, this is an Aggregate Root"; methods `buy()`, `sell()`, `deposit()`, `withdraw()` enforce invariants
- `Holding.java`: Javadoc states "In DDD terms, this is an Entity"; `sell()` implements FIFO lot consumption
- `Lot.java`: Javadoc states "In DDD terms, this is an Entity"
- `Money.java`, `Price.java`, `ShareQuantity.java`, `Ticker.java`: Javadoc states "In DDD terms, this is a Value Object"
- `HoldingPerformanceCalculator.java`: domain service with no infrastructure dependencies
- `CONTRIBUTING.md`: "Business logic lives in the domain layer - inside aggregates, entities, and value objects. Application services orchestrate; they do not decide."
- `CONTRIBUTING.md`: "Value Objects over Primitives. The domain uses Money, Price, ShareQuantity, Ticker, PortfolioId, and similar types instead of raw BigDecimal, int, or String."
- `CONTRIBUTING.md`: "Aggregate Root as Consistency Boundary. All state changes to entities within an aggregate pass through the aggregate root."
- `doc/tutorial/richVsAnemicDomainModel/`: Entire tutorial comparing rich vs anemic approaches using HexaStock's sell use case
- Domain tests: `PortfolioTest.java`, `HoldingTest.java`, `LotTest.java`, `MoneyTest.java`, `TickerTest.java`

## Relation to other specifications

- **Gherkin:** Behavioural scenarios (e.g. `sell-stocks.feature`) describe the business rules that the rich domain model enforces. The domain model is the *how*; Gherkin is the *what*.
- **OpenAPI:** The API contract operates at the HTTP boundary. The domain model sits behind the port interface. DTOs translate between API representations and domain objects.
- **PlantUML:** Domain model diagrams (`hexastock-domain-model.puml`, `domain-class-diagram.puml`) visualise the same aggregate/entity/value-object structure this ADR formalises. The `rich-vs-anemic` tutorial diagrams explicitly compare the two approaches.
