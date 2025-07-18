package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Lot domain entity.
 * Tests focus on business rules and invariants for stock lots.
 */
@DisplayName("Lot Domain Tests")
class LotTest {

    private static final BigDecimal VALID_PRICE = new BigDecimal("100.00");
    private static final LocalDateTime NOW = LocalDateTime.now();
    
    @Nested
    @DisplayName("Lot Creation")
    class LotCreation {
        
        @Test
        @DisplayName("Should create a valid lot with constructor")
        void shouldCreateValidLot() {
            // Given
            String id = UUID.randomUUID().toString();
            int quantity = 10;
            
            // When
            Lot lot = new Lot(id, quantity, quantity, VALID_PRICE, NOW, true);
            
            // Then
            assertEquals(id, lot.getId());
            assertEquals(quantity, lot.getInitialStocks());
            assertEquals(quantity, lot.getRemaining());
            assertEquals(VALID_PRICE, lot.getUnitPrice());
            assertEquals(NOW, lot.getPurchasedAt());
        }
        
        @Test
        @DisplayName("Should throw exception when creating with invalid quantity")
        void shouldThrowExceptionWhenCreatingWithInvalidQuantity() {
            // Given
            String id = UUID.randomUUID().toString();
            
            // Then
            assertThrows(InvalidQuantityException.class, 
                    () -> new Lot(id, 0, 0, VALID_PRICE, NOW, true));
            assertThrows(InvalidQuantityException.class, 
                    () -> new Lot(id, -5, -5, VALID_PRICE, NOW, true));
        }
        
        @Test
        @DisplayName("Should throw exception when creating with invalid price")
        void shouldThrowExceptionWhenCreatingWithInvalidPrice() {
            // Given
            String id = UUID.randomUUID().toString();
            int quantity = 10;
            
            // Then
            assertThrows(InvalidAmountException.class, 
                    () -> new Lot(id, quantity, quantity, BigDecimal.ZERO, NOW, true));
            assertThrows(InvalidAmountException.class, 
                    () -> new Lot(id, quantity, quantity, new BigDecimal("-10.00"), NOW, true));
        }
        
        @Test
        @DisplayName("Should bypass validation when specified")
        void shouldBypassValidationWhenSpecified() {
            // Given
            String id = UUID.randomUUID().toString();
            
            // When - Creating with invalid values but validation=false
            Lot lot = new Lot(id, -5, -5, BigDecimal.ZERO, NOW, false);
            
            // Then - No exceptions thrown
            assertEquals(id, lot.getId());
            assertEquals(-5, lot.getInitialStocks());
            assertEquals(-5, lot.getRemaining());
            assertEquals(BigDecimal.ZERO, lot.getUnitPrice());
        }
    }
    
    @Nested
    @DisplayName("Lot Operations")
    class LotOperations {
        
        @Test
        @DisplayName("Should reduce remaining quantity")
        void shouldReduceRemainingQuantity() {
            // Given
            Lot lot = new Lot(UUID.randomUUID().toString(), 10, 10, VALID_PRICE, NOW, true);
            
            // When
            lot.reduce(3);
            
            // Then
            assertEquals(10, lot.getInitialStocks()); // Initial stocks unchanged
            assertEquals(7, lot.getRemaining());      // Remaining reduced
        }
        
        @Test
        @DisplayName("Should reduce to zero")
        void shouldReduceToZero() {
            // Given
            Lot lot = new Lot(UUID.randomUUID().toString(), 10, 10, VALID_PRICE, NOW, true);
            
            // When
            lot.reduce(10);
            
            // Then
            assertEquals(10, lot.getInitialStocks());
            assertEquals(0, lot.getRemaining());
        }
        
        @Test
        @DisplayName("Should throw exception when reducing by more than remaining")
        void shouldThrowExceptionWhenReducingByMoreThanRemaining() {
            // Given
            Lot lot = new Lot(UUID.randomUUID().toString(), 10, 10, VALID_PRICE, NOW, true);
            
            // Then
            assertThrows(ConflictQuantityException.class, () -> lot.reduce(15));
        }
        
        @Test
        @DisplayName("Should throw exception when reducing already depleted lot")
        void shouldThrowExceptionWhenReducingDepletedLot() {
            // Given
            Lot lot = new Lot(UUID.randomUUID().toString(), 10, 10, VALID_PRICE, NOW, true);
            lot.reduce(10); // Reduce to zero
            
            // Then
            assertThrows(ConflictQuantityException.class, () -> lot.reduce(1));
        }
        
        @Test
        @DisplayName("Should handle multiple reductions")
        void shouldHandleMultipleReductions() {
            // Given
            Lot lot = new Lot(UUID.randomUUID().toString(), 10, 10, VALID_PRICE, NOW, true);
            
            // When
            lot.reduce(3);
            lot.reduce(2);
            lot.reduce(4);
            
            // Then
            assertEquals(10, lot.getInitialStocks());
            assertEquals(1, lot.getRemaining());
        }
    }
}