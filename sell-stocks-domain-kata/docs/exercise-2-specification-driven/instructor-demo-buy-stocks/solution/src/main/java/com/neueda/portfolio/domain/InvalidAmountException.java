package com.neueda.portfolio.domain;

/**
 * A monetary amount (a price, or a deposit) is not positive.
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
