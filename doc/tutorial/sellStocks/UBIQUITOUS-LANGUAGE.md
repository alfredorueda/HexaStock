# Ubiquitous Language in HexaStock

**Companion to [Sell Stock Tutorial](SELL-STOCK-TUTORIAL.md)**

---

## The Concept

Domain-Driven Design is not primarily concerned with technical patterns. It is concerned with aligning software with the business reality it serves [Evans, 2003; Vernon, 2013]. At the centre of that alignment stands the **ubiquitous language** — the shared vocabulary that the development team and domain experts use to describe the model, and that the model, in turn, makes explicit in code.

Eric Evans introduced the ubiquitous language as a foundational DDD practice: within a bounded context, the same terms must appear in conversation, documentation, diagrams, and source code [Evans, 2003, ch. 2]. A change in the language is a change in the model, and a change in the model is a change in the language; the two co-evolve. Vaughn Vernon reinforces this by arguing that tactical design should embody domain concepts explicitly and consistently, so that software stays focused on business meaning rather than drifting toward purely technical abstractions [Vernon, 2013, ch. 1]. Martin Fowler describes the ubiquitous language as a rigorous shared language between developers and domain experts, whose purpose is to remove ambiguity and keep the model grounded in testable conversation [Fowler, 2006].

This is not a matter of cosmetic naming. When the same concept is called one thing in a Gherkin scenario, another in a class diagram, and a third in Java source, the outcome is a modelling defect. Inconsistent terminology erodes traceability, slows onboarding, introduces subtle defects where participants believe they are discussing the same idea but in fact are not, and silently decouples the software from the business it is supposed to represent.

## Why It Matters in a Pedagogical Project

HexaStock is a teaching codebase. Its readers are engineers, architects, and students who will carry the patterns they learn here into production systems. If the project is sloppy with names — using "stock" in one place, "equity" in another, and "position" in a third to mean the same thing — the pedagogical message undermines itself. Conversely, when the same business term appears consistently from specification to diagram to code to test, readers absorb the discipline of Ubiquitous Language by example, not by lecture.

## Ubiquitous Language in the Sell-Stock Use Case

The sell-stock use case illustrates how one vocabulary thread runs through every artifact type in the repository.

**Gherkin specifications** express behaviour in business terms. The canonical scenario in `sell-stocks.feature` speaks of a *portfolio*, *lots* in *purchase order*, *shares*, a *market price*, *FIFO* consumption, *proceeds*, *cost basis*, and *profit*. These are not arbitrary labels — they are the language of portfolio accounting, and they appear here first because behaviour is specified before any design decisions are made.

**Domain classes** embody the same terms operationally. The aggregate root is `Portfolio`. It contains `Holding` entities, each composed of `Lot` instances. The sell operation returns a `SellResult` carrying `proceeds`, `costBasis`, and `profit` — the same three financial concepts named in the Gherkin scenario. Value objects such as `Money`, `Price`, `ShareQuantity`, and `Ticker` replace primitives, making the domain language type-safe and self-documenting. Domain exceptions — `InsufficientFundsException`, `ConflictQuantityException`, `HoldingNotFoundException` — name the business error, not the technical failure mode.

**Application services** preserve the vocabulary at the orchestration layer. `PortfolioStockOperationsService.sellStock(PortfolioId, Ticker, ShareQuantity)` reads as a domain sentence: *sell stock identified by a ticker and a share quantity from a specific portfolio*. The method delegates to `Portfolio.sell(...)`, which in turn delegates to `Holding.sell(...)` — each layer using the same terms, with progressively finer detail.

**UML class diagrams** reflect the domain structure, not persistence or framework concerns. The diagram in the tutorial's Domain Context section shows `Portfolio`, `Holding`, `Lot`, `Money`, `Price`, `ShareQuantity`, and `SellResult` — the same names the reader has already seen in the Gherkin scenario and will see again in the Java source. A reader who understands the diagram understands the code, because both speak the same language.

**UML sequence diagrams** trace the sell-stock flow through architectural layers. Even when the diagram shows technical interactions — controller calls service, service calls port, port returns aggregate — the operation names are `sellStock`, `sell`, `Portfolio`, `Holding`, `SellResult`. The technical structure is visible, but the domain vocabulary is never displaced by it.

