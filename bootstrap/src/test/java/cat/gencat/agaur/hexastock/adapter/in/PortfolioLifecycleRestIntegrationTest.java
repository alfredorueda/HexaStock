package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
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
    class CreatePortfolio {

        @Test
        @SpecificationRef(value = "US-01.AC-1", level = TestLevel.INTEGRATION, feature = "create-portfolio.feature")
        void createPortfolio_returns201WithExpectedFields() {
            // The createPortfolio() helper already asserts 201, Location header,
            // and body fields (id, ownerName, cashBalance, currency)
            String id = createPortfolio("Alice");
            org.junit.jupiter.api.Assertions.assertNotNull(id);
        }
    }

    @Nested
    class WhenPortfolioExists {

        String portfolioId;

        @BeforeEach
        void createBasePortfolio() {
            portfolioId = createPortfolio("IntegrationUser");
        }

        @Test
        @SpecificationRef(value = "US-02.AC-1", level = TestLevel.INTEGRATION, feature = "get-portfolio.feature")
        void getPortfolio_returnsDtoWithBasicFields() {
            getPortfolio(portfolioId)
                    .body("id", equalTo(portfolioId))
                    .body("ownerName", equalTo("IntegrationUser"))
                    .body("balance", notNullValue())
                    .body("createdAt", notNullValue());
        }

        @Test
        @SpecificationRef(value = "US-09.AC-2", level = TestLevel.INTEGRATION, feature = "get-holdings-performance.feature")
        void getHoldings_emptyAfterCreation() {
            getHoldings(portfolioId)
                    .body("size()", equalTo(0));
        }

        @Test
        @SpecificationRef(value = "US-09.AC-1", level = TestLevel.INTEGRATION, feature = "get-holdings-performance.feature")
        void getHoldings_returnsPerformanceForPortfolioWithHoldings() {
            deposit(portfolioId, 50_000);
            buy(portfolioId, "AAPL", 10);

            getHoldings(portfolioId)
                    .body("size()", equalTo(1))
                    .body("[0].ticker", equalTo("AAPL"))
                    .body("[0].remaining", equalTo(10))
                    .body("[0].quantity", notNullValue())
                    .body("[0].averagePurchasePrice", notNullValue())
                    .body("[0].currentPrice", notNullValue())
                    .body("[0].unrealizedGain", notNullValue())
                    .body("[0].realizedGain", notNullValue());
        }

        @Nested
        class DepositsAndWithdrawals {

            @Test
            @SpecificationRef(value = "US-04.AC-1", level = TestLevel.INTEGRATION, feature = "deposit-funds.feature")
            void deposit_updatesBalance() {
                deposit(portfolioId, 5_000);

                getPortfolio(portfolioId)
                        .body("balance", equalTo(5_000.0f));
            }

            @Test
            @SpecificationRef(value = "US-05.AC-1", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
            void withdraw_updatesBalance() {
                deposit(portfolioId, 5_000);
                withdraw(portfolioId, 2_000);

                getPortfolio(portfolioId)
                        .body("balance", equalTo(3_000.0f));
            }

            @Test
            @SpecificationRef(value = "US-04.AC-2", level = TestLevel.INTEGRATION, feature = "deposit-funds.feature")
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
            @SpecificationRef(value = "US-05.AC-2", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
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
            @SpecificationRef(value = "US-04.AC-3", level = TestLevel.INTEGRATION, feature = "deposit-funds.feature")
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
            @SpecificationRef(value = "US-05.AC-3", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
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
            @SpecificationRef(value = "US-05.AC-4", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
            void withdrawMoreThanBalance_returns409() {
                deposit(portfolioId, 100);

                withdraw(portfolioId, 200, 409)
                        .body("title", equalTo("Insufficient Funds"))
                        .body("detail", containsString("Insufficient funds"))
                        .body("status", equalTo(409));
            }

            @Test
            @SpecificationRef(value = "US-05.AC-5", level = TestLevel.INTEGRATION, feature = "withdraw-funds.feature")
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
        @SpecificationRef(value = "US-03.AC-1", level = TestLevel.INTEGRATION, feature = "list-portfolios.feature")
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

        // US-03.AC-2: "Listing portfolios when none exist → 200 OK with empty array []"
        // This scenario cannot be reliably tested in the current shared-database integration
        // environment because other tests may create portfolios that persist across test
        // methods within the same Spring context. Proper isolation would require either
        // a @DirtiesContext strategy (expensive) or transactional rollback at the HTTP level
        // (not supported by RestAssured black-box tests). The behaviour IS verified indirectly:
        // the endpoint returns 200 with a JSON array in all tests, and the empty-list case
        // is a trivial serialization of an empty collection. See list-portfolios.feature for
        // the Gherkin specification of this scenario.
    }
}
