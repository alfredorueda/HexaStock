/**
 * Watchlists / Market Sentinel module — Spring Modulith {@code @ApplicationModule}
 * declaration.
 *
 * <p>The same Java package also has a documentation-only {@code package-info.java}
 * inside the {@code application} Maven module. That copy carries the javadoc and
 * preserves ADR-007's "{@code application} stays Spring-free" constraint. <em>This</em>
 * copy lives in {@code bootstrap} where the {@code spring-modulith} dependency is
 * available, so the {@code @ApplicationModule} annotation can be declared without
 * polluting the application layer.</p>
 *
 * <p>At runtime both {@code package-info.class} files are present on the classpath
 * for the same package, but only this one carries the Spring Modulith annotation.</p>
 *
 * <p>Allowed cross-module dependency: {@code marketdata} — the {@code Ticker} value
 * object is part of the {@code WatchlistAlertTriggeredEvent} payload published by
 * this module.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Watchlists / Market Sentinel",
        allowedDependencies = {"marketdata::model"}
)
package cat.gencat.agaur.hexastock.watchlists;
