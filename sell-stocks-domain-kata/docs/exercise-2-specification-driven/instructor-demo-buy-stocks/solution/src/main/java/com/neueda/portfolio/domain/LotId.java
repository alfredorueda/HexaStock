package com.neueda.portfolio.domain;

/**
 * Lot identity.
 */
public record LotId(String value) {

    public LotId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LotId cannot be empty");
        }
    }
}
