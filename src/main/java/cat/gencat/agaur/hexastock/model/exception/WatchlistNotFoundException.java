package cat.gencat.agaur.hexastock.model.exception;

public class WatchlistNotFoundException extends DomainException {
    public WatchlistNotFoundException(String watchlistId) {
        super("Watchlist not found with id: " + watchlistId);
    }
}
