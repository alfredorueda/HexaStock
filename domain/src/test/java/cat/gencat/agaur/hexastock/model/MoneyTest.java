package cat.gencat.agaur.hexastock.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Money value object.
 * Tests focus on ensuring immutability, proper validation, and correct financial calculations.
 */
@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Nested
    @DisplayName("Money Creation")
    class MoneyCreation {

        @Test
        @DisplayName("Should create money with valid string amount")
        void shouldCreateMoneyWithValidStringAmount() {
            Money money = Money.of("100.50");

            assertEquals(new BigDecimal("100.50"), money.amount());
        }

        @Test
        @DisplayName("Should create money with valid BigDecimal amount")
        void shouldCreateMoneyWithValidBigDecimalAmount() {
            Money money = Money.of(new BigDecimal("100.50"));

            assertEquals(new BigDecimal("100.50"), money.amount());
        }

        @Test
        @DisplayName("Should create money with valid double amount")
        void shouldCreateMoneyWithValidDoubleAmount() {
            Money money = Money.of(100.50);

            assertEquals(new BigDecimal("100.50"), money.amount());
        }

        @Test
        @DisplayName("Should create zero money")
        void shouldCreateZeroMoney() {
            assertEquals(0, Money.ZERO.amount().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(NullPointerException.class,
                    () -> Money.of((BigDecimal) null));
        }
    }

    @Nested
    @DisplayName("Money Operations")
    class MoneyOperations {

        @Test
        @DisplayName("Should multiply money by an integer")
        void shouldMultiplyMoneyByInteger() {
            Money money = Money.of("10.50");

            Money result = money.multiply(3);

            assertEquals(new BigDecimal("31.50"), result.amount());
            // Original money is unchanged (immutability)
            assertEquals(new BigDecimal("10.50"), money.amount());
        }

        @Test
        @DisplayName("Should add money")
        void shouldAddMoney() {
            Money money1 = Money.of("10.50");
            Money money2 = Money.of("20.75");

            Money result = money1.add(money2);

            assertEquals(new BigDecimal("31.25"), result.amount());
            // Original money is unchanged (immutability)
            assertEquals(new BigDecimal("10.50"), money1.amount());
            assertEquals(new BigDecimal("20.75"), money2.amount());
        }

        @Test
        @DisplayName("Should subtract money")
        void shouldSubtractMoney() {
            Money money1 = Money.of("30.00");
            Money money2 = Money.of("10.50");

            Money result = money1.subtract(money2);

            assertEquals(new BigDecimal("19.50"), result.amount());
        }

        @Test
        @DisplayName("Should check if positive")
        void shouldCheckIfPositive() {
            assertTrue(Money.of("10.00").isPositive());
            assertFalse(Money.ZERO.isPositive());
            assertFalse(Money.of("-10.00").isPositive());
        }

        @Test
        @DisplayName("Should check if negative")
        void shouldCheckIfNegative() {
            assertTrue(Money.of("-10.00").isNegative());
            assertFalse(Money.ZERO.isNegative());
            assertFalse(Money.of("10.00").isNegative());
        }

        @Test
        @DisplayName("Should check if greater than or equal")
        void shouldCheckIfGreaterThanOrEqual() {
            Money money1 = Money.of("100.00");
            Money money2 = Money.of("50.00");
            Money money3 = Money.of("100.00");

            assertTrue(money1.isGreaterThanOrEqual(money2));
            assertTrue(money1.isGreaterThanOrEqual(money3));
            assertFalse(money2.isGreaterThanOrEqual(money1));
        }
    }

    @Nested
    @DisplayName("Money Equality")
    class MoneyEquality {

        @Test
        @DisplayName("Should equal money with same amount")
        void shouldEqualMoneyWithSameAmount() {
            Money money1 = Money.of("100.00");
            Money money2 = Money.of("100.00");

            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("Should not equal money with different amount")
        void shouldNotEqualMoneyWithDifferentAmount() {
            Money money1 = Money.of("100.00");
            Money money2 = Money.of("100.01");

            assertNotEquals(money1, money2);
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            Money money = Money.of("100.00");

            assertNotEquals(null, money);
        }

        @Test
        @DisplayName("Should not equal different type")
        void shouldNotEqualDifferentType() {
            Money money = Money.of("100.00");

            assertNotEquals("100.00", money);
        }
    }
}