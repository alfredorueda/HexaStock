package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.AlertEntry;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;

import java.util.List;

public final class WatchlistDTOs {

    private WatchlistDTOs() {}

    public record CreateWatchlistRequestDTO(String ownerName, String listName, String userNotificationId) {}

    public record AlertEntryRequestDTO(String ticker, String thresholdPrice) {}

    public record WatchlistResponseDTO(
            String id,
            String ownerName,
            String listName,
            boolean active,
            String userNotificationId,
            List<AlertEntryResponseDTO> alerts
    ) {
        public static WatchlistResponseDTO from(Watchlist w) {
            return new WatchlistResponseDTO(
                    w.getId().value(),
                    w.getOwnerName(),
                    w.getListName(),
                    w.isActive(),
                    w.getUserNotificationId(),
                    w.getAlerts().stream().map(AlertEntryResponseDTO::from).toList()
            );
        }
    }

    public record AlertEntryResponseDTO(String ticker, String thresholdPrice) {
        public static AlertEntryResponseDTO from(AlertEntry entry) {
            return new AlertEntryResponseDTO(entry.ticker().value(), entry.thresholdPrice().toString());
        }
    }

    public static Ticker toTicker(String value) {
        return Ticker.of(value);
    }

    public static Money toMoney(String value) {
        return Money.of(value);
    }
}

