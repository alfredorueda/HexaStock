package com.neueda.portfolio.domain.vo;

import java.util.Objects;

/**
 * A stock symbol. Used as the key of the portfolio's holdings map, so its
 * {@code equals}/{@code hashCode} matter — a record gives both.
 */
public record Ticker(String symbol) {

    // TODO (open spec question — see docs/spec/sell-stocks-spec.md §6.1)
    //  The spec states tickers must match ^[A-Z]{1,5}$, but US-07 defines no
    //  acceptance criterion for a malformed ticker: neither which exception is
    //  raised nor where validation happens (request boundary vs. Portfolio.sell,
    //  before or after the portfolio is located). Format validation is therefore
    //  deliberately NOT implemented here, and has no tests. Close the gap in the
    //  spec first, then add it here with its acceptance criteria.

    public Ticker {
        Objects.requireNonNull(symbol, "Ticker symbol cannot be null");
    }

    public static Ticker of(String symbol) {
        return new Ticker(symbol);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
