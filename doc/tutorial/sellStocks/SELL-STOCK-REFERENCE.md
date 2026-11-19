# References and Further Reading

This page is optional. Return to the [learning path](SELL-STOCK-TUTORIAL.md) to continue the staged tutorial. The bibliography below preserves the original tutorial's sources; the stages link directly to current implementation evidence where behaviour matters.

## Choose a deeper topic

| Topic | Reading |
| --- | --- |
| Domain vocabulary | [Ubiquitous Language](UBIQUITOUS-LANGUAGE.md) |
| Aggregate boundaries | [Portfolio and Transactions](<../../DDD Portfolio and Transactions.md>) |
| Depleted lots | [Removing empty lots](<../../Remove Lots with Zero Remaining Quantity from Portfolio Aggregate.md>) |
| Responsibility allocation | [Rich vs Anemic Domain Model](../richVsAnemicDomainModel/RICH_VS_ANEMIC_DOMAIN_MODEL_TUTORIAL.md) |
| Ports and dependencies | [Dependency Inversion](../DEPENDENCY-INVERSION-STOCK-SELLING.md) |
| Concurrency | [Pessimistic locking and optimistic concurrency](../CONCURRENCY-PESSIMISTIC-LOCKING.md) |
| Testing | [Testing Strategy](https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/TESTING-STRATEGY.md) |
| Reporting scale | [Holdings Performance at Scale](../portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) |
| Additional features | [Watchlists and Market Sentinel](../watchlists/WATCHLISTS-MARKET-SENTINEL.md) |
| Alternative lot policies | [Lot Selection Strategies](../DDD-Hexagonal-exercise.md) |
| Specification and AI | [Specification-Driven Development with AI](../specificationDrivenAI/SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md) |
| Architecture origins | [Reference sources and diagrams](HEXAGONAL-ARCHITECTURE-REFERENCE-DIAGRAMS.md) |

Some companion studies introduce extensions or alternative implementations. Use the main Java modules to distinguish implemented behaviour from an exercise or proposal.

## Source map

All links below target `main`. Package names are relative to `cat.gencat.agaur.hexastock`.

| Concern | Module and package |
| --- | --- |
| Aggregate, holding, lot and sale result | `domain` / `model.portfolio` |
| Money, price and quantity | `domain` / `model.money` |
| Ticker and market quote | `domain` / `model.market` |
| Transaction history | `domain` / `model.transaction` |
| Use cases and ports | `application` / `application.service`, `application.port.in`, `application.port.out` |
| HTTP | `adapters-inbound-rest` / `adapter.in` |
| Relational storage | `adapters-outbound-persistence-jpa` / `adapter.out.persistence.jpa` |
| Document storage | `adapters-outbound-persistence-mongodb` / `adapter.out.persistence.mongodb` |
| Application wiring and retry aspect | `bootstrap` / `config` |

