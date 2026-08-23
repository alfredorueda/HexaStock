package cat.gencat.agaur.hexastock.adapter.in.telegram.web;

import cat.gencat.agaur.hexastock.adapter.in.telegram.TelegramBotClient;
import cat.gencat.agaur.hexastock.adapter.in.telegram.TelegramCommandHandler;
import cat.gencat.agaur.hexastock.adapter.in.telegram.TelegramCommandParser;
import cat.gencat.agaur.hexastock.application.port.in.WatchlistUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static cat.gencat.agaur.hexastock.adapter.in.telegram.web.TelegramUpdateDTOs.*;

@RestController
@Profile("telegram")
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    private final TelegramCommandHandler handler;
    private final TelegramBotClient telegramBotClient;

    public TelegramWebhookController(WatchlistUseCase watchlistUseCase, TelegramBotClient telegramBotClient) {
        this.handler = new TelegramCommandHandler(watchlistUseCase);
        this.telegramBotClient = telegramBotClient;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> onUpdate(@RequestBody Update update) {
        if (update == null || update.message() == null) {
            return ResponseEntity.ok("ignored");
        }
        String text = update.message().text();
        String chatId = update.message().chat() != null ? update.message().chat().id() : null;
        String ownerName = resolveOwnerName(update);

        var parsed = TelegramCommandParser.parse(text);
        if (parsed.isEmpty()) {
            return ResponseEntity.ok("ignored");
        }

        String response = handler.handle(parsed.get(), ownerName, chatId);
        telegramBotClient.sendMessage(chatId, response);
        return ResponseEntity.ok(response);
    }

    private String resolveOwnerName(Update update) {
        return Optional.ofNullable(update.message())
                .map(Message::from)
                .map(from -> {
                    if (from.username() != null && !from.username().isBlank()) {
                        return from.username();
                    }
                    return from.id() != null ? String.valueOf(from.id()) : "unknown";
                })
                .orElse("unknown");
    }
}

