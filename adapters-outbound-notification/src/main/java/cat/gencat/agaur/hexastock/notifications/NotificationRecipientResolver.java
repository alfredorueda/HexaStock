package cat.gencat.agaur.hexastock.notifications;

/**
 * Resolves a business {@code userId} into a {@link NotificationRecipient} containing
 * one or more channel-specific destinations.
 *
 * <p>This is an internal port of the Notifications module — it must NOT be exposed to
 * the Watchlists module.</p>
 */
public interface NotificationRecipientResolver {

    NotificationRecipient resolve(String userId);
}
