package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.PortfolioManagementUseCase;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.PortfolioId;
import cat.gencat.agaur.hexastock.model.Transaction;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * PortfolioManagmentService implements the core use cases for portfolio creation and cash management.
 * 
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link PortfolioManagementUseCase}) to be used by driving adapters</li>
 *   <li>Uses secondary ports ({@link PortfolioPort} and {@link TransactionPort}) to communicate with driven adapters</li>
 * </ul>
 * </p>
 * 
 * <p>This service orchestrates the portfolio management operations by:
 * <ul>
 *   <li>Creating new portfolios</li>
 *   <li>Retrieving existing portfolios</li>
 *   <li>Handling deposits and withdrawals of funds</li>
 *   <li>Recording transactions for auditing and tracking purposes</li>
 * </ul>
 * </p>
 * 
 * <p>The service ensures that all operations are transactional, maintaining data consistency
 * between the portfolio state and transaction records.</p>
 */
@Transactional
public class PortfolioManagementService implements PortfolioManagementUseCase {

    /**
     * The secondary port used to persist and retrieve portfolios.
     */
    private final PortfolioPort portfolioPort;
    
    /**
     * The secondary port used to record financial transactions.
     */
    private final TransactionPort transactionPort;

    /**
     * Constructs a new PortfolioManagmentService with the required secondary ports.
     * 
     * @param portfolioPort The port for portfolio persistence operations
     * @param transactionPort The port for transaction recording operations
     */
    public PortfolioManagementService(PortfolioPort portfolioPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.transactionPort = transactionPort;
    }

    /**
     * Creates a new portfolio with the specified owner name.
     * 
     * <p>This method:
     * <ol>
     *   <li>Creates a new Portfolio domain object with a generated ID</li>
     *   <li>Persists the portfolio through the portfolio port</li>
     *   <li>Returns the newly created portfolio</li>
     * </ol>
     * </p>
     * 
     * @param ownerName The name of the portfolio owner
     * @return The newly created Portfolio
     */
    @Override
    public Portfolio createPortfolio(String ownerName) {
        Portfolio portfolio = Portfolio.create(ownerName);
        portfolioPort.createPortfolio(portfolio);
        return portfolio;
    }

    /**
     * Retrieves a portfolio by its unique identifier.
     * 
     * @param portfolioId The unique identifier of the portfolio to retrieve
     * @return The requested Portfolio
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    @Override
    public Portfolio getPortfolio(PortfolioId portfolioId) {
        return portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
    }

    /**
     * Deposits money into a portfolio's cash balance.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Adds the deposit amount to the portfolio's balance</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a deposit transaction record</li>
     * </ol>
     * </p>
     * 
     * @param portfolioId The ID of the portfolio to deposit into
     * @param amount The amount of money to deposit
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if the deposit amount is not positive
     */
    @Override
    public void deposit(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.deposit(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createDeposit(portfolioId, amount);
        transactionPort.save(transaction);
    }

    /**
     * Withdraws money from a portfolio's cash balance.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Subtracts the withdrawal amount from the portfolio's balance</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a withdrawal transaction record</li>
     * </ol>
     * </p>
     * 
     * @param portfolioId The ID of the portfolio to withdraw from
     * @param amount The amount of money to withdraw
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidAmountException if the withdrawal amount is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    @Override
    public void withdraw(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.withdraw(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createWithdrawal(portfolioId, amount);
        transactionPort.save(transaction);
    }

    /**
     * Retrieves all portfolios.
     * 
     * @return A list of all portfolios
     */
    @Override
    public List<Portfolio> getAllPortfolios() {
        return portfolioPort.getAllPortfolios();
    }
}
