package com.neueda.portfolio.domain.vo;

import java.util.Objects;

/**
 * The outcome of a sale: what it earned, what it originally cost, and the
 * difference between the two.
 *
 * <p>{@code profit = proceeds − costBasis}. A negative profit is a valid
 * result — it is a realized loss (spec AC-06).
 */
public record SellResult(Money proceeds, Money costBasis, Money profit) {

    public SellResult {
        Objects.requireNonNull(proceeds, "proceeds cannot be null");
        Objects.requireNonNull(costBasis, "costBasis cannot be null");
        Objects.requireNonNull(profit, "profit cannot be null");
    }

    /** Builds the result, deriving the profit so it can never drift from the definition. */
    public static SellResult of(Money proceeds, Money costBasis) {
        return new SellResult(proceeds, costBasis, proceeds.subtract(costBasis));
    }
}
