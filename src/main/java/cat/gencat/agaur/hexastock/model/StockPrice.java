package cat.gencat.agaur.hexastock.model;

import java.time.Instant;

/**
 * StockPrice represents the price of a specific stock at a particular moment in time.
 * 
 * <p>In DDD terms, this is a Domain Object that contains the current or historical
 * price information for a stock identified by its ticker symbol.</p>
 * 
 * <p>This class captures essential information about a stock's price:</p>
 * <ul>
 *   <li>The ticker symbol identifying the stock</li>
 *   <li>The price value</li>
 *   <li>The timestamp when the price was recorded</li>
 *   <li>The currency in which the price is denominated</li>
 * </ul>
 * 
 * <p>StockPrice objects are typically returned by external market data services
 * and are used for portfolio valuation and trade execution.</p>
 */
public record StockPrice(
    Ticker ticker,
    double price,
    Instant time,
    String currency
) {}
