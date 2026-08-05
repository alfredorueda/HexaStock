package com.neueda.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A monetary amount. Always {@link BigDecimal}, scale 2, HALF_UP — never double or float.
 *
 * <p>Money itself carries no sign restriction: a profit may legitimately be negative
 * (sell spec AC-06) and a balance may legitimately be zero.</p>
 */
public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }
}
