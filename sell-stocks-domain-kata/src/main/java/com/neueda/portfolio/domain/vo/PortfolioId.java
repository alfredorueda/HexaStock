package com.neueda.portfolio.domain.vo;

import java.util.UUID;

/** Portfolio identity. Not null, not blank. */
public record PortfolioId(String value) {

    public PortfolioId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PortfolioId cannot be null or blank");
        }
    }

    public static PortfolioId generate() {
        return new PortfolioId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
