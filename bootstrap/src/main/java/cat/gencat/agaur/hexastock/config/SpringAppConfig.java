package cat.gencat.agaur.hexastock.config;

import cat.gencat.agaur.hexastock.application.port.in.*;
import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.application.service.*;
import cat.gencat.agaur.hexastock.model.portfolio.HoldingPerformanceCalculator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring application configuration, making Spring beans from services defined in application
 * module.
 *
 * @author Francisco José Nebrera Rodríguez
 */
@Configuration
@EnableCaching
public class SpringAppConfig {

  private final TransactionPort transactionPort;
  private final StockPriceProviderPort stockPriceProviderPort;
  private final PortfolioPort portfolioPort;
  private final WatchlistPort watchlistPort;
  private final WatchlistQueryPort watchlistQueryPort;

  public SpringAppConfig(TransactionPort transactionPort,
                         StockPriceProviderPort stockPriceProviderPort,
                         PortfolioPort portfolioPort,
                         WatchlistPort watchlistPort,
                         WatchlistQueryPort watchlistQueryPort) {
    this.transactionPort = transactionPort;
    this.stockPriceProviderPort = stockPriceProviderPort;
    this.portfolioPort = portfolioPort;
    this.watchlistPort = watchlistPort;
    this.watchlistQueryPort = watchlistQueryPort;
  }

  @Bean
  HoldingPerformanceCalculator holdingPerformanceCalculator() {
    return new HoldingPerformanceCalculator();
  }

  @Bean
  ReportingUseCase getReportingUseCase() {
    return new ReportingService(transactionPort, stockPriceProviderPort, portfolioPort, holdingPerformanceCalculator());
  }

  @Bean
  GetStockPriceUseCase getStockPriceUseCase() {
    return new GetStockPriceService(stockPriceProviderPort);
  }

  @Bean
  PortfolioLifecycleUseCase getPortfolioLifecycleUseCase() {
    return new PortfolioLifecycleService(portfolioPort);
  }

  @Bean
  CashManagementUseCase getCashManagementUseCase() {
    return new CashManagementService(portfolioPort, transactionPort);
  }

  @Bean
  PortfolioStockOperationsUseCase getPortfolioStockOperationsUseCase() {
    return new PortfolioStockOperationsService(portfolioPort, stockPriceProviderPort, transactionPort);
  }

  @Bean
  TransactionUseCase getTransactionUseCase() {
    return new TransactionService(transactionPort);
  }

  @Bean
  WatchlistUseCase getWatchlistUseCase() {
    return new WatchlistService(watchlistPort);
  }

  @Bean
  MarketSentinelUseCase getMarketSentinelService(DomainEventPublisher domainEventPublisher) {
    return new MarketSentinelService(watchlistQueryPort, stockPriceProviderPort, domainEventPublisher);
  }
}