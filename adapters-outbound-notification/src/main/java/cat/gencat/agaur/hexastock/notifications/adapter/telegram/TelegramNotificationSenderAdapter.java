package cat.gencat.agaur.hexastock.notifications.adapter.telegram;

import cat.gencat.agaur.hexastock.notifications.NotificationDestination;
import cat.gencat.agaur.hexastock.notifications.NotificationSender;
import cat.gencat.agaur.hexastock.notifications.TelegramNotificationDestination;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Telegram {@link NotificationSender}.
 *
 * <p>Activated only when the {@code telegram-notifications} profile is enabled, so the
 * default deployment, all tests, and local development run without any Telegram dependency
 * and without requiring a bot token.</p>
 */
@Component
@Profile("telegram-notifications")
public class TelegramNotificationSenderAdapter implements NotificationSender {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.api.base-url:https://api.telegram.org}")
    private String telegramApiBaseUrl;

    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(NotificationDestination destination) {
        return destination instanceof TelegramNotificationDestination;
    }

    @Override
    public void send(NotificationDestination destination, WatchlistAlertTriggeredEvent event) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("telegram.bot.token must be configured");
        }
        TelegramNotificationDestination dest = (TelegramNotificationDestination) destination;

        String url = telegramApiBaseUrl + "/bot" + botToken + "/sendMessage";
        String text = formatMessage(event);

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", dest.chatId(),
                        "text", text
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String formatMessage(WatchlistAlertTriggeredEvent event) {
        return "BUY SIGNAL: %s — threshold %s reached for %s (current=%s)"
                .formatted(event.userId(), event.threshold(), event.ticker().value(), event.currentPrice());
    }
}
