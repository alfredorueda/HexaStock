package cat.gencat.agaur.hexastock.model;

import java.time.Instant;

/**
 * StockPrice represents the price of a specific stock at a particular moment in time.
 *
 * <p>In DDD terms, this is a Value Object that contains the current or historical
 * price information for a stock identified by its ticker symbol.</p>
 */
public record StockPrice(
    Ticker ticker,
    Price price,
    Instant time
) {
    /**
     * Factory method to create a StockPrice.
     *
     * @param ticker The ticker symbol
     * @param price The stock price
     * @param time The timestamp
     * @return A new StockPrice instance
     */
    public static StockPrice of(Ticker ticker, Price price, Instant time) {
        return new StockPrice(ticker, price, time);
    }
}
