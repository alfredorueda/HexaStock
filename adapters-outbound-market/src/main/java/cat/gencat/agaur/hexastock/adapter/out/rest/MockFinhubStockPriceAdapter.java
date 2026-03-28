package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * MockFinhubStockPriceAdapter provides random but reasonable stock prices for testing purposes.
 * It implements StockPriceProviderPort and is activated with the 'mockfinhub' Spring profile.
 */
@Component
@Profile("mockfinhub")
public class MockFinhubStockPriceAdapter implements StockPriceProviderPort {
    private final SecureRandom random = new SecureRandom();

    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {
        // Generate a random price between $10 and $1000
        double priceValue = 10.0 + (990.0 * random.nextDouble());
        return new StockPrice(
            ticker,
            Price.of(priceValue),
            Instant.now()
        );
    }
}
