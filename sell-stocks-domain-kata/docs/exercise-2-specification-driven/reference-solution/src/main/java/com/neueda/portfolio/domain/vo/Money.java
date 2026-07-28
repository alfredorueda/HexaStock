package com.neueda.portfolio.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A monetary amount. Immutable, always {@code BigDecimal} at scale 2, HALF_UP —
 * never {@code double} or {@code float}.
 */
public record Money(BigDecimal amount) {

    public static final Money ZERO = Money.of(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
