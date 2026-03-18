package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lot")
public class LotJpaEntity {
    @Id 
    private String id;
    private int initialStocks;
    private int remaining;
    private BigDecimal unitPrice;
    private LocalDateTime purchasedAt;
    private LocalDateTime settlementDate;
    private boolean reserved;

    protected LotJpaEntity() {}

    public LotJpaEntity(String id, int initialStocks, int quantity, BigDecimal unitPrice,
                        LocalDateTime purchasedAt, LocalDateTime settlementDate, boolean reserved) {
        this.id = id;
        this.initialStocks = initialStocks;
        this.remaining = quantity;
        this.unitPrice = unitPrice;
        this.purchasedAt = purchasedAt;
        this.settlementDate = settlementDate;
        this.reserved = reserved;
    }

    // Getters
    public String getId() {
        return id;
    }

    public int getInitialStocks() {
        return initialStocks;
    }

    public int getRemaining() {
        return remaining;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public boolean isReserved() {
        return reserved;
    }

}
