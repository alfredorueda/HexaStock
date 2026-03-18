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

    /** SEC T+2 settlement rule: lots settle 2 business days after purchase. */
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
     * Constructs a Lot with the specified attributes.
     *
     * @param id The unique identifier for the lot
     * @param initialShares The number of shares initially purchased
     * @param remainingShares The number of shares currently remaining
     * @param unitPrice The price paid per share
     * @param purchasedAt The date and time of purchase
     */
    public Lot(LotId id, ShareQuantity initialShares, ShareQuantity remainingShares, Price unitPrice, LocalDateTime purchasedAt) {
        this(id, initialShares, remainingShares, unitPrice, purchasedAt,
                purchasedAt.plusDays(SETTLEMENT_DAYS), false);
    }

    public Lot(LotId id, ShareQuantity initialShares, ShareQuantity remainingShares,
               Price unitPrice, LocalDateTime purchasedAt,
               LocalDateTime settlementDate, boolean reserved) {
        Objects.requireNonNull(id, "Lot id must not be null");
        Objects.requireNonNull(initialShares, "Initial shares must not be null");
        Objects.requireNonNull(remainingShares, "Remaining shares must not be null");
        Objects.requireNonNull(unitPrice, "Unit price must not be null");
        Objects.requireNonNull(purchasedAt, "Purchase date must not be null");
        Objects.requireNonNull(settlementDate, "Settlement date must not be null");

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

    /**
     * Checks if this lot has settled (T+2 rule satisfied).
     *
     * @param asOf The reference date/time to check against
     * @return true if the current date is on or after the settlement date
     */
    public boolean isSettled(LocalDateTime asOf) {
        return !asOf.isBefore(settlementDate);
    }

    /**
     * Checks if this lot is available for sale: must be settled AND not reserved.
     *
     * @param asOf The reference date/time to check against
     * @return true if the lot can participate in a sell operation
     */
    public boolean isAvailableForSale(LocalDateTime asOf) {
        return isSettled(asOf) && !reserved;
    }

    /**
     * Returns the number of shares available for sale from this lot.
     *
     * @param asOf The reference date/time to check settlement
     * @return remainingShares if available, ZERO otherwise
     */
    public ShareQuantity availableShares(LocalDateTime asOf) {
        return isAvailableForSale(asOf) ? remainingShares : ShareQuantity.ZERO;
    }

    /**
     * Marks this lot as reserved (e.g., used as collateral or pending transfer).
     */
    public void reserve() {
        this.reserved = true;
    }

    /**
     * Removes the reservation from this lot, making it available for sale again.
     */
    public void unreserve() {
        this.reserved = false;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public boolean isReserved() {
        return reserved;
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
