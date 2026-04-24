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
 * Watchlists module remains channel-agnostic.</p>
 */
package cat.gencat.agaur.hexastock.notifications;
