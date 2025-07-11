package cat.gencat.agaur.hexastock.adapter.out.rest;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import com.github.oscerd.finnhub.client.FinnhubClient;
import com.github.oscerd.finnhub.models.Quote;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Profile("finhub")
public class FinhubStockPriceAdapter implements StockPriceProviderPort {

    @Override
    public StockPrice fetchStockPrice(Ticker ticker) {

        FinnhubClient client = new FinnhubClient.Builder().token("d141qr9r01qs7glkje10d141qr9r01qs7glkje1g").build();

        Quote quote = null;
        try {
            quote = client.quote(ticker.value());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var currentPrice = quote.getC();

        return new StockPrice(ticker, currentPrice, LocalDateTime.now()
                .atZone(ZoneId.of("Europe/Madrid")) .toInstant(), "USD");
    }
}
