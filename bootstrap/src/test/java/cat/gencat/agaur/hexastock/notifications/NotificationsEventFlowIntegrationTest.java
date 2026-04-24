package cat.gencat.agaur.hexastock.notifications;

import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.notifications.adapter.logging.LoggingNotificationSenderAdapter;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent;
import cat.gencat.agaur.hexastock.watchlists.WatchlistAlertTriggeredEvent.AlertType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Integration test for the end-to-end event flow that the Spring Modulith POC introduces
 * between the {@code watchlists} and {@code notifications} modules.
 *
 * <p>The test boots a full Spring context and publishes a
 * {@link WatchlistAlertTriggeredEvent} through the application-side
 * {@link DomainEventPublisher} port (the same path used in production by
 * {@code MarketSentinelService}). It then asserts that
 * {@link WatchlistAlertNotificationListener} consumed the event and dispatched it to
 * {@link LoggingNotificationSenderAdapter} — the always-on default channel — proving
 * that:</p>
 * <ul>
 *   <li>the publisher port is correctly wired to Spring's {@code ApplicationEventPublisher};</li>
 *   <li>the {@link org.springframework.modulith.events.ApplicationModuleListener} resolves
 *       and invokes asynchronously after commit; and</li>
 *   <li>the default {@link CompositeNotificationRecipientResolver} produces a recipient with
 *       at least one logging destination, even when no Telegram chat id is configured.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles({"test", "jpa", "mockfinhub"})
@DisplayName("Notifications module - event flow")
class NotificationsEventFlowIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private LoggingNotificationSenderAdapter loggingSender;

    @Test
    @DisplayName("publishing a WatchlistAlertTriggeredEvent reaches the logging sender")
    void watchlistAlertEventTriggersLoggingSender() {
        WatchlistAlertTriggeredEvent event = new WatchlistAlertTriggeredEvent(
                UUID.randomUUID().toString(),
                "alice",
                new Ticker("AAPL"),
                AlertType.PRICE_THRESHOLD_REACHED,
                new Money(new BigDecimal("150.00")),
                new Money(new BigDecimal("155.50")),
                Instant.parse("2026-01-15T10:00:00Z"),
                "AAPL crossed 150.00"
        );

        // The listener is @ApplicationModuleListener (after-commit + async), so it only
        // fires when the event is published inside a transaction that successfully commits.
        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(event));

        // The listener is @ApplicationModuleListener (after-commit + async), so we wait for
        // the asynchronous dispatch to invoke the always-on logging sender.
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(loggingSender, atLeastOnce())
                        .send(any(LoggingNotificationDestination.class), any(WatchlistAlertTriggeredEvent.class)));

        assertThat(loggingSender).isNotNull();
    }
}
