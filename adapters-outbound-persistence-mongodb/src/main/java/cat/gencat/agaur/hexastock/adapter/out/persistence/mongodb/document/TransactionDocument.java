package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import cat.gencat.agaur.hexastock.model.transaction.TransactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "idx_tx_portfolio_createdAt", def = "{'portfolioId': 1, 'createdAt': 1}")
})
public class TransactionDocument {

    @Id
    private String id;

    @Indexed
    private String portfolioId;

    private TransactionType type;
    private String ticker;
    private int quantity;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitPrice;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal profit;

    private LocalDateTime createdAt;

    protected TransactionDocument() {
    }

    public TransactionDocument(String id, String portfolioId, TransactionType type, String ticker,
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
