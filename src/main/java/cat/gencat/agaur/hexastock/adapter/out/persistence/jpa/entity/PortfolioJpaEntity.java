package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {
    @Id
    private String id;

    private String ownerName;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "portfolio_id")
    private Set<HoldingJpaEntity> holdings = new HashSet<>();

    protected PortfolioJpaEntity() {}

    public PortfolioJpaEntity(String id, String ownerName, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public Set<HoldingJpaEntity> getHoldings() {
        return holdings;
    }
    
    public boolean isEmpty() {
        return holdings.isEmpty();
    }

    public void setHoldings(Set<HoldingJpaEntity> holdings) {
        this.holdings = holdings;
    }
}
