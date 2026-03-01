package cat.gencat.agaur.hexastock.adapter.out.notification;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationAdapter.class);

    @Override
    public void notifyBuySignal(BuySignal signal) {
        log.info("BUY SIGNAL: {} should consider buying {} (watchlist: {}, threshold: {}, current: {})",
                signal.ownerName(),
                signal.ticker().value(),
                signal.listName(),
                signal.thresholdPrice(),
                signal.currentPrice().price().toMoney()
        );
    }
}
