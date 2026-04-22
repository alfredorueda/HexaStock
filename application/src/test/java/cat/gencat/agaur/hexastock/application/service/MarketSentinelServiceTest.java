package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.in.MarketSentinelUseCase;
import cat.gencat.agaur.hexastock.application.port.out.*;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MarketSentinelService")
class MarketSentinelServiceTest {

    private WatchlistQueryPort queryPort;
    private StockPriceProviderPort stockPriceProviderPort;
    private NotificationPort notificationPort;
    private MarketSentinelUseCase service;

    @BeforeEach
    void setUp() {
        queryPort = mock(WatchlistQueryPort.class);
        stockPriceProviderPort = mock(StockPriceProviderPort.class);
        notificationPort = mock(NotificationPort.class);
        service = new MarketSentinelService(queryPort, stockPriceProviderPort, notificationPort);
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-1", level = TestLevel.DOMAIN, feature = "market-sentinel-price-threshold-alerts.feature")
    void shouldFetchDistinctTickerPriceOncePerDetectionCycle() {
        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        when(stockPriceProviderPort.fetchStockPrice(Set.of(aapl))).thenReturn(
                Map.of(aapl, new StockPrice(aapl, Price.of("140.00"), Instant.now()))
        );
        when(queryPort.findTriggeredAlerts(any(), any())).thenReturn(List.of());

        service.detectBuySignals();

        verify(stockPriceProviderPort, times(1)).fetchStockPrice(Set.of(aapl));
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-2", level = TestLevel.DOMAIN, feature = "market-sentinel-price-threshold-alerts.feature")
    void shouldNotifyForTriggeredAlertsThresholdGreaterOrEqualCurrentPrice() {
        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        StockPrice stockPrice = new StockPrice(aapl, Price.of("140.00"), Instant.now());
        when(stockPriceProviderPort.fetchStockPrice(Set.of(aapl))).thenReturn(Map.of(aapl, stockPrice));
        when(queryPort.findTriggeredAlerts(aapl, Money.of("140.00"))).thenReturn(List.of(
                new TriggeredAlertView("alice", "Tech", "123456", aapl, Money.of("150.00"))
        ));

        service.detectBuySignals();

        verify(notificationPort, times(1)).notifyBuySignal(any(BuySignal.class));
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-3", level = TestLevel.DOMAIN, feature = "market-sentinel-price-threshold-alerts.feature")
    void shouldNotifyMultipleTimesWhenMultipleAlertsTriggered() {
        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        StockPrice stockPrice = new StockPrice(aapl, Price.of("128.00"), Instant.now());
        when(stockPriceProviderPort.fetchStockPrice(Set.of(aapl))).thenReturn(Map.of(aapl, stockPrice));
        when(queryPort.findTriggeredAlerts(aapl, Money.of("128.00"))).thenReturn(List.of(
                new TriggeredAlertView("alice", "Tech", "123456", aapl, Money.of("150.00")),
                new TriggeredAlertView("alice", "Tech", "123456", aapl, Money.of("140.00"))
        ));

        service.detectBuySignals();

        verify(notificationPort, times(2)).notifyBuySignal(any(BuySignal.class));
    }
}

