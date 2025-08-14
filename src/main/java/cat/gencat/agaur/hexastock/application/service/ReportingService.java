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
import java.util.Map;
import java.util.stream.Collectors;


/*

For 20,000 transactions, in-memory processing with Java Streams is likely the best approach for your hexagonal architecture. This keeps your business logic in the domain layer where it belongs while maintaining reasonable performance.

Here's why:


Memory consumption is manageable: 20,000 transaction objects would typically consume between 10-50MB of heap space (depending on object size), which is insignificant for modern JVMs.


Domain logic clarity: Complex financial calculations are more clearly expressed and maintained in your domain layer using Java than in SQL.


Architectural integrity: This approach maintains the hexagonal architecture principle of keeping business logic in the domain.


Practical Thresholds
The threshold where in-memory processing becomes problematic typically occurs around:


100,000-500,000 transactions depending on transaction complexity
When total memory consumption exceeds ~25% of available heap space
When calculation time exceeds acceptable response times for your use case

Hybrid Approach for Scalability
As your system grows, consider these optimizations:


Implement lazy loading patterns for transactions when feasible
Create specialized read models for performance-critical calculations
Consider pagination or windowing for extremely large portfolios
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
        // Constructor injection for required ports and services
        // This ensures that the service has all dependencies it needs to function correctly
        this.transactionPort = transactionPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.portfolioPort = portfolioPort;
        this.holdingPerformanceCalculator = holdingPerformanceCalculator;
    }

    @Override
    public List<HoldingDTO> getHoldingsPerfomance(String portfolioId) {

        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId).orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        List<Transaction> transactions = transactionPort.getTransactionsByPortfolioId(portfolioId);

        var tickers = portfolio.getHoldings().stream()
                .map(Holding::getTicker)
                .collect(Collectors.toSet());

        Map<Ticker, StockPrice> tickerStockPriceMap = stockPriceProviderPort.fetchStockPrice(tickers);

        return holdingPerformanceCalculator.getHoldingsPerformance(portfolio, transactions, tickerStockPriceMap);
    }

}