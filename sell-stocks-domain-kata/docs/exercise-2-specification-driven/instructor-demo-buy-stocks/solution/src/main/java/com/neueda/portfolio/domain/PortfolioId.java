package com.neueda.portfolio.domain;

/**
 * Portfolio identity (a UUID string).
 */
public record PortfolioId(String value) {

    public PortfolioId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PortfolioId cannot be empty");
        }
    }
}
