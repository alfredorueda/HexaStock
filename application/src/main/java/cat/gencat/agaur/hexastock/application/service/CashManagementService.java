package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict;
import cat.gencat.agaur.hexastock.application.port.in.CashManagementUseCase;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.Transaction;
import jakarta.transaction.Transactional;

/**
 * Application service for cash management operations: depositing and withdrawing funds.
 *
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link CashManagementUseCase}) to be used by driving adapters</li>
 *   <li>Uses secondary ports ({@link PortfolioPort} and {@link TransactionPort}) to communicate with driven adapters</li>
 * </ul>
 * </p>
 *
 * <p>This service was extracted from the former {@code PortfolioManagementService} together with
 * {@link PortfolioLifecycleService} to improve Interface Segregation.</p>
 *
 * @see PortfolioLifecycleService
 */
@Transactional
public class CashManagementService implements CashManagementUseCase {

    private final PortfolioPort portfolioPort;
    private final TransactionPort transactionPort;

    public CashManagementService(PortfolioPort portfolioPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.transactionPort = transactionPort;
    }

    @Override
    @RetryOnWriteConflict
    public void deposit(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.deposit(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createDeposit(portfolioId, amount);
        transactionPort.save(transaction);
    }

    @Override
    @RetryOnWriteConflict
    public void withdraw(PortfolioId portfolioId, Money amount) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.withdraw(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createWithdrawal(portfolioId, amount);
        transactionPort.save(transaction);
    }

    private Portfolio getPortfolio(PortfolioId portfolioId) {
        return portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
    }
}
