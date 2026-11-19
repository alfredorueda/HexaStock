# 2. Test Adapters Deterministically

**Goal:** exercise a technology boundary while controlling inputs that would otherwise vary.

## Market adapters: use a local HTTP server

`FinhubStockPriceAdapterTest` and `AlphaVantageStockPriceAdapterTest` use WireMock. The adapter sends an actual HTTP request to a local server that returns a configured response. This exercises URL construction and JSON parsing without relying on a live market quote or an API key.

The Finnhub success test matches `/quote`, the symbol and the test token, then returns a known price. Separate tests cover missing data, server errors and a null-like response. An unmatched stub normally produces an HTTP error response; it does not necessarily cause a timeout.

These tests instantiate the adapter directly and inject its URL and key fields. Consequently, they do not exercise profile selection, Spring property binding or `@Cacheable` interception. A timeout setting needs a delayed-response test to be verified; the existence of WireMock alone does not test it.

Stubs document the provider contract assumed by the tests. They do not detect a provider change automatically.

## Integration prices: use a controlled port implementation

The trading integration test uses a different technique. Its nested `FixedPriceStockPriceAdapter` returns queued prices and a configured fallback of 150.00 when the queue is empty. It is registered as `@Primary` for the trading test class, not only for its nested Gherkin scenarios.

The scenario setup clears the queue, creates a portfolio, deposits cash, then queues 100.00 for the first purchase and 120.00 for the second. The sale queues 150.00. The resulting calculation is repeatable.

The production `mockfinhub` adapter generates random demonstration prices. It is not interchangeable with a fixed expected-price fixture when asserting exact financial results.

## Persistence: separate mapping from database behavior

A mapper unit test checks conversion between domain objects and persistence representations. A database-backed test adds schema, query and repository behavior.

Both persistence modules inherit shared application-level contracts:

- `AbstractPortfolioPortContractTest` checks selected portfolio operations and round trips.
- `AbstractTransactionPortContractTest` checks selected transaction-record operations.

JPA subclasses supply MySQL-backed adapters; MongoDB subclasses supply MongoDB-backed adapters. The abstract tests keep common expectations in one place. Each adapter still needs technology-specific tests for its locking or version behavior.

Passing the shared cases demonstrates agreement for those cases, not complete equivalence across isolation, ordering, failure modes or performance. MongoDB is already implemented; it is not merely a proposed future adapter.

## Be precise about test transactions

The JPA test classes use Spring test transaction support and delegate overridden methods to shared contract assertions. Do not generalize this local arrangement into a rule that Spring cannot run inherited test methods transactionally. Check the concrete test configuration and actual rollback behavior.

Sharing a Testcontainers instance reduces startup work. It does not automatically isolate every committed write across tests; transaction rollback, cleanup and unique test data remain relevant. Using the same database engine improves fidelity, but different deployment configuration and workload can still change behavior.

## Checkpoint

If both adapters pass a create-and-load contract, have their concurrency policies been shown equivalent?

**Answer:** no. Use controlled concurrent-update tests and inspect their outcomes separately. See [the concurrency lessons](../CONCURRENCY-PESSIMISTIC-LOCKING.md).

[Previous: rules](01-RULES-AND-ORCHESTRATION.md) · [Next: the assembled system](03-INTEGRATION-AND-ARCHITECTURE.md)
