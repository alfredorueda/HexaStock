package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.AlertEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface JpaWatchlistQuerySpringDataRepository extends JpaRepository<AlertEntryJpaEntity, Long> {

    interface TriggeredAlertRow {
        String getOwnerName();
        String getListName();
        String getuserNotificationId();
        String getTicker();
        BigDecimal getThresholdPrice();
    }

    @Query("""
            select distinct e.ticker
            from AlertEntryJpaEntity e
            join WatchlistJpaEntity w on w.id = e.watchlistId
            where w.active = true
            """)
    Set<String> findDistinctTickersInActiveWatchlists();

    @Query("""
            select w.ownerName as ownerName,
                   w.listName as listName,
                   w.userNotificationId as userNotificationId,
                   e.ticker as ticker,
                   e.thresholdPrice as thresholdPrice
            from AlertEntryJpaEntity e
            join WatchlistJpaEntity w on w.id = e.watchlistId
            where w.active = true
              and e.ticker = :ticker
              and e.thresholdPrice >= :currentPrice
            """)
    List<TriggeredAlertRow> findTriggeredAlerts(@Param("ticker") String ticker,
                                                @Param("currentPrice") BigDecimal currentPrice);
}

