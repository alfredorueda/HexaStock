package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.WatchlistManagementUseCase;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;
import cat.gencat.agaur.hexastock.model.exception.WatchlistNotFoundException;
import jakarta.transaction.Transactional;

@Transactional
public class WatchlistManagementService implements WatchlistManagementUseCase {

    private final WatchlistPort watchlistPort;

    public WatchlistManagementService(WatchlistPort watchlistPort) {
        this.watchlistPort = watchlistPort;
    }

    @Override
    public Watchlist createWatchlist(String ownerName, String listName) {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), ownerName, listName);
        watchlistPort.createWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist getWatchlist(WatchlistId watchlistId) {
        return getExistingWatchlist(watchlistId);
    }

    @Override
    public void deleteWatchlist(WatchlistId watchlistId) {
        getExistingWatchlist(watchlistId);
        watchlistPort.deleteWatchlist(watchlistId);
    }

    @Override
    public Watchlist addAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice) {
        Watchlist watchlist = getExistingWatchlist(watchlistId);
        watchlist.addAlert(ticker, thresholdPrice);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist removeAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice) {
        Watchlist watchlist = getExistingWatchlist(watchlistId);
        watchlist.removeAlert(ticker, thresholdPrice);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist removeAllAlertsForTicker(WatchlistId watchlistId, Ticker ticker) {
        Watchlist watchlist = getExistingWatchlist(watchlistId);
        watchlist.removeAllAlertsForTicker(ticker);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist activateWatchlist(WatchlistId watchlistId) {
        Watchlist watchlist = getExistingWatchlist(watchlistId);
        watchlist.activate();
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist deactivateWatchlist(WatchlistId watchlistId) {
        Watchlist watchlist = getExistingWatchlist(watchlistId);
        watchlist.deactivate();
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    private Watchlist getExistingWatchlist(WatchlistId watchlistId) {
        return watchlistPort.getWatchlistById(watchlistId)
                .orElseThrow(() -> new WatchlistNotFoundException(watchlistId.value()));
    }
}
