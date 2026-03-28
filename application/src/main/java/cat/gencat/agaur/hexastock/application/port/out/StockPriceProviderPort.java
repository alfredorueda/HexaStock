package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StockPriceProviderPort defines the secondary port for retrieving stock price information.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary port</strong> (output port)
 * that defines how the application core communicates with external stock price providers.
 * It is implemented by driven adapters that connect to actual market data services.</p>
 * 
 * <p>This interface allows the application to:</p>
 * <ul>
 *   <li>Abstract away the details of how stock prices are obtained</li>
 *   <li>Support multiple stock price data sources without changing the application core</li>
 *   <li>Facilitate testing by allowing mock implementations</li>
 * </ul>
 * 
 * <p>Implementations of this interface might connect to:</p>
 * <ul>
 *   <li>Real-time market data APIs (like AlphaVantage, Finnhub, Yahoo Finance)</li>
 *   <li>Historical price databases</li>
 *   <li>Mock data providers for testing</li>
 * </ul>
 */
public interface StockPriceProviderPort {

    /**
     * Fetches the current price for a given stock ticker.
     * 
     * <p>This method retrieves the latest available price information for the
     * specified stock from an external data source.</p>
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    StockPrice fetchStockPrice(Ticker ticker);

    /**
     * Fetches the current price for each given stock ticker.
     *
     * <p>This method retrieves the latest available price information for the
     * specified stock from an external data source.</p>
     *
     * @param sTickers The list of ticker's symbol of the stock to get the price for
     * @return A Map<StockPrice> containing the current price and related information for each Ticker
     * @throws RuntimeException if the price cannot be retrieved for any reason
     */
    default Map<Ticker, StockPrice> fetchStockPrice(Set<Ticker> sTickers) {

        return sTickers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::fetchStockPrice
                ));

    }
}