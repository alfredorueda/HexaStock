package com.neueda.portfolio.domain.exception;

/**
 * The ticker symbol is malformed — not 1 to 5 uppercase letters.
 *
 * <p>A later API layer maps this to 400 Bad Request
 * ("Invalid Ticker"). See {@code docs/spec/error-contract.md}.
 */
public class InvalidTickerException extends RuntimeException {

    public InvalidTickerException(String message) {
        super(message);
    }
}
