package com.example.portfolio;

/**
 * The outcome of a sale: what it earned, what the shares originally cost,
 * and the profit realized on them.
 */
public class SaleResult {

    private final String ticker;
    private final int quantity;
    private final double proceeds;
    private final double costBasis;
    private final double profit;

    public SaleResult(String ticker, int quantity, double proceeds, double costBasis, double profit) {
        this.ticker = ticker;
        this.quantity = quantity;
        this.proceeds = proceeds;
        this.costBasis = costBasis;
        this.profit = profit;
    }

    public String getTicker() {
        return ticker;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getProceeds() {
        return proceeds;
    }

    public double getCostBasis() {
        return costBasis;
    }

    public double getProfit() {
        return profit;
    }

    @Override
    public String toString() {
        return "Sold " + quantity + " " + ticker
                + " for " + proceeds
                + " (cost " + costBasis + ", profit " + profit + ")";
    }
}
