package cat.gencat.agaur.hexastock.notifications;

import java.util.Objects;

/**
 * A single user's preference for a given {@link NotificationChannel}, holding the
 * channel-specific address (Telegram chat id, e-mail address, phone number, ...).
 *
 * <p>This is the persistence model the Notifications module would store; in the POC
 * a thin in-memory implementation is provided.</p>
 */
public record UserNotificationPreference(String userId,
                                         NotificationChannel channel,
                                         String address) {

    public UserNotificationPreference {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(channel, "channel is required");
        Objects.requireNonNull(address, "address is required");
        if (address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
    }

    public NotificationDestination toDestination() {
        return switch (channel) {
            case LOG -> new LoggingNotificationDestination(userId);
            case TELEGRAM -> new TelegramNotificationDestination(address);
        };
    }
}
