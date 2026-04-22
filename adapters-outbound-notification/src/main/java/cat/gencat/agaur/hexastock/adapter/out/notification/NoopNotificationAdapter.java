package cat.gencat.agaur.hexastock.adapter.out.notification;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No-op implementation used when Telegram profile is not active.
 */
@Component
@Profile("!telegram")
public class NoopNotificationAdapter implements NotificationPort {
    @Override
    public void notifyBuySignal(BuySignal signal) {
        // intentionally no-op
    }
}

