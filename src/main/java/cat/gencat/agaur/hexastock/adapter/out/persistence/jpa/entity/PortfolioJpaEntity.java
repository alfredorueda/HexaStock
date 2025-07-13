package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

/**
 * PortfolioJpaEntity is a JPA entity representing the Portfolio domain model in the database.
 * 
 * <p>In hexagonal architecture terms, this class is part of the <strong>secondary adapter</strong> layer
 * (persistence infrastructure) that provides a database representation of the Portfolio aggregate root
 * from the domain model.</p>
 * 
 * <p>This entity:</p>
 * <ul>
 *   <li>Maps the Portfolio domain object to a relational database structure</li>
 *   <li>Maintains the parent-child relationship with Holdings through JPA associations</li>
 *   <li>Provides a clean separation between the domain model and persistence concerns</li>
 * </ul>
 * 
 * <p>The entity corresponds to the "portfolio" table in the database and maintains
 * a one-to-many relationship with {@link HoldingJpaEntity}, representing the
 * collection of stock holdings in a portfolio.</p>
 */
@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {
    /**
     * The unique identifier of the portfolio.
     */
    @Id
    private String id;

    /**
     * The name of the portfolio owner.
     */
    private String ownerName;
    
    /**
     * The current cash balance of the portfolio.
     */
    private BigDecimal balance;
    
    /**
     * The timestamp when the portfolio was created.
     */
    private LocalDateTime createdAt;

    /**
     * The collection of stock holdings in this portfolio.
     * 
     * <p>This establishes a one-to-many relationship with HoldingJpaEntity, where
     * portfolio_id is the foreign key in the holding table. The cascade type ALL
     * ensures that operations on the portfolio (like save or delete) are propagated
     * to its holdings.</p>
     */
    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "portfolio_id")
    private Set<HoldingJpaEntity> holdings = new HashSet<>();

    /**
     * Protected default constructor required by JPA.
     */
    protected PortfolioJpaEntity() {}

    /**
     * Constructs a new PortfolioJpaEntity with the specified properties.
     * 
     * @param id The unique identifier of the portfolio
     * @param ownerName The name of the portfolio owner
     * @param balance The initial cash balance
     * @param createdAt The timestamp when the portfolio was created
     */
    public PortfolioJpaEntity(String id, String ownerName, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    // Getters
    /**
     * Returns the unique identifier of the portfolio.
     * 
     * @return The portfolio ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns the name of the portfolio owner.
     * 
     * @return The owner name
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Returns the current cash balance of the portfolio.
     * 
     * @return The cash balance
     */
    public BigDecimal getBalance() {
        return balance;
    }
    
    /**
     * Returns the timestamp when the portfolio was created.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Returns the collection of stock holdings in this portfolio.
     * 
     * @return The set of HoldingJpaEntity objects
     */
    public Set<HoldingJpaEntity> getHoldings() {
        return holdings;
    }
    
    /**
     * Checks if the portfolio has any holdings.
     * 
     * @return true if the portfolio has no holdings, false otherwise
     */
    public boolean isEmpty() {
        return holdings.isEmpty();
    }

    /**
     * Sets the collection of holdings for this portfolio.
     * 
     * @param holdings The set of HoldingJpaEntity objects to set
     */
    public void setHoldings(Set<HoldingJpaEntity> holdings) {
        this.holdings = holdings;
    }
}
