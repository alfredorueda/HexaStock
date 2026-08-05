package com.neueda.portfolio.domain;

/**
 * The portfolio does not hold the requested ticker.
 */
public class HoldingNotFoundException extends RuntimeException {

    public HoldingNotFoundException(String message) {
        super(message);
    }
}
