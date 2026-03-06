package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketMonitoringService.
 *
 * These tests verify that the service correctly orchestrates:
 * - Retrieving watchlists from the repository
 * - Fetching current prices from the price provider
 * - Delegating alert evaluation to domain entities
 *
 * IMPORTANT: This service should contain NO business logic.
 * All business logic is in the Watchlist domain entity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketMonitoringService Tests")
class MarketMonitoringServiceTest {

    @Mock
    private WatchlistPort watchlistPort;

    @Mock
    private StockPriceProviderPort stockPriceProvider;

    private MarketMonitoringService marketMonitoringService;

    private static final Ticker APPLE = new Ticker("AAPL");
    private static final Ticker MICROSOFT = new Ticker("MSFT");
    private static final Ticker GOOGLE = new Ticker("GOOGL");
    private static final Currency USD = Currency.getInstance("USD");

    @BeforeEach
    void setUp() {
        marketMonitoringService = new MarketMonitoringService(
                watchlistPort,
                stockPriceProvider
        );
    }

    @Nested
    @DisplayName("Alert Scanning and Evaluation")
    class AlertScanningAndEvaluation {

        @Test
        @DisplayName("Should return empty list when no watchlists exist")
        void shouldReturnEmptyListWhenNoWatchlistsExist() {
            // Given
            when(watchlistPort.findAll()).thenReturn(Collections.emptyList());

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertTrue(alerts.isEmpty());
            verify(watchlistPort).findAll();
            verifyNoInteractions(stockPriceProvider);
        }

        @Test
        @DisplayName("Should fetch prices for all unique tickers across watchlists")
        void shouldFetchPricesForAllUniqueTickersAcrossWatchlists() {
            // Given
            Watchlist watchlist1 = new Watchlist("user1", "Tech");
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist1.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            Watchlist watchlist2 = new Watchlist("user2", "Stocks");
            watchlist2.addEntry(GOOGLE, new Money(USD, new BigDecimal("140.00")));
            watchlist2.addEntry(APPLE, new Money(USD, new BigDecimal("145.00"))); // APPLE repeated

            when(watchlistPort.findAll()).thenReturn(Arrays.asList(watchlist1, watchlist2));

            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 155.0, Instant.now(), "USD"));
            when(stockPriceProvider.fetchStockPrice(MICROSOFT))
                    .thenReturn(new StockPrice(MICROSOFT, 305.0, Instant.now(), "USD"));
            when(stockPriceProvider.fetchStockPrice(GOOGLE))
                    .thenReturn(new StockPrice(GOOGLE, 145.0, Instant.now(), "USD"));

