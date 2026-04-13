package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Price;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetStockPriceService}.
 *
 * <p>The outgoing port ({@link StockPriceProviderPort}) is mocked.
 * This service is a thin delegation layer; tests verify correct pass-through
 * and exception propagation.</p>
 */
@DisplayName("GetStockPriceService")
class GetStockPriceServiceTest {

    private StockPriceProviderPort stockPriceProviderPort;
    private GetStockPriceService service;

    private static final Ticker AAPL = Ticker.of("AAPL");

    @BeforeEach
    void setUp() {
        stockPriceProviderPort = mock(StockPriceProviderPort.class);
        service = new GetStockPriceService(stockPriceProviderPort);
    }

    @Test
    @DisplayName("should delegate to StockPriceProviderPort and return result")
    @SpecificationRef(value = "US-10.AC-1", level = TestLevel.DOMAIN,
            feature = "get-stock-price.feature")
    void delegatesToPortAndReturnsPrice() {
        StockPrice expected = new StockPrice(AAPL, Price.of("150.00"), Instant.now());
        when(stockPriceProviderPort.fetchStockPrice(AAPL)).thenReturn(expected);

        StockPrice result = service.getPrice(AAPL);

        assertSame(expected, result);
        verify(stockPriceProviderPort).fetchStockPrice(AAPL);
    }

    @Test
    @DisplayName("should propagate exception from port")
    void propagatesException() {
        when(stockPriceProviderPort.fetchStockPrice(AAPL))
                .thenThrow(new RuntimeException("Provider unavailable"));

        assertThrows(RuntimeException.class, () -> service.getPrice(AAPL));
    }
}
