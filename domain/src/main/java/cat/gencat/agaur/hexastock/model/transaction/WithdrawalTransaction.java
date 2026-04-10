package cat.gencat.agaur.hexastock.model.transaction;

import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A cash withdrawal from the portfolio.
 *
 * <p>Contains only the fields meaningful for a withdrawal: portfolio identity,
 * the withdrawn amount, and a timestamp.</p>
 */
public record WithdrawalTransaction(
        TransactionId id,
        PortfolioId portfolioId,
        Money totalAmount,
        LocalDateTime createdAt
) implements Transaction {

    public WithdrawalTransaction {
        Objects.requireNonNull(id, "Transaction id must not be null");
        Objects.requireNonNull(portfolioId, "Portfolio id must not be null");
        Objects.requireNonNull(totalAmount, "Total amount must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
    }

    @Override
    public TransactionType type() {
        return TransactionType.WITHDRAWAL;
    }
}
