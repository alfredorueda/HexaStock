package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;

import java.util.Optional;

public interface WatchlistPort {

    Optional<Watchlist> getWatchlistById(WatchlistId watchlistId);

    void createWatchlist(Watchlist watchlist);

    void saveWatchlist(Watchlist watchlist);

    void deleteWatchlist(WatchlistId watchlistId);
}
