package cat.gencat.agaur.hexastock.adapter.out.demo;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("mock")
public class MockStockPriceAdapter implements StockPriceProviderPort {

    private final Map<String, Double> mockPrices = new ConcurrentHashMap<>();
    private final Random random = new Random();

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
}
