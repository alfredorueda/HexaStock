package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.TransactionMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository.JpaTransactionSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JpaTransactionRepository implements the transaction persistence port using JPA.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary adapter</strong> (driven adapter)
 * that implements a secondary port ({@link TransactionPort}) to connect the application
 * core with a relational database for transaction record persistence.</p>
 * 
 * <p>This adapter:</p>
 * <ul>
 *   <li>Uses Spring Data JPA for database operations</li>
 *   <li>Converts between domain Transaction objects and JPA entities</li>
 *   <li>Provides methods to save new transactions and retrieve transaction history</li>
 * </ul>
 * 
 * <p>The adapter is only active when the "jpa" Spring profile is enabled,
 * allowing the application to use different persistence mechanisms (like in-memory
 * repositories for testing) based on the runtime environment.</p>
 * 
 * <p>Transaction records are a critical part of the system, providing an audit trail
 * of all financial activities and supporting reporting and analysis use cases.</p>
 */
@Component
@Profile("jpa")
public class JpaTransactionRepository implements TransactionPort {

    /**
     * The Spring Data JPA repository that handles low-level database operations.
     */
    private final JpaTransactionSpringDataRepository jpaSpringDataRepository;

    /**
     * Constructs a new JpaTransactionRepository with the required Spring Data repository.
     * 
     * @param jpaSpringDataRepository The Spring Data JPA repository for transaction entities
     */
    public JpaTransactionRepository(JpaTransactionSpringDataRepository jpaSpringDataRepository) {
        this.jpaSpringDataRepository = jpaSpringDataRepository;
    }

    /**
     * Retrieves all transactions for a specific portfolio.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Queries the database for all transactions associated with the specified portfolio ID</li>
     *   <li>Converts each JPA entity to a domain Transaction object</li>
     *   <li>Returns the list of domain Transaction objects</li>
     * </ol>
     * 
     * @param portFolioId The ID of the portfolio to get transactions for
     * @return A list of Transaction domain objects
     */
    @Override
    public List<Transaction> getTransactionsByPortfolioId(String portFolioId) {
        List<TransactionJpaEntity> lTransactions = jpaSpringDataRepository.getAllByPortfolioId(portFolioId);
        return lTransactions.stream().map(TransactionMapper::toModelEntity).toList();
    }

    /**
     * Saves a new transaction record to the database.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Converts the domain Transaction to a JPA entity</li>
     *   <li>Persists the entity to the database</li>
     * </ol>
     * 
     * <p>This method is called whenever a financial activity occurs that should be
     * recorded in the transaction history, such as deposits, withdrawals, stock
     * purchases, or stock sales.</p>
     * 
     * @param transaction The Transaction domain object to persist
     */
    @Override
    public void save(Transaction transaction) {
        TransactionJpaEntity jpaEntity = TransactionMapper.toJpaEntity(transaction);
        jpaSpringDataRepository.save(jpaEntity);
    }
}