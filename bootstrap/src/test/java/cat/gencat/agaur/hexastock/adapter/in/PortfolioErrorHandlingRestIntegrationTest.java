package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for error handling across all portfolio operations:
 * operations on non-existent portfolios (404) and related edge cases.
 */
class PortfolioErrorHandlingRestIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    @Nested
    class WhenPortfolioDoesNotExist {

        private static final String FAKE_ID = "non-existent-id";

        @Test
        @SpecificationRef(value = "US-06.AC-8", level = TestLevel.INTEGRATION, feature = "buy-stocks.feature")
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

        // Traceability: US-07.AC-7 = sell on non-existent portfolio → 404
        @Test
        @SpecificationRef(value = "US-07.AC-7", level = TestLevel.INTEGRATION)
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
        @SpecificationRef(value = "US-04.AC-4", level = TestLevel.INTEGRATION, feature = "deposit-funds.feature")
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
        @SpecificationRef(value = "US-05.AC-6", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
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
        @SpecificationRef(value = "US-02.AC-2", level = TestLevel.INTEGRATION, feature = "get-portfolio.feature")
        void getNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .get("/api/portfolios/" + FAKE_ID)
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }

        @Test
        @SpecificationRef(value = "US-09.AC-3", level = TestLevel.INTEGRATION, feature = "get-holdings-performance.feature")
        void getHoldingsOnNonExistentPortfolio_returns404() {
            RestAssured.given()
                    .get("/api/portfolios/" + FAKE_ID + "/holdings")
                .then()
                    .statusCode(404)
                    .body("title", equalTo("Portfolio Not Found"))
                    .body("detail", containsString(FAKE_ID))
                    .body("status", equalTo(404));
        }
    }
}
