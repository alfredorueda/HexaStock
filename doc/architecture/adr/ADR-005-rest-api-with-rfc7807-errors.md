# ADR-005: Expose functionality via REST API with RFC 7807 error responses

## Status

Accepted

## Context

The system needs an external interface for clients to manage portfolios, execute trades, and query data. The interface must be documented, testable, and decoupled from domain internals. Error responses must carry structured, machine-readable information to enable client-side handling.

## Decision

Expose all functionality via a REST API implemented as a primary (driving) adapter. Use RFC 7807 Problem Detail (`ProblemDetail`) for all error responses. Maintain a separate DTO layer in the adapter to decouple HTTP representations from domain objects.

Key design decisions within the API:

- Two controllers: `PortfolioRestController` (`/api/portfolios`) and `StockRestController` (`/api/stocks`).
- Controllers depend only on input port interfaces (`*UseCase`), not on service implementations.
- Resource creation (portfolio) returns HTTP 201 with a `Location` header.
- A centralised `@ControllerAdvice` maps domain exceptions to appropriate HTTP status codes.
- API documentation is auto-generated via SpringDoc OpenAPI and served at `/swagger-ui.html`.

## Alternatives considered

- **GraphQL:** Would provide flexible querying but adds complexity for a CRUD-oriented domain. Standard alternative, not discussed in the repository.
- **gRPC:** Would provide efficient binary communication but is less accessible for educational use and browser-based tooling. Standard alternative.
- **OpenAPI code generation (contract-first):** Generate controller interfaces from `openapi.yaml`. The project currently uses an implementation-first approach with `openapi.yaml` as a reverse-engineered specification. Standard alternative.

## Consequences

**Positive:**
- REST is universally understood and toolable (Swagger UI, curl, REST-Assured, IntelliJ HTTP client).
- `ProblemDetail` (RFC 7807) provides consistent, structured error responses with title, detail, and status fields.
- DTO layer prevents domain model leakage into the API contract.
- Input port dependency ensures the controller is testable by substituting port implementations.

**Negative:**
- DTO-to-domain mapping adds boilerplate.
- The `openapi.yaml` must be manually kept in sync with controller changes (no code generation enforces alignment).

## Repository evidence

- `PortfolioRestController.java`: `@RestController`, constructor injects `*UseCase` interfaces
- `StockRestController.java`: `@RestController` for stock price lookup
- `ExceptionHandlingAdvice.java`: `@ControllerAdvice` mapping 8 domain exception types to `ProblemDetail` responses (400, 404, 409, 503)
- `adapters-inbound-rest/pom.xml`: `spring-boot-starter-web`, `springdoc-openapi-starter-webmvc-ui`
- `application.properties`: `springdoc.api-docs.path=/api-docs`, `springdoc.swagger-ui.path=/swagger-ui.html`
- `doc/openapi.yaml`: Complete API contract with RFC 7807 `ProblemDetail` schema
- DTOs under `adapter.in.webmodel.*`: `CreatePortfolioDTO`, `PurchaseDTO`, `SaleResponseDTO`, etc.
- `doc/calls.http`: Pre-built HTTP requests for manual API testing
- Integration tests: REST-Assured assertions verifying status codes, `ProblemDetail` fields (`title`, `status`, `detail`)

## Relation to other specifications

- **Gherkin:** Gherkin scenarios reference HTTP status codes and response fields (e.g. "I receive 400 Bad Request with ProblemDetail"). The REST adapter implements these behaviours.
- **OpenAPI:** `doc/openapi.yaml` is the formal API contract. This ADR documents the decision to use REST and RFC 7807; OpenAPI specifies the exact endpoints, schemas, and error responses.
- **PlantUML:** Sequence diagrams (e.g. `sell-http-to-port.puml`) trace HTTP requests through the REST adapter to the application port.
