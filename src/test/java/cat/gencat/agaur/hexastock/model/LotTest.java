package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lot Domain Tests")
class LotTest {

    private static final Price VALID_PRICE = Price.of("100.00");
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Nested
    @DisplayName("Lot Creation")
    class LotCreation {

        @Test
        @DisplayName("Should create a valid lot with constructor")
        void shouldCreateValidLot() {
            LotId id = LotId.generate();
            ShareQuantity quantity = ShareQuantity.of(10);

            Lot lot = new Lot(id, quantity, quantity, VALID_PRICE, NOW);

            assertEquals(id, lot.getId());
            assertEquals(quantity, lot.getInitialShares());
            assertEquals(quantity, lot.getRemainingShares());
            assertEquals(VALID_PRICE, lot.getUnitPrice());
            assertEquals(NOW, lot.getPurchasedAt());
        }

        @Test
        @DisplayName("Should create lot with factory method")
        void shouldCreateLotWithFactoryMethod() {
            ShareQuantity quantity = ShareQuantity.of(10);

            Lot lot = Lot.create(quantity, VALID_PRICE);

            assertNotNull(lot.getId());
            assertEquals(quantity, lot.getInitialShares());
            assertEquals(quantity, lot.getRemainingShares());
            assertEquals(VALID_PRICE, lot.getUnitPrice());
            assertNotNull(lot.getPurchasedAt());
        }

        @Test
        @DisplayName("Should throw exception when creating with zero quantity")
        void shouldThrowExceptionWhenCreatingWithZeroQuantity() {
            assertThrows(InvalidQuantityException.class,
                    () -> Lot.create(ShareQuantity.ZERO, VALID_PRICE));
        }
    }

    @Nested
    @DisplayName("Lot Operations")
    class LotOperations {

        @Test
        @DisplayName("Should reduce remaining quantity")
        void shouldReduceRemainingQuantity() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);

            lot.reduce(ShareQuantity.of(3));

            assertEquals(ShareQuantity.of(10), lot.getInitialShares());
            assertEquals(ShareQuantity.of(7), lot.getRemainingShares());
        }

        @Test
        @DisplayName("Should reduce to zero")
        void shouldReduceToZero() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);

            lot.reduce(ShareQuantity.of(10));

            assertEquals(ShareQuantity.of(10), lot.getInitialShares());
            assertEquals(ShareQuantity.ZERO, lot.getRemainingShares());
            assertTrue(lot.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when reducing by more than remaining")
        void shouldThrowExceptionWhenReducingByMoreThanRemaining() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);

            assertThrows(ConflictQuantityException.class, () -> lot.reduce(ShareQuantity.of(15)));
        }

        @Test
        @DisplayName("Should throw exception when reducing already depleted lot")
        void shouldThrowExceptionWhenReducingDepletedLot() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);
            lot.reduce(ShareQuantity.of(10));

            assertThrows(ConflictQuantityException.class, () -> lot.reduce(ShareQuantity.of(1)));
        }

        @Test
        @DisplayName("Should handle multiple reductions")
        void shouldHandleMultipleReductions() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);

            lot.reduce(ShareQuantity.of(3));
            lot.reduce(ShareQuantity.of(2));
            lot.reduce(ShareQuantity.of(4));

            assertEquals(ShareQuantity.of(10), lot.getInitialShares());
            assertEquals(ShareQuantity.of(1), lot.getRemainingShares());
        }

        @Test
        @DisplayName("Should calculate cost basis correctly")
        void shouldCalculateCostBasisCorrectly() {
            Lot lot = Lot.create(ShareQuantity.of(10), Price.of("50.00"));

            Money costBasis = lot.calculateCostBasis(ShareQuantity.of(5));

            assertEquals(Money.of("250.00"), costBasis);
        }
    }

    @Nested
    @DisplayName("Lot Equality and HashCode")
    class LotEquality {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreSame() {
            LotId id = LotId.generate();
            Lot lot1 = new Lot(id, ShareQuantity.of(10), ShareQuantity.of(10), VALID_PRICE, NOW);
            Lot lot2 = new Lot(id, ShareQuantity.of(20), ShareQuantity.of(15), Price.of("200.00"), NOW.plusDays(1));

            assertEquals(lot1, lot2);
            assertEquals(lot2, lot1);
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            Lot lot1 = Lot.create(ShareQuantity.of(10), VALID_PRICE);
            Lot lot2 = Lot.create(ShareQuantity.of(10), VALID_PRICE);

            assertNotEquals(lot1, lot2);
            assertNotEquals(lot2, lot1);
        }

        @Test
        @DisplayName("Should be equal to itself (reflexive)")
        void shouldBeEqualToItself() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);
            assertEquals(lot, lot);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);
            assertNotEquals(null, lot);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Lot lot = Lot.create(ShareQuantity.of(10), VALID_PRICE);
            String notALot = "not a lot";
            assertNotEquals(lot, notALot);
        }

        @Test
        @DisplayName("Should have same hashCode when equal")
        void shouldHaveSameHashCodeWhenEqual() {
            LotId id = LotId.generate();
            Lot lot1 = new Lot(id, ShareQuantity.of(10), ShareQuantity.of(10), VALID_PRICE, NOW);
            Lot lot2 = new Lot(id, ShareQuantity.of(20), ShareQuantity.of(15), Price.of("200.00"), NOW.plusDays(1));

            assertEquals(lot1.hashCode(), lot2.hashCode());
        }

        @Test
        @DisplayName("Should maintain equality after state changes")
        void shouldMaintainEqualityAfterStateChanges() {
            LotId id = LotId.generate();
            Lot lot1 = new Lot(id, ShareQuantity.of(10), ShareQuantity.of(10), VALID_PRICE, NOW);
            Lot lot2 = new Lot(id, ShareQuantity.of(10), ShareQuantity.of(10), VALID_PRICE, NOW);

            lot1.reduce(ShareQuantity.of(5));

            assertEquals(lot1, lot2);
            assertEquals(lot1.hashCode(), lot2.hashCode());
        }

        @Test
        @DisplayName("Should work correctly in HashSet")
        void shouldWorkCorrectlyInHashSet() {
            LotId id = LotId.generate();
            Lot lot1 = new Lot(id, ShareQuantity.of(10), ShareQuantity.of(10), VALID_PRICE, NOW);
            Lot lot2 = new Lot(id, ShareQuantity.of(20), ShareQuantity.of(15), Price.of("200.00"), NOW.plusDays(1));
            java.util.Set<Lot> lotSet = new java.util.HashSet<>();

            lotSet.add(lot1);
            lotSet.add(lot2);

            assertEquals(1, lotSet.size());
            assertTrue(lotSet.contains(lot1));
            assertTrue(lotSet.contains(lot2));
        }
    }
}

