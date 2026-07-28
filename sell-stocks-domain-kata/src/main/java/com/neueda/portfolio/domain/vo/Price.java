package com.neueda.portfolio.domain.vo;

import com.neueda.portfolio.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A per-share price. Immutable, scale 2 HALF_UP, and strictly positive:
 * a non-positive price is rejected at construction (spec §2, precondition 6).
 */
public record Price(BigDecimal amount) {

    public Price {
        Objects.requireNonNull(amount, "Price amount cannot be null");
        // Scale first, then validate: 0.001 rounds to 0.00 and must not pass as positive.
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Price must be positive: " + amount.toPlainString());
        }
    }

    public static Price of(BigDecimal amount) {
        return new Price(amount);
    }

    public static Price of(String amount) {
        return new Price(new BigDecimal(amount));
    }

    public static Price of(long amount) {
        return new Price(BigDecimal.valueOf(amount));
    }

    /** {@code quantity × price}, i.e. the proceeds or cost of that many shares. */
    public Money multiply(ShareQuantity qty) {
        return Money.of(amount.multiply(BigDecimal.valueOf(qty.value())));
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
