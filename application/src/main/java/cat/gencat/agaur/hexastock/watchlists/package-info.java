/**
 * Watchlists / Market Sentinel module — published API.
 *
 * <p>This package is the published API surface of the Watchlists Spring Modulith
 * application module. It contains domain events emitted when alert conditions are
 * detected by the Market Sentinel scheduler. Other modules (notably
 * {@code cat.gencat.agaur.hexastock.notifications}) MAY depend on these events.</p>
 *
 * <p>The Watchlists module is intentionally a pure publisher: it knows nothing about
 * notification channels, persistence implementations, or downstream consumers.
 * Routing notifications to specific channels (Telegram, email, etc.) is the
 * responsibility of the Notifications bounded context.</p>
 *
 * <p><b>Why no {@code @ApplicationModule} annotation here?</b> The {@code application}
 * Maven module is intentionally kept Spring-free (see ADR-007 and the architecture
 * specification). The Spring Modulith {@code @ApplicationModule} annotation lives in
 * {@code org.springframework.modulith} and would force a Spring API dependency onto
 * the application layer. The boundary is therefore declared on the consumer side
 * ({@code notifications}) via {@code allowedDependencies = "watchlists"}, and Spring
 * Modulith still detects this package as the {@code watchlists} module by virtue of
 * its top-level package position under {@code cat.gencat.agaur.hexastock}.</p>
 *
 * <p>The Watchlist aggregate itself, its ports, and its use case services still live
 * under the legacy {@code model.watchlist}, {@code application.port.*}, and
 * {@code application.service} packages and remain there until a future phase of the
 * Spring Modulith refactoring extracts them into this module.</p>
 *
 * <p>Allowed Modulith dependency: {@code marketdata} — the {@code Ticker} value
 * object travels inside {@code WatchlistAlertTriggeredEvent} as part of the
 * published event payload. The {@code @ApplicationModule} annotation lives in
 * the {@code bootstrap} module's mirror package
 * ({@link cat.gencat.agaur.hexastock.watchlists}) to keep the {@code application}
 * Maven module Spring-free per ADR-007. See
 * {@link cat.gencat.agaur.hexastock.notifications} for an analogous arrangement.</p>
 */
package cat.gencat.agaur.hexastock.watchlists;
