package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JpaTransactionSpringDataRepository provides Spring Data JPA operations for Transaction entities.
 * 
 * <p>In hexagonal architecture terms, this interface is part of the <strong>secondary adapter</strong> layer
 * (persistence infrastructure) that provides database access for transaction record management.</p>
 * 
 * <p>This repository:</p>
 * <ul>
 *   <li>Extends Spring Data's JpaRepository to get standard CRUD operations</li>
 *   <li>Provides custom finder methods for retrieving transaction history</li>
 *   <li>Serves as the database access layer for the JpaTransactionRepository adapter</li>
 * </ul>
 * 
 * <p>Transaction records are critical for auditing, reporting, and maintaining a complete
 * history of all financial operations within the system. This repository provides the
 * infrastructure to persist and retrieve these records efficiently.</p>
 */
@Repository
public interface JpaTransactionSpringDataRepository extends JpaRepository <TransactionJpaEntity, String> {

    /**
     * Retrieves all transaction records for a specific portfolio.
     * 
     * <p>This method allows the application to fetch the complete transaction history
     * for a given portfolio, which can be used for:</p>
     * <ul>
     *   <li>Generating account statements and financial reports</li>
     *   <li>Auditing portfolio activities over time</li>
     *   <li>Analyzing investment performance</li>
     * </ul>
     * 
     * @param portFolioId The unique identifier of the portfolio to get transactions for
     * @return A list of transaction records associated with the specified portfolio
     */
    List<TransactionJpaEntity> getAllByPortfolioId(String portFolioId);
}
