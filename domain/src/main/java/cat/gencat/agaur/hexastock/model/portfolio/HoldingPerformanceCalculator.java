package cat.gencat.agaur.hexastock.model.portfolio;

import cat.gencat.agaur.hexastock.model.transaction.*;
import cat.gencat.agaur.hexastock.model.market.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes per-ticker performance from historical transactions, remaining lots
 * and a supplied quote map, without infrastructure access.
 *
 * <p>A single transaction pass builds a {@code LinkedHashMap} of accumulators.
 * Result construction also visits historical tickers and their remaining lots.
 * With T transactions, K historical tickers and L visited lots, approximate work
 * is O(T + K + L), with O(K) additional accumulator/result space under ordinary
 * map-access assumptions. Loaded inputs require separate memory.</p>
 *
 * <p>Average historical purchase price uses division at scale 2 with HALF_UP.
 * Historical purchased cost is distinct from the remaining lots' cost basis.
 * A missing quote-map entry yields zero current price and unrealized gain;
 * the application service does not catch provider exceptions to create this case.</p>
 */
public class HoldingPerformanceCalculator {

    /** Decimal scale used for all monetary BigDecimal results. */
    static final int SCALE = 2;

    /** Rounding mode for ties away from zero; distinct from HALF_EVEN. */
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ──────────────────────────────────────────────────────────────────────
    //  Mutable accumulator — package-private so tests can inspect if needed
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Mutable accumulator that collects running totals for a single ticker
     * while we iterate the transaction list.  Kept intentionally simple:
     * three counters that map directly to the fields of {@link HoldingPerformance}.
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

            // A caller may supply a map without this ticker; provider failures propagate upstream.
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