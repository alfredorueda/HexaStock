package cat.gencat.agaur.hexastock.model;

import java.util.Objects;

/**
 * Ticker represents a stock ticker symbol used to uniquely identify a publicly traded company.
 * 
 * <p>In DDD terms, this is a Value Object that encapsulates a stock ticker symbol
 * with immutability and value semantics. Ticker objects are immutable and their
 * equality is based on their string value.</p>
 * 
 * <p>This class enforces domain rules for valid ticker symbols:</p>
 * <ul>
 *   <li>Must not be null or empty</li>
 *   <li>Must contain 1-5 uppercase letters (matching standard stock exchange formats)</li>
 * </ul>
 * 
 * <p>Examples of valid tickers: "AAPL" (Apple), "MSFT" (Microsoft), "AMZN" (Amazon)</p>
 */
public record Ticker(String value) {

    /**
     * Constructs a Ticker instance with validation.
     * 
     * @param value The string representation of the ticker symbol
     * @throws IllegalArgumentException if the value is null, blank, or does not match the required format
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
     * Factory method for creating a Ticker instance.
     * 
     * <p>This method provides a more readable way to create a Ticker than using the constructor directly.</p>
     *
     * @param value The string representation of the ticker symbol
     * @return A validated Ticker instance
     * @throws IllegalArgumentException if the value is invalid
     */
    public static Ticker of(String value) {
        return new Ticker(value);
    }
}