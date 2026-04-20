package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representation of the Portfolio aggregate root.
 *
 * <p>The whole aggregate — portfolio + holdings + lots — is stored as one document
 * so that a single {@code updateOne} is atomic across the entire consistency boundary
 * defined by DDD. This removes the need for multi-document MongoDB transactions.</p>
 *
 * <p><strong>Concurrency:</strong> the {@code @Version} field enables Spring Data MongoDB
 * <em>optimistic locking</em>. On {@code save()}, Spring Data generates an update filter
 * that matches on both {@code _id} <em>and</em> the current {@code version}; if another
 * writer bumped the version in between, the update matches zero documents and Spring
 * Data raises {@code OptimisticLockingFailureException}. See
 * {@code doc/architecture/adr/ADR-016-mongodb-adapter-with-optimistic-locking.md}.</p>
 *
 * <p>Adapter-local: {@code version} is never exposed to the domain or application layer.</p>
 */
@Document(collection = "portfolio")
public class PortfolioDocument {

    @Id
    private String id;

    private String ownerName;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private List<HoldingDocument> holdings = new ArrayList<>();

    /**
     * Optimistic-locking version, managed entirely by Spring Data MongoDB.
     * {@code null} on a freshly-constructed (not-yet-persisted) document; Spring Data
     * initialises it on first save and increments it on every subsequent save.
     */
    @Version
    private Long version;

    protected PortfolioDocument() { /* for MongoDB mapper */ }

    public PortfolioDocument(String id, String ownerName, BigDecimal balance,
                             LocalDateTime createdAt,
                             List<HoldingDocument> holdings, Long version) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
        this.holdings = holdings != null ? holdings : new ArrayList<>();
        this.version = version;
    }

    public String getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<HoldingDocument> getHoldings() { return holdings; }
    public Long getVersion() { return version; }
}
