package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.AlertEntryJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.WatchlistJpaEntity;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.AlertEntry;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;

import java.util.List;

public final class WatchlistMapper {

    private WatchlistMapper() {}

    public static Watchlist toModelEntity(WatchlistJpaEntity jpa) {
        Watchlist model = Watchlist.create(
                WatchlistId.of(jpa.getId()),
                jpa.getOwnerName(),
                jpa.getListName(),
                jpa.getTelegramChatId()
        );
        if (!jpa.isActive()) {
            model.deactivate();
        }
        for (AlertEntryJpaEntity entry : jpa.getAlerts()) {
            model.addAlert(Ticker.of(entry.getTicker()), Money.of(entry.getThresholdPrice()));
        }
        return model;
    }

    public static WatchlistJpaEntity toJpaEntity(Watchlist model) {
        WatchlistJpaEntity jpa = new WatchlistJpaEntity(
                model.getId().value(),
                model.getOwnerName(),
                model.getListName(),
                model.isActive(),
                model.getUserNotificationId()
        );
        List<AlertEntryJpaEntity> alerts = model.getAlerts().stream()
                .map(WatchlistMapper::toJpaAlertEntry)
                .toList();
        jpa.setAlerts(alerts);
        return jpa;
    }

    private static AlertEntryJpaEntity toJpaAlertEntry(AlertEntry entry) {
        return new AlertEntryJpaEntity(entry.ticker().value(), entry.thresholdPrice().amount());
    }
}

