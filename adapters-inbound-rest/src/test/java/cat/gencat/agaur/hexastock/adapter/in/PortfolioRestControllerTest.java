package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.in.*;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.model.portfolio.*;
import cat.gencat.agaur.hexastock.model.transaction.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioRestController.class)
@DisplayName("PortfolioRestController – WebMvc slice tests")
class PortfolioRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioLifecycleUseCase portfolioLifecycleUseCase;
    @MockitoBean
    private CashManagementUseCase cashManagementUseCase;
    @MockitoBean
    private PortfolioStockOperationsUseCase portfolioStockOperationsUseCase;
    @MockitoBean
    private TransactionUseCase transactionUseCase;
    @MockitoBean
    private ReportingUseCase reportingUseCase;

    private static final String PORTFOLIO_ID = "p-1";
    private static final String OWNER = "Alice";
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    private Portfolio samplePortfolio() {
        return new Portfolio(PortfolioId.of(PORTFOLIO_ID), OWNER, Money.of(1000), NOW);
    }

    // ── Create Portfolio ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portfolios")
    class CreatePortfolio {

        @Test
        @SpecificationRef(value = "US-01.AC-1", level = TestLevel.INTEGRATION,
                feature = "create-portfolio.feature")
        @DisplayName("returns 201 Created with Location header and response body")
        void returns201WithLocationAndBody() throws Exception {
            Portfolio portfolio = new Portfolio(PortfolioId.of(PORTFOLIO_ID), OWNER, Money.ZERO, NOW);
            when(portfolioLifecycleUseCase.createPortfolio(OWNER)).thenReturn(portfolio);

            mockMvc.perform(post("/api/portfolios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerName\":\"Alice\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/portfolios/p-1")))
                    .andExpect(jsonPath("$.id").value(PORTFOLIO_ID))
                    .andExpect(jsonPath("$.ownerName").value(OWNER))
                    .andExpect(jsonPath("$.cashBalance").value(0.00))
                    .andExpect(jsonPath("$.currency").value("USD"));

            verify(portfolioLifecycleUseCase).createPortfolio(OWNER);
        }
    }

    // ── Get Portfolio ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolios/{id}")
    class GetPortfolio {

        @Test
        @SpecificationRef(value = "US-02.AC-1", level = TestLevel.INTEGRATION,
                feature = "get-portfolio.feature")
        @DisplayName("returns 200 OK with portfolio data")
        void returns200WithPortfolioData() throws Exception {
            when(portfolioLifecycleUseCase.getPortfolio(PortfolioId.of(PORTFOLIO_ID)))
                    .thenReturn(samplePortfolio());

            mockMvc.perform(get("/api/portfolios/{id}", PORTFOLIO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PORTFOLIO_ID))
                    .andExpect(jsonPath("$.ownerName").value(OWNER))
                    .andExpect(jsonPath("$.balance").value(1000.00));
        }

        @Test
        @SpecificationRef(value = "US-02.AC-2", level = TestLevel.INTEGRATION,
                feature = "get-portfolio.feature")
        @DisplayName("returns 404 when portfolio not found")
        void returns404WhenNotFound() throws Exception {
            when(portfolioLifecycleUseCase.getPortfolio(any()))
                    .thenThrow(new PortfolioNotFoundException("unknown"));

            mockMvc.perform(get("/api/portfolios/{id}", "unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Portfolio Not Found"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ── List Portfolios ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolios")
    class ListPortfolios {

        @Test
        @SpecificationRef(value = "US-03.AC-1", level = TestLevel.INTEGRATION,
                feature = "list-portfolios.feature")
        @DisplayName("returns 200 OK with list of portfolios")
        void returns200WithList() throws Exception {
            when(portfolioLifecycleUseCase.getAllPortfolios())
                    .thenReturn(List.of(samplePortfolio()));

            mockMvc.perform(get("/api/portfolios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(PORTFOLIO_ID));
        }

        @Test
        @SpecificationRef(value = "US-03.AC-2", level = TestLevel.INTEGRATION,
                feature = "list-portfolios.feature")
        @DisplayName("returns 200 OK with empty list when no portfolios exist")
        void returns200WithEmptyList() throws Exception {
            when(portfolioLifecycleUseCase.getAllPortfolios()).thenReturn(List.of());

            mockMvc.perform(get("/api/portfolios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── Deposit ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portfolios/{id}/deposits")
    class Deposit {

        @Test
        @SpecificationRef(value = "US-04.AC-1", level = TestLevel.INTEGRATION,
                feature = "deposit-funds.feature")
        @DisplayName("returns 200 OK on successful deposit")
        void returns200OnSuccess() throws Exception {
            mockMvc.perform(post("/api/portfolios/{id}/deposits", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":500.00}"))
                    .andExpect(status().isOk());

            verify(cashManagementUseCase).deposit(
                    PortfolioId.of(PORTFOLIO_ID),
                    Money.of(new BigDecimal("500.00")));
        }

        @Test
        @SpecificationRef(value = "US-04.AC-4", level = TestLevel.INTEGRATION,
                feature = "deposit-funds.feature")
        @DisplayName("returns 404 when portfolio not found")
        void returns404WhenNotFound() throws Exception {
            doThrow(new PortfolioNotFoundException(PORTFOLIO_ID))
                    .when(cashManagementUseCase).deposit(any(), any());

            mockMvc.perform(post("/api/portfolios/{id}/deposits", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":500.00}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Portfolio Not Found"));
        }
    }

    // ── Withdrawal ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portfolios/{id}/withdrawals")
    class Withdrawal {

        @Test
        @SpecificationRef(value = "US-05.AC-1", level = TestLevel.INTEGRATION,
                feature = "withdraw-funds.feature")
        @DisplayName("returns 200 OK on successful withdrawal")
        void returns200OnSuccess() throws Exception {
            mockMvc.perform(post("/api/portfolios/{id}/withdrawals", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":200.00}"))
                    .andExpect(status().isOk());

            verify(cashManagementUseCase).withdraw(
                    PortfolioId.of(PORTFOLIO_ID),
                    Money.of(new BigDecimal("200.00")));
        }

        @Test
        @SpecificationRef(value = "US-05.AC-6", level = TestLevel.INTEGRATION,
                feature = "withdraw-funds.feature")
        @DisplayName("returns 409 when insufficient funds")
        void returns409WhenInsufficientFunds() throws Exception {
            doThrow(new InsufficientFundsException("Not enough cash"))
                    .when(cashManagementUseCase).withdraw(any(), any());

            mockMvc.perform(post("/api/portfolios/{id}/withdrawals", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":9999.00}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Insufficient Funds"));
        }
    }

    // ── Buy Stock ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portfolios/{id}/purchases")
    class BuyStock {

        @Test
        @SpecificationRef(value = "US-06.AC-1", level = TestLevel.INTEGRATION,
                feature = "buy-stocks.feature")
        @DisplayName("returns 200 OK on successful purchase")
        void returns200OnSuccess() throws Exception {
            mockMvc.perform(post("/api/portfolios/{id}/purchases", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"AAPL\",\"quantity\":10}"))
                    .andExpect(status().isOk());

            verify(portfolioStockOperationsUseCase).buyStock(
                    PortfolioId.of(PORTFOLIO_ID),
                    Ticker.of("AAPL"),
                    ShareQuantity.positive(10));
        }

        @Test
        @SpecificationRef(value = "US-06.AC-8", level = TestLevel.INTEGRATION,
                feature = "buy-stocks.feature")
        @DisplayName("returns 409 when insufficient funds for purchase")
        void returns409WhenInsufficientFunds() throws Exception {
            doThrow(new InsufficientFundsException("Not enough cash for purchase"))
                    .when(portfolioStockOperationsUseCase).buyStock(any(), any(), any());

            mockMvc.perform(post("/api/portfolios/{id}/purchases", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"AAPL\",\"quantity\":10}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Insufficient Funds"));
        }
    }

    // ── Sell Stock ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portfolios/{id}/sales")
    class SellStock {

        @Test
        @SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.INTEGRATION,
                feature = "sell-stocks.feature")
        @DisplayName("returns 200 OK with sale response on successful sale")
        void returns200WithSaleResponse() throws Exception {
            SellResult sellResult = SellResult.of(Money.of(1500), Money.of(1000));
            when(portfolioStockOperationsUseCase.sellStock(any(), any(), any()))
                    .thenReturn(sellResult);

            mockMvc.perform(post("/api/portfolios/{id}/sales", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"AAPL\",\"quantity\":10}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.portfolioId").value(PORTFOLIO_ID))
                    .andExpect(jsonPath("$.ticker").value("AAPL"))
                    .andExpect(jsonPath("$.quantity").value(10))
                    .andExpect(jsonPath("$.proceeds").value(1500.00))
                    .andExpect(jsonPath("$.costBasis").value(1000.00))
                    .andExpect(jsonPath("$.profit").value(500.00));
        }

        @Test
        @DisplayName("returns 409 when selling more shares than owned")
        void returns409WhenConflictQuantity() throws Exception {
            doThrow(new ConflictQuantityException("Cannot sell more than owned"))
                    .when(portfolioStockOperationsUseCase).sellStock(any(), any(), any());

            mockMvc.perform(post("/api/portfolios/{id}/sales", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"AAPL\",\"quantity\":999}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflict Quantity"));
        }

        @Test
        @DisplayName("returns 404 when holding not found for sale")
        void returns404WhenHoldingNotFound() throws Exception {
            doThrow(new HoldingNotFoundException("AAPL"))
                    .when(portfolioStockOperationsUseCase).sellStock(any(), any(), any());

            mockMvc.perform(post("/api/portfolios/{id}/sales", PORTFOLIO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"AAPL\",\"quantity\":5}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Holding Not Found"));
        }
    }

    // ── Transaction History ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolios/{id}/transactions")
    class TransactionHistory {

        @Test
        @SpecificationRef(value = "US-08.AC-1", level = TestLevel.INTEGRATION,
                feature = "get-transaction-history.feature")
        @DisplayName("returns 200 OK with list of transactions")
        void returns200WithTransactions() throws Exception {
            DepositTransaction tx = new DepositTransaction(
                    TransactionId.of("tx-1"),
                    PortfolioId.of(PORTFOLIO_ID),
                    Money.of(500),
                    NOW);
            when(transactionUseCase.getTransactions(eq(PORTFOLIO_ID), any()))
                    .thenReturn(List.of(tx));

            mockMvc.perform(get("/api/portfolios/{id}/transactions", PORTFOLIO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value("tx-1"))
                    .andExpect(jsonPath("$[0].portfolioId").value(PORTFOLIO_ID))
                    .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                    .andExpect(jsonPath("$[0].totalAmount").value(500.00));
        }

        @Test
        @DisplayName("passes type filter parameter to use case")
        void passesTypeFilter() throws Exception {
            when(transactionUseCase.getTransactions(PORTFOLIO_ID, Optional.of("DEPOSIT")))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/portfolios/{id}/transactions", PORTFOLIO_ID)
                            .param("type", "DEPOSIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(transactionUseCase).getTransactions(PORTFOLIO_ID, Optional.of("DEPOSIT"));
        }
    }

    // ── Holdings Performance ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolios/{id}/holdings")
    class HoldingsPerformance {

        @Test
        @SpecificationRef(value = "US-09.AC-1", level = TestLevel.INTEGRATION,
                feature = "get-holdings-performance.feature")
        @DisplayName("returns 200 OK with holdings performance data")
        void returns200WithHoldings() throws Exception {
            HoldingPerformance hp = new HoldingPerformance(
                    "AAPL",
                    new BigDecimal("10"),
                    new BigDecimal("10"),
                    new BigDecimal("150.00"),
                    new BigDecimal("175.00"),
                    new BigDecimal("250.00"),
                    new BigDecimal("0.00"));
            when(reportingUseCase.getHoldingsPerformance(PORTFOLIO_ID))
                    .thenReturn(List.of(hp));

            mockMvc.perform(get("/api/portfolios/{id}/holdings", PORTFOLIO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                    .andExpect(jsonPath("$[0].currentPrice").value(175.00))
                    .andExpect(jsonPath("$[0].unrealizedGain").value(250.00));
        }
    }
}
