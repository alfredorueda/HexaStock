package cat.gencat.agaur.hexastock.notifications;

/**
 * Where a notification can be delivered for a given user.
 *
 * <p>Sealed type so the dispatch logic can exhaustively pattern-match against all
 * supported destinations. New channels add a new permitted record here and a matching
 * {@link NotificationSender}.</p>
 */
public sealed interface NotificationDestination
        permits LoggingNotificationDestination,
                TelegramNotificationDestination {

    NotificationChannel channel();
}
