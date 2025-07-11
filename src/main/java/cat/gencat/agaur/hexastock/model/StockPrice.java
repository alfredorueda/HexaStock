package cat.gencat.agaur.hexastock.model;

import java.time.Instant;

public class StockPrice {

    private Ticker ticker;
    private double price;
    private Instant time;
    private String currency;

    public StockPrice(Ticker ticker, double price, Instant time, String currency) {
        this.ticker = ticker;
        this.price = price;
        this.time = time;
        this.currency = currency;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}