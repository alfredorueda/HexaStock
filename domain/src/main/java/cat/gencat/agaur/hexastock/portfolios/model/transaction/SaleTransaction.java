package cat.gencat.agaur.hexastock.portfolios.model.transaction;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.money.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A stock sale — shares sold at a given unit price, with recorded profit.
 *
 * <p>Contains the ticker, quantity, unit price, total proceeds, and the
 * realised profit (proceeds minus cost basis). This is the only transaction
 * subtype that carries a profit field.</p>
 */
public record SaleTransaction(
        TransactionId id,
        PortfolioId portfolioId,
        Ticker ticker,
        ShareQuantity quantity,
        Price unitPrice,
        Money totalAmount,
        Money profit,
        LocalDateTime createdAt
) implements Transaction {

    public SaleTransaction {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Objects.requireNonNull(portfolioId, "Portfolio id must not be null");
        Objects.requireNonNull(ticker, "Ticker must not be null for a sale");
        Objects.requireNonNull(quantity, "Quantity must not be null");
        Objects.requireNonNull(unitPrice, "Unit price must not be null for a sale");
        Objects.requireNonNull(totalAmount, "Total amount must not be null");
        Objects.requireNonNull(profit, "Profit must not be null for a sale");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Sale quantity must be positive");
        }
    }

    @Override
    public TransactionType type() {
        return TransactionType.SALE;
    }

    @Override
    public Ticker ticker() { return ticker; }

    @Override
    public ShareQuantity quantity() { return quantity; }

    @Override
    public Price unitPrice() { return unitPrice; }

    @Override
    public Money profit() { return profit; }
}
