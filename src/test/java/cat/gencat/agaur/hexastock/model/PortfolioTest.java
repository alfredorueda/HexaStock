package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Portfolio domain entity.
 * Tests focus on business rules and invariants without any infrastructure dependencies.
 */
@DisplayName("Portfolio Domain Tests")
class PortfolioTest {

    private static final Ticker APPLE = new Ticker("AAPL");
    private static final Ticker MICROSOFT = new Ticker("MSFT");
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private static final Currency USD = Currency.getInstance("USD");
    
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(
                UUID.randomUUID().toString(),
                "John Doe",
                INITIAL_BALANCE,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Portfolio Creation")
    class PortfolioCreation {
        
        @Test
        @DisplayName("Should create a portfolio with a factory method")
        void shouldCreatePortfolioWithFactoryMethod() {
            // When
            Portfolio newPortfolio = Portfolio.create("Jane Doe");
            
            // Then
            assertNotNull(newPortfolio.getId());
            assertEquals("Jane Doe", newPortfolio.getOwnerName());
            assertEquals(BigDecimal.ZERO, newPortfolio.getBalance());
            assertNotNull(newPortfolio.getCreatedAt());
            assertTrue(newPortfolio.getHoldings().isEmpty());
        }
    }

    @Nested
    @DisplayName("Cash Operations")
    class CashOperations {
        
        @Test
        @DisplayName("Should increase balance when depositing money")
        void shouldIncreaseBalanceWhenDepositing() {
            // Given
            Money deposit = Money.of(USD, new BigDecimal("500.00"));
            
            // When
            portfolio.deposit(deposit);
            
            // Then
            assertEquals(new BigDecimal("1500.00"), portfolio.getBalance());
        }
        
        @Test
        @DisplayName("Should throw exception when depositing zero or negative amount")
        void shouldThrowExceptionWhenDepositingZeroOrNegative() {
            // Given
            Money zeroDeposit = Money.of(USD, BigDecimal.ZERO);
            Money negativeDeposit = Money.of(USD, new BigDecimal("-100.00"));
            
            // Then
            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(zeroDeposit));
            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(negativeDeposit));
        }
        
        @Test
        @DisplayName("Should decrease balance when withdrawing money")
        void shouldDecreaseBalanceWhenWithdrawing() {
            // Given
            Money withdrawal = Money.of(USD, new BigDecimal("300.00"));
            
            // When
            portfolio.withdraw(withdrawal);
            
            // Then
            assertEquals(new BigDecimal("700.00"), portfolio.getBalance());
        }
        
