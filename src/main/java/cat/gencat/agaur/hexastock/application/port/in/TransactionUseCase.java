package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.TransactionDTO;

import java.util.List;
import java.util.Optional;

/**
 * TransactionUseCase defines the primary port for transaction history retrieval.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary port</strong> (input port)
 * that defines how the outside world can interact with the application core for accessing
 * the financial transaction history. It encapsulates the use case of retrieving transaction
 * records for a portfolio, with optional filtering.</p>
 * 
 * <p>This interface is implemented by application services in the domain layer and
 * used by driving adapters (like REST controllers) in the infrastructure layer.</p>
 * 
 * <p>Transaction history provides valuable information for:</p>
 * <ul>
 *   <li>Reviewing past financial activities</li>
 *   <li>Auditing portfolio changes</li>
 *   <li>Analyzing investment performance</li>
 *   <li>Supporting tax reporting requirements</li>
 * </ul>
 */
public interface TransactionUseCase {

    /**
     * Retrieves transactions for a specified portfolio, optionally filtered by transaction type.
     * 
     * <p>This method returns the transaction history for a portfolio, which can include
     * deposits, withdrawals, stock purchases, and stock sales. The results can be
     * optionally filtered by transaction type.</p>
     * 
     * @param portfolioId The ID of the portfolio to retrieve transactions for
     * @param type Optional transaction type to filter by (e.g., "DEPOSIT", "WITHDRAWAL", "PURCHASE", "SALE")
     * @return A list of TransactionDTO objects representing the transaction history
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    List<TransactionDTO> getTransactions(String portfolioId, Optional<String> type);
}
