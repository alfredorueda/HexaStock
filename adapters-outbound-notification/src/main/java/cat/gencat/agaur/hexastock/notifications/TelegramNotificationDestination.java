package cat.gencat.agaur.hexastock.notifications;

import java.util.Objects;

/**
 * Telegram-specific {@link NotificationDestination}.
 *
 * @param chatId Telegram chat id (private chat with the user, group, or channel).
 */
public record TelegramNotificationDestination(String chatId) implements NotificationDestination {

    public TelegramNotificationDestination {
        Objects.requireNonNull(chatId, "chatId is required");
        if (chatId.isBlank()) {
            throw new IllegalArgumentException("chatId must not be blank");
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.TELEGRAM;
    }
}
