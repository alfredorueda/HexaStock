package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.AlertNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.DuplicateAlertException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Watchlist Aggregate Tests")
class WatchlistTest {

    @Test
    @DisplayName("should reject negative threshold price")
    void shouldRejectNegativeThresholdPrice() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");

        assertThrows(IllegalArgumentException.class, () ->
                watchlist.addAlert(Ticker.of("AAPL"), Money.of(new BigDecimal("-10.00"))));
    }

    @Test
    @DisplayName("should reject exact duplicate alert")
    void shouldRejectExactDuplicateAlert() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        Ticker aapl = Ticker.of("AAPL");
        Money threshold = Money.of(new BigDecimal("150.00"));

        watchlist.addAlert(aapl, threshold);

        assertThrows(DuplicateAlertException.class, () -> watchlist.addAlert(aapl, threshold));
    }

    @Test
    @DisplayName("should allow multiple alerts for same ticker at different prices")
    void shouldAllowMultipleAlertsForSameTickerAtDifferentPrices() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        Ticker aapl = Ticker.of("AAPL");

        watchlist.addAlert(aapl, Money.of(new BigDecimal("150.00")));
        watchlist.addAlert(aapl, Money.of(new BigDecimal("140.00")));
        watchlist.addAlert(aapl, Money.of(new BigDecimal("130.00")));

        assertEquals(3, watchlist.getAlerts().size());
        assertEquals(3, watchlist.getAlertsForTicker(aapl).size());
    }

    @Test
    @DisplayName("should remove specific alert")
    void shouldRemoveSpecificAlert() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        Ticker aapl = Ticker.of("AAPL");

        watchlist.addAlert(aapl, Money.of(new BigDecimal("150.00")));
        watchlist.addAlert(aapl, Money.of(new BigDecimal("140.00")));

        watchlist.removeAlert(aapl, Money.of(new BigDecimal("150.00")));

        assertEquals(1, watchlist.getAlerts().size());
        assertEquals(Money.of(new BigDecimal("140.00")), watchlist.getAlerts().getFirst().thresholdPrice());
    }

    @Test
    @DisplayName("should remove all alerts for ticker")
    void shouldRemoveAllAlertsForTicker() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        Ticker aapl = Ticker.of("AAPL");
        Ticker googl = Ticker.of("GOOGL");

        watchlist.addAlert(aapl, Money.of(new BigDecimal("150.00")));
        watchlist.addAlert(aapl, Money.of(new BigDecimal("140.00")));
        watchlist.addAlert(googl, Money.of(new BigDecimal("120.00")));

        watchlist.removeAllAlertsForTicker(aapl);

        assertEquals(1, watchlist.getAlerts().size());
        assertEquals(googl, watchlist.getAlerts().getFirst().ticker());
    }

    @Test
    @DisplayName("should fail removing missing alert")
    void shouldFailRemovingMissingAlert() {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");

        assertThrows(AlertNotFoundException.class, () ->
                watchlist.removeAlert(Ticker.of("AAPL"), Money.of(new BigDecimal("150.00"))));
    }
}
