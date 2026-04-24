package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.exception.WatchlistNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WatchlistService")
class WatchlistServiceTest {

    @Test
    @SpecificationRef(value = "US-WL-01.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-create.feature")
    void createWatchlist_shouldPersistAndReturnAggregate() {
        WatchlistPort port = mock(WatchlistPort.class);
        WatchlistService service = new WatchlistService(port);

        Watchlist created = service.createWatchlist("alice", "Tech");

        assertNotNull(created.getId());
        assertTrue(created.isActive());
        verify(port).createWatchlist(created);
    }

    @Test
    @SpecificationRef(value = "US-WL-02.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
    void addAlert_shouldLoadModifyAndSave() {
        WatchlistPort port = mock(WatchlistPort.class);
        WatchlistService service = new WatchlistService(port);

        WatchlistId id = WatchlistId.of("wl-1");
        Watchlist existing = Watchlist.create(id, "alice", "Tech");
        when(port.getWatchlistById(id)).thenReturn(Optional.of(existing));

        Watchlist updated = service.addAlertEntry(id, Ticker.of("AAPL"), Money.of("150.00"));

        assertEquals(1, updated.getAlerts().size());
        verify(port).saveWatchlist(existing);
    }

    @Test
    void operations_shouldThrowWhenWatchlistMissing() {
        WatchlistPort port = mock(WatchlistPort.class);
        WatchlistService service = new WatchlistService(port);

        WatchlistId id = WatchlistId.of("missing");
        when(port.getWatchlistById(id)).thenReturn(Optional.empty());

        assertThrows(WatchlistNotFoundException.class, () ->
                service.activate(id));
    }
}
