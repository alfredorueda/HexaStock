package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.model.*;

public class TransactionMapper {
    public static Transaction toModelEntity(TransactionJpaEntity jpaEntity) {
        return new Transaction(
                TransactionId.of(jpaEntity.getId()),
                PortfolioId.of(jpaEntity.getPortfolioId()),
                jpaEntity.getType(),
                jpaEntity.getTicker() != null ? Ticker.of(jpaEntity.getTicker()) : null,
                ShareQuantity.of(jpaEntity.getQuantity()),
                jpaEntity.getUnitPrice() != null ? Price.of(jpaEntity.getUnitPrice()) : null,
                Money.of(jpaEntity.getTotalAmount()),
                Money.of(jpaEntity.getProfit()),
                jpaEntity.getCreatedAt()
        );
    }

    public static TransactionJpaEntity toJpaEntity(Transaction entity) {
        return new TransactionJpaEntity(
                entity.getId().value(),
                entity.getPortfolioId().value(),
                entity.getType(),
                entity.getTicker() != null ? entity.getTicker().value() : null,
                entity.getQuantity().value(),
                entity.getUnitPrice() != null ? entity.getUnitPrice().value() : null,
                entity.getTotalAmount().amount(),
                entity.getProfit().amount(),
                entity.getCreatedAt()
        );
    }
}