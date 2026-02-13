package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.in.ReportingUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.service.HoldingPerformanceCalculator;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service that orchestrates the holdings-performance report.
 *
 * <p>This service sits in the <strong>application layer</strong> of the hexagonal
 * architecture.  It coordinates infrastructure ports (persistence, stock-price
 * provider) and delegates the actual computation to the domain-layer
 * {@link HoldingPerformanceCalculator}.</p>
 *
 * <h2>Current design — sequential price fetching</h2>
 * <p>Stock prices are fetched sequentially via the default implementation of
 * {@link StockPriceProviderPort#fetchStockPrice(java.util.Set)}, which iterates
 * the ticker set and calls the single-ticker method one by one.  This is
 * intentional: the free-tier API (Finnhub) enforces strict rate limits, and
 * parallel calls would quickly trigger HTTP 429 responses.</p>
 *
 * <h2>Future improvements (not implemented — documented for pedagogy)</h2>
 * <ol>
 *   <li><strong>Batch price fetching</strong> — If the provider exposes a
 *       multi-symbol endpoint (e.g., {@code /quote?symbols=AAPL,MSFT}), the
 *       adapter can override the default method to fetch all prices in a single
 *       HTTP call, eliminating the N+1 problem entirely.</li>
 *   <li><strong>Parallel fetching with a bounded executor</strong> — Replace the
 *       sequential loop with {@code ExecutorService} (virtual threads or a
 *       fixed-size pool) to fetch prices concurrently while respecting rate
 *       limits.  This avoids the common-pool contention problem of
 *       {@code parallelStream()} and gives explicit control over concurrency.</li>
 *   <li><strong>Short-lived caching (e.g., 30 s TTL)</strong> — A Caffeine or
 *       Spring {@code @Cacheable} layer in front of the price adapter would
 *       deduplicate calls for the same ticker within a short window, reducing
 *       both latency and API quota consumption.</li>
 * </ol>
 * <p>These optimisations are omitted here because the current dataset size
 * (single-digit tickers per portfolio) does not justify the added complexity,
 * and the free-tier API would not benefit from parallelism.</p>
 *
 * <h2>Scalability thresholds</h2>
 * <p>In-memory processing with Java Streams is appropriate for up to ~100 000
 * transactions.  Beyond that, consider specialised read models, pagination, or
 * pushing aggregation into the persistence layer.</p>
 */
@Transactional
public class ReportingService implements ReportingUseCase {

    private final TransactionPort transactionPort;
    private final StockPriceProviderPort stockPriceProviderPort;
    private final PortfolioPort portfolioPort;
    private final HoldingPerformanceCalculator holdingPerformanceCalculator;

    public ReportingService(TransactionPort transactionPort,
                            StockPriceProviderPort stockPriceProviderPort,
                            PortfolioPort portfolioPort,
                            HoldingPerformanceCalculator holdingPerformanceCalculator) {
        this.transactionPort = transactionPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.portfolioPort = portfolioPort;
        this.holdingPerformanceCalculator = holdingPerformanceCalculator;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orchestration steps:</p>
     * <ol>
     *   <li>Load the portfolio aggregate (or throw if not found).</li>
     *   <li>Load the full transaction list for this portfolio.</li>
     *   <li>Collect the distinct tickers from the portfolio's holdings.</li>
     *   <li>Fetch live prices for those tickers (sequentially — see class-level doc).</li>
     *   <li>Delegate to {@link HoldingPerformanceCalculator} for the O(T) computation.</li>
     * </ol>
     */
    @Override
    public List<HoldingDTO> getHoldingsPerformance(String portfolioId) {
        var id = PortfolioId.of(portfolioId);

        var portfolio = portfolioPort.getPortfolioById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        var transactions = transactionPort.getTransactionsByPortfolioId(id);

        // Collect distinct tickers from current holdings (not from transactions,
        // because closed positions with zero shares are removed from holdings).
        var tickers = portfolio.getHoldings().stream()
                .map(Holding::getTicker)
                .collect(Collectors.toSet());

        var tickerPrices = stockPriceProviderPort.fetchStockPrice(tickers);

        return holdingPerformanceCalculator.getHoldingsPerformance(
                portfolio, transactions, tickerPrices);
    }
}