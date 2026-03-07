package cat.gencat.agaur.hexastock.adapter.in;

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
}
