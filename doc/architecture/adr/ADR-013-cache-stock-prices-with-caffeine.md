# ADR-013: Cache stock prices with Caffeine

## Status

Accepted

## Context

Stock price lookups are frequent operations (every buy, sell, and performance calculation requires a provider quote, potentially served from cache). External API calls to Finnhub or AlphaVantage have latency, rate limits, and quota constraints. Redundant API calls for the same ticker within a short window waste resources and risk hitting rate limits.

## Decision

Cache stock price responses using Spring Cache backed by Caffeine. Configuration:

- **Cache name:** `stockPrices`
- **Time-to-live:** 5 minutes (`expireAfterWrite=300s`)
- **Maximum entries:** 1,000
- **Cache annotation:** `@Cacheable("stockPrices")` on the stock price provider adapter method
- **Placement:** Adapter annotations use Spring Cache; bootstrap enables caching and supplies configuration

## Alternatives considered

- **Redis:** Distributed cache suitable for multi-instance deployments. Adds infrastructure complexity for a single-instance educational project. Standard alternative.
- **No caching:** Every request hits the external API. Would exhaust free-tier API quotas quickly. Rejected.
- **EhCache:** Another popular Java cache. This repository does not benchmark it against Caffeine. Standard alternative.
- **Longer TTL:** Would reduce API calls further but increase the staleness of price data. 5 minutes is a balance between freshness and API quota preservation for a non-HFT use case.

## Consequences

**Positive:**
- Can reduce external API calls for repeated lookups of the same ticker.
- Caffeine supplies an in-process cache; this decision does not establish an end-to-end performance bound.
- Spring `@Cacheable` provides transparent caching without modifying service logic.
- The five-minute policy deliberately allows reuse; it does not guarantee a fresh market quote.

**Negative:**
- Entries may be reused for up to five minutes after insertion; the provider quote may already be older.
- Cache is local to the JVM instance (no sharing across instances).
- Cache eviction policy (size-based + time-based) may evict actively traded tickers.

## Repository evidence

- `adapters-outbound-market/pom.xml`: `spring-boot-starter-cache`, `com.github.ben-manes.caffeine:caffeine`
- `application.properties`: `spring.cache.type=caffeine`, `spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=300s`, `spring.cache.cache-names=stockPrices`
- `SpringAppConfig.java`: `@EnableCaching`
- `FinhubStockPriceAdapter.java` and `AlphaVantageStockPriceAdapter.java`: `@Cacheable(cacheNames = "stockPrices", key = "#ticker.value()", sync = true)`. The random mock adapter is not cached by these annotations.
- `README.md`: provider profile setup

## Relation to other specifications

- **Gherkin:** Caching is transparent to behavioural specifications. A repeated request can receive a cached quote; tests must control the provider and cache state when asserting exact prices.
- **OpenAPI:** The `GET /api/stocks/{symbol}` endpoint returns stock prices. Caching affects latency and freshness and must be considered when interpreting the returned price and timestamp.
- **PlantUML:** Not directly represented. Could be visualised in a deployment or component diagram showing the cache layer.
