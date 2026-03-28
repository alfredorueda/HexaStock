package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

/**
 * GetStockPriceUseCase defines the primary port for retrieving stock price information.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary port</strong> (input port)
 * that defines an interface through which the application core can be driven. It
 * encapsulates the use case of retrieving the current price of a stock.</p>
 * 
 * <p>This interface allows the application to:</p>
 * <ul>
 *   <li>Fetch the current price of a stock by its ticker symbol</li>
 *   <li>Isolate the core domain logic from the details of how stock prices are obtained</li>
 * </ul>
 * 
 * <p>External adapters (such as a market data API client) will implement this interface
 * to provide the actual price data to the application.</p>
 */
public interface GetStockPriceUseCase {

    /**
     * Retrieves the current price for a given stock ticker.
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    StockPrice getPrice(Ticker ticker);
}
