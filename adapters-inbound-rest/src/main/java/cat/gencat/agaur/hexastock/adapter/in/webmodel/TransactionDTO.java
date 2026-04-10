package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.transaction.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionDTO is a flat Data Transfer Object for transaction information in the REST API.
 *
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer.
 * It maps the domain {@link Transaction} sealed hierarchy to a flat structure of
 * primitive/simple fields, preventing the domain model from leaking into the API contract.</p>
 */
public record TransactionDTO(
        String id,
        String portfolioId,
        String type,
        String ticker,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BigDecimal profit,
        LocalDateTime createdAt
) {
    /**
     * Maps a domain Transaction to a flat REST DTO.
     */
    public static TransactionDTO from(Transaction tx) {
        return new TransactionDTO(
                tx.id().value(),
                tx.portfolioId().value(),
                tx.type().name(),
                tx.ticker() != null ? tx.ticker().value() : null,
                tx.quantity().value(),
                tx.unitPrice() != null ? tx.unitPrice().value() : null,
                tx.totalAmount().amount(),
                tx.profit().amount(),
                tx.createdAt()
        );
    }
}