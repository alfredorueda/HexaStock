package cat.gencat.agaur.hexastock.application.exception;

public class WatchlistNotFoundException extends RuntimeException {
    public WatchlistNotFoundException(String watchlistId) {
        super("Watchlist not found: " + watchlistId);
    }
}