        @Test
        @DisplayName("Should throw exception when withdrawing zero or negative amount")
        void shouldThrowExceptionWhenWithdrawingZeroOrNegative() {
            // Given
            Money zeroWithdrawal = Money.of(USD, BigDecimal.ZERO);
            Money negativeWithdrawal = Money.of(USD, new BigDecimal("-100.00"));
            
            // Then
            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(zeroWithdrawal));
            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(negativeWithdrawal));
        }
        
        @Test
        @DisplayName("Should throw exception when withdrawing more than balance")
        void shouldThrowExceptionWhenWithdrawingMoreThanBalance() {
            // Given
            Money excessiveWithdrawal = Money.of(USD, new BigDecimal("1500.00"));
            
            // Then
            assertThrows(InsufficientFundsException.class, () -> portfolio.withdraw(excessiveWithdrawal));
        }
    }

    @Nested
    @DisplayName("Stock Operations")
    class StockOperations {
        
        @Test
        @DisplayName("Should add holding and decrease balance when buying stock")
        void shouldAddHoldingAndDecreaseBalanceWhenBuying() {
            // Given
            int quantity = 10;
            BigDecimal price = new BigDecimal("50.00");
            
            // When
            portfolio.buy(APPLE, quantity, price);
            
            // Then
            assertEquals(new BigDecimal("500.00"), portfolio.getBalance());
            assertEquals(1, portfolio.getHoldings().size());
            
            Holding appleHolding = portfolio.getHoldings().get(0);
            assertEquals(APPLE, appleHolding.getTicker());
            assertEquals(10, appleHolding.getTotalShares());
            assertEquals(1, appleHolding.getLots().size());
            
            Lot appleLot = appleHolding.getLots().get(0);
            assertEquals(10, appleLot.getInitialStocks());
            assertEquals(10, appleLot.getRemaining());
            assertEquals(price, appleLot.getUnitPrice());
        }
        
        @Test
        @DisplayName("Should add lot to existing holding when buying more of same stock")
        void shouldAddLotToExistingHoldingWhenBuyingMore() {
            // Given
            portfolio.buy(APPLE, 5, new BigDecimal("50.00"));
            
            // When
            portfolio.buy(APPLE, 3, new BigDecimal("55.00"));
            
            // Then
            assertEquals(new BigDecimal("585.00"), portfolio.getBalance());
            assertEquals(1, portfolio.getHoldings().size());
            
            Holding appleHolding = portfolio.getHoldings().get(0);
            assertEquals(8, appleHolding.getTotalShares());
            assertEquals(2, appleHolding.getLots().size());
        }
        
        @Test
        @DisplayName("Should throw exception when buying with insufficient funds")
        void shouldThrowExceptionWhenBuyingWithInsufficientFunds() {
            // Given
            int quantity = 25;
            BigDecimal price = new BigDecimal("50.00");
            
            // Then
            assertThrows(InsufficientFundsException.class, () -> portfolio.buy(APPLE, quantity, price));
        }
        
        @Test
        @DisplayName("Should throw exception when buying with invalid quantity")
        void shouldThrowExceptionWhenBuyingWithInvalidQuantity() {
            // Given
            int negativeQuantity = -5;
            int zeroQuantity = 0;
            BigDecimal price = new BigDecimal("50.00");
            
            // Then
            assertThrows(InvalidQuantityException.class, () -> portfolio.buy(APPLE, negativeQuantity, price));
            assertThrows(InvalidQuantityException.class, () -> portfolio.buy(APPLE, zeroQuantity, price));
        }
        
        @Test
        @DisplayName("Should throw exception when buying with invalid price")
        void shouldThrowExceptionWhenBuyingWithInvalidPrice() {
            // Given
            int quantity = 5;
            BigDecimal zeroPrice = BigDecimal.ZERO;
            BigDecimal negativePrice = new BigDecimal("-10.00");
            
            // Then
            assertThrows(InvalidAmountException.class, () -> portfolio.buy(APPLE, quantity, zeroPrice));
            assertThrows(InvalidAmountException.class, () -> portfolio.buy(APPLE, quantity, negativePrice));
        }
        
        @Test
        @DisplayName("Should increase balance and update holding when selling stock")
        void shouldIncreaseBalanceAndUpdateHoldingWhenSelling() {
            // Given
            portfolio.buy(APPLE, 10, new BigDecimal("50.00"));
            BigDecimal sellPrice = new BigDecimal("60.00");
            
            // When
            SellResult result = portfolio.sell(APPLE, 5, sellPrice);
            
            // Then
            assertEquals(new BigDecimal("800.00"), portfolio.getBalance());
            assertEquals(5, portfolio.getHoldings().get(0).getTotalShares());
            assertEquals(new BigDecimal("300.00"), result.proceeds());
            assertEquals(new BigDecimal("250.00"), result.costBasis());
            assertEquals(new BigDecimal("50.00"), result.profit());
        }
        
        @Test
        @DisplayName("Should throw exception when selling stock not in portfolio")
        void shouldThrowExceptionWhenSellingStockNotInPortfolio() {
            // Then
            assertThrows(DomainException.class, () -> portfolio.sell(MICROSOFT, 5, new BigDecimal("100.00")));
        }
        
        @Test
        @DisplayName("Should throw exception when selling invalid quantity")
        void shouldThrowExceptionWhenSellingInvalidQuantity() {
            // Given
            portfolio.buy(APPLE, 10, new BigDecimal("50.00"));
            
            // Then
            assertThrows(InvalidQuantityException.class, 
                    () -> portfolio.sell(APPLE, 0, new BigDecimal("60.00")));
            assertThrows(InvalidQuantityException.class, 
                    () -> portfolio.sell(APPLE, -5, new BigDecimal("60.00")));
        }
        
        @Test
        @DisplayName("Should throw exception when selling more shares than owned")
        void shouldThrowExceptionWhenSellingMoreSharesThanOwned() {
            // Given
            portfolio.buy(APPLE, 10, new BigDecimal("50.00"));
            
            // Then
            assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(APPLE, 15, new BigDecimal("60.00")));
        }
    }

    @Nested
    @DisplayName("Portfolio Management")
    class PortfolioManagement {
        
        @Test
        @DisplayName("Should add a holding to portfolio")
        void shouldAddHoldingToPortfolio() {
            // Given
            Holding holding = Holding.create(MICROSOFT);
            
            // When
            portfolio.addHolding(holding);
            
            // Then
            assertEquals(1, portfolio.getHoldings().size());
            assertEquals(MICROSOFT, portfolio.getHoldings().get(0).getTicker());
        }
        
        @Test
        @DisplayName("Should throw exception when adding duplicate holding")
        void shouldThrowExceptionWhenAddingDuplicateHolding() {
            // Given
            Holding holding1 = Holding.create(MICROSOFT);
            Holding holding2 = Holding.create(MICROSOFT);
            
            portfolio.addHolding(holding1);
            
            // Then
            assertThrows(EntityExistsException.class, () -> portfolio.addHolding(holding2));
        }
    }
}