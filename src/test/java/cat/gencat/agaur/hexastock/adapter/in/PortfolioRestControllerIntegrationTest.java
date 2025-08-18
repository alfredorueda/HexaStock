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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "jpa", "finhub"})
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
            .body("{\"amount\": 10000}")
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
            .statusCode(409);
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
            .statusCode(400);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": -5}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(400);
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
            .statusCode(400);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": -3}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(400);
    }

    @Test
    void error_operateOnNonExistentPortfolio() {
        String fakePortfolioId = "non-existent-id";
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + fakePortfolioId + "/purchases")
            .then()
            .statusCode(404);
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + fakePortfolioId + "/sales")
            .then()
            .statusCode(404);
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
            .statusCode(400);
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
            .statusCode(400);
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
            .statusCode(409);
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
}
