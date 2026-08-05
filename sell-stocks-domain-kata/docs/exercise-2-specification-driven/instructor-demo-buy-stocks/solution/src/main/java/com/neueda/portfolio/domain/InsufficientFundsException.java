package com.neueda.portfolio.domain;

/**
 * The portfolio's cash balance does not cover the cost of a purchase.
 * Introduced by US-08 (Buy Stocks).
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
