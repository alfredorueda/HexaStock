package cat.gencat.agaur.hexastock.portfolios.model.transaction;

import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.*;

import java.time.LocalDateTime;

/**
 * Transaction represents a financial activity within a portfolio.
 *
 * <p>In DDD terms, this is an <strong>immutable ledger entry</strong> — an entity
 * with identity, created by the application layer after the aggregate mutation,
 * persisted for auditability and reporting, but never modified after creation.</p>
 *
 * <p>The sealed hierarchy models the four semantically distinct transaction kinds
 * as separate types, each carrying only the fields meaningful to that kind.
 * This eliminates nullable/meaningless fields and enables exhaustive pattern
 * matching in Java 21.</p>
 *
 * <h3>Subtypes</h3>
 * <ul>
 *   <li>{@link DepositTransaction} — cash deposited into the portfolio</li>
 *   <li>{@link WithdrawalTransaction} — cash withdrawn from the portfolio</li>
 *   <li>{@link PurchaseTransaction} — shares purchased at a given price</li>
 *   <li>{@link SaleTransaction} — shares sold, recording proceeds and profit</li>
 * </ul>
 *
 * <h3>Why Transactions Are Separate from the Portfolio Aggregate</h3>
 * <p>Transaction history is append-only and unbounded. Including it in the
 * Portfolio aggregate would force loading all historical transactions for every
 * operation. Transactions do not participate in the invariants that the Portfolio
 * aggregate protects (cash sufficiency, share availability, FIFO ordering).</p>
 */
public sealed interface Transaction
        permits DepositTransaction, WithdrawalTransaction, PurchaseTransaction, SaleTransaction {

    /** Unique identity of this transaction. */
    TransactionId id();

    /** The portfolio this transaction belongs to. */
    PortfolioId portfolioId();

    /** The type discriminator (for persistence and switch expressions). */
    TransactionType type();

    /** The total monetary amount of this transaction. */
    Money totalAmount();

    /** When this transaction was created. */
    LocalDateTime createdAt();

    // ── Convenience accessors with safe defaults for non-stock transactions ──

    /**
     * Returns the ticker for stock transactions, or {@code null} for cash transactions.
     * Prefer pattern matching over this method when possible.
     */
    default Ticker ticker() { return null; }

    /**
     * Returns the share quantity for stock transactions, or {@link ShareQuantity#ZERO}
     * for cash transactions.
     */
    default ShareQuantity quantity() { return ShareQuantity.ZERO; }

    /**
     * Returns the unit price for stock transactions, or {@code null} for cash transactions.
     */
    default Price unitPrice() { return null; }

    /**
     * Returns the profit for sale transactions, or {@link Money#ZERO} for all others.
     */
    default Money profit() { return Money.ZERO; }

    // ── Factory methods for convenience ─────────────────────────────────

    static DepositTransaction createDeposit(PortfolioId portfolioId, Money amount) {
        return new DepositTransaction(
                TransactionId.generate(), portfolioId, amount, LocalDateTime.now());
    }

    static WithdrawalTransaction createWithdrawal(PortfolioId portfolioId, Money amount) {
        return new WithdrawalTransaction(
                TransactionId.generate(), portfolioId, amount, LocalDateTime.now());
    }

    static PurchaseTransaction createPurchase(PortfolioId portfolioId, Ticker ticker,
                                              ShareQuantity quantity, Price unitPrice) {
        return new PurchaseTransaction(
                TransactionId.generate(), portfolioId, ticker, quantity, unitPrice,
                unitPrice.multiply(quantity), LocalDateTime.now());
    }

    static SaleTransaction createSale(PortfolioId portfolioId, Ticker ticker,
                                      ShareQuantity quantity, Price unitPrice,
                                      Money totalAmount, Money profit) {
        return new SaleTransaction(
                TransactionId.generate(), portfolioId, ticker, quantity, unitPrice,
                totalAmount, profit, LocalDateTime.now());
    }
}
