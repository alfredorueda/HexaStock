package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.AlertEntryDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.WatchlistDocument;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.AlertEntry;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;

import java.util.List;

public final class WatchlistDocumentMapper {

    private WatchlistDocumentMapper() {}

    public static Watchlist toModelEntity(WatchlistDocument doc) {
        Watchlist model = Watchlist.create(
                WatchlistId.of(doc.getId()),
                doc.getOwnerName(),
                doc.getListName(),
                doc.getUserNotificationId()
        );
        if (!doc.isActive()) {
            model.deactivate();
        }
        for (AlertEntryDocument entry : doc.getAlerts()) {
            model.addAlert(Ticker.of(entry.getTicker()), Money.of(entry.getThresholdPrice()));
        }
        return model;
    }

    public static WatchlistDocument toDocument(Watchlist model) {
        List<AlertEntryDocument> alerts = model.getAlerts().stream()
                .map(WatchlistDocumentMapper::toDocument)
                .toList();
        return new WatchlistDocument(
                model.getId().value(),
                model.getOwnerName(),
                model.getListName(),
                model.isActive(),
                model.getUserNotificationId(),
                alerts
        );
    }

    private static AlertEntryDocument toDocument(AlertEntry entry) {
        return new AlertEntryDocument(entry.ticker().value(), entry.thresholdPrice().amount());
    }
}

