package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.SaleResponseDTO;
import cat.gencat.agaur.hexastock.model.SellResult;
import cat.gencat.agaur.hexastock.model.Ticker;

public interface PortfolioStockOperationsUseCase {

    void buyStock(String portfolioId, Ticker ticker, int quantity);
    SellResult sellStock(String portfolioId, Ticker ticker, int quantity);
}
