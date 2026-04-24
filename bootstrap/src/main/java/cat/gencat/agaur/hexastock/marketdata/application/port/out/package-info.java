/**
 * Market Data — published secondary port (consumed for bean wiring).
 *
 * <p>{@link MarketDataPort} is a secondary (output) port. Strictly per
 * hexagonal/Modulith doctrine other modules should not depend on another
 * module's <em>secondary</em> ports — only on its primary ports
 * ({@code port.in}). However, the current Spring wiring in
 * {@code SpringAppConfig} injects {@code MarketDataPort} into Portfolio
 * services ({@code PortfolioStockOperationsService},
 * {@code ReportingService}) for performance reasons (those services need
 * batch price lookups that the primary port doesn't currently expose).
 * Exposing this port via a named interface lets {@code MODULES.verify()}
 * accept the existing wiring while we evaluate whether to introduce a
 * richer primary port.</p>
 *
 * <p>This is a deliberate, documented exception. A future refactor may
 * promote the batch price lookup to a primary port and remove this
 * exposure.</p>
 */
@org.springframework.modulith.NamedInterface("port-out")
package cat.gencat.agaur.hexastock.marketdata.application.port.out;
