package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lot represents a specific purchase of shares for a particular stock.
 *
 * <p>In DDD terms, this is an <strong>Entity</strong> that belongs to the Holding aggregate.
 * It tracks the details of a single stock purchase transaction.</p>
 */
public class Lot {
    public static final int SETTLEMENT_DAYS = 2;

    private LotId id;
    private ShareQuantity initialShares;
    private ShareQuantity remainingShares;
    private Price unitPrice;
    private LocalDateTime purchasedAt;
    private LocalDateTime settlementDate;
    private boolean reserved;

    protected Lot() {}

    /**
     * Constructs a Lot with the specified attributes (backward-compatible 5-arg).
     */
    public Lot(LotId id, ShareQuantity initialShares, ShareQuantity remainingShares, Price unitPrice, LocalDateTime purchasedAt) {
        this(id, initialShares, remainingShares, unitPrice, purchasedAt,
             purchasedAt.plusDays(SETTLEMENT_DAYS), false);
    }

    /**
     * Constructs a Lot with settlement date and reservation flag.
     */
    public Lot(LotId id, ShareQuantity initialShares, ShareQuantity remainingShares,
               Price unitPrice, LocalDateTime purchasedAt,
               LocalDateTime settlementDate, boolean reserved) {
        Objects.requireNonNull(id, "Lot id must not be null");
        Objects.requireNonNull(initialShares, "Initial shares must not be null");
        Objects.requireNonNull(remainingShares, "Remaining shares must not be null");
        Objects.requireNonNull(unitPrice, "Unit price must not be null");
        Objects.requireNonNull(purchasedAt, "Purchase date must not be null");

        this.id = id;
        this.initialShares = initialShares;
        this.remainingShares = remainingShares;
        this.unitPrice = unitPrice;
        this.purchasedAt = purchasedAt;
        this.settlementDate = settlementDate;
        this.reserved = reserved;
    }

    /**
     * Factory method to create a new Lot for a purchase.
     *
     * @param quantity The number of shares purchased
     * @param unitPrice The price paid per share
     * @return A new Lot instance
     */
    public static Lot create(ShareQuantity quantity, Price unitPrice) {
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Lot(LotId.generate(), quantity, quantity, unitPrice, now,
                       now.plusDays(SETTLEMENT_DAYS), false);
    }

    /**
     * Reduces the number of remaining shares in this lot.
     *
     * @param quantity The number of shares to reduce
     * @throws ConflictQuantityException if attempting to reduce by more shares than remain
     */
    public void reduce(ShareQuantity quantity) {
        if (quantity.value() > remainingShares.value()) {
            throw new ConflictQuantityException("Cannot reduce by more than remaining quantity");
        }
        remainingShares = remainingShares.subtract(quantity);
    }

    /**
     * Calculates the cost basis for a given quantity of shares from this lot.
     *
     * @param quantity The number of shares
     * @return The cost basis as Money
     */
    public Money calculateCostBasis(ShareQuantity quantity) {
        return unitPrice.multiply(quantity);
    }

    public LotId getId() {
        return id;
    }

    public ShareQuantity getInitialShares() {
        return initialShares;
    }

    public ShareQuantity getRemainingShares() {
        return remainingShares;
    }

    public Price getUnitPrice() {
        return unitPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public boolean isEmpty() {
        return remainingShares.isZero();
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    /**
     * Checks if this lot has settled (T+2 rule satisfied).
     */
    public boolean isSettled(LocalDateTime asOf) {
        return !asOf.isBefore(settlementDate);
    }

    /**
     * Checks if this lot is available for sale: settled and not reserved.
     *
     * <p>Note: in the anemic model this is a passive data query — the service
     *    is responsible for actually enforcing the constraint during sell.</p>
     */
    public boolean isAvailableForSale(LocalDateTime asOf) {
        // BUG: only checks settlement, forgets reservation flag
        // This is a realistic drift — the settlement check was added first,
        // and reservation was added later in a different sprint without
        // updating this convenience method.
        return isSettled(asOf);
    }

    /**
     * Returns the number of shares available for sale from this lot.
     */
    public ShareQuantity availableShares(LocalDateTime asOf) {
        return isAvailableForSale(asOf) ? remainingShares : ShareQuantity.ZERO;
    }

    /**
     * Marks this lot as reserved.
     */
    public void reserve() {
        this.reserved = true;
    }

    /**
     * Removes the reservation from this lot.
     */
    public void unreserve() {
        this.reserved = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lot lot)) return false;
        return Objects.equals(id, lot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
