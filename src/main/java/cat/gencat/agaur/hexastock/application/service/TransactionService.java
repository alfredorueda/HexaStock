package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.TransactionDTO;
import cat.gencat.agaur.hexastock.application.port.in.TransactionUseCase;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.PortfolioId;
import cat.gencat.agaur.hexastock.model.Transaction;
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
 *   <li>Tax reporting and compliance</li>
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
     * Retrieves transactions for a specified portfolio, optionally filtered by transaction type.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves transactions for the specified portfolio ID</li>
     *   <li>Optionally filters them by transaction type if provided</li>
     *   <li>Converts domain Transaction objects to DTOs for the presentation layer</li>
     * </ol>
     * </p>
     * 
     * @param portfolioId The ID of the portfolio to retrieve transactions for
     * @param type Optional transaction type to filter by (e.g., DEPOSIT, WITHDRAWAL, PURCHASE, SALE)
     * @return A list of TransactionDTO objects representing the transaction history
     */
    @Override
    public List<TransactionDTO> getTransactions(String portfolioId, Optional<String> type) {
        List<Transaction> transactions = transactionPort.getTransactionsByPortfolioId(PortfolioId.of(portfolioId));
        return transactions.stream().map(TransactionDTO::new).toList();
    }
}
