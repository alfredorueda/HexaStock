package cat.gencat.agaur.hexastock.notifications;

import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;

/**
 * Outbound port of the Notifications module: a strategy for delivering an event-derived
 * notification to a single {@link NotificationDestination}.
 *
 * <p>Implementations are picked by the {@code WatchlistAlertNotificationListener} based on
 * {@link #supports(NotificationDestination)}.</p>
 */
public interface NotificationSender {

    boolean supports(NotificationDestination destination);

    void send(NotificationDestination destination, WatchlistAlertTriggeredEvent event);
}
