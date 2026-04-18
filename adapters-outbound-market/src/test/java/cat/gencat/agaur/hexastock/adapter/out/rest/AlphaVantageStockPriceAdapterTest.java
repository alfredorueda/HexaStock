package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.model.ExternalApiException;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
@DisplayName("AlphaVantageStockPriceAdapter – WireMock integration tests")
class AlphaVantageStockPriceAdapterTest {

    private static final Ticker AAPL = Ticker.of("AAPL");

    private AlphaVantageStockPriceAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        adapter = new AlphaVantageStockPriceAdapter();
        ReflectionTestUtils.setField(adapter, "apiKey", "test-key");
        ReflectionTestUtils.setField(adapter, "baseUrl", wmInfo.getHttpBaseUrl());
        ReflectionTestUtils.setField(adapter, "timeout", 5000);
    }

    @Test
    @DisplayName("parses a valid Alpha Vantage Global Quote response")
    void validResponse() {
        stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("function", equalTo("GLOBAL_QUOTE"))
                .withQueryParam("symbol", equalTo("AAPL"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(okJson("""
                        {
                          "Global Quote": {
                            "01. symbol": "AAPL",
                            "02. open": "175.00",
                            "05. price": "178.25",
                            "08. previous close": "174.50"
                          }
                        }
                        """)));

        StockPrice result = adapter.fetchStockPrice(Ticker.of("AAPL"));

        assertThat(result.ticker()).isEqualTo(Ticker.of("AAPL"));
        assertThat(result.price().value().doubleValue()).isEqualTo(178.25);
        assertThat(result.time()).isNotNull();
    }

    @Test
    @DisplayName("throws ExternalApiException when 'Global Quote' is missing")
    void missingGlobalQuote() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(okJson("""
                        {"Information": "Rate limit reached"}
                        """)));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    @DisplayName("throws ExternalApiException when '05. price' is missing")
    void missingPriceField() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(okJson("""
                        {"Global Quote": {"01. symbol": "AAPL"}}
                        """)));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    @DisplayName("throws ExternalApiException on server error")
    void throwsOnServerError() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    @DisplayName("throws ExternalApiException when price is not a number")
    void invalidPriceFormat() {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(okJson("""
                        {"Global Quote": {"05. price": "not-a-number"}}
                        """)));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }
}
