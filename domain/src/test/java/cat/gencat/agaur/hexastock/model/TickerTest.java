package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.InvalidTickerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Ticker value object.
 * Tests focus on validating the ticker symbol format and immutability.
 */
@DisplayName("Ticker Value Object Tests")
class TickerTest {

    @Test
    @DisplayName("Should create a valid ticker")
    void shouldCreateValidTicker() {
        // When
        Ticker ticker = new Ticker("AAPL");
        
        // Then
        assertEquals("AAPL", ticker.value());
    }
    
    @Test
    @DisplayName("Should create ticker using factory method")
    void shouldCreateTickerUsingFactoryMethod() {
        // When
        Ticker ticker = Ticker.of("MSFT");
        
        // Then
        assertEquals("MSFT", ticker.value());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "MSFT", "GOOG", "AMZN", "TSLA"})
    @DisplayName("Should accept valid ticker symbols")
    void shouldAcceptValidTickerSymbols(String symbol) {
        // When
        Ticker ticker = new Ticker(symbol);
        
        // Then
        assertEquals(symbol, ticker.value());
    }
    
    @Test
    @DisplayName("Should throw exception when ticker is null")
    void shouldThrowExceptionWhenTickerIsNull() {
        // Then
        assertThrows(InvalidTickerException.class, () -> new Ticker(null));
    }
    
    @Test
    @DisplayName("Should throw exception when ticker is empty")
    void shouldThrowExceptionWhenTickerIsEmpty() {
        // Then
        assertThrows(InvalidTickerException.class, () -> new Ticker(""));
        assertThrows(InvalidTickerException.class, () -> new Ticker("  "));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"aapl", "MSFT1", "GO-OG", "a", "TOOLONG"})
    @DisplayName("Should throw exception for invalid ticker format")
    void shouldThrowExceptionForInvalidTickerFormat(String invalidSymbol) {
        // Then
        assertThrows(InvalidTickerException.class, () -> new Ticker(invalidSymbol));
    }
    
    @Test
    @DisplayName("Should maintain equality for same ticker")
    void shouldMaintainEqualityForSameTicker() {
        // Given
        Ticker ticker1 = new Ticker("AAPL");
        Ticker ticker2 = new Ticker("AAPL");
        
        // Then
        assertEquals(ticker1, ticker2);
        assertEquals(ticker1.hashCode(), ticker2.hashCode());
    }
    
    @Test
    @DisplayName("Should not equal different tickers")
    void shouldNotEqualDifferentTickers() {
        // Given
        Ticker appleTicker = new Ticker("AAPL");
        Ticker microsoftTicker = new Ticker("MSFT");
        
        // Then
        assertNotEquals(appleTicker, microsoftTicker);
    }
}