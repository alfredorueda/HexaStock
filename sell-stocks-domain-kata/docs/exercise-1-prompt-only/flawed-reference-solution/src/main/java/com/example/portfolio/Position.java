package com.example.portfolio;

/**
 * How many shares of one ticker the portfolio holds, and what they cost in total.
 *
 * <p>Individual purchases are folded into a running total as they happen, so the cost
 * of any particular share is the average across everything bought so far.
 */
class Position {

    private final String ticker;
    private int shares;
    private double totalCost;

    Position(String ticker) {
        this.ticker = ticker;
    }

    void add(int quantity, double pricePerShare) {
        shares += quantity;
        totalCost += quantity * pricePerShare;
    }

    void remove(int quantity) {
        double averageCost = getAverageCost();
        shares -= quantity;
        totalCost -= averageCost * quantity;
    }

    double getAverageCost() {
        return shares == 0 ? 0 : totalCost / shares;
    }

    int getShares() {
        return shares;
    }

    String getTicker() {
        return ticker;
    }
}
