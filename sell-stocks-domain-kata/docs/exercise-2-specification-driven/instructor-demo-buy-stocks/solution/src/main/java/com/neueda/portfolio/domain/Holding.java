package com.neueda.portfolio.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The position a portfolio owns in one ticker, as a list of lots kept in strict purchase
 * order. Selling consumes them oldest-first; buying appends a new one to the end.
 */
public class Holding {

    private final HoldingId id;
    private final Ticker ticker;
    private final List<Lot> lots = new ArrayList<>();

    public Holding(Ticker ticker) {
        this.id = new HoldingId(UUID.randomUUID().toString());
        this.ticker = Objects.requireNonNull(ticker, "Ticker cannot be null");
    }

    /**
     * FIFO sale. The whole quantity is checked against the position before anything is
     * mutated, so a rejected sale leaves every lot exactly as it was.
     */
    public SellResult sell(ShareQuantity quantity, Price price) {
        ShareQuantity available = getTotalShares();
        if (quantity.value() > available.value()) {
            throw new ConflictQuantityException(
                    "Not enough shares to sell. Available: " + available.value()
                            + ", Requested: " + quantity.value());
        }

        Money costBasis = new Money(BigDecimal.ZERO);
        ShareQuantity stillToSell = quantity;

        Iterator<Lot> oldestFirst = lots.iterator();
        while (!stillToSell.isZero()) {
            Lot lot = oldestFirst.next();
            ShareQuantity taken = lot.getRemainingShares().min(stillToSell);
            costBasis = costBasis.add(lot.calculateCostBasis(taken));
            lot.reduce(taken);
            stillToSell = stillToSell.subtract(taken);
            if (lot.isEmpty()) {
                oldestFirst.remove();
            }
        }

        Money proceeds = price.multiply(quantity);
        return new SellResult(proceeds, costBasis, proceeds.subtract(costBasis));
    }

    /**
     * Appends a new lot to the end of the list. It never merges with an existing lot,
     * even when the price matches exactly, and existing lots are never reordered.
     */
    public void buy(ShareQuantity quantity, Price price) {
        lots.add(new Lot(quantity, price));
    }

    public ShareQuantity getTotalShares() {
        int total = 0;
        for (Lot lot : lots) {
            total += lot.getRemainingShares().value();
        }
        return new ShareQuantity(total);
    }

    public List<Lot> getLots() {
        return List.copyOf(lots);
    }
}
