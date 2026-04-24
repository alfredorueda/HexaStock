package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.Transaction;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CashManagementService}.
 *
 * <p>The two outgoing ports ({@link PortfolioPort} and {@link TransactionPort})
 * are mocked so no I/O takes place. Tests verify the orchestration sequence:
 * fetch portfolio, mutate domain object, save, record transaction.</p>
 */
@DisplayName("CashManagementService")
class CashManagementServiceTest {

    private PortfolioPort portfolioPort;
    private TransactionPort transactionPort;
    private CashManagementService service;

    @BeforeEach
    void setUp() {
        portfolioPort = mock(PortfolioPort.class);
        transactionPort = mock(TransactionPort.class);
        service = new CashManagementService(portfolioPort, transactionPort);
    }

    // ── deposit ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("should deposit funds and record transaction")
        @SpecificationRef(value = "US-04.AC-1", level = TestLevel.DOMAIN,
                feature = "deposit-funds.feature")
        void depositsAndRecordsTransaction() {
            Portfolio portfolio = Portfolio.create("Alice");
            PortfolioId id = portfolio.getId();
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));

            Money amount = Money.of("500.00");
            service.deposit(id, amount);

            // Balance updated in domain
            assertEquals(new BigDecimal("500.00"), portfolio.getBalance().amount());

            // Portfolio saved
            verify(portfolioPort).savePortfolio(portfolio);

            // Transaction recorded
            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionPort).save(txCaptor.capture());
            Transaction recorded = txCaptor.getValue();
            assertNotNull(recorded);
        }

        @Test
        @DisplayName("should throw PortfolioNotFoundException for unknown portfolio")
        @SpecificationRef(value = "US-04.AC-4", level = TestLevel.DOMAIN,
                feature = "deposit-funds.feature")
        void throwsWhenPortfolioNotFound() {
            PortfolioId id = PortfolioId.of("non-existent");
            Money amount = Money.of("100.00");
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.deposit(id, amount));

            verifyNoInteractions(transactionPort);
        }

        @Test
        @DisplayName("should verify correct port call sequence")
        void portCallSequence() {
            Portfolio portfolio = Portfolio.create("Alice");
            PortfolioId id = portfolio.getId();
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));

            service.deposit(id, Money.of("100.00"));

            var inOrder = inOrder(portfolioPort, transactionPort);
            inOrder.verify(portfolioPort).getPortfolioById(id);
            inOrder.verify(portfolioPort).savePortfolio(portfolio);
            inOrder.verify(transactionPort).save(any(Transaction.class));
        }
    }

    // ── withdraw ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("should withdraw funds and record transaction")
        @SpecificationRef(value = "US-05.AC-1", level = TestLevel.DOMAIN,
                feature = "withdraw-funds.feature")
        void withdrawsAndRecordsTransaction() {
            Portfolio portfolio = Portfolio.create("Bob");
            portfolio.deposit(Money.of("1000.00"));
            PortfolioId id = portfolio.getId();
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));

            Money amount = Money.of("300.00");
            service.withdraw(id, amount);

            // Balance reduced
            assertEquals(new BigDecimal("700.00"), portfolio.getBalance().amount());

            // Portfolio saved and transaction recorded
            verify(portfolioPort).savePortfolio(portfolio);
            verify(transactionPort).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw PortfolioNotFoundException for unknown portfolio")
        @SpecificationRef(value = "US-05.AC-6", level = TestLevel.DOMAIN,
                feature = "withdraw-funds.feature")
        void throwsWhenPortfolioNotFound() {
            PortfolioId id = PortfolioId.of("non-existent");
            Money amount = Money.of("100.00");
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.withdraw(id, amount));

            verifyNoInteractions(transactionPort);
        }

        @Test
        @DisplayName("should verify correct port call sequence")
        void portCallSequence() {
            Portfolio portfolio = Portfolio.create("Bob");
            portfolio.deposit(Money.of("500.00"));
            PortfolioId id = portfolio.getId();
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));

            service.withdraw(id, Money.of("200.00"));

            var inOrder = inOrder(portfolioPort, transactionPort);
            inOrder.verify(portfolioPort).getPortfolioById(id);
            inOrder.verify(portfolioPort).savePortfolio(portfolio);
            inOrder.verify(transactionPort).save(any(Transaction.class));
        }
    }
}
