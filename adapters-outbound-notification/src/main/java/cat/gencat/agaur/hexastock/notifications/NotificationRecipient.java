package cat.gencat.agaur.hexastock.notifications;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate of all {@link NotificationDestination}s configured for a single user.
 *
 * <p>Resolved by a {@link NotificationRecipientResolver} from the business {@code userId}
 * carried in {@link cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent}.</p>
 */
public record NotificationRecipient(String userId, List<NotificationDestination> destinations) {

    public NotificationRecipient {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(destinations, "destinations is required");
        destinations = List.copyOf(destinations);
    }
}
