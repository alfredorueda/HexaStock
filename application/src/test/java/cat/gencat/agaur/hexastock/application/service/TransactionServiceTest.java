package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.transaction.Transaction;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionService}.
 *
 * <p>The outgoing port ({@link TransactionPort}) is mocked.
 * Tests verify delegation and filtering behaviour.</p>
 */
@DisplayName("TransactionService")
class TransactionServiceTest {

    private TransactionPort transactionPort;
    private TransactionService service;

    private static final PortfolioId PORTFOLIO_ID = PortfolioId.of("portfolio-1");
    private static final Ticker AAPL = Ticker.of("AAPL");

    @BeforeEach
    void setUp() {
        transactionPort = mock(TransactionPort.class);
        service = new TransactionService(transactionPort);
    }

    @Test
    @DisplayName("should return all transactions for a portfolio")
    @SpecificationRef(value = "US-08.AC-1", level = TestLevel.DOMAIN,
            feature = "get-transaction-history.feature")
    void returnsAllTransactions() {
        var deposit = Transaction.createDeposit(PORTFOLIO_ID, Money.of("1000.00"));
        var purchase = Transaction.createPurchase(PORTFOLIO_ID, AAPL,
                ShareQuantity.of(10), Price.of("100.00"));

        when(transactionPort.getTransactionsByPortfolioId(PORTFOLIO_ID))
                .thenReturn(List.of(deposit, purchase));

        List<Transaction> result = service.getTransactions(PORTFOLIO_ID.value(), Optional.empty());

        assertEquals(2, result.size());
        verify(transactionPort).getTransactionsByPortfolioId(PORTFOLIO_ID);
    }

    @Test
    @DisplayName("should return empty list when no transactions exist")
    void returnsEmptyList() {
        when(transactionPort.getTransactionsByPortfolioId(PORTFOLIO_ID))
                .thenReturn(List.of());

        List<Transaction> result = service.getTransactions(PORTFOLIO_ID.value(), Optional.empty());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should delegate with correct PortfolioId conversion")
    void delegatesWithCorrectId() {
        when(transactionPort.getTransactionsByPortfolioId(PORTFOLIO_ID))
                .thenReturn(List.of());

        service.getTransactions("portfolio-1", Optional.empty());

        verify(transactionPort).getTransactionsByPortfolioId(PORTFOLIO_ID);
    }
}
