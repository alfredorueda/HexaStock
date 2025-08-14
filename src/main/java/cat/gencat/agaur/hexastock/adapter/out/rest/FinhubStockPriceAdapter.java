package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * FinhubStockPriceAdapter implements the stock price provider port by connecting to the Finnhub API.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary adapter</strong> (driven adapter)
 * that implements a secondary port ({@link StockPriceProviderPort}) to connect the application
 * core with an external service - in this case, the Finnhub financial API.</p>
 * 
 * <p>This adapter:</p>
 * <ul>
 *   <li>Connects to the Finnhub API using the Finnhub Java client library</li>
 *   <li>Retrieves real-time stock quotes for requested ticker symbols</li>
 *   <li>Converts the external API response into the domain's {@link StockPrice} model</li>
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

    /**
     * Fetches the current price for a given stock ticker from the Finnhub API.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a Finnhub client with the API token</li>
     *   <li>Requests a quote for the specified ticker symbol</li>
     *   <li>Extracts the current price from the response</li>
     *   <li>Creates and returns a domain StockPrice object</li>
     * </ol>
     * 
     * @param ticker The ticker symbol of the stock to get the price for
     * @return A StockPrice object containing the current price and related information
     * @throws RuntimeException if there is an error communicating with the Finnhub API
     */
    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        String url = String.format("%s/quote?symbol=%s&token=%s", finhubApiUrl, ticker.value(), finhubApiKey);
        RestClient restClient = RestClient.builder().build();
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
