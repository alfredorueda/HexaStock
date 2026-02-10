package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.PortfolioMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository.JpaPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.PortfolioId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JpaPortfolioRepository implements the portfolio persistence port using JPA.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary adapter</strong> (driven adapter)
 * that implements a secondary port ({@link PortfolioPort}) to connect the application
 * core with a relational database using JPA (Java Persistence API).</p>
 * 
 * <p>This adapter:</p>
 * <ul>
 *   <li>Uses Spring Data JPA for database operations</li>
 *   <li>Converts between domain models and JPA entities</li>
 *   <li>Handles the persistence of Portfolio aggregates and their components (Holdings and Lots)</li>
 * </ul>
 * 
 * <p>The adapter is only active when the "jpa" Spring profile is enabled,
 * allowing the application to use different persistence mechanisms (like in-memory
 * repositories for testing) based on the runtime environment.</p>
 * 
 * <p>This implementation demonstrates the clean separation between domain logic and
 * persistence details that hexagonal architecture provides, allowing the domain model
 * to remain focused on business rules without infrastructure concerns.</p>
 */
@Component
@Profile("jpa")
public class JpaPortfolioRepository implements PortfolioPort {

    /**
     * The Spring Data JPA repository that handles low-level database operations.
     */
    private final JpaPortfolioSpringDataRepository jpaSpringDataRepository;

    /**
     * Constructs a new JpaPortfolioRepository with the required Spring Data repository.
     * 
     * @param jpaSpringDataRepository The Spring Data JPA repository for portfolio entities
     */
    public JpaPortfolioRepository(JpaPortfolioSpringDataRepository jpaSpringDataRepository) {
        this.jpaSpringDataRepository = jpaSpringDataRepository;
    }

    /**
     * Retrieves a portfolio by its unique identifier.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Queries the database for the portfolio with the specified ID using a pessimistic lock</li>
     *   <li>Converts the JPA entity to a domain model if found</li>
     *   <li>Throws an exception if no portfolio with the ID exists</li>
     * </ol>
     * 
     * @param id The unique identifier of the portfolio to retrieve
     * @return The Portfolio domain object
     * @throws PortfolioNotFoundException if no portfolio with the ID exists
     */
    /**
     * Retrieves a portfolio by its unique identifier.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Queries the database for the portfolio with the specified ID using a pessimistic lock</li>
     *   <li>Converts the JPA entity to a domain model if found</li>
     *   <li>Throws an exception if no portfolio with the ID exists</li>
     * </ol>
     *
     * @param portfolioId The unique identifier of the portfolio to retrieve
     * @return The Portfolio domain object
     * @throws PortfolioNotFoundException if no portfolio with the ID exists
     */
    @Override
    public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
        return jpaSpringDataRepository.findByIdForUpdate(portfolioId.value())
                .map(PortfolioMapper::toModelEntity);
    }

    /**
     * Retrieves all portfolios from the database.
     *
     * @return List of Portfolio domain objects
     */
    public List<Portfolio> getAllPortfolios() {
        return jpaSpringDataRepository.findAll().stream()
            .map(PortfolioMapper::toModelEntity)
            .toList();
    }

    /**
     * Persists a new portfolio to the database.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Converts the domain Portfolio to a JPA entity</li>
     *   <li>Saves the entity and its associated objects (Holdings and Lots) to the database</li>
     * </ol>
     * 
     * @param portfolio The Portfolio domain object to persist
     */
    @Override
    public void createPortfolio(Portfolio portfolio) {
        PortfolioJpaEntity jpaEntity = PortfolioMapper.toJpaEntity(portfolio);
        jpaSpringDataRepository.save(jpaEntity);
    }

    /**
     * Updates an existing portfolio in the database.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Converts the domain Portfolio to a JPA entity</li>
     *   <li>Saves the updated entity and its associated objects to the database</li>
     * </ol>
     * 
     * <p>This operation handles complex updates including adding/removing Holdings
     * and Lots as a result of stock operations.</p>
     * 
     * @param portfolio The updated Portfolio domain object
     */
    @Override
    public void savePortfolio(Portfolio portfolio) {
        PortfolioJpaEntity jpaEntity = PortfolioMapper.toJpaEntity(portfolio);
        jpaSpringDataRepository.save(jpaEntity);
    }
}