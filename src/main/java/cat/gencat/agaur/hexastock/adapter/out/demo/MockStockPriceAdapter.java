package cat.gencat.agaur.hexastock.adapter.out.demo;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MockStockPriceAdapter provides mock stock price data for testing and demonstration purposes.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary adapter</strong> (driven adapter)
 * that implements a secondary port ({@link StockPriceProviderPort}) to provide stock price data
 * without requiring an actual external market data service.</p>
 * 
 * <p>This adapter:</p>
 * <ul>
 *   <li>Maintains an in-memory database of mock stock prices for popular companies</li>
 *   <li>Adds small random variations to simulate real market fluctuations</li>
 *   <li>Returns consistent data in the domain's {@link StockPrice} format</li>
 * </ul>
 * 
 * <p>The mock adapter is only active when the "mock" Spring profile is enabled,
 * allowing the application to run in environments without external API access,
 * or for testing and demonstration purposes where real market data is not needed.</p>
 * 
 * <p>This implementation demonstrates the power of hexagonal architecture, where
 * we can completely swap out the source of stock price data without changing any
 * of the application's core business logic.</p>
 */
@Component
@Profile("mock")
public class MockStockPriceAdapter implements StockPriceProviderPort {

    /**
     * In-memory database of mock stock prices for popular companies.
     */
    private final Map<String, Double> mockPrices = new ConcurrentHashMap<>();
    
    /**
     * Random number generator for simulating price variations.
     */
    private final Random random = new Random();

    /**
     * Constructs a new MockStockPriceAdapter with pre-populated stock prices.
     * 
     * <p>Initializes the mock database with a set of popular stocks and
     * realistic price points to simulate a real market environment.</p>
     */
    public MockStockPriceAdapter() {
        mockPrices.put("AAPL", 201.45);
        mockPrices.put("MSFT", 472.75);
        mockPrices.put("GOOGL", 177.63);
        mockPrices.put("AMZN", 216.98);
        mockPrices.put("TSLA", 308.58);
        mockPrices.put("NVDA", 142.63);
        mockPrices.put("META", 694.06);
        mockPrices.put("JPM", 266.74);
        mockPrices.put("BAC", 44.87);
        mockPrices.put("WFC", 61.16);
        mockPrices.put("GS", 460.67);
        mockPrices.put("GRFS", 8.83);
    }

    /**
     * Fetches a mock price for a given stock ticker.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Validates the ticker symbol</li>
     *   <li>Retrieves the base price from the mock database</li>
     *   <li>Adds a small random variation (±1%) to simulate market fluctuations</li>
     *   <li>Creates and returns a domain StockPrice object</li>
     * </ol>
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the mock price and related information
     * @throws IllegalArgumentException if the ticker is null or not found in the mock database
     */
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("Stock symbol cannot be empty");
        }

        String normalizedSymbol = ticker.value();

        // Check if the symbol exists in our mock database
        if (!mockPrices.containsKey(normalizedSymbol)) {
            throw new IllegalArgumentException(normalizedSymbol);
        }

        // Get the base price
        double basePrice = mockPrices.get(normalizedSymbol);

        // Add some small random variation to simulate market changes
        double variation = (random.nextDouble() - 0.5) * 2.0; // -1.0 to 1.0
        double currentPrice = basePrice * (1.0 + (variation / 100.0));

        // Round to 2 decimal places
        currentPrice = Math.round(currentPrice * 100.0) / 100.0;

        return new StockPrice(ticker, currentPrice, LocalDateTime.now()
                .atZone(ZoneId.of("Europe/Madrid")) .toInstant(), "USD");
    }

    @Override
    public Map<Ticker, StockPrice> fetchStockPrice(Set<Ticker> sTickers) {
        return sTickers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::fetchStockPrice
                ));
    }
}
