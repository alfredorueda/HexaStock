# ADR-011: Implement FIFO lot accounting for stock sales in the domain

## Status

Accepted

## Context

When a portfolio holds multiple purchase lots of the same stock (bought at different times and prices), selling shares requires a policy to determine which lots are consumed first. The lot selection policy affects cost basis calculation, profit/loss reporting, and tax implications. The FIFO (First-In-First-Out) policy is the most common default for individual investor accounting.

## Decision

Implement FIFO lot accounting as a core domain rule within the `Holding.sell()` method:

1. Iterate lots in insertion order (oldest first).
2. From each lot, consume `min(lot.remainingQuantity, requestedQuantity)` shares.
3. Accumulate cost basis from each lot's purchase price proportional to consumed shares.
4. Reduce each lot's remaining quantity.
5. Remove fully depleted lots from the holding.
6. Return a `SellResult` containing total proceeds, cost basis, and profit.

This logic is entirely within the domain layer, with no infrastructure dependency.

## Alternatives considered

- **LIFO (Last-In-First-Out):** Consumes the most recently purchased lots first. Different tax implications. The Gherkin specification explicitly specifies FIFO. Standard alternative.
- **Specific lot identification:** Let the user choose which lots to sell. More flexible but more complex. Not specified in current requirements. Standard alternative.
- **Average cost basis:** Average the cost across all lots. Simpler but less precise for tax reporting. Standard alternative.

## Consequences

**Positive:**
- Cost basis and profit/loss are deterministic given a known purchase history.
- The FIFO algorithm is well-understood, auditable, and documented by Gherkin scenarios.
- Business rule is encapsulated in the domain entity, not scattered across services.
- Fully testable without infrastructure.

**Negative:**
- Only one lot selection policy is supported. Adding alternatives would require refactoring to a strategy pattern.
- Lots must be maintained in insertion order (guaranteed by Java's `ArrayList` and JPA's `@OrderColumn` if present, or by creation timestamp).

## Repository evidence

- `Holding.java`: `sell()` method implements the FIFO algorithm - loops through lots oldest-first, reduces quantities, accumulates cost basis, removes depleted lots
- `sell-stocks.feature`: Scenario "Shares are consumed from lots in FIFO order" (US-07.FIFO-1, US-07.FIFO-2, US-07.FIFO-3) specifies exhaustive FIFO examples
- `fifo-settlement-selling.feature`: Extended FIFO scenarios with settlement considerations (US-13)
- `PortfolioTradingRestIntegrationTest.java`: `GherkinFifoScenarios` nested class with `@SpecificationRef("US-07.FIFO-1")` etc., using `FixedPriceStockPriceAdapter` for deterministic price control
- `SellResult.java`: record containing `proceeds`, `costBasis`, `profit` returned from sell operations
- `PortfolioTest.java`, `HoldingTest.java`: domain-level tests verifying FIFO behaviour

## Relation to other specifications

- **Gherkin:** This ADR documents the decision behind the FIFO policy that `sell-stocks.feature` and `fifo-settlement-selling.feature` specify in behavioural terms. The Gherkin scenarios provide the acceptance criteria; this ADR provides the rationale.
- **OpenAPI:** The `SaleResponse` schema in `openapi.yaml` returns `costBasis`, `proceeds`, and `profit` fields that are computed by the FIFO algorithm.
- **PlantUML:** Sequence diagrams under `doc/tutorial/sellStocks/diagrams/` trace the sell flow through `Portfolio.sell()` → `Holding.sell()` → lot iteration.
