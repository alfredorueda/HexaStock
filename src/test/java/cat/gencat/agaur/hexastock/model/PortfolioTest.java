package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Portfolio Domain Tests")
class PortfolioTest {

    private static final Ticker APPLE = new Ticker("AAPL");
    private static final Ticker MICROSOFT = new Ticker("MSFT");
    private static final Money INITIAL_BALANCE = Money.of("1000.00");

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(
                PortfolioId.generate(),
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
            Portfolio newPortfolio = Portfolio.create("Jane Doe");

            assertNotNull(newPortfolio.getId());
            assertEquals("Jane Doe", newPortfolio.getOwnerName());
            assertEquals(Money.ZERO, newPortfolio.getBalance());
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
            Money deposit = Money.of("500.00");

            portfolio.deposit(deposit);

            assertEquals(Money.of("1500.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when depositing zero or negative amount")
        void shouldThrowExceptionWhenDepositingZeroOrNegative() {
            Money zeroDeposit = Money.ZERO;
            Money negativeDeposit = Money.of("-100.00");

            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(zeroDeposit));
            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(negativeDeposit));
        }

        @Test
        @DisplayName("Should decrease balance when withdrawing money")
        void shouldDecreaseBalanceWhenWithdrawing() {
            Money withdrawal = Money.of("300.00");

            portfolio.withdraw(withdrawal);

            assertEquals(Money.of("700.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when withdrawing zero or negative amount")
        void shouldThrowExceptionWhenWithdrawingZeroOrNegative() {
            Money zeroWithdrawal = Money.ZERO;
            Money negativeWithdrawal = Money.of("-100.00");

            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(zeroWithdrawal));
            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(negativeWithdrawal));
        }

        @Test
        @DisplayName("Should throw exception when withdrawing more than balance")
        void shouldThrowExceptionWhenWithdrawingMoreThanBalance() {
            Money excessiveWithdrawal = Money.of("1500.00");

            assertThrows(InsufficientFundsException.class, () -> portfolio.withdraw(excessiveWithdrawal));
        }
    }

    @Nested
    @DisplayName("Stock Operations")
    class StockOperations {

        @Test
        @DisplayName("Should add holding and decrease balance when buying stock")
        void shouldAddHoldingAndDecreaseBalanceWhenBuying() {
            ShareQuantity quantity = ShareQuantity.of(10);
            Price price = Price.of("50.00");

            portfolio.buy(APPLE, quantity, price);

            assertEquals(Money.of("500.00"), portfolio.getBalance());
            assertEquals(1, portfolio.getHoldings().size());

            Holding appleHolding = portfolio.getHoldings().get(0);
            assertEquals(APPLE, appleHolding.getTicker());
            assertEquals(ShareQuantity.of(10), appleHolding.getTotalShares());
            assertEquals(1, appleHolding.getLots().size());

            Lot appleLot = appleHolding.getLots().get(0);
            assertEquals(ShareQuantity.of(10), appleLot.getInitialShares());
            assertEquals(ShareQuantity.of(10), appleLot.getRemainingShares());
            assertEquals(price, appleLot.getUnitPrice());
        }

        @Test
        @DisplayName("Should add lot to existing holding when buying more of same stock")
        void shouldAddLotToExistingHoldingWhenBuyingMore() {
            portfolio.buy(APPLE, ShareQuantity.of(5), Price.of("50.00"));

            portfolio.buy(APPLE, ShareQuantity.of(3), Price.of("55.00"));

            assertEquals(Money.of("585.00"), portfolio.getBalance());
            assertEquals(1, portfolio.getHoldings().size());

            Holding appleHolding = portfolio.getHoldings().get(0);
            assertEquals(ShareQuantity.of(8), appleHolding.getTotalShares());
            assertEquals(2, appleHolding.getLots().size());
        }

        @Test
        @DisplayName("Should throw exception when buying with insufficient funds")
        void shouldThrowExceptionWhenBuyingWithInsufficientFunds() {
            ShareQuantity quantity = ShareQuantity.of(25);
            Price price = Price.of("50.00");

            assertThrows(InsufficientFundsException.class, () -> portfolio.buy(APPLE, quantity, price));
        }

        @Test
        @DisplayName("Should throw exception when buying with invalid quantity")
        void shouldThrowExceptionWhenBuyingWithInvalidQuantity() {
            ShareQuantity zeroQuantity = ShareQuantity.ZERO;
            Price price = Price.of("50.00");

            assertThrows(InvalidQuantityException.class, () -> portfolio.buy(APPLE, zeroQuantity, price));
        }

        @Test
        @DisplayName("Should increase balance and update holding when selling stock")
        void shouldIncreaseBalanceAndUpdateHoldingWhenSelling() {
            portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("50.00"));
            Price sellPrice = Price.of("60.00");

            SellResult result = portfolio.sell(APPLE, ShareQuantity.of(5), sellPrice);

            assertEquals(Money.of("800.00"), portfolio.getBalance());
            assertEquals(ShareQuantity.of(5), portfolio.getHoldings().get(0).getTotalShares());
            assertEquals(Money.of("300.00"), result.proceeds());
            assertEquals(Money.of("250.00"), result.costBasis());
            assertEquals(Money.of("50.00"), result.profit());
        }

        @Test
        @DisplayName("Should throw exception when selling stock not in portfolio")
        void shouldThrowExceptionWhenSellingStockNotInPortfolio() {
            assertThrows(HoldingNotFoundException.class, () -> portfolio.sell(MICROSOFT, ShareQuantity.of(5), Price.of("100.00")));
        }

        @Test
        @DisplayName("Should throw exception when selling invalid quantity")
        void shouldThrowExceptionWhenSellingInvalidQuantity() {
            portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("50.00"));

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(APPLE, ShareQuantity.ZERO, Price.of("60.00")));
        }

        @Test
        @DisplayName("Should throw exception when selling more shares than owned")
        void shouldThrowExceptionWhenSellingMoreSharesThanOwned() {
            portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("50.00"));

            assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(APPLE, ShareQuantity.of(15), Price.of("60.00")));
        }
    }

    @Nested
    @DisplayName("Portfolio Management")
    class PortfolioManagement {

        @Test
        @DisplayName("Should add a holding to portfolio")
        void shouldAddHoldingToPortfolio() {
            Holding holding = Holding.create(MICROSOFT);

            portfolio.addHolding(holding);

            assertEquals(1, portfolio.getHoldings().size());
            assertEquals(MICROSOFT, portfolio.getHoldings().get(0).getTicker());
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate holding")
        void shouldThrowExceptionWhenAddingDuplicateHolding() {
            Holding holding1 = Holding.create(MICROSOFT);
            Holding holding2 = Holding.create(MICROSOFT);

            portfolio.addHolding(holding1);

            assertThrows(EntityExistsException.class, () -> portfolio.addHolding(holding2));
        }
    }
}