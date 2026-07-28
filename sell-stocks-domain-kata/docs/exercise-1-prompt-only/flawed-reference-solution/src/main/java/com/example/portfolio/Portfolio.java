package com.example.portfolio;

import java.util.HashMap;
import java.util.Map;

/**
 * An investor's portfolio: a cash balance plus the stock they hold.
 *
 * <p>Positions are tracked per ticker. Selling uses the average cost of the shares
 * held, which is the standard way to work out the cost basis of a partial sale.
 */
public class Portfolio {

    private final String owner;
    private double cashBalance;
    private final Map<String, Position> positions = new HashMap<>();

    public Portfolio(String owner) {
        this.owner = owner;
    }

    /**
     * Records a purchase of {@code quantity} shares at {@code pricePerShare}.
     */
    public void buy(String ticker, int quantity, double pricePerShare) {
        positions.computeIfAbsent(ticker, Position::new).add(quantity, pricePerShare);
    }

    /**
     * Sells {@code quantity} shares of {@code ticker} at {@code pricePerShare} and
     * credits the proceeds to the cash balance.
     *
     * @return what the sale earned, what it cost, and the profit
     * @throws IllegalArgumentException if the ticker is not held, or not enough shares
     */
    public SaleResult sell(String ticker, int quantity, double pricePerShare) {
        Position position = positions.get(ticker);
        if (position == null) {
            throw new IllegalArgumentException("No position for " + ticker);
        }
        if (quantity > position.getShares()) {
            throw new IllegalArgumentException("Not enough shares");
        }

        double costBasis = position.getAverageCost() * quantity;
        double proceeds = quantity * pricePerShare;

        position.remove(quantity);
        cashBalance += proceeds;

        return new SaleResult(ticker, quantity, proceeds, costBasis, proceeds - costBasis);
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public int getShares(String ticker) {
        Position position = positions.get(ticker);
        return position == null ? 0 : position.getShares();
    }

    public String getOwner() {
        return owner;
    }
}
