package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import cat.gencat.agaur.hexastock.model.transaction.TransactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB document for the immutable {@link cat.gencat.agaur.hexastock.model.transaction.Transaction}
 * ledger entries.
 *
 * <p>Transactions are append-only and not part of the Portfolio aggregate (see
 * {@code Transaction} Javadoc), so each transaction is stored as its own document
 * in a separate {@code portfolio_transaction} collection. No {@code @Version} field
 * is needed: transactions are never updated.</p>
 */
@Document(collection = "portfolio_transaction")
public class TransactionDocument {

    @Id
    private String id;

    @Indexed
    private String portfolioId;

    private TransactionType type;
    private String ticker;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal profit;
    private LocalDateTime createdAt;

    protected TransactionDocument() { /* for MongoDB mapper */ }

    private TransactionDocument(Builder b) {
        this.id = b.id;
        this.portfolioId = b.portfolioId;
        this.type = b.type;
        this.ticker = b.ticker;
        this.quantity = b.quantity;
        this.unitPrice = b.unitPrice;
        this.totalAmount = b.totalAmount;
        this.profit = b.profit;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public TransactionType getType() { return type; }
    public String getTicker() { return ticker; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getProfit() { return profit; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class Builder {
        private String id;
        private String portfolioId;
        private TransactionType type;
        private String ticker;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private BigDecimal profit;
        private LocalDateTime createdAt;

        public Builder id(String v)           { this.id = v; return this; }
        public Builder portfolioId(String v)  { this.portfolioId = v; return this; }
        public Builder type(TransactionType v){ this.type = v; return this; }
        public Builder ticker(String v)       { this.ticker = v; return this; }
        public Builder quantity(int v)        { this.quantity = v; return this; }
        public Builder unitPrice(BigDecimal v){ this.unitPrice = v; return this; }
        public Builder totalAmount(BigDecimal v){ this.totalAmount = v; return this; }
        public Builder profit(BigDecimal v)   { this.profit = v; return this; }
        public Builder createdAt(LocalDateTime v){ this.createdAt = v; return this; }
        public TransactionDocument build()    { return new TransactionDocument(this); }
    }
}
