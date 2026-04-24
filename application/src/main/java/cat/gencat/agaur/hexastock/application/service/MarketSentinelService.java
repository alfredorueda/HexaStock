package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.MarketSentinelUseCase;
import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import cat.gencat.agaur.hexastock.marketdata.application.port.out.MarketDataPort;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.marketdata.model.market.StockPrice;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent.AlertType;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Detects buy signals on the CQRS read side and publishes a
 * {@link WatchlistAlertTriggeredEvent} for each triggered alert.
 *
 * <p>This service has zero knowledge of how those events are turned into user-facing
 * notifications: that is the responsibility of the Notifications module. This is the
 * core decoupling enabled by Spring Modulith and in-process domain events.</p>
 */
public class MarketSentinelService implements MarketSentinelUseCase {

    private final WatchlistQueryPort queryPort;
    private final MarketDataPort stockPriceProviderPort;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public MarketSentinelService(WatchlistQueryPort queryPort,
                                 MarketDataPort stockPriceProviderPort,
                                 DomainEventPublisher eventPublisher) {
        this(queryPort, stockPriceProviderPort, eventPublisher, Clock.systemUTC());
    }

    public MarketSentinelService(WatchlistQueryPort queryPort,
                                 MarketDataPort stockPriceProviderPort,
                                 DomainEventPublisher eventPublisher,
                                 Clock clock) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
        this.stockPriceProviderPort = Objects.requireNonNull(stockPriceProviderPort, "stockPriceProviderPort must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void detectBuySignals() {
        Set<Ticker> tickers = queryPort.findDistinctTickersInActiveWatchlists();
        if (tickers.isEmpty()) {
            return;
        }

        Map<Ticker, StockPrice> prices = stockPriceProviderPort.fetchStockPrice(tickers);

        prices.forEach((ticker, stockPrice) -> {
            Money currentPrice = stockPrice.price().toMoney();

            queryPort.findTriggeredAlerts(ticker, currentPrice)
                    .forEach(view -> eventPublisher.publish(toEvent(view, currentPrice)));
        });
    }

    private WatchlistAlertTriggeredEvent toEvent(TriggeredAlertView view, Money currentPrice) {
        String message = "Threshold %s reached for %s on watchlist '%s' (current=%s)"
                .formatted(view.thresholdPrice(), view.ticker().value(), view.listName(), currentPrice);
        return new WatchlistAlertTriggeredEvent(
                view.watchlistId(),
                view.ownerName(),
                view.ticker(),
                AlertType.PRICE_THRESHOLD_REACHED,
                view.thresholdPrice(),
                currentPrice,
                clock.instant(),
                message
        );
    }
}
