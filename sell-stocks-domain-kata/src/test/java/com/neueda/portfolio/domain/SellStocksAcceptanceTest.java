package com.neueda.portfolio.domain;

import com.neueda.portfolio.domain.exception.ConflictQuantityException;
import com.neueda.portfolio.domain.exception.HoldingNotFoundException;
import com.neueda.portfolio.domain.exception.InvalidAmountException;
import com.neueda.portfolio.domain.exception.InvalidQuantityException;
import com.neueda.portfolio.domain.exception.InvalidTickerException;
import com.neueda.portfolio.domain.model.Holding;
import com.neueda.portfolio.domain.model.Lot;
import com.neueda.portfolio.domain.model.Portfolio;
import com.neueda.portfolio.domain.vo.Money;
import com.neueda.portfolio.domain.vo.Price;
import com.neueda.portfolio.domain.vo.SellResult;
import com.neueda.portfolio.domain.vo.ShareQuantity;
import com.neueda.portfolio.domain.vo.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US-07 — Sell Stocks. One test per acceptance criterion in
 * {@code docs/spec/sell-stocks-spec.md} §5.3.
 *
 * <p>Baseline holding (spec §5.1): a portfolio for owner "Alice" with a cash balance of
 * 0.00, holding AAPL as two lots in purchase order — 10 @ 100.00, then 5 @ 120.00 — for a
 * total of 15 shares. The market price is 150.00 unless a test says otherwise.
 */
@DisplayName("US-07 Sell Stocks — FIFO domain behaviour")
class SellStocksAcceptanceTest {

    private static final Ticker AAPL = Ticker.of("AAPL");
    private static final Ticker MSFT = Ticker.of("MSFT");
    private static final Price MARKET_PRICE = Price.of("150.00");

    private Portfolio portfolio;

