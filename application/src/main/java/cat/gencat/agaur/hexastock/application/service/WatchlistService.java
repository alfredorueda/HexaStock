package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.exception.WatchlistNotFoundException;
import cat.gencat.agaur.hexastock.application.port.in.WatchlistUseCase;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;

import jakarta.transaction.Transactional;

import java.util.Objects;

@Transactional
public class WatchlistService implements WatchlistUseCase {

    private final WatchlistPort watchlistPort;

    public WatchlistService(WatchlistPort watchlistPort) {
        this.watchlistPort = Objects.requireNonNull(watchlistPort, "watchlistPort must not be null");
    }

    @Override
    public Watchlist createWatchlist(String ownerName, String listName) {
        Watchlist watchlist = Watchlist.create(WatchlistId.generate(), ownerName, listName);
        watchlistPort.createWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public void deleteWatchlist(WatchlistId watchlistId) {
        // ensure existence for clearer error semantics
        watchlistPort.getWatchlistById(watchlistId)
                .orElseThrow(() -> new WatchlistNotFoundException(watchlistId.value()));
        watchlistPort.deleteWatchlist(watchlistId);
    }

    @Override
    public Watchlist addAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice) {
        Watchlist watchlist = load(watchlistId);
        watchlist.addAlert(ticker, thresholdPrice);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist removeAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice) {
        Watchlist watchlist = load(watchlistId);
        watchlist.removeAlert(ticker, thresholdPrice);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist removeAllAlertsForTicker(WatchlistId watchlistId, Ticker ticker) {
        Watchlist watchlist = load(watchlistId);
        watchlist.removeAllAlertsForTicker(ticker);
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist activate(WatchlistId watchlistId) {
        Watchlist watchlist = load(watchlistId);
        watchlist.activate();
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    @Override
    public Watchlist deactivate(WatchlistId watchlistId) {
        Watchlist watchlist = load(watchlistId);
        watchlist.deactivate();
        watchlistPort.saveWatchlist(watchlist);
        return watchlist;
    }

    private Watchlist load(WatchlistId watchlistId) {
        return watchlistPort.getWatchlistById(watchlistId)
                .orElseThrow(() -> new WatchlistNotFoundException(watchlistId.value()));
    }
}
