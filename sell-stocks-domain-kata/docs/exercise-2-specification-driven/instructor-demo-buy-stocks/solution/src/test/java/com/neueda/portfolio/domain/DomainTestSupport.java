package com.neueda.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shared fixtures and assertions for the acceptance tests.
 *
 * <p>Note on lot assertions: the class diagram gives {@code Lot} no unit-price getter, so a
 * lot's unit price is observed the way the diagram allows — the cost basis of exactly one
 * share is, by definition, that lot's unit price.</p>
 */
abstract class DomainTestSupport {

    static final Ticker AAPL = new Ticker("AAPL");
    static final Ticker MSFT = new Ticker("MSFT");

    static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    static Price price(String amount) {
        return new Price(new BigDecimal(amount));
    }

    static ShareQuantity qty(int shares) {
        return new ShareQuantity(shares);
    }

    /**
     * The sell specification's baseline holding: owner "Alice", cash balance 0.00, AAPL held as
     * Lot #1 of 10 shares at 100.00 followed by Lot #2 of 5 shares at 120.00 (15 shares total).
     *
     * <p>It is built by depositing exactly the two lots' combined cost
     * (10 x 100.00 + 5 x 120.00 = 1600.00) and then buying them, which spends the deposit back
     * down to 0.00 — the same shape the buy specification's worked example describes.</p>
     */
    static Portfolio baselinePortfolio() {
        Portfolio portfolio = new Portfolio("Alice");
        portfolio.deposit(money("1600.00"));
        portfolio.buy(AAPL, qty(10), price("100.00"));
        portfolio.buy(AAPL, qty(5), price("120.00"));
        assertEquals(money("0.00"), portfolio.getBalance(), "baseline starts with a 0.00 balance");
        return portfolio;
    }

    /** Asserts a lot's remaining share count and its unit price. */
    static void assertLot(Lot lot, int remainingShares, String unitPrice) {
        assertEquals(remainingShares, lot.getRemainingShares().value(), "remaining shares");
        assertEquals(money(unitPrice), lot.calculateCostBasis(qty(1)), "unit price");
    }

    /** Asserts that the AAPL position is still the untouched baseline: 10 @ 100.00, 5 @ 120.00. */
    static void assertBaselineHoldingIntact(Portfolio portfolio) {
        List<Lot> lots = portfolio.getHolding(AAPL).getLots();
        assertEquals(2, lots.size(), "lot count");
        assertLot(lots.get(0), 10, "100.00");
        assertLot(lots.get(1), 5, "120.00");
        assertEquals(15, portfolio.getHolding(AAPL).getTotalShares().value(), "total shares");
    }
}
