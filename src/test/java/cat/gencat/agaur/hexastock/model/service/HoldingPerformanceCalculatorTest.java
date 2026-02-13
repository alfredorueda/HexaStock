package cat.gencat.agaur.hexastock.model.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.exception.HoldingNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HoldingPerformanceCalculator}.
 *
 * <p>The calculator is a pure domain service with no I/O — every test builds
 * its inputs in memory and asserts against the returned {@link HoldingDTO}
 * list.  Results are sorted by ticker before assertion so that tests are
 * independent of insertion order.</p>
 */
@DisplayName("HoldingPerformanceCalculator")
class HoldingPerformanceCalculatorTest {

    private HoldingPerformanceCalculator calculator;

    // ── Reusable tickers ────────────────────────────────────────────────
    private static final Ticker AAPL = Ticker.of("AAPL");
    private static final Ticker MSFT = Ticker.of("MSFT");
    private static final Ticker AMZN = Ticker.of("AMZN");

    @BeforeEach
    void setUp() {
        calculator = new HoldingPerformanceCalculator();
    }

    // ────────────────────────────────────────────────────────────────────
    //  Helpers — keep test bodies focused on the scenario, not plumbing
    // ────────────────────────────────────────────────────────────────────

    /** Shorthand for creating a portfolio with enough cash for any test. */
    private static Portfolio portfolioWithCash(String cash) {
        var p = Portfolio.create("Test Owner");
        p.deposit(Money.of(cash));
        return p;
    }

    /** Creates a live-price map entry. */
    private static StockPrice livePrice(Ticker ticker, String price) {
        return new StockPrice(ticker, Price.of(price), Instant.now());
    }

