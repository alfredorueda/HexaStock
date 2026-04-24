package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "portfolios")
public class PortfolioDocument {

    @Id
    private String id;
    private String ownerName;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private List<HoldingDocument> holdings = new ArrayList<>();

    protected PortfolioDocument() {
    }

    public PortfolioDocument(String id, String ownerName, BigDecimal balance, LocalDateTime createdAt,
                             List<HoldingDocument> holdings) {
        this(id, ownerName, balance, createdAt, holdings, null);
    }

    public PortfolioDocument(String id, String ownerName, BigDecimal balance, LocalDateTime createdAt,
                             List<HoldingDocument> holdings, Long version) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
        this.holdings = holdings;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<HoldingDocument> getHoldings() {
        return holdings;
    }
}
