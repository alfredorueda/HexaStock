package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document;

import cat.gencat.agaur.hexastock.portfolios.model.transaction.TransactionType;
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
public record TransactionDocument(
        @Id String id,
        @Indexed String portfolioId,
        TransactionType type,
        String ticker,
        int quantity,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPrice,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal totalAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal profit,
        LocalDateTime createdAt
) {
}
