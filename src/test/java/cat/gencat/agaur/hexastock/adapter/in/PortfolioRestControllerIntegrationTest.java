package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration Test for Portfolio REST API.
 *
 * <b>Overview</b>
 * By default, this project uses a mocked finhub adapter (same interface as the real one) that returns random yet reasonable stock prices.
 * This allows contributors to clone the repository and run tests without configuring a real finhub API key.
 * Testcontainers automatically starts a MySQL database if Docker is available and running.
 *
 * <b>Quick Start (CLI)</b>
 * To get started, follow these steps:
 *
 * <pre>
 * # 1) Clone the repository
 * git clone https://github.com/<org>/<repo>.git
 * cd <repo>
 *
 * # 2) Ensure Java 21+ is available
 * java -version
 * # (If needed, set JAVA_HOME to a JDK 21+)
 *
 * # 3) Ensure Docker is running (required by Testcontainers)
 * #   - Docker Desktop (macOS/Windows) or Docker Engine (Linux)
 * #   - Verify with:
 * docker info
 *
 * # 4) Run the test suite (uses the mocked finhub adapter by default)
 * ./mvnw clean verify
 * # (or to run just tests)
 * ./mvnw test
 * </pre>
 *
 * <b>Active Profiles and Switching to the Real finhub Adapter</b>
 * - By default, the mocked finhub adapter (profile: mockfinhub) is used for tests and local development.
 * - To use the real finhub adapter, switch the active Spring profile and provide a valid API key.
 *
 * <pre>
 * # application.properties (or application.yml):
 * finhub.api-key=<your-api-key>
 * </pre>
 *
 * <b>Why a Mocked Adapter?</b>
 * - Ensures a smooth out-of-the-box experience: no external credentials required to run tests.
 * - Testcontainers handles the database lifecycle automatically.
 * - Keeps CI stable and fast while still exercising the application’s ports/adapters and domain logic.
 *
 * <b>Troubleshooting</b>
 * - If tests fail with messages from Testcontainers, confirm Docker is installed and running.
 * - Verify Java 21+ is on PATH.
 * - If switching to the real adapter, ensure the API key property is correctly set and the finhub profile is active.
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

    @Test
    void happyPath_endToEnd() {
        // 1. Create portfolio
        String ownerName = "TestUser";
        ValidatableResponse createResp = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
        String portfolioId = createResp.extract().path("id");

        // 2. Deposit
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 100000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);

        // 3. Buy shares
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 10}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(200);

        // 4. Sell shares
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(200)
            .body("portfolioId", equalTo(portfolioId))
            .body("ticker", equalTo("AAPL"))
            .body("quantity", equalTo(5))
            .body("proceeds", greaterThan(0f));

        // 5. Withdraw
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 1000}")
            .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
            .statusCode(200);
    }

    @Test
    void happyPath_multipleBuysAndSells() {
        String ownerName = "MultiBuyUser";
        ValidatableResponse createResp = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
        String portfolioId = createResp.extract().path("id");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 50000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);

        String[] tickers = {"AAPL", "MSFT", "GOOG"};
        int[] buyQuantities = {5, 7};

        for (String ticker : tickers) {
            // Buy shares twice for each company
            int totalBought = 0;
            for (int qty : buyQuantities) {
                RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("{\"ticker\": \"" + ticker + "\", \"quantity\": " + qty + "}")
                    .post("/api/portfolios/" + portfolioId + "/purchases")
                    .then()
                    .statusCode(200);
                totalBought += qty;
            }
            // Sell a smaller quantity than purchased
            int sellQty = totalBought - 3;
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"ticker\": \"" + ticker + "\", \"quantity\": " + sellQty + "}")
                .post("/api/portfolios/" + portfolioId + "/sales")
                .then()
                .statusCode(200);
            // Check remaining shares in portfolio
            int remaining = RestAssured.given()
                .get("/api/portfolios/" + portfolioId + "/holdings")
                .then()
                .statusCode(200)
                .extract()
                .path("find { it.ticker == '" + ticker + "' }.remaining");
            assertEquals(3, remaining);
        }
    }

    @Test
    void error_buyWithInsufficientFunds() {
        String ownerName = "ErrorFundsUser";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        // No deposit, try to buy
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 10}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(409)
            .body("title", equalTo("Insufficient Funds"))
            .body("detail", containsString("Insufficient funds"))
            .body("status", equalTo(409));
    }

    @Test
    void error_sellMoreThanOwned() {
        String ownerName = "ErrorSellUser";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 10000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 2}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(200);
        // Try to sell more than owned
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 10}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(409);
    }

    @Test
    void error_buyWithInvalidQuantity() {
        String ownerName = "ErrorInvalidQtyUser";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"amount\": 10000}")
                .post("/api/portfolios/" + portfolioId + "/deposits")
                .then()
                .statusCode(200);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 0}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Quantity"))
            .body("detail", containsString("Quantity must be positive"))
            .body("status", equalTo(400));
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": -5}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Quantity"))
            .body("detail", containsString("Quantity must be positive"))
            .body("status", equalTo(400));
    }

    @Test
    void error_sellWithInvalidQuantity() {
        String ownerName = "ErrorInvalidSellQtyUser";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 10000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(200);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 0}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Quantity"))
            .body("detail", containsString("Quantity must be positive"))
            .body("status", equalTo(400));

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": -3}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Quantity"))
            .body("detail", containsString("Quantity must be positive"))
            .body("status", equalTo(400));
    }

    @Test
    void error_operateOnNonExistentPortfolio() {
        String fakePortfolioId = "non-existent-id";
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + fakePortfolioId + "/purchases")
            .then()
            .statusCode(404)
            .body("title", equalTo("Portfolio Not Found"))
            .body("detail", containsString(fakePortfolioId))
            .body("status", equalTo(404));
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + fakePortfolioId + "/sales")
            .then()
            .statusCode(404)
            .body("title", equalTo("Portfolio Not Found"))
            .body("detail", containsString(fakePortfolioId))
            .body("status", equalTo(404));
    }

    @Test
    void error_depositNegativeAmount() {
        String ownerName = "ErrorDepositNegative";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": -100}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Amount"))
            .body("detail", containsString("amount"))
            .body("status", equalTo(400));
    }

    @Test
    void error_withdrawNegativeAmount() {
        String ownerName = "ErrorWithdrawNegative";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": -50}")
            .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Amount"))
            .body("detail", containsString("amount"))
            .body("status", equalTo(400));
    }

    @Test
    void error_withdrawMoreThanBalance() {
        String ownerName = "ErrorWithdrawExcess";
        String portfolioId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .extract().path("id");
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 100}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 200}")
            .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
            .statusCode(409)
            .body("title", equalTo("Insufficient Funds"))
            .body("detail", containsString("Insufficient funds"))
            .body("status", equalTo(409));
    }

    @Test
    void error_depositToNonExistentPortfolio() {
        String fakePortfolioId = "non-existent-id";
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 100}")
            .post("/api/portfolios/" + fakePortfolioId + "/deposits")
            .then()
            .statusCode(404);
    }

    @Test
    void getPortfolio_returnsDtoWithBasicFields() {
        String ownerName = "TestUser";
        ValidatableResponse createResp = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
        String portfolioId = createResp.extract().path("id");

        RestAssured.given()
            .get("/api/portfolios/" + portfolioId)
            .then()
            .statusCode(200)
            .body("id", equalTo(portfolioId))
            .body("ownerName", equalTo(ownerName))
            .body("balance", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    void error_buyWithInvalidTicker() {
        String ownerName = "ErrorInvalidTickerUser";
        ValidatableResponse createResp = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
        String portfolioId = createResp.extract().path("id");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 10000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);

        // Attempt to buy with an invalid ticker
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"ZZZZ_INVALID\", \"quantity\": 5}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Ticker"))
            .body("detail", containsString("ZZZZ_INVALID"))
            .body("status", equalTo(400));

        // Assert no holdings were created
        RestAssured.given()
            .get("/api/portfolios/" + portfolioId + "/holdings")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }
}
