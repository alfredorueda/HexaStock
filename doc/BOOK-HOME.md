# HexaStock: Engineering Architecture That Grows Stronger Through Change

*A technical book on Domain-Driven Design, Hexagonal Architecture, and Specification-Driven Development in a financial domain.*

---

This documentation is organised as a book-style technical journey through a working financial portfolio system. HexaStock serves as a concrete architectural case study — built with Java 21, Spring Boot 3, and Maven — where every design decision is grounded in real code, executable specifications, and automated tests. The central narrative follows a single use case, **selling stocks**, through every architectural layer: from Gherkin specification to REST controller, through application service orchestration, into the aggregate root's FIFO lot-consumption algorithm, and back as a structured financial result. The goal is to show how Domain-Driven Design and Hexagonal Architecture function not as abstract principles but as engineering disciplines applied under realistic constraints.

Every chapter in this book is directly backed by a robust, working open-source project. The full source code is publicly available at [github.com/alfredorueda/HexaStock](https://github.com/alfredorueda/HexaStock). Readers are encouraged to clone the repository and explore the codebase alongside the text — the repository [README](https://github.com/alfredorueda/HexaStock#readme) provides the practical information needed to build and run the system locally.

---

## Start Here

The primary entry point to this book is the sell-stock architectural study. It traces the full engineering workflow — specification, domain modelling, hexagonal structure, persistence, error handling, integration testing — applied to one use case from end to end.

- [Sell Stock Deep Dive — Reference Use Case](tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)
- [HexaStock — Project Overview](tutorial/sellStocks/HEXASTOCK-PROJECT-OVERVIEW.md) — System overview, architectural identity, Maven module structure, and domain package organisation.

---

## Supporting Chapters

These chapters deepen specific themes introduced in the main study.

- [Rich vs Anemic Domain Model](tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) — Side-by-side architectural comparison using the sell flow, with failure mode demonstration.
- [Dependency Inversion in Stock Selling](tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md) — Full execution flow through ports and adapters, with testability and extensibility analysis.
- [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) — Pessimistic locking (JPA/MySQL) and optimistic concurrency with retries (MongoDB), isolation levels, and race-condition tests.
- [DDD Portfolio and Transactions](DDD%20Portfolio%20and%20Transactions.md) — Why Portfolio and Transaction are separate aggregates: consistency boundaries, invariants, and a decision matrix.
- [Remove Lots with Zero Remaining Quantity](Remove%20Lots%20with%20Zero%20Remaining%20Quantity%20from%20Portfolio%20Aggregate.md) — Design decision on retaining or pruning fully consumed lots, with DDD-grounded analysis.
- [Holdings Performance at Scale](tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) — Four reporting strategies from in-memory aggregation to CQRS read models.
- [Watchlists & Market Sentinel](tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) — Automated market monitoring with CQRS and progressive domain model evolution.
- [Lot Selection Strategies — DDD Hexagonal Exercise](tutorial/DDD-Hexagonal-exercise.md) — Extending beyond FIFO with Strategy pattern and hexagonal structure.

---

## Featured: Specification-Driven Development with AI

> How structurally precise specifications — Gherkin, UML, OpenAPI, ADRs — enable high-quality AI-assisted implementation. This chapter analyses a real consulting case where the HexaStock codebase was built using specification-driven AI-assisted development, and examines the conditions under which this approach meets production engineering standards.

- [Specification-Driven Development with AI](https://alfredo-rueda-unsain.gitbook.io/alfredo-rueda-unsain-docs/supporting-chapters/specification-driven-development-with-ai)

---

## Technical Review

- [AI-Assisted Architectural Review — DDD and Hexagonal Architecture](domain-review/DDD-REVIEW-PERSONAL-INVESTMENT-PORTFOLIO.md) — A structured assessment of the domain model, aggregate design, and hexagonal layer separation.

---

## API and Specifications

- [Stock Portfolio API Specification](stock-portfolio-api-specification.md) — Complete REST API for all 10 use cases, RFC 7807 error contract, domain model, and exception mapping.
- [Gherkin Feature Files](features/sell-stocks.feature) — Fifteen executable behavioural specifications covering the full system.

---

## Reading Contract: Pedagogical Simplifications vs Production Refinements

HexaStock is intentionally designed to serve two audiences at once: it is a working, tested codebase, and it is a teaching artefact for engineers learning Domain-Driven Design and Hexagonal Architecture. To keep that dual purpose honest, this book classifies design choices in three explicit categories. Readers should keep this classification in mind whenever a chapter discusses a "simplification" or a "future refinement".

| Category | Examples in HexaStock | What it means in the book |
|---|---|---|
| **Intentional pedagogical simplifications** — chosen to make the model legible; perfectly defensible at this scale | Loading the entire `Portfolio` aggregate on sell; recording transactions through application-service orchestration rather than domain events; single currency (USD); integer share quantities; no fees, taxes or margin | The current code is the right answer **for teaching**. A production team operating at very different scale would refine these. |
| **Deliberate architectural choices** — production-quality decisions that are not simplifications | Two interchangeable persistence adapters (JPA pessimistic vs MongoDB optimistic + retry); separating `Portfolio` (state) from `Transaction` (history) into two aggregates; value objects (`Money`, `Price`, `Ticker`) instead of primitives; ArchUnit fitness tests | The current code is the right answer **in general**. These should be preserved or carried over to any production derivative. |
| **Acknowledged future enhancements** — explicitly out of scope today, but discussed as roadmap | Domain events for transaction creation; CQRS read models for reporting at scale; promoting `Holding` to its own aggregate when sell invariants justify it; Flyway/Liquibase migrations replacing `ddl-auto` | Treated as legitimate next steps; the book explains the reasoning rather than implementing them today. |

When the book uses the term **"production-grade"** without further qualification, it refers to *production-quality code* (typed, tested, layered, with enforced architectural boundaries) rather than to a *fully production-ready deployed system* (which would additionally require schema migrations, observability, capacity planning and operational tooling that are out of scope for this study).

---

## License

This project uses a dual-license model.

- **Source code** is licensed under [Apache License 2.0](../LICENSE).
- **Book and editorial written content** are licensed under [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).

[![License: CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

This book and its editorial written content are licensed under the Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0). You are welcome to read, share, and adapt this material for non-commercial purposes, provided that appropriate credit is given. For commercial use, including commercial training, paid courses, corporate programs, commercial publishing, or reuse within paid products or services, prior permission is required.

This dual-license approach is intentional: the software remains open source under Apache 2.0, while the book is shared openly for learning and non-commercial educational use under Creative Commons.

The full licensing details are available on the [Licensing](legal/LICENSE.md) page.
