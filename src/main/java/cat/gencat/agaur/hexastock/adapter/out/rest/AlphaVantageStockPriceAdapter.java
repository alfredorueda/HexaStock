package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * AlphaVantageStockPriceAdapter implements the stock price provider port by connecting to the Alpha Vantage API using Spring Boot's RestClient.
 *
 * <p>This project is pedagogical and demonstrates Domain-Driven Design (DDD) and Hexagonal Architecture.
 * The domain use cases only need to know "what" (getting a stock price) without caring "how" (HTTP, JSON/XML, which provider).
 * These adapters belong to the infrastructure layer and implement the port to fetch stock prices from external providers.
 * Spring profiles make it easy to switch between providers without changing domain logic.
 * Separating domain needs from infrastructure details improves maintainability, testability, and flexibility.</p>
 *
 * This adapter is structurally identical to the FinhubStockPriceAdapter, but uses Alpha Vantage endpoints and configuration.
 * It is only active when the "alphaVantage" Spring profile is enabled.
 */
@Component
@Profile("alphaVantage")
public class AlphaVantageStockPriceAdapter implements StockPriceProviderPort {
    @Value("${alphaVantage.api.key}")
    private String apiKey;
    @Value("${alphaVantage.api.base-url}")
    private String baseUrl;
    @Value("${alphaVantage.api.timeout:5000}")
    private int timeout;

    // Throttles outbound API calls to avoid hitting free-tier rate limits.
    // We intentionally sleep the current thread before performing the request.
    // NOTE: If you move to reactive/non-blocking I/O in the future, replace this with a non-blocking delay.
    private static final long THROTTLE_MS = 500L;
    private void throttle() {
        try {
            Thread.sleep(THROTTLE_MS);
        } catch (InterruptedException ie) {
            // Restore the interrupted status so higher-level code can react if needed.
            Thread.currentThread().interrupt();
            // Optionally log the interruption; we do NOT rethrow to avoid breaking the call path.
        }
    }

    private static final String GLOBAL_QUOTE_FIELD = "Global Quote";

    /**
     * Fetches the current price for a given stock ticker from the Alpha Vantage API.
     *
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws ExternalApiException if there is an error communicating with the Alpha Vantage API or the response is invalid
     */
    @Override
    @Cacheable(cacheNames = "stockPrices", key = "#ticker.value()", sync = true)
    public StockPrice fetchStockPrice(Ticker ticker) {
        // Throttle to stay within free-tier rate limits (Alpha Vantage).
        throttle();

        // Alpha Vantage endpoint for real-time quote
        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", baseUrl, ticker.value(), apiKey);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        JsonNode responseJson;
        try {
            responseJson = restClient.get()
                .uri(url)
                .retrieve()
                .body(JsonNode.class);
            if (responseJson == null || responseJson.get(GLOBAL_QUOTE_FIELD) == null || responseJson.get(GLOBAL_QUOTE_FIELD).get("05. price") == null) {
                throw new ExternalApiException("Invalid response from Alpha Vantage API: missing or malformed price data");
            }
        } catch (Exception e) {
            throw new ExternalApiException("Error communicating with Alpha Vantage API. Please check the value for alphaVantage.api.key in application.properties", e);
        }
        double currentPrice;
        try {
            currentPrice = Double.parseDouble(responseJson.get(GLOBAL_QUOTE_FIELD).get("05. price").asText());
        } catch (Exception e) {
            throw new ExternalApiException("Could not parse price from Alpha Vantage response", e);
        }
        return new StockPrice(ticker, currentPrice, LocalDateTime.now().atZone(ZoneId.of("Europe/Madrid")).toInstant(), "USD");
    }
}
