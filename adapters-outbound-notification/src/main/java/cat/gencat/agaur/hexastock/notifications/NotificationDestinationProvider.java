package cat.gencat.agaur.hexastock.notifications;

import java.util.List;

/**
 * Strategy that produces zero or more {@link NotificationDestination}s for a given
 * {@code userId} and a single notification channel.
 *
 * <p>One implementation per supported channel. Implementations are profile-gated to
 * mirror the lifecycle of their corresponding {@link NotificationSender} (for example,
 * the Telegram provider is only loaded when the {@code telegram-notifications} Spring
 * profile is active). This keeps the resolver itself channel-agnostic and prevents
 * channel-specific configuration leakage when a channel is disabled.</p>
 *
 * <p>The order in which providers are returned by Spring is irrelevant: the recipient
 * is just an aggregation of every destination produced by every active provider.</p>
 */
public interface NotificationDestinationProvider {

    /**
     * @param userId the business identity of the recipient (matches
     *               {@code WatchlistAlertTriggeredEvent.userId()})
     * @return destinations this provider knows about for {@code userId}, possibly empty.
     */
    List<NotificationDestination> destinationsFor(String userId);
}
