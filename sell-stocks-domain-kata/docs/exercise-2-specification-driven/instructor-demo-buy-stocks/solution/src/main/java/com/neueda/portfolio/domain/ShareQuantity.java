package com.neueda.portfolio.domain;

/**
 * A number of shares. Negative values are rejected at construction; zero is a legal
 * value (a lot can be drained to zero, a portfolio can hold zero of a ticker), while
 * selling and buying additionally require a strictly positive quantity.
 */
public record ShareQuantity(int value) {

    public ShareQuantity {
        if (value < 0) {
            throw new InvalidQuantityException("Quantity must be positive: " + value);
        }
    }

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
}
