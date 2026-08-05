package cat.gencat.agaur.hexastock.notifications;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link CompositeNotificationRecipientResolver}.
 *
 * <p>Asserts the contract of the post-Phase-5 channel-agnostic resolver: it does not
 * know about Telegram, logging, or any other channel; it merely aggregates whatever
 * {@link NotificationDestinationProvider} beans Spring activated for the current
 * profile set.</p>
 */
@DisplayName("CompositeNotificationRecipientResolver")
class CompositeNotificationRecipientResolverTest {

    @Nested
    @DisplayName("when no providers are registered")
    class NoProviders {

        @Test
        @DisplayName("resolves to a recipient with zero destinations")
        void noProvidersYieldsNoDestinations() {
            CompositeNotificationRecipientResolver resolver =
                    new CompositeNotificationRecipientResolver(List.of());

            NotificationRecipient recipient = resolver.resolve("alice");

            assertThat(recipient.userId()).isEqualTo("alice");
            assertThat(recipient.destinations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("with multiple providers")
    class MultipleProviders {

        @Test
        @DisplayName("aggregates destinations from every provider in order")
        void aggregatesAllProviders() {
            NotificationDestinationProvider logging =
                    userId -> List.of(new LoggingNotificationDestination(userId));
            NotificationDestinationProvider telegram =
                    userId -> List.of(new TelegramNotificationDestination("chat-" + userId));

            CompositeNotificationRecipientResolver resolver =
                    new CompositeNotificationRecipientResolver(List.of(logging, telegram));

            NotificationRecipient recipient = resolver.resolve("bob");

            assertThat(recipient.userId()).isEqualTo("bob");
            assertThat(recipient.destinations())
                    .hasSize(2)
                    .anyMatch(d -> d instanceof LoggingNotificationDestination)
                    .anyMatch(d -> d instanceof TelegramNotificationDestination);
        }

        @Test
        @DisplayName("a provider returning an empty list contributes nothing")
        void emptyProviderContributesNothing() {
            NotificationDestinationProvider logging =
                    userId -> List.of(new LoggingNotificationDestination(userId));
            NotificationDestinationProvider silentChannel = userId -> List.of();

            CompositeNotificationRecipientResolver resolver =
                    new CompositeNotificationRecipientResolver(List.of(logging, silentChannel));

            NotificationRecipient recipient = resolver.resolve("carol");

            assertThat(recipient.destinations())
                    .singleElement()
                    .isInstanceOf(LoggingNotificationDestination.class);
        }
    }
}
