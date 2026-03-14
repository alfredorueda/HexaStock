package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;

/**
 * Base class for Portfolio REST integration tests.
 *
 * <p>Provides shared infrastructure (RestAssured setup, Testcontainers, Spring Boot random port)
 * and reusable helper methods for REST operations. Concrete test classes extend this base
 * and focus on specific use-case concerns: lifecycle, trading, or error handling.</p>
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
abstract class AbstractPortfolioRestIntegrationTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    // ── JSON body builders ──────────────────────────────────────────────

    String jsonCreatePortfolio(String ownerName) {
        return "{\"ownerName\": \"" + ownerName + "\"}";
    }

    String jsonAmount(int amount) {
        return "{\"amount\": " + amount + "}";
    }

    String jsonTrade(String ticker, int quantity) {
        return "{\"ticker\": \"" + ticker + "\", \"quantity\": " + quantity + "}";
    }

    // ── RestAssured helper methods ──────────────────────────────────────

    /** Creates a portfolio, asserts 201 + standard fields, returns the generated id. */
    String createPortfolio(String ownerName) {
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
    void deposit(String portfolioId, int amount) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonAmount(amount))
                .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
                .statusCode(200);
    }

    /** POST withdrawal and assert the given expected status. */
    ValidatableResponse withdraw(String portfolioId, int amount, int expectedStatus) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonAmount(amount))
                .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
                .statusCode(expectedStatus);
    }

    /** POST withdrawal and assert 200. */
    void withdraw(String portfolioId, int amount) {
        withdraw(portfolioId, amount, 200);
    }

    /** POST purchase and assert 200. */
    void buy(String portfolioId, String ticker, int quantity) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonTrade(ticker, quantity))
                .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
                .statusCode(200);
    }

    /** POST sale and return the ValidatableResponse so the caller can add extra assertions. */
    ValidatableResponse sell(String portfolioId, String ticker, int quantity) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonTrade(ticker, quantity))
                .post("/api/portfolios/" + portfolioId + "/sales")
            .then();
    }

    /** GET single portfolio and assert 200. */
    ValidatableResponse getPortfolio(String portfolioId) {
        return RestAssured.given()
                .get("/api/portfolios/" + portfolioId)
            .then()
                .statusCode(200);
    }

    /** GET holdings for a portfolio and assert 200. */
    ValidatableResponse getHoldings(String portfolioId) {
        return RestAssured.given()
                .get("/api/portfolios/" + portfolioId + "/holdings")
            .then()
                .statusCode(200);
    }

    /** GET all portfolios and assert 200. */
    ValidatableResponse getAllPortfolios() {
        return RestAssured.given()
                .get("/api/portfolios")
            .then()
                .statusCode(200);
    }

    /** GET transactions for a portfolio and assert 200. */
    ValidatableResponse getTransactions(String portfolioId) {
        return RestAssured.given()
                .get("/api/portfolios/" + portfolioId + "/transactions")
            .then()
                .statusCode(200);
    }

    /** GET transactions for a portfolio with type filter and assert 200. */
    ValidatableResponse getTransactions(String portfolioId, String type) {
        return RestAssured.given()
                .queryParam("type", type)
                .get("/api/portfolios/" + portfolioId + "/transactions")
            .then()
                .statusCode(200);
    }
}
