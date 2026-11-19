# Holdings performance: from calculation to scaling decisions

A report combines purchase history, remaining lots and a market quote. Before optimizing it, establish what each number means and what work the request performs.

Only **Approach A**, in-memory calculation, is implemented on `main`. The other approaches below are design exercises. They do not imply existing query ports, snapshot tables or event infrastructure.

## Learning route

| Lesson | Goal | Starting knowledge |
|---|---|---|
| [1. Calculate one report](01-CURRENT-CALCULATION.md) | Explain the current metrics and trace their sources | Portfolio, holding, lot and FIFO |
| [2. Measure before optimizing](02-MEASUREMENT.md) | Separate CPU work, retained memory, allocations and I/O | Lesson 1 |
| [3. Query and aggregate differently](03-QUERY-ALTERNATIVES.md) | Compare database and hybrid aggregation | Basic SQL and output ports |
| [4. Maintain a projection](04-SNAPSHOTS.md) | Reason about consistency and recovery | Transactions; optional advanced lesson |

For a first session, complete only lesson 1 and its numerical checkpoint. Return to the remaining lessons when discussing performance requirements.

## Keep reads and writes distinct

A reporting query can inspect many transactions without making them children of the portfolio aggregate. Write boundaries follow invariants; read models follow query needs. See [history and scale](../../architecture/aggregates/02-HISTORY-AND-SCALE.md).

The public response is documented in [US-09](../../api/US-09-GET-HOLDINGS-PERFORMANCE.md). Domain calculations return `HoldingPerformance`; the REST adapter maps that to `HoldingDTO`.

## Source and further reading

- [Calculator](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/HoldingPerformanceCalculator.java) and [calculator tests](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/test/java/cat/gencat/agaur/hexastock/model/portfolio/HoldingPerformanceCalculatorTest.java).
- [Reporting service](https://github.com/alfredorueda/HexaStock/blob/main/application/src/main/java/cat/gencat/agaur/hexastock/application/service/ReportingService.java) and [service tests](https://github.com/alfredorueda/HexaStock/blob/main/application/src/test/java/cat/gencat/agaur/hexastock/application/service/ReportingServiceTest.java).
- Martin Fowler, [CQRS](https://martinfowler.com/bliki/CQRS.html), for the separation of read and write models and its added complexity.
- Chris Richardson, [Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html), for durable event publication and duplicate delivery concerns.
- Oracle, [Java 21 BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), for decimal arithmetic and explicit rounding.
- Vaughn Vernon, *Implementing Domain-Driven Design*, 2013, for aggregate and architectural trade-offs.
