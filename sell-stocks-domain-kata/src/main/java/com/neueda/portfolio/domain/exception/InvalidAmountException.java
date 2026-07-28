package com.neueda.portfolio.domain.exception;

/**
 * A monetary amount is not valid — here, a sale price that is not positive.
 *
 * <p>The API layer of the full system maps this to 400 Bad Request
 * ("Invalid Amount"). See {@code docs/spec/error-contract.md}.
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
