package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.LotJpaEntity;
import cat.gencat.agaur.hexastock.model.Lot;

public class LotMapper {

    public static Lot toModelEntity(LotJpaEntity jpaEntity) {
        return new Lot(jpaEntity.getId(), jpaEntity.getInitialStocks(), jpaEntity.getRemaining(), jpaEntity.getUnitPrice(), jpaEntity.getPurchasedAt(), false);
    }

    public static LotJpaEntity toJpaEntity(Lot entity) {

        return new LotJpaEntity(entity.getId(), entity.getInitialStocks(), entity.getRemaining(), entity.getUnitPrice(), entity.getPurchasedAt());

    }


}