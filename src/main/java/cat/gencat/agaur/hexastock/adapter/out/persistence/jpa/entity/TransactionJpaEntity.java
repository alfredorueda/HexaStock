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
     * The timestamp when the transaction occurred.
     */
    private LocalDateTime createdAt;

    /**
     * Protected default constructor required by JPA.
     */
    protected TransactionJpaEntity() {}
    
    /**
     * Constructs a new TransactionJpaEntity with the specified properties.
     * 
     * @param id The unique identifier of the transaction
     * @param portfolioId The ID of the portfolio this transaction belongs to
     * @param type The type of transaction
     * @param ticker The ticker symbol (for stock transactions)
     * @param quantity The quantity of shares (for stock transactions)
     * @param unitPrice The price per share (for stock transactions)
     * @param totalAmount The total monetary amount of the transaction
     * @param profit The profit or loss (for sale transactions)
     * @param createdAt The timestamp when the transaction occurred
     */
    public TransactionJpaEntity(String id, String portfolioId, TransactionType type, String ticker,
                                 int quantity, BigDecimal unitPrice, BigDecimal totalAmount,
                                 BigDecimal profit, LocalDateTime createdAt) {
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
    
    /**
     * Returns the timestamp when the transaction occurred.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
