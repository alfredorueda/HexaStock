package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.model.portfolio.HoldingPerformance;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.in.ReportingUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.portfolio.*;
import cat.gencat.agaur.hexastock.model.portfolio.HoldingPerformanceCalculator;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Coordinates portfolio/history loading and quote lookup, then delegates
 * reporting arithmetic to {@link HoldingPerformanceCalculator}.
 *
 * <p>The default set-based quote method calls providers sequentially. That alone
 * is not rate limiting. Real provider adapters already use the stockPrices cache;
 * the random mock has different behavior. Provider exceptions propagate.</p>
 *
 * <p>The request loads full history and the portfolio graph. Performance depends
 * on their sizes, quote/cache behavior, database work, contention and concurrent
 * requests. No universal transaction-count capacity threshold is established.
 * Dedicated query ports or projections are possible future designs.</p>
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
     *   <li>Delegate to {@link HoldingPerformanceCalculator} for aggregation and remaining-lot calculations.</li>
     * </ol>
     */
    @Override
    public List<HoldingPerformance> getHoldingsPerformance(String portfolioId) {
        var id = PortfolioId.of(portfolioId);

        var portfolio = portfolioPort.getPortfolioById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        var transactions = transactionPort.getTransactionsByPortfolioId(id);

        // Collect holding tickers, including empty holdings retained after liquidation.
        var tickers = portfolio.getHoldings().stream()
                .map(Holding::getTicker)
                .collect(Collectors.toSet());

        var tickerPrices = stockPriceProviderPort.fetchStockPrice(tickers);

        return holdingPerformanceCalculator.getHoldingsPerformance(
                portfolio, transactions, tickerPrices);
    }
}