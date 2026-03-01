package cat.gencat.agaur.hexastock.model;

import java.util.Objects;
import java.util.UUID;

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
}
