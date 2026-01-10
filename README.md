[![Build Status](https://github.com/alfredorueda/HexaStock/workflows/CI/badge.svg)](https://github.com/alfredorueda/HexaStock/actions)

# HexaStock

HexaStock is a financial portfolio management system built with Spring Boot, Domain-Driven Design (DDD), and Hexagonal Architecture. This educational project demonstrates practical implementation of these architectural patterns in a real-world domain: financial portfolio management.

## Project Overview

HexaStock is designed as a teaching tool for engineering students learning Hexagonal Architecture and Domain-Driven Design. Rather than theoretical examples, it provides a complete, testable system that handles realistic business requirements.

The system allows users to manage investment portfolios, including:

- Creating portfolios and managing cash through deposits and withdrawals
- Buying and selling stocks using real-time market prices
- Tracking holdings with detailed purchase history (lots)
- Viewing transaction history and calculating profit/loss using FIFO accounting

What makes this project particularly valuable for learning is its implementation of DDD tactical patterns (aggregates, value objects, domain services) and Hexagonal Architecture (ports and adapters), making the separation between business logic and infrastructure concerns explicit and testable.

## Why the Sell Stock Use Case Matters

The sell stock use case has been deliberately chosen as the central reference example because it is the richest and most sophisticated one in the project. It concentrates the highest amount of domain logic and therefore illustrates particularly well the power of Domain-Driven Design and Hexagonal Architecture.

This use case demonstrates:

- Aggregate boundary protection (Portfolio as aggregate root)
- FIFO accounting logic implemented in the domain layer
- Transactional consistency across multiple operations
- Domain exception handling and HTTP status code mapping
- Orchestration by application services while aggregates enforce invariants

By understanding this use case thoroughly, students will be able to progress confidently through the rest of the practice and apply the same patterns to other use cases.

For a comprehensive deep dive into the sell stock execution flow, including step-by-step traces, architectural decisions, and hands-on exercises, refer to the dedicated [Sell Stock Tutorial](doc/tutorial/SELL-STOCK-TUTORIAL.md).

## Prerequisites

Before starting, ensure you have the following installed:

- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.6 or higher
- IntelliJ IDEA Ultimate (recommended for full Spring Boot support)
- Docker (for running MySQL in a container)
- Git

You should have basic knowledge of Java, object-oriented programming, and familiarity with Spring Boot concepts.

## Getting Started

### Cloning the Repository

Clone the repository from GitHub:

```bash
git clone https://github.com/alfredorueda/HexaStock.git
cd HexaStock
```

### Opening the Project in IntelliJ IDEA

1. Launch IntelliJ IDEA
2. Select "Open" from the welcome screen
3. Navigate to the cloned HexaStock directory
4. Select the `pom.xml` file and choose "Open as Project"
5. Wait for Maven to download dependencies and index the project

IntelliJ IDEA will automatically detect the Spring Boot configuration and Maven structure.

### Starting the Database

Start MySQL using Docker:

```bash
docker-compose up -d
```

This will start a MySQL container on port 3307 with the required database configuration.

### Running Tests

HexaStock includes comprehensive unit and integration tests that verify both domain logic and infrastructure adapters.

#### Running Tests from Command Line

Execute all tests using Maven:

```bash
./mvnw clean test
```

This runs unit tests (classes ending in `Test.java`) using the Surefire plugin. To run both unit and integration tests:

```bash
./mvnw clean verify
```

This executes unit tests and integration tests (classes ending in `IT.java`) using the Failsafe plugin, and generates a merged code coverage report.

#### Running Tests from IntelliJ IDEA

To run all tests:

1. Open the Project view
2. Right-click on `src/test/java`
3. Select "Run 'All Tests'"

To run a specific test class:

1. Navigate to the test class (e.g., `PortfolioTest.java`)
2. Right-click on the class name
3. Select "Run 'PortfolioTest'"

#### Understanding Test Types

HexaStock distinguishes between two categories of tests:

**Domain Unit Tests** verify business logic in isolation without any infrastructure dependencies. Examples include `PortfolioTest`, `HoldingTest`, `LotTest`, and `MoneyTest`. These tests run fast and validate that domain invariants are correctly enforced.

**Integration Tests** verify the interaction between adapters and the application core. Examples include `PortfolioRestControllerIntegrationTest`. These tests use Testcontainers to spin up a real MySQL database and validate end-to-end flows through the REST API.

### Running the Application

#### Mandatory Spring Profiles

**CRITICAL:** The application will NOT start unless you activate the required Spring profiles. You must specify:

1. **Persistence profile (mandatory):**
    - `jpa` - Activates JPA/Hibernate persistence with MySQL

2. **Stock price provider profile (exactly one must be chosen):**
    - `finhub` - Uses the Finnhub API for stock prices
    - `alphaVantage` - Uses the AlphaVantage API for stock prices

Valid profile combinations:
- `jpa,finhub`
- `jpa,alphaVantage`

#### Running from IntelliJ IDEA

1. Locate the main class: `src/main/java/cat/gencat/agaur/hexastock/HexaStockApplication.java`
2. Right-click on the class and select "Run 'HexaStockApplication'"
3. The application will fail to start because profiles are not set
4. Open "Run > Edit Configurations"
5. Select the HexaStockApplication run configuration
6. In the "Active profiles" field, enter the profiles as a comma-separated list:
    - For Finnhub: `jpa,finhub`
    - For AlphaVantage: `jpa,alphaVantage`
7. Click "OK" and run the application again

The application will start on port 8081. You should see log messages indicating successful startup.

#### Running from Command Line

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=jpa,finhub
```

or

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=jpa,alphaVantage
```

Each profile configures a different adapter for the `StockPriceProviderPort`. The Finnhub adapter calls the Finnhub API, while the AlphaVantage adapter calls the AlphaVantage API. The domain and application layers remain unchanged regardless of which adapter is active. This demonstrates the flexibility of Hexagonal Architecture.

## API Keys Configuration

Both stock price providers require API keys. Currently, all API keys are configured directly in the single existing file:

```
src/main/resources/application.properties
```

Open this file and locate the following properties:

```properties
# Finhub API settings (used when profile is set to 'finhub')
finhub.api.url=https://finnhub.io/api/v1
finhub.api.key=your_finhub_key_here

# Alpha Vantage API configuration (used when profile is set to 'alphaVantage')
alphaVantage.api.base-url=https://www.alphavantage.co/query
alphaVantage.api.key=your_alphavantage_key_here
```

**Note:** Profile-specific properties files (such as `application-finhub.properties` or `application-alphavantage.properties`) do NOT exist yet. All configuration is currently centralized in the main `application.properties` file.

You can obtain free-tier API keys from:

- Finnhub: https://finnhub.io/
- AlphaVantage: https://www.alphavantage.co/

Replace the placeholder values with your actual API keys. Alternatively, you can set them as environment variables to keep credentials out of version control.

## Interacting with the Application

### Using the HTTP Client

HexaStock provides pre-configured HTTP requests for testing the REST API manually. These requests are located in:

[doc/calls.http](doc/calls.http)

#### Opening the HTTP Client in IntelliJ IDEA

1. Navigate to [doc/calls.http](doc/calls.http)
2. Open the file in the editor
3. Ensure the application is running
4. Click the green "Run" arrow next to any request to execute it

#### Example Workflow

Execute the requests in sequence to simulate a complete portfolio lifecycle:

1. Create a portfolio
2. Deposit cash into the portfolio
3. Buy stocks (AAPL, MSFT, etc.)
4. Check portfolio status
5. Sell stocks
6. View transaction history

Each request includes example payloads and expected responses.

### API Documentation

Access the Swagger UI to explore and test the API interactively:

```
http://localhost:8081/swagger-ui.html
```

### Main Endpoints

| Endpoint | Method | Description | Request Body | Response |
|----------|--------|-------------|--------------|----------|
| `/api/portfolios` | POST | Create a new portfolio | `{"ownerName":"John Doe"}` | Portfolio details |
| `/api/portfolios/{id}` | GET | Get portfolio details | - | Portfolio details |
| `/api/portfolios/{id}/deposits` | POST | Deposit cash | `{"amount":1000.00}` | Status 200 |
| `/api/portfolios/{id}/withdrawals` | POST | Withdraw cash | `{"amount":500.00}` | Status 200 |
| `/api/portfolios/{id}/purchases` | POST | Buy stock | `{"ticker":"AAPL","quantity":10}` | Status 200 |
| `/api/portfolios/{id}/sales` | POST | Sell stock | `{"ticker":"AAPL","quantity":5}` | Sale result with profit/loss |
| `/api/portfolios/{id}/transactions` | GET | Get transaction history | - | List of transactions |
| `/api/stocks/{symbol}` | GET | Get current stock price | - | Stock price information |

## Architecture Overview

HexaStock implements Hexagonal Architecture, which organizes code into three primary layers: domain, application, and adapters.

### Domain Layer

The core of the application contains business entities and logic with no dependencies on external frameworks:

- **Entities**: Portfolio (aggregate root), Holding, Lot, Transaction
- **Value Objects**: Money, Ticker
- **Domain Exceptions**: Business rule violations expressed in domain language

### Application Layer

Defines use case interfaces (ports) and implements orchestration logic in application services:

- **Inbound Ports**: Use cases the application provides (e.g., `PortfolioStockOperationsUseCase`)
- **Outbound Ports**: Services the application needs (e.g., `PortfolioPort`, `StockPriceProviderPort`)
- **Application Services**: Coordinate domain objects, call ports, manage transactions

### Adapters Layer

Connects the application to the outside world:

- **Inbound Adapters**: REST controllers that receive HTTP requests and call use case ports
- **Outbound Adapters**: JPA repositories, HTTP clients for external APIs (Finnhub, AlphaVantage)

This separation enables independent testing of business logic, flexible adapter replacement, and clear architectural boundaries that prevent infrastructure concerns from leaking into business rules.

### UML Class Diagram

<img width="1693" height="1576" alt="image" src="https://github.com/user-attachments/assets/85a5e57f-86dc-4b44-b221-fb7a4e3d3e6c" />

## Domain Concepts

### Portfolio

The central aggregate root in the system. A portfolio maintains a cash balance and contains a collection of holdings. It enforces business rules such as preventing purchases with insufficient funds and ensuring all changes to contained entities maintain consistency.

### Holding

Represents ownership of a specific stock identified by a ticker symbol (e.g., AAPL for Apple). Contains a collection of lots and manages selling using FIFO (First-In-First-Out) accounting.

### Lot

Represents a specific purchase of shares. Records the quantity, price, and purchase date. Tracks how many shares remain unsold and is used for calculating cost basis and profit/loss when selling.

### Transaction

Records all financial activities within a portfolio. Types include BUY, SELL, DEPOSIT, and WITHDRAWAL. Stores details like price, quantity, and timestamp. For sales, includes profit/loss calculations.

## Further Reading

Now that you have the application running and understand the basic structure, explore these resources:

### Deep Dive Tutorial

Read the comprehensive [tutorial on the sell stock use case](doc/tutorial/SELL-STOCK-TUTORIAL.md).

This document traces the execution path from HTTP request to domain logic to persistence, explaining why application services orchestrate while aggregates protect invariants. It includes hands-on exercises to reinforce your understanding.

### Codebase Exploration

Start from the REST controllers in `adapter.in`, follow calls through application services in `application.service`, and examine domain logic in the `model` package.

Review the test classes to understand how domain rules are verified in isolation and how integration tests validate end-to-end behavior.

### Additional Documentation

Explore the documentation in the `doc/` directory for API specifications, architectural diagrams, and design decisions.

## Test Coverage

HexaStock maintains high test coverage to ensure reliability and facilitate refactoring. The project uses JaCoCo for coverage reporting.

<img width="1287" height="420" alt="image" src="https://github.com/user-attachments/assets/267beb7f-dfc1-4db0-8337-7b690848b5e5" />

## Contributing

This is an educational project. Students are encouraged to experiment with adding new features or modifying existing behavior to gain practical experience with the architectural patterns in action.

