# ADR-013: Cache stock prices with Caffeine

## Status

Accepted

## Context

Stock price lookups are frequent operations (every buy, sell, and performance calculation requires a current price). External API calls to Finnhub or AlphaVantage have latency, rate limits, and quota constraints. Redundant API calls for the same ticker within a short window waste resources and risk hitting rate limits.

## Decision

Cache stock price responses using Spring Cache backed by Caffeine. Configuration:

- **Cache name:** `stockPrices`
- **Time-to-live:** 5 minutes (`expireAfterWrite=300s`)
- **Maximum entries:** 1,000
- **Cache annotation:** `@Cacheable("stockPrices")` on the stock price provider adapter method
- **Profile:** Caching is configured in the `adapters-outbound-market` module

## Alternatives considered

- **Redis:** Distributed cache suitable for multi-instance deployments. Adds infrastructure complexity for a single-instance educational project. Standard alternative.
- **No caching:** Every request hits the external API. Would exhaust free-tier API quotas quickly. Rejected.
- **EhCache:** Another popular Java cache. Caffeine is the recommended default for Spring Boot and provides superior performance. Standard alternative.
- **Longer TTL:** Would reduce API calls further but increase the staleness of price data. 5 minutes is a balance between freshness and API quota preservation for a non-HFT use case.

## Consequences

**Positive:**
- Dramatically reduces external API calls for repeated lookups of the same ticker.
- Caffeine is a high-performance, thread-safe cache with O(1) operations.
- Spring `@Cacheable` provides transparent caching without modifying service logic.
- 5-minute TTL ensures prices are reasonably current for educational purposes.

**Negative:**
- Prices may be up to 5 minutes stale.
- Cache is local to the JVM instance (no sharing across instances).
- Cache eviction policy (size-based + time-based) may evict actively traded tickers.

## Repository evidence

- `adapters-outbound-market/pom.xml`: `spring-boot-starter-cache`, `com.github.ben-manes.caffeine:caffeine`
- `application.properties`: `spring.cache.type=caffeine`, `spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=300s`, `spring.cache.cache-names=stockPrices`
- `SpringAppConfig.java`: `@EnableCaching`
- `FinhubStockPriceAdapter.java`: likely annotated with `@Cacheable("stockPrices")` (cache is configured for the stock price lookup operation)
- `README.md`: "Caffeine-based local caching with TTL" listed as a feature

## Relation to other specifications

- **Gherkin:** Caching is transparent to behavioural specifications. Users see the same stock price behaviour regardless of cache state.
- **OpenAPI:** The `GET /api/stocks/{symbol}` endpoint returns stock prices. Caching affects latency and freshness but not the API contract.
- **PlantUML:** Not directly represented. Could be visualised in a deployment or component diagram showing the cache layer.
