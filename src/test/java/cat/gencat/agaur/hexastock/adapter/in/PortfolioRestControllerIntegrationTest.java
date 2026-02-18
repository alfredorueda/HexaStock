package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;

/**
 * Integration Test for Portfolio REST API.
 *
 * <p>Uses the <b>mockfinhub</b> profile (mocked stock-price adapter returning random yet reasonable prices)
 * so contributors can run the full suite without a real Finnhub API key.
 * <b>Testcontainers</b> automatically starts a MySQL instance when Docker is available.</p>
 *
 * <pre>
 * # Quick start
 * docker info          # ensure Docker is running
 * ./mvnw clean verify  # runs all tests including this one
 * </pre>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "jpa", "mockfinhub"})
class PortfolioRestControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    // ── JSON body builders ──────────────────────────────────────────────

    private String jsonCreatePortfolio(String ownerName) {
        return "{\"ownerName\": \"" + ownerName + "\"}";
    }

    private String jsonAmount(int amount) {
        return "{\"amount\": " + amount + "}";
    }

    private String jsonTrade(String ticker, int quantity) {
        return "{\"ticker\": \"" + ticker + "\", \"quantity\": " + quantity + "}";
    }

    // ── RestAssured helper methods ──────────────────────────────────────

    /** Creates a portfolio, asserts 201 + standard fields, returns the generated id. */
    private String createPortfolio(String ownerName) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonCreatePortfolio(ownerName))
                .post("/api/portfolios")
            .then()
                .statusCode(201)
                .header("Location", containsString("/api/portfolios/"))
                .body("id", notNullValue())
                .body("ownerName", equalTo(ownerName))
                .body("cashBalance", equalTo(0.0f))
                .body("currency", equalTo("USD"))
                .extract().path("id");
    }

    /** POST deposit and assert 200. */
    private void deposit(String portfolioId, int amount) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonAmount(amount))
                .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
                .statusCode(200);
    }

    /** POST withdrawal and assert the given expected status. */
    private ValidatableResponse withdraw(String portfolioId, int amount, int expectedStatus) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonAmount(amount))
                .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
                .statusCode(expectedStatus);
    }

    /** POST withdrawal and assert 200. */
    private void withdraw(String portfolioId, int amount) {
        withdraw(portfolioId, amount, 200);
    }

    /** POST purchase and assert 200. */
    private void buy(String portfolioId, String ticker, int quantity) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonTrade(ticker, quantity))
                .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
                .statusCode(200);
    }

    /** POST sale and return the ValidatableResponse so the caller can add extra assertions. */
    private ValidatableResponse sell(String portfolioId, String ticker, int quantity) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonTrade(ticker, quantity))
                .post("/api/portfolios/" + portfolioId + "/sales")
            .then();
    }

    /** GET single portfolio and assert 200. */
    private ValidatableResponse getPortfolio(String portfolioId) {
        return RestAssured.given()
                .get("/api/portfolios/" + portfolioId)
            .then()
                .statusCode(200);
    }

    /** GET holdings for a portfolio and assert 200. */
    private ValidatableResponse getHoldings(String portfolioId) {
        return RestAssured.given()
                .get("/api/portfolios/" + portfolioId + "/holdings")
            .then()
                .statusCode(200);
    }

    /** GET all portfolios and assert 200. */
    private ValidatableResponse getAllPortfolios() {
        return RestAssured.given()
                .get("/api/portfolios")
            .then()
                .statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Nested test groups
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class WhenPortfolioExists {

        String portfolioId;

        @BeforeEach
        void createBasePortfolio() {
            portfolioId = createPortfolio("IntegrationUser");
        }

        // ── Happy-path scenarios ────────────────────────────────────

        @Nested
        class HappyPath {

            @Test
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

            @Test
            void getPortfolio_returnsDtoWithBasicFields() {
                getPortfolio(portfolioId)
                        .body("id", equalTo(portfolioId))
                        .body("ownerName", equalTo("IntegrationUser"))
                        .body("balance", notNullValue())
                        .body("createdAt", notNullValue());
            }

            @Test
            void getHoldings_emptyAfterCreation() {
                getHoldings(portfolioId)
                        .body("size()", equalTo(0));
            }
        }

        // ── Deposits & Withdrawals ─────────────────────────────────

        @Nested
        class DepositsAndWithdrawals {

            @Test
            void deposit_updatesBalance() {
                deposit(portfolioId, 5_000);

                getPortfolio(portfolioId)
                        .body("balance", equalTo(5_000.0f));
            }

            @Test
            void withdraw_updatesBalance() {
                deposit(portfolioId, 5_000);
                withdraw(portfolioId, 2_000);

                getPortfolio(portfolioId)
                        .body("balance", equalTo(3_000.0f));
            }

            @Test
            void depositZeroAmount_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonAmount(0))
                        .post("/api/portfolios/" + portfolioId + "/deposits")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Amount"))
                        .body("status", equalTo(400))
                        .body("detail", containsString("amount"));
            }

            @Test
            void withdrawZeroAmount_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonAmount(0))
                        .post("/api/portfolios/" + portfolioId + "/withdrawals")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Amount"))
                        .body("status", equalTo(400))
                        .body("detail", containsString("amount"));
            }

            @Test
            void depositNegativeAmount_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonAmount(-100))
                        .post("/api/portfolios/" + portfolioId + "/deposits")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Amount"))
                        .body("status", equalTo(400))
                        .body("detail", containsString("amount"));
            }

            @Test
            void withdrawNegativeAmount_returns400() {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(jsonAmount(-50))
                        .post("/api/portfolios/" + portfolioId + "/withdrawals")
                    .then()
                        .statusCode(400)
                        .body("title", equalTo("Invalid Amount"))
                        .body("status", equalTo(400))
                        .body("detail", containsString("amount"));
            }

            @Test
            void withdrawMoreThanBalance_returns409() {
                deposit(portfolioId, 100);

                withdraw(portfolioId, 200, 409)
                        .body("title", equalTo("Insufficient Funds"))
                        .body("detail", containsString("Insufficient funds"))
                        .body("status", equalTo(409));
            }

            @Test
            void withdrawFromZeroBalance_returns409() {
                withdraw(portfolioId, 1, 409)
                        .body("title", equalTo("Insufficient Funds"))
                        .body("detail", containsString("Insufficient funds"))
                        .body("status", equalTo(409));
            }
        }

        // ── Buying shares ──────────────────────────────────────────

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

        // ── Selling shares ─────────────────────────────────────────

        @Nested
        class SellingShares {

            @BeforeEach
            void fundAndBuy() {
                deposit(portfolioId, 10_000);
                buy(portfolioId, "AAPL", 5);
            }

            @Test
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
            void sellMoreThanOwned_returns409() {
                sell(portfolioId, "AAPL", 10)
                        .statusCode(409)
                        .body("title", equalTo("Conflict Quantity"))
                        .body("detail", containsString("Not enough shares to sell"))
                        .body("status", equalTo(409));
            }

            @Test
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
            void sellTickerNotOwned_returns404() {
                sell(portfolioId, "MSFT", 1)
                        .statusCode(404)
                        .body("title", equalTo("Holding Not Found"))
                        .body("status", equalTo(404));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Operations on non-existent portfolios
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class WhenPortfolioDoesNotExist {

        private static final String FAKE_ID = "non-existent-id";

        @Test
        void buyOnNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(jsonTrade("AAPL", 5))
                    .post("/api/portfolios/" + FAKE_ID + "/purchases")
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }

        @Test
        void sellOnNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(jsonTrade("AAPL", 5))
                    .post("/api/portfolios/" + FAKE_ID + "/sales")
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }

        @Test
        void depositToNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(jsonAmount(100))
                    .post("/api/portfolios/" + FAKE_ID + "/deposits")
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }

        @Test
        void withdrawFromNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(jsonAmount(100))
                    .post("/api/portfolios/" + FAKE_ID + "/withdrawals")
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }

        @Test
        void getNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .get("/api/portfolios/" + FAKE_ID)
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Listing all portfolios
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ListAllPortfolios {

        @Test
        void returnsAllCreatedPortfoliosWithCorrectBalances() {
            String id1 = createPortfolio("Alice");
            String id2 = createPortfolio("Bob");
            String id3 = createPortfolio("Charlie");

            deposit(id1, 1_000);
            deposit(id2, 2_500);
            // id3 has no deposit

            getAllPortfolios()
                    .body("find { it.id == '" + id1 + "' }.balance", equalTo(1_000.0f))
                    .body("find { it.id == '" + id2 + "' }.balance", equalTo(2_500.0f))
                    .body("find { it.id == '" + id3 + "' }.balance", equalTo(0.0f));
        }
    }
}
