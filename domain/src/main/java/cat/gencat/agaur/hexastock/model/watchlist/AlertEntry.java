package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Objects;

/**
 * AlertEntry is a Value Object representing a price-threshold alert for a ticker.
 */
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

