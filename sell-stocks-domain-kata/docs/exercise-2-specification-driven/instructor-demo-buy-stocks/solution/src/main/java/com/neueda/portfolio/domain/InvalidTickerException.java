package com.neueda.portfolio.domain;

/**
 * The ticker is not 1-5 uppercase letters.
 */
public class InvalidTickerException extends RuntimeException {

    public InvalidTickerException(String message) {
        super(message);
    }
}
