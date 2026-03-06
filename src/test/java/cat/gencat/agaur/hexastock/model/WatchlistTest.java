package cat.gencat.agaur.hexastock.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Watchlist domain aggregate.
 * Tests focus on business rules and invariants without any infrastructure dependencies.
 *
 * Tests cover:
 * - Watchlist creation and validation
 * - Entry management (add/remove)
 * - Alert evaluation logic
 * - Edge cases and business rules
 */
@DisplayName("Watchlist Domain Tests")
class WatchlistTest {

    private static final Ticker APPLE = new Ticker("AAPL");
    private static final Ticker MICROSOFT = new Ticker("MSFT");
    private static final Ticker GOOGLE = new Ticker("GOOGL");
    private static final Currency USD = Currency.getInstance("USD");

    private Watchlist watchlist;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist("John Doe", "Tech Stocks");
    }

    @Nested
    @DisplayName("Watchlist Creation")
    class WatchlistCreation {

        @Test
        @DisplayName("Should create watchlist with valid owner and name")
        void shouldCreateWatchlistWithValidOwnerAndName() {
            // When
            Watchlist newWatchlist = new Watchlist("Jane Smith", "My Portfolio");

            // Then
            assertNotNull(newWatchlist.getId());
            assertEquals("Jane Smith", newWatchlist.getOwnerName());
            assertEquals("My Portfolio", newWatchlist.getName());
            assertTrue(newWatchlist.getEntries().isEmpty());
        }

        @Test
        @DisplayName("Should generate unique ID for each watchlist")
        void shouldGenerateUniqueIdForEachWatchlist() {
            // When
            Watchlist watchlist1 = new Watchlist("Owner1", "List1");
            Watchlist watchlist2 = new Watchlist("Owner2", "List2");

            // Then
            assertNotEquals(watchlist1.getId(), watchlist2.getId());
        }

        @Test
        @DisplayName("Should throw exception when owner name is null")
        void shouldThrowExceptionWhenOwnerNameIsNull() {
            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist(null, "Some Name"));
        }

        @Test
        @DisplayName("Should throw exception when owner name is empty")
        void shouldThrowExceptionWhenOwnerNameIsEmpty() {
            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist("", "Some Name"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist("   ", "Some Name"));
        }

        @Test
        @DisplayName("Should throw exception when watchlist name is null")
        void shouldThrowExceptionWhenWatchlistNameIsNull() {
            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist("Owner", null));
        }

        @Test
        @DisplayName("Should throw exception when watchlist name is empty")
        void shouldThrowExceptionWhenWatchlistNameIsEmpty() {
            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist("Owner", ""));
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist("Owner", "   "));
        }
    }

    @Nested
    @DisplayName("Entry Management")
    class EntryManagement {

        @Test
        @DisplayName("Should add entry to watchlist")
        void shouldAddEntryToWatchlist() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            watchlist.addEntry(APPLE, thresholdPrice);

            // Then
            assertEquals(1, watchlist.getEntries().size());
            WatchlistEntry entry = watchlist.getEntries().get(0);
            assertEquals(APPLE, entry.getTicker());
            assertEquals(thresholdPrice, entry.getThresholdPrice());
        }

        @Test
        @DisplayName("Should add multiple entries to watchlist")
        void shouldAddMultipleEntriesToWatchlist() {
            // Given
            Money appleThreshold = new Money(USD, new BigDecimal("150.00"));
            Money microsoftThreshold = new Money(USD, new BigDecimal("300.00"));

            // When
            watchlist.addEntry(APPLE, appleThreshold);
            watchlist.addEntry(MICROSOFT, microsoftThreshold);

            // Then
            assertEquals(2, watchlist.getEntries().size());
        }

        @Test
        @DisplayName("Should allow same ticker multiple times with different thresholds")
        void shouldAllowSameTickerMultipleTimes() {
            // Given
            Money lowThreshold = new Money(USD, new BigDecimal("100.00"));
            Money highThreshold = new Money(USD, new BigDecimal("200.00"));

            // When
            watchlist.addEntry(APPLE, lowThreshold);
            watchlist.addEntry(APPLE, highThreshold);

            // Then
            assertEquals(2, watchlist.getEntries().size());
            assertEquals(APPLE, watchlist.getEntries().get(0).getTicker());
            assertEquals(APPLE, watchlist.getEntries().get(1).getTicker());
        }

        @Test
        @DisplayName("Should remove entry by ID")
        void shouldRemoveEntryById() {
            // Given
            Money threshold = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, threshold);
            String entryId = watchlist.getEntries().get(0).getId();

            // When
            watchlist.removeEntry(entryId);

            // Then
            assertTrue(watchlist.getEntries().isEmpty());
        }

        @Test
        @DisplayName("Should remove correct entry when multiple entries exist")
        void shouldRemoveCorrectEntryWhenMultipleEntriesExist() {
            // Given
            Money appleThreshold = new Money(USD, new BigDecimal("150.00"));
            Money microsoftThreshold = new Money(USD, new BigDecimal("300.00"));
            Money googleThreshold = new Money(USD, new BigDecimal("140.00"));

            watchlist.addEntry(APPLE, appleThreshold);
            watchlist.addEntry(MICROSOFT, microsoftThreshold);
            watchlist.addEntry(GOOGLE, googleThreshold);

            String microsoftEntryId = watchlist.getEntries().get(1).getId();

            // When
            watchlist.removeEntry(microsoftEntryId);

            // Then
            assertEquals(2, watchlist.getEntries().size());
            assertEquals(APPLE, watchlist.getEntries().get(0).getTicker());
            assertEquals(GOOGLE, watchlist.getEntries().get(1).getTicker());
        }

        @Test
        @DisplayName("Should do nothing when removing non-existent entry")
        void shouldDoNothingWhenRemovingNonExistentEntry() {
            // Given
            Money threshold = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, threshold);
            int initialSize = watchlist.getEntries().size();

            // When
            watchlist.removeEntry("non-existent-id");

            // Then
            assertEquals(initialSize, watchlist.getEntries().size());
        }

        @Test
        @DisplayName("Should return unmodifiable list of entries")
        void shouldReturnUnmodifiableListOfEntries() {
            // Given
            Money threshold = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, threshold);

            // When
            List<WatchlistEntry> entries = watchlist.getEntries();

            // Then
            assertThrows(UnsupportedOperationException.class,
                    () -> entries.add(new WatchlistEntry(MICROSOFT, threshold)));
        }
    }

    @Nested
    @DisplayName("Alert Evaluation")
    class AlertEvaluation {

        @Test
        @DisplayName("Should trigger alert when current price is below threshold")
        void shouldTriggerAlertWhenCurrentPriceBelowThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, thresholdPrice);

            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("145.00")));

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertEquals(1, alerts.size());
            AlertTrigger alert = alerts.get(0);
            assertEquals("John Doe", alert.ownerName());
            assertEquals("Tech Stocks", alert.watchlistName());
            assertEquals(APPLE, alert.ticker());
            assertEquals(thresholdPrice, alert.thresholdPrice());
            assertEquals(new BigDecimal("145.00"), alert.currentPrice().amount());
        }

        @Test
        @DisplayName("Should trigger alert when current price equals threshold")
        void shouldTriggerAlertWhenCurrentPriceEqualsThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, thresholdPrice);

            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("150.00")));

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertEquals(1, alerts.size());
        }

        @Test
        @DisplayName("Should NOT trigger alert when current price is above threshold")
        void shouldNotTriggerAlertWhenCurrentPriceAboveThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            watchlist.addEntry(APPLE, thresholdPrice);

            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("155.00")));

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should trigger multiple alerts when multiple entries match")
        void shouldTriggerMultipleAlertsWhenMultipleEntriesMatch() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));
            watchlist.addEntry(GOOGLE, new Money(USD, new BigDecimal("140.00")));

            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("145.00"))); // Triggers
            currentPrices.put(MICROSOFT, new Money(USD, new BigDecimal("295.00"))); // Triggers
            currentPrices.put(GOOGLE, new Money(USD, new BigDecimal("145.00"))); // Does NOT trigger

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertEquals(2, alerts.size());
            assertTrue(alerts.stream().anyMatch(a -> a.ticker().equals(APPLE)));
            assertTrue(alerts.stream().anyMatch(a -> a.ticker().equals(MICROSOFT)));
            assertFalse(alerts.stream().anyMatch(a -> a.ticker().equals(GOOGLE)));
        }

        @Test
        @DisplayName("Should NOT trigger alert when ticker has no current price")
        void shouldNotTriggerAlertWhenTickerHasNoCurrentPrice() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));

            Map<Ticker, Money> currentPrices = new HashMap<>();
            // APPLE price not provided

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty watchlist without errors")
        void shouldHandleEmptyWatchlistWithoutErrors() {
            // Given - empty watchlist
            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("150.00")));

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty price map without errors")
        void shouldHandleEmptyPriceMapWithoutErrors() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            Map<Ticker, Money> currentPrices = new HashMap<>(); // Empty

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Should trigger multiple alerts for same ticker with different thresholds")
        void shouldTriggerMultipleAlertsForSameTickerWithDifferentThresholds() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("145.00")));
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("140.00")));

            Map<Ticker, Money> currentPrices = new HashMap<>();
            currentPrices.put(APPLE, new Money(USD, new BigDecimal("142.00")));

            // When
            List<AlertTrigger> alerts = watchlist.evaluateAlerts(currentPrices);

            // Then
            assertEquals(2, alerts.size()); // 150 and 145 should trigger, 140 should not
        }
    }

    @Nested
    @DisplayName("Ticker Management")
    class TickerManagement {

        @Test
        @DisplayName("Should return all unique tickers")
        void shouldReturnAllUniqueTickers() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));
            watchlist.addEntry(GOOGLE, new Money(USD, new BigDecimal("140.00")));

            // When
            Set<Ticker> tickers = watchlist.getAllTickers();

            // Then
            assertEquals(3, tickers.size());
            assertTrue(tickers.contains(APPLE));
            assertTrue(tickers.contains(MICROSOFT));
            assertTrue(tickers.contains(GOOGLE));
        }

        @Test
        @DisplayName("Should return unique tickers even when same ticker appears multiple times")
        void shouldReturnUniqueTickersEvenWhenSameTickerAppearsMultipleTimes() {
            // Given
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("150.00")));
            watchlist.addEntry(APPLE, new Money(USD, new BigDecimal("145.00")));
            watchlist.addEntry(MICROSOFT, new Money(USD, new BigDecimal("300.00")));

            // When
            Set<Ticker> tickers = watchlist.getAllTickers();

            // Then
            assertEquals(2, tickers.size());
            assertTrue(tickers.contains(APPLE));
            assertTrue(tickers.contains(MICROSOFT));
        }

        @Test
        @DisplayName("Should return empty set when no entries")
        void shouldReturnEmptySetWhenNoEntries() {
            // When
            Set<Ticker> tickers = watchlist.getAllTickers();

            // Then
            assertTrue(tickers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Watchlist Reconstitution")
    class WatchlistReconstitution {

        @Test
        @DisplayName("Should reconstitute watchlist from persistence")
        void shouldReconstituteWatchlistFromPersistence() {
            // Given
            String id = "test-id-123";
            String ownerName = "Jane Doe";
            String name = "My Watchlist";
            List<WatchlistEntry> entries = Arrays.asList(
                    new WatchlistEntry("entry-1", APPLE, new Money(USD, new BigDecimal("150.00"))),
                    new WatchlistEntry("entry-2", MICROSOFT, new Money(USD, new BigDecimal("300.00")))
            );

            // When
            Watchlist reconstituted = new Watchlist(id, ownerName, name, entries);

            // Then
            assertEquals(id, reconstituted.getId());
            assertEquals(ownerName, reconstituted.getOwnerName());
            assertEquals(name, reconstituted.getName());
            assertEquals(2, reconstituted.getEntries().size());
        }

        @Test
        @DisplayName("Should validate owner name during reconstitution")
        void shouldValidateOwnerNameDuringReconstitution() {
            // Given
            String id = "test-id";
            List<WatchlistEntry> entries = new ArrayList<>();

            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist(id, "", "Name", entries));
        }

        @Test
        @DisplayName("Should validate watchlist name during reconstitution")
        void shouldValidateWatchlistNameDuringReconstitution() {
            // Given
            String id = "test-id";
            List<WatchlistEntry> entries = new ArrayList<>();

            // Then
            assertThrows(IllegalArgumentException.class,
                    () -> new Watchlist(id, "Owner", "", entries));
        }
    }
}