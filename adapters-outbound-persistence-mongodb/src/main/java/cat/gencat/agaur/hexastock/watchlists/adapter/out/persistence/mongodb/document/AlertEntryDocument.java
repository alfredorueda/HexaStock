package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.document;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

public class AlertEntryDocument {

    private String ticker;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal thresholdPrice;

    protected AlertEntryDocument() {}

    public AlertEntryDocument(String ticker, BigDecimal thresholdPrice) {
        this.ticker = ticker;
        this.thresholdPrice = thresholdPrice;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }
}

