package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for portfolio lifecycle operations:
 * creation, retrieval, deposits, withdrawals, and listing.
 */
class PortfolioLifecycleRestIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    @Nested
    class WhenPortfolioExists {

        String portfolioId;

        @BeforeEach
        void createBasePortfolio() {
            portfolioId = createPortfolio("IntegrationUser");
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
    }

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
