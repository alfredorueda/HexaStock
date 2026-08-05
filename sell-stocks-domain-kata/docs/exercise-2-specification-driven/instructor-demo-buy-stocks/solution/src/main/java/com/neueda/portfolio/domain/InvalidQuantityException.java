package com.neueda.portfolio.domain;

/**
 * The quantity is zero, negative, or otherwise invalid.
 */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
