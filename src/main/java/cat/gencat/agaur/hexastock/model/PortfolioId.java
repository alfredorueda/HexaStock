package cat.gencat.agaur.hexastock.model;

import java.util.Objects;
import java.util.UUID;

/**
 * PortfolioId is a Value Object representing a unique identifier for a Portfolio.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the identity
 * of a Portfolio aggregate root. It ensures type safety and prevents primitive obsession
 * by wrapping the underlying UUID-based identifier.</p>
 *
 * <p>This class enforces domain rules:</p>
 * <ul>
 *   <li>Must not be null or blank</li>
 *   <li>Provides factory method for generating new unique identifiers</li>
 * </ul>
 */
public record PortfolioId(String value) {

    /**
     * Constructs a PortfolioId with validation.
     *
     * @param value The string representation of the portfolio identifier
     * @throws IllegalArgumentException if the value is null or blank
     */
    public PortfolioId {
        Objects.requireNonNull(value, "PortfolioId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PortfolioId value must not be blank");
        }
    }

    /**
     * Factory method to generate a new unique PortfolioId.
     *
     * @return A new PortfolioId with a randomly generated UUID
     */
    public static PortfolioId generate() {
        return new PortfolioId(UUID.randomUUID().toString());
    }

    /**
     * Factory method to create a PortfolioId from an existing string value.
     *
     * @param value The string representation of the portfolio identifier
     * @return A validated PortfolioId instance
     */
    public static PortfolioId of(String value) {
        return new PortfolioId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
