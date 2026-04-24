package cat.gencat.agaur.hexastock.portfolios.model.transaction;

import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A cash deposit into the portfolio.
 *
 * <p>Contains only the fields meaningful for a deposit: portfolio identity,
 * the deposited amount, and a timestamp. No ticker, quantity, unit price,
 * or profit fields — those belong to stock transactions.</p>
 */
public record DepositTransaction(
        TransactionId id,
        PortfolioId portfolioId,
        Money totalAmount,
        LocalDateTime createdAt
) implements Transaction {

    public DepositTransaction {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Objects.requireNonNull(portfolioId, "Portfolio id must not be null");
        Objects.requireNonNull(totalAmount, "Total amount must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
    }

    @Override
    public TransactionType type() {
        return TransactionType.DEPOSIT;
    }
}
