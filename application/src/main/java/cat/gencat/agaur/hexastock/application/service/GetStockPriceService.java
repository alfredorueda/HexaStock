package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

/**
 * GetStockPriceService implements the use case for retrieving stock price information.
 * 
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link GetStockPriceUseCase}) to be used by driving adapters</li>
 *   <li>Uses a secondary port ({@link StockPriceProviderPort}) to communicate with driven adapters</li>
 * </ul>
 * </p>
 * 
 * <p>This service acts as a simple mediator between the primary and secondary ports,
 * delegating the actual price retrieval to the injected {@link StockPriceProviderPort} implementation.</p>
 * 
 * <p>The service is responsible for:
 * <ul>
 *   <li>Accepting stock price requests from various parts of the application</li>
 *   <li>Delegating to the appropriate external provider to fetch the current price</li>
 *   <li>Returning price information in the domain model format</li>
 * </ul>
 * </p>
 */

public class GetStockPriceService implements GetStockPriceUseCase {

    /**
     * The secondary port used to fetch stock prices from an external provider.
     */
    private final StockPriceProviderPort stockPriceProviderPort;

    /**
     * Constructs a new GetStockPriceService with the required secondary port.
     * 
     * @param stockPriceProviderPort The secondary port that will provide stock price data
     */
    public GetStockPriceService(StockPriceProviderPort stockPriceProviderPort) {
        this.stockPriceProviderPort = stockPriceProviderPort;
    }

    /**
     * Retrieves the current price for a given stock ticker.
     * 
     * <p>This method delegates to the injected {@link StockPriceProviderPort} to fetch
     * the current price information for the specified ticker.</p>
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    public StockPrice getPrice(Ticker ticker) {
        return stockPriceProviderPort.fetchStockPrice(ticker);
    }
}
