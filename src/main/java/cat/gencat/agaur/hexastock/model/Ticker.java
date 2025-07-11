package cat.gencat.agaur.hexastock.model;

import java.util.Objects;

/**
 * Value Object representing a stock ticker symbol.
 * Immutable and validated against domain rules.
 */
public record Ticker(String value) {

    /**
     * Compact canonical constructor with validation rules.
     *
     * @param value the raw ticker symbol string
     * @throws IllegalArgumentException if the value is null, blank, or does not match ticker format
     */
    public Ticker {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El ticker no puede estar vacío");
        }

        if (!value.matches("^[A-Z]{1,5}$")) {
            throw new IllegalArgumentException("Ticker inválido: " + value);
        }
    }

    /**
     * Factory method for clearer construction.
     *
     * @param value the ticker string
     * @return a validated Ticker instance
     */
    public static Ticker of(String value) {
        return new Ticker(value);
    }
}