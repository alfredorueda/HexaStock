[![Build Status](https://github.com/alfredorueda/HexaStock/workflows/CI/badge.svg)](https://github.com/alfredorueda/HexaStock/actions)

# HexaStock

HexaStock is a Spring Boot project that teaches Domain-Driven Design and Hexagonal Architecture through a realistic financial portfolio domain. Built for engineering students and workshop participants, it provides a complete, testable system with real business rules, multiple interchangeable adapters, and clear architectural boundaries.

## Core Documentation

Two documents form the backbone of HexaStock's documentation.

### Stock Portfolio API Specification

> **[doc/stock-portfolio-api-specification.md](doc/stock-portfolio-api-specification.md)**

The functional reference for the entire system:

- User stories covering portfolio creation, deposits, withdrawals, buying/selling stocks, holdings, transactions, and stock price queries
- Global error handling following RFC 7807 Problem Details
- Domain model overview with PlantUML diagrams
- HTTP request/response examples for every endpoint

### Sell Stock Deep Dive (Reference Use Case)

> **[doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md)**

The sell stock use case is the architectural reference of the project. It concentrates the highest density of domain logic and best illustrates DDD and Hexagonal Architecture in practice. The tutorial covers:

- Full execution trace: REST adapter → application service → domain model → persistence
- Aggregate boundary protection (`Portfolio` as aggregate root enforcing invariants)
- FIFO accounting logic implemented entirely in the domain layer
- Orchestration by application services vs. rule enforcement by the aggregate
- Hands-on exercises for training sessions and self-guided study

### Documentation Map

| Document | Description |
|----------|-------------|
| [API Specification](doc/stock-portfolio-api-specification.md) | User stories, domain model, error handling, HTTP examples |
| [Sell Stock Tutorial](doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md) | End-to-end use case walkthrough with exercises |
| [HTTP Requests](doc/calls.http) | Pre-built requests for manual API testing in IntelliJ |
| [OpenAPI Definition](doc/openapi.yaml) | Machine-readable API contract |
| [DDD Portfolio and Transactions](doc/DDD%20Portfolio%20and%20Transactions.md) | Domain design notes for portfolios and transactions |
| [Remove Zero-Quantity Lots](doc/Remove%20Lots%20with%20Zero%20Remaining%20Quantity%20from%20Portfolio%20Aggregate.md) | Design discussion on aggregate cleanup |
| [Dependency Inversion (Selling)](doc/tutorial/DEPENDENCY-INVERSION-STOCK-SELLING.md) | Tutorial on dependency inversion in the sell flow |
| [Concurrency and Pessimistic Locking](doc/tutorial/CONCURRENCY-PESSIMISTIC-LOCKING.md) | Handling concurrent portfolio updates |
| [DDD Hexagonal Exercise](doc/tutorial/DDD-Hexagonal-exercise.md) | Guided exercise on hexagonal patterns |
| [Holdings Performance at Scale](doc/tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) | Reporting and performance considerations |
| [Watchlists: Market Sentinel](doc/tutorial/watchlists/WATCHLISTS-MARKET-SENTINEL.md) | Watchlist feature design and tutorial |

## Prerequisites

- JDK 21 or higher
- Apache Maven 3.6 or higher
- IntelliJ IDEA Ultimate (recommended)
- Docker (for running MySQL in a container)
- Git

## Running the Application

### Mandatory Spring Profiles

The application will **not start** without activating the required profiles. You must specify:

- **Persistence (mandatory):** `jpa`
- **Stock price provider (choose one):** `finhub` or `alphaVantage`

Valid combinations:
- `jpa,finhub`
- `jpa,alphaVantage`

**From the command line:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=jpa,finhub
```

**From IntelliJ IDEA:** open *Run > Edit Configurations*, set the *Active profiles* field to `jpa,finhub` or `jpa,alphaVantage`, then run.

The application starts on port **8081**.

## API Keys Configuration

All configuration is centralized in `src/main/resources/application.properties`. Profile-specific files (`application-finhub.properties`, `application-alphavantage.properties`) do **not** exist yet.

```properties
# Finnhub (active when profile = finhub)
finhub.api.url=https://finnhub.io/api/v1
finhub.api.key=your_finhub_key_here

# Alpha Vantage (active when profile = alphaVantage)
alphaVantage.api.base-url=https://www.alphavantage.co/query
alphaVantage.api.key=your_alphavantage_key_here
```

Free-tier keys: [finnhub.io](https://finnhub.io/) · [alphavantage.co](https://www.alphavantage.co/)

You can also supply keys as environment variables to keep credentials out of version control.

## Interacting with the Application

Pre-configured HTTP requests are in [doc/calls.http](doc/calls.http).

**To use in IntelliJ IDEA:**
1. Open `doc/calls.http`
2. Ensure the application is running
3. Click the green **Run** arrow next to any request

**Example workflow:**
1. Create a portfolio
2. Deposit cash
3. Buy stocks (e.g. AAPL, MSFT)
4. Check portfolio status
5. Sell stocks
6. View transaction history

Swagger UI is available at `http://localhost:8081/swagger-ui.html`.

## Contributing

This is an educational project. Students are encouraged to experiment with adding new features or modifying existing behaviour to gain practical experience with the architectural patterns in action.

