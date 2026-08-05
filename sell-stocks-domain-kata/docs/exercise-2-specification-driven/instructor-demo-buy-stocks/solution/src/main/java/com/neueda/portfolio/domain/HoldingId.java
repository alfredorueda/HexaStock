package com.neueda.portfolio.domain;

/**
 * Holding identity.
 */
public record HoldingId(String value) {

    public HoldingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HoldingId cannot be empty");
        }
    }
}
