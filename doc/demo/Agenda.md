# HexaStock classroom demonstration

**Goal:** explain one complete sale and the boundaries that make it testable. Suggested duration: 30 minutes. Prepare Java 21; use the [start guide](../learning/START-HERE.md). Docker is needed for database integration tests, not the domain-only session.

| Time | Demonstration | Ask learners to explain |
|---|---|---|
| 0–5 min | Original [hexagonal overview](../tutorial/sellStocks/SELL-STOCK-04-HTTP-AND-PORTS.md) | Which arrows represent dependencies? |
| 5–12 min | [FIFO example](../tutorial/sellStocks/SELL-STOCK-03-FIFO.md) | Why do 12 sold shares have cost basis 1240? |
| 12–17 min | [Domain tests](../tutorial/sellStocks/SELL-STOCK-02-DOMAIN-TEST.md) | Which assertions check remaining state? |
| 17–23 min | [Application and persistence](../tutorial/sellStocks/SELL-STOCK-05-ORCHESTRATION.md) | Who decides, who coordinates, and who stores? |
| 23–28 min | [Integration evidence](../tutorial/testing/03-INTEGRATION-AND-ARCHITECTURE.md) | What can only a real adapter test establish? |
| 28–30 min | [CI and coverage](../../CI_DISCIPLINE_WALKTHROUGH.md) | What does a passing check still not prove? |

Use a deterministic quote fixture when demonstrating exact amounts. The `mockfinhub` runtime adapter returns random prices; the trading integration tests use a fixed-price test adapter. Do not expect an arbitrary live quote to reproduce the tutorial's arithmetic.

For an optional adapter comparison, explain the `finhub`, `alphaVantage`, and `mockfinhub` profiles. Real providers need their own configuration and can fail or return cached observations. Keep credentials outside the demonstration material.

Use the current [API reference](../stock-portfolio-api-specification.md) and `http/api-tests.http` for request examples. Run destructive database initialization only in a disposable teaching environment; the default JPA `create-drop` setting recreates the schema.

Show the coverage report from the run being demonstrated. Do not quote a fixed percentage or claim that coverage guarantees correct behavior. CI runs on its configured triggers; see [CI setup](../../CI_SETUP.md).

**Exit question:** if the database changes from JPA/MySQL to MongoDB, which business-rule tests should remain unchanged, and which adapter guarantees must be verified again?
