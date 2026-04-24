package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.SellResult;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.Transaction;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PortfolioStockOperationsService}.
 *
 * <p>The three outgoing ports ({@link PortfolioPort}, {@link StockPriceProviderPort},
 * and {@link TransactionPort}) are mocked so no I/O takes place. Tests verify
 * correct orchestration: fetch portfolio, fetch price, execute domain operation,
 * persist, record transaction.</p>
 */
@DisplayName("PortfolioStockOperationsService")
class PortfolioStockOperationsServiceTest {

    private PortfolioPort portfolioPort;
    private StockPriceProviderPort stockPriceProviderPort;
    private TransactionPort transactionPort;
    private PortfolioStockOperationsService service;

    private static final Ticker AAPL = Ticker.of("AAPL");

    @BeforeEach
    void setUp() {
        portfolioPort = mock(PortfolioPort.class);
        stockPriceProviderPort = mock(StockPriceProviderPort.class);
        transactionPort = mock(TransactionPort.class);
        service = new PortfolioStockOperationsService(
                portfolioPort, stockPriceProviderPort, transactionPort);
    }

    // ── buyStock ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buyStock")
    class BuyStock {

        @Test
        @DisplayName("should buy stock, save portfolio, and record purchase transaction")
        @SpecificationRef(value = "US-06.AC-1", level = TestLevel.DOMAIN,
                feature = "buy-stocks.feature")
        void buysStockSuccessfully() {
            Portfolio portfolio = Portfolio.create("Alice");
            portfolio.deposit(Money.of("5000.00"));
            PortfolioId id = portfolio.getId();

            StockPrice stockPrice = new StockPrice(AAPL, Price.of("100.00"), Instant.now());
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(stockPriceProviderPort.fetchStockPrice(AAPL)).thenReturn(stockPrice);

            service.buyStock(id, AAPL, ShareQuantity.of(10));

            // Domain state mutated: balance reduced by 10 × $100.00
            assertEquals(Money.of("4000.00"), portfolio.getBalance());

            // Portfolio persisted and transaction recorded
            verify(portfolioPort).savePortfolio(portfolio);
            verify(transactionPort).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw PortfolioNotFoundException for unknown portfolio")
        @SpecificationRef(value = "US-06.AC-8", level = TestLevel.DOMAIN,
                feature = "buy-stocks.feature")
        void throwsWhenPortfolioNotFound() {
            PortfolioId id = PortfolioId.of("non-existent");
            ShareQuantity qty = ShareQuantity.of(5);
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.buyStock(id, AAPL, qty));

            verifyNoInteractions(stockPriceProviderPort);
            verifyNoInteractions(transactionPort);
        }

        @Test
        @DisplayName("should verify correct port call sequence: portfolio → price → save → transaction")
        void portCallSequence() {
            Portfolio portfolio = Portfolio.create("Alice");
            portfolio.deposit(Money.of("5000.00"));
            PortfolioId id = portfolio.getId();

            StockPrice stockPrice = new StockPrice(AAPL, Price.of("50.00"), Instant.now());
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(stockPriceProviderPort.fetchStockPrice(AAPL)).thenReturn(stockPrice);

            service.buyStock(id, AAPL, ShareQuantity.of(2));

            var inOrder = inOrder(portfolioPort, stockPriceProviderPort, transactionPort);
            inOrder.verify(portfolioPort).getPortfolioById(id);
            inOrder.verify(stockPriceProviderPort).fetchStockPrice(AAPL);
            inOrder.verify(portfolioPort).savePortfolio(portfolio);
            inOrder.verify(transactionPort).save(any(Transaction.class));
        }
    }

    // ── sellStock ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("sellStock")
    class SellStock {

        @Test
        @DisplayName("should sell stock, save portfolio, record sale, and return SellResult")
        @SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.DOMAIN,
                feature = "sell-stocks.feature")
        void sellsStockSuccessfully() {
            Portfolio portfolio = Portfolio.create("Bob");
            portfolio.deposit(Money.of("5000.00"));
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));
            PortfolioId id = portfolio.getId();

            StockPrice sellPrice = new StockPrice(AAPL, Price.of("120.00"), Instant.now());
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(stockPriceProviderPort.fetchStockPrice(AAPL)).thenReturn(sellPrice);

            SellResult result = service.sellStock(id, AAPL, ShareQuantity.of(5));

            assertNotNull(result);
            // 5 shares × $120 = $600 proceeds
            assertEquals(Money.of("600.00"), result.proceeds());
            // 5 shares × $100 = $500 cost basis
            assertEquals(Money.of("500.00"), result.costBasis());
            // $600 - $500 = $100 profit
            assertEquals(Money.of("100.00"), result.profit());

            verify(portfolioPort).savePortfolio(portfolio);
            verify(transactionPort).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw PortfolioNotFoundException for unknown portfolio")
        void throwsWhenPortfolioNotFound() {
            PortfolioId id = PortfolioId.of("non-existent");
            ShareQuantity qty = ShareQuantity.of(5);
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.sellStock(id, AAPL, qty));

            verifyNoInteractions(stockPriceProviderPort);
            verifyNoInteractions(transactionPort);
        }

        @Test
        @DisplayName("should verify correct port call sequence: portfolio → price → save → transaction")
        void portCallSequence() {
            Portfolio portfolio = Portfolio.create("Bob");
            portfolio.deposit(Money.of("5000.00"));
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));
            PortfolioId id = portfolio.getId();

            StockPrice sellPrice = new StockPrice(AAPL, Price.of("120.00"), Instant.now());
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));
            when(stockPriceProviderPort.fetchStockPrice(AAPL)).thenReturn(sellPrice);

            service.sellStock(id, AAPL, ShareQuantity.of(3));

            var inOrder = inOrder(portfolioPort, stockPriceProviderPort, transactionPort);
            inOrder.verify(portfolioPort).getPortfolioById(id);
            inOrder.verify(stockPriceProviderPort).fetchStockPrice(AAPL);
            inOrder.verify(portfolioPort).savePortfolio(portfolio);
            inOrder.verify(transactionPort).save(any(Transaction.class));
        }
    }
}
