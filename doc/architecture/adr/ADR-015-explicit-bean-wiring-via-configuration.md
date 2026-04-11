# ADR-015: Wire application services explicitly via @Configuration beans

## Status

Accepted

## Context

In Spring Boot, dependency injection can be configured in two ways: annotation-based auto-discovery (e.g. `@Service`, `@Component` on classes in scanned packages) or explicit `@Bean` methods in a `@Configuration` class. The choice affects how tightly application services are coupled to the Spring framework and how visible the wiring is to developers.

For a hexagonal architecture project, keeping application services free of Spring annotations strengthens the boundary between the application layer and the framework.

## Decision

Wire all application services via explicit `@Bean` methods in `SpringAppConfig.java` (the composition root in the bootstrap module). Application service classes carry no Spring annotations (`@Service`, `@Component`, etc.).

```java
@Configuration
@EnableCaching
public class SpringAppConfig {
    @Autowired PortfolioPort portfolioPort;
    @Autowired TransactionPort transactionPort;
    @Autowired StockPriceProviderPort stockPricePort;

    @Bean
    PortfolioManagementUseCase getPortfolioManagementUseCase() {
        return new PortfolioManagementService(portfolioPort, transactionPort);
    }

    @Bean
    PortfolioStockOperationsUseCase getPortfolioStockOperationsUseCase() {
        return new PortfolioStockOperationsService(portfolioPort, stockPricePort, transactionPort);
    }
    // ...
}
```

## Alternatives considered

- **`@Service` on application service classes:** Simpler, fewer files, Spring convention. But it couples application services to Spring, requiring `spring-context` as a compile dependency in the application module. Standard approach in most Spring Boot projects.
- **JSR 330 `@Inject` / `@Named`:** Framework-neutral annotations. Still requires a dependency (`jakarta.inject`) in the application module. Standard alternative.
- **Manual `new` construction in `main()`:** Maximum explicitness but loses Spring's lifecycle management and integration with other auto-configured beans. Standard alternative for non-Spring projects.

## Consequences

**Positive:**
- Application service classes contain only `@Transactional` (from `jakarta.transaction-api`, the standard Jakarta Transactions annotation). They do not carry `@Service` or `@Component`.
- The `application` module has zero Spring dependencies. Transactional demarcation uses the standard Jakarta annotation; Spring recognises it at runtime.
- Wiring is visible in one place (`SpringAppConfig.java`), making the composition root explicit.
- Application services can be unit-tested without a Spring context: just instantiate with mock ports.

**Negative:**
- Adding a new service requires editing `SpringAppConfig.java` in addition to creating the service class.
- New contributors unfamiliar with this pattern may look for `@Service` annotations and be confused by their absence.

## Repository evidence

- `bootstrap/src/main/java/.../config/SpringAppConfig.java`: 5 `@Bean` methods wiring use cases to service implementations
- `PortfolioManagementService.java`: annotated `@Transactional`, no `@Service` annotation
- `PortfolioStockOperationsService.java`: annotated `@Transactional`, no `@Service` annotation
- `application/pom.xml`: depends on standard `jakarta.transaction-api` only, not `spring-tx`, `spring-context` or `spring-beans`
- `CONTRIBUTING.md`: Documents the composition root pattern and warns against adding `@Service` to application services

## Relation to other specifications

- **Gherkin:** Not directly related. The wiring mechanism is invisible to behavioural specifications.
- **OpenAPI:** Not directly related.
- **PlantUML:** Architecture diagrams show ports wired to adapters. `SpringAppConfig.java` is the physical manifestation of this wiring. This ADR explains the mechanism behind the wiring shown in diagrams.
