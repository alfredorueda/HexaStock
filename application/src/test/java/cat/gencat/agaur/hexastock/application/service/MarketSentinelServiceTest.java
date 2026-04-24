package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.application.port.in.MarketSentinelUseCase;
import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import cat.gencat.agaur.hexastock.marketdata.application.port.out.MarketDataPort;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.marketdata.model.market.StockPrice;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MarketSentinelService")
class MarketSentinelServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-15T10:00:00Z");

    private WatchlistQueryPort queryPort;
    private MarketDataPort stockPriceProviderPort;
    private DomainEventPublisher eventPublisher;
    private MarketSentinelUseCase service;

    @BeforeEach
    void setUp() {
        queryPort = mock(WatchlistQueryPort.class);
        stockPriceProviderPort = mock(MarketDataPort.class);
        eventPublisher = mock(DomainEventPublisher.class);
        service = new MarketSentinelService(
                queryPort,
                stockPriceProviderPort,
                eventPublisher,
                Clock.fixed(FIXED, ZoneOffset.UTC)
        );
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-2", level = TestLevel.DOMAIN, feature = "market-sentinel-price-threshold-alerts.feature")
    void shouldPublishEventForEachTriggeredAlert() {
        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        StockPrice stockPrice = new StockPrice(aapl, Price.of("140.00"), Instant.now());
        when(stockPriceProviderPort.fetchStockPrice(Set.of(aapl))).thenReturn(Map.of(aapl, stockPrice));
        when(queryPort.findTriggeredAlerts(aapl, Money.of("140.00"))).thenReturn(List.of(
                new TriggeredAlertView("wl-1", "alice", "Tech", aapl, Money.of("150.00"))
        ));

        service.detectBuySignals();

        ArgumentCaptor<WatchlistAlertTriggeredEvent> captor =
                ArgumentCaptor.forClass(WatchlistAlertTriggeredEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());

        WatchlistAlertTriggeredEvent ev = captor.getValue();
        assertThat(ev.watchlistId()).isEqualTo("wl-1");
        assertThat(ev.userId()).isEqualTo("alice");
        assertThat(ev.ticker()).isEqualTo(aapl);
        assertThat(ev.threshold()).isEqualTo(Money.of("150.00"));
        assertThat(ev.currentPrice()).isEqualTo(Money.of("140.00"));
        assertThat(ev.alertType()).isEqualTo(WatchlistAlertTriggeredEvent.AlertType.PRICE_THRESHOLD_REACHED);
        assertThat(ev.occurredOn()).isEqualTo(FIXED);
        assertThat(ev.message()).contains("Tech").contains("AAPL");
    }

    @Test
    @SpecificationRef(value = "US-MS-01.AC-3", level = TestLevel.DOMAIN, feature = "market-sentinel-price-threshold-alerts.feature")
    void shouldPublishOneEventPerTriggeredAlertWhenMultipleMatch() {
        Ticker aapl = Ticker.of("AAPL");
        when(queryPort.findDistinctTickersInActiveWatchlists()).thenReturn(Set.of(aapl));
        StockPrice stockPrice = new StockPrice(aapl, Price.of("128.00"), Instant.now());
        when(stockPriceProviderPort.fetchStockPrice(Set.of(aapl))).thenReturn(Map.of(aapl, stockPrice));
        when(queryPort.findTriggeredAlerts(aapl, Money.of("128.00"))).thenReturn(List.of(
                new TriggeredAlertView("wl-1", "alice", "Tech", aapl, Money.of("150.00")),
                new TriggeredAlertView("wl-1", "alice", "Tech", aapl, Money.of("140.00"))
        ));

        service.detectBuySignals();

        verify(eventPublisher, times(2)).publish(any(WatchlistAlertTriggeredEvent.class));
    }

    @Test
    @DisplayName("Published events do not carry any infrastructure data (no chatId, no email...)")
    void publishedEventsAreFreeOfInfrastructureData() {
        for (var component : WatchlistAlertTriggeredEvent.class.getRecordComponents()) {
            String name = component.getName().toLowerCase();
            assertThat(name).doesNotContain("telegram").doesNotContain("chat")
                    .doesNotContain("email").doesNotContain("phone").doesNotContain("notification");
        }
    }
}