* [Domain scenario tests](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/test/java/cat/gencat/agaur/hexastock/model/portfolio/PortfolioTest.java)
* [Holding algorithm tests](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/test/java/cat/gencat/agaur/hexastock/model/portfolio/HoldingTest.java)
* [Domain traceability annotation](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/test/java/cat/gencat/agaur/hexastock/SpecificationRef.java)
* [HTTP test base](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/AbstractPortfolioRestIntegrationTest.java)
* [Trading integration tests](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioTradingRestIntegrationTest.java)
* [Missing-portfolio integration tests](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/test/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioErrorHandlingRestIntegrationTest.java)
* [Architecture rules](https://github.com/alfredorueda/HexaStock/blob/main/bootstrap/src/test/java/cat/gencat/agaur/hexastock/architecture/HexagonalArchitectureTest.java)

## Toolchain in context

| Tool | Role |
| --- | --- |
| Java 21, Maven wrapper | Compile the seven-module application and run checks |
| Spring Boot 3.5 | Compose adapters and services; host HTTP and transaction infrastructure |
| JUnit, REST Assured | Check domain behaviour and HTTP contracts |
| Testcontainers | Supply real databases for integration and adapter tests |
| ArchUnit | Check the seven dependency rules described in Stage 9 |
| Hibernate / MySQL, Spring Data MongoDB | Alternative persistence implementations |
| SpringDoc | Generate OpenAPI documentation from controller metadata |
| Gherkin and `@SpecificationRef` | Link readable scenarios to JUnit tests |
| PlantUML | Maintain versionable diagram sources and render SVG/PNG assets |
| JaCoCo, GitHub Actions | Instrument tests and run the build in CI |

Diagram source files, rendering commands and asset maintenance are documented in the [diagram guide](https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/diagrams/README.md).

The API behaviour is specified in project documents; SpringDoc generates the runtime OpenAPI description. This tutorial does not claim that controllers are generated from an OpenAPI contract. Inspect the build and workflow files for exact versions and reporting configuration.

## Bibliography


### Foundational Works

* Berenson, H., Bernstein, P. A., Gray, J., Melton, J., O’Neil, E., and O’Neil, P. "A Critique of ANSI SQL Isolation Levels." _Proceedings of the 1995 ACM SIGMOD International Conference on Management of Data_, 1995.
* Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." _alistair.cockburn.us_, 2005. https://alistair.cockburn.us/hexagonal-architecture/
* Cohn, Mike. _Succeeding with Agile: Software Development Using Scrum._ Addison-Wesley, 2009. (Source of the test-pyramid model.)
* Evans, Eric. _Domain-Driven Design: Tackling Complexity in the Heart of Software._ Addison-Wesley, 2003.
* Evans, Eric. _Domain-Driven Design Reference: Definitions and Pattern Summaries._ Dog Ear Publishing, 2014.
* Fowler, Martin. _Patterns of Enterprise Application Architecture._ Addison-Wesley, 2002. (Unit of Work, Repository, Domain Model, Data Mapper.)
* Fowler, Martin. "AnemicDomainModel." _martinfowler.com_, 2003. https://martinfowler.com/bliki/AnemicDomainModel.html
* Fowler, Martin. "Ubiquitous Language." _martinfowler.com_, 2006. https://martinfowler.com/bliki/UbiquitousLanguage.html
* Fowler, Martin. _Refactoring: Improving the Design of Existing Code._ 2nd ed., Addison-Wesley, 2018. (Primitive Obsession code smell.)
* Gray, Jim and Reuter, Andreas. _Transaction Processing: Concepts and Techniques._ Morgan Kaufmann, 1993.
* Haerder, Theo and Reuter, Andreas. "Principles of Transaction-Oriented Database Recovery." _ACM Computing Surveys_, 15(4):287–317, December 1983. (Original statement of the ACID properties.)
* Hombergs, Tom. _Get Your Hands Dirty on Clean Architecture._ Packt Publishing, 2019. Reference implementation: [BuckPal](https://github.com/thombergs/buckpal).
* Graça, Herberto. "DDD, Hexagonal, Onion, Clean, CQRS, … How I Put It All Together." _herbertograca.com_, 2017. https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/
* Martin, Robert C. "The Dependency Inversion Principle." _C++ Report_, 8(6), May 1996.
* Martin, Robert C. _Clean Architecture: A Craftsman's Guide to Software Structure and Design._ Prentice Hall, 2017.
* Meszaros, Gerard. _xUnit Test Patterns: Refactoring Test Code._ Addison-Wesley, 2007.
* North, Dan. "Introducing BDD." _Better Software_, March 2006. https://dannorth.net/introducing-bdd/
* Vernon, Vaughn. _Implementing Domain-Driven Design._ Addison-Wesley, 2013.

### Framework and Platform References

* Bauer, Christian, King, Gavin, and Gregory, Gary. _Java Persistence with Hibernate._ 2nd ed., Manning, 2015.
* _Hibernate ORM User Guide._ Red Hat. https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html (cited for batch fetching and fetching strategies).
* _MySQL 8.0 Reference Manual._ Oracle. https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html (cited for InnoDB transaction isolation levels).
* _Spring Framework Reference Documentation — Data Access._ Pivotal/VMware. https://docs.spring.io/spring-framework/reference/data-access/transaction.html (cited for declarative transaction management).

### Standards and Specifications

* Nottingham, M. and Wilde, E. "Problem Details for HTTP APIs." RFC 7807, IETF, March 2016. https://www.rfc-editor.org/rfc/rfc7807
* OpenAPI Initiative. _OpenAPI Specification, Version 3.0.3._ 2021. https://spec.openapis.org/oas/v3.0.3


## Acknowledgements

Thank you to [Víctor García](https://www.linkedin.com/in/garcia-victor/) for his collaboration on the original hexagonal architecture diagram. The staged edition redraws the diagrams while preserving the architectural teaching purpose of that work.

See the broader [project acknowledgements](../../ACKNOWLEDGEMENTS.md).

---

[Learning path](SELL-STOCK-TUTORIAL.md) · [Exercises](SELL-STOCK-EXERCISES.md)
