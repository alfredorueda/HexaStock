package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the stock price endpoint (US-10).
 *
 * <p>Uses the {@code mockfinhub} profile, so the price returned for valid tickers
 * is a random-but-reasonable value. Tests assert on structure (fields present)
 * rather than exact price values.</p>
 */
@DisplayName("Stock Price Integration Tests (US-10)")
class StockPriceRestIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    @Test
    @DisplayName("GET /api/stocks/{symbol} with valid ticker returns 200 with price data")
    @SpecificationRef(value = "US-10.AC-1", level = TestLevel.INTEGRATION, feature = "get-stock-price.feature")
    void getStockPrice_validTicker_returns200() {
        RestAssured.given()
                .get("/api/stocks/AAPL")
            .then()
                .statusCode(200)
                .body("symbol", equalTo("AAPL"))
                .body("price", notNullValue())
                .body("time", notNullValue())
                .body("currency", equalTo("USD"));
    }

    @Test
    @DisplayName("GET /api/stocks/{symbol} with invalid ticker returns 400")
    @SpecificationRef(value = "US-10.AC-2", level = TestLevel.INTEGRATION, feature = "get-stock-price.feature")
    void getStockPrice_invalidTicker_returns400() {
        RestAssured.given()
                .get("/api/stocks/aapl_invalid")
            .then()
                .statusCode(400)
                .body("title", equalTo("Invalid Ticker"))
                .body("status", equalTo(400));
    }
}
