package cat.gencat.agaur.hexastock.notifications.adapter.telegram;

import cat.gencat.agaur.hexastock.notifications.NotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationDestinationProvider;
import cat.gencat.agaur.hexastock.notifications.TelegramNotificationDestination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Profile-gated {@link NotificationDestinationProvider} for Telegram.
 *
 * <p>Only registered when the {@code telegram-notifications} Spring profile is active —
 * exactly the same activation contract as {@link TelegramNotificationSenderAdapter}.
 * When the profile is off, no Telegram destinations are produced and no
 * {@code chat-ids} configuration is consulted, which prevents the
 * "no sender available for channel TELEGRAM" warning that would otherwise be logged
 * by the listener whenever an alert fires.</p>
 *
 * <p>Chat ids are bound from the SpEL-mapped property
 * {@code notifications.telegram.chat-ids={alice:'123',bob:'456'}}. Production
 * deployments will replace this in-memory mapping with a persistent lookup; that
 * substitution is a {@code @Primary} bean swap and requires no change here.</p>
 */
@Component
@Profile("telegram-notifications")
public class TelegramNotificationDestinationProvider implements NotificationDestinationProvider {

    private final Map<String, String> telegramChatIdsByUser;

    public TelegramNotificationDestinationProvider(
            @Value("#{${notifications.telegram.chat-ids:{:}}}") Map<String, String> telegramChatIdsByUser) {
        this.telegramChatIdsByUser = telegramChatIdsByUser == null
                ? Map.of()
                : Map.copyOf(telegramChatIdsByUser);
    }

    @Override
    public List<NotificationDestination> destinationsFor(String userId) {
        String chatId = telegramChatIdsByUser.get(userId);
        if (chatId == null || chatId.isBlank()) {
            return List.of();
        }
        return List.of(new TelegramNotificationDestination(chatId));
    }
}
