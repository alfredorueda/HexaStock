package cat.gencat.agaur.hexastock.adapter.in.telegram;

import java.util.Optional;

public final class TelegramCommandParser {

    private TelegramCommandParser() {}

    public static Optional<TelegramCommand> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("/")) {
            return Optional.empty();
        }

        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0];

        return switch (cmd) {
            case "/watchlist_create" -> parseCreateWatchlist(parts);
            case "/watchlist_delete" -> parseSingleId(parts, TelegramCommand.DeleteWatchlist::new);
            case "/watchlist_activate" -> parseSingleId(parts, TelegramCommand.ActivateWatchlist::new);
            case "/watchlist_deactivate" -> parseSingleId(parts, TelegramCommand.DeactivateWatchlist::new);
            case "/alert_add" -> parseAlert3(parts, TelegramCommand.AddAlert::new);
            case "/alert_remove" -> parseAlert3(parts, TelegramCommand.RemoveAlert::new);
            case "/alert_remove_all" -> parseAlert2(parts, TelegramCommand.RemoveAllAlerts::new);
            default -> Optional.empty();
        };
    }

    private static Optional<TelegramCommand> parseCreateWatchlist(String[] parts) {
        if (parts.length < 2) {
            return Optional.empty();
        }
        String listName = join(parts, 1);
        return Optional.of(new TelegramCommand.CreateWatchlist(listName));
    }

    private static <T extends TelegramCommand> Optional<TelegramCommand> parseSingleId(
            String[] parts,
            java.util.function.Function<String, T> ctor
    ) {
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(ctor.apply(parts[1]));
    }

    private static <T extends TelegramCommand> Optional<TelegramCommand> parseAlert3(
            String[] parts,
            TriFunction<String, String, String, T> ctor
    ) {
        if (parts.length != 4) {
            return Optional.empty();
        }
        return Optional.of(ctor.apply(parts[1], parts[2], parts[3]));
    }

    private static <T extends TelegramCommand> Optional<TelegramCommand> parseAlert2(
            String[] parts,
            BiFunction<String, String, T> ctor
    ) {
        if (parts.length != 3) {
            return Optional.empty();
        }
        return Optional.of(ctor.apply(parts[1], parts[2]));
    }

    private static String join(String[] parts, int startIdx) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i < parts.length; i++) {
            if (i > startIdx) sb.append(' ');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface BiFunction<A, B, R> {
        R apply(A a, B b);
    }
}

