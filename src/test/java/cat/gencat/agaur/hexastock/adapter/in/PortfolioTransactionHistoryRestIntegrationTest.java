package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for retrieving transaction history (US-08).
 *
 * <p>Verifies that the transaction history endpoint returns the expected
 * transaction records after portfolio operations (deposits, purchases, etc.).</p>
 */
@DisplayName("Transaction History Integration Tests (US-08)")
class PortfolioTransactionHistoryRestIntegrationTest extends AbstractPortfolioRestIntegrationTest {

    @Nested
    @DisplayName("When portfolio has transactions")
    class WhenPortfolioHasTransactions {

        String portfolioId;

        @BeforeEach
        void setUp() {
            portfolioId = createPortfolio("TransactionUser");
            deposit(portfolioId, 10_000);
            buy(portfolioId, "AAPL", 5);
        }

        @Test
        @DisplayName("GET transactions returns all transactions for the portfolio")
        @SpecificationRef(value = "US-08.AC-1", level = TestLevel.INTEGRATION, feature = "get-transaction-history.feature")
        void getTransactions_returnsAllTransactions() {
            getTransactions(portfolioId)
                    .body("size()", greaterThanOrEqualTo(2));
        }

        @Test
        @DisplayName("GET transactions with type parameter returns transactions")
        @SpecificationRef(value = "US-08.AC-2", level = TestLevel.INTEGRATION, feature = "get-transaction-history.feature")
        void getTransactions_withTypeParameter_returnsTransactions() {
            getTransactions(portfolioId, "PURCHASE")
                    .body("size()", greaterThanOrEqualTo(1));
        }
    }
}
