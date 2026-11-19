# DDD, Hexagonal, Onion and Clean Architecture

These terms answer related but different questions. Domain-Driven Design helps a team model business concepts and rules. Hexagonal, Onion and Clean Architecture describe ways to organize software boundaries and dependencies. They can be combined, but none is a certificate of design quality.

**Prerequisite:** understand one [HexaStock sale](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md). Read this comparison in three short passes: vocabulary, example, then trade-offs.

## 1. Distinguish the questions

| Approach | Main question | Characteristic vocabulary |
|---|---|---|
| Domain-Driven Design (DDD) | Which model and language express this business? | Bounded context, entity, value object, aggregate, invariant |
| Hexagonal Architecture | How can the application interact with different external technologies? | Driving/driven ports, inbound/outbound adapters |
| Onion Architecture | How can dependencies protect an inner domain model? | Domain center, application services, outer infrastructure |
| Clean Architecture | How can source dependencies protect policy from mechanisms? | Entities, use cases, interface adapters, frameworks and drivers |

A **bounded context** is a boundary within which a model and its language have a specific meaning. An **aggregate** is a consistency boundary for changes to related domain objects. These are different concepts: a context can contain many aggregates.

A **port** describes an interaction. A **source dependency** means one piece of code refers to another. A runtime call can go outward to a database adapter while the adapter's source depends inward on a core-owned interface. That distinction explains dependency inversion.

## 2. Trace the same sale through four perspectives

From a DDD perspective, `Portfolio`, `Holding` and `Lot` express ownership and FIFO accounting. `Money`, `Price`, `ShareQuantity` and `Ticker` distinguish concepts that primitive numbers and strings would blur. A lot has identity and mutable remaining quantity; it is not an immutable purchase record. Historical `Transaction` records are separate.

From a hexagonal perspective, the HTTP controller invokes `PortfolioStockOperationsUseCase`. The service uses `PortfolioPort`, `TransactionPort` and `StockPriceProviderPort`, all declared in `application`. JPA, MongoDB and quote clients implement output interactions. An inbound controller invokes an input port; it does not have to implement that interface itself.

From an onion perspective, the domain sits inside application coordination, with infrastructure outside. From a Clean Architecture perspective, the domain and use cases express policy while transport and persistence are mechanisms. These are useful readings of the same code; the project does not implement four independent architectures.

The concrete [module map](ARCHITECTURE-MAP.md) is the authoritative description of HexaStock. It includes Jakarta transaction declarations in application code, explicit Spring wiring in bootstrap, and seven narrowly defined architecture tests. Public mutable child access also limits how completely the aggregate boundary is enforced.

**Checkpoint:** replacing a quote provider should preserve FIFO. Adding a new lot-selection rule changes the domain even if the database remains the same. Explain the different change boundaries.

## 3. Evaluate benefits and costs

Separating a rule from HTTP and persistence allows isolated tests and makes some technology changes more local. It also introduces interfaces, mappings, configuration and concepts that readers must learn. An adapter change can still require configuration, transaction analysis and contract tests; it is not automatically a one-file replacement.

DDD's strategic work includes learning from domain experts and resolving language ambiguity. Merely creating `domain` and `application` directories does not perform that work. Similarly, concentric diagrams do not prove that dependencies follow the intended direction.

These approaches do not establish a universal ranking of maintainability or scalability. A small data-entry use case may justify a simpler transaction script; interdependent business rules may benefit from a richer model. The choice depends on behavior, change patterns, team understanding and verification cost.

Business rules can change rapidly too. Protecting them is useful because their meaning matters and their changes deserve isolation, not because rules are guaranteed to remain unchanged for decades.

For an architectural decision, state the concrete change or failure you want to handle, the boundary that helps, and the additional work it creates. Measure operational claims independently of the architectural label.

## Primary sources and further study

- Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, 2003, introduces the modeling discipline and its strategic and tactical vocabulary.
- Alistair Cockburn's [Ports and Adapters article](https://alistair.cockburn.us/hexagonal-architecture), 2005, explains isolating application interactions from external devices and databases.
- Jeffrey Palermo's [Onion Architecture, part 1](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/), 2008, motivates protecting long-lived business applications from infrastructure coupling.
- Robert C. Martin's [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html), 2012, synthesizes related ideas around the inward source dependency rule.

The mapping to HexaStock above is an interpretation of its current implementation. These sources supply the concepts; they do not certify this repository or prove comparative performance.

Continue with [architecture decisions](adr/README.md) or [the learning program](ARCHITECTURE-TRANSFORMATION-PROGRAM.md).
