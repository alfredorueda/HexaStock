package cat.gencat.agaur.hexastock.model;

import java.util.Objects;

public record AlertEntry(
        Ticker ticker,
        Money thresholdPrice
) {
    public AlertEntry {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(thresholdPrice, "Threshold price must not be null");
        if (!thresholdPrice.isPositive()) {
            throw new IllegalArgumentException("Threshold price must be positive");
        }
    }
}
