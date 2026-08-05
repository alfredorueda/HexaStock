package com.neueda.portfolio.domain;

import java.util.regex.Pattern;

/**
 * A stock symbol: 1-5 uppercase letters.
 */
public record Ticker(String symbol) {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,5}$");

    public Ticker {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidTickerException("Ticker cannot be empty");
        }
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new InvalidTickerException("Invalid ticker: " + symbol);
        }
    }
}
