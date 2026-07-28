package com.neueda.portfolio.domain.exception;

/**
 * A monetary amount is not valid — here, a sale price that is not positive.
 *
 * <p>A later API layer maps this to 400 Bad Request
 * ("Invalid Amount"). See {@code ../spec/error-contract.md}.
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
