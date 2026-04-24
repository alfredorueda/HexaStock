package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.entity.LotJpaEntity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.*;
import cat.gencat.agaur.hexastock.model.money.*;

public final class LotMapper {

    private LotMapper() {
        // Utility class - prevent instantiation
    }

    public static Lot toModelEntity(LotJpaEntity jpaEntity) {
        return new Lot(
                LotId.of(jpaEntity.getId()),
                ShareQuantity.of(jpaEntity.getInitialStocks()),
                ShareQuantity.of(jpaEntity.getRemaining()),
                Price.of(jpaEntity.getUnitPrice()),
                jpaEntity.getPurchasedAt()
        );
    }

    public static LotJpaEntity toJpaEntity(Lot entity) {
        return new LotJpaEntity(
                entity.getId().value(),
                entity.getInitialShares().value(),
                entity.getRemainingShares().value(),
                entity.getUnitPrice().value(),
                entity.getPurchasedAt()
        );
    }
}