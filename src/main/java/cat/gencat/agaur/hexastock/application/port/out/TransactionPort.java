package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.PortfolioId;
import cat.gencat.agaur.hexastock.model.Transaction;

import java.util.List;

/**
 * TransactionPort defines the secondary port for transaction persistence operations.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary port</strong> (output port)
 * that defines how the application core communicates with the persistence layer for
 * financial transaction records. It is implemented by driven adapters that connect to 
 * actual database systems.</p>
 * 
 * <p>This generic interface allows the application to:</p>
 * <ul>
 *   <li>Abstract away the details of how transactions are stored and retrieved</li>
 *   <li>Support multiple storage technologies without changing the application core</li>
 *   <li>Facilitate testing by allowing mock implementations</li>
 * </ul>
 * 
 * <p>The port handles the core transaction persistence operations needed by the application:</p>
 * <ul>
 *   <li>Retrieving transaction history for a specific portfolio</li>
 *   <li>Saving new transaction records when financial activities occur</li>
 * </ul>
 * 
 * <p>The generic type parameter &lt;T&gt; allows flexibility in how transactions are represented
 * when retrieved from persistence, while ensuring type safety.</p>
 */
public interface TransactionPort {

    /**
     * Retrieves all transactions for a specific portfolio.
     * 
     * <p>This method provides the transaction history for a portfolio, which can include
     * deposits, withdrawals, stock purchases, and stock sales.</p>
     * 
     * @param portfolioId The unique identifier of the portfolio to get transactions for
     * @return A list of transaction records of type T
     */
    List<Transaction> getTransactionsByPortfolioId(PortfolioId portfolioId);
    
    /**
     * Saves a new transaction record to the persistence store.
     * 
     * <p>This method is called whenever a financial activity occurs that should be
     * recorded in the transaction history, such as deposits, withdrawals, stock
     * purchases, or stock sales.</p>
     * 
     * @param transaction The Transaction domain object to persist
     */
    void save(Transaction transaction);
}