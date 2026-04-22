package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.MarketSentinelUseCase;
import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Map;
import java.util.Set;

public class MarketSentinelService implements MarketSentinelUseCase {

    private final WatchlistQueryPort queryPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final NotificationPort notificationPort;

    public MarketSentinelService(WatchlistQueryPort queryPort,
                                 StockPriceProviderPort stockPriceProviderPort,
                                 NotificationPort notificationPort) {
        this.queryPort = queryPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.notificationPort = notificationPort;
    }

    @Override
    public void detectBuySignals() {
        Set<Ticker> tickers = queryPort.findDistinctTickersInActiveWatchlists();
        if (tickers.isEmpty()) {
            return;
        }

        Map<Ticker, StockPrice> prices = stockPriceProviderPort.fetchStockPrice(tickers);

        for (Ticker ticker : tickers) {
            StockPrice stockPrice = prices.get(ticker);
            if (stockPrice == null) {
                continue;
            }
            Money currentPrice = stockPrice.price().toMoney();

            queryPort.findTriggeredAlerts(ticker, currentPrice)
                    .forEach(view -> notificationPort.notifyBuySignal(
                            BuySignal.from(
                                    view.ownerName(),
                                    view.listName(),
                                    view.telegramChatId(),
                                    ticker,
                                    view.thresholdPrice(),
                                    stockPrice
                            )
                    ));
        }
    }
}

