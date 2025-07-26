package cat.gencat.agaur.hexastock.adapter;


import cat.gencat.agaur.hexastock.application.port.in.*;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.application.service.*;
import cat.gencat.agaur.hexastock.model.service.HoldingPerformanceCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring application configuration, making Spring beans from services defined in application
 * module.
 *
 * @author Francisco José Nebrera Rodríguez
 */
@SpringBootApplication
public class SpringAppConfig {

  @Autowired
  TransactionPort transactionPort;

  @Autowired
  StockPriceProviderPort stockPriceProviderPort;

  @Autowired
  PortfolioPort portfolioPort;

  @Bean
  ReportingUseCase getReportingUseCase() {
    return new ReportingService(transactionPort, stockPriceProviderPort, portfolioPort, holdingPerformanceCalculator() );
  }

  @Bean
  HoldingPerformanceCalculator holdingPerformanceCalculator() {
    return new HoldingPerformanceCalculator();
  }

  @Bean
  GetStockPriceUseCase getStockPriceUseCase() {
    return new GetStockPriceService(stockPriceProviderPort);
  }

  @Bean
  PortfolioManagementUseCase getPortfolioManagementUseCase() {
    return new PortfolioManagementService(portfolioPort, transactionPort);
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
  HoldingPerformanceCalculator getHoldingPerformanceCalculator() {
    return new HoldingPerformanceCalculator();
  }


}