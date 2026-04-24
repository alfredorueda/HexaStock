/**
 * Market Data module — Spring Modulith application module.
 *
 * <p>Owns the {@code Ticker} and {@code StockPrice} value objects, the
 * {@code GetStockPriceUseCase} primary port, the {@code MarketDataPort}
 * secondary port (formerly {@code StockPriceProviderPort}), the
 * {@code GetStockPriceService} application service, the
 * {@code StockRestController} primary REST adapter, and the Finnhub /
 * Alpha Vantage / mock outbound adapters. See
 * {@code doc/architecture/MODULITH-BOUNDED-CONTEXT-INVENTORY.md §2} for
 * the full inventory.</p>
 *
 * <h2>Module shape</h2>
 * <p>The {@code marketdata} package spans every Maven module that holds
 * Market Data code:</p>
 * <ul>
 *   <li>{@code domain} — {@code marketdata.model.market.*}
 *       (Ticker, StockPrice, InvalidTickerException).</li>
 *   <li>{@code application} — {@code marketdata.application.port.in.*},
 *       {@code marketdata.application.port.out.*},
 *       {@code marketdata.application.service.*}.</li>
 *   <li>{@code adapters-inbound-rest} —
 *       {@code marketdata.adapter.in.*}
 *       (StockRestController, StockPriceDTO).</li>
 *   <li>{@code adapters-outbound-market} —
 *       {@code marketdata.adapter.out.rest.*}
 *       (FinhubStockPriceAdapter, AlphaVantageStockPriceAdapter,
 *       MockFinhubStockPriceAdapter).</li>
 * </ul>
 *
 * <h2>Why the {@code @ApplicationModule} annotation lives here</h2>
 * <p>Same reason as {@code portfolios}: {@code domain} and {@code application}
 * Maven modules are deliberately Spring-free per ADR-007. The
 * {@code bootstrap} Maven module already aggregates Spring configuration,
 * so declaring the marketdata Modulith boundary here preserves the
 * constraint while still letting Spring Modulith discover the module via
 * classpath scanning of {@code cat.gencat.agaur.hexastock.marketdata}.</p>
 *
 * <h2>Allowed cross-module dependencies</h2>
 * <p>Market Data is a leaf module — it does not depend on
 * {@code portfolios}, {@code watchlists}, or {@code notifications}.
 * Conversely, both {@code portfolios} and (eventually) {@code watchlists}
 * depend on Market Data through {@code MarketDataPort} and the
 * {@code Ticker} / {@code StockPrice} value objects.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Market Data",
        allowedDependencies = {}
)
package cat.gencat.agaur.hexastock.marketdata;
