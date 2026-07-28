package com.example.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for selling stock from a portfolio.
 *
 * <p>Uses Alice's portfolio from the brief: 10 shares of AAPL at 100.00, then
 * 5 more at 120.00, with a market price of 150.00.
 */
class PortfolioTest {

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio("Alice");
        portfolio.buy("AAPL", 10, 100.00);
        portfolio.buy("AAPL", 5, 120.00);
    }

    @Test
    @DisplayName("selling part of a position returns proceeds, cost basis and profit")
    void sellingEightShares() {
        SaleResult result = portfolio.sell("AAPL", 8, 150.00);

        assertEquals("AAPL", result.getTicker());
        assertEquals(8, result.getQuantity());
        assertEquals(1200.00, result.getProceeds(), 0.01);
        assertEquals(853.33, result.getCostBasis(), 0.01);
        assertEquals(346.67, result.getProfit(), 0.01);
    }

    @Test
    @DisplayName("selling across the whole position uses the average cost of the shares")
    void sellingTwelveShares() {
        SaleResult result = portfolio.sell("AAPL", 12, 150.00);

        assertEquals(1800.00, result.getProceeds(), 0.01);
        assertEquals(1280.00, result.getCostBasis(), 0.01);
        assertEquals(520.00, result.getProfit(), 0.01);
    }

    @Test
    @DisplayName("selling the entire position leaves no shares")
    void sellingEverything() {
        SaleResult result = portfolio.sell("AAPL", 15, 150.00);

        assertEquals(2250.00, result.getProceeds(), 0.01);
        assertEquals(1600.00, result.getCostBasis(), 0.01);
        assertEquals(650.00, result.getProfit(), 0.01);
        assertEquals(0, portfolio.getShares("AAPL"));
    }

    @Test
    @DisplayName("a sale below the purchase price produces a loss")
    void sellingAtALoss() {
        SaleResult result = portfolio.sell("AAPL", 8, 90.00);

        assertEquals(720.00, result.getProceeds(), 0.01);
        assertEquals(-133.33, result.getProfit(), 0.01);
    }

    @Test
    @DisplayName("proceeds are added to the cash balance")
    void proceedsGoIntoTheCashBalance() {
        portfolio.sell("AAPL", 8, 150.00);

        assertEquals(1200.00, portfolio.getCashBalance(), 0.01);
    }

    @Test
    @DisplayName("the remaining share count is reduced by the sale")
    void sharesAreReduced() {
        portfolio.sell("AAPL", 8, 150.00);

        assertEquals(7, portfolio.getShares("AAPL"));
    }

    @Test
    @DisplayName("selling more shares than are held is rejected")
    void cannotSellMoreThanOwned() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.sell("AAPL", 16, 150.00));
    }

    @Test
    @DisplayName("selling a stock that is not held is rejected")
    void cannotSellUnknownTicker() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.sell("MSFT", 5, 150.00));
    }
}
