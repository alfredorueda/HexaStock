package cat.gencat.agaur.hexastock.watchlists.application.port.in;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;

public interface WatchlistUseCase {

    Watchlist createWatchlist(String ownerName, String listName);

    void deleteWatchlist(WatchlistId watchlistId);

    Watchlist addAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice);

    Watchlist removeAlertEntry(WatchlistId watchlistId, Ticker ticker, Money thresholdPrice);

    Watchlist removeAllAlertsForTicker(WatchlistId watchlistId, Ticker ticker);

    Watchlist activate(WatchlistId watchlistId);

    Watchlist deactivate(WatchlistId watchlistId);
}
