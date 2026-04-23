package cat.gencat.agaur.hexastock.adapter.in.telegram;

import cat.gencat.agaur.hexastock.model.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Profile("telegram")
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.api.base-url:https://api.telegram.org}")
    private String telegramApiBaseUrl;

    private final RestClient restClient = RestClient.create();

    public void sendMessage(String chatId, String text) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        if (botToken == null || botToken.isBlank()) {
            log.warn("Skipping Telegram sendMessage: telegram.bot.token is not configured.");
            return;
        }
        String url = telegramApiBaseUrl + "/bot" + botToken + "/sendMessage";
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new ExternalApiException("Telegram sendMessage failed: " + ex.getMessage(), ex);
        }
    }
}

