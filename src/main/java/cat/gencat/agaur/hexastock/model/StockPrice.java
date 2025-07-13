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
public class StockPrice {

    private Ticker ticker;
    private double price;
    private Instant time;
    private String currency;

    /**
     * Constructs a StockPrice with all required attributes.
     * 
     * @param ticker The ticker symbol of the stock
     * @param price The current price of the stock
     * @param time The timestamp when this price was recorded
     * @param currency The currency code for the price (e.g., "USD", "EUR")
     */
    public StockPrice(Ticker ticker, double price, Instant time, String currency) {
        this.ticker = ticker;
        this.price = price;
        this.time = time;
        this.currency = currency;
    }

    /**
     * Gets the ticker symbol of the stock.
     * 
     * @return The ticker symbol
     */
    public Ticker getTicker() {
        return ticker;
    }

    /**
     * Sets the ticker symbol of the stock.
     * 
     * @param ticker The ticker symbol to set
     */
    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    /**
     * Gets the price of the stock.
     * 
     * @return The current price value
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price of the stock.
     * 
     * @param price The price value to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Gets the timestamp when this price was recorded.
     * 
     * @return The timestamp as an Instant
     */
    public Instant getTime() {
        return time;
    }

    /**
     * Sets the timestamp for this price.
     * 
     * @param time The timestamp to set
     */
    public void setTime(Instant time) {
        this.time = time;
    }

    /**
     * Gets the currency code for this price.
     * 
     * @return The currency code (e.g., "USD", "EUR")
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code for this price.
     * 
     * @param currency The currency code to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}