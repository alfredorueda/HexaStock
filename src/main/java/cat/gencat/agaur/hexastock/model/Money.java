package cat.gencat.agaur.hexastock.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money represents a monetary amount in USD (single-currency assumption).
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the concept of money
 * with immutability and value semantics. Money objects are immutable and their
 * equality is based on their amount values.</p>
 *
 * <p>This class enforces important financial domain rules:</p>
 * <ul>
 *   <li>Amount cannot be null</li>
 *   <li>Scale is normalized to 2 decimal places using HALF_UP rounding</li>
 *   <li>Provides safe arithmetic operations that maintain immutability</li>
 * </ul>
 *
 * <p><strong>Single-Currency Assumption:</strong> This implementation assumes all monetary
 * values are in USD. For multi-currency support, the Currency field would need to be
 * reintroduced with appropriate validation.</p>
 */
public record Money(BigDecimal amount) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE));

    /**
     * Constructs a Money instance with validation and normalization.
     *
     * @param amount The numeric value of the monetary amount
     * @throws NullPointerException if amount is null
     */
    public Money {
        Objects.requireNonNull(amount, "'amount' must not be null");
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Creates a Money instance from a BigDecimal value.
     *
     * @param value The monetary amount as a BigDecimal
     * @return A new Money instance
     */
    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    /**
     * Creates a Money instance from a double value.
     *
     * @param value The monetary amount as a double
     * @return A new Money instance
     */
    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    /**
     * Creates a Money instance from a string value.
     *
     * @param value The monetary amount as a string
     * @return A new Money instance
     */
    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    /**
     * Creates a Money instance from dollars and cents.
     *
     * <p>For example, to create $12.34, use Money.of(12, 34)</p>
     *
     * @param dollars The dollar amount
     * @param cents The cents amount (0-99)
     * @return A new Money instance
     */
    public static Money of(int dollars, int cents) {
        return new Money(BigDecimal.valueOf(dollars).add(BigDecimal.valueOf(cents, SCALE)));
    }

    /**
     * Adds another Money value to this one.
     *
     * @param augend The Money value to add
     * @return A new Money instance with the sum
     */
    public Money add(Money augend) {
        return new Money(amount.add(augend.amount()));
    }

    /**
     * Subtracts another Money value from this one.
     *
     * @param subtrahend The Money value to subtract
     * @return A new Money instance with the difference
     */
    public Money subtract(Money subtrahend) {
        return new Money(amount.subtract(subtrahend.amount()));
    }

    /**
     * Multiplies this monetary amount by an integer factor.
     *
     * @param multiplicand The integer to multiply by
     * @return A new Money instance with the multiplied amount
     */
    public Money multiply(int multiplicand) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplicand)));
    }

    /**
     * Multiplies this monetary amount by a ShareQuantity.
     *
     * @param quantity The share quantity to multiply by
     * @return A new Money instance with the multiplied amount
     */
    public Money multiply(ShareQuantity quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity.value())));
    }

    /**
     * Negates this monetary amount.
     *
     * @return A new Money instance with the negated amount
     */
    public Money negate() {
        return new Money(amount.negate());
    }

    /**
     * Checks if this amount is positive (greater than zero).
     *
     * @return true if the amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this amount is negative (less than zero).
     *
     * @return true if the amount is negative
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if this amount is zero.
     *
     * @return true if the amount is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this amount is greater than or equal to another.
     *
     * @param other The Money to compare with
     * @return true if this amount is greater than or equal to other
     */
    public boolean isGreaterThanOrEqual(Money other) {
        return amount.compareTo(other.amount()) >= 0;
    }

    /**
     * Checks if this amount is less than another.
     *
     * @param other The Money to compare with
     * @return true if this amount is less than other
     */
    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount()) < 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
