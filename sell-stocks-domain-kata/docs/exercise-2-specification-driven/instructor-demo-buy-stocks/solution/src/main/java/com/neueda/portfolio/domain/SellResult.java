package com.neueda.portfolio.domain;

/**
 * The outcome of a sale: {@code profit = proceeds - costBasis}.
 */
public record SellResult(Money proceeds, Money costBasis, Money profit) {
}
