package cat.gencat.agaur.hexastock.model.portfolio;

import cat.gencat.agaur.hexastock.model.transaction.*;
import cat.gencat.agaur.hexastock.model.market.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Domain service that computes per-ticker holding performance from a flat
 * list of transactions and live market prices.
 *
 * <h2>Algorithm — single-pass aggregation, O(T)</h2>
 * <p>The calculator iterates the transaction list <strong>exactly once</strong>.
 * For every stock-related transaction it updates a {@link TickerAccumulator}
 * keyed by {@link Ticker} in a local {@code HashMap}.  After the loop each
 * accumulator already contains the totals needed to build a {@link HoldingPerformance},
 * so no second pass or per-ticker filtering is required.</p>
 *
 * <h2>Why sequential instead of parallel?</h2>
 * <ul>
 *   <li><strong>Common-pool contention</strong> — {@code parallelStream()} uses the
 *       shared {@code ForkJoinPool.commonPool()}.  In a web server every request
 *       that calls {@code parallelStream()} competes for the same threads, causing
 *       unpredictable latency spikes under load.</li>
 *   <li><strong>Overhead</strong> — splitting, thread hand-off, and merging cost more
 *       than the work itself for the typical dataset size (hundreds to low thousands
 *       of transactions).</li>
 *   <li><strong>Predictability</strong> — sequential execution gives deterministic
 *       throughput and is easier to reason about in profiling and testing.</li>
 * </ul>
 *
 * <h2>Rounding</h2>
 * <p>All intermediate and final {@link BigDecimal} arithmetic uses {@link #SCALE}
 * and {@link #ROUNDING} so that financial precision is explicit and centralized
 * in one place.</p>
 */
public class HoldingPerformanceCalculator {

    /** Decimal scale used for all monetary BigDecimal results. */
    static final int SCALE = 2;

    /** Rounding mode — {@code HALF_UP} is the standard "banker's rounding". */
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ──────────────────────────────────────────────────────────────────────
    //  Mutable accumulator — package-private so tests can inspect if needed
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Mutable accumulator that collects running totals for a single ticker
     * while we iterate the transaction list.  Kept intentionally simple:
     * just four counters that map directly to the fields of {@link HoldingPerformance}.
     */
    static final class TickerAccumulator {
        /** Total number of shares ever purchased (BUY side only). */
        BigDecimal totalBoughtQty = BigDecimal.ZERO;

        /** Total cost of all purchased shares (quantity × unitPrice, BUY side). */
        BigDecimal totalBoughtCost = BigDecimal.ZERO;

        /** Sum of profit recorded on every SALE transaction. */
        BigDecimal realizedGain = BigDecimal.ZERO;

        /** Computes average purchase price = totalCost / totalQty, safely. */
        BigDecimal averagePurchasePrice() {
            if (totalBoughtQty.signum() == 0) return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            return totalBoughtCost.divide(totalBoughtQty, SCALE, ROUNDING);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Calculates holding performance for every ticker that appears in the
     * transaction list.
     *
     * @param portfolio     the portfolio aggregate (used to read remaining
     *                      shares and unrealized gain from the domain model)
     * @param transactions  full list of transactions — iterated exactly once
     * @param tickerPrices  live market prices keyed by ticker
     * @return immutable list of {@link HoldingPerformance} (one per ticker that had
     *         at least one stock transaction); tickers with a {@code null}
     *         ticker field (deposits / withdrawals) are silently skipped
     */
    public List<HoldingPerformance> getHoldingsPerformance(Portfolio portfolio,
                                                   List<? extends Transaction> transactions,
                                                   Map<Ticker, StockPrice> tickerPrices) {

        // 1. Single-pass aggregation — O(T)
        var accumulators = new LinkedHashMap<Ticker, TickerAccumulator>();

        for (var tx : transactions) {
            // Skip non-stock transactions (deposits, withdrawals have null ticker)
            if (tx.ticker() == null) continue;

            var acc = accumulators.computeIfAbsent(tx.ticker(), k -> new TickerAccumulator());

            // Java 21 switch expression — exhaustive, no fall-through
            switch (tx.type()) {
                case PURCHASE -> {
                    var qty = BigDecimal.valueOf(tx.quantity().value());
                    acc.totalBoughtQty = acc.totalBoughtQty.add(qty);
                    acc.totalBoughtCost = acc.totalBoughtCost.add(
                            tx.unitPrice().value().multiply(qty));
                }
                case SALE -> acc.realizedGain = acc.realizedGain.add(
                        tx.profit().amount());

                // Deposits and withdrawals should already be filtered by the null-ticker
                // guard above, but the switch must be exhaustive.
                case DEPOSIT, WITHDRAWAL -> { /* no-op for stock metrics */ }
            }
        }

        // 2. Build immutable result list
        var results = new ArrayList<HoldingPerformance>(accumulators.size());

        for (var entry : accumulators.entrySet()) {
            var ticker = entry.getKey();
            var acc = entry.getValue();

            var holding = portfolio.getHolding(ticker);
            var remaining = BigDecimal.valueOf(holding.getTotalShares().value());

            // Look up live price — may be absent if provider failed
            var stockPrice = tickerPrices.get(ticker);
            var currentPrice = (stockPrice != null)
                    ? stockPrice.price().value()
                    : BigDecimal.ZERO.setScale(SCALE, ROUNDING);

            var unrealizedGain = (stockPrice != null)
                    ? holding.getUnrealizedGain(stockPrice.price()).amount()
                    : BigDecimal.ZERO.setScale(SCALE, ROUNDING);

            results.add(new HoldingPerformance(
                    ticker.value(),
                    acc.totalBoughtQty,
                    remaining,
                    acc.averagePurchasePrice(),
                    currentPrice,
                    unrealizedGain,
                    acc.realizedGain.setScale(SCALE, ROUNDING)
            ));
        }

        return List.copyOf(results);
    }
}