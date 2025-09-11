package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * FinhubStockPriceAdapter implements the stock price provider port by connecting to the Finnhub API using Spring Boot's RestClient.
 *
 * <p>This project is pedagogical and demonstrates Domain-Driven Design (DDD) and Hexagonal Architecture. The domain use cases only need to know "what" (getting a stock price) without caring "how" (HTTP, JSON/XML, which provider). These adapters belong to the infrastructure layer and implement the port to fetch stock prices from external providers. Spring profiles make it easy to switch between providers without changing domain logic. Separating domain needs from infrastructure details improves maintainability, testability, and flexibility.</p>
 *
 * <p>In hexagonal architecture terms, this is a <strong>secondary adapter</strong> (driven adapter)
 * that implements a secondary port ({@link StockPriceProviderPort}) to connect the application
 * core with an external service - in this case, the Finnhub financial API.</p>
 *
 * <p>This adapter:</p>
 * <ul>
 *   <li>Connects to the Finnhub API using Spring Boot's RestClient (no external Finnhub client library)</li>
 *   <li>Retrieves real-time stock quotes for requested ticker symbols synchronously</li>
 *   <li>Uses configuration properties for the API base URL and key</li>
 *   <li>Converts the external API response into the domain's {@link StockPrice} model</li>
 *   <li>Handles errors and invalid responses gracefully</li>
 * </ul>
 *
 * <p>The adapter is only active when the "finhub" Spring profile is enabled,
 * allowing the application to switch between different stock price providers
 * (like this one or a mock implementation) based on the runtime environment.</p>
 */
@Component
@Profile("finhub")
public class FinhubStockPriceAdapter implements StockPriceProviderPort {

    @Value("${finhub.api.key}")
    private String finhubApiKey;

    @Value("${finhub.api.url}")
    private String finhubApiUrl;

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

    /**
     * Fetches the current price for a given stock ticker from the Finnhub API.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Builds the Finnhub API URL using the ticker symbol and API key, with the base URL from configuration</li>
     *   <li>Uses Spring's RestClient to synchronously request a quote for the specified ticker symbol</li>
     *   <li>Parses the JSON response and extracts the current price</li>
     *   <li>Creates and returns a domain StockPrice object</li>
     * </ol>
     *
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws ExternalApiException if there is an error communicating with the Finnhub API or the response is invalid
     */
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // Throttle to stay within free-tier rate limits (Finhub).
        throttle();

        String url = String.format("%s/quote?symbol=%s&token=%s", finhubApiUrl, ticker.value(), finhubApiKey);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000); // milliseconds
        factory.setReadTimeout(5_000);    // milliseconds

        RestClient restClient = RestClient.builder()
                .baseUrl(finhubApiUrl)
                .requestFactory(factory)
                .build();

        // Fetch the quote from the Finnhub API

        JsonNode quoteJson;
        try {
            quoteJson = restClient.get()
                .uri(url)
                .retrieve()
                .body(JsonNode.class);
            if (quoteJson == null || quoteJson.get("c") == null || !quoteJson.get("c").isNumber()) {
                throw new ExternalApiException("Invalid response from Finnhub API: missing or malformed price data");
            }
        } catch (Exception e) {
            throw new ExternalApiException("Error communicating with Finnhub API. Please check the value for finhubApiKey in application.properties ", e);
        }
        double currentPrice = quoteJson.get("c").asDouble();
        return new StockPrice(ticker, currentPrice, LocalDateTime.now()
                .atZone(ZoneId.of("Europe/Madrid")).toInstant(), "USD");
    }

}