**REST endpoints** translate at the boundary without inventing a separate vocabulary. The endpoint `POST /api/portfolios/{id}/sales` uses the plural *sales* as a resource noun consistent with the domain's `SALE` transaction type. The request DTO carries `ticker` and `quantity`; the response includes `proceeds`, `costBasis`, and `profit`. A domain expert reading the API documentation recognises the terms immediately.

**Tests** describe behaviour in domain language. Test methods are named `shouldSellSharesFromOldestLotFirst` and `shouldSellSharesAcrossMultipleLots` — these are business observations, not implementation details. `@SpecificationRef("US-07.FIFO-1")` ties each test back to a Gherkin scenario, closing the traceability loop with the same vocabulary at every link in the chain.

**Packages** group code by business meaning. The domain module organises concepts under `portfolio/`, `money/`, `market/`, and `transaction/` — reflecting what the code is about, not what DDD building block it implements.

## Cross-Artifact Consistency as a Design Discipline

The following table traces six domain terms across artifact types to show the consistency that Ubiquitous Language demands:

| Domain Term | Gherkin | Class / Type | Method / Field | Test | REST API |
|---|---|---|---|---|---|
| Portfolio | "a portfolio exists" | `Portfolio` | `Portfolio.sell(...)` | `PortfolioTest` | `/api/portfolios/{id}` |
| Holding | "the portfolio holds AAPL" | `Holding` | `Holding.sell(...)` | `HoldingTest` | — |
| Lot | "lots (in purchase order)" | `Lot` | `Lot.reduce(...)` | lot assertions in tests | — |
| Proceeds | "proceeds: 1200.00" | `SellResult` | `result.proceeds()` | `assertEquals(Money.of("1200.00"), result.proceeds())` | `"proceeds"` in JSON |
| Cost Basis | "costBasis: 800.00" | `SellResult` | `result.costBasis()` | `assertEquals(Money.of("800.00"), result.costBasis())` | `"costBasis"` in JSON |
| FIFO | "FIFO Lot Consumption" | algorithm in `Holding` | `sell(quantity, price)` | `shouldSellSharesFromOldestLotFirst` | implicit |

When every row in this table is consistent, the chain from business conversation to running code is unbroken. When a cell drifts — a test calls proceeds "revenue", or a diagram renames Holding to "Position" — the chain weakens, and with it the model's integrity.

## What Goes Wrong Without Ubiquitous Language

Terminology drift is not a cosmetic defect. It is a structural problem with concrete consequences:

- **One term, multiple meanings.** If "transaction" means a financial operation in the domain but a database transaction in the service layer, discussions become ambiguous and bugs become harder to trace.
- **Technical names displacing business names.** If the domain calls the result of a sale `SellResult` but the API calls it `TradeResponse` and the test calls it `saleOutput`, three teams can argue about the same concept without realising they agree.
- **Diagrams diverging from code.** If a class diagram labels a concept differently from the Java source, the diagram becomes unreliable, and developers stop trusting documentation.
- **Tests describing mechanics instead of behaviour.** A test named `testMethod7` or `verifySellServiceCallsRepositorySaveOnce` tells the reader nothing about the business rule being verified, and breaks the traceability chain to the specification.

These are not hypothetical risks. They are common degradation patterns in codebases that do not treat vocabulary as a first-class design artifact.

## Language Evolves with the Model

Ubiquitous Language is not frozen at the start of a project. As the team's understanding of the domain deepens — through conversations with domain experts, through collaborative modelling sessions, or through the discovery that a term is ambiguous — the language changes, and the model changes with it. Renaming a class, splitting a concept, or introducing a new term is not rework. It is model refinement, and it should be treated with the same rigour as any other design improvement.

Inside a bounded context, each important term should carry a single, precise meaning, but that meaning may evolve; the codebase should evolve with it. A healthy ubiquitous language is not one that never changes — it is one that changes deliberately, coherently, and across all artifacts at once [Evans, 2003, ch. 2; Vernon, 2013, ch. 1].

## References

- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003. (See chapter 2 on Ubiquitous Language.)
- Fowler, Martin. "Ubiquitous Language." *martinfowler.com*, 2006. https://martinfowler.com/bliki/UbiquitousLanguage.html
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013. (See chapter 1 on language and context.)

