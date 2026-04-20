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
 * in a separate {@code portfolio_transaction} collection. No optimistic-locking
 * version is needed: transactions are never updated.</p>
 *
 * <p>Modelled as a Java 21 {@code record} — the immutability matches the domain
 * semantics, and using a record collapses the field/constructor/getter/builder
 * boilerplate that would otherwise mirror the JPA {@code TransactionJpaEntity}
 * (flagged by Sonar as duplicated code).</p>
 */
@Document(collection = "portfolio_transaction")
public record TransactionDocument(
        @Id String id,
        @Indexed String portfolioId,
        TransactionType type,
        String ticker,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BigDecimal profit,
        LocalDateTime createdAt) {
}
