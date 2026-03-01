package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.AlertEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Repository
public interface JpaAlertEntrySpringDataRepository extends JpaRepository<AlertEntryJpaEntity, Long> {

    @Query("""
            select distinct ae.ticker
            from AlertEntryJpaEntity ae
            join ae.watchlist w
            where w.active = true
            """)
    Set<String> findDistinctTickersInActiveWatchlists();

    @Query("""
            select w.ownerName as ownerName,
                   w.listName as listName,
                   ae.ticker as ticker,
                   ae.thresholdPrice as thresholdPrice
            from AlertEntryJpaEntity ae
            join ae.watchlist w
            where w.active = true
              and ae.ticker = :ticker
              and ae.thresholdPrice >= :currentPrice
            """)
    List<TriggeredAlertProjection> findTriggeredAlerts(@Param("ticker") String ticker,
                                                       @Param("currentPrice") BigDecimal currentPrice);

    interface TriggeredAlertProjection {
        String getOwnerName();

        String getListName();

        String getTicker();

        BigDecimal getThresholdPrice();
    }
}
