# HexaStock Project Overview

**Goal:** locate a business rule, a use case and an adapter in the repository.

**Prerequisite:** basic Java interfaces. **Reading time:** about 8 minutes.

## Begin with a portfolio

A portfolio has cash and shares. Buying shares reduces cash and creates a purchase lot. Selling shares increases cash and consumes lots in purchase order. This First In, First Out (FIFO) rule determines the cost basis used to calculate profit.

HexaStock implements portfolio creation and lookup, deposits, withdrawals, purchases, sales, transaction history and holdings reports. Quotes can come from external providers or a demonstration adapter. Cached quotes are not a guarantee of a live exchange price. Settlement, fees and watchlists are advanced extensions, not features of the current baseline.

## Three questions organize the design

1. **What must always be true?** The domain model enforces business rules, such as not selling more shares than are available.
2. **What must happen to complete a request?** An application service obtains data, invokes the domain and coordinates persistence.
3. **How does the application communicate with a technology?** Adapters translate HTTP, database and price-provider representations.

Domain-Driven Design (DDD) helps name and model the business concepts. Hexagonal architecture separates the application from external technologies through interfaces called *ports*. They address different questions and can be studied separately.

## The seven Maven modules

| Module | Responsibility | Start by locating |
|---|---|---|
| `domain` | Business objects and rules | `Portfolio`, `Holding`, `Lot` |
| `application` | Use cases and ports | `PortfolioStockOperationsService`, `PortfolioPort` |
| `adapters-inbound-rest` | HTTP translation | `PortfolioRestController` |
| `adapters-outbound-persistence-jpa` | MySQL persistence | `JpaPortfolioRepository` |
| `adapters-outbound-persistence-mongodb` | MongoDB persistence | The implementation of `PortfolioPort` |
| `adapters-outbound-market` | Quote-provider integration | `FinhubStockPriceAdapter` |
| `bootstrap` | Startup, wiring and cross-cutting configuration | `HexaStockApplication` |

The domain's production code depends on the JDK, not on Spring or database APIs. Test libraries are inherited separately from the parent POM. Application services use Jakarta transaction annotations and application-owned ports. Bootstrap connects those interfaces to concrete adapters.

Choose `jpa` or `mongodb` at startup. Both adapters implement the same persistence ports, but their concurrency mechanisms differ: JPA uses pessimistic write locking; MongoDB combines version checks, transactions and retries. Interchangeable interfaces do not make database behavior identical.

## Follow one request

For `POST /api/portfolios/{id}/sales`:

1. The REST controller converts the request into domain value objects.
2. The application service loads a portfolio and obtains a quote through outbound ports.
3. `Portfolio.sell` checks the command and delegates lot consumption to `Holding.sell`.
4. The service saves the updated portfolio and records a sale transaction.
5. The REST adapter returns proceeds, cost basis and profit.

An inbound port describes what a caller can ask the application to do. An outbound port describes a capability the application needs. The application calls an outbound interface at runtime, while the concrete adapter depends on that interface in source code. This distinction is the basis of dependency inversion.

See [Stage 4](SELL-STOCK-04-HTTP-AND-PORTS.md) for the original hexagonal diagram and [Stage 5](SELL-STOCK-05-ORCHESTRATION.md) for the service.

## Understand the aggregate boundary

`Portfolio` is the aggregate root: the intended entry point for business updates. `Holding` and `Lot` are child entities inside its consistency boundary, not independent aggregates. Transaction records are stored separately and associated by portfolio ID.

The domain packages group concepts by meaning:

| Package | Concepts |
|---|---|
| `model.portfolio` | Portfolio, holdings, lots and sale results |
| `model.transaction` | Financial transaction records and identifiers |
| `model.market` | Tickers and prices returned by the market provider |
| `model.money` | Money, unit price and share quantity value objects |

The current API exposes some mutable children. An unmodifiable list prevents structural changes to the list; it does not make the objects inside it immutable. Treat root-only mutation as a design rule to preserve, not a fully enforced property of every public method.

## What the structure does and does not guarantee

Maven dependencies restrict which classes a module can compile against. Seven ArchUnit rules check selected package dependency directions. Neither mechanism proves business correctness or forbids every possible dependency between outbound adapters.

The modules are parts of one application, not seven microservices or seven bounded contexts. A smaller project could express the same responsibilities using packages. This repository uses modules to make the boundaries visible for teaching.

## Check your understanding

A new quote provider becomes available. Which code should change?

**Answer:** add or replace a market adapter and its bootstrap configuration. The FIFO rule in `Holding` should remain unchanged because selecting purchase lots does not depend on the provider's HTTP protocol.

Continue with the [domain vocabulary](UBIQUITOUS-LANGUAGE.md) or [the staged sale tutorial](SELL-STOCK-TUTORIAL.md).

## References

* Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." _alistair.cockburn.us_, 2005. https://alistair.cockburn.us/hexagonal-architecture/
* Evans, Eric. _Domain-Driven Design: Tackling Complexity in the Heart of Software._ Addison-Wesley, 2003.
* Martin, Robert C. _Clean Architecture: A Craftsman's Guide to Software Structure and Design._ Prentice Hall, 2017.
* Vernon, Vaughn. _Implementing Domain-Driven Design._ Addison-Wesley, 2013.

***

_Return to the_ [_Sell Stock Deep Dive_](SELL-STOCK-TUTORIAL.md) _or the_ [_book home page_](../../BOOK-HOME.md)_._
