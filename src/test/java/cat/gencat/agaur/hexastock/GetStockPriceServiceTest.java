package cat.gencat.agaur.hexastock;


import cat.gencat.agaur.hexastock.adapter.out.demo.MockStockPriceAdapter;
import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.service.GetStockPriceService;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for GetStockPriceUseCase implementation using MockStockPriceAdapter
 */
class GetStockPriceServiceTest {

    private GetStockPriceUseCase getStockPriceUseCase;
    private StockPriceProviderPort stockPriceProviderPort;

    @BeforeEach
    void setUp() {
        stockPriceProviderPort = new MockStockPriceAdapter();
        getStockPriceUseCase = new GetStockPriceService(stockPriceProviderPort);
    }

    @Test
    void shouldReturnStockPriceForValidSymbol() {
        // Given a valid stock symbol
        Ticker ticker = Ticker.of("AAPL");
        
        // When getStockPrice is called
        StockPrice stockPrice = getStockPriceUseCase.getPrice(ticker);
        
        // Then it should return a valid StockPrice object
        assertNotNull(stockPrice);
        assertEquals(ticker.value(), stockPrice.getTicker().value());
        assertTrue(stockPrice.getPrice() > 0);
        assertNotNull(stockPrice.getTime());
    }

    @Test
    void shouldThrowExceptionForInvalidSymbol() {
        // Given an invalid stock symbol
        Ticker ticker = Ticker.of("INVALID");
        
        // When getStockPrice is called
        // Then it should throw StockNotFoundException
        assertThrows(IllegalArgumentException.class, () -> {
            getStockPriceUseCase.getPrice(ticker);
        });
    }

    @Test
    void shouldThrowExceptionForEmptySymbol() {
        // Given an empty stock symbol
        Ticker ticker = Ticker.of("");
        
        // When getStockPrice is called
        // Then it should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            getStockPriceUseCase.getPrice(ticker);
        });
    }

    @Test
    void shouldThrowExceptionForNullSymbol() {
        // Given a null stock symbol
        Ticker ticker = Ticker.of(null);
        
        // When getStockPrice is called
        // Then it should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            getStockPriceUseCase.getPrice(ticker);
        });
    }
}