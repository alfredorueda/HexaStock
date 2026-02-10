package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Holding Domain Tests")
class HoldingTest {

    private static final Ticker GOOGLE = new Ticker("GOOG");
    private static final Price PRICE_100 = Price.of("100.00");
    private static final Price PRICE_120 = Price.of("120.00");
    private static final Price PRICE_90 = Price.of("90.00");
    private static final Price PRICE_110 = Price.of("110.00");

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
            assertNotNull(holding.getId());
            assertEquals(GOOGLE, holding.getTicker());
            assertEquals(ShareQuantity.ZERO, holding.getTotalShares());
            assertTrue(holding.getLots().isEmpty());
        }

        @Test
        @DisplayName("Should create a holding with explicit constructor")
        void shouldCreateHoldingWithExplicitConstructor() {
            HoldingId id = HoldingId.generate();

            Holding explicitHolding = new Holding(id, GOOGLE);

            assertEquals(id, explicitHolding.getId());
            assertEquals(GOOGLE, explicitHolding.getTicker());
            assertEquals(ShareQuantity.ZERO, explicitHolding.getTotalShares());
            assertTrue(explicitHolding.getLots().isEmpty());
        }
    }

    @Nested
    @DisplayName("Buying Operations")
    class BuyingOperations {

        @Test
        @DisplayName("Should add a lot when buying shares")
        void shouldAddLotWhenBuyingShares() {
            holding.buy(ShareQuantity.of(10), PRICE_100);

            assertEquals(ShareQuantity.of(10), holding.getTotalShares());
            assertEquals(1, holding.getLots().size());

            Lot lot = holding.getLots().get(0);
            assertEquals(ShareQuantity.of(10), lot.getInitialShares());
            assertEquals(ShareQuantity.of(10), lot.getRemainingShares());
            assertEquals(PRICE_100, lot.getUnitPrice());
            assertNotNull(lot.getPurchasedAt());
        }

        @Test
        @DisplayName("Should add multiple lots when buying multiple times")
        void shouldAddMultipleLotsWhenBuyingMultipleTimes() {
            holding.buy(ShareQuantity.of(10), PRICE_100);
            holding.buy(ShareQuantity.of(5), PRICE_120);

            assertEquals(ShareQuantity.of(15), holding.getTotalShares());
            assertEquals(2, holding.getLots().size());

            Lot firstLot = holding.getLots().get(0);
            assertEquals(ShareQuantity.of(10), firstLot.getRemainingShares());
            assertEquals(PRICE_100, firstLot.getUnitPrice());

            Lot secondLot = holding.getLots().get(1);
            assertEquals(ShareQuantity.of(5), secondLot.getRemainingShares());
            assertEquals(PRICE_120, secondLot.getUnitPrice());
        }
    }

    @Nested
    @DisplayName("Selling Operations")
    class SellingOperations {

        @Test
        @DisplayName("Should sell shares from oldest lot first (FIFO)")
        void shouldSellSharesFromOldestLotFirst() {
            holding.buy(ShareQuantity.of(10), PRICE_100);
            holding.buy(ShareQuantity.of(5), PRICE_120);

            SellResult result = holding.sell(ShareQuantity.of(8), PRICE_110);

            assertEquals(ShareQuantity.of(7), holding.getTotalShares());
            assertEquals(ShareQuantity.of(2), holding.getLots().get(0).getRemainingShares());
            assertEquals(ShareQuantity.of(5), holding.getLots().get(1).getRemainingShares());

            assertEquals(Money.of("880.00"), result.proceeds());
            assertEquals(Money.of("800.00"), result.costBasis());
            assertEquals(Money.of("80.00"), result.profit());
        }

        @Test
        @DisplayName("Should sell shares across multiple lots if needed")
        void shouldSellSharesAcrossMultipleLots() {
            holding.buy(ShareQuantity.of(10), PRICE_100);
            holding.buy(ShareQuantity.of(5), PRICE_120);

            SellResult result = holding.sell(ShareQuantity.of(12), PRICE_110);

            assertEquals(ShareQuantity.of(3), holding.getTotalShares());
            assertEquals(1, holding.getLots().size());

            Lot remainingLot = holding.getLots().get(0);
            assertEquals(ShareQuantity.of(3), remainingLot.getRemainingShares());
            assertEquals(PRICE_120, remainingLot.getUnitPrice());

            assertEquals(Money.of("1320.00"), result.proceeds());
            assertEquals(Money.of("1240.00"), result.costBasis());
            assertEquals(Money.of("80.00"), result.profit());
        }

        @Test
        @DisplayName("Should throw exception when selling more shares than available")
        void shouldThrowExceptionWhenSellingMoreSharesThanAvailable() {
            holding.buy(ShareQuantity.of(10), PRICE_100);

            assertThrows(ConflictQuantityException.class, () -> holding.sell(ShareQuantity.of(15), PRICE_110));
        }

        @Test
        @DisplayName("Should handle selling at a loss")
        void shouldHandleSellingAtALoss() {
            holding.buy(ShareQuantity.of(10), PRICE_100);

            SellResult result = holding.sell(ShareQuantity.of(5), PRICE_90);

            assertEquals(ShareQuantity.of(5), holding.getTotalShares());
            assertEquals(Money.of("450.00"), result.proceeds());
            assertEquals(Money.of("500.00"), result.costBasis());
            assertEquals(Money.of("-50.00"), result.profit());
        }

        @Test
        @DisplayName("Should correctly sell shares across multiple lots with FIFO")
        void testSellStockWithMultipleLots() {
            holding.buy(ShareQuantity.of(10), Price.of("100.00"));
            holding.buy(ShareQuantity.of(15), Price.of("105.00"));
            holding.buy(ShareQuantity.of(20), Price.of("110.00"));
            holding.buy(ShareQuantity.of(25), Price.of("115.00"));
            holding.buy(ShareQuantity.of(30), Price.of("120.00"));

            ShareQuantity sharesToSell = ShareQuantity.of(50);
            Price sellPrice = Price.of("130.00");
            SellResult result = holding.sell(sharesToSell, sellPrice);

            assertEquals(2, holding.getLots().size());
            Lot lot4 = holding.getLots().get(0);
            Lot lot5 = holding.getLots().get(1);
            assertEquals(ShareQuantity.of(20), lot4.getRemainingShares());
            assertEquals(Price.of("115.00"), lot4.getUnitPrice());
            assertEquals(ShareQuantity.of(30), lot5.getRemainingShares());
            assertEquals(Price.of("120.00"), lot5.getUnitPrice());
            assertEquals(Money.of("1150.00"), result.profit());
        }
    }

    @Nested
    @DisplayName("Lot Management")
    class LotManagement {

        @Test
        @DisplayName("Should add a lot explicitly")
        void shouldAddLotExplicitly() {
            LotId lotId = LotId.generate();
            Lot lot = new Lot(lotId, ShareQuantity.of(10), ShareQuantity.of(10), PRICE_100, LocalDateTime.now());

            holding.addLot(lot);

            assertEquals(1, holding.getLots().size());
            assertEquals(lotId, holding.getLots().get(0).getId());
            assertEquals(ShareQuantity.of(10), holding.getTotalShares());
        }

        @Test
        @DisplayName("Should throw exception when adding a duplicate lot")
        void shouldThrowExceptionWhenAddingDuplicateLot() {
            LotId lotId = LotId.generate();
            Lot lot1 = new Lot(lotId, ShareQuantity.of(10), ShareQuantity.of(10), PRICE_100, LocalDateTime.now());
            Lot lot2 = new Lot(lotId, ShareQuantity.of(5), ShareQuantity.of(5), PRICE_120, LocalDateTime.now());

            holding.addLot(lot1);

            assertThrows(EntityExistsException.class, () -> holding.addLot(lot2));
        }
    }

    @Nested
    @DisplayName("Holding Equality and HashCode")
    class HoldingEquality {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreSame() {
            HoldingId id = HoldingId.generate();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));
            holding1.buy(ShareQuantity.of(10), PRICE_100);
            holding2.buy(ShareQuantity.of(20), PRICE_120);

            assertEquals(holding1, holding2);
            assertEquals(holding2, holding1);
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            Holding holding1 = Holding.create(GOOGLE);
            Holding holding2 = Holding.create(GOOGLE);

            assertNotEquals(holding1, holding2);
            assertNotEquals(holding2, holding1);
        }

        @Test
        @DisplayName("Should be equal to itself (reflexive)")
        void shouldBeEqualToItself() {
            assertEquals(holding, holding);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertNotEquals(null, holding);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            String notAHolding = "not a holding";
            assertNotEquals(holding, notAHolding);
        }

        @Test
        @DisplayName("Should have same hashCode when equal")
        void shouldHaveSameHashCodeWhenEqual() {
            HoldingId id = HoldingId.generate();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));
            holding1.buy(ShareQuantity.of(10), PRICE_100);
            holding2.buy(ShareQuantity.of(20), PRICE_120);

            assertEquals(holding1.hashCode(), holding2.hashCode());
        }

        @Test
        @DisplayName("Should maintain equality after state changes")
        void shouldMaintainEqualityAfterStateChanges() {
            HoldingId id = HoldingId.generate();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, GOOGLE);

            holding1.buy(ShareQuantity.of(10), PRICE_100);
            holding1.sell(ShareQuantity.of(5), PRICE_110);

            assertEquals(holding1, holding2);
            assertEquals(holding1.hashCode(), holding2.hashCode());
        }

        @Test
        @DisplayName("Should work correctly in HashSet")
        void shouldWorkCorrectlyInHashSet() {
            HoldingId id = HoldingId.generate();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));
            java.util.Set<Holding> holdingSet = new java.util.HashSet<>();

            holdingSet.add(holding1);
            holdingSet.add(holding2);

            assertEquals(1, holdingSet.size());
            assertTrue(holdingSet.contains(holding1));
            assertTrue(holdingSet.contains(holding2));
        }

        @Test
        @DisplayName("Should work correctly in HashMap")
        void shouldWorkCorrectlyInHashMap() {
            HoldingId id = HoldingId.generate();
            Holding holding1 = new Holding(id, GOOGLE);
            Holding holding2 = new Holding(id, new Ticker("AAPL"));
            java.util.Map<Holding, String> holdingMap = new java.util.HashMap<>();

            holdingMap.put(holding1, "First");
            holdingMap.put(holding2, "Second");

            assertEquals(1, holdingMap.size());
            assertEquals("Second", holdingMap.get(holding1));
            assertEquals("Second", holdingMap.get(holding2));
        }
    }
}

