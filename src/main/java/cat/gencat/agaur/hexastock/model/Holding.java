package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.EntityExistsException;

import java.util.*;

/**
 * Holding represents the ownership of a specific stock within a portfolio.
 *
 * <p>In DDD terms, this is an <strong>Entity</strong> that belongs to the Portfolio aggregate.
 * It tracks all lots (purchases) of a particular stock and handles the selling
 * process using the FIFO (First-In-First-Out) accounting method.</p>
 */
public class Holding {
    private HoldingId id;
    private Ticker ticker;
    private final List<Lot> lots = new ArrayList<>();

    protected Holding() {}

    public Holding(HoldingId id, Ticker ticker) {
        Objects.requireNonNull(id, "Holding id must not be null");
        Objects.requireNonNull(ticker, "Ticker must not be null");
        this.id = id;
        this.ticker = ticker;
    }

    public static Holding create(Ticker ticker) {
        return new Holding(HoldingId.generate(), ticker);
    }

    /**
     * Buys shares of this stock, creating a new Lot to track the purchase.
     *
     * @param quantity The number of shares to buy
     * @param unitPrice The price per share
     */
    public void buy(ShareQuantity quantity, Price unitPrice) {
        Lot lot = Lot.create(quantity, unitPrice);
        lots.add(lot);
    }

    /**
     * Sells shares of this stock using the FIFO (First-In-First-Out) accounting method.
     *
     * @param quantity The number of shares to sell
     * @param sellPrice The current market price per share
     * @return A SellResult containing proceeds, cost basis, and profit information
     * @throws ConflictQuantityException if there are not enough shares to sell
     */
    public SellResult sell(ShareQuantity quantity, Price sellPrice) {
        if (getTotalShares().value() < quantity.value()) {
            throw new ConflictQuantityException(
                    "Not enough shares to sell. Available: " + getTotalShares() + ", Requested: " + quantity);
        }

        ShareQuantity remainingToSell = quantity;
        Money costBasis = Money.ZERO;

        for (var lot : lots) {
            if (remainingToSell.isZero()) break;

            ShareQuantity sharesSoldFromLot = lot.getRemainingShares().min(remainingToSell);
            Money lotCostBasis = lot.calculateCostBasis(sharesSoldFromLot);

            costBasis = costBasis.add(lotCostBasis);
            lot.reduce(sharesSoldFromLot);
            remainingToSell = remainingToSell.subtract(sharesSoldFromLot);
        }

        lots.removeIf(Lot::isEmpty);

        Money proceeds = sellPrice.multiply(quantity);
        return SellResult.of(proceeds, costBasis);
    }

    public ShareQuantity getTotalShares() {
        return lots.stream()
                .map(Lot::getRemainingShares)
                .reduce(ShareQuantity.ZERO, ShareQuantity::add);
    }

    public HoldingId getId() {
        return id;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public List<Lot> getLots() {
        return List.copyOf(lots);
    }

    public void addLot(Lot lot) {
        boolean exists = lots.stream().anyMatch(l -> l.getId().equals(lot.getId()));
        if (exists) {
            throw new EntityExistsException("Lot " + lot.getId() + " already exists");
        }
        lots.add(lot);
    }

    public Money getRemainingSharesPurchasePrice() {
        return lots.stream()
                .map(l -> l.getUnitPrice().multiply(l.getRemainingShares()))
                .reduce(Money.ZERO, Money::add);
    }

    public Money getTheoreticSalePrice(Price currentPrice) {
        return lots.stream()
                .map(l -> currentPrice.multiply(l.getRemainingShares()))
                .reduce(Money.ZERO, Money::add);
    }

    public Money getUnrealizedGain(Price currentPrice) {
        return getTheoreticSalePrice(currentPrice).subtract(getRemainingSharesPurchasePrice());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Holding holding)) return false;
        return Objects.equals(id, holding.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
