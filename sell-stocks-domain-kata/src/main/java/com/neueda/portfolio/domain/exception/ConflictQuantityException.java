package com.neueda.portfolio.domain.exception;

/**
 * Not enough shares to satisfy the requested sale.
 *
 * <p>The API layer of the full system maps this to 409 Conflict
 * ("Conflict Quantity"). See {@code docs/spec/error-contract.md}.
 */
public class ConflictQuantityException extends RuntimeException {

    public ConflictQuantityException(String message) {
        super(message);
    }
}
