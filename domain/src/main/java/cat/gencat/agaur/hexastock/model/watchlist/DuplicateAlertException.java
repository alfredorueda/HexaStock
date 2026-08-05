package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Objects;

public final class DuplicateAlertException extends RuntimeException {
    public DuplicateAlertException(Ticker ticker, Money thresholdPrice) {
        super("Duplicate alert for ticker %s and threshold %s"
                .formatted(
                        Objects.requireNonNull(ticker, "ticker must not be null").value(),
                        Objects.requireNonNull(thresholdPrice, "thresholdPrice must not be null")
                ));
    }
}

