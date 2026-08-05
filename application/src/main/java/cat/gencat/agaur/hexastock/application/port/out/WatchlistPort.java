package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;

import java.util.Optional;

public interface WatchlistPort {
    Optional<Watchlist> getWatchlistById(WatchlistId id);

    void createWatchlist(Watchlist watchlist);

    void saveWatchlist(Watchlist watchlist);

    void deleteWatchlist(WatchlistId id);
}

