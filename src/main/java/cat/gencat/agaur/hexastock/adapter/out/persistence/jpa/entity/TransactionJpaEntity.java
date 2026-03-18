package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import cat.gencat.agaur.hexastock.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionJpaEntity is a JPA entity representing the Transaction domain model in the database.
 * 
 * <p>In hexagonal architecture terms, this class is part of the <strong>secondary adapter</strong> layer
 * (persistence infrastructure) that provides a database representation of transaction records
 * from the domain model.</p>
 * 
 * <p>This entity:</p>
 * <ul>
 *   <li>Maps the Transaction domain object to a relational database structure</li>
 *   <li>Stores financial transaction details for auditing and reporting purposes</li>
 *   <li>Records different types of transactions (deposits, withdrawals, purchases, sales)</li>
 * </ul>
 * 
 * <p>The entity corresponds to the "transaction" table in the database and captures
 * all financial activities within the system, providing a complete audit trail
 * of portfolio operations.</p>
 */
@Entity
@Table(name = "transaction")
public class TransactionJpaEntity {
    /**
     * The unique identifier of the transaction.
     */
    @Id 
    private String id;
    
    /**
     * The ID of the portfolio this transaction belongs to.
     */
    private String portfolioId;
    
    /**
     * The type of transaction (DEPOSIT, WITHDRAWAL, PURCHASE, SALE).
     * Stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    /**
     * The ticker symbol of the stock involved in the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     */
    private String ticker;
    
    /**
     * The quantity of shares involved in the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     */
    private int quantity;
    
    /**
     * The price per share at the time of the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     */
    private BigDecimal unitPrice;
    
    /**
     * The total monetary amount of the transaction.
     * For deposits and withdrawals, this is the cash amount.
     * For purchases and sales, this is the total cost or proceeds.
     */
    private BigDecimal totalAmount;
    
    /**
     * The profit or loss from a sale transaction.
     * Only applicable for SALE transactions.
     */
    private BigDecimal profit;

    /**
     * The fee charged for a sale transaction.
     * Only applicable for SALE transactions.
     */
    private BigDecimal fee;
    
    /**
     * The timestamp when the transaction occurred.
     */
    private LocalDateTime createdAt;

    /**
     * Protected default constructor required by JPA.
     */
    protected TransactionJpaEntity() {}

    /** Returns a new {@link Builder} for constructing a {@code TransactionJpaEntity}. */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    /**
     * Returns the unique identifier of the transaction.
     * 
     * @return The transaction ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns the ID of the portfolio this transaction belongs to.
     * 
     * @return The portfolio ID
     */
    public String getPortfolioId() {
        return portfolioId;
    }
    
    /**
     * Returns the type of transaction.
     * 
     * @return The transaction type (DEPOSIT, WITHDRAWAL, PURCHASE, SALE)
     */
    public TransactionType getType() {
        return type;
    }
    
    /**
     * Returns the ticker symbol of the stock involved in the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     * 
     * @return The ticker symbol or null if not applicable
     */
    public String getTicker() {
        return ticker;
    }
    
    /**
     * Returns the quantity of shares involved in the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     * 
     * @return The quantity of shares or 0 if not applicable
     */
    public int getQuantity() {
        return quantity;
    }
    
    /**
     * Returns the price per share at the time of the transaction.
     * Only applicable for PURCHASE and SALE transactions.
     * 
     * @return The unit price or null if not applicable
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    /**
     * Returns the total monetary amount of the transaction.
     * 
     * @return The total amount
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    /**
     * Returns the profit or loss from a sale transaction.
     * Only applicable for SALE transactions.
     * 
     * @return The profit amount or null if not applicable
     */
    public BigDecimal getProfit() {
        return profit;
    }

    public BigDecimal getFee() {
        return fee;
    }
    
    /**
     * Returns the timestamp when the transaction occurred.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Fluent builder for {@link TransactionJpaEntity}.
     * Eliminates the need for a constructor with more than seven parameters.
     */
    public static class Builder {
        private String id;
        private String portfolioId;
        private TransactionType type;
        private String ticker;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private BigDecimal profit;
        private BigDecimal fee;
        private LocalDateTime createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder portfolioId(String portfolioId) { this.portfolioId = portfolioId; return this; }
        public Builder type(TransactionType type) { this.type = type; return this; }
        public Builder ticker(String ticker) { this.ticker = ticker; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder profit(BigDecimal profit) { this.profit = profit; return this; }
        public Builder fee(BigDecimal fee) { this.fee = fee; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TransactionJpaEntity build() {
            var entity = new TransactionJpaEntity();
            entity.id = this.id;
            entity.portfolioId = this.portfolioId;
            entity.type = this.type;
            entity.ticker = this.ticker;
            entity.quantity = this.quantity;
            entity.unitPrice = this.unitPrice;
            entity.totalAmount = this.totalAmount;
            entity.profit = this.profit;
            entity.fee = this.fee;
            entity.createdAt = this.createdAt;
            return entity;
        }
    }
}
