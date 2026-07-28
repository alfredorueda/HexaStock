package com.neueda.portfolio.domain.model;

import com.neueda.portfolio.domain.exception.ConflictQuantityException;
import com.neueda.portfolio.domain.exception.InvalidQuantityException;
import com.neueda.portfolio.domain.vo.LotId;
import com.neueda.portfolio.domain.vo.Money;
import com.neueda.portfolio.domain.vo.Price;
import com.neueda.portfolio.domain.vo.ShareQuantity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One purchase of shares at one price, at one moment in time.
 *
 * <p>A lot is the unit FIFO consumes: {@code initialShares} never changes, while
 * {@code remainingShares} shrinks as sales take shares out of it. When it reaches
 * zero the lot is empty and its holding drops it.
 */
public class Lot {

    private final LotId id;
    private final ShareQuantity initialShares;
    private ShareQuantity remainingShares;
    private final Price unitPrice;
    private final LocalDateTime purchasedAt;

    private Lot(LotId id, ShareQuantity initialShares, Price unitPrice, LocalDateTime purchasedAt) {
        this.id = Objects.requireNonNull(id, "Lot id cannot be null");
        this.initialShares = Objects.requireNonNull(initialShares, "initialShares cannot be null");
        this.remainingShares = initialShares;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        this.purchasedAt = Objects.requireNonNull(purchasedAt, "purchasedAt cannot be null");
    }

    /** Creates a lot of freshly bought shares. The quantity must be positive (spec AC-19). */
    public static Lot create(ShareQuantity shares, Price unitPrice, LocalDateTime purchasedAt) {
        Objects.requireNonNull(shares, "shares cannot be null");
        if (!shares.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + shares.value());
        }
        return new Lot(LotId.generate(), shares, unitPrice, purchasedAt);
    }

    /**
     * Takes {@code quantity} shares out of this lot.
     *
     * @throws ConflictQuantityException if the lot does not hold that many shares
     */
    public void reduce(ShareQuantity quantity) {
        Objects.requireNonNull(quantity, "quantity cannot be null");
        if (quantity.value() > remainingShares.value()) {
            throw new ConflictQuantityException(
                    "Not enough shares to sell. Available: " + remainingShares.value()
                            + ", Requested: " + quantity.value());
        }
        remainingShares = remainingShares.subtract(quantity);
    }

    /** What {@code quantity} shares from this lot originally cost: {@code quantity × unitPrice}. */
    public Money calculateCostBasis(ShareQuantity quantity) {
        Objects.requireNonNull(quantity, "quantity cannot be null");
        return unitPrice.multiply(quantity);
    }

    public ShareQuantity getRemainingShares() {
        return remainingShares;
    }

    public boolean isEmpty() {
        return remainingShares.isZero();
    }

    public LotId getId() {
        return id;
    }

    public ShareQuantity getInitialShares() {
        return initialShares;
    }

    public Price getUnitPrice() {
        return unitPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    @Override
    public String toString() {
        return remainingShares + "/" + initialShares + " @ " + unitPrice;
    }
}
