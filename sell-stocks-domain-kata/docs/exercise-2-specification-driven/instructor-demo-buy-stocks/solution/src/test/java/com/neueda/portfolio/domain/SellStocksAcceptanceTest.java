package com.neueda.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Acceptance tests for US-07 — Sell Stocks. One test per criterion, AC-01 to AC-24 of
 * {@code spec/sell-stocks-spec.md}.
 */
@DisplayName("US-07 Sell Stocks")
class SellStocksAcceptanceTest extends DomainTestSupport {

    @Nested
    @DisplayName("Successful sales")
    class SuccessfulSales {

        @Test
        @DisplayName("AC-01: sale consumed entirely from a single lot")
        void ac01_saleFromASingleLot() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(8), price("150.00"));

            assertEquals(money("1200.00"), result.proceeds(), "proceeds");
            assertEquals(money("800.00"), result.costBasis(), "cost basis");
            assertEquals(money("400.00"), result.profit(), "profit");

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(2, lots.size(), "both lots survive");
            assertLot(lots.get(0), 2, "100.00");
            assertLot(lots.get(1), 5, "120.00");
            assertEquals(7, portfolio.getHolding(AAPL).getTotalShares().value());
        }

        @Test
        @DisplayName("AC-02: sale consumed across multiple lots, emptied lot removed")
        void ac02_saleAcrossMultipleLots() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(12), price("150.00"));

            assertEquals(money("1800.00"), result.proceeds(), "proceeds");
            assertEquals(money("1240.00"), result.costBasis(), "1000.00 from Lot #1 + 240.00 from Lot #2");
            assertEquals(money("560.00"), result.profit(), "profit");

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "Lot #1 is depleted and removed");
            assertLot(lots.get(0), 3, "120.00");
            assertEquals(3, portfolio.getHolding(AAPL).getTotalShares().value());
        }

        @Test
        @DisplayName("AC-03: smallest possible sale")
        void ac03_smallestPossibleSale() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(1), price("150.00"));

            assertEquals(money("150.00"), result.proceeds());
            assertEquals(money("100.00"), result.costBasis());
            assertEquals(money("50.00"), result.profit());

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(2, lots.size());
            assertLot(lots.get(0), 9, "100.00");
            assertLot(lots.get(1), 5, "120.00");
        }

        @Test
        @DisplayName("AC-04: boundary — the sale exactly exhausts the oldest lot")
        void ac04_saleExactlyExhaustsOldestLot() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(10), price("150.00"));

            assertEquals(money("1500.00"), result.proceeds());
            assertEquals(money("1000.00"), result.costBasis());
            assertEquals(money("500.00"), result.profit());

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "Lot #1 emptied and removed");
            assertLot(lots.get(0), 5, "120.00");
            assertEquals(5, portfolio.getHolding(AAPL).getTotalShares().value(),
                    "no shares are taken from Lot #2");
        }

        @Test
        @DisplayName("AC-05: boundary — the sale liquidates the entire position")
        void ac05_saleLiquidatesEntirePosition() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(15), price("150.00"));

            assertEquals(money("2250.00"), result.proceeds());
            assertEquals(money("1600.00"), result.costBasis());
            assertEquals(money("650.00"), result.profit());

            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL),
                    "the holding is removed from the portfolio");
            assertEquals(money("2250.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-06: loss — sale price below the purchase price")
        void ac06_saleAtALoss() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(8), price("90.00"));

            assertEquals(money("720.00"), result.proceeds());
            assertEquals(money("800.00"), result.costBasis());
            assertEquals(money("-80.00"), result.profit(), "a negative profit is a valid outcome");

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertLot(lots.get(0), 2, "100.00");
            assertLot(lots.get(1), 5, "120.00");
            assertEquals(money("720.00"), portfolio.getBalance(), "the sale still succeeds");
        }

        @Test
        @DisplayName("AC-07: FIFO consumes the oldest lot first")
        void ac07_fifoConsumesOldestLotFirst() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(8), price("150.00"));

            assertEquals(money("800.00"), result.costBasis(), "8 x 100.00, the oldest lot's price");
            assertFalse(money("960.00").equals(result.costBasis()), "not the newer lot's 120.00");
            assertFalse(money("853.33").equals(result.costBasis()), "not a blended average");

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertLot(lots.get(1), 5, "120.00");
        }

        @Test
        @DisplayName("AC-08: proceeds are credited to the portfolio's cash balance")
        void ac08_proceedsCreditedToBalance() {
            Portfolio portfolio = baselinePortfolio();
            assertEquals(money("0.00"), portfolio.getBalance());

            portfolio.sell(AAPL, qty(8), price("150.00"));

            assertEquals(money("1200.00"), portfolio.getBalance());
        }

        @DisplayName("AC-09: profit is always proceeds minus cost basis")
        @ParameterizedTest(name = "AC-09: sell {0} @ {1}")
        @MethodSource("com.neueda.portfolio.domain.SellStocksAcceptanceTest#workedExamples")
        void ac09_profitIsProceedsMinusCostBasis(int quantity, String salePrice,
                                                 String proceeds, String costBasis, String profit) {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(quantity), price(salePrice));

            assertEquals(money(proceeds), result.proceeds());
            assertEquals(money(costBasis), result.costBasis());
            assertEquals(money(profit), result.profit());
            assertEquals(result.proceeds().subtract(result.costBasis()), result.profit(),
                    "profit = proceeds - costBasis");
        }
    }

    /** The worked FIFO examples from section 5.2 of the sell specification. */
    static Stream<Arguments> workedExamples() {
        return Stream.of(
                Arguments.of(1, "150.00", "150.00", "100.00", "50.00"),
                Arguments.of(8, "150.00", "1200.00", "800.00", "400.00"),
                Arguments.of(10, "150.00", "1500.00", "1000.00", "500.00"),
                Arguments.of(12, "150.00", "1800.00", "1240.00", "560.00"),
                Arguments.of(15, "150.00", "2250.00", "1600.00", "650.00"),
                Arguments.of(8, "90.00", "720.00", "800.00", "-80.00"));
    }

    @Nested
    @DisplayName("Rejected sales")
    class RejectedSales {

        @Test
        @DisplayName("AC-10: rejected — quantity of zero")
        void ac10_zeroQuantityRejected() {
            Portfolio portfolio = baselinePortfolio();

            InvalidQuantityException thrown = assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(AAPL, qty(0), price("150.00")));

            assertTrue(thrown.getMessage().contains("must be positive"), thrown.getMessage());
            assertBaselineHoldingIntact(portfolio);
            assertEquals(money("0.00"), portfolio.getBalance(), "nothing is sold");
        }

        @Test
        @DisplayName("AC-11: rejected — negative quantity, refused when the value is created")
        void ac11_negativeQuantityRejected() {
            assertThrows(InvalidQuantityException.class, () -> new ShareQuantity(-5),
                    "a negative share quantity cannot exist");

            Portfolio portfolio = baselinePortfolio();
            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(AAPL, qty(-5), price("150.00")));
            assertBaselineHoldingIntact(portfolio);
            assertEquals(money("0.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-12: rejected — selling more shares than are held")
        void ac12_sellingMoreThanHeldRejected() {
            Portfolio portfolio = baselinePortfolio();

            ConflictQuantityException thrown = assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(AAPL, qty(16), price("150.00")));

            assertEquals("Not enough shares to sell. Available: 15, Requested: 16",
                    thrown.getMessage());
        }

        @Test
        @DisplayName("AC-13: rejected — the portfolio does not hold that ticker")
        void ac13_tickerNotHeldRejected() {
            Portfolio portfolio = baselinePortfolio();

            HoldingNotFoundException thrown = assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(MSFT, qty(5), price("150.00")));

            assertTrue(thrown.getMessage().contains("MSFT"), thrown.getMessage());
        }

        @DisplayName("AC-14: rejected — non-positive sale price")
        @ParameterizedTest(name = "AC-14: price {0}")
        @ValueSource(strings = {"0.00", "-10.00"})
        void ac14_nonPositivePriceRejected(String amount) {
            InvalidAmountException thrown = assertThrows(InvalidAmountException.class,
                    () -> new Price(new BigDecimal(amount)));

            assertTrue(thrown.getMessage().contains("must be positive"), thrown.getMessage());
        }

        @Test
        @DisplayName("AC-15: validation order — quantity is checked before the holding is looked up")
        void ac15_quantityCheckedBeforeHoldingLookup() {
            Portfolio portfolio = baselinePortfolio();

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(MSFT, qty(0), price("150.00")),
                    "the quantity precondition is evaluated first, so this is not a holding-not-found");
        }

        @Test
        @DisplayName("AC-16: a rejected sale leaves the holding untouched")
        void ac16_rejectedSaleLeavesHoldingUntouched() {
            Portfolio portfolio = baselinePortfolio();

            assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(AAPL, qty(16), price("150.00")));

            assertBaselineHoldingIntact(portfolio);
            assertEquals(money("0.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-17: a rejected sale of an unheld ticker leaves the portfolio untouched")
        void ac17_rejectedSaleOfUnheldTickerChangesNothing() {
            Portfolio portfolio = baselinePortfolio();

            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(MSFT, qty(5), price("150.00")));

            assertEquals(money("0.00"), portfolio.getBalance());
            assertBaselineHoldingIntact(portfolio);
        }
    }

    @Nested
    @DisplayName("Lot invariants")
    class LotInvariants {

        @Test
        @DisplayName("AC-18: a lot cannot be reduced below zero")
        void ac18_lotCannotBeReducedBelowZero() {
            Lot lot = new Lot(qty(10), price("100.00"));

            ConflictQuantityException thrown =
                    assertThrows(ConflictQuantityException.class, () -> lot.reduce(qty(11)));

            assertEquals("Not enough shares to sell. Available: 10, Requested: 11",
                    thrown.getMessage());
            assertEquals(10, lot.getRemainingShares().value(), "the lot is untouched");
            assertFalse(lot.isEmpty());
        }

        @Test
        @DisplayName("AC-19: a lot cannot be created with a non-positive share count")
        void ac19_lotCannotBeCreatedWithNonPositiveShares() {
            assertThrows(InvalidQuantityException.class, () -> new Lot(qty(0), price("100.00")),
                    "zero shares");
            assertThrows(InvalidQuantityException.class, () -> new Lot(qty(-3), price("100.00")),
                    "a negative share count");
        }
    }

    @Nested
    @DisplayName("Ticker validation")
    class TickerValidation {

        @DisplayName("AC-20: rejected — malformed ticker")
        @ParameterizedTest(name = "AC-20: [{0}] -> \"{1}\"")
        @MethodSource("com.neueda.portfolio.domain.SellStocksAcceptanceTest#malformedTickers")
        void ac20_malformedTickerRejected(String symbol, String expectedMessage) {
            InvalidTickerException thrown =
                    assertThrows(InvalidTickerException.class, () -> new Ticker(symbol));

            assertEquals(expectedMessage, thrown.getMessage());
        }

        @DisplayName("AC-21: boundary — well-formed ticker")
        @ParameterizedTest(name = "AC-21: {0}")
        @ValueSource(strings = {"A", "AA", "AAPL", "GOOGL"})
        void ac21_wellFormedTickerAccepted(String symbol) {
            assertEquals(symbol, new Ticker(symbol).symbol(), "the symbol is kept unchanged");
        }
    }

    static Stream<Arguments> malformedTickers() {
        return Stream.of(
                Arguments.of("aapl", "Invalid ticker: aapl"),
                Arguments.of("Aapl", "Invalid ticker: Aapl"),
                Arguments.of("TOOLONG", "Invalid ticker: TOOLONG"),
                Arguments.of("123", "Invalid ticker: 123"),
                Arguments.of("AA1", "Invalid ticker: AA1"),
                Arguments.of("A-B", "Invalid ticker: A-B"),
                Arguments.of("", "Ticker cannot be empty"),
                Arguments.of("   ", "Ticker cannot be empty"),
                Arguments.of(null, "Ticker cannot be empty"));
    }

    @Nested
    @DisplayName("Position lifecycle")
    class PositionLifecycle {

        @Test
        @DisplayName("AC-22: selling the whole position removes the holding")
        void ac22_fullSaleRemovesTheHolding() {
            Portfolio portfolio = baselinePortfolio();

            SellResult result = portfolio.sell(AAPL, qty(15), price("150.00"));
            assertEquals(money("2250.00"), result.proceeds());
            assertEquals(money("2250.00"), portfolio.getBalance());

            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(AAPL, qty(1), price("150.00")),
                    "holding-not-found, not a quantity conflict");

            Portfolio partiallySold = baselinePortfolio();
            partiallySold.sell(AAPL, qty(14), price("150.00"));
            assertEquals(1, partiallySold.getHolding(AAPL).getTotalShares().value(),
                    "a partial sale keeps the holding");
        }

        @Test
        @DisplayName("AC-23: buying again after full liquidation starts a fresh holding")
        void ac23_buyingAfterLiquidationStartsAFreshHolding() {
            Portfolio portfolio = baselinePortfolio();
            portfolio.sell(AAPL, qty(15), price("150.00"));
            assertEquals(money("2250.00"), portfolio.getBalance());

            portfolio.buy(AAPL, qty(4), price("130.00"));

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "no leftover lots from the previous position");
            assertLot(lots.get(0), 4, "130.00");
            assertEquals(4, portfolio.getHolding(AAPL).getTotalShares().value());
            assertEquals(money("1730.00"), portfolio.getBalance(), "2250.00 - 520.00");
        }

        @Test
        @DisplayName("AC-24: a rejected purchase leaves no holding behind")
        void ac24_rejectedPurchaseLeavesNoHolding() {
            Portfolio portfolio = baselinePortfolio();

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.buy(MSFT, qty(0), price("150.00")));

            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(MSFT),
                    "no MSFT holding was created");
            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(MSFT, qty(1), price("150.00")),
                    "a ticker never acquired is a ticker not held");
        }
    }
}
