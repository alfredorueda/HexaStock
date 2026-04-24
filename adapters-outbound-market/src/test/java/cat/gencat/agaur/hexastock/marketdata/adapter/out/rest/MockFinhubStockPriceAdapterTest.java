package cat.gencat.agaur.hexastock.marketdata.adapter.out.rest;

import cat.gencat.agaur.hexastock.marketdata.model.market.StockPrice;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockFinhubStockPriceAdapter – unit tests")
class MockFinhubStockPriceAdapterTest {

    private final MockFinhubStockPriceAdapter adapter = new MockFinhubStockPriceAdapter();

    @Test
    @DisplayName("returns a StockPrice with the requested ticker")
    void returnsCorrectTicker() {
        StockPrice result = adapter.fetchStockPrice(Ticker.of("AAPL"));

        assertThat(result.ticker()).isEqualTo(Ticker.of("AAPL"));
        assertThat(result.time()).isNotNull();
    }

    @Test
    @DisplayName("returns a price within the expected random range [10, 1000]")
    void returnsPriceInRange() {
        StockPrice result = adapter.fetchStockPrice(Ticker.of("MSFT"));

        double price = result.price().value().doubleValue();
        assertThat(price).isBetween(10.0, 1000.0);
    }
}
