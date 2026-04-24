package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.HoldingJpaEntity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Holding;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.HoldingId;
import cat.gencat.agaur.hexastock.model.market.Ticker;

import java.util.ArrayList;
import java.util.stream.Collectors;

public final class HoldingMapper {

    private HoldingMapper() {
        // Utility class - prevent instantiation
    }

    public static Holding toModelEntity(HoldingJpaEntity jpaEntity) {
        Holding holding = new Holding(HoldingId.of(jpaEntity.getId()), Ticker.of(jpaEntity.getTicker()));
        for (var lotJpaEntity : jpaEntity.getLots()) {
            holding.addLotFromPersistence(LotMapper.toModelEntity(lotJpaEntity));
        }
        return holding;
    }

    public static HoldingJpaEntity toJpaEntity(Holding entity) {
        HoldingJpaEntity holdingJpaEntity = new HoldingJpaEntity(entity.getId().value(), entity.getTicker().value());
        // Mutable list because Hibernate may augment/replace it when managing the persistent collection.
        holdingJpaEntity.setLots(entity.getLots().stream().map(LotMapper::toJpaEntity).collect(Collectors.toCollection(ArrayList::new)));
        return holdingJpaEntity;
    }
}