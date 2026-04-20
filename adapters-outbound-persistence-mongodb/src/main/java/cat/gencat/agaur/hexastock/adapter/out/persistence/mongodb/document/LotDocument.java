package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Embedded lot sub-document inside {@link HoldingDocument}.
 *
 * <p>Holds the raw scalar state of a single purchase lot. Adapter-local; never
 * leaks into domain or application layers.</p>
 *
 * <p>Modelled as a Java 21 {@code record} so that Spring Data MongoDB's mapping
 * layer reconstructs instances via the canonical constructor and component
 * accessors. Using a record also collapses the field/constructor/getter
 * boilerplate that would otherwise mirror the JPA {@code LotJpaEntity}
 * (flagged by Sonar as duplicated code).</p>
 */
public record LotDocument(
        String id,
        int initialStocks,
        int remaining,
        BigDecimal unitPrice,
        LocalDateTime purchasedAt) {
}
