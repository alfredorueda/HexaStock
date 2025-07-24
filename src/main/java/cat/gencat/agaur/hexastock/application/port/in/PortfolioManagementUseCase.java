package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;

/**
 * PortfolioManagmentUseCase defines the primary port for portfolio creation and cash management.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary port</strong> (input port)
 * that defines how the outside world can interact with the application core for portfolio
 * management operations. It encapsulates the following use cases:</p>
 * <ul>
 *   <li>Creating new investment portfolios</li>
 *   <li>Retrieving existing portfolios</li>
 *   <li>Depositing funds into portfolios</li>
 *   <li>Withdrawing funds from portfolios</li>
 * </ul>
 * 
 * <p>This interface is implemented by application services in the domain layer and
 * used by driving adapters (like REST controllers) in the infrastructure layer.</p>
 */
public interface PortfolioManagementUseCase {

    /**
     * Creates a new portfolio for the specified owner.
     * 
     * @param ownerName The name of the portfolio owner
     * @return The newly created Portfolio domain object
     */
    Portfolio createPortfolio(String ownerName);
    
    /**
     * Retrieves a portfolio by its unique identifier.
     * 
     * @param id The unique identifier of the portfolio to retrieve
     * @return The requested Portfolio domain object
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    Portfolio getPortfolio(String id);
    
    /**
     * Deposits money into a portfolio's cash balance.
     * 
     * @param idPortfolio The ID of the portfolio to deposit into
     * @param amount The amount of money to deposit
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if the deposit amount is not positive
     */
    void deposit(String idPortfolio, Money amount);
    
    /**
     * Withdraws money from a portfolio's cash balance.
     * 
     * @param idPortfolio The ID of the portfolio to withdraw from
     * @param amount The amount of money to withdraw
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidAmountException if the withdrawal amount is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    void withdraw(String idPortfolio, Money amount);

}
