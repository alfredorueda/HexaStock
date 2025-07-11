package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

public interface StockPriceProviderPort {

    StockPrice fetchStockPrice(Ticker ticker);
}