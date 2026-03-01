package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.Ticker;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.Set;

@Transactional
public class MarketSentinelService {

    private final WatchlistQueryPort watchlistQueryPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final NotificationPort notificationPort;

    public MarketSentinelService(WatchlistQueryPort watchlistQueryPort,
                                 StockPriceProviderPort stockPriceProviderPort,
                                 NotificationPort notificationPort) {
        this.watchlistQueryPort = watchlistQueryPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.notificationPort = notificationPort;
    }

    public void detectBuySignals() {
        Set<Ticker> tickers = watchlistQueryPort.findDistinctTickersInActiveWatchlists();
        if (tickers.isEmpty()) {
            return;
        }

        Map<Ticker, cat.gencat.agaur.hexastock.model.StockPrice> prices = stockPriceProviderPort.fetchStockPrice(tickers);

        for (Ticker ticker : tickers) {
            var stockPrice = prices.get(ticker);
            if (stockPrice == null) {
                continue;
            }
            var currentPrice = stockPrice.price().toMoney();

            watchlistQueryPort.findTriggeredAlerts(ticker, currentPrice)
                    .forEach(view -> notificationPort.notifyBuySignal(
                            new BuySignal(
                                    view.ownerName(),
                                    view.listName(),
                                    view.ticker(),
                                    view.thresholdPrice(),
                                    stockPrice
                            )
                    ));
        }
    }
}