    /** Finds a HoldingDTO by ticker in the result list (order-independent). */
    private static HoldingDTO findByTicker(List<HoldingDTO> results, String ticker) {
        return results.stream()
                .filter(h -> h.ticker().equals(ticker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for ticker " + ticker));
    }

    /** Scales a string to 2-decimal BigDecimal for assertion comparisons. */
    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ====================================================================
    //  1. Empty / no-transaction scenarios
    // ====================================================================

    @Nested
    @DisplayName("When there are no transactions")
    class NoTransactions {

        @Test
        @DisplayName("should return empty list for empty portfolio with no transactions")
        void emptyPortfolioNoTransactions() {
            var portfolio = Portfolio.create("Empty");
            var result = calculator.getHoldingsPerformance(
                    portfolio, List.of(), Map.of());

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should skip deposit/withdrawal transactions (null ticker)")
        void onlyDepositsAndWithdrawals() {
            var portfolio = portfolioWithCash("5000.00");
            var txs = List.of(
                    Transaction.createDeposit(portfolio.getId(), Money.of("3000.00")),
                    Transaction.createWithdrawal(portfolio.getId(), Money.of("500.00"))
            );

            var result = calculator.getHoldingsPerformance(
                    portfolio, txs, Map.of());

            assertTrue(result.isEmpty(),
                    "Deposits/withdrawals have null ticker and must be silently skipped");
        }
    }

    // ====================================================================
    //  2. Only BUY transactions
    // ====================================================================

    @Nested
    @DisplayName("When there are only BUY transactions")
    class OnlyBuys {

        @Test
        @DisplayName("single purchase — basic metrics")
        void singlePurchase() {
            var portfolio = portfolioWithCash("5000.00");
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(10), Price.of("100.00"))
            );
            var prices = Map.of(AAPL, livePrice(AAPL, "110.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);

            assertEquals(1, result.size());
            var h = result.getFirst();

            assertAll("single purchase metrics",
                    () -> assertEquals("AAPL", h.ticker()),
                    () -> assertEquals(new BigDecimal("10"), h.quantity()),
                    () -> assertEquals(new BigDecimal("10"), h.remaining()),
                    () -> assertEquals(bd("100.00"), h.averagePurchasePrice()),
                    () -> assertEquals(bd("110.00"), h.currentPrice()),
                    () -> assertEquals(bd("100.00"), h.unrealizedGain()),   // (110-100)*10
                    () -> assertEquals(bd("0.00"), h.realizedGain())
            );
        }

        @Test
        @DisplayName("multiple purchases of the same ticker — weighted average price")
        void multiplePurchasesSameTicker() {
            var portfolio = portfolioWithCash("10000.00");
            portfolio.buy(MSFT, ShareQuantity.of(10), Price.of("100.00"));
            portfolio.buy(MSFT, ShareQuantity.of(5), Price.of("120.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), MSFT,
                            ShareQuantity.of(10), Price.of("100.00")),
                    Transaction.createPurchase(portfolio.getId(), MSFT,
                            ShareQuantity.of(5), Price.of("120.00"))
            );
            var prices = Map.of(MSFT, livePrice(MSFT, "115.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            var h = result.getFirst();

            // avgPrice = (10*100 + 5*120) / 15 = 1600/15 = 106.67
            assertAll("weighted average",
                    () -> assertEquals(new BigDecimal("15"), h.quantity()),
                    () -> assertEquals(new BigDecimal("15"), h.remaining()),
                    () -> assertEquals(bd("106.67"), h.averagePurchasePrice())
            );
        }
    }

    // ====================================================================
    //  3. BUY + partial SELL
    // ====================================================================

    @Nested
    @DisplayName("When there are BUY and partial SELL transactions")
    class BuyAndPartialSell {

        @Test
        @DisplayName("partial sell — remaining shares and realized gain")
        void partialSell() {
            var portfolio = portfolioWithCash("5000.00");
            portfolio.buy(MSFT, ShareQuantity.of(10), Price.of("100.00"));
            portfolio.buy(MSFT, ShareQuantity.of(5), Price.of("120.00"));

            var sellResult = portfolio.sell(MSFT, ShareQuantity.of(8), Price.of("110.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), MSFT,
                            ShareQuantity.of(10), Price.of("100.00")),
                    Transaction.createPurchase(portfolio.getId(), MSFT,
                            ShareQuantity.of(5), Price.of("120.00")),
                    Transaction.createSale(portfolio.getId(), MSFT,
                            ShareQuantity.of(8), Price.of("110.00"),
                            sellResult.proceeds(), sellResult.profit())
            );
            var prices = Map.of(MSFT, livePrice(MSFT, "120.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            var h = result.getFirst();

            assertAll("partial sell",
                    () -> assertEquals(new BigDecimal("15"), h.quantity(),   "total bought"),
                    () -> assertEquals(new BigDecimal("7"), h.remaining(),   "15 - 8 = 7"),
                    () -> assertEquals(bd("106.67"), h.averagePurchasePrice()),
                    () -> assertEquals(bd("120.00"), h.currentPrice()),
                    // FIFO sell: 8 shares from lot@100 → cost=800, proceeds=880, profit=80
                    () -> assertEquals(bd("80.00"), h.realizedGain())
            );
        }

        @Test
        @DisplayName("cross-lot FIFO sell — spanning multiple purchase lots")
        void crossLotFifoSell() {
            var portfolio = portfolioWithCash("20000.00");
            portfolio.buy(AMZN, ShareQuantity.of(10), Price.of("100.00"));
            portfolio.buy(AMZN, ShareQuantity.of(15), Price.of("120.00"));
            portfolio.buy(AMZN, ShareQuantity.of(5), Price.of("140.00"));

            // Sell 22 shares @ 150 — FIFO: 10@100 + 12@120
            var sellResult = portfolio.sell(AMZN, ShareQuantity.of(22), Price.of("150.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AMZN,
                            ShareQuantity.of(10), Price.of("100.00")),
                    Transaction.createPurchase(portfolio.getId(), AMZN,
                            ShareQuantity.of(15), Price.of("120.00")),
                    Transaction.createPurchase(portfolio.getId(), AMZN,
                            ShareQuantity.of(5), Price.of("140.00")),
                    Transaction.createSale(portfolio.getId(), AMZN,
                            ShareQuantity.of(22), Price.of("150.00"),
                            sellResult.proceeds(), sellResult.profit())
            );
            var prices = Map.of(AMZN, livePrice(AMZN, "150.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            var h = result.getFirst();

            // costBasis = 10*100 + 12*120 = 1000 + 1440 = 2440
            // proceeds  = 22*150 = 3300
            // profit    = 3300 - 2440 = 860
            assertAll("cross-lot FIFO",
                    () -> assertEquals(new BigDecimal("30"), h.quantity()),
                    () -> assertEquals(new BigDecimal("8"), h.remaining()),
                    () -> assertEquals(bd("116.67"), h.averagePurchasePrice()),
                    () -> assertEquals(bd("860.00"), h.realizedGain())
            );
        }
    }

    // ====================================================================
    //  4. Full close (BUY + SELL everything)
    // ====================================================================

    @Nested
    @DisplayName("When position is fully closed")
    class FullClose {

        @Test
        @DisplayName("full sell — remaining = 0, unrealized gain = 0")
        void fullSell() {
            var portfolio = portfolioWithCash("5000.00");
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));
            var sellResult = portfolio.sell(AAPL, ShareQuantity.of(10), Price.of("130.00"));

            // After full sell the holding's lots are empty → getTotalShares() == 0.
            // But the holding still exists in the portfolio map until lots are empty.
            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(10), Price.of("100.00")),
                    Transaction.createSale(portfolio.getId(), AAPL,
                            ShareQuantity.of(10), Price.of("130.00"),
                            sellResult.proceeds(), sellResult.profit())
            );
            var prices = Map.of(AAPL, livePrice(AAPL, "130.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            var h = result.getFirst();

            assertAll("fully closed position",
                    () -> assertEquals(new BigDecimal("10"), h.quantity()),
                    () -> assertEquals(new BigDecimal("0"), h.remaining()),
                    () -> assertEquals(bd("100.00"), h.averagePurchasePrice()),
                    () -> assertEquals(bd("0.00"), h.unrealizedGain()),
                    () -> assertEquals(bd("300.00"), h.realizedGain())   // (130-100)*10
            );
        }
    }

    // ====================================================================
    //  5. Multiple tickers in one call
    // ====================================================================

    @Nested
    @DisplayName("When multiple tickers exist")
    class MultipleTickers {

        @Test
        @DisplayName("two tickers with mixed buy/sell — independent metrics")
        void twoTickers() {
            var portfolio = portfolioWithCash("50000.00");

            // AAPL: buy 10 @ 150
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("150.00"));
            // MSFT: buy 20 @ 300, sell 5 @ 350
            portfolio.buy(MSFT, ShareQuantity.of(20), Price.of("300.00"));
            var msftSell = portfolio.sell(MSFT, ShareQuantity.of(5), Price.of("350.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(10), Price.of("150.00")),
                    Transaction.createPurchase(portfolio.getId(), MSFT,
                            ShareQuantity.of(20), Price.of("300.00")),
                    Transaction.createSale(portfolio.getId(), MSFT,
                            ShareQuantity.of(5), Price.of("350.00"),
                            msftSell.proceeds(), msftSell.profit())
            );
            var prices = Map.of(
                    AAPL, livePrice(AAPL, "160.00"),
                    MSFT, livePrice(MSFT, "320.00")
            );

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            assertEquals(2, result.size());

            var aapl = findByTicker(result, "AAPL");
            var msft = findByTicker(result, "MSFT");

            assertAll("AAPL metrics",
                    () -> assertEquals(new BigDecimal("10"), aapl.quantity()),
                    () -> assertEquals(new BigDecimal("10"), aapl.remaining()),
                    () -> assertEquals(bd("150.00"), aapl.averagePurchasePrice()),
                    () -> assertEquals(bd("160.00"), aapl.currentPrice()),
                    () -> assertEquals(bd("100.00"), aapl.unrealizedGain()),
                    () -> assertEquals(bd("0.00"), aapl.realizedGain())
            );

            assertAll("MSFT metrics",
                    () -> assertEquals(new BigDecimal("20"), msft.quantity()),
                    () -> assertEquals(new BigDecimal("15"), msft.remaining()),
                    () -> assertEquals(bd("300.00"), msft.averagePurchasePrice()),
                    () -> assertEquals(bd("320.00"), msft.currentPrice()),
                    // unrealized: (320-300)*15 = 300
                    () -> assertEquals(bd("300.00"), msft.unrealizedGain()),
                    // realized: (350-300)*5 = 250
                    () -> assertEquals(bd("250.00"), msft.realizedGain())
            );
        }
    }

    // ====================================================================
    //  6. Rounding edge cases
    // ====================================================================

    @Nested
    @DisplayName("Rounding edge cases")
    class RoundingEdgeCases {

        @ParameterizedTest(name = "buy {0}×${1} + {2}×${3} → avg ${4}")
        @MethodSource("averagePriceProvider")
        @DisplayName("average purchase price rounding")
        void averagePriceRounding(int qty1, String price1,
                                  int qty2, String price2,
                                  String expectedAvg) {
            var portfolio = portfolioWithCash("999999.00");
            portfolio.buy(AAPL, ShareQuantity.of(qty1), Price.of(price1));
            portfolio.buy(AAPL, ShareQuantity.of(qty2), Price.of(price2));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(qty1), Price.of(price1)),
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(qty2), Price.of(price2))
            );
            var prices = Map.of(AAPL, livePrice(AAPL, price1)); // price irrelevant here

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            assertEquals(bd(expectedAvg), result.getFirst().averagePurchasePrice());
        }

        static Stream<Arguments> averagePriceProvider() {
            return Stream.of(
                    //  qty1, price1,   qty2, price2,   expectedAvg
                    // 1600/15 = 106.6666… → 106.67 (HALF_UP)
                    Arguments.of(10, "100.00", 5, "120.00", "106.67"),
                    // 3500/30 = 116.6666… → 116.67
                    Arguments.of(10, "100.00", 20, "125.00", "116.67"),
                    // 3*33.33 + 7*66.67 = 99.99 + 466.69 = 566.68 / 10 = 56.668 → 56.67
                    Arguments.of(3, "33.33", 7, "66.67", "56.67")
            );
        }
    }

    // ====================================================================
    //  7. Missing price scenario
    // ====================================================================

    @Nested
    @DisplayName("When a ticker's live price is missing from the price map")
    class MissingPrice {

        @Test
        @DisplayName("should default currentPrice and unrealizedGain to 0.00")
        void missingPriceDefaultsToZero() {
            var portfolio = portfolioWithCash("5000.00");
            portfolio.buy(AAPL, ShareQuantity.of(10), Price.of("100.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(10), Price.of("100.00"))
            );
            // Empty price map — simulates provider failure
            Map<Ticker, StockPrice> prices = Map.of();

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);
            var h = result.getFirst();

            assertAll("missing price defaults",
                    () -> assertEquals(bd("0.00"), h.currentPrice(),
                            "currentPrice should be 0.00 when provider has no data"),
                    () -> assertEquals(bd("0.00"), h.unrealizedGain(),
                            "unrealizedGain should be 0.00 when provider has no data"),
                    // Non-price fields still calculated normally
                    () -> assertEquals(new BigDecimal("10"), h.quantity()),
                    () -> assertEquals(bd("100.00"), h.averagePurchasePrice()),
                    () -> assertEquals(bd("0.00"), h.realizedGain())
            );
        }
    }

    // ====================================================================
    //  8. Holding not in portfolio (transaction exists but holding missing)
    // ====================================================================

    @Nested
    @DisplayName("When the portfolio has no holding for a transacted ticker")
    class HoldingNotInPortfolio {

        @Test
        @DisplayName("should throw HoldingNotFoundException")
        void shouldThrowWhenHoldingMissing() {
            var portfolio = Portfolio.create("Empty");
            // Transaction references AAPL but portfolio has no such holding
            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(5), Price.of("100.00"))
            );
            var prices = Map.of(AAPL, livePrice(AAPL, "110.00"));

            assertThrows(HoldingNotFoundException.class,
                    () -> calculator.getHoldingsPerformance(portfolio, txs, prices));
        }
    }

    // ====================================================================
    //  9. Result immutability
    // ====================================================================

    @Nested
    @DisplayName("Returned list immutability")
    class Immutability {

        @Test
        @DisplayName("returned list should be unmodifiable (List.copyOf)")
        void resultIsImmutable() {
            var portfolio = portfolioWithCash("5000.00");
            portfolio.buy(AAPL, ShareQuantity.of(1), Price.of("50.00"));

            var txs = List.of(
                    Transaction.createPurchase(portfolio.getId(), AAPL,
                            ShareQuantity.of(1), Price.of("50.00"))
            );
            var prices = Map.of(AAPL, livePrice(AAPL, "60.00"));

            var result = calculator.getHoldingsPerformance(portfolio, txs, prices);

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(new HoldingDTO("X", BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO)));
        }
    }

    // ====================================================================
    //  10. Large dataset — correctness (not timing)
    // ====================================================================

    @Nested
    @DisplayName("Large deterministic dataset")
    class LargeDataset {

        @Test
        @DisplayName("5 000 BUY transactions across 10 tickers — correct totals")
        void fiveThousandTransactions() {
            var portfolio = portfolioWithCash("99999999.00");
            var tickers = IntStream.rangeClosed(1, 10)
                    .mapToObj(i -> Ticker.of("T" + String.valueOf((char) ('A' + i - 1))))
                    .toList();

            var txs = new ArrayList<Transaction>();
            // 500 buys per ticker, each 2 shares @ $50.00
            for (var ticker : tickers) {
                for (int j = 0; j < 500; j++) {
                    portfolio.buy(ticker, ShareQuantity.of(2), Price.of("50.00"));
                    txs.add(Transaction.createPurchase(portfolio.getId(), ticker,
                            ShareQuantity.of(2), Price.of("50.00")));
                }
            }

            var prices = new HashMap<Ticker, StockPrice>();
            for (var ticker : tickers) {
                prices.put(ticker, livePrice(ticker, "55.00"));
            }

            var result = calculator.getHoldingsPerformance(
                    portfolio, List.copyOf(txs), Map.copyOf(prices));

            assertEquals(10, result.size());

            for (var h : result) {
                assertAll("ticker " + h.ticker(),
                        () -> assertEquals(new BigDecimal("1000"), h.quantity()),   // 500 × 2
                        () -> assertEquals(new BigDecimal("1000"), h.remaining()),
                        () -> assertEquals(bd("50.00"), h.averagePurchasePrice()),
                        () -> assertEquals(bd("55.00"), h.currentPrice()),
                        () -> assertEquals(bd("5000.00"), h.unrealizedGain()),  // (55-50)*1000
                        () -> assertEquals(bd("0.00"), h.realizedGain())
                );
            }
        }
    }
}

