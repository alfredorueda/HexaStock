package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PortfolioLifecycleService}.
 *
 * <p>The outgoing port ({@link PortfolioPort}) is mocked so no I/O takes place.
 * Tests verify orchestration logic: correct delegation to the port and proper
 * exception handling.</p>
 */
@DisplayName("PortfolioLifecycleService")
class PortfolioLifecycleServiceTest {

    private PortfolioPort portfolioPort;
    private PortfolioLifecycleService service;

    @BeforeEach
    void setUp() {
        portfolioPort = mock(PortfolioPort.class);
        service = new PortfolioLifecycleService(portfolioPort);
    }

    // ── createPortfolio ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createPortfolio")
    class CreatePortfolio {

        @Test
        @DisplayName("should create portfolio and delegate to port")
        @SpecificationRef(value = "US-01.AC-1", level = TestLevel.DOMAIN,
                feature = "create-portfolio.feature")
        void createsAndPersists() {
            Portfolio result = service.createPortfolio("Alice");

            assertAll(
                    () -> assertNotNull(result),
                    () -> assertNotNull(result.getId()),
                    () -> assertEquals("Alice", result.getOwnerName())
            );
            verify(portfolioPort).createPortfolio(result);
        }
    }

    // ── getPortfolio ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPortfolio")
    class GetPortfolio {

        @Test
        @DisplayName("should return portfolio when found")
        @SpecificationRef(value = "US-02.AC-1", level = TestLevel.DOMAIN,
                feature = "get-portfolio.feature")
        void returnsExistingPortfolio() {
            Portfolio portfolio = Portfolio.create("Bob");
            PortfolioId id = portfolio.getId();

            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.of(portfolio));

            Portfolio result = service.getPortfolio(id);

            assertSame(portfolio, result);
            verify(portfolioPort).getPortfolioById(id);
        }

        @Test
        @DisplayName("should throw PortfolioNotFoundException when not found")
        @SpecificationRef(value = "US-02.AC-2", level = TestLevel.DOMAIN,
                feature = "get-portfolio.feature")
        void throwsWhenNotFound() {
            PortfolioId id = PortfolioId.of("non-existent-id");
            when(portfolioPort.getPortfolioById(id)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.getPortfolio(id));
        }
    }

    // ── getAllPortfolios ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllPortfolios")
    class GetAllPortfolios {

        @Test
        @DisplayName("should return all portfolios from port")
        @SpecificationRef(value = "US-03.AC-1", level = TestLevel.DOMAIN,
                feature = "list-portfolios.feature")
        void returnsAllPortfolios() {
            var p1 = Portfolio.create("Alice");
            var p2 = Portfolio.create("Bob");
            when(portfolioPort.getAllPortfolios()).thenReturn(List.of(p1, p2));

            List<Portfolio> result = service.getAllPortfolios();

            assertEquals(2, result.size());
            verify(portfolioPort).getAllPortfolios();
        }

        @Test
        @DisplayName("should return empty list when no portfolios exist")
        @SpecificationRef(value = "US-03.AC-2", level = TestLevel.DOMAIN,
                feature = "list-portfolios.feature")
        void returnsEmptyList() {
            when(portfolioPort.getAllPortfolios()).thenReturn(List.of());

            List<Portfolio> result = service.getAllPortfolios();

            assertTrue(result.isEmpty());
        }
    }
}
