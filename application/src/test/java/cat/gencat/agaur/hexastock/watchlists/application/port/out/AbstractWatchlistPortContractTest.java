package cat.gencat.agaur.hexastock.watchlists.application.port.out;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractWatchlistPortContractTest {

    protected abstract WatchlistPort port();

    @Test
    @SpecificationRef(value = "US-WL-01.AC-1", level = TestLevel.INTEGRATION, feature = "watchlists-create.feature")
    @DisplayName("create and retrieve watchlist preserves scalar fields")
    protected void createAndGetById_roundTrip() {
        Watchlist watchlist = Watchlist.create(WatchlistId.of("wl-1"), "alice", "Tech");
        port().createWatchlist(watchlist);

        Watchlist found = port().getWatchlistById(WatchlistId.of("wl-1")).orElseThrow();
        assertThat(found.getId()).isEqualTo(WatchlistId.of("wl-1"));
        assertThat(found.getOwnerName()).isEqualTo("alice");
        assertThat(found.getListName()).isEqualTo("Tech");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getAlerts()).isEmpty();
    }

    @Test
    @SpecificationRef(value = "US-WL-02.AC-4", level = TestLevel.INTEGRATION, feature = "watchlists-alerts.feature")
    @DisplayName("saveWatchlist persists multiple alerts for same ticker")
    protected void saveWatchlist_persistsAlerts() {
        Watchlist watchlist = Watchlist.create(WatchlistId.of("wl-2"), "alice", "Tech");
        watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
        watchlist.addAlert(Ticker.of("AAPL"), Money.of("140.00"));
        port().createWatchlist(watchlist);

        Watchlist found = port().getWatchlistById(WatchlistId.of("wl-2")).orElseThrow();
        assertThat(found.getAlertsForTicker(Ticker.of("AAPL"))).hasSize(2);
    }

    @Test
    @DisplayName("deleteWatchlist removes it")
    protected void deleteWatchlist_removes() {
        Watchlist watchlist = Watchlist.create(WatchlistId.of("wl-3"), "alice", "Tech");
        port().createWatchlist(watchlist);

        port().deleteWatchlist(WatchlistId.of("wl-3"));

        assertThat(port().getWatchlistById(WatchlistId.of("wl-3"))).isEmpty();
    }
}
