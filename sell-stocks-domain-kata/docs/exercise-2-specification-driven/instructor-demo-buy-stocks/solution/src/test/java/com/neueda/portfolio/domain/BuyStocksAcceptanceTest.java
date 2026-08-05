package com.neueda.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Acceptance tests for US-08 — Buy Stocks. One test per criterion, AC-01 to AC-12 of
 * {@code instructor-demo-buy-stocks/spec/buy-stocks-spec.md}. This numbering is independent
 * of the sell specification's.
 */
@DisplayName("US-08 Buy Stocks")
class BuyStocksAcceptanceTest extends DomainTestSupport {

    /** The buy specification's starting context: owner "Alice", nothing held, balance 0.00. */
    private static Portfolio emptyPortfolio() {
        Portfolio portfolio = new Portfolio("Alice");
        assertEquals(money("0.00"), portfolio.getBalance());
        return portfolio;
    }

    /** The empty portfolio after depositing 1000.00. */
    private static Portfolio fundedPortfolio() {
        Portfolio portfolio = emptyPortfolio();
        portfolio.deposit(money("1000.00"));
        return portfolio;
    }

    /** Balance 500.00, one AAPL lot of 5 @ 100.00 — the state AC-03 leaves behind. */
    private static Portfolio portfolioAfterFirstPurchase() {
        Portfolio portfolio = fundedPortfolio();
        portfolio.buy(AAPL, qty(5), price("100.00"));
        return portfolio;
    }

    @Nested
    @DisplayName("Deposits")
    class Deposits {

