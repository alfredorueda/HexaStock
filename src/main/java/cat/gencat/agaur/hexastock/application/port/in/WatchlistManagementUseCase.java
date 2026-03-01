package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;

public interface WatchlistManagementUseCase {

    Watchlist createWatchlist(String ownerName, String listName);

    Watchlist getWatchlist(WatchlistId watchlistId);

    void deleteWatchlist(WatchlistId watchlistId);

    Watchlist addAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice);

    Watchlist removeAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice);

    Watchlist removeAllAlertsForTicker(WatchlistId watchlistId, Ticker ticker);

    Watchlist activateWatchlist(WatchlistId watchlistId);

    Watchlist deactivateWatchlist(WatchlistId watchlistId);
}