    @BeforeEach
    void createBaselineHolding() {
        portfolio = Portfolio.create("Alice");
        portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));
        portfolio.buy(AAPL, ShareQuantity.of(5), Price.of("120.00"));
    }

    // ---------------------------------------------------------------------
    // Happy path and FIFO arithmetic
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Successful sales")
    class SuccessfulSales {

        @Test
        @DisplayName("AC-01: selling 8 shares is taken entirely from the oldest lot")
        void ac01_saleConsumedEntirelyFromASingleLot() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(8), MARKET_PRICE);

            assertMoney("1200.00", result.proceeds());
            assertMoney("800.00", result.costBasis());
            assertMoney("400.00", result.profit());

            List<Lot> lots = lotsOfAapl();
            assertEquals(2, lots.size(), "both lots survive a sale of 8 out of 15");
            assertLot(lots.get(0), 10, 2, "100.00");
            assertLot(lots.get(1), 5, 5, "120.00");
        }

        @Test
        @DisplayName("AC-02: selling 12 shares crosses both lots and removes the emptied one")
        void ac02_saleConsumedAcrossMultipleLots() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(12), MARKET_PRICE);

            assertMoney("1800.00", result.proceeds());
            assertMoney("1240.00", result.costBasis());
            assertMoney("560.00", result.profit());

            List<Lot> lots = lotsOfAapl();
            assertEquals(1, lots.size(), "the fully depleted first lot is removed");
            assertLot(lots.get(0), 5, 3, "120.00");
            assertEquals(3, totalSharesOfAapl());
        }

        @Test
        @DisplayName("AC-03: selling 1 share leaves both lots in place")
        void ac03_smallestPossibleSale() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(1), MARKET_PRICE);

            assertMoney("150.00", result.proceeds());
            assertMoney("100.00", result.costBasis());
            assertMoney("50.00", result.profit());

            List<Lot> lots = lotsOfAapl();
            assertEquals(2, lots.size());
            assertLot(lots.get(0), 10, 9, "100.00");
            assertLot(lots.get(1), 5, 5, "120.00");
        }

        @Test
        @DisplayName("AC-04: selling exactly 10 shares empties the first lot and touches no other")
        void ac04_boundarySaleExactlyExhaustsTheOldestLot() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(10), MARKET_PRICE);

            assertMoney("1500.00", result.proceeds());
            assertMoney("1000.00", result.costBasis());
            assertMoney("500.00", result.profit());

            List<Lot> lots = lotsOfAapl();
            assertEquals(1, lots.size(), "the exactly emptied first lot is removed");
            assertLot(lots.get(0), 5, 5, "120.00");
            assertEquals(5, totalSharesOfAapl());
        }

        @Test
        @DisplayName("AC-05: selling all 15 shares liquidates the position")
        void ac05_boundarySaleLiquidatesTheEntirePosition() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(15), MARKET_PRICE);

            assertMoney("2250.00", result.proceeds());
            assertMoney("1600.00", result.costBasis());
            assertMoney("650.00", result.profit());

            // The holding is gone, so ask the result rather than the portfolio.
            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL));
        }

        @Test
        @DisplayName("AC-06: selling 8 shares at 90.00 realizes a loss of -80.00")
        void ac06_saleBelowCostRealizesALoss() {
            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(8), Price.of("90.00"));

            assertMoney("720.00", result.proceeds());
            assertMoney("800.00", result.costBasis());
            assertMoney("-80.00", result.profit());

            assertMoney("720.00", portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-07: FIFO consumes the oldest lot first, never the newer one or an average")
        void ac07_fifoConsumesTheOldestLotFirst() {
            List<Lot> before = lotsOfAapl();
            Lot oldestLot = before.get(0);
            Lot newestLot = before.get(1);

            SellResult result = portfolio.sell(AAPL, ShareQuantity.of(8), MARKET_PRICE);

            // 8 × 100.00 (oldest lot) — not 8 × 120.00 (960.00) and not a blended
            // average of the two prices (8 × 106.67 ≈ 853.33).
            assertMoney("800.00", result.costBasis());

            List<Lot> after = lotsOfAapl();
            assertSame(oldestLot, after.get(0), "the oldest lot is still first");
            assertEquals(2, oldestLot.getRemainingShares().value(), "the oldest lot absorbed the sale");
            assertEquals(5, newestLot.getRemainingShares().value(), "the newer lot is untouched");
        }

        @Test
        @DisplayName("AC-08: proceeds are credited to the portfolio cash balance")
        void ac08_proceedsAreAddedToTheCashBalance() {
            assertMoney("0.00", portfolio.getBalance());

            portfolio.sell(AAPL, ShareQuantity.of(8), MARKET_PRICE);

            assertMoney("1200.00", portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-09: profit always equals proceeds minus cost basis")
        void ac09_profitIsProceedsMinusCostBasis() {
            for (int quantity : new int[] {1, 8, 10, 12, 15}) {
                createBaselineHolding();
                SellResult result = portfolio.sell(AAPL, ShareQuantity.of(quantity), MARKET_PRICE);

                assertMoney(
                        result.proceeds().subtract(result.costBasis()).amount().toPlainString(),
                        result.profit());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Rejected sales
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Rejected sales")
    class RejectedSales {

        @Test
        @DisplayName("AC-10: selling zero shares is rejected as an invalid quantity")
        void ac10_zeroQuantityIsRejected() {
            InvalidQuantityException thrown = assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(AAPL, ShareQuantity.of(0), MARKET_PRICE));

            assertTrue(thrown.getMessage().contains("Quantity must be positive"), thrown.getMessage());
        }

        @Test
        @DisplayName("AC-11: a negative quantity is rejected as an invalid quantity")
        void ac11_negativeQuantityIsRejected() {
            // A negative quantity cannot even be expressed: the value object refuses it,
            // so it never reaches the sale.
            InvalidQuantityException thrown = assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(AAPL, ShareQuantity.of(-5), MARKET_PRICE));

            assertTrue(thrown.getMessage().contains("cannot be negative"), thrown.getMessage());
        }

        @Test
        @DisplayName("AC-12: selling 16 of 15 owned shares is rejected as a quantity conflict")
        void ac12_sellingMoreThanOwnedIsRejected() {
            ConflictQuantityException thrown = assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(AAPL, ShareQuantity.of(16), MARKET_PRICE));

            assertTrue(thrown.getMessage().contains("Not enough shares to sell"), thrown.getMessage());
            assertTrue(thrown.getMessage().contains("Available: 15"), thrown.getMessage());
            assertTrue(thrown.getMessage().contains("Requested: 16"), thrown.getMessage());
        }

        @Test
        @DisplayName("AC-13: selling a ticker the portfolio does not hold is rejected as holding not found")
        void ac13_sellingATickerNotHeldIsRejected() {
            HoldingNotFoundException thrown = assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(MSFT, ShareQuantity.of(5), MARKET_PRICE));

            assertTrue(thrown.getMessage().contains("MSFT"), thrown.getMessage());
        }

        @Test
        @DisplayName("AC-14: a non-positive sale price is rejected as an invalid amount")
        void ac14_nonPositiveSalePriceIsRejected() {
            InvalidAmountException zero = assertThrows(InvalidAmountException.class,
                    () -> Price.of("0.00"));
            assertTrue(zero.getMessage().contains("Price must be positive"), zero.getMessage());

            assertThrows(InvalidAmountException.class, () -> Price.of("-150.00"));
        }

        @Test
        @DisplayName("AC-15: the quantity is validated before the holding is looked up")
        void ac15_quantityIsValidatedBeforeTheHoldingLookup() {
            // Zero shares of a ticker that is not held: both preconditions are violated,
            // and the quantity one wins.
            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.sell(MSFT, ShareQuantity.of(0), MARKET_PRICE));
        }
    }

    // ---------------------------------------------------------------------
    // State after a rejected sale
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("A rejected sale mutates nothing")
    class NoPartialMutation {

        @Test
        @DisplayName("AC-16: a rejected oversized sale leaves lots and cash balance untouched")
        void ac16_rejectedOversizedSaleLeavesEverythingUnchanged() {
            assertThrows(ConflictQuantityException.class,
                    () -> portfolio.sell(AAPL, ShareQuantity.of(16), MARKET_PRICE));

            List<Lot> lots = lotsOfAapl();
            assertEquals(2, lots.size(), "no lot was removed");
            assertLot(lots.get(0), 10, 10, "100.00");
            assertLot(lots.get(1), 5, 5, "120.00");
            assertEquals(15, totalSharesOfAapl());
            assertMoney("0.00", portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-17: a rejected sale of an unheld ticker leaves the portfolio untouched")
        void ac17_rejectedSaleOfUnheldTickerLeavesEverythingUnchanged() {
            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(MSFT, ShareQuantity.of(5), MARKET_PRICE));

            List<Lot> lots = lotsOfAapl();
            assertEquals(2, lots.size());
            assertLot(lots.get(0), 10, 10, "100.00");
            assertLot(lots.get(1), 5, 5, "120.00");
            assertMoney("0.00", portfolio.getBalance());
        }
    }

    // ---------------------------------------------------------------------
    // Lot-level preconditions
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Lot preconditions")
    class LotPreconditions {

        @Test
        @DisplayName("AC-18: a lot cannot be reduced below zero remaining shares")
        void ac18_lotCannotBeReducedBelowZero() {
            Lot lot = Lot.create(ShareQuantity.of(10), Price.of("100.00"), LocalDateTime.now());

            assertThrows(ConflictQuantityException.class, () -> lot.reduce(ShareQuantity.of(11)));

            assertEquals(10, lot.getRemainingShares().value(), "the failed reduction changed nothing");
        }

        @Test
        @DisplayName("AC-19: a lot cannot be created with a non-positive share count")
        void ac19_lotCannotBeCreatedWithNonPositiveShares() {
            Price price = Price.of("100.00");
            LocalDateTime now = LocalDateTime.now();

            assertThrows(InvalidQuantityException.class,
                    () -> Lot.create(ShareQuantity.of(0), price, now));
            assertThrows(InvalidQuantityException.class,
                    () -> Lot.create(ShareQuantity.of(-1), price, now));
        }
    }

    // ---------------------------------------------------------------------
    // Ticker format, and the holding lifecycle after full liquidation
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Ticker format")
    class TickerFormat {

        @ParameterizedTest(name = "\"{0}\" is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "aapl", "Aapl", "TOOLONG", "123", "AA1", "A-B"})
        @DisplayName("AC-20: a malformed ticker is rejected")
        void ac20_malformedTickerIsRejected(String symbol) {
            assertThrows(InvalidTickerException.class, () -> Ticker.of(symbol));
        }

        @ParameterizedTest(name = "\"{0}\" is accepted")
        @ValueSource(strings = {"A", "AA", "AAPL", "GOOGL"})
        @DisplayName("AC-21: one to five uppercase letters is accepted")
        void ac21_wellFormedTickerIsAccepted(String symbol) {
            assertEquals(symbol, Ticker.of(symbol).symbol());
        }
    }

    @Nested
    @DisplayName("Holding lifecycle")
    class HoldingLifecycle {

        @Test
        @DisplayName("AC-22: selling the whole position removes the holding")
        void ac22_fullLiquidationRemovesTheHolding() {
            portfolio.sell(AAPL, ShareQuantity.of(15), MARKET_PRICE);

            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL));
            // A further sell is now a missing holding, not a quantity conflict.
            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(AAPL, ShareQuantity.of(1), MARKET_PRICE));
            // The sale that emptied it still stands.
            assertMoney("2250.00", portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-23: buying the ticker again starts a fresh holding")
        void ac23_buyingAgainAfterLiquidationStartsAFreshHolding() {
            Holding original = portfolio.getHolding(AAPL);
            portfolio.sell(AAPL, ShareQuantity.of(15), MARKET_PRICE);

            portfolio.buy(AAPL, ShareQuantity.of(4), Price.of("130.00"));

            Holding reopened = portfolio.getHolding(AAPL);
            assertNotSame(original, reopened, "a new holding, not the emptied one revived");
            List<Lot> lots = reopened.getLots();
            assertEquals(1, lots.size(), "only the new purchase, no ghost lots");
            assertLot(lots.get(0), 4, 4, "130.00");
        }

        @Test
        @DisplayName("AC-22 (converse): a partial sale keeps the holding")
        void ac22_partialSaleKeepsTheHolding() {
            portfolio.sell(AAPL, ShareQuantity.of(14), MARKET_PRICE);

            assertEquals(1, totalSharesOfAapl(), "one share left, so the holding survives");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<Lot> lotsOfAapl() {
        return portfolio.getHolding(AAPL).getLots();
    }

    private int totalSharesOfAapl() {
        Holding holding = portfolio.getHolding(AAPL);
        return holding.getTotalShares().value();
    }

    /** Compares with {@code compareTo} so 1200 and 1200.00 are the same amount. */
    private static void assertMoney(String expected, Money actual) {
        assertEquals(0, new java.math.BigDecimal(expected).compareTo(actual.amount()),
                () -> "expected " + expected + " but was " + actual);
    }

    private static void assertLot(Lot lot, int initialShares, int remainingShares, String unitPrice) {
        assertEquals(initialShares, lot.getInitialShares().value(), "initial shares");
        assertEquals(remainingShares, lot.getRemainingShares().value(), "remaining shares");
        assertEquals(0, new java.math.BigDecimal(unitPrice).compareTo(lot.getUnitPrice().amount()),
                () -> "unit price expected " + unitPrice + " but was " + lot.getUnitPrice());
    }
}
