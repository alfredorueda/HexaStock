package cat.gencat.agaur.hexastock.model.exception;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;

public class DuplicateAlertException extends DomainException {
    public DuplicateAlertException(Ticker ticker, Money thresholdPrice) {
        super("Alert already exists for ticker " + ticker.value() + " at threshold " + thresholdPrice);
    }
}
