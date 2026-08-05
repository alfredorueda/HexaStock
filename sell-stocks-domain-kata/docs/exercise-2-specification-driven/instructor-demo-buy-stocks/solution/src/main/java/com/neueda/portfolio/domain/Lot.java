package com.neueda.portfolio.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * One purchase event: a number of shares bought at one unit price at one moment.
 * Lots are consumed oldest-first when the holding is sold (FIFO).
 */
public class Lot {

    private final LotId id;
    private final ShareQuantity initialShares;
    private ShareQuantity remainingShares;
    private final Price unitPrice;
    private final LocalDateTime purchasedAt;

    public Lot(ShareQuantity shares, Price unitPrice) {
        Objects.requireNonNull(shares, "Shares cannot be null");
        Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        if (!shares.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + shares.value());
        }
        this.id = new LotId(UUID.randomUUID().toString());
        this.initialShares = shares;
        this.remainingShares = shares;
        this.unitPrice = unitPrice;
        this.purchasedAt = LocalDateTime.now();
    }

    /**
     * Removes {@code quantity} shares from this lot. Rejected — leaving the lot untouched —
     * when it would drive the remaining shares below zero.
     */
    public void reduce(ShareQuantity quantity) {
        if (quantity.value() > remainingShares.value()) {
            throw new ConflictQuantityException(
                    "Not enough shares to sell. Available: " + remainingShares.value()
                            + ", Requested: " + quantity.value());
        }
        this.remainingShares = remainingShares.subtract(quantity);
    }

    /**
     * The original acquisition cost of {@code quantity} shares taken from this lot.
     */
    public Money calculateCostBasis(ShareQuantity quantity) {
        return unitPrice.multiply(quantity);
    }

    public ShareQuantity getRemainingShares() {
        return remainingShares;
    }

    public boolean isEmpty() {
        return remainingShares.isZero();
    }
}
