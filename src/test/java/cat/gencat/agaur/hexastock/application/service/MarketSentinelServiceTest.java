package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Price;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@DisplayName("MarketSentinelService")
class MarketSentinelServiceTest {

    @Test
    @DisplayName("should notify when price below threshold")
    void shouldNotifyWhenPriceBelowThreshold() {
        WatchlistQueryPort queryPort = mock(WatchlistQueryPort.class);
        StockPriceProviderPort stockPriceProviderPort = mock(StockPriceProviderPort.class);
        NotificationPort notificationPort = mock(NotificationPort.class);
        MarketSentinelService marketSentinelService = new MarketSentinelService(queryPort, stockPriceProviderPort, notificationPort);

        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        when(stockPriceProviderPort.fetchStockPrice(anySet()))
                .thenReturn(Map.of(aapl, StockPrice.of(aapl, Price.of(140.0), Instant.now())));
        when(queryPort.findTriggeredAlerts(any(), any()))
                .thenReturn(List.of(new TriggeredAlertView("john", "Tech Stocks", aapl, Money.of("150.00"))));

        marketSentinelService.detectBuySignals();

        verify(notificationPort).notifyBuySignal(any());
    }

    @Test
    @DisplayName("should notify multiple times when multiple alerts triggered")
    void shouldNotifyMultipleTimesWhenMultipleAlertsTriggered() {
        WatchlistQueryPort queryPort = mock(WatchlistQueryPort.class);
        StockPriceProviderPort stockPriceProviderPort = mock(StockPriceProviderPort.class);
        NotificationPort notificationPort = mock(NotificationPort.class);
        MarketSentinelService marketSentinelService = new MarketSentinelService(queryPort, stockPriceProviderPort, notificationPort);

        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        when(stockPriceProviderPort.fetchStockPrice(anySet()))
                .thenReturn(Map.of(aapl, StockPrice.of(aapl, Price.of(128.0), Instant.now())));
        when(queryPort.findTriggeredAlerts(any(), any()))
                .thenReturn(List.of(
                        new TriggeredAlertView("john", "Tech Stocks", aapl, Money.of("150.00")),
                        new TriggeredAlertView("john", "Tech Stocks", aapl, Money.of("140.00"))
                ));

        marketSentinelService.detectBuySignals();

        ArgumentCaptor<BuySignal> signalCaptor = ArgumentCaptor.forClass(BuySignal.class);
        verify(notificationPort, times(2)).notifyBuySignal(signalCaptor.capture());
        List<BuySignal> sentSignals = signalCaptor.getAllValues();

        assertEquals(2, sentSignals.size());
        assertEquals(2, sentSignals.stream().map(BuySignal::thresholdPrice).distinct().count());
    }

    @Test
    @DisplayName("should not notify when no active tickers exist")
    void shouldNotNotifyWhenNoActiveTickersExist() {
        WatchlistQueryPort queryPort = mock(WatchlistQueryPort.class);
        StockPriceProviderPort stockPriceProviderPort = mock(StockPriceProviderPort.class);
        NotificationPort notificationPort = mock(NotificationPort.class);
        MarketSentinelService marketSentinelService = new MarketSentinelService(queryPort, stockPriceProviderPort, notificationPort);

        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of());

        marketSentinelService.detectBuySignals();

        verify(stockPriceProviderPort, never()).fetchStockPrice(anySet());
        verifyNoInteractions(notificationPort);
    }

    @Test
    @DisplayName("should skip ticker when provider returns null price")
    void shouldSkipTickerWhenProviderReturnsNullPrice() {
        WatchlistQueryPort queryPort = mock(WatchlistQueryPort.class);
        StockPriceProviderPort stockPriceProviderPort = mock(StockPriceProviderPort.class);
        NotificationPort notificationPort = mock(NotificationPort.class);
        MarketSentinelService marketSentinelService = new MarketSentinelService(queryPort, stockPriceProviderPort, notificationPort);

        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        Map<Ticker, StockPrice> prices = new HashMap<>();
        prices.put(aapl, null);
        when(stockPriceProviderPort.fetchStockPrice(anySet())).thenReturn(prices);

        marketSentinelService.detectBuySignals();

        verify(queryPort, never()).findTriggeredAlerts(any(), any());
        verifyNoInteractions(notificationPort);
    }
}
