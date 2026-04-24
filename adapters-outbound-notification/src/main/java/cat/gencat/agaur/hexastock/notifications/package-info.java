/**
 * Notifications module — Spring Modulith application module.
 *
 * <p>Listens to {@link cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent}
 * published by the Watchlists / Market Sentinel module and dispatches user-facing
 * notifications through pluggable {@link cat.gencat.agaur.hexastock.notifications.NotificationSender}
 * adapters.</p>
 *
 * <p>Default sender is {@code LoggingNotificationSenderAdapter}. The Telegram sender is
 * activated only by the {@code telegram-notifications} Spring profile.</p>
 *
 * <p>This module is the only one allowed to know about notification channels. The
 * Watchlists module remains channel-agnostic and depends on nothing.</p>
 *
 * <p>Allowed Modulith dependencies:</p>
 * <ul>
 *   <li>{@code watchlists} — for the published {@code WatchlistAlertTriggeredEvent} type.</li>
 *   <li>{@code marketdata} — for the {@code Ticker} value object carried inside the event
 *       payload and rendered into outbound notification messages.</li>
 * </ul>
 * <p>Channel-specific adapters live under the internal {@code adapter.telegram} and
 * {@code adapter.logging} subpackages, which are not exported as a named interface
 * and therefore not consumable by any other module.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {"watchlists", "marketdata::model"}
)
package cat.gencat.agaur.hexastock.notifications;
