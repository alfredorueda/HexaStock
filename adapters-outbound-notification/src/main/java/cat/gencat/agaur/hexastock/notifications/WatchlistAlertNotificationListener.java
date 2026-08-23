package cat.gencat.agaur.hexastock.notifications;

import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Modulith listener that bridges the Watchlists module to the Notifications module
 * over an in-process domain event.
 *
 * <p>Uses {@link ApplicationModuleListener}, which is functionally equivalent to
 * {@code @TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)}.
 * That guarantees notifications are dispatched only after the publishing transaction has
 * successfully committed — an important integrity property for any "side-effect after
 * persistence" use case.</p>
 */
@Component
public class WatchlistAlertNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(WatchlistAlertNotificationListener.class);

    private final NotificationRecipientResolver recipientResolver;
    private final List<NotificationSender> senders;

    public WatchlistAlertNotificationListener(NotificationRecipientResolver recipientResolver,
                                              List<NotificationSender> senders) {
        this.recipientResolver = recipientResolver;
        this.senders = List.copyOf(senders);
    }

    @ApplicationModuleListener
    public void on(WatchlistAlertTriggeredEvent event) {
        NotificationRecipient recipient = recipientResolver.resolve(event.userId());
        for (NotificationDestination destination : recipient.destinations()) {
            NotificationSender sender = pickSender(destination);
            if (sender == null) {
                log.warn("No NotificationSender available for channel {} (user={}, ticker={})",
                        destination.channel(), event.userId(), event.ticker().value());
                continue;
            }
            sender.send(destination, event);
        }
    }

    private NotificationSender pickSender(NotificationDestination destination) {
        for (NotificationSender s : senders) {
            if (s.supports(destination)) {
                return s;
            }
        }
        return null;
    }
}
