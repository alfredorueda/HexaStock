package cat.gencat.agaur.hexastock.model;

import java.util.Objects;
import java.util.UUID;

/**
 * TransactionId is a Value Object representing a unique identifier for a Transaction.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the identity
 * of a Transaction entity. It ensures type safety and prevents primitive obsession
 * by wrapping the underlying UUID-based identifier.</p>
 */
public record TransactionId(String value) {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TransactionId value must not be blank");
        }
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
