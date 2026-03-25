# HexaStock: Engineering Architecture That Grows Stronger Through Change

*A technical book on Domain-Driven Design and Hexagonal Architecture in a financial domain.*

---

This documentation is organised as a book-style technical journey through a working financial portfolio system. HexaStock serves as a concrete architectural case study — built with Java 21, Spring Boot 3, and Maven — where every design decision is grounded in real code, executable specifications, and automated tests. The central narrative follows a single use case, **selling stocks**, through every architectural layer: from Gherkin specification to REST controller, through application service orchestration, into the aggregate root's FIFO lot-consumption algorithm, and back as a structured financial result. The goal is to show how Domain-Driven Design and Hexagonal Architecture function not as abstract principles but as engineering disciplines applied under realistic constraints. 

Every chapter in this book is directly backed by a robust, working open-source project. The full source code is publicly available at [github.com/alfredorueda/HexaStock](https://github.com/alfredorueda/HexaStock). Readers are encouraged to clone the repository and explore the codebase alongside the text — the repository [README](https://github.com/alfredorueda/HexaStock#readme) provides the practical information needed to build and run the system locally.

---

## Start Here

The primary entry point to this book is the sell-stock architectural study. It traces the full engineering workflow — specification, domain modelling, hexagonal structure, persistence, error handling, integration testing — applied to one use case from end to end.

- [Sell Stock Deep Dive — Reference Use Case](tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)

---

## Supporting Chapters

These chapters deepen specific themes introduced in the main study.

- [Rich vs Anemic Domain Model](tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) — Side-by-side architectural comparison using the sell flow, with failure mode demonstration.
- [Dependency Inversion in Stock Selling](tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md) — Full execution flow through ports and adapters, with testability and extensibility analysis.
- [Concurrency Control with Pessimistic Database Locking](tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) — Locking strategies, isolation levels, race condition tests, and Java 21 virtual thread considerations.
- [DDD Portfolio and Transactions](DDD%20Portfolio%20and%20Transactions.md) — Why Portfolio and Transaction are separate aggregates: consistency boundaries, invariants, and a decision matrix.
- [Remove Lots with Zero Remaining Quantity](Remove%20Lots%20with%20Zero%20Remaining%20Quantity%20from%20Portfolio%20Aggregate.md) — Design decision on retaining or pruning fully consumed lots, with DDD-grounded analysis.
- [Holdings Performance at Scale](tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) — Four reporting strategies from in-memory aggregation to CQRS read models.
- [Watchlists & Market Sentinel](tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) — Automated market monitoring with CQRS and progressive domain model evolution.
- [Lot Selection Strategies — DDD Hexagonal Exercise](tutorial/DDD-Hexagonal-exercise.md) — Extending beyond FIFO with Strategy pattern and hexagonal structure.

---

## API and Specifications

- [Stock Portfolio API Specification](stock-portfolio-api-specification.md) — Complete REST API for all 10 use cases, RFC 7807 error contract, domain model, and exception mapping.
- [Gherkin Feature Files](features/sell-stocks.feature) — Fifteen executable behavioural specifications covering the full system.

---

## License

This project uses a dual-license model.

- **Source code** is licensed under [Apache License 2.0](../LICENSE).
- **Book and editorial written content** are licensed under [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).

[![License: CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

This book and its editorial written content are licensed under the Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0). You are welcome to read, share, and adapt this material for non-commercial purposes, provided that appropriate credit is given. For commercial use, including commercial training, paid courses, corporate programs, commercial publishing, or reuse within paid products or services, prior permission is required.

This dual-license approach is intentional: the software remains open source under Apache 2.0, while the book is shared openly for learning and non-commercial educational use under Creative Commons.

The full licensing details are available on the [Licensing](legal/LICENSE.md) page.
