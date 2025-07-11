package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.HoldingJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.model.Portfolio;

import java.util.stream.Collectors;

public class PortfolioMapper {

    public static Portfolio toModelEntity(PortfolioJpaEntity jpaEntity) {

        Portfolio portfolio = new Portfolio(jpaEntity.getId(), jpaEntity.getOwnerName(), jpaEntity.getBalance(), jpaEntity.getCreatedAt());

        for(var holdingJpaEntity: jpaEntity.getHoldings())
            portfolio.addHolding(HoldingMapper.toModelEntity(holdingJpaEntity));

        return portfolio;
    }

    public static PortfolioJpaEntity toJpaEntity(Portfolio entity) {

        PortfolioJpaEntity portfolioJpaEntity =  new PortfolioJpaEntity(entity.getId(), entity.getOwnerName(), entity.getBalance(), entity.getCreatedAt());

        portfolioJpaEntity.setHoldings(entity.getHoldings().stream().map(HoldingMapper::toJpaEntity).collect(Collectors.toSet()));

        return portfolioJpaEntity;

    }


}