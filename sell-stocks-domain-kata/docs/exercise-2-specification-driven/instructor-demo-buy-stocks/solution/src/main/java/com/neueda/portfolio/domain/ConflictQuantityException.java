package com.neueda.portfolio.domain;

/**
 * Not enough shares to satisfy the sale.
 */
public class ConflictQuantityException extends RuntimeException {

    public ConflictQuantityException(String message) {
        super(message);
    }
}
