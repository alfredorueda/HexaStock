package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.Price;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for settlement-aware FIFO selling with fees.
 *
 * <p>Verifies the full round-trip: REST → Service → Domain → JPA → MySQL.
 * Uses a fixed-price stock adapter (100.00) so assertions are deterministic.</p>
 *
 * <p>Because lots persist with settlement_date = NOW + 2 days, newly-purchased lots
 * are unsettled. The settlement-sales endpoint should reject selling them.</p>
 */
@Import(SettlementSellIntegrationTest.FixedPriceConfig.class)
@DisplayName("Settlement-Aware Sell — Integration Tests")
class SettlementSellIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    private static final BigDecimal FIXED_PRICE = new BigDecimal("100.00");

    @TestConfiguration
    static class FixedPriceConfig {
        @Bean
        @Primary
        StockPriceProviderPort fixedPriceProvider() {
            return ticker -> new StockPrice(
                    ticker, Price.of(FIXED_PRICE),
                    Instant.now()
            );
        }
    }

    /** POST settlement-sale and return response. */
    ValidatableResponse sellWithSettlement(String portfolioId, String ticker, int quantity) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jsonTrade(ticker, quantity))
                .post("/api/portfolios/" + portfolioId + "/settlement-sales")
            .then();
    }

    @Nested
    @DisplayName("Settlement Gate via REST")
    class SettlementGateRest {

        String portfolioId;

        @BeforeEach
        void setup() {
            portfolioId = createPortfolio("SettlementUser");
            deposit(portfolioId, 100_000);
        }

        @Test
        @DisplayName("Newly bought lots are unsettled — settlement-sale should be rejected")
        @SpecificationRef(value = "SETTLE-REST-01", level = TestLevel.INTEGRATION)
        void sellUnsettledLots_returns409() {
            buy(portfolioId, "AAPL", 10);

            // Settlement-aware sell should fail because lots bought "just now" aren't settled
            sellWithSettlement(portfolioId, "AAPL", 5)
                    .statusCode(409)
                    .body("title", equalTo("Insufficient Eligible Shares"))
                    .body("status", equalTo(409));
        }

        @Test
        @DisplayName("Regular sell still works for unsettled lots (backward compatibility)")
        @SpecificationRef(value = "SETTLE-REST-02", level = TestLevel.INTEGRATION)
        void regularSellStillWorks() {
            buy(portfolioId, "AAPL", 10);

            // Regular sell endpoint doesn't check settlement
            sell(portfolioId, "AAPL", 5)
                    .statusCode(200)
                    .body("proceeds", greaterThan(0f));
        }
    }

    @Nested
    @DisplayName("Fee in Sale Response")
    class FeeInResponse {

        String portfolioId;

        @BeforeEach
        void setup() {
            portfolioId = createPortfolio("FeeUser");
            deposit(portfolioId, 100_000);
        }

        @Test
        @DisplayName("Regular sale response includes fee=0 for backward compatibility")
        @SpecificationRef(value = "FEE-REST-01", level = TestLevel.INTEGRATION)
        void regularSaleIncludesZeroFee() {
            buy(portfolioId, "AAPL", 10);

            sell(portfolioId, "AAPL", 5)
                    .statusCode(200)
                    .body("fee", equalTo(0.0f));
        }
    }

    @Nested
    @DisplayName("Persistence Round-Trip")
    class PersistenceRoundTrip {

        String portfolioId;

        @BeforeEach
        void setup() {
            portfolioId = createPortfolio("PersistUser");
            deposit(portfolioId, 100_000);
        }

        @Test
        @DisplayName("Holdings survive persistence round-trip with correct share counts")
        @SpecificationRef(value = "PERSIST-01", level = TestLevel.INTEGRATION)
        void holdingsSurviveRoundTrip() {
            buy(portfolioId, "AAPL", 10);
            buy(portfolioId, "AAPL", 5);

            // Verify holdings reflect both lots
            getHoldings(portfolioId)
                    .body("find { it.ticker == 'AAPL' }.remaining", equalTo(15));

            // Regular sell 3 shares — should work (no settlement check)
            sell(portfolioId, "AAPL", 3).statusCode(200);

            getHoldings(portfolioId)
                    .body("find { it.ticker == 'AAPL' }.remaining", equalTo(12));
        }

        @Test
        @DisplayName("Transaction records exist after regular sale")
        @SpecificationRef(value = "PERSIST-02", level = TestLevel.INTEGRATION)
        void transactionRecordsExistAfterRegularSale() {
            buy(portfolioId, "AAPL", 10);
            sell(portfolioId, "AAPL", 5).statusCode(200);

            // Verify SALE transaction exists
            getTransactions(portfolioId, "SALE")
                    .body("size()", greaterThanOrEqualTo(1));
        }
    }
}
