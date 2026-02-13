package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.model.service.HoldingPerformanceCalculator;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportingService}.
 *
 * <p>This is an <strong>application-layer</strong> test: the three outgoing ports
 * ({@link PortfolioPort}, {@link TransactionPort}, {@link StockPriceProviderPort})
 * are mocked so that no I/O takes place.  The real
 * {@link HoldingPerformanceCalculator} is used because it is a pure domain
 * service with no side effects — using the real implementation gives higher
 * confidence than mocking it.</p>
 */
@DisplayName("ReportingService")
class ReportingServiceTest {

    // ── Mocks for the three outgoing ports ──────────────────────────────
    private PortfolioPort portfolioPort;
    private TransactionPort transactionPort;
    private StockPriceProviderPort stockPriceProviderPort;

    // ── Real domain service (no I/O, safe to use directly) ──────────────
    private HoldingPerformanceCalculator calculator;

    // ── System under test ───────────────────────────────────────────────
    private ReportingService reportingService;

    private static final Ticker AAPL = Ticker.of("AAPL");

    @BeforeEach
    void setUp() {
        portfolioPort = mock(PortfolioPort.class);
        transactionPort = mock(TransactionPort.class);
        stockPriceProviderPort = mock(StockPriceProviderPort.class);
        calculator = new HoldingPerformanceCalculator();

        reportingService = new ReportingService(
                transactionPort, stockPriceProviderPort,
                portfolioPort, calculator);
    }

    // ────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Portfolio not found")
    class PortfolioNotFound {

        @Test
        @DisplayName("should throw PortfolioNotFoundException for unknown ID")
        void unknownId() {
            var unknownId = "non-existent-id";
            when(portfolioPort.getPortfolioById(PortfolioId.of(unknownId)))
                    .thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> reportingService.getHoldingsPerfomance(unknownId));

            // Verify no further port calls were made
            verifyNoInteractions(transactionPort);
            verifyNoInteractions(stockPriceProviderPort);
        }
    }

    @Nested
    @DisplayName("Portfolio exists")
    class PortfolioExists {

        @Test
        @DisplayName("empty portfolio with no holdings — returns empty list")
        void emptyPortfolio() {
            var portfolio = Portfolio.create("Owner");
            var id = portfolio.getId();

            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(transactionPort.getTransactionsByPortfolioId(id)).thenReturn(List.of());
            when(stockPriceProviderPort.fetchStockPrice(Set.of())).thenReturn(Map.of());

            var result = reportingService.getHoldingsPerfomance(id.value());

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("portfolio with one holding — delegates to calculator correctly")
        void singleHolding() {
            var portfolio = Portfolio.create("Owner");
            portfolio.deposit(Money.of("5000.00"));
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));
            var id = portfolio.getId();

            var purchase = Transaction.createPurchase(id, AAPL,
                    ShareQuantity.of(10), Price.of("100.00"));
            var livePrice = new StockPrice(AAPL, Price.of("120.00"), Instant.now());

            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(transactionPort.getTransactionsByPortfolioId(id)).thenReturn(List.of(purchase));
            when(stockPriceProviderPort.fetchStockPrice(Set.of(AAPL)))
                    .thenReturn(Map.of(AAPL, livePrice));

            var result = reportingService.getHoldingsPerfomance(id.value());

            assertEquals(1, result.size());
            var h = result.getFirst();

            assertAll(
                    () -> assertEquals("AAPL", h.ticker()),
                    () -> assertEquals(new BigDecimal("10"), h.quantity()),
                    () -> assertEquals(new BigDecimal("10"), h.remaining()),
                    () -> assertEquals(new BigDecimal("200.00"), h.unrealizedGain()) // (120-100)*10
            );
        }

        @Test
        @DisplayName("verifies correct port call sequence")
        void portCallSequence() {
            var portfolio = Portfolio.create("Owner");
            portfolio.deposit(Money.of("5000.00"));
            portfolio.buy(AAPL, ShareQuantity.of(5), Price.of("50.00"));
            var id = portfolio.getId();

            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(transactionPort.getTransactionsByPortfolioId(id)).thenReturn(List.of());
            when(stockPriceProviderPort.fetchStockPrice(anySet())).thenReturn(Map.of());

            reportingService.getHoldingsPerfomance(id.value());

            // Verify ordering: portfolio first, then transactions, then prices
            var inOrder = inOrder(portfolioPort, transactionPort, stockPriceProviderPort);
            inOrder.verify(portfolioPort).getPortfolioById(id);
            inOrder.verify(transactionPort).getTransactionsByPortfolioId(id);
            inOrder.verify(stockPriceProviderPort).fetchStockPrice(anySet());
        }
    }
}
