package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.HoldingJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.model.Portfolio;

import java.util.stream.Collectors;

/**
 * PortfolioMapper handles the conversion between Portfolio domain objects and PortfolioJpaEntity objects.
 * 
 * <p>In hexagonal architecture terms, this class is part of the <strong>secondary adapter</strong> layer
 * that provides the translation between the domain model and the persistence model. It serves as
 * an anti-corruption layer that keeps the domain model clean from persistence concerns.</p>
 * 
 * <p>This mapper:</p>
 * <ul>
 *   <li>Converts domain objects to JPA entities for persistence (domain → database)</li>
 *   <li>Converts JPA entities to domain objects for application use (database → domain)</li>
 *   <li>Maintains the object graph by recursively mapping child objects (Holdings and Lots)</li>
 * </ul>
 * 
 * <p>By centralizing the mapping logic in dedicated mapper classes, the application maintains
 * a clean separation between the domain and persistence layers, allowing each to evolve
 * independently while preserving the integrity of the domain model.</p>
 */
public class PortfolioMapper {

    /**
     * Converts a PortfolioJpaEntity to a Portfolio domain object.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a new Portfolio domain object with the core properties</li>
     *   <li>Recursively converts and adds each Holding using the HoldingMapper</li>
     * </ol>
     * 
     * @param jpaEntity The JPA entity to convert
     * @return The corresponding Portfolio domain object
     */
    public static Portfolio toModelEntity(PortfolioJpaEntity jpaEntity) {
        Portfolio portfolio = new Portfolio(jpaEntity.getId(), jpaEntity.getOwnerName(), jpaEntity.getBalance(), jpaEntity.getCreatedAt());

        for(var holdingJpaEntity: jpaEntity.getHoldings())
            portfolio.addHolding(HoldingMapper.toModelEntity(holdingJpaEntity));

        return portfolio;
    }

    /**
     * Converts a Portfolio domain object to a PortfolioJpaEntity.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a new PortfolioJpaEntity with the core properties</li>
     *   <li>Recursively converts each Holding using the HoldingMapper</li>
     *   <li>Sets the converted Holdings collection on the PortfolioJpaEntity</li>
     * </ol>
     * 
     * @param entity The domain entity to convert
     * @return The corresponding JPA entity
     */
    public static PortfolioJpaEntity toJpaEntity(Portfolio entity) {
        PortfolioJpaEntity portfolioJpaEntity = new PortfolioJpaEntity(entity.getId(), entity.getOwnerName(), entity.getBalance(), entity.getCreatedAt());

        portfolioJpaEntity.setHoldings(entity.getHoldings().stream().map(HoldingMapper::toJpaEntity).collect(Collectors.toSet()));

        return portfolioJpaEntity;
    }
}