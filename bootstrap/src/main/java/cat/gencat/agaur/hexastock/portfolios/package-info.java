/**
 * Portfolio Management module — Spring Modulith application module.
 *
 * <p>Owns the Portfolio aggregate, FIFO Trading orchestration, and the
 * append-only Transaction ledger. See
 * {@code doc/architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md §1} for the
 * full inventory and the rationale for keeping these three closely related
 * concerns inside a single Modulith module.</p>
 *
 * <h2>Module shape</h2>
 * <p>The {@code portfolios} package spans every Maven module of the build:</p>
 * <ul>
 *   <li>{@code domain} — {@code portfolios.model.portfolio.*},
 *       {@code portfolios.model.transaction.*}.</li>
 *   <li>{@code application} — {@code portfolios.application.service.*},
 *       {@code portfolios.application.port.in.*},
 *       {@code portfolios.application.port.out.*},
 *       {@code portfolios.application.exception.*}.</li>
 *   <li>{@code adapters-inbound-rest} —
 *       {@code portfolios.adapter.in.*}.</li>
 *   <li>{@code adapters-outbound-persistence-jpa} —
 *       {@code portfolios.adapter.out.persistence.jpa.*}.</li>
 *   <li>{@code adapters-outbound-persistence-mongodb} —
 *       {@code portfolios.adapter.out.persistence.mongodb.*}.</li>
 * </ul>
 *
 * <h2>Why the {@code @ApplicationModule} annotation lives here</h2>
 * <p>The natural locations ({@code domain} and {@code application}) are
 * deliberately Spring-free per ADR-007. The {@code bootstrap} Maven module
 * already aggregates Spring configuration for the whole application, so
 * declaring the portfolios Modulith boundary here keeps the constraint
 * intact while still letting Spring Modulith discover the module via
 * classpath scanning of {@code cat.gencat.agaur.hexastock.portfolios}.</p>
 *
 * <h2>Allowed cross-module dependencies</h2>
 * <p>Portfolios does not depend on {@code watchlists} or {@code notifications}.
 * It does depend on {@code marketdata} through the
 * {@code MarketDataPort} secondary port (used by
 * {@code PortfolioStockOperationsService} and {@code ReportingService} to
 * fetch current stock prices) and through the {@code Ticker} /
 * {@code StockPrice} value objects. That dependency is declared
 * explicitly via {@code allowedDependencies = {"marketdata"}} so
 * Spring Modulith's {@code MODULES.verify()} can enforce the boundary.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Portfolio Management",
        allowedDependencies = {"marketdata::model", "marketdata::port-out"}
)
package cat.gencat.agaur.hexastock.portfolios;
