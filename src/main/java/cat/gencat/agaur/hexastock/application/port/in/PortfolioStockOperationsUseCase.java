package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.SellResult;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;

/**
 * PortfolioStockOperationsUseCase defines the primary port for stock trading operations.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary port</strong> (input port)
 * that defines how the outside world can interact with the application core for stock
 * trading operations. It encapsulates the following use cases:</p>
 * <ul>
 *   <li>Buying shares of a stock for a portfolio</li>
 *   <li>Selling shares of a stock from a portfolio with profit/loss calculation</li>
 * </ul>
 * 
 * <p>This interface is implemented by application services in the domain layer and
 * used by driving adapters (like REST controllers) in the infrastructure layer.</p>
 * 
 * <p>Stock operations automatically handle:</p>
 * <ul>
 *   <li>Retrieving current market prices</li>
 *   <li>Updating portfolio cash balances</li>
 *   <li>Managing stock holdings and lots</li>
 *   <li>Recording transaction history</li>
 *   <li>Applying FIFO accounting for sales</li>
 * </ul>
 */
public interface PortfolioStockOperationsUseCase {

    /**
     * Buys shares of a stock for a portfolio.
     * 
     * <p>This operation:</p>
     * <ol>
     *   <li>Retrieves the current market price of the stock</li>
     *   <li>Verifies the portfolio has sufficient funds</li>
     *   <li>Creates a new lot in the appropriate holding</li>
     *   <li>Deducts the purchase amount from the portfolio's cash balance</li>
     *   <li>Records the purchase transaction</li>
     * </ol>
     * 
     * @param portfolioId The ID of the portfolio to buy stock for
     * @param ticker The ticker symbol of the stock to buy
     * @param quantity The number of shares to buy
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds for the purchase
     */
    void buyStock(String portfolioId, Ticker ticker, int quantity);
    
    /**
     * Sells shares of a stock from a portfolio.
     * 
     * <p>This operation:</p>
     * <ol>
     *   <li>Retrieves the current market price of the stock</li>
     *   <li>Verifies the portfolio has sufficient shares to sell</li>
     *   <li>Applies FIFO accounting to determine which lots to sell from</li>
     *   <li>Calculates proceeds, cost basis, and profit/loss</li>
     *   <li>Adds the proceeds to the portfolio's cash balance</li>
     *   <li>Records the sale transaction</li>
     * </ol>
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
    SellResult sellStock(String portfolioId, Ticker ticker, int quantity);
}
