package com.neueda.portfolio.domain.vo;

import com.neueda.portfolio.domain.exception.InvalidTickerException;

import java.util.regex.Pattern;

/**
 * A stock symbol: 1 to 5 uppercase letters, validated at construction so a
 * malformed ticker can never reach a sale (spec AC-20, AC-21).
 *
 * <p>Used as the key of the portfolio's holdings map, so its {@code equals} and
 * {@code hashCode} matter — a record gives both.
 */
public record Ticker(String symbol) {

    private static final Pattern VALID_SYMBOL = Pattern.compile("^[A-Z]{1,5}$");

    public Ticker {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidTickerException("Ticker cannot be empty");
        }
        if (!VALID_SYMBOL.matcher(symbol).matches()) {
            throw new InvalidTickerException("Invalid ticker: " + symbol);
        }
    }

    public static Ticker of(String symbol) {
        return new Ticker(symbol);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
