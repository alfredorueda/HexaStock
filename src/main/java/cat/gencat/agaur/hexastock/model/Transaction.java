package cat.gencat.agaur.hexastock.model;

import java.time.LocalDateTime;

/**
 * Transaction represents a financial activity within a portfolio.
 * 
 * In DDD terms, this is a Value Object that records a financial event.
 * Each transaction is immutable and represents one of four activities:
 * - Deposits (adding money to the portfolio)
 * - Withdrawals (removing money from the portfolio)
 * - Purchases (buying stocks)
 * - Sales (selling stocks)
 * 
 * Think of Transactions as your financial history or ledger.
 * They track every money movement and stock trade, allowing you to:
 * - Review your investment history
 * - Calculate profits and losses
 * - Analyze your investment performance over time
 * 
 * Transactions are created through factory methods that ensure all required data
 * for each type of transaction is properly recorded.
 *
 * #########
 *
 * Why Separating Transactions from the Portfolio Aggregate is Better
 *
 * There are several significant benefits to this approach compared to including
 * transactions directly within the Portfolio aggregate:
 *
 *
 * Key Benefits of Separate Transaction Aggregate
 * Performance and Memory Efficiency
 *
 *
 * A portfolio with 20,000+ transactions would create a massive aggregate if loaded entirely
 * The current approach allows loading just the portfolio with its positions (current state) without the entire transaction history
 * Memory usage is significantly reduced for common operations that don't need transaction details
 * Transactional Boundaries
 *
 *
 * Each portfolio operation doesn't need to lock the entire transaction history
 * Transaction creation/modification can happen independently of other portfolio operations
 * Reduced contention in high-throughput systems with many concurrent users
 * Scalability
 *
 *
 * The system can scale to handle portfolios with unlimited transaction history
 * Historical transactions can be archived or stored differently from active positions
 * Specialized queries can be optimized for different access patterns
 * Flexibility in Data Access
 *
 *
 * The PerformanceMetricsService can load transactions in batches, with pagination, or filtered
 * Different read models can be created for different calculation needs
 * Time-series or date-range queries become more efficient
 * Maintains Domain Invariants
 *
 *
 * The Portfolio still enforces critical invariants through the applyTransaction method
 * Position-related consistency (can't sell more than you own) is maintained
 * The domain model accurately reflects the business reality: a portfolio's current state (positions) vs. its history (transactions)
 * Improved Testability
 *
 *
 * Easier to test portfolio operations without setting up extensive transaction history
 * Performance tests can be conducted with realistic transaction volumes
 * Different components can be tested in isolation
 * The code implements a pattern where:
 *
 *
 * Portfolio maintains the current state (positions)
 * Transaction records historical events that led to the current state
 * PerformanceMetricsService handles complex calculations across both entities
 * This separation follows the Command-Query Responsibility Segregation (CQRS) pattern naturally, where commands update the portfolio state while queries use specialized models for efficient reporting.
 *
 * In DDD terms, this design respects aggregate boundaries based on consistency
 * requirements while optimizing for the reality of large transaction volumes in financial systems.
 */
public class Transaction {
    private TransactionId id;
    private PortfolioId portfolioId;
    private TransactionType type;
    private Ticker ticker;
    private ShareQuantity quantity;
    private Price unitPrice;
    private Money totalAmount;
    private Money profit;
    private LocalDateTime createdAt;

    protected Transaction() {}

    public Transaction(TransactionId id, PortfolioId portfolioId, TransactionType type, Ticker ticker,
                       ShareQuantity quantity, Price unitPrice, Money totalAmount,
                       Money profit, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.type = type;
        this.ticker = ticker;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.profit = profit;
        this.createdAt = createdAt;
    }

    public static Transaction createDeposit(PortfolioId portfolioId, Money amount) {
        return new Transaction(
                TransactionId.generate(),
                portfolioId,
                TransactionType.DEPOSIT,
                null,
                ShareQuantity.ZERO,
                null,
                amount,
                Money.ZERO,
                LocalDateTime.now()
        );
    }

    public static Transaction createWithdrawal(PortfolioId portfolioId, Money amount) {
        return new Transaction(
                TransactionId.generate(),
                portfolioId,
                TransactionType.WITHDRAWAL,
                null,
                ShareQuantity.ZERO,
                null,
                amount,
                Money.ZERO,
                LocalDateTime.now()
        );
    }

    public static Transaction createPurchase(PortfolioId portfolioId, Ticker ticker,
                                             ShareQuantity quantity, Price unitPrice) {
        Money totalAmount = unitPrice.multiply(quantity);
        return new Transaction(
                TransactionId.generate(),
                portfolioId,
                TransactionType.PURCHASE,
                ticker,
                quantity,
                unitPrice,
                totalAmount,
                Money.ZERO,
                LocalDateTime.now()
        );
    }

    public static Transaction createSale(PortfolioId portfolioId, Ticker ticker,
                                         ShareQuantity quantity, Price unitPrice,
                                         Money totalAmount, Money profit) {
        return new Transaction(
                TransactionId.generate(),
                portfolioId,
                TransactionType.SALE,
                ticker,
                quantity,
                unitPrice,
                totalAmount,
                profit,
                LocalDateTime.now()
        );
    }

    public TransactionId getId() {
        return id;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public TransactionType getType() {
        return type;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public ShareQuantity getQuantity() {
        return quantity;
    }

    public Price getUnitPrice() {
        return unitPrice;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getProfit() {
        return profit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
