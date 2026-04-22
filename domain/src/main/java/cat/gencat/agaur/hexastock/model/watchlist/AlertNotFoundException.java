package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Ticker ticker, Money thresholdPrice) {
        super("Alert not found for ticker " + ticker.value() + " and threshold " + thresholdPrice);
    }

    public AlertNotFoundException(Ticker ticker) {
        super("No alerts found for ticker " + ticker.value());
    }
}

