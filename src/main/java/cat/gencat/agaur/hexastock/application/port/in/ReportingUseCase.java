package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;

import java.util.List;

public interface ReportingUseCase {

    List<HoldingDTO> getHoldingsPerfomance(String portfolioId);

}