package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JpaPortfolioSpringDataRepository provides Spring Data JPA operations for Portfolio entities.
 * 
 * <p>In hexagonal architecture terms, this interface is part of the <strong>secondary adapter</strong> layer
 * (persistence infrastructure) that provides database access for portfolio management operations.</p>
 * 
 * <p>This repository:</p>
 * <ul>
 *   <li>Extends Spring Data's JpaRepository to get standard CRUD operations</li>
 *   <li>Provides optimistic locking to handle concurrent portfolio modifications</li>
 *   <li>Serves as the database access layer for the JpaPortfolioRepository adapter</li>
 * </ul>
 * 
 * <p>The repository handles the low-level database operations, while the JpaPortfolioRepository
 * handles the mapping between domain objects and JPA entities, maintaining a clean separation
 * of concerns aligned with hexagonal architecture principles.</p>
 */
@Repository
public interface JpaPortfolioSpringDataRepository extends JpaRepository <PortfolioJpaEntity, String> {

    /**
     * Finds a portfolio by ID with a pessimistic write lock.
     * 
     * <p>This method acquires a database-level lock on the portfolio record to prevent
     * concurrent modifications, which is essential for maintaining data integrity
     * during financial operations like stock trades or cash management.</p>
     * 
     * <p>The pessimistic lock ensures that when multiple users try to modify the same
     * portfolio simultaneously (e.g., two users trying to buy stocks from the same
     * portfolio), one transaction will wait for the other to complete, preventing
     * race conditions that could lead to incorrect balances or holdings.</p>
     * 
     * @param id The unique identifier of the portfolio to retrieve
     * @return An Optional containing the portfolio if found, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
    Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);
}
