package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;

public record BuySignal(
        String ownerName,
        String listName,
        Ticker ticker,
        Money thresholdPrice,
        StockPrice currentPrice
) {
}
