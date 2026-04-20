package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Embedded lot sub-document inside {@link HoldingDocument}.
 *
 * <p>Holds the raw scalar state of a single purchase lot. Adapter-local; never
 * leaks into domain or application layers.</p>
 */
public class LotDocument {

    private String id;
    private int initialStocks;
    private int remaining;
    private BigDecimal unitPrice;
    private LocalDateTime purchasedAt;

    protected LotDocument() { /* for MongoDB mapper */ }

    public LotDocument(String id, int initialStocks, int remaining,
                       BigDecimal unitPrice, LocalDateTime purchasedAt) {
        this.id = id;
        this.initialStocks = initialStocks;
        this.remaining = remaining;
        this.unitPrice = unitPrice;
        this.purchasedAt = purchasedAt;
    }

    public String getId() { return id; }
    public int getInitialStocks() { return initialStocks; }
    public int getRemaining() { return remaining; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
}
