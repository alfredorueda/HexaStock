package com.neueda.portfolio.domain.vo;

import java.util.UUID;

/** Holding identity. Not null, not blank. */
public record HoldingId(String value) {

    public HoldingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HoldingId cannot be null or blank");
        }
    }

    public static HoldingId generate() {
        return new HoldingId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
