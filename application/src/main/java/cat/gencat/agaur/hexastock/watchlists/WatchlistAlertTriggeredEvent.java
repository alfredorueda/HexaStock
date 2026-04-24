package cat.gencat.agaur.hexastock.watchlists;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain event published by the Watchlists / Market Sentinel module when a price-threshold
 * alert is triggered for a ticker that the user is monitoring.
 *
 * <p>This event carries ONLY business data. Notification routing concerns
 * (Telegram chat ids, emails, phone numbers, etc.) are intentionally absent: they are
 * resolved by the Notifications module from the {@link #userId()} business identity.</p>
 *
 * <p>It is a pure POJO record — no Spring, no JPA, no framework dependency — so it can
 * be consumed by any in-process listener (Spring {@code @ApplicationModuleListener}) and
 * potentially externalised in the future without changing the domain.</p>
 *
 * @param watchlistId   identity of the {@code Watchlist} aggregate that triggered the alert
 * @param userId        business identifier of the watchlist owner (matches {@code Watchlist.ownerName})
 * @param ticker        symbol whose current price triggered the alert
 * @param alertType     kind of alert that fired (currently only {@code PRICE_THRESHOLD_REACHED})
 * @param threshold     configured threshold price the user is watching for
 * @param currentPrice  observed price that crossed the threshold
 * @param occurredOn    instant the alert was detected
 * @param message       optional human-readable note (may be {@code null})
 */
public record WatchlistAlertTriggeredEvent(
        String watchlistId,
        String userId,
        Ticker ticker,
        AlertType alertType,
        Money threshold,
        Money currentPrice,
        Instant occurredOn,
        String message
) {

    public WatchlistAlertTriggeredEvent {
        Objects.requireNonNull(watchlistId, "watchlistId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(ticker, "ticker is required");
        Objects.requireNonNull(alertType, "alertType is required");
        Objects.requireNonNull(threshold, "threshold is required");
        Objects.requireNonNull(currentPrice, "currentPrice is required");
        Objects.requireNonNull(occurredOn, "occurredOn is required");
    }

    public Optional<String> messageOptional() {
        return Optional.ofNullable(message);
    }

    public enum AlertType {
        PRICE_THRESHOLD_REACHED
    }
}
