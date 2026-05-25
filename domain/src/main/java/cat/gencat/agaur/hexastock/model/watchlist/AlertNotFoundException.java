package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Objects;

public final class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Ticker ticker, Money thresholdPrice) {
        super("Alert not found for ticker %s and threshold %s"
                .formatted(
                        Objects.requireNonNull(ticker, "ticker must not be null").value(),
                        Objects.requireNonNull(thresholdPrice, "thresholdPrice must not be null")
                ));
    }

    public AlertNotFoundException(Ticker ticker) {
        super("No alerts found for ticker %s"
                .formatted(Objects.requireNonNull(ticker, "ticker must not be null").value()));
    }
}

