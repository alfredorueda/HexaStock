package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;

/**
 * ShareQuantity is a Value Object representing a number of shares.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the concept
 * of share quantity with immutability and value semantics. It ensures that share quantities
 * are always non-negative and provides domain operations for share arithmetic.</p>
 *
 * <p>This class enforces domain rules:</p>
 * <ul>
 *   <li>Quantity must be non-negative (zero is allowed for depleted lots)</li>
 *   <li>Provides arithmetic operations that maintain invariants</li>
 * </ul>
 */
public record ShareQuantity(int value) {

    public static final ShareQuantity ZERO = new ShareQuantity(0);

    /**
     * Constructs a ShareQuantity with validation.
     *
     * @param value The number of shares
     * @throws InvalidQuantityException if the value is negative
     */
    public ShareQuantity {
        if (value < 0) {
            throw new InvalidQuantityException("Share quantity cannot be negative: " + value);
        }
    }

    /**
     * Factory method to create a ShareQuantity.
     *
     * @param value The number of shares
     * @return A validated ShareQuantity instance
     */
    public static ShareQuantity of(int value) {
        return new ShareQuantity(value);
    }

    /**
     * Creates a ShareQuantity that must be positive (greater than zero).
     * Use this for operations that require at least one share.
     *
     * @param value The number of shares
     * @return A validated ShareQuantity instance
     * @throws InvalidQuantityException if the value is not positive
     */
    public static ShareQuantity positive(int value) {
        if (value <= 0) {
            throw new InvalidQuantityException("Quantity must be positive: " + value);
        }
        return new ShareQuantity(value);
    }

    /**
     * Adds another ShareQuantity to this one.
     *
     * @param other The ShareQuantity to add
     * @return A new ShareQuantity with the sum
     */
    public ShareQuantity add(ShareQuantity other) {
        return new ShareQuantity(this.value + other.value);
    }

    /**
     * Subtracts another ShareQuantity from this one.
     *
     * @param other The ShareQuantity to subtract
     * @return A new ShareQuantity with the difference
     * @throws InvalidQuantityException if the result would be negative
     */
    public ShareQuantity subtract(ShareQuantity other) {
        return new ShareQuantity(this.value - other.value);
    }

    /**
     * Returns the minimum of this ShareQuantity and another.
     *
     * @param other The ShareQuantity to compare with
     * @return The smaller of the two ShareQuantity values
     */
    public ShareQuantity min(ShareQuantity other) {
        return new ShareQuantity(Math.min(this.value, other.value));
    }

    /**
     * Checks if this quantity is zero.
     *
     * @return true if the quantity is zero
     */
    public boolean isZero() {
        return value == 0;
    }

    /**
     * Checks if this quantity is greater than zero.
     *
     * @return true if the quantity is positive
     */
    public boolean isPositive() {
        return value > 0;
    }

    /**
     * Checks if this quantity is greater than or equal to another.
     *
     * @param other The ShareQuantity to compare with
     * @return true if this quantity is greater than or equal to other
     */
    public boolean isGreaterThanOrEqual(ShareQuantity other) {
        return this.value >= other.value;
    }

    /**
     * Checks if this quantity is less than or equal to another.
     *
     * @param other The ShareQuantity to compare with
     * @return true if this quantity is less than or equal to other
     */
    public boolean isLessThanOrEqual(ShareQuantity other) {
        return this.value <= other.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
