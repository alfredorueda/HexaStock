package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.model.StockPrice;

import java.time.Instant;

public record StockPriceDTO(
        String symbol, double price, Instant time, String currency
) {

    public static StockPriceDTO fromDomainModel(StockPrice stockPrice) { return new StockPriceDTO(stockPrice.getTicker().value(), stockPrice.getPrice(), stockPrice.getTime(), stockPrice.getCurrency()); }

}
