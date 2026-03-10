package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;
import cat.gencat.agaur.hexastock.model.exception.WatchlistNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WatchlistManagementService")
class WatchlistManagementServiceTest {

    @Test
    @DisplayName("should create watchlist and persist it")
    void shouldCreateWatchlistAndPersistIt() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);

        Watchlist watchlist = service.createWatchlist("john", "Tech");

        assertNotNull(watchlist.getId());
        assertEquals("john", watchlist.getOwnerName());
        assertEquals("Tech", watchlist.getListName());
        assertTrue(watchlist.isActive());
        verify(watchlistPort).createWatchlist(watchlist);
    }

    @Test
    @DisplayName("should throw not found when reading unknown watchlist")
    void shouldThrowNotFoundWhenReadingUnknownWatchlist() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);
        WatchlistId watchlistId = WatchlistId.generate();
        when(watchlistPort.getWatchlistById(watchlistId)).thenReturn(Optional.empty());

        assertThrows(WatchlistNotFoundException.class, () -> service.getWatchlist(watchlistId));
    }

    @Test
    @DisplayName("should add and remove specific alert")
    void shouldAddAndRemoveSpecificAlert() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        WatchlistId watchlistId = watchlist.getId();
        Ticker aapl = Ticker.of("AAPL");
        Money threshold = Money.of("150.00");

        when(watchlistPort.getWatchlistById(watchlistId)).thenReturn(Optional.of(watchlist));

        Watchlist afterAdd = service.addAlertEntry(watchlistId, aapl, threshold);
        assertEquals(1, afterAdd.getAlerts().size());

        Watchlist afterRemove = service.removeAlertEntry(watchlistId, aapl, threshold);
        assertEquals(0, afterRemove.getAlerts().size());
        verify(watchlistPort, times(2)).saveWatchlist(watchlist);
    }

    @Test
    @DisplayName("should remove all alerts for ticker and keep others")
    void shouldRemoveAllAlertsForTickerAndKeepOthers() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        WatchlistId watchlistId = watchlist.getId();
        Ticker aapl = Ticker.of("AAPL");
        Ticker msft = Ticker.of("MSFT");

        watchlist.addAlert(aapl, Money.of("150.00"));
        watchlist.addAlert(aapl, Money.of("140.00"));
        watchlist.addAlert(msft, Money.of("300.00"));
        when(watchlistPort.getWatchlistById(watchlistId)).thenReturn(Optional.of(watchlist));

        Watchlist updated = service.removeAllAlertsForTicker(watchlistId, aapl);

        assertEquals(1, updated.getAlerts().size());
        assertEquals(msft, updated.getAlerts().getFirst().ticker());
        verify(watchlistPort).saveWatchlist(watchlist);
    }

    @Test
    @DisplayName("should toggle active status")
    void shouldToggleActiveStatus() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        WatchlistId watchlistId = watchlist.getId();
        when(watchlistPort.getWatchlistById(watchlistId)).thenReturn(Optional.of(watchlist));

        Watchlist deactivated = service.deactivateWatchlist(watchlistId);
        assertFalse(deactivated.isActive());

        Watchlist activated = service.activateWatchlist(watchlistId);
        assertTrue(activated.isActive());
        verify(watchlistPort, times(2)).saveWatchlist(watchlist);
    }

    @Test
    @DisplayName("should delete existing watchlist")
    void shouldDeleteExistingWatchlist() {
        WatchlistPort watchlistPort = mock(WatchlistPort.class);
        WatchlistManagementService service = new WatchlistManagementService(watchlistPort);
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), "john", "Tech");
        WatchlistId watchlistId = watchlist.getId();
        when(watchlistPort.getWatchlistById(watchlistId)).thenReturn(Optional.of(watchlist));

        service.deleteWatchlist(watchlistId);

        verify(watchlistPort).deleteWatchlist(watchlistId);
    }
}
