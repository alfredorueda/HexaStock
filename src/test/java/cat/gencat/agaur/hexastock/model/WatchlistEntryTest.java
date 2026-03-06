package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WatchlistEntry entity.
 * Tests focus on validation, alert triggering logic, and entity behavior.
 */
@DisplayName("WatchlistEntry Tests")
class WatchlistEntryTest {

    private static final Ticker APPLE = new Ticker("AAPL");
    private static final Ticker MICROSOFT = new Ticker("MSFT");
    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("WatchlistEntry Creation")
    class WatchlistEntryCreation {

        @Test
        @DisplayName("Should create entry with valid ticker and threshold price")
        void shouldCreateEntryWithValidTickerAndThresholdPrice() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);

            // Then
            assertNotNull(entry.getId());
            assertEquals(APPLE, entry.getTicker());
            assertEquals(thresholdPrice, entry.getThresholdPrice());
        }

        @Test
        @DisplayName("Should generate unique ID for each entry")
        void shouldGenerateUniqueIdForEachEntry() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            WatchlistEntry entry1 = new WatchlistEntry(APPLE, thresholdPrice);
            WatchlistEntry entry2 = new WatchlistEntry(MICROSOFT, thresholdPrice);

            // Then
            assertNotEquals(entry1.getId(), entry2.getId());
        }

        @Test
        @DisplayName("Should throw exception when threshold price is zero")
        void shouldThrowExceptionWhenThresholdPriceIsZero() {
            // Given
            Money zeroPrice = new Money(USD, BigDecimal.ZERO);

            // Then
            assertThrows(InvalidAmountException.class,
                    () -> new WatchlistEntry(APPLE, zeroPrice));
        }

        @Test
        @DisplayName("Should throw exception when threshold price is negative")
        void shouldThrowExceptionWhenThresholdPriceIsNegative() {
            // Given
            Money negativePrice = new Money(USD, new BigDecimal("-50.00"));

            // Then
            assertThrows(InvalidAmountException.class,
                    () -> new WatchlistEntry(APPLE, negativePrice));
        }

        @Test
        @DisplayName("Should accept very small positive threshold price")
        void shouldAcceptVerySmallPositiveThresholdPrice() {
            // Given
            Money smallPrice = new Money(USD, new BigDecimal("0.01"));

            // When
            WatchlistEntry entry = new WatchlistEntry(APPLE, smallPrice);

            // Then
            assertEquals(smallPrice, entry.getThresholdPrice());
        }

        @Test
        @DisplayName("Should accept very large threshold price")
        void shouldAcceptVeryLargeThresholdPrice() {
            // Given
            Money largePrice = new Money(USD, new BigDecimal("999999.99"));

            // When
            WatchlistEntry entry = new WatchlistEntry(APPLE, largePrice);

            // Then
            assertEquals(largePrice, entry.getThresholdPrice());
        }
    }

    @Nested
    @DisplayName("Alert Triggering Logic")
    class AlertTriggeringLogic {

        @Test
        @DisplayName("Should trigger alert when current price is below threshold")
        void shouldTriggerAlertWhenCurrentPriceBelowThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);
            Money currentPrice = new Money(USD, new BigDecimal("145.00"));

            // When
            boolean shouldTrigger = entry.shouldTriggerAlert(currentPrice);

            // Then
            assertTrue(shouldTrigger);
        }

        @Test
        @DisplayName("Should trigger alert when current price equals threshold")
        void shouldTriggerAlertWhenCurrentPriceEqualsThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);
            Money currentPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            boolean shouldTrigger = entry.shouldTriggerAlert(currentPrice);

            // Then
            assertTrue(shouldTrigger);
        }

        @Test
        @DisplayName("Should NOT trigger alert when current price is above threshold")
        void shouldNotTriggerAlertWhenCurrentPriceAboveThreshold() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);
            Money currentPrice = new Money(USD, new BigDecimal("155.00"));

            // When
            boolean shouldTrigger = entry.shouldTriggerAlert(currentPrice);

            // Then
            assertFalse(shouldTrigger);
        }

        @Test
        @DisplayName("Should trigger alert with very small price difference")
        void shouldTriggerAlertWithVerySmallPriceDifference() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);
            Money currentPrice = new Money(USD, new BigDecimal("149.99"));

            // When
            boolean shouldTrigger = entry.shouldTriggerAlert(currentPrice);

            // Then
            assertTrue(shouldTrigger);
        }

        @Test
        @DisplayName("Should NOT trigger alert with very small price difference above")
        void shouldNotTriggerAlertWithVerySmallPriceDifferenceAbove() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);
            Money currentPrice = new Money(USD, new BigDecimal("150.01"));

            // When
            boolean shouldTrigger = entry.shouldTriggerAlert(currentPrice);

            // Then
            assertFalse(shouldTrigger);
        }

        @Test
        @DisplayName("Should handle large price differences correctly")
        void shouldHandleLargePriceDifferencesCorrectly() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);

            // When
            boolean triggersWithLowPrice = entry.shouldTriggerAlert(
                    new Money(USD, new BigDecimal("50.00")));
            boolean doesNotTriggerWithHighPrice = entry.shouldTriggerAlert(
                    new Money(USD, new BigDecimal("500.00")));

            // Then
            assertTrue(triggersWithLowPrice);
            assertFalse(doesNotTriggerWithHighPrice);
        }

        @Test
        @DisplayName("Should work with different decimal precisions")
        void shouldWorkWithDifferentDecimalPrecisions() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.50"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);

            // When
            boolean triggersAt150_50 = entry.shouldTriggerAlert(
                    new Money(USD, new BigDecimal("150.50")));
            boolean triggersAt150_49 = entry.shouldTriggerAlert(
                    new Money(USD, new BigDecimal("150.49")));
            boolean notTriggersAt150_51 = entry.shouldTriggerAlert(
                    new Money(USD, new BigDecimal("150.51")));

            // Then
            assertTrue(triggersAt150_50);
            assertTrue(triggersAt150_49);
            assertFalse(notTriggersAt150_51);
        }
    }

    @Nested
    @DisplayName("Entry Reconstitution")
    class EntryReconstitution {

        @Test
        @DisplayName("Should reconstitute entry from persistence")
        void shouldReconstituteEntryFromPersistence() {
            // Given
            String id = "entry-id-123";
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            WatchlistEntry entry = new WatchlistEntry(id, APPLE, thresholdPrice);

            // Then
            assertEquals(id, entry.getId());
            assertEquals(APPLE, entry.getTicker());
            assertEquals(thresholdPrice, entry.getThresholdPrice());
        }

        @Test
        @DisplayName("Should validate threshold price during reconstitution")
        void shouldValidateThresholdPriceDuringReconstitution() {
            // Given
            String id = "entry-id-123";
            Money invalidPrice = new Money(USD, BigDecimal.ZERO);

            // Then
            assertThrows(InvalidAmountException.class,
                    () -> new WatchlistEntry(id, APPLE, invalidPrice));
        }

        @Test
        @DisplayName("Should preserve ID during reconstitution")
        void shouldPreserveIdDuringReconstitution() {
            // Given
            String expectedId = "specific-entry-id";
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));

            // When
            WatchlistEntry entry = new WatchlistEntry(expectedId, APPLE, thresholdPrice);

            // Then
            assertEquals(expectedId, entry.getId());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle threshold at 0.01 (minimum valid price)")
        void shouldHandleThresholdAtMinimumValidPrice() {
            // Given
            Money minPrice = new Money(USD, new BigDecimal("0.01"));

            // When
            WatchlistEntry entry = new WatchlistEntry(APPLE, minPrice);

            // Then
            assertTrue(entry.shouldTriggerAlert(new Money(USD, new BigDecimal("0.01"))));
            assertFalse(entry.shouldTriggerAlert(new Money(USD, new BigDecimal("0.02"))));
        }

        @Test
        @DisplayName("Should handle very large threshold values")
        void shouldHandleVeryLargeThresholdValues() {
            // Given
            Money largePrice = new Money(USD, new BigDecimal("999999.99"));

            // When
            WatchlistEntry entry = new WatchlistEntry(APPLE, largePrice);

            // Then
            assertTrue(entry.shouldTriggerAlert(new Money(USD, new BigDecimal("999999.99"))));
            assertTrue(entry.shouldTriggerAlert(new Money(USD, new BigDecimal("500000.00"))));
            assertFalse(entry.shouldTriggerAlert(new Money(USD, new BigDecimal("1000000.00"))));
        }

        @Test
        @DisplayName("Should work correctly with entries for different tickers")
        void shouldWorkCorrectlyWithEntriesForDifferentTickers() {
            // Given
            Money appleThreshold = new Money(USD, new BigDecimal("150.00"));
            Money microsoftThreshold = new Money(USD, new BigDecimal("300.00"));

            WatchlistEntry appleEntry = new WatchlistEntry(APPLE, appleThreshold);
            WatchlistEntry microsoftEntry = new WatchlistEntry(MICROSOFT, microsoftThreshold);

            // When/Then
            assertTrue(appleEntry.shouldTriggerAlert(new Money(USD, new BigDecimal("140.00"))));
            assertFalse(appleEntry.shouldTriggerAlert(new Money(USD, new BigDecimal("160.00"))));

            assertTrue(microsoftEntry.shouldTriggerAlert(new Money(USD, new BigDecimal("290.00"))));
            assertFalse(microsoftEntry.shouldTriggerAlert(new Money(USD, new BigDecimal("310.00"))));
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicValidation {

        @Test
        @DisplayName("Should maintain immutability of threshold price")
        void shouldMaintainImmutabilityOfThresholdPrice() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry entry = new WatchlistEntry(APPLE, thresholdPrice);

            // When
            Money retrievedPrice = entry.getThresholdPrice();

            // Then
            // Money is a record (immutable), so it's fine to return the same reference
            assertEquals(thresholdPrice, retrievedPrice);

            // Verify immutability: Money's amount cannot be changed
            // (This is guaranteed by the record being immutable)
            assertNotNull(retrievedPrice.amount());
            assertNotNull(retrievedPrice.currency());
        }

        @Test
        @DisplayName("Should correctly identify ticker")
        void shouldCorrectlyIdentifyTicker() {
            // Given
            Money thresholdPrice = new Money(USD, new BigDecimal("150.00"));
            WatchlistEntry appleEntry = new WatchlistEntry(APPLE, thresholdPrice);
            WatchlistEntry microsoftEntry = new WatchlistEntry(MICROSOFT, thresholdPrice);

            // Then
            assertEquals(APPLE, appleEntry.getTicker());
            assertEquals(MICROSOFT, microsoftEntry.getTicker());
            assertNotEquals(appleEntry.getTicker(), microsoftEntry.getTicker());
        }
    }
}