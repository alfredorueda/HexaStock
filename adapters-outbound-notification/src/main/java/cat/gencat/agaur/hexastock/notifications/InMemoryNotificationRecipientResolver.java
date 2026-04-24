package cat.gencat.agaur.hexastock.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default in-memory {@link NotificationRecipientResolver}.
 *
 * <p>For each {@code userId} it returns:</p>
 * <ul>
 *   <li>a {@link TelegramNotificationDestination} when the user has a Telegram chat id
 *       configured via the SpEL-bound map property
 *       {@code notifications.telegram.chat-ids={alice:'123',bob:'456'}}; and</li>
 *   <li>always a fallback {@link LoggingNotificationDestination} so notifications are
 *       never silently dropped.</li>
 * </ul>
 *
 * <p>Replace with a persistent implementation (database, configuration service, etc.) by
 * declaring another bean of {@link NotificationRecipientResolver} marked with
 * {@code @Primary}; this default has no {@code @ConditionalOnMissingBean} guard, so
 * overriding requires the {@code @Primary} marker.</p>
 *
 * <p>Properly registering Telegram chat ids "live" (e.g. when a user contacts the bot)
 * is intentionally out of scope for this POC and would be a natural next step using
 * another in-process domain event.</p>
 */
@Component
public class InMemoryNotificationRecipientResolver implements NotificationRecipientResolver {

    private final Map<String, String> telegramChatIdsByUser;

    public InMemoryNotificationRecipientResolver(
            @Value("#{${notifications.telegram.chat-ids:{:}}}") Map<String, String> telegramChatIdsByUser) {
        this.telegramChatIdsByUser = telegramChatIdsByUser == null
                ? Map.of()
                : Map.copyOf(telegramChatIdsByUser);
    }

    @Override
    public NotificationRecipient resolve(String userId) {
        List<NotificationDestination> destinations = new ArrayList<>();
        String chatId = telegramChatIdsByUser.get(userId);
        if (chatId != null && !chatId.isBlank()) {
            destinations.add(new TelegramNotificationDestination(chatId));
        }
        // Logging destination is always added so there is an audit trail and a guaranteed
        // delivery channel in tests and local profiles.
        destinations.add(new LoggingNotificationDestination(userId));
        return new NotificationRecipient(userId, List.copyOf(destinations));
    }
}
