package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.HoldingPerformance;

import java.util.List;

public interface ReportingUseCase {

    List<HoldingPerformance> getHoldingsPerformance(String portfolioId);

}