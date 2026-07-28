package com.neueda.portfolio.domain.vo;

import com.neueda.portfolio.domain.exception.InvalidQuantityException;

/**
 * A number of shares. Immutable and never negative — a negative quantity is
 * rejected at construction, so it can never reach a sale (spec AC-11).
 *
 * <p>Zero is a legal quantity (a depleted lot holds zero shares); it is the
 * <em>sell</em> operation that additionally requires a positive quantity.
 */
public record ShareQuantity(int value) {

    public static final ShareQuantity ZERO = new ShareQuantity(0);

    public ShareQuantity {
        if (value < 0) {
            throw new InvalidQuantityException("Share quantity cannot be negative: " + value);
        }
    }

    public static ShareQuantity of(int value) {
        return new ShareQuantity(value);
    }

    /** The smaller of the two quantities — the FIFO "take min(lot, still to sell)" step. */
    public ShareQuantity min(ShareQuantity other) {
        return value <= other.value ? this : other;
    }

    public ShareQuantity subtract(ShareQuantity other) {
        return new ShareQuantity(value - other.value);
    }

    public boolean isPositive() {
        return value > 0;
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