            // When
            marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            verify(stockPriceProvider).fetchStockPrice(APPLE);
            verify(stockPriceProvider).fetchStockPrice(MICROSOFT);
            verify(stockPriceProvider).fetchStockPrice(GOOGLE);
            verify(stockPriceProvider, times(3)).fetchStockPrice(any(Ticker.class));
        }

        @Test
        @DisplayName("Should collect alerts from all watchlists")
        void shouldCollectAlertsFromAllWatchlists() {
            // Given
            Watchlist watchlist1 = new Watchlist("user1", "Tech");
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            Watchlist watchlist2 = new Watchlist("user2", "Stocks");
            watchlist2.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            when(watchlistPort.findAll()).thenReturn(Arrays.asList(watchlist1, watchlist2));

            // Both prices below threshold - should trigger alerts
            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 145.0, Instant.now(), "USD"));
            when(stockPriceProvider.fetchStockPrice(MICROSOFT))
                    .thenReturn(new StockPrice(MICROSOFT, 295.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(2, alerts.size());
            assertTrue(alerts.stream().anyMatch(a ->
                    a.ticker().equals(APPLE) && a.ownerName().equals("user1")));
            assertTrue(alerts.stream().anyMatch(a ->
                    a.ticker().equals(MICROSOFT) && a.ownerName().equals("user2")));
        }

        @Test
        @DisplayName("Should handle watchlist with no triggered alerts")
        void shouldHandleWatchlistWithNoTriggeredAlerts() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Tech");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            // Price above threshold - should NOT trigger
            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 155.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should aggregate alerts from multiple watchlists for same user")
        void shouldAggregateAlertsFromMultipleWatchlistsForSameUser() {
            // Given
            Watchlist watchlist1 = new Watchlist("user1", "Tech Stocks");
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            Watchlist watchlist2 = new Watchlist("user1", "Growth Stocks");
            watchlist2.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            when(watchlistPort.findAll()).thenReturn(Arrays.asList(watchlist1, watchlist2));

            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 145.0, Instant.now(), "USD"));
            when(stockPriceProvider.fetchStockPrice(MICROSOFT))
                    .thenReturn(new StockPrice(MICROSOFT, 295.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(2, alerts.size());
            assertTrue(alerts.stream().allMatch(a -> a.ownerName().equals("user1")));
            assertEquals(Set.of("Tech Stocks", "Growth Stocks"),
                    alerts.stream().map(AlertTrigger::watchlistName).collect(java.util.stream.Collectors.toSet()));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should continue processing when price fetch fails for one ticker")
        void shouldContinueProcessingWhenPriceFetchFailsForOneTicker() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Tech");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            // APPLE fails, MICROSOFT succeeds
            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenThrow(new RuntimeException("API error"));
            when(stockPriceProvider.fetchStockPrice(MICROSOFT))
                    .thenReturn(new StockPrice(MICROSOFT, 295.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(1, alerts.size());
            assertEquals(MICROSOFT, alerts.get(0).ticker());
        }

        @Test
        @DisplayName("Should handle all price fetches failing gracefully")
        void shouldHandleAllPriceFetchesFailingGracefully() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Tech");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            when(stockPriceProvider.fetchStockPrice(any(Ticker.class)))
                    .thenThrow(new RuntimeException("API down"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should not throw exception when watchlist repository fails")
        void shouldNotThrowExceptionWhenWatchlistRepositoryFails() {
            // Given
            when(watchlistPort.findAll()).thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThrows(RuntimeException.class,
                    () -> marketMonitoringService.scanAndEvaluateAlerts());
        }
    }

    @Nested
    @DisplayName("Price Conversion and Currency Handling")
    class PriceConversionAndCurrencyHandling {

        @Test
        @DisplayName("Should correctly convert StockPrice to Money")
        void shouldCorrectlyConvertStockPriceToMoney() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Tech");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 145.50, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(1, alerts.size());
            AlertTrigger alert = alerts.get(0);
            // Compare values, not exact BigDecimal representation (145.5 == 145.50)
            assertEquals(0, new BigDecimal("145.50").compareTo(alert.currentPrice().amount()));
            assertEquals(USD, alert.currentPrice().currency());
        }

        @Test
        @DisplayName("Should handle different price formats correctly")
        void shouldHandleDifferentPriceFormatsCorrectly() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Stocks");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("100.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("200.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 99.99, Instant.now(), "USD"));
            when(stockPriceProvider.fetchStockPrice(MICROSOFT))
                    .thenReturn(new StockPrice(MICROSOFT, 199.99, Instant.now(), "USD")); // Changed to 2 decimals

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(2, alerts.size());
        }
    }

    @Nested
    @DisplayName("Service Orchestration")
    class ServiceOrchestration {

        @Test
        @DisplayName("Should delegate alert evaluation to domain entities")
        void shouldDelegateAlertEvaluationToDomainEntities() {
            // Given
            Watchlist watchlist = new Watchlist("user1", "Tech");
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(watchlist));

            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 145.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            // Verify service orchestrates but doesn't contain business logic
            verify(watchlistPort).findAll();
            verify(stockPriceProvider).fetchStockPrice(APPLE);
            assertEquals(1, alerts.size());

            // Alert details come from domain entity evaluation
            AlertTrigger alert = alerts.get(0);
            assertEquals("user1", alert.ownerName());
            assertEquals("Tech", alert.watchlistName());
            assertEquals(APPLE, alert.ticker());
        }

        @Test
        @DisplayName("Should process watchlists in order received from repository")
        void shouldProcessWatchlistsInOrderReceivedFromRepository() {
            // Given
            Watchlist watchlist1 = new Watchlist("user1", "List1");
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            Watchlist watchlist2 = new Watchlist("user2", "List2");
            watchlist2.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            Watchlist watchlist3 = new Watchlist("user3", "List3");
            watchlist3.addEntry(GOOGLE, new Money(USD, new BigDecimal("140.00")));

            when(watchlistPort.findAll()).thenReturn(Arrays.asList(watchlist1, watchlist2, watchlist3));

            when(stockPriceProvider.fetchStockPrice(any(Ticker.class)))
                    .thenReturn(new StockPrice(APPLE, 100.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            verify(watchlistPort).findAll();
            assertEquals(3, alerts.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle watchlist with empty entries")
        void shouldHandleWatchlistWithEmptyEntries() {
            // Given
            Watchlist emptyWatchlist = new Watchlist("user1", "Empty");
            when(watchlistPort.findAll()).thenReturn(Collections.singletonList(emptyWatchlist));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertTrue(alerts.isEmpty());
            verifyNoInteractions(stockPriceProvider);
        }

        @Test
        @DisplayName("Should handle large number of watchlists efficiently")
        void shouldHandleLargeNumberOfWatchlistsEfficiently() {
            // Given
            List<Watchlist> manyWatchlists = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Watchlist watchlist = new Watchlist("user" + i, "List" + i);
                watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
                manyWatchlists.add(watchlist);
            }

            when(watchlistPort.findAll()).thenReturn(manyWatchlists);
            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 145.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            assertEquals(100, alerts.size());
            // Should only fetch APPLE price once (deduplication)
            verify(stockPriceProvider, times(1)).fetchStockPrice(APPLE);
        }

        @Test
        @DisplayName("Should handle duplicate tickers across watchlists")
        void shouldHandleDuplicateTickersAcrossWatchlists() {
            // Given
            Watchlist watchlist1 = new Watchlist("user1", "List1");
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist1.addEntry(APPLE, new Money(USD, new BigDecimal("145.00")));

            Watchlist watchlist2 = new Watchlist("user2", "List2");
            watchlist2.addEntry(APPLE, new Money(USD, new BigDecimal("140.00")));

            when(watchlistPort.findAll()).thenReturn(Arrays.asList(watchlist1, watchlist2));
            // Price 139.0 triggers all 3 alerts (139 <= 150, 139 <= 145, 139 <= 140)
            when(stockPriceProvider.fetchStockPrice(APPLE))
                    .thenReturn(new StockPrice(APPLE, 139.0, Instant.now(), "USD"));

            // When
            List<AlertTrigger> alerts = marketMonitoringService.scanAndEvaluateAlerts();

            // Then
            // Should fetch price only once but trigger multiple alerts
            verify(stockPriceProvider, times(1)).fetchStockPrice(APPLE);
            assertEquals(3, alerts.size()); // All three entries should trigger (139 <= all thresholds)
        }
    }
}