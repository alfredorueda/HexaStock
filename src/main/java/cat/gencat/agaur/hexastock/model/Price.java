package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Price is a Value Object representing a per-share price.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the concept
 * of a stock price with immutability and value semantics. It ensures that prices are
 * always positive and provides domain operations for price calculations.</p>
 *
 * <p>This class enforces domain rules:</p>
 * <ul>
 *   <li>Price must be positive (greater than zero)</li>
 *   <li>Scale is normalized to 2 decimal places using HALF_UP rounding</li>
 * </ul>
 *
 * <p>Note: This is a single-currency implementation. All prices are assumed to be in USD.
 * For multi-currency support, consider using the Money value object instead.</p>
 */
public record Price(BigDecimal value) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Constructs a Price with validation and normalization.
     *
     * @param value The price amount
     * @throws InvalidAmountException if the value is null or not positive
     */
    public Price {
        Objects.requireNonNull(value, "Price value must not be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Price must be positive: " + value);
        }
        value = value.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Factory method to create a Price from a BigDecimal.
     *
     * @param value The price amount
     * @return A validated and normalized Price instance
     */
    public static Price of(BigDecimal value) {
        return new Price(value);
    }

    /**
     * Factory method to create a Price from a double.
     *
     * @param value The price amount
     * @return A validated and normalized Price instance
     */
    public static Price of(double value) {
        return new Price(BigDecimal.valueOf(value));
    }

    /**
     * Factory method to create a Price from a string.
     *
     * @param value The price amount as a string
     * @return A validated and normalized Price instance
     */
    public static Price of(String value) {
        return new Price(new BigDecimal(value));
    }

    /**
     * Multiplies this price by a share quantity to calculate total cost.
     *
     * @param quantity The number of shares
     * @return A Money value representing the total cost
     */
    public Money multiply(ShareQuantity quantity) {
        BigDecimal total = value.multiply(BigDecimal.valueOf(quantity.value()))
                .setScale(SCALE, ROUNDING_MODE);
        return Money.of(total);
    }

    /**
     * Converts this Price to a Money value.
     *
     * @return A Money value with the same amount
     */
    public Money toMoney() {
        return Money.of(value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
