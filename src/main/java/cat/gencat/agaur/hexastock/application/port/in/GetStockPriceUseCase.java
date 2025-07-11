package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

public interface GetStockPriceUseCase {

    StockPrice getPrice(Ticker ticker);
}
