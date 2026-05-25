package cat.gencat.agaur.hexastock.adapter.in.telegram;

import cat.gencat.agaur.hexastock.model.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Registers the bot command menu (Telegram setMyCommands) so users see suggestions
 * when typing '/' in Telegram.
 */
@Component
@Profile("telegram")
public class TelegramBotCommandsRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotCommandsRegistrar.class);

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.api.base-url:https://api.telegram.org}")
    private String telegramApiBaseUrl;

    private final RestClient restClient = RestClient.create();

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Skipping Telegram setMyCommands: telegram.bot.token is not configured.");
            return;
        }

        String url = telegramApiBaseUrl + "/bot" + botToken + "/setMyCommands";

        List<Map<String, String>> commands = List.of(
                cmd("watchlist_create", "Crear watchlist: /watchlist_create <listName>"),
                cmd("watchlist_delete", "Borrar watchlist: /watchlist_delete <watchlistId>"),
                cmd("watchlist_activate", "Activar watchlist: /watchlist_activate <watchlistId>"),
                cmd("watchlist_deactivate", "Desactivar watchlist: /watchlist_deactivate <watchlistId>"),
                cmd("alert_add", "Añadir alerta: /alert_add <watchlistId> <TICKER> <thresholdPrice>"),
                cmd("alert_remove", "Eliminar alerta: /alert_remove <watchlistId> <TICKER> <thresholdPrice>"),
                cmd("alert_remove_all", "Eliminar alertas ticker: /alert_remove_all <watchlistId> <TICKER>")
        );

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("commands", commands))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram bot commands registered (setMyCommands).");
        } catch (Exception ex) {
            throw new ExternalApiException("Telegram setMyCommands failed: " + ex.getMessage(), ex);
        }
    }

    private static Map<String, String> cmd(String command, String description) {
        return Map.of("command", command, "description", description);
    }
}

