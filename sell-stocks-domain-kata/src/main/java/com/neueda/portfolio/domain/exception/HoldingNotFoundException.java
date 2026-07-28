package com.neueda.portfolio.domain.exception;

/**
 * The portfolio does not hold the requested ticker.
 *
 * <p>The API layer of the full system maps this to 404 Not Found
 * ("Holding Not Found"). See {@code docs/spec/error-contract.md}.
 */
public class HoldingNotFoundException extends RuntimeException {

    public HoldingNotFoundException(String message) {
        super(message);
    }
}
