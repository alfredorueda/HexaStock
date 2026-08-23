package cat.gencat.agaur.hexastock.notifications;

import java.util.Objects;

/**
 * Default {@link NotificationDestination} backed by SLF4J logging.
 *
 * <p>Used when no channel-specific destination has been configured for a user, and as
 * the safe default for tests and local development.</p>
 */
public record LoggingNotificationDestination(String userId) implements NotificationDestination {

    public LoggingNotificationDestination {
        Objects.requireNonNull(userId, "userId is required");
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }
}
