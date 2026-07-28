package com.neueda.portfolio.domain.vo;

import java.util.UUID;

/** Lot identity. Not null, not blank. */
public record LotId(String value) {

    public LotId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LotId cannot be null or blank");
        }
    }

    public static LotId generate() {
        return new LotId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
