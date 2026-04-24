/**
 * Market Data — published domain types.
 *
 * <p>This subpackage holds value objects that form part of the Market Data
 * module's published API: {@link Ticker}, {@link StockPrice}, and
 * {@link InvalidTickerException}. Other Spring Modulith application modules
 * (Portfolio Management, Watchlists, Notifications) reference these types
 * directly through the named interface declared below.</p>
 *
 * <p>The {@code @NamedInterface} declaration is required because Spring
 * Modulith treats subpackages as <em>internal</em> by default. Without it,
 * cross-module references to {@code Ticker} would fail
 * {@code MODULES.verify()} as "non-exposed type" violations.</p>
 */
@org.springframework.modulith.NamedInterface("model")
package cat.gencat.agaur.hexastock.marketdata.model.market;
