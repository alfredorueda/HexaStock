package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.model.ExternalApiException;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockRestController.class)
@DisplayName("StockRestController – WebMvc slice tests")
class StockRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetStockPriceUseCase getStockPriceUseCase;

    @Test
    @SpecificationRef(value = "US-10.AC-1", level = TestLevel.INTEGRATION,
            feature = "get-stock-price.feature")
    @DisplayName("GET /api/stocks/{symbol} returns 200 OK with stock price data")
    void returns200WithStockPrice() throws Exception {
        Instant now = Instant.parse("2025-06-15T10:00:00Z");
        StockPrice stockPrice = StockPrice.of(Ticker.of("AAPL"), Price.of(175.50), now);
        when(getStockPriceUseCase.getPrice(Ticker.of("AAPL"))).thenReturn(stockPrice);

        mockMvc.perform(get("/api/stocks/{symbol}", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(175.50))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("GET /api/stocks/{symbol} returns 503 when external API fails")
    void returns503WhenExternalApiFails() throws Exception {
        when(getStockPriceUseCase.getPrice(any()))
                .thenThrow(new ExternalApiException("Finnhub is down"));

        mockMvc.perform(get("/api/stocks/{symbol}", "AAPL"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("External API Error"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    @DisplayName("GET /api/stocks/{symbol} returns 400 for invalid ticker format")
    void returns400ForInvalidTicker() throws Exception {
        // Ticker.of("invalid") throws InvalidTickerException because it fails ^[A-Z]{1,5}$ regex
        mockMvc.perform(get("/api/stocks/{symbol}", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Ticker"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
