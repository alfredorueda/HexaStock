package cat.gencat.agaur.hexastock.notifications.adapter.logging;

import cat.gencat.agaur.hexastock.notifications.LoggingNotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationSender;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link NotificationSender}: writes a structured SLF4J line per alert.
 *
 * <p>Always active — no profile required. This is the safe-by-default channel for
 * tests, local development, and any deployment where Telegram is not configured.</p>
 */
@Component
public class LoggingNotificationSenderAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSenderAdapter.class);

    @Override
    public boolean supports(NotificationDestination destination) {
        return destination instanceof LoggingNotificationDestination;
    }

    @Override
    public void send(NotificationDestination destination, WatchlistAlertTriggeredEvent event) {
        LoggingNotificationDestination dest = (LoggingNotificationDestination) destination;
        log.info("WATCHLIST_ALERT user={} watchlist={} ticker={} type={} threshold={} current={} occurredOn={} message={}",
                dest.userId(),
                event.watchlistId(),
                event.ticker().value(),
                event.alertType(),
                event.threshold(),
                event.currentPrice(),
                event.occurredOn(),
                event.message());
    }
}
