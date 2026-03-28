package cat.gencat.agaur.hexastock.model;

import java.util.Objects;
import java.util.UUID;

/**
 * HoldingId is a Value Object representing a unique identifier for a Holding.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the identity
 * of a Holding entity within the Portfolio aggregate. It ensures type safety and prevents
 * primitive obsession by wrapping the underlying UUID-based identifier.</p>
 */
public record HoldingId(String value) {

    public HoldingId {
        Objects.requireNonNull(value, "HoldingId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("HoldingId value must not be blank");
        }
    }

    public static HoldingId generate() {
        return new HoldingId(UUID.randomUUID().toString());
    }

    public static HoldingId of(String value) {
        return new HoldingId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
