package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/**
 * PortfolioStockOperationsService implements the core use cases for stock trading operations.
 * 
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link PortfolioStockOperationsUseCase}) to be used by driving adapters</li>
 *   <li>Uses secondary ports ({@link PortfolioPort}, {@link TransactionPort}, and {@link StockPriceProviderPort}) 
 *       to communicate with driven adapters</li>
 * </ul>
 * </p>
 * 
 * <p>This service orchestrates stock trading operations by:
 * <ul>
 *   <li>Retrieving current stock prices from external providers</li>
 *   <li>Executing buy and sell operations on portfolios</li>
 *   <li>Recording transactions for auditing and tracking purposes</li>
 *   <li>Calculating profits/losses for sell operations using FIFO accounting</li>
 * </ul>
 * </p>
 * 
 * <p>The service ensures that all operations are transactional, maintaining data consistency
 * between the portfolio state, stock prices, and transaction records.</p>
 */

@Transactional
public class PortfolioStockOperationsService implements PortfolioStockOperationsUseCase {

    /**
     * The secondary port used to persist and retrieve portfolios.
     */
    private final PortfolioPort portfolioPort;
    
    /**
     * The secondary port used to record financial transactions.
     */
    private final TransactionPort transactionPort;
    
    /**
     * The secondary port used to retrieve current stock prices.
     */
    private final StockPriceProviderPort stockPriceProviderPort;

    /**
     * Constructs a new PortfolioStockOperationsService with the required secondary ports.
     * 
     * @param portfolioPort The port for portfolio persistence operations
     * @param stockPriceProviderPort The port for retrieving stock prices
     * @param transactionPort The port for transaction recording operations
     */
    public PortfolioStockOperationsService(PortfolioPort portfolioPort, StockPriceProviderPort stockPriceProviderPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.transactionPort = transactionPort;
    }

    /**
     * Buys shares of a stock for a portfolio.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Gets the current price of the stock</li>
     *   <li>Executes the buy operation on the portfolio domain object</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a purchase transaction record</li>
     * </ol>
     * </p>
     * 
     * @param portfolioId The ID of the portfolio to buy stock for
     * @param ticker The ticker symbol of the stock to buy
     * @param quantity The number of shares to buy
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds for the purchase
     */
    @Override
    public void buyStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        portfolio.buy(ticker, quantity, price);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createPurchase(portfolioId, ticker, quantity, price);
        transactionPort.save(transaction);
    }

    /**
     * Sells shares of a stock from a portfolio.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Gets the current price of the stock</li>
     *   <li>Executes the sell operation on the portfolio domain object</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a sale transaction record with profit/loss information</li>
     *   <li>Returns the SellResult with financial details of the sale</li>
     * </ol>
     * </p>
     * 
     * <p>The sale follows FIFO (First-In-First-Out) accounting principles,
     * selling the oldest shares first to calculate cost basis and profit.</p>
     * 
     * @param portfolioId The ID of the portfolio to sell stock from
     * @param ticker The ticker symbol of the stock to sell
     * @param quantity The number of shares to sell
     * @return A SellResult containing proceeds, cost basis, and profit information
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.DomainException if the ticker is not found in holdings
     * @throws ConflictQuantityException if trying to sell more shares than owned
     */
    @Override
    public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        SellResult sellResult = portfolio.sell(ticker, quantity, price);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSale(
                portfolioId, ticker, quantity, price, sellResult.proceeds(), sellResult.profit());
        transactionPort.save(transaction);

        return sellResult;
    }
}
