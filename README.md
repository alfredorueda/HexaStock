[![CI](https://github.com/alfredorueda/HexaStock/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/alfredorueda/HexaStock/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=${SONAR_PROJECT_KEY}&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=${SONAR_PROJECT_KEY})
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=${SONAR_PROJECT_KEY}&metric=coverage)](https://sonarcloud.io/summary/new_code?id=${SONAR_PROJECT_KEY})
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=${SONAR_PROJECT_KEY}&metric=bugs)](https://sonarcloud.io/summary/new_code?id=${SONAR_PROJECT_KEY})
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=${SONAR_PROJECT_KEY}&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=${SONAR_PROJECT_KEY})

# HexaStock

HexaStock is a financial portfolio management system built with Spring Boot, Domain-Driven Design (DDD), and Hexagonal Architecture. This educational project demonstrates best practices for building a robust, maintainable financial application using modern software architecture patterns.

## Project Overview

HexaStock allows users to manage investment portfolios, including:

- Creating portfolios and managing cash through deposits and withdrawals
- Buying and selling stocks using real-time market prices
- Tracking holdings with detailed purchase history (lots)
- Viewing transaction history and calculating profit/loss

What makes this project particularly interesting is its implementation of DDD tactical patterns and Hexagonal Architecture, making it an excellent educational resource for developers looking to understand these architectural approaches in a real-world context.

## Architecture

HexaStock implements Hexagonal Architecture (also known as Ports and Adapters), which separates the application into distinct layers:

### Domain Layer
The core of the application contains business entities and logic with no dependencies on external frameworks:
- **Entities**: Portfolio, Holding, Lot, Transaction
- **Value Objects**: Money, Ticker
- **Domain Services**: Business rules for buying, selling, and portfolio management

### Application Layer
Defines use cases and orchestrates the domain objects:
- **Ports**: Interfaces that define how the application interacts with the outside world
  - **Input Ports**: Use cases the application provides (e.g., `PortfolioManagementUseCase`)
  - **Output Ports**: Services the application needs (e.g., `PortfolioPort`, `StockPriceProviderPort`)
- **Services**: Implementations of the input ports that coordinate domain objects

### Adapters Layer
Connects the application to external systems:
- **Inbound Adapters**: REST controllers that expose the application's functionality
- **Outbound Adapters**: JPA repositories, external API clients for stock prices

This architecture provides several benefits:
- **Testability**: The domain and application logic can be tested in isolation
- **Flexibility**: Adapters can be replaced without changing the core logic
- **Focus on Business Rules**: Domain logic is protected from infrastructure concerns

## Main Concepts

### Portfolio
The central aggregate root in the system. A portfolio:
- Belongs to a user/owner
- Maintains a cash balance
- Contains a collection of holdings (stocks)
- Enforces business rules like preventing purchases with insufficient funds

### Holding
Represents ownership of a specific stock:
- Identified by a ticker symbol (e.g., AAPL for Apple)
- Contains a collection of lots (individual purchases)
- Manages selling using FIFO (First-In-First-Out) accounting

### Lot
Represents a specific purchase of shares:
- Records the quantity, price, and purchase date
- Tracks how many shares remain unsold
- Used for calculating cost basis and profit/loss when selling

### Transaction
Records all financial activities within a portfolio:
- Types include: BUY, SELL, DEPOSIT, WITHDRAWAL
- Stores details like price, quantity, and timestamp
- For sales, includes profit/loss calculations

### UML Class Diagram (Plant UML)
<img width="1693" height="1576" alt="image" src="https://github.com/user-attachments/assets/85a5e57f-86dc-4b44-b221-fb7a4e3d3e6c" />


## How to Run

### Prerequisites
- JDK 21 or higher
- Maven
- MySQL (or Docker for running the database)

### Setup Database
```bash
# Start MySQL using Docker
docker-compose up -d
```

### Build and Run
```bash
# Clone the repository
git clone https://github.com/yourusername/hexastock.git
cd hexastock

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8081`

### API Documentation
Access the Swagger UI to explore and test the API:
```
http://localhost:8081/swagger-ui.html
```

## Endpoints

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

## Concurrency and Consistency

Financial applications require careful handling of concurrent operations to maintain data integrity. HexaStock addresses this in several ways:

### Pessimistic Locking
When retrieving a portfolio, the system uses pessimistic locking to prevent concurrent modifications:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
```

This ensures that when multiple requests attempt to operate on the same portfolio (e.g., buying/selling stocks), one operation completes fully before the next begins.

### Transactional Boundaries
All services that modify portfolio data are annotated with `@Transactional`, ensuring that operations either complete fully or roll back completely, maintaining database consistency:

```java
@Service
@Transactional
public class PortfolioStockOperationsService implements PortfolioStockOperationsUseCase {
    // ...
}
```

### Aggregate Design
The Portfolio acts as an aggregate root, ensuring that all changes to its contained entities (Holdings, Lots) are consistent. This DDD pattern helps maintain invariants across related entities.

## Testing Strategy

HexaStock uses a comprehensive testing approach:

### Unit Tests
- Domain entities are thoroughly tested in isolation
- Tests verify business rules and invariants without any infrastructure dependencies
- Each domain concept has dedicated test classes (e.g., `PortfolioTest`, `HoldingTest`, `LotTest`)

### Integration Tests
- Test the interaction between the application and its adapters
- Verify that persistence mechanisms work correctly
- Ensure API endpoints function as expected

To run the tests:
```bash
./mvnw test
```

## Educational Notes

### Aggregate Design
The Portfolio-Holding-Lot hierarchy demonstrates proper aggregate design in DDD. The Portfolio is the aggregate root, ensuring all changes to Holdings and Lots maintain business invariants. This design prevents inconsistent states, such as selling more shares than owned or having negative balances.

### Value Objects
Money and Ticker are implemented as immutable value objects, focusing on what they are rather than who they are. This enhances code clarity and prevents bugs related to primitive obsession.

### FIFO Accounting
The system implements First-In-First-Out (FIFO) accounting for stock sales, a common approach in finance. When selling shares, the oldest lots are sold first, which is clearly demonstrated in the Holding entity's sell method.

### Domain Events (Potential Enhancement)
While not currently implemented, a natural evolution would be to add domain events for significant business operations (e.g., StockSold, CashDeposited). This would allow for features like notifications, audit logging, or event sourcing.

## Further Improvements

HexaStock could be enhanced with:

1. **Event-Driven Architecture**: Implement domain events to enable more decoupled, reactive features
2. **User Authentication**: Add multi-user support with proper authentication and authorization
3. **Performance Optimizations**: Caching for stock prices and portfolio data
4. **Extended Financial Features**:
   - Dividend tracking
   - Portfolio analytics and performance metrics
   - Tax lot optimization strategies (beyond FIFO)
5. **Real-Time Updates**: WebSocket integration for live price updates
6. **Reporting**: Generate financial reports and tax documents

---

This project demonstrates the power of Domain-Driven Design and Hexagonal Architecture in creating maintainable, testable applications for complex domains like finance. By separating concerns and focusing on the business core, HexaStock provides a solid foundation that can evolve with changing requirements.

JaCoCo

<img width="1287" height="420" alt="image" src="https://github.com/user-attachments/assets/267beb7f-dfc1-4db0-8337-7b690848b5e5" />

