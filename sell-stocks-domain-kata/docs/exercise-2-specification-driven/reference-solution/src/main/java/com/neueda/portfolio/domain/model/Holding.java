package com.neueda.portfolio.domain.model;

import com.neueda.portfolio.domain.exception.ConflictQuantityException;
import com.neueda.portfolio.domain.exception.InvalidQuantityException;
import com.neueda.portfolio.domain.vo.HoldingId;
import com.neueda.portfolio.domain.vo.Money;
import com.neueda.portfolio.domain.vo.Price;
import com.neueda.portfolio.domain.vo.SellResult;
import com.neueda.portfolio.domain.vo.ShareQuantity;
import com.neueda.portfolio.domain.vo.Ticker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Everything the portfolio owns of one ticker, as an ordered list of purchase lots.
 *
 * <p>The list is kept in purchase order — each {@link #buy} appends — which is what
 * makes FIFO a plain forward traversal: index 0 is always the oldest lot.
 */
public class Holding {

    private final HoldingId id;
    private final Ticker ticker;
    private final List<Lot> lots = new ArrayList<>();

    private Holding(HoldingId id, Ticker ticker) {
        this.id = Objects.requireNonNull(id, "Holding id cannot be null");
        this.ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
    }

    public static Holding create(Ticker ticker) {
        return new Holding(HoldingId.generate(), ticker);
    }

    /**
     * Adds a lot of freshly bought shares at the end of the list, keeping it in
     * purchase order.
     *
     * <p>Buying is US-06 and out of scope for this kata: this method exists only so a
     * holding can be brought into the state US-07 sells from. It has no cash effect —
     * see {@link Portfolio#buy}.
     */
    public void buy(ShareQuantity quantity, Price price) {
        lots.add(Lot.create(quantity, price, LocalDateTime.now()));
    }

    /**
     * Sells {@code quantity} shares at {@code price}, consuming lots oldest-first.
     *
     * <p>The whole sale is validated before a single lot is touched, so a rejected sale
     * leaves every lot exactly as it was (spec AC-16).
     *
     * @throws InvalidQuantityException  if the quantity is not positive (AC-10)
     * @throws ConflictQuantityException if the holding has fewer shares than requested (AC-12)
     */
    public SellResult sell(ShareQuantity quantity, Price price) {
        Objects.requireNonNull(quantity, "quantity cannot be null");
        Objects.requireNonNull(price, "price cannot be null");

        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + quantity.value());
        }
        ShareQuantity available = getTotalShares();
        if (quantity.value() > available.value()) {
            throw new ConflictQuantityException(
                    "Not enough shares to sell. Available: " + available.value()
                            + ", Requested: " + quantity.value());
        }

        // FIFO: walk the lots oldest-first, taking min(lot remaining, still to sell)
        // from each, and dropping any lot the sale empties.
        Money costBasis = Money.ZERO;
        ShareQuantity stillToSell = quantity;
        Iterator<Lot> it = lots.iterator();
        while (!stillToSell.isZero()) {
            Lot lot = it.next();
            ShareQuantity taken = lot.getRemainingShares().min(stillToSell);
            costBasis = costBasis.add(lot.calculateCostBasis(taken));
            lot.reduce(taken);
            stillToSell = stillToSell.subtract(taken);
            if (lot.isEmpty()) {
                it.remove();
            }
        }

        // If that consumed the last lot, this holding is now empty. Discarding it is
        // Portfolio's job — it owns the holdings map. See Portfolio.sell (AC-22).

        return SellResult.of(price.multiply(quantity), costBasis);
    }

    /** Shares actually still owned: the sum of every lot's remaining shares. */
    public ShareQuantity getTotalShares() {
        int total = 0;
        for (Lot lot : lots) {
            total += lot.getRemainingShares().value();
        }
        return ShareQuantity.of(total);
    }

    /** The lots in purchase order, oldest first. Read-only: sales go through {@link #sell}. */
    public List<Lot> getLots() {
        return Collections.unmodifiableList(lots);
    }

    public HoldingId getId() {
        return id;
    }

    public Ticker getTicker() {
        return ticker;
    }

    @Override
    public String toString() {
        return ticker + " " + lots;
    }
}
