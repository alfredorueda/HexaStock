package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.stereotype.Service;

@Service
public class GetStockPriceService implements GetStockPriceUseCase {

    private final StockPriceProviderPort stockPriceProviderPort;

    public GetStockPriceService(StockPriceProviderPort stockPriceProviderPort) {
        this.stockPriceProviderPort = stockPriceProviderPort;
    }

    public StockPrice getPrice(Ticker ticker) {

        return stockPriceProviderPort.fetchStockPrice(ticker);
    }
}
