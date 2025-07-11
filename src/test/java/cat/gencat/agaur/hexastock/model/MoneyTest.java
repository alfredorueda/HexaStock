package cat.gencat.agaur.hexastock.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Money value object.
 * Tests focus on ensuring immutability, proper validation, and correct financial calculations.
 */
@DisplayName("Money Value Object Tests")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency JPY = Currency.getInstance("JPY");
    
    @Nested
    @DisplayName("Money Creation")
    class MoneyCreation {
        
        @Test
        @DisplayName("Should create money with valid amount")
        void shouldCreateMoneyWithValidAmount() {
            // When
            Money money = Money.of(USD, new BigDecimal("100.50"));
            
            // Then
            assertEquals(USD, money.currency());
            assertEquals(new BigDecimal("100.50"), money.amount());
        }
        
        @Test
        @DisplayName("Should throw exception when currency is null")
        void shouldThrowExceptionWhenCurrencyIsNull() {
            // Then
            assertThrows(NullPointerException.class, 
                    () -> Money.of(null, new BigDecimal("100.00")));
        }
        
        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            // Then
            assertThrows(NullPointerException.class, 
                    () -> Money.of(USD, null));
        }
        
        @Test
        @DisplayName("Should throw exception when amount scale exceeds currency's fraction digits")
        void shouldThrowExceptionWhenAmountScaleExceedsCurrencyFractionDigits() {
            // Given
            Currency jpy = Currency.getInstance("JPY"); // JPY has 0 fraction digits
            BigDecimal amountWithScale = new BigDecimal("100.1"); // Has 1 fraction digit
            
            // Then
            assertThrows(IllegalArgumentException.class, 
                    () -> Money.of(jpy, amountWithScale));
        }
        
        @Test
        @DisplayName("Should create money using major and minor parts")
        void shouldCreateMoneyUsingMajorAndMinorParts() {
            // When
            Money money = Money.of(USD, 100, 50);
            
            // Then
            assertEquals(USD, money.currency());
            assertEquals(new BigDecimal("100.50"), money.amount());
        }
        
        @Test
        @DisplayName("Should handle different currency scales correctly")
        void shouldHandleDifferentCurrencyScalesCorrectly() {
            // When
            Money usdMoney = Money.of(USD, 100, 50); // USD has 2 fraction digits
            Money jpyMoney = Money.of(JPY, 100, 0);  // JPY has 0 fraction digits
            
            // Then
            assertEquals(new BigDecimal("100.50"), usdMoney.amount());
            assertEquals(new BigDecimal("100"), jpyMoney.amount());
        }
    }
    
    @Nested
    @DisplayName("Money Operations")
    class MoneyOperations {
        
        @Test
        @DisplayName("Should multiply money by an integer")
        void shouldMultiplyMoneyByInteger() {
            // Given
            Money money = Money.of(USD, new BigDecimal("10.50"));
            
            // When
            Money result = money.multiply(3);
            
            // Then
            assertEquals(USD, result.currency());
            assertEquals(new BigDecimal("31.50"), result.amount());
            
            // Original money is unchanged (immutability)
            assertEquals(new BigDecimal("10.50"), money.amount());
        }
        
        @Test
        @DisplayName("Should add money of same currency")
        void shouldAddMoneyOfSameCurrency() {
            // Given
            Money money1 = Money.of(USD, new BigDecimal("10.50"));
            Money money2 = Money.of(USD, new BigDecimal("20.75"));
            
            // When
            Money result = money1.add(money2);
            
            // Then
            assertEquals(USD, result.currency());
            assertEquals(new BigDecimal("31.25"), result.amount());
            
            // Original money is unchanged (immutability)
            assertEquals(new BigDecimal("10.50"), money1.amount());
            assertEquals(new BigDecimal("20.75"), money2.amount());
        }
        
        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            // Given
            Money usdMoney = Money.of(USD, new BigDecimal("10.50"));
            Money eurMoney = Money.of(EUR, new BigDecimal("20.75"));
            
            // Then
            assertThrows(IllegalArgumentException.class, 
                    () -> usdMoney.add(eurMoney));
        }
    }
    
    @Nested
    @DisplayName("Money Equality")
    class MoneyEquality {
        
        @Test
        @DisplayName("Should equal money with same currency and amount")
        void shouldEqualMoneyWithSameCurrencyAndAmount() {
            // Given
            Money money1 = Money.of(USD, new BigDecimal("100.00"));
            Money money2 = Money.of(USD, new BigDecimal("100.00"));
            
            // Then
            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }
        
        @Test
        @DisplayName("Should not equal money with different currency")
        void shouldNotEqualMoneyWithDifferentCurrency() {
            // Given
            Money usdMoney = Money.of(USD, new BigDecimal("100.00"));
            Money eurMoney = Money.of(EUR, new BigDecimal("100.00"));
            
            // Then
            assertNotEquals(usdMoney, eurMoney);
        }
        
        @Test
        @DisplayName("Should not equal money with different amount")
        void shouldNotEqualMoneyWithDifferentAmount() {
            // Given
            Money money1 = Money.of(USD, new BigDecimal("100.00"));
            Money money2 = Money.of(USD, new BigDecimal("100.01"));
            
            // Then
            assertNotEquals(money1, money2);
        }
    }
}