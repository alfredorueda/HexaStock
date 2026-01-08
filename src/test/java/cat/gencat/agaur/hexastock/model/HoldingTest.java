package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Holding domain entity.
 * Tests focus on business rules and invariants related to stock ownership and lot management.
 */
@DisplayName("Holding Domain Tests")
class HoldingTest {
    
    private static final Ticker GOOGLE = new Ticker("GOOG");
    private static final BigDecimal PRICE_100 = new BigDecimal("100.00");
    private static final BigDecimal PRICE_120 = new BigDecimal("120.00");
    private static final BigDecimal PRICE_90 = new BigDecimal("90.00");
    private static final BigDecimal PRICE_110 = new BigDecimal("110.00");
    
    private Holding holding;
    
    @BeforeEach
    void setUp() {
        holding = Holding.create(GOOGLE);
    }
    
    @Nested
    @DisplayName("Holding Creation")
    class HoldingCreation {
        
        @Test
        @DisplayName("Should create a holding with factory method")
        void shouldCreateHoldingWithFactoryMethod() {
            // Then
            assertNotNull(holding.getId());
            assertEquals(GOOGLE, holding.getTicker());
            assertEquals(0, holding.getTotalShares());
            assertTrue(holding.getLots().isEmpty());
        }
        
        @Test
        @DisplayName("Should create a holding with explicit constructor")
        void shouldCreateHoldingWithExplicitConstructor() {
            // Given
            String id = UUID.randomUUID().toString();
            
            // When
            Holding explicitHolding = new Holding(id, GOOGLE);
            
            // Then
            assertEquals(id, explicitHolding.getId());
            assertEquals(GOOGLE, explicitHolding.getTicker());
            assertEquals(0, explicitHolding.getTotalShares());
            assertTrue(explicitHolding.getLots().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Buying Operations")
    class BuyingOperations {
        
        @Test
        @DisplayName("Should add a lot when buying shares")
        void shouldAddLotWhenBuyingShares() {
            // When
            holding.buy(10, PRICE_100);
            
            // Then
            assertEquals(10, holding.getTotalShares());
            assertEquals(1, holding.getLots().size());
            
            Lot lot = holding.getLots().get(0);
            assertEquals(10, lot.getInitialStocks());
            assertEquals(10, lot.getRemaining());
            assertEquals(PRICE_100, lot.getUnitPrice());
            assertNotNull(lot.getPurchasedAt());
        }
        
        @Test
        @DisplayName("Should add multiple lots when buying multiple times")
        void shouldAddMultipleLotsWhenBuyingMultipleTimes() {
            // When
            holding.buy(10, PRICE_100);
            holding.buy(5, PRICE_120);
            
            // Then
            assertEquals(15, holding.getTotalShares());
            assertEquals(2, holding.getLots().size());
            
            Lot firstLot = holding.getLots().get(0);
            assertEquals(10, firstLot.getRemaining());
            assertEquals(PRICE_100, firstLot.getUnitPrice());
            
            Lot secondLot = holding.getLots().get(1);
            assertEquals(5, secondLot.getRemaining());
            assertEquals(PRICE_120, secondLot.getUnitPrice());
        }
    }
    
    @Nested
    @DisplayName("Selling Operations")
    class SellingOperations {
        
        @Test
        @DisplayName("Should sell shares from oldest lot first (FIFO)")
        void shouldSellSharesFromOldestLotFirst() {
            // Given
            holding.buy(10, PRICE_100);
            holding.buy(5, PRICE_120);
            
            // When
            SellResult result = holding.sell(8, PRICE_110);
            
            // Then
            assertEquals(7, holding.getTotalShares());
            assertEquals(2, holding.getLots().get(0).getRemaining());
            assertEquals(5, holding.getLots().get(1).getRemaining());
            
            // Proceeds calculation: 8 shares * $110 = $880
            assertEquals(new BigDecimal("880.00"), result.proceeds());
            
            // Cost basis calculation: (8 shares from first lot * $100) = $800
            assertEquals(new BigDecimal("800.00"), result.costBasis());
            
            // Profit calculation: $880 - $800 = $80
            assertEquals(new BigDecimal("80.00"), result.profit());
        }
        
        @Test
        @DisplayName("Should sell shares across multiple lots if needed")
        void shouldSellSharesAcrossMultipleLots() {
            // Given
            holding.buy(10, PRICE_100);  // Lot 1: 10 shares @ $100
            holding.buy(5, PRICE_120);   // Lot 2: 5 shares @ $120
            
            // When selling 12 shares, should take 10 from first lot and 2 from second lot
            SellResult result = holding.sell(12, PRICE_110);
            
            // Then
            assertEquals(3, holding.getTotalShares());
            assertEquals(1, holding.getLots().size()); // Only one active lot should remain
            
            Lot remainingLot = holding.getLots().get(0);
            assertEquals(3, remainingLot.getRemaining());
            assertEquals(PRICE_120, remainingLot.getUnitPrice());
            
            // Proceeds calculation: 12 shares * $110 = $1320
            assertEquals(new BigDecimal("1320.00"), result.proceeds());
            
            // Cost basis calculation: (10 shares from first lot * $100) + (2 shares from second lot * $120) = $1000 + $240 = $1240
            assertEquals(new BigDecimal("1240.00"), result.costBasis());
            
            // Profit calculation: $1320 - $1240 = $80
            assertEquals(new BigDecimal("80.00"), result.profit());
        }
        
        @Test
        @DisplayName("Should throw exception when selling more shares than available")
        void shouldThrowExceptionWhenSellingMoreSharesThanAvailable() {
            // Given
            holding.buy(10, PRICE_100);
            
            // Then
            assertThrows(ConflictQuantityException.class, () -> holding.sell(15, PRICE_110));
        }
        
        @Test
        @DisplayName("Should handle selling at a loss")
        void shouldHandleSellingAtALoss() {
            // Given
            holding.buy(10, PRICE_100);
            
            // When - Selling at a price lower than purchase price
            SellResult result = holding.sell(5, PRICE_90);
            
            // Then
            assertEquals(5, holding.getTotalShares());
            
            // Proceeds calculation: 5 shares * $90 = $450
            assertEquals(new BigDecimal("450.00"), result.proceeds());
            
            // Cost basis calculation: 5 shares * $100 = $500
            assertEquals(new BigDecimal("500.00"), result.costBasis());
            
            // Loss calculation: $450 - $500 = -$50
            assertEquals(new BigDecimal("-50.00"), result.profit());
        }
        
        @Test
        @DisplayName("Should correctly sell shares across multiple lots with FIFO")
        void testSellStockWithMultipleLots() {
            // Arrange: 5 lots of the same stock at different prices
            holding.buy(10, new BigDecimal("100.00")); // Lot 1
            holding.buy(15, new BigDecimal("105.00")); // Lot 2
            holding.buy(20, new BigDecimal("110.00")); // Lot 3
            holding.buy(25, new BigDecimal("115.00")); // Lot 4
            holding.buy(30, new BigDecimal("120.00")); // Lot 5

            // Act: Sell all shares from first 3 lots (10+15+20=45), and 5 shares from 4th lot
            int sharesToSell = 10 + 15 + 20 + 5; // 50 shares
            BigDecimal sellPrice = new BigDecimal("130.00");
            SellResult result = holding.sell(sharesToSell, sellPrice);

            BigDecimal expectedProfit = new BigDecimal("1150.00");

            // Assert: Only lots with remaining > 0 should be present
            assertEquals(2, holding.getLots().size(), "Only lots with remaining shares should be present");
            Lot lot4 = holding.getLots().get(0);
            Lot lot5 = holding.getLots().get(1);
            assertEquals(20, lot4.getRemaining(), "Lot 4 should have 20 shares left");
            assertEquals(new BigDecimal("115.00"), lot4.getUnitPrice(), "Lot 4 price should be $115.00");
            assertEquals(30, lot5.getRemaining(), "Lot 5 should have 30 shares left");
            assertEquals(new BigDecimal("120.00"), lot5.getUnitPrice(), "Lot 5 price should be $120.00");
            assertEquals(expectedProfit, result.profit(), "Profit should match expected FIFO calculation");
        }
    }
    
    @Nested
    @DisplayName("Lot Management")
    class LotManagement {
        
        @Test
        @DisplayName("Should add a lot explicitly")
        void shouldAddLotExplicitly() {
            // Given
            String lotId = UUID.randomUUID().toString();
            Lot lot = new Lot(lotId, 10, 10, PRICE_100, LocalDateTime.now(), true);
            
            // When
            holding.addLot(lot);
            
            // Then
            assertEquals(1, holding.getLots().size());
            assertEquals(lotId, holding.getLots().get(0).getId());
            assertEquals(10, holding.getTotalShares());
        }
        
        @Test
        @DisplayName("Should throw exception when adding a duplicate lot")
        void shouldThrowExceptionWhenAddingDuplicateLot() {
            // Given
            String lotId = UUID.randomUUID().toString();
            Lot lot1 = new Lot(lotId, 10, 10, PRICE_100, LocalDateTime.now(), true);
            Lot lot2 = new Lot(lotId, 5, 5, PRICE_120, LocalDateTime.now(), true);
            
            holding.addLot(lot1);
            
            // Then
            assertThrows(EntityExistsException.class, () -> holding.addLot(lot2));
        }
    }

    @Nested
    @DisplayName("Holding Equality and HashCode")
    class HoldingEquality {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreSame() {
            // Given
            String id = UUID.randomUUID().toString();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL")); // Different ticker

            holding1.buy(10, PRICE_100);
            holding2.buy(20, PRICE_120);

            // Then - Same ID means equal, regardless of ticker or lots
            assertEquals(holding1, holding2);
            assertEquals(holding2, holding1);
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Given
            Holding holding1 = Holding.create(GOOGLE);
            Holding holding2 = Holding.create(GOOGLE); // Same ticker, different ID

            // Then
            assertNotEquals(holding1, holding2);
            assertNotEquals(holding2, holding1);
        }

        @Test
        @DisplayName("Should be equal to itself (reflexive)")
        void shouldBeEqualToItself() {
            // Then
            assertEquals(holding, holding);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Then
            assertNotEquals(null, holding);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            String notAHolding = "not a holding";

            // Then
            assertNotEquals(holding, notAHolding);
        }

        @Test
        @DisplayName("Should have same hashCode when equal")
        void shouldHaveSameHashCodeWhenEqual() {
            // Given
            String id = UUID.randomUUID().toString();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));

            holding1.buy(10, PRICE_100);
            holding2.buy(20, PRICE_120);

            // Then
            assertEquals(holding1.hashCode(), holding2.hashCode());
        }

        @Test
        @DisplayName("Should maintain equality after state changes")
        void shouldMaintainEqualityAfterStateChanges() {
            // Given
            String id = UUID.randomUUID().toString();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, GOOGLE);

            // When - Modify state of holding1
            holding1.buy(10, PRICE_100);
            holding1.sell(5, PRICE_110);

            // Then - Still equal because ID hasn't changed
            assertEquals(holding1, holding2);
            assertEquals(holding1.hashCode(), holding2.hashCode());
        }

        @Test
        @DisplayName("Should work correctly in HashSet")
        void shouldWorkCorrectlyInHashSet() {
            // Given
            String id = UUID.randomUUID().toString();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));

            java.util.Set<Holding> holdingSet = new java.util.HashSet<>();

            // When
            holdingSet.add(holding1);
            holdingSet.add(holding2); // Same ID, should not add duplicate

            // Then
            assertEquals(1, holdingSet.size());
            assertTrue(holdingSet.contains(holding1));
            assertTrue(holdingSet.contains(holding2)); // Both should be found
        }

        @Test
        @DisplayName("Should work correctly in HashMap")
        void shouldWorkCorrectlyInHashMap() {
            // Given
            String id = UUID.randomUUID().toString();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));

            java.util.Map<Holding, String> holdingMap = new java.util.HashMap<>();

            // When
            holdingMap.put(holding1, "First");
            holdingMap.put(holding2, "Second"); // Same ID, should replace value

            // Then
            assertEquals(1, holdingMap.size());
            assertEquals("Second", holdingMap.get(holding1));
            assertEquals("Second", holdingMap.get(holding2));
        }
    }
}

