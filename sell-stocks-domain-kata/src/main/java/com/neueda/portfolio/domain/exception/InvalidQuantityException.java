package com.neueda.portfolio.domain.exception;

/**
 * A share quantity is zero, negative, or otherwise unusable.
 *
 * <p>A later API layer maps this to 400 Bad Request
 * ("Invalid Quantity"). See {@code docs/spec/error-contract.md}.
 */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
