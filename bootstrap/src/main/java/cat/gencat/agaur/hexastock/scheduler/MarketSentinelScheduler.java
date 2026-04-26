package cat.gencat.agaur.hexastock.scheduler;

import cat.gencat.agaur.hexastock.watchlists.application.port.in.MarketSentinelUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MarketSentinelScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketSentinelScheduler.class);

    private final MarketSentinelUseCase marketSentinelUseCase;

    public MarketSentinelScheduler(MarketSentinelUseCase marketSentinelUseCase) {
        this.marketSentinelUseCase = marketSentinelUseCase;
    }

    /**
     * One sentinel cycle. Wrapped in a transaction so that any
     * {@code WatchlistAlertTriggeredEvent} published by the application service
     * is dispatched to {@code @ApplicationModuleListener} consumers AFTER_COMMIT;
     * without an active transaction Spring's transactional event listeners
     * silently drop the event.
     */
    @Scheduled(fixedRateString = "${market.sentinel.interval:60000}")
    @Transactional
    public void runDetection() {
        long startedAt = System.currentTimeMillis();
        log.info("MARKET_SENTINEL_TICK started");
        try {
            marketSentinelUseCase.detectBuySignals();
            log.info("MARKET_SENTINEL_TICK finished durationMs={}", System.currentTimeMillis() - startedAt);
        } catch (RuntimeException e) {
            log.warn("MARKET_SENTINEL_TICK failed durationMs={} error={}",
                    System.currentTimeMillis() - startedAt, e.toString(), e);
            throw e;
        }
    }
}

