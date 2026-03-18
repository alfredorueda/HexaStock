package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.model.*;

public class TransactionMapper {
    public static Transaction toModelEntity(TransactionJpaEntity jpaEntity) {
        return Transaction.builder()
                .id(TransactionId.of(jpaEntity.getId()))
                .portfolioId(PortfolioId.of(jpaEntity.getPortfolioId()))
                .type(jpaEntity.getType())
                .ticker(jpaEntity.getTicker() != null ? Ticker.of(jpaEntity.getTicker()) : null)
                .quantity(ShareQuantity.of(jpaEntity.getQuantity()))
                .unitPrice(jpaEntity.getUnitPrice() != null ? Price.of(jpaEntity.getUnitPrice()) : null)
                .totalAmount(Money.of(jpaEntity.getTotalAmount()))
                .profit(Money.of(jpaEntity.getProfit()))
                .fee(jpaEntity.getFee() != null ? Money.of(jpaEntity.getFee()) : Money.ZERO)
                .createdAt(jpaEntity.getCreatedAt())
                .build();
    }

    public static TransactionJpaEntity toJpaEntity(Transaction entity) {
        return TransactionJpaEntity.builder()
                .id(entity.getId().value())
                .portfolioId(entity.getPortfolioId().value())
                .type(entity.getType())
                .ticker(entity.getTicker() != null ? entity.getTicker().value() : null)
                .quantity(entity.getQuantity().value())
                .unitPrice(entity.getUnitPrice() != null ? entity.getUnitPrice().value() : null)
                .totalAmount(entity.getTotalAmount().amount())
                .profit(entity.getProfit().amount())
                .fee(entity.getFee() != null ? entity.getFee().amount() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}