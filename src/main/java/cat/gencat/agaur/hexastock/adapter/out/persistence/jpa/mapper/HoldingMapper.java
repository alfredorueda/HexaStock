package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.HoldingJpaEntity;
import cat.gencat.agaur.hexastock.model.Holding;
import cat.gencat.agaur.hexastock.model.Ticker;

import java.util.stream.Collectors;

public class HoldingMapper {

    public static Holding toModelEntity(HoldingJpaEntity jpaEntity) {
        Holding holding = new Holding(jpaEntity.getId(), Ticker.of(jpaEntity.getTicker()));

        for(var lotJpaEntity: jpaEntity.getLots())
            holding.addLot(LotMapper.toModelEntity(lotJpaEntity));

        return holding;
    }

    public static HoldingJpaEntity toJpaEntity(Holding entity) {

        HoldingJpaEntity holdingJpaEntity =  new HoldingJpaEntity(entity.getId(), entity.getTicker().value());

        holdingJpaEntity.setLots(entity.getLots().stream().map(LotMapper::toJpaEntity).collect(Collectors.toList()));

        return holdingJpaEntity;

    }




}