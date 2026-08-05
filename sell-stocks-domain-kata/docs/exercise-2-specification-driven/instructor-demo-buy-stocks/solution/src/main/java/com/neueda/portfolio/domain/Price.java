package com.neueda.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A per-share price. Rejects non-positive values at construction, so an invalid price
 * can never reach a sale or a purchase.
 */
public record Price(BigDecimal amount) {

    public Price {
        Objects.requireNonNull(amount, "Price cannot be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Price must be positive: " + amount.toPlainString());
        }
    }

    public Money multiply(ShareQuantity qty) {
        return new Money(amount.multiply(BigDecimal.valueOf(qty.value())));
    }
}
