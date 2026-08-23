package cat.gencat.agaur.hexastock.notifications.adapter.logging;

import cat.gencat.agaur.hexastock.notifications.LoggingNotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationDestinationProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Always-on {@link NotificationDestinationProvider}: every user receives a logging
 * destination so notifications are never silently dropped.
 *
 * <p>Pairs with {@link LoggingNotificationSenderAdapter}. No profile is required, which
 * makes this the safe-by-default channel for tests, local development, and any deployment
 * where Telegram (or future channels) is not configured.</p>
 */
@Component
public class LoggingNotificationDestinationProvider implements NotificationDestinationProvider {

    @Override
    public List<NotificationDestination> destinationsFor(String userId) {
        return List.of(new LoggingNotificationDestination(userId));
    }
}
