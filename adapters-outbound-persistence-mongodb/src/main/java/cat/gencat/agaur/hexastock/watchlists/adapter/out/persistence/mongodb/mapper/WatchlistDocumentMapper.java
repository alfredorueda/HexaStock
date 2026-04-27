package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.document.AlertEntryDocument;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.document.WatchlistDocument;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.AlertEntry;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;

import java.util.List;

public final class WatchlistDocumentMapper {

    private WatchlistDocumentMapper() {}

    public static Watchlist toModelEntity(WatchlistDocument doc) {
        List<AlertEntry> alerts = doc.getAlerts().stream()
                .map(e -> new AlertEntry(Ticker.of(e.getTicker()), Money.of(e.getThresholdPrice())))
                .toList();
        return Watchlist.rehydrate(
                WatchlistId.of(doc.getId()),
                doc.getOwnerName(),
                doc.getListName(),
                doc.isActive(),
                alerts
        );
    }

    public static WatchlistDocument toDocument(Watchlist model) {
        return toDocument(model, null);
    }

    public static WatchlistDocument toDocument(Watchlist model, Long version) {
        List<AlertEntryDocument> alerts = model.getAlerts().stream()
                .map(WatchlistDocumentMapper::toDocument)
                .toList();
        return new WatchlistDocument(
                model.getId().value(),
                model.getOwnerName(),
                model.getListName(),
                model.isActive(),
                alerts,
                version
        );
    }

    private static AlertEntryDocument toDocument(AlertEntry entry) {
        return new AlertEntryDocument(entry.ticker().value(), entry.thresholdPrice().amount());
    }
}
