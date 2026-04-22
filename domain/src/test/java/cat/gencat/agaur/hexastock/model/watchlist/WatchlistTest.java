package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Watchlist Aggregate Tests")
class WatchlistTest {

    @Nested
    @DisplayName("Create watchlist")
    class CreateWatchlist {

        @Test
        @SpecificationRef(value = "US-WL-01.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-create.feature")
        void shouldCreateActiveWatchlistWithNoAlerts() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            assertTrue(watchlist.isActive());
            assertEquals("alice", watchlist.getOwnerName());
            assertEquals("Tech", watchlist.getListName());
            assertTrue(watchlist.getAlerts().isEmpty());
            assertEquals("123456", watchlist.getTelegramChatId());
        }

        @Test
        @SpecificationRef(value = "US-WL-01.AC-2", level = TestLevel.DOMAIN, feature = "watchlists-create.feature")
        void shouldRejectBlankOwnerName() {
            WatchlistId id = WatchlistId.generate();
            assertThrows(IllegalArgumentException.class, () -> Watchlist.create(id, "", "Tech", "123456"));
        }

        @Test
        @SpecificationRef(value = "US-WL-01.AC-3", level = TestLevel.DOMAIN, feature = "watchlists-create.feature")
        void shouldRejectBlankListName() {
            WatchlistId id = WatchlistId.generate();
            assertThrows(IllegalArgumentException.class, () -> Watchlist.create(id, "alice", "", "123456"));
        }
    }

    @Nested
    @DisplayName("Alert entries")
    class AlertEntries {

        @Test
        @SpecificationRef(value = "US-WL-02.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldAddAlertEntry() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));

            assertEquals(1, watchlist.getAlerts().size());
            assertEquals(Ticker.of("AAPL"), watchlist.getAlerts().getFirst().ticker());
            assertEquals(Money.of("150.00"), watchlist.getAlerts().getFirst().thresholdPrice());
        }

        @Test
        @SpecificationRef(value = "US-WL-02.AC-2", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldRejectNonPositiveThresholdPrice() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            Ticker ticker = Ticker.of("AAPL");
            Money threshold = Money.of("0.00");
            assertThrows(IllegalArgumentException.class, () -> watchlist.addAlert(ticker, threshold));
        }

        @Test
        @SpecificationRef(value = "US-WL-02.AC-3", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldRejectExactDuplicateAlert() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));

            Ticker ticker = Ticker.of("AAPL");
            Money threshold = Money.of("150.00");
            assertThrows(DuplicateAlertException.class, () -> watchlist.addAlert(ticker, threshold));
        }

        @Test
        @SpecificationRef(value = "US-WL-02.AC-4", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldAllowMultipleAlertsForSameTickerAtDifferentThresholds() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("140.00"));
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("130.00"));

            assertEquals(3, watchlist.getAlertsForTicker(Ticker.of("AAPL")).size());
        }

        @Test
        @SpecificationRef(value = "US-WL-02.AC-5", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldRemoveSpecificAlertEntry() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("140.00"));

            watchlist.removeAlert(Ticker.of("AAPL"), Money.of("150.00"));

            assertEquals(1, watchlist.getAlerts().size());
            assertEquals(Money.of("140.00"), watchlist.getAlerts().getFirst().thresholdPrice());
        }

        @Test
        @SpecificationRef(value = "US-WL-02.AC-6", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
        void shouldRemoveAllAlertsForTicker() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("150.00"));
            watchlist.addAlert(Ticker.of("AAPL"), Money.of("140.00"));
            watchlist.addAlert(Ticker.of("GOOGL"), Money.of("120.00"));

            watchlist.removeAllAlertsForTicker(Ticker.of("AAPL"));

            assertTrue(watchlist.getAlertsForTicker(Ticker.of("AAPL")).isEmpty());
            assertEquals(1, watchlist.getAlerts().size());
            assertEquals(Ticker.of("GOOGL"), watchlist.getAlerts().getFirst().ticker());
        }
    }

    @Nested
    @DisplayName("Activation")
    class Activation {

        @Test
        @SpecificationRef(value = "US-WL-03.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-activation.feature")
        void shouldDeactivateWatchlist() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");

            watchlist.deactivate();

            assertFalse(watchlist.isActive());
        }

        @Test
        @SpecificationRef(value = "US-WL-03.AC-2", level = TestLevel.DOMAIN, feature = "watchlists-activation.feature")
        void shouldActivateWatchlist() {
            Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "alice", "Tech", "123456");
            watchlist.deactivate();

            watchlist.activate();

            assertTrue(watchlist.isActive());
        }
    }
}

