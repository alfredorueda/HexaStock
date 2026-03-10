package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.AlertEntryJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.WatchlistJpaEntity;
import cat.gencat.agaur.hexastock.model.AlertEntry;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;

import java.util.List;

public class WatchlistMapper {

    public static Watchlist toModelEntity(WatchlistJpaEntity jpaEntity) {
        return new Watchlist(
                WatchlistId.of(jpaEntity.getId()),
                jpaEntity.getOwnerName(),
                jpaEntity.getListName(),
                jpaEntity.isActive(),
                jpaEntity.getAlerts().stream()
                        .map(WatchlistMapper::toModelAlertEntry)
                        .toList()
        );
    }

    public static WatchlistJpaEntity toJpaEntity(Watchlist watchlist) {
        WatchlistJpaEntity watchlistJpaEntity = new WatchlistJpaEntity(
                watchlist.getId().value(),
                watchlist.getOwnerName(),
                watchlist.getListName(),
                watchlist.isActive()
        );

        List<AlertEntryJpaEntity> alertEntries = watchlist.getAlerts().stream()
                .map(alert -> toJpaAlertEntry(watchlistJpaEntity, alert))
                .toList();
        watchlistJpaEntity.setAlerts(alertEntries);
        return watchlistJpaEntity;
    }

    private static AlertEntry toModelAlertEntry(AlertEntryJpaEntity jpaEntity) {
        return new AlertEntry(
                Ticker.of(jpaEntity.getTicker()),
                Money.of(jpaEntity.getThresholdPrice())
        );
    }

    private static AlertEntryJpaEntity toJpaAlertEntry(WatchlistJpaEntity watchlistJpaEntity, AlertEntry alertEntry) {
        return new AlertEntryJpaEntity(
                watchlistJpaEntity,
                alertEntry.ticker().value(),
                alertEntry.thresholdPrice().amount()
        );
    }
}
