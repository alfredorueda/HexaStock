package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LotDocument {
    private String id;
    private int initialShares;
    private int remainingShares;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitPrice;

    private LocalDateTime purchasedAt;

    protected LotDocument() {
    }

    public LotDocument(String id, int initialShares, int remainingShares, BigDecimal unitPrice, LocalDateTime purchasedAt) {
        this.id = id;
        this.initialShares = initialShares;
        this.remainingShares = remainingShares;
        this.unitPrice = unitPrice;
        this.purchasedAt = purchasedAt;
    }

    public String getId() {
        return id;
    }

    public int getInitialShares() {
        return initialShares;
    }

    public int getRemainingShares() {
        return remainingShares;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}
