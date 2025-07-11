package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.SaleResponseDTO;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioManagmentUseCase;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class PortfolioStockOperationsService implements PortfolioStockOperationsUseCase {

    private final PortfolioPort portfolioPort;
    private final TransactionPort transactionPort;
    private final StockPriceProviderPort stockPriceProviderPort;

    public PortfolioStockOperationsService(PortfolioPort portfolioPort, StockPriceProviderPort stockPriceProviderPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public void buyStock(String portfolioId, Ticker ticker, int quantity) {

        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId);

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);

        portfolio.buy(ticker, quantity, BigDecimal.valueOf(stockPrice.getPrice()));

        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createPurchase(
                portfolioId, ticker, quantity, BigDecimal.valueOf(stockPrice.getPrice()));
        transactionPort.save(transaction);

    }

    @Override
    public SellResult sellStock(String portfolioId, Ticker ticker, int quantity) {

        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId);

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);

        SellResult sellResult = portfolio.sell(ticker, quantity, BigDecimal.valueOf(stockPrice.getPrice()));

        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSale(
                portfolioId, ticker, quantity, BigDecimal.valueOf(stockPrice.getPrice()), sellResult.proceeds(), sellResult.profit());
        transactionPort.save(transaction);

        return sellResult;

    }
}
