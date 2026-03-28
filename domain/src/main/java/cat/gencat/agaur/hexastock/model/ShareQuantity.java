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
 *
 * <h3>Design Note: Why Zero Is Allowed</h3>
 *
 * <p>ShareQuantity enforces the invariant {@code value >= 0}, not {@code value > 0}.
 * This is an intentional design decision. Zero quantities are valid <strong>domain state</strong>:
 * a lot whose shares have been fully sold has {@code remainingShares = ShareQuantity.ZERO},
 * and arithmetic operations such as {@link #subtract(ShareQuantity)} may legitimately
 * produce zero as a result.</p>
 *
 * <p>However, <strong>operational commands</strong> (buy, sell) require strictly positive
 * quantities. This validation is enforced by the aggregate root ({@code Portfolio.buy()},
 * {@code Portfolio.sell()}) using {@link #isPositive()}, and at the system boundary
 * using the {@link #positive(int)} factory method. The distinction is:</p>
 *
 * <ul>
 *   <li><strong>State quantity</strong> — may be zero ({@code ShareQuantity >= 0})</li>
 *   <li><strong>Operation quantity</strong> — must be positive ({@code > 0}), enforced by the caller</li>
 * </ul>
 *
 * <h4>Alternative Modeling Options</h4>
 *
 * <p>Three valid DDD approaches exist for this validation boundary:</p>
 * <ol>
 *   <li><strong>Option A (current):</strong> A single ShareQuantity ({@code >= 0}).
 *       Aggregate root operations validate positivity. Simple, minimal, and sufficient
 *       for this domain.</li>
 *   <li><strong>Option B:</strong> ShareQuantity enforces {@code > 0} everywhere.
 *       Zero would not be representable, requiring a different mechanism for depleted
 *       lots and arithmetic results.</li>
 *   <li><strong>Option C:</strong> Two separate Value Objects — {@code ShareQuantity (>= 0)}
 *       for state and a hypothetical {@code TradeQuantity (> 0)} for commands.
 *       Type-safe but adds complexity that is not justified by the current domain.</li>
 * </ol>
 *
 * <p>Option A was chosen for its simplicity. The {@link #positive(int)} factory method
 * and the aggregate root guards provide sufficient protection without introducing
 * additional types.</p>
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
