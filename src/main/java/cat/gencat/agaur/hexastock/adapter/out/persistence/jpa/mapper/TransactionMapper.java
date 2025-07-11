package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Transaction;

public class TransactionMapper {

    public static Transaction toModelEntity(TransactionJpaEntity jpaEntity) {

        return new Transaction(jpaEntity.getId(), jpaEntity.getPortfolioId(), jpaEntity.getType(), jpaEntity.getTicker() != null ? Ticker.of(jpaEntity.getTicker()) : null,
                jpaEntity.getQuantity(), jpaEntity.getUnitPrice(), jpaEntity.getTotalAmount(), jpaEntity.getProfit(), jpaEntity.getCreatedAt());
    }

    public static TransactionJpaEntity toJpaEntity(Transaction entity) {

        return new TransactionJpaEntity(entity.getId(), entity.getPortfolioId(), entity.getType(), entity.getTicker() != null ? entity.getTicker().value() : null,
                entity.getQuantity(), entity.getUnitPrice(), entity.getTotalAmount(), entity.getProfit(), entity.getCreatedAt());

    }


}