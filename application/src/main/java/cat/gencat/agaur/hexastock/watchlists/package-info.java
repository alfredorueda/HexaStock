/**
 * Watchlists / Market Sentinel module — published API.
 *
 * <p>This package is the published API surface of the Watchlists Spring Modulith module.
 * It contains domain events emitted when alert conditions are detected. Other modules
 * (notably {@code cat.gencat.agaur.hexastock.notifications}) MAY depend on these events.</p>
 *
 * <p>The Watchlists module itself MUST NOT depend on the Notifications module: routing
 * notifications to specific channels (Telegram, email, etc.) is the responsibility of
 * the Notifications bounded context.</p>
 */
package cat.gencat.agaur.hexastock.watchlists;
