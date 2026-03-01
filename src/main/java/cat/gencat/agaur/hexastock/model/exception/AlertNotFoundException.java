package cat.gencat.agaur.hexastock.model.exception;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;

public class AlertNotFoundException extends DomainException {
    public AlertNotFoundException(Ticker ticker, Money thresholdPrice) {
        super("Alert not found for ticker " + ticker.value() + " at threshold " + thresholdPrice);
    }

    public AlertNotFoundException(Ticker ticker) {
        super("No alerts found for ticker " + ticker.value());
    }
}
