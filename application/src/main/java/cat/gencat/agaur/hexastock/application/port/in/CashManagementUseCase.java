package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.money.InvalidAmountException;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;

/**
 * Primary port for cash management operations: depositing and withdrawing funds.
 *
 * <p>In hexagonal architecture terms, this is a <strong>driving port</strong> (input port)
 * that defines how the outside world can interact with the application core for
 * cash management within a portfolio.</p>
 *
 * <p>This interface was extracted from the former {@code PortfolioManagementUseCase}
 * to satisfy the <strong>Interface Segregation Principle</strong>: clients that only
 * need cash operations are not forced to depend on portfolio lifecycle methods.</p>
 *
 * @see PortfolioLifecycleUseCase
 */
public interface CashManagementUseCase {

    /**
     * Deposits money into a portfolio's cash balance.
     *
     * @param portfolioId The ID of the portfolio to deposit into
     * @param amount The amount of money to deposit
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws cat.gencat.agaur.hexastock.model.portfolio.InsufficientFundsException if the deposit amount is not positive
     */
    void deposit(PortfolioId portfolioId, Money amount);

    /**
     * Withdraws money from a portfolio's cash balance.
     *
     * @param portfolioId The ID of the portfolio to withdraw from
     * @param amount The amount of money to withdraw
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidAmountException if the withdrawal amount is not positive
     * @throws cat.gencat.agaur.hexastock.model.portfolio.InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    void withdraw(PortfolioId portfolioId, Money amount);
}
