package cat.gencat.agaur.hexastock.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Money represents a monetary amount with a specific currency.
 * 
 * <p>In DDD terms, this is a Value Object that encapsulates the concept of money
 * with immutability and value semantics. Money objects are immutable and their
 * equality is based on their currency and amount values.</p>
 * 
 * <p>This class enforces important financial domain rules:</p>
 * <ul>
 *   <li>Currency and amount cannot be null</li>
 *   <li>The scale (decimal places) of the amount must not exceed the currency's default fraction digits</li>
 *   <li>Operations between different currencies are not allowed</li>
 * </ul>
 * 
 * <p>Money provides safe arithmetic operations that ensure currency compatibility
 * and maintain immutability by returning new instances.</p>
 */
public record Money(Currency currency, BigDecimal amount) {

    /**
     * Constructs a Money instance with validation.
     * 
     * @param currency The currency of the monetary amount
     * @param amount The numeric value of the monetary amount
     * @throws NullPointerException if currency or amount is null
     * @throws IllegalArgumentException if amount scale exceeds currency's fraction digits
     */
    public Money {
        Objects.requireNonNull(currency, "'currency' must not be null");
        Objects.requireNonNull(amount, "'amount' must not be null");
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException(
                    "Scale of amount %s is greater than the number of fraction digits used with currency %s"
                            .formatted(amount, currency));
        }
    }

    /**
     * Creates a Money instance from a currency and decimal value.
     * 
     * @param currency The currency to use
     * @param value The monetary amount as a BigDecimal
     * @return A new Money instance
     */
    public static Money of(Currency currency, BigDecimal value) {
        return new Money(currency, value);
    }

    /**
     * Creates a Money instance from a currency and major/minor units.
     * 
     * <p>For example, to create $12.34, use Money.of(USD, 12, 34)</p>
     * 
     * @param currency The currency to use
     * @param mayor The major units (e.g., dollars, euros)
     * @param minor The minor units (e.g., cents)
     * @return A new Money instance
     */
    public static Money of(Currency currency, int mayor, int minor) {
        int scale = currency.getDefaultFractionDigits();
        return new Money(currency, BigDecimal.valueOf(mayor).add(BigDecimal.valueOf(minor, scale)));
    }

    /**
     * Multiplies this monetary amount by an integer factor.
     * 
     * @param multiplicand The integer to multiply by
     * @return A new Money instance with the multiplied amount
     */
    public Money multiply(int multiplicand) {
        return new Money(currency, amount.multiply(BigDecimal.valueOf(multiplicand)));
    }

    /**
     * Adds another Money value to this one.
     * 
     * @param augend The Money value to add
     * @return A new Money instance with the sum
     * @throws IllegalArgumentException if the currencies don't match
     */
    public Money add(Money augend) {
        if (!this.currency.equals(augend.currency())) {
            throw new IllegalArgumentException(
                    "Currency %s of augend does not match this money's currency %s"
                            .formatted(augend.currency(), this.currency));
        }

        return new Money(currency, amount.add(augend.amount()));
    }
}
