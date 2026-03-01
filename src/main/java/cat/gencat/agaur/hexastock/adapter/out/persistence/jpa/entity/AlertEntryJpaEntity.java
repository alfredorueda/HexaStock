package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "alert_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_ticker_threshold",
                columnNames = {"watchlist_id", "ticker", "threshold_price"})
)
public class AlertEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private WatchlistJpaEntity watchlist;

    private String ticker;

    @Column(name = "threshold_price")
    private BigDecimal thresholdPrice;

    protected AlertEntryJpaEntity() {
    }

    public AlertEntryJpaEntity(WatchlistJpaEntity watchlist, String ticker, BigDecimal thresholdPrice) {
        this.watchlist = watchlist;
        this.ticker = ticker;
        this.thresholdPrice = thresholdPrice;
    }

    public Long getId() {
        return id;
    }

    public WatchlistJpaEntity getWatchlist() {
        return watchlist;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }
}
