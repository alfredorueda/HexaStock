# Start Here: Your First Steps in HexaStock

**Goal:** understand what the application does and choose a manageable first exercise.

You do not need previous experience with Domain-Driven Design (DDD), Hexagonal Architecture or financial systems. For the Java exercises, you should know how to read a class, call a method and recognize a collection. If these are new, first read the examples as descriptions of behaviour; running the application can wait.

## Begin with a concrete problem

Alice keeps track of cash and shares she owns. She buys 10 AAPL shares at 100.00 each, then another 5 at 120.00. Later she sells 12 at 150.00.

The application must answer three questions:

1. How much cash does Alice receive? **1800.00**: 12 × 150.00.
2. Which purchases did those shares come from? Using **FIFO** (first in, first out), the oldest purchase supplies 10 and the next supplies 2.
3. What did the sold shares originally cost? **1240.00**: 10 × 100.00 + 2 × 120.00. The profit is **560.00**, and Alice retains 3 shares.

The program must update cash, remaining shares and transaction history consistently. The architecture is a way to organize that work so that each rule can be understood and tested.

## Learn the vocabulary when you need it

| Word | Plain-language meaning | In this project |
| --- | --- | --- |
| Domain | The subject the program models | Cash, holdings, purchases and sales |
| Business rule | A condition the operation must respect | Do not sell more shares than are owned |
| Invariant | A rule that must hold after each permitted change | A lot's remaining quantity cannot be negative |
| Use case | One task the application performs | Sell shares from a portfolio |
| Port | An interface describing a capability | Fetch a stock price or save a portfolio |
| Adapter | Code connecting a port to a technology | Read a quote from an HTTP API |
| Test | An executable check of a stated expectation | Selling 12 shares leaves 3 |

You will meet *aggregate*, *value object*, *transaction* and *dependency inversion* in the relevant stages. You do not need to memorize these terms before starting.

## Follow one path, not the entire library

| Step | Read or do | You are ready to continue when… |
| --- | --- | --- |
| 1. Understand the behaviour | [Sell tutorial, Stage 1](../tutorial/sellStocks/SELL-STOCK-01-BEHAVIOUR.md) | You can calculate the example by hand |
| 2. Test a Java object | [Stage 2](../tutorial/sellStocks/SELL-STOCK-02-DOMAIN-TEST.md) | You can identify setup, action and assertions |
| 3. Follow the rule | [Stage 3](../tutorial/sellStocks/SELL-STOCK-03-FIFO.md) | You can trace which lots are consumed |
| 4. Connect the application | [Stages 4–6](../tutorial/sellStocks/SELL-STOCK-04-HTTP-AND-PORTS.md) | You can distinguish HTTP, orchestration and storage |
| 5. Handle failure and competition | [Stages 7–9](../tutorial/sellStocks/SELL-STOCK-07-CONSISTENCY.md) | You can explain rollback, concurrency and test scope |

Each stage has a checkpoint. One stage is enough for a first study session. The [complete learning path](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md) gives estimated study times and optional exercises.

## Run the smallest useful check

Install **JDK 21** and clone the repository. Use the included Maven wrapper so that you do not need a separate Maven installation:

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
./mvnw -pl domain test
```

This runs the domain tests without starting Spring, a database or a market-price service. Maven needs network access to download dependencies on the first run. On Windows, use `mvnw.cmd` in place of `./mvnw`.

**Expected result:** Maven reports `BUILD SUCCESS`. If it fails, first inspect `java -version` and the first error in the output. A passing test means its assertions passed; it does not establish that every possible input has been checked.

When you are ready for HTTP and databases, follow the repository [setup instructions](https://github.com/alfredorueda/HexaStock/blob/main/README.md#getting-started).

## Understand the scope of each reading

* **Current implementation:** behaviour present in the main Java modules, supported by linked source and tests.
* **Exercise or proposal:** an additional requirement to design or implement. Its classes and endpoints may not exist in the baseline.
* **Advanced review:** a reasoned assessment of alternatives and limits. Recommendations are not automatically implemented decisions.
* **Presentation or independent kata:** material with its own audience, specification and learning objectives.

For example, baseline sales use whole shares and assume USD, with no fees or settlement delay. Do not add settlement rules to your understanding of the baseline just because a later comparison discusses them.

## For a university class

Begin with one outcome students can explain: “selling shares changes inventory and cash together.” Ask students to calculate the result, inspect the test, then trace the implementation. Introduce the architectural term after students have encountered the problem it names.

A useful first submission is a short explanation of the 12-share sale and one additional test case. Assess the calculation, the test's assertions and the explanation of responsibilities; a large diagram or extensive terminology is not a substitute for understanding.

**Checkpoint:** if Alice receives 1800.00 and makes 560.00 profit, which amount increases her cash balance? Explain why.

**Answer:** cash increases by **1800.00**. The purchase cost was paid earlier; profit compares the proceeds with that historical cost.

[Continue to the sell-stock learning path](../tutorial/sellStocks/SELL-STOCK-TUTORIAL.md).
