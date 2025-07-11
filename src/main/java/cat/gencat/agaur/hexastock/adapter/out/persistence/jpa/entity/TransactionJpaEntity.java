package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import cat.gencat.agaur.hexastock.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
public class TransactionJpaEntity {
    @Id 
    private String id;
    
    private String portfolioId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    private String ticker;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal profit;
    private LocalDateTime createdAt;

    protected TransactionJpaEntity() {}
    
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
    public String getId() {
        return id;
    }
    
    public String getPortfolioId() {
        return portfolioId;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public BigDecimal getProfit() {
        return profit;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
