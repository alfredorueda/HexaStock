package cat.gencat.agaur.hexastock.watchlists.model.watchlist;

import java.util.Objects;
import java.util.UUID;

/**
 * WatchlistId is a Value Object representing a unique identifier for a Watchlist.
 */
public record WatchlistId(String value) {

    public WatchlistId {
        Objects.requireNonNull(value, "WatchlistId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WatchlistId value must not be blank");
        }
    }

    public static WatchlistId generate() {
        return new WatchlistId(UUID.randomUUID().toString());
    }

    public static WatchlistId of(String value) {
        return new WatchlistId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