        @Test
        @DisplayName("AC-01: a deposit increases the cash balance")
        void ac01_depositIncreasesBalance() {
            Portfolio portfolio = emptyPortfolio();

            portfolio.deposit(money("1000.00"));

            assertEquals(money("1000.00"), portfolio.getBalance());
            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL),
                    "nothing else changes — no holding appears");
        }

        @DisplayName("AC-02: rejected — non-positive deposit")
        @ParameterizedTest(name = "AC-02: deposit {0}")
        @ValueSource(strings = {"0.00", "-50.00"})
        void ac02_nonPositiveDepositRejected(String amount) {
            Portfolio portfolio = emptyPortfolio();

            InvalidAmountException thrown = assertThrows(InvalidAmountException.class,
                    () -> portfolio.deposit(new Money(new BigDecimal(amount))));

            assertTrue(thrown.getMessage().contains("must be positive"), thrown.getMessage());
            assertEquals(money("0.00"), portfolio.getBalance());
        }
    }

    @Nested
    @DisplayName("Successful purchases")
    class SuccessfulPurchases {

        @Test
        @DisplayName("AC-03: first purchase of a ticker creates a new holding")
        void ac03_firstPurchaseCreatesHolding() {
            Portfolio portfolio = fundedPortfolio();

            portfolio.buy(AAPL, qty(5), price("100.00"));

            assertEquals(money("500.00"), portfolio.getBalance());
            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "exactly one lot");
            assertLot(lots.get(0), 5, "100.00");
            assertEquals(5, portfolio.getHolding(AAPL).getTotalShares().value());
        }

        @Test
        @DisplayName("AC-04: purchase deducts the exact cost from the balance")
        void ac04_purchaseDeductsExactCost() {
            Portfolio portfolio = fundedPortfolio();

            portfolio.buy(AAPL, qty(5), price("100.00"));

            assertEquals(0, portfolio.getBalance().amount().compareTo(new BigDecimal("500.00")),
                    "1000.00 - 500.00, exactly");
            assertEquals(money("500.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-05: a second purchase appends a second lot, in order")
        void ac05_secondPurchaseAppendsSecondLot() {
            Portfolio portfolio = portfolioAfterFirstPurchase();
            assertEquals(money("500.00"), portfolio.getBalance());

            portfolio.buy(AAPL, qty(3), price("110.00"));

            assertEquals(money("170.00"), portfolio.getBalance(), "500.00 - 330.00");
            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(2, lots.size());
            assertLot(lots.get(0), 5, "100.00");
            assertLot(lots.get(1), 3, "110.00");
        }

        @Test
        @DisplayName("AC-06: buying never merges lots, even at an identical price")
        void ac06_buyingNeverMergesLots() {
            Portfolio portfolio = portfolioAfterFirstPurchase();

            portfolio.buy(AAPL, qty(5), price("100.00"));

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(2, lots.size(), "two separate lots, not one merged lot of 10");
            assertLot(lots.get(0), 5, "100.00");
            assertLot(lots.get(1), 5, "100.00");
            assertEquals(10, portfolio.getHolding(AAPL).getTotalShares().value());
            assertEquals(money("0.00"), portfolio.getBalance());
        }
    }

    @Nested
    @DisplayName("Rejected purchases")
    class RejectedPurchases {

        @Test
        @DisplayName("AC-07: rejected — insufficient funds")
        void ac07_insufficientFundsRejected() {
            Portfolio portfolio = portfolioAfterFirstPurchase();

            InsufficientFundsException thrown = assertThrows(InsufficientFundsException.class,
                    () -> portfolio.buy(AAPL, qty(4), price("130.00")));

            assertTrue(thrown.getMessage().contains("Available: 500.00, Required: 520.00"),
                    thrown.getMessage());
            assertEquals(money("500.00"), portfolio.getBalance(), "balance stays 500.00");
            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "no lot is added");
            assertLot(lots.get(0), 5, "100.00");
        }

        @DisplayName("AC-08: rejected — non-positive quantity")
        @ParameterizedTest(name = "AC-08: buy {0} shares")
        @ValueSource(ints = {0, -3})
        void ac08_nonPositiveQuantityRejected(int shares) {
            Portfolio portfolio = fundedPortfolio();

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.buy(AAPL, qty(shares), price("100.00")));

            assertEquals(money("1000.00"), portfolio.getBalance());
            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL),
                    "no holding is created");
        }

        @DisplayName("AC-09: rejected — malformed ticker")
        @ParameterizedTest(name = "AC-09: buy \"{0}\"")
        @ValueSource(strings = {"aapl", "TOOLONG", ""})
        void ac09_malformedTickerRejected(String symbol) {
            Portfolio portfolio = fundedPortfolio();

            assertThrows(InvalidTickerException.class,
                    () -> portfolio.buy(new Ticker(symbol), qty(5), price("100.00")));

            assertEquals(money("1000.00"), portfolio.getBalance());
            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL),
                    "no holding is created for any of them");
        }

        @DisplayName("AC-10: rejected — non-positive price")
        @ParameterizedTest(name = "AC-10: buy at {0}")
        @ValueSource(strings = {"0.00", "-10.00"})
        void ac10_nonPositivePriceRejected(String amount) {
            Portfolio portfolio = fundedPortfolio();

            assertThrows(InvalidAmountException.class,
                    () -> portfolio.buy(AAPL, qty(5), new Price(new BigDecimal(amount))));

            assertEquals(money("1000.00"), portfolio.getBalance());
        }

        @Test
        @DisplayName("AC-11: a rejected purchase leaves no partial state behind")
        void ac11_rejectedPurchaseLeavesNoPartialState() {
            Portfolio portfolio = fundedPortfolio();

            assertThrows(InvalidQuantityException.class,
                    () -> portfolio.buy(AAPL, qty(0), price("100.00")));

            assertThrows(HoldingNotFoundException.class, () -> portfolio.getHolding(AAPL),
                    "not an empty holding, not a partial one — no holding at all");
            assertThrows(HoldingNotFoundException.class,
                    () -> portfolio.sell(AAPL, qty(1), price("100.00")),
                    "a later sale is still a holding-not-found failure");
            assertEquals(money("1000.00"), portfolio.getBalance());
        }
    }

    @Nested
    @DisplayName("Position lifecycle")
    class PositionLifecycle {

        @Test
        @DisplayName("AC-12: buying again after a position was fully sold starts a fresh holding")
        void ac12_buyingAfterFullSaleStartsAFreshHolding() {
            Portfolio portfolio = baselinePortfolio();
            portfolio.sell(AAPL, qty(15), price("150.00"));
            assertEquals(money("2250.00"), portfolio.getBalance());

            portfolio.buy(AAPL, qty(4), price("130.00"));

            List<Lot> lots = portfolio.getHolding(AAPL).getLots();
            assertEquals(1, lots.size(), "exactly one lot, no leftovers from the old position");
            assertLot(lots.get(0), 4, "130.00");
            assertEquals(money("1730.00"), portfolio.getBalance(), "2250.00 - 520.00");
        }
    }
}
