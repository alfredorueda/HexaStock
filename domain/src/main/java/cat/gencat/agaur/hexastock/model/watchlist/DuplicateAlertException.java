package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

public class DuplicateAlertException extends RuntimeException {
    public DuplicateAlertException(Ticker ticker, Money thresholdPrice) {
        super("Duplicate alert for ticker " + ticker.value() + " and threshold " + thresholdPrice);
    }
}

