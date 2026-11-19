package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.TransactionUseCase;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.transaction.Transaction;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TransactionService implements the use case for retrieving transaction history.
 * 
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link TransactionUseCase}) to be used by driving adapters</li>
 *   <li>Uses a secondary port ({@link TransactionPort}) to communicate with driven adapters for data access</li>
 * </ul>
 * </p>
 * 
 * <p>This service provides functionality to query and retrieve transaction history for portfolios,
 * allowing users to track their financial activities including:</p>
 * <ul>
 *   <li>Deposits and withdrawals</li>
 *   <li>Stock purchases</li>
 *   <li>Stock sales with profit/loss information</li>
 * </ul>
 * 
 * <p>Transaction history is crucial for:
 * <ul>
 *   <li>Performance analysis and reporting</li>
 *   <li>Audit trails for financial activities</li>
 *   <li>Educational analysis of recorded operations</li>
 * </ul>
 * </p>
 */

@Transactional
public class TransactionService implements TransactionUseCase {

    /**
     * The secondary port used to retrieve transaction records.
     */
    private final TransactionPort transactionPort;

    /**
     * Constructs a new TransactionService with the required secondary port.
     * 
     * @param transactionPort The port for transaction data access operations
     */
    public TransactionService(TransactionPort transactionPort) {
        this.transactionPort = transactionPort;
    }

    /**
     * Retrieves domain transaction records for the supplied portfolio identifier.
     * The current implementation ignores the optional type parameter and does not
     * independently verify portfolio existence. The REST adapter maps the records
     * to DTOs; this service returns domain values.
     *
     * @param portfolioId portfolio identifier
     * @param type accepted but currently unused filter value
     * @return transaction records returned by the persistence port
     */
    @Override
    public List<Transaction> getTransactions(String portfolioId, Optional<String> type) {
        return transactionPort.getTransactionsByPortfolioId(PortfolioId.of(portfolioId));
    }
}
