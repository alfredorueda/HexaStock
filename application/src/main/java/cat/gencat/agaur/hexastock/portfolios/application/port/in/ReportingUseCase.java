package cat.gencat.agaur.hexastock.portfolios.application.port.in;

import cat.gencat.agaur.hexastock.portfolios.model.portfolio.HoldingPerformance;

import java.util.List;

public interface ReportingUseCase {

    List<HoldingPerformance> getHoldingsPerformance(String portfolioId);

}