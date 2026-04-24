package cat.gencat.agaur.hexastock.portfolios.model.transaction;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.money.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A stock purchase — shares bought at a given unit price.
 *
 * <p>Contains the ticker, quantity, unit price, and computed total cost.
 * The total cost is always {@code unitPrice × quantity}; the constructor
 * receives it as a parameter for flexibility in reconstitution from persistence,
 * but the factory method computes it automatically.</p>
 */
public record PurchaseTransaction(
        TransactionId id,
        PortfolioId portfolioId,
        Ticker ticker,
        ShareQuantity quantity,
        Price unitPrice,
        Money totalAmount,
        LocalDateTime createdAt
) implements Transaction {

    public PurchaseTransaction {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Objects.requireNonNull(portfolioId, "Portfolio id must not be null");
        Objects.requireNonNull(ticker, "Ticker must not be null for a purchase");
        Objects.requireNonNull(quantity, "Quantity must not be null");
        Objects.requireNonNull(unitPrice, "Unit price must not be null for a purchase");
        Objects.requireNonNull(totalAmount, "Total amount must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Purchase quantity must be positive");
        }
    }

    @Override
    public TransactionType type() {
        return TransactionType.PURCHASE;
    }

    @Override
    public Ticker ticker() { return ticker; }

    @Override
    public ShareQuantity quantity() { return quantity; }

    @Override
    public Price unitPrice() { return unitPrice; }
}
