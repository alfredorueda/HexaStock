package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractWatchlistQueryPortContractTest {

    protected abstract WatchlistPort watchlistPort();
    protected abstract WatchlistQueryPort queryPort();

    @Test
    @SpecificationRef(value = "US-MS-01.AC-1", level = TestLevel.INTEGRATION, feature = "market-sentinel-price-threshold-alerts.feature")
    @DisplayName("findDistinctTickersInActiveWatchlists returns distinct tickers from active watchlists only")
    protected void distinctTickers_activeOnly() {
        Watchlist active = Watchlist.create(WatchlistId.of("wl-a"), "alice", "Tech");
        active.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
        active.addAlert(Ticker.of("AAPL"), Money.of("140.00"));
        watchlistPort().createWatchlist(active);

        Watchlist inactive = Watchlist.create(WatchlistId.of("wl-i"), "bob", "Other");
        inactive.addAlert(Ticker.of("GOOGL"), Money.of("120.00"));
        inactive.deactivate();
        watchlistPort().createWatchlist(inactive);

        Set<Ticker> tickers = queryPort().findDistinctTickersInActiveWatchlists();
        assertThat(tickers).containsExactlyInAnyOrder(Ticker.of("AAPL"));
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-2", level = TestLevel.INTEGRATION, feature = "market-sentinel-price-threshold-alerts.feature")
    @DisplayName("findTriggeredAlerts returns only triggered alerts (threshold >= currentPrice)")
    protected void triggeredAlerts_filterByThreshold() {
        Watchlist wl = Watchlist.create(WatchlistId.of("wl-t"), "alice", "Tech");
        wl.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
        wl.addAlert(Ticker.of("AAPL"), Money.of("140.00"));
        wl.addAlert(Ticker.of("AAPL"), Money.of("130.00"));
        watchlistPort().createWatchlist(wl);

        List<TriggeredAlertView> triggered = queryPort().findTriggeredAlerts(Ticker.of("AAPL"), Money.of("145.00"));

        assertThat(triggered).hasSize(1);
        assertThat(triggered.getFirst().thresholdPrice()).isEqualTo(Money.of("150.00"));
        assertThat(triggered.getFirst().watchlistId()).isEqualTo("wl-t");
        assertThat(triggered.getFirst().ownerName()).isEqualTo("alice");
        assertThat(triggered.getFirst().listName()).isEqualTo("Tech");
    }
}
