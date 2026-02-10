package cat.gencat.agaur.hexastock.model;

import java.util.Objects;
import java.util.UUID;

/**
 * LotId is a Value Object representing a unique identifier for a Lot.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the identity
 * of a Lot entity within the Holding aggregate. It ensures type safety and prevents
 * primitive obsession by wrapping the underlying UUID-based identifier.</p>
 */
public record LotId(String value) {

    public LotId {
        Objects.requireNonNull(value, "LotId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("LotId value must not be blank");
        }
    }

    public static LotId generate() {
        return new LotId(UUID.randomUUID().toString());
    }

    public static LotId of(String value) {
        return new LotId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
