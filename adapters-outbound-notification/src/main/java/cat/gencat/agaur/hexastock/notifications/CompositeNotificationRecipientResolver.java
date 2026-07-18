package cat.gencat.agaur.hexastock.notifications;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Composite {@link NotificationRecipientResolver} that aggregates every active
 * {@link NotificationDestinationProvider} into a single recipient.
 *
 * <p>The resolver is intentionally channel-agnostic: it does not know whether Telegram,
 * email, or SMS exist. Each channel contributes its own {@code NotificationDestinationProvider}
 * (profile-gated where appropriate). Adding a new channel to the system therefore requires
 * <em>zero</em> changes here.</p>
 *
 * <p>Replace this with a persistent resolver (database, configuration service, etc.) by
 * declaring another bean of {@link NotificationRecipientResolver} marked
 * {@code @Primary}; this default has no {@code @ConditionalOnMissingBean} guard, so
 * overriding requires the {@code @Primary} marker.</p>
 *
 * <p>Properly registering Telegram chat ids "live" (e.g. when a user contacts the bot)
 * is intentionally out of scope for this POC and would be a natural next step using
 * another in-process domain event consumed by a persistent provider.</p>
 */
@Component
public class CompositeNotificationRecipientResolver implements NotificationRecipientResolver {

    private final List<NotificationDestinationProvider> providers;

    public CompositeNotificationRecipientResolver(List<NotificationDestinationProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public NotificationRecipient resolve(String userId) {
        List<NotificationDestination> destinations = providers.stream()
                .flatMap(provider -> provider.destinationsFor(userId).stream())
                .toList();
        return new NotificationRecipient(userId, destinations);
    }
}
