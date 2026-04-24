package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "alert_entry")
public class AlertEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "watchlist_id", insertable = false, updatable = false)
    private String watchlistId;

    private String ticker;

    private BigDecimal thresholdPrice;

    protected AlertEntryJpaEntity() {}

    public AlertEntryJpaEntity(String ticker, BigDecimal thresholdPrice) {
        this.ticker = ticker;
        this.thresholdPrice = thresholdPrice;
    }

    public Long getId() {
        return id;
    }

    public String getWatchlistId() {
        return watchlistId;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }
}

