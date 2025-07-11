package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.PortfolioManagmentUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.Transaction;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class PortfolioManagmentService implements PortfolioManagmentUseCase {

    private final PortfolioPort portfolioPort;
    private final TransactionPort transactionPort;

    public PortfolioManagmentService(PortfolioPort portfolioPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public Portfolio createPortfolio(String ownerName) {

        Portfolio portfolio = Portfolio.create(ownerName);
        portfolioPort.createPortfolio(portfolio);
        return portfolio;
    }

    @Override
    public Portfolio getPortfolio(String id) {
        return portfolioPort.getPortfolioById(id);
    }

    @Override
    public void deposit(String portfolioId, Money amount) {

        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.deposit(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createDeposit(portfolioId, amount.amount());
        transactionPort.save(transaction);
    }

    @Override
    public void withdraw(String portfolioId, Money amount) {

        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.withdraw(amount);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createWithdrawal(portfolioId, amount.amount());
        transactionPort.save(transaction);

    }
}
