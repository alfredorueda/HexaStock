package cat.gencat.agaur.hexastock.model.portfolio;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.model.money.*;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
        @SpecificationRef(value = "US-01.AC-1", level = TestLevel.DOMAIN, feature = "create-portfolio.feature")
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
        @SpecificationRef(value = "US-04.AC-1", level = TestLevel.DOMAIN, feature = "deposit-funds.feature")
        void shouldIncreaseBalanceWhenDepositing() {
            Money deposit = Money.of("500.00");

            portfolio.deposit(deposit);

            assertEquals(Money.of("1500.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when depositing zero or negative amount")
        @SpecificationRef(value = "US-04.AC-2", level = TestLevel.DOMAIN, feature = "deposit-funds.feature")
        @SpecificationRef(value = "US-04.AC-3", level = TestLevel.DOMAIN, feature = "deposit-funds.feature")
        void shouldThrowExceptionWhenDepositingZeroOrNegative() {
            Money zeroDeposit = Money.ZERO;
            Money negativeDeposit = Money.of("-100.00");

            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(zeroDeposit));
            assertThrows(InvalidAmountException.class, () -> portfolio.deposit(negativeDeposit));
        }

        @Test
        @DisplayName("Should decrease balance when withdrawing money")
        @SpecificationRef(value = "US-05.AC-1", level = TestLevel.DOMAIN, feature = "withdraw-funds.feature")
        void shouldDecreaseBalanceWhenWithdrawing() {
            Money withdrawal = Money.of("300.00");

            portfolio.withdraw(withdrawal);

            assertEquals(Money.of("700.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when withdrawing zero or negative amount")
        @SpecificationRef(value = "US-05.AC-2", level = TestLevel.DOMAIN, feature = "withdraw-funds.feature")
        @SpecificationRef(value = "US-05.AC-3", level = TestLevel.DOMAIN, feature = "withdraw-funds.feature")
        void shouldThrowExceptionWhenWithdrawingZeroOrNegative() {
            Money zeroWithdrawal = Money.ZERO;
            Money negativeWithdrawal = Money.of("-100.00");

            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(zeroWithdrawal));
            assertThrows(InvalidAmountException.class, () -> portfolio.withdraw(negativeWithdrawal));
        }

        @Test
        @DisplayName("Should throw exception when withdrawing more than balance")
        @SpecificationRef(value = "US-05.AC-4", level = TestLevel.DOMAIN, feature = "withdraw-funds.feature")
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
        @SpecificationRef(value = "US-06.AC-1", level = TestLevel.DOMAIN, feature = "buy-stocks.feature")
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
        @SpecificationRef(value = "US-06.AC-2", level = TestLevel.DOMAIN, feature = "buy-stocks.feature")
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
        @SpecificationRef(value = "US-06.AC-3", level = TestLevel.DOMAIN, feature = "buy-stocks.feature")
        void shouldThrowExceptionWhenBuyingWithInsufficientFunds() {
            ShareQuantity quantity = ShareQuantity.of(25);
            Price price = Price.of("50.00");

            assertThrows(InsufficientFundsException.class, () -> portfolio.buy(APPLE, quantity, price));
        }

        @Test
        @DisplayName("Should throw exception when buying with invalid quantity")
        @SpecificationRef(value = "US-06.AC-4", level = TestLevel.DOMAIN, feature = "buy-stocks.feature")
        void shouldThrowExceptionWhenBuyingWithInvalidQuantity() {
            ShareQuantity zeroQuantity = ShareQuantity.ZERO;
            Price price = Price.of("50.00");

            assertThrows(InvalidQuantityException.class, () -> portfolio.buy(APPLE, zeroQuantity, price));
        }

        // Traceability: US-07.AC-1 = happy-path sell (acceptance criterion #1 in the API spec)
        @Test
        @DisplayName("Should increase balance and update holding when selling stock")
        @SpecificationRef(value = "US-07.AC-1", level = TestLevel.DOMAIN)
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
        @SpecificationRef(value = "US-07.AC-6", level = TestLevel.DOMAIN)
        void shouldThrowExceptionWhenSellingStockNotInPortfolio() {
            assertThrows(HoldingNotFoundException.class, () -> portfolio.sell(MICROSOFT, ShareQuantity.of(5), Price.of("100.00")));
        }

        @Test
        @DisplayName("Should throw exception when selling invalid quantity")
        @SpecificationRef(value = "US-07.AC-4", level = TestLevel.DOMAIN)
        void shouldThrowExceptionWhenSellingInvalidQuantity() {
            portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("50.00"));

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(APPLE, ShareQuantity.ZERO, Price.of("60.00")));
        }

        @Test
        @DisplayName("Should throw exception when selling more shares than owned")
        @SpecificationRef(value = "US-07.AC-3", level = TestLevel.DOMAIN)
        void shouldThrowExceptionWhenSellingMoreSharesThanOwned() {
            portfolio.buy(APPLE, ShareQuantity.of(10), Price.of("50.00"));

            assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(APPLE, ShareQuantity.of(15), Price.of("60.00")));
        }

        // Link to Gherkin scenario:
        // https://github.com/alfredorueda/HexaStock/blob/main/doc/stock-portfolio-api-specification.md#27-us-07--sell-stocks
        // Traceability: US-07.FIFO-2 corresponds to the Gherkin scenario
        // "Selling shares consumed across multiple lots" in sell-stocks.feature
        @Test
        @DisplayName("Should sell shares across multiple lots using FIFO through the aggregate root (Gherkin scenario)")
        @SpecificationRef(value = "US-07.FIFO-2", level = TestLevel.DOMAIN, feature = "sell-stocks.feature")
        void shouldSellSharesUsingFIFOThroughPortfolioAggregateRoot_GherkinScenario() {
            // Background: a portfolio with sufficient funds to buy AAPL lots
            Price purchasePrice1 = Price.of("100.00");
            Price purchasePrice2 = Price.of("120.00");
            Price marketSellPrice = Price.of("150.00");

            Portfolio fundedPortfolio = new Portfolio(
                    PortfolioId.generate(), "Alice", Money.of("10000.00"), LocalDateTime.now());

            // Background: buy 10 shares of AAPL @ 100, then 5 shares @ 120
            fundedPortfolio.buy(APPLE, ShareQuantity.of(10), purchasePrice1);
            fundedPortfolio.buy(APPLE, ShareQuantity.of(5), purchasePrice2);

            Money balanceBeforeSell = fundedPortfolio.getBalance(); // 10000 - 1000 - 600 = 8400

            // When: sell 12 shares of AAPL @ 150 through the aggregate root
            SellResult result = fundedPortfolio.sell(APPLE, ShareQuantity.of(12), marketSellPrice);

            // Then: financial results match Gherkin expectations
            assertEquals(Money.of("1800.00"), result.proceeds());   // 12 × 150
            assertEquals(Money.of("1240.00"), result.costBasis());   // (10 × 100) + (2 × 120)
            assertEquals(Money.of("560.00"), result.profit());       // 1800 − 1240

            // And: portfolio balance increased by proceeds
            assertEquals(balanceBeforeSell.add(Money.of("1800.00")), fundedPortfolio.getBalance());

            // And: FIFO lot consumption — only Lot #2 survives with 3 remaining shares
            Holding aaplHolding = fundedPortfolio.getHolding(APPLE);
            assertEquals(ShareQuantity.of(3), aaplHolding.getTotalShares());
            assertEquals(1, aaplHolding.getLots().size());

            Lot remainingLot = aaplHolding.getLots().getFirst();
            assertEquals(ShareQuantity.of(3), remainingLot.getRemainingShares());
            assertEquals(purchasePrice2, remainingLot.getUnitPrice());
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