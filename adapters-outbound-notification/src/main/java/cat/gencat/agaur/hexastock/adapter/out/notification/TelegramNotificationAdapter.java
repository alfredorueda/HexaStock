package cat.gencat.agaur.hexastock.adapter.out.notification;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import cat.gencat.agaur.hexastock.model.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Profile("telegram")
public class TelegramNotificationAdapter implements NotificationPort {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.api.base-url:https://api.telegram.org}")
    private String telegramApiBaseUrl;

    private final RestClient restClient = RestClient.create();

    @Override
    public void notifyBuySignal(BuySignal signal) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("telegram.bot.token must be configured");
        }
        if (signal.telegramChatId() == null || signal.telegramChatId().isBlank()) {
            throw new IllegalArgumentException("telegramChatId is required to send Telegram notification");
        }

        String url = telegramApiBaseUrl + "/bot" + botToken + "/sendMessage";
        String text = formatMessage(signal);

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", signal.telegramChatId(),
                            "text", text
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new ExternalApiException("Telegram notification failed: " + ex.getMessage(), ex);
        }
    }

    private String formatMessage(BuySignal signal) {
        return "BUY SIGNAL: %s should consider buying %s (watchlist: %s, threshold: %s, current: %s)"
                .formatted(
                        signal.ownerName(),
                        signal.ticker().value(),
                        signal.listName(),
                        signal.thresholdPrice(),
                        signal.currentPrice().price().toMoney()
                );
    }
}

