package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.Price;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for portfolio trading operations:
 * buying shares, selling shares, multi-step trading flows,
 * and Gherkin-aligned FIFO verification.
 *
 * <p>Uses a {@link FixedPriceStockPriceAdapter} ({@code @Primary}) that overrides the mock
 * stock price adapter. Non-Gherkin tests use the fallback price (150.00); Gherkin tests
 * configure a price queue mapping each operation to the exact Gherkin scenario prices.</p>
 */
@Import(PortfolioTradingRestIntegrationTest.FixedPriceConfiguration.class)
class PortfolioTradingRestIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    FixedPriceStockPriceAdapter fixedPriceAdapter;

    @Nested
    class WhenPortfolioExists {

        String portfolioId;

        @BeforeEach
        void createBasePortfolio() {
            portfolioId = createPortfolio("IntegrationUser");
        }

        @Test
        @SpecificationRef(value = "US-07.AC-1", level = TestLevel.INTEGRATION)
        void endToEnd_depositBuySellWithdraw() {
            deposit(portfolioId, 100_000);

            // Verify balance after deposit
            getPortfolio(portfolioId)
                    .body("balance", equalTo(100_000.0f));

            buy(portfolioId, "AAPL", 10);

            // Holdings should contain AAPL
            getHoldings(portfolioId)
                    .body("size()", equalTo(1))
                    .body("[0].ticker", equalTo("AAPL"))
                    .body("[0].remaining", equalTo(10));

            sell(portfolioId, "AAPL", 5)
                    .statusCode(200)
                    .body("portfolioId", equalTo(portfolioId))
                    .body("ticker", equalTo("AAPL"))
                    .body("quantity", equalTo(5))
                    .body("proceeds", greaterThan(0f));

            // 5 shares should remain
            getHoldings(portfolioId)
                    .body("[0].remaining", equalTo(5));

            withdraw(portfolioId, 1_000);

            // Balance should have decreased
            getPortfolio(portfolioId)
                    .body("balance", greaterThan(0f));
        }

        @Nested
        class BuyingShares {

            @BeforeEach
            void fundPortfolio() {
                deposit(portfolioId, 50_000);
            }

            @Test
            void buyReducesBalanceAndAddsHolding() {
                float balanceBefore = getPortfolio(portfolioId).extract().path("balance");

                buy(portfolioId, "AAPL", 5);

                float balanceAfter = getPortfolio(portfolioId).extract().path("balance");
                org.junit.jupiter.api.Assertions.assertTrue(balanceAfter < balanceBefore,
                        "Balance should decrease after purchase");

                getHoldings(portfolioId)
                        .body("size()", greaterThanOrEqualTo(1))
                        .body("find { it.ticker == 'AAPL' }.remaining", equalTo(5));
            }

            @Test
            void multipleBuysAndSellsAcrossTickers() {
                String[] tickers = {"AAPL", "MSFT", "GOOG"};
                int[] buyQuantities = {5, 7};

                for (String ticker : tickers) {
                    int totalBought = 0;
                    for (int qty : buyQuantities) {
                        buy(portfolioId, ticker, qty);
                        totalBought += qty;
                    }
                    int sellQty = totalBought - 3;
                    sell(portfolioId, ticker, sellQty).statusCode(200);

                    int remaining = getHoldings(portfolioId).extract()
                            .path("find { it.ticker == '" + ticker + "' }.remaining");
                    org.junit.jupiter.api.Assertions.assertEquals(3, remaining);
                }
            }

            @Test
            void buyWithInsufficientFunds_returns409() {
                // Withdraw almost everything first so we know the balance is low
                withdraw(portfolioId, 49_999);

                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("AAPL", 10))
                        .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                        .statusCode(409)
                        .body("title", equalTo("Insufficient Funds"))
                        .body("detail", containsString("Insufficient funds"))
                        .body("status", equalTo(409));
            }

            @Test
            void buyWithZeroQuantity_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("AAPL", 0))
                        .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Quantity"))
                        .body("detail", containsString("Quantity must be positive"))
                        .body("status", equalTo(400));
            }

            @Test
            void buyWithNegativeQuantity_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("AAPL", -5))
                        .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Quantity"))
                        .body("detail", containsString("Quantity must be positive"))
                        .body("status", equalTo(400));
            }

            @Test
            void buyWithInvalidTicker_returns400_andNoHoldingCreated() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("ZZZZ_INVALID", 5))
                        .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Ticker"))
                        .body("detail", containsString("ZZZZ_INVALID"))
                        .body("status", equalTo(400));

                getHoldings(portfolioId).body("size()", equalTo(0));
            }

            @Test
            void buyWithEmptyTicker_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("", 5))
                        .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Ticker"))
                        .body("status", equalTo(400));
            }
        }

        @Nested
        class SellingShares {

            @BeforeEach
            void fundAndBuy() {
                deposit(portfolioId, 10_000);
                buy(portfolioId, "AAPL", 5);
            }

            @Test
            @SpecificationRef(value = "US-07.AC-1", level = TestLevel.INTEGRATION)
            void sellReturnsProceeds_andUpdatesHoldings() {
                sell(portfolioId, "AAPL", 3)
                        .statusCode(200)
                        .body("portfolioId", equalTo(portfolioId))
                        .body("ticker", equalTo("AAPL"))
                        .body("quantity", equalTo(3))
                        .body("proceeds", greaterThan(0f));

                getHoldings(portfolioId)
                        .body("find { it.ticker == 'AAPL' }.remaining", equalTo(2));
            }

            @Test
            @SpecificationRef(value = "US-07.AC-3", level = TestLevel.INTEGRATION)
            void sellMoreThanOwned_returns409() {
                sell(portfolioId, "AAPL", 10)
                        .statusCode(409)
                        .body("title", equalTo("Conflict Quantity"))
                        .body("detail", containsString("Not enough shares to sell"))
                        .body("status", equalTo(409));
            }

            @Test
            @SpecificationRef(value = "US-07.AC-4", level = TestLevel.INTEGRATION)
            void sellWithZeroQuantity_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("AAPL", 0))
                        .post("/api/portfolios/" + portfolioId + "/sales")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Quantity"))
                        .body("detail", containsString("Quantity must be positive"))
                        .body("status", equalTo(400));
            }

            @Test
            @SpecificationRef(value = "US-07.AC-5", level = TestLevel.INTEGRATION)
            void sellWithNegativeQuantity_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonTrade("AAPL", -3))
                        .post("/api/portfolios/" + portfolioId + "/sales")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Quantity"))
                        .body("detail", containsString("Quantity must be positive"))
                        .body("status", equalTo(400));
            }

            @Test
            @SpecificationRef(value = "US-07.AC-6", level = TestLevel.INTEGRATION)
            void sellTickerNotOwned_returns404() {
                sell(portfolioId, "MSFT", 1)
                        .statusCode(404)
                        .body("title", equalTo("Holding Not Found"))
                        .body("status", equalTo(404));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Gherkin-aligned FIFO integration test with deterministic prices
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A test-only stock price adapter that returns prices from a pre-configured queue.
     * Each call to {@link #fetchStockPrice} polls the next price from the queue.
     * If the queue is empty, returns a default fallback price.
     *
     * <p>This demonstrates a key Hexagonal Architecture principle: adapters are swappable.
     * The domain and application core are completely unaware of which price provider is used.
     * By injecting a deterministic adapter, integration tests can verify exact financial
     * outcomes (proceeds, costBasis, profit) matching the Gherkin specification.</p>
     */
    static class FixedPriceStockPriceAdapter implements StockPriceProviderPort {

        private final ConcurrentLinkedQueue<Price> priceQueue = new ConcurrentLinkedQueue<>();
        private final Price fallbackPrice;

        FixedPriceStockPriceAdapter(Price fallbackPrice) {
            this.fallbackPrice = fallbackPrice;
        }

        void enqueuePrice(Price price) {
            priceQueue.add(price);
        }

        void enqueuePrices(Price... prices) {
            for (Price p : prices) {
                priceQueue.add(p);
            }
        }

        void clear() {
            priceQueue.clear();
        }

        @Override
        public StockPrice fetchStockPrice(Ticker ticker) {
            Price price = priceQueue.poll();
            if (price == null) {
                price = fallbackPrice;
            }
            return new StockPrice(ticker, price, Instant.now());
        }
    }

    /**
     * Provides a {@link FixedPriceStockPriceAdapter} as a {@code @Primary} bean,
     * overriding the mockfinhub adapter for tests that need deterministic prices.
     */
    @TestConfiguration
    static class FixedPriceConfiguration {

        @Bean
        @Primary
        FixedPriceStockPriceAdapter fixedPriceStockPriceAdapter() {
            return new FixedPriceStockPriceAdapter(Price.of("150.00"));
        }
    }

    /**
     * Gherkin-aligned integration tests that mirror the tutorial's functional specification.
     *
     * <p>These tests execute the full use case through the REST API (true black-box)
     * and verify the exact financial results from the Gherkin scenarios in
     * {@code doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md}.</p>
     *
     * <p>A {@link FixedPriceStockPriceAdapter} is used to control stock prices,
     * making the tests deterministic and allowing verification of exact FIFO calculations.</p>
     *
     * @see <a href="doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md">Sell Stock Tutorial — Functional Specification</a>
     */
    @Nested
    @DisplayName("Gherkin FIFO Scenarios (deterministic prices)")
    class GherkinFifoScenarios {

        String portfolioId;

        @BeforeEach
        void setUpGherkinScenario() {
            // Reset the price queue for each test
            fixedPriceAdapter.clear();

            // Background: create portfolio for "Alice" and deposit sufficient funds
            portfolioId = createPortfolio("Alice");
            deposit(portfolioId, 100_000);

            // Background: buy 10 shares of AAPL @ 100.00 (Lot #1)
            fixedPriceAdapter.enqueuePrice(Price.of("100.00"));
            buy(portfolioId, "AAPL", 10);

            // Background: buy 5 shares of AAPL @ 120.00 (Lot #2)
            fixedPriceAdapter.enqueuePrice(Price.of("120.00"));
            buy(portfolioId, "AAPL", 5);
        }

        /**
         * Sells shares using BigDecimal JSON parsing for precise financial assertions.
         * This per-request config avoids affecting other tests that expect float parsing.
         */
        private ValidatableResponse sellPrecise(String portfolioId, String ticker, int quantity) {
            return RestAssured.given()
                    .config(RestAssured.config().jsonConfig(
                            JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                    .contentType(ContentType.JSON)
                    .body(jsonTrade(ticker, quantity))
                    .post("/api/portfolios/" + portfolioId + "/sales")
                .then();
        }

        @Test
        @DisplayName("Selling 8 shares consumed entirely from a single lot (Gherkin Scenario 1)")
        @SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.INTEGRATION, feature = "sell-stocks.feature")
        void sellSharesConsumedFromSingleLot_FIFOGherkinScenario() {
            // When: sell 8 shares of AAPL at market price 150.00
            fixedPriceAdapter.enqueuePrice(Price.of("150.00"));

            sellPrecise(portfolioId, "AAPL", 8)
                    .statusCode(200)

                    // Then: sale response matches Gherkin expectations
                    .body("portfolioId", equalTo(portfolioId))
                    .body("ticker", equalTo("AAPL"))
                    .body("quantity", equalTo(8))
                    // proceeds  = 8 × 150.00 = 1200.00
                    .body("proceeds", comparesEqualTo(new BigDecimal("1200.00")))
                    // costBasis = 8 × 100.00 = 800.00 (all from Lot #1)
                    .body("costBasis", comparesEqualTo(new BigDecimal("800.00")))
                    // profit    = 1200.00 − 800.00 = 400.00
                    .body("profit", comparesEqualTo(new BigDecimal("400.00")));

            // And: FIFO lot consumption verified — 15 initial − 8 sold = 7 remaining
            // (The costBasis of 800.00 = 8 × 100.00 proves FIFO consumed from Lot #1,
            //  not Lot #2 at 120.00. If LIFO were used, costBasis = 5×120 + 3×100 = 900.00)
            getHoldings(portfolioId)
                    .body("find { it.ticker == 'AAPL' }.remaining", equalTo(7));
        }

        // Link to Gherkin scenario:
        // https://github.com/alfredorueda/HexaStock/blob/main/doc/stock-portfolio-api-specification.md#27-us-07--sell-stocks
        @Test
        @DisplayName("Selling 12 shares consumed across multiple lots (Gherkin Scenario 2)")
        @SpecificationRef(value = "US-07.FIFO-2", level = TestLevel.INTEGRATION, feature = "sell-stocks.feature")
        void sellSharesAcrossMultipleLots_FIFOGherkinScenario() {
            // When: sell 12 shares of AAPL at market price 150.00
            fixedPriceAdapter.enqueuePrice(Price.of("150.00"));

            sellPrecise(portfolioId, "AAPL", 12)
                    .statusCode(200)

                    // Then: sale response matches Gherkin expectations
                    .body("portfolioId", equalTo(portfolioId))
                    .body("ticker", equalTo("AAPL"))
                    .body("quantity", equalTo(12))
                    // proceeds  = 12 × 150.00 = 1800.00
                    .body("proceeds", comparesEqualTo(new BigDecimal("1800.00")))
                    // costBasis = (10 × 100.00) + (2 × 120.00) = 1240.00
                    .body("costBasis", comparesEqualTo(new BigDecimal("1240.00")))
                    // profit    = 1800.00 − 1240.00 = 560.00
                    .body("profit", comparesEqualTo(new BigDecimal("560.00")));

            // And: FIFO lot consumption verified — Lot #1 fully depleted, 3 remaining from Lot #2
            // (costBasis = 10×100 + 2×120 = 1240.00 proves FIFO order:
            //  all of Lot #1 consumed first, then 2 shares from Lot #2)
            getHoldings(portfolioId)
                    .body("find { it.ticker == 'AAPL' }.remaining", equalTo(3));
        }
    }
}
