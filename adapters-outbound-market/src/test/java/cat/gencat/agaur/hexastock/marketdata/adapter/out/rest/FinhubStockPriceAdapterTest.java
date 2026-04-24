package cat.gencat.agaur.hexastock.marketdata.adapter.out.rest;

import cat.gencat.agaur.hexastock.model.ExternalApiException;
import cat.gencat.agaur.hexastock.marketdata.model.market.StockPrice;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
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
@DisplayName("FinhubStockPriceAdapter – WireMock integration tests")
class FinhubStockPriceAdapterTest {

    private static final Ticker AAPL = Ticker.of("AAPL");

    private FinhubStockPriceAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        adapter = new FinhubStockPriceAdapter();
        ReflectionTestUtils.setField(adapter, "finhubApiKey", "test-key");
        ReflectionTestUtils.setField(adapter, "finhubApiUrl", wmInfo.getHttpBaseUrl());
    }

    @Test
    @DisplayName("parses a valid Finnhub quote response")
    void validResponse(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlPathEqualTo("/quote"))
                .withQueryParam("symbol", equalTo("AAPL"))
                .withQueryParam("token", equalTo("test-key"))
                .willReturn(okJson("""
                        {"c":175.50,"d":2.5,"dp":1.44,"h":177.0,"l":174.0,"o":175.0,"pc":173.0}
                        """)));

        StockPrice result = adapter.fetchStockPrice(Ticker.of("AAPL"));

        assertThat(result.ticker()).isEqualTo(Ticker.of("AAPL"));
        assertThat(result.price().value().doubleValue()).isEqualTo(175.50);
        assertThat(result.time()).isNotNull();
    }

    @Test
    @DisplayName("throws ExternalApiException when price field 'c' is missing")
    void missingPriceField() {
        stubFor(get(urlPathEqualTo("/quote"))
                .willReturn(okJson("""
                        {"d":2.5,"dp":1.44}
                        """)));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    @DisplayName("throws ExternalApiException on server error")
    void throwsOnServerError() {
        stubFor(get(urlPathEqualTo("/quote"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    @DisplayName("throws ExternalApiException when response is null-like")
    void nullJsonBody() {
        stubFor(get(urlPathEqualTo("/quote"))
                .willReturn(okJson("null")));

        assertThatThrownBy(() -> adapter.fetchStockPrice(AAPL))
                .isInstanceOf(ExternalApiException.class);
    }
}
