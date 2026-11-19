# Welcome to HexaStock

HexaStock is a Java application for learning how to organize and test software. It models a small stock portfolio: create an account, add cash, buy shares and sell them. The examples use this concrete problem to introduce Domain-Driven Design (DDD) and Hexagonal Architecture step by step.

## Choose Your Starting Point

* **New to architecture or financial examples?** Begin with [Your First Steps](learning/START-HERE.md). It explains the vocabulary and gives a first exercise that needs no database.
* **Ready to follow a complete use case?** Work through the [Sell Stock learning path](tutorial/sellStocks/SELL-STOCK-TUTORIAL.md), one stage at a time. Each stage has a goal, example and checkpoint.
* **Looking for a particular decision?** Use the supporting chapters below or the [architecture decision records](https://github.com/alfredorueda/HexaStock/blob/main/doc/architecture/adr/README.md).
* **Setting up the application?** Use the repository [README](https://github.com/alfredorueda/HexaStock/blob/main/README.md).

The book and [source repository](https://github.com/alfredorueda/HexaStock) can be read together. You do not need to understand every module to begin: the first exercises work with ordinary Java objects and JUnit tests.

***

## Supporting Chapters

These chapters deepen specific themes introduced in the main study.

* [Rich vs Anemic Domain Model](tutorial/richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) — Compare where rules live; the settlement example extends the baseline.
* [Dependency Inversion in Stock Selling](tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md) — Full execution flow through ports and adapters, with testability and extensibility analysis.
* [Concurrency Control: Pessimistic Locking and Optimistic Concurrency](tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) — Pessimistic locking (JPA/MySQL) and optimistic concurrency with retries (MongoDB), isolation levels, and race-condition tests.
* [DDD Portfolio and Transactions](<DDD Portfolio and Transactions.md>) — Why Portfolio and Transaction are separate aggregates: consistency boundaries, invariants, and a decision matrix.
* [Remove Lots with Zero Remaining Quantity](<Remove Lots with Zero Remaining Quantity from Portfolio Aggregate.md>) — Design decision on retaining or pruning fully consumed lots, with DDD-grounded analysis.
* [Holdings Performance at Scale](tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) — Four reporting strategies from in-memory aggregation to CQRS read models.
* [Watchlists & Market Sentinel](tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) — A proposed extension for automated market monitoring.
* [Lot Selection Strategies — DDD Hexagonal Exercise](tutorial/DDD-Hexagonal-exercise.md) — Extending beyond FIFO with Strategy pattern and hexagonal structure.

***

## Featured: Specification-Driven Development with AI

This advanced chapter examines how specifications, tests and review can guide AI-assisted implementation. Read it after you can distinguish a requirement from a test and from implementation code.

* [Specification-Driven Development with AI](tutorial/specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md)

***

## Technical Review

* [AI-Assisted Architectural Review — DDD and Hexagonal Architecture](domain-review/DDD-REVIEW-PERSONAL-INVESTMENT-PORTFOLIO.md) — A structured assessment of the domain model, aggregate design, and hexagonal layer separation.

***

## API and Specifications

* [Stock Portfolio API Specification](stock-portfolio-api-specification.md) — Complete REST API for all 10 use cases, RFC 7807 error contract, domain model, and exception mapping.
* [Gherkin Feature Files](https://github.com/alfredorueda/HexaStock/blob/main/doc/features/sell-stocks.feature) — Readable behavioural specifications linked to executable JUnit tests.

***

## How to Read the Technical Chapters

A chapter may describe the current implementation, propose an extension, or compare alternatives. Check that scope before treating an example as a feature available in the application. Linked source files and tests provide implementation evidence; a recommendation is a design argument to evaluate.

For example, the baseline sell flow has no settlement delay or fees. Reporting projections, domain events and alternative lot-selection policies are later design topics. You can finish the introductory path before studying them.

The project is a teaching application. Its use of tests and architectural boundaries does not, by itself, establish suitability for every production workload or deployment.

***

## License

This project uses a dual-license model.

* **Source code** is licensed under [Apache License 2.0](legal/LICENSE.md).
* **Book and editorial written content** are licensed under [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).

[![License: CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

This book and its editorial written content are licensed under the Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0). You are welcome to read, share, and adapt this material for non-commercial purposes, provided that appropriate credit is given. For commercial use, including commercial training, paid courses, corporate programs, commercial publishing, or reuse within paid products or services, prior permission is required.

This dual-license approach is intentional: the software remains open source under Apache 2.0, while the book is shared openly for learning and non-commercial educational use under Creative Commons.

The full licensing details are available on the [Licensing](legal/LICENSE.md) page.
