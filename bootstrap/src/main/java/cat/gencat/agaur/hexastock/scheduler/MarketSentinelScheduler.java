package cat.gencat.agaur.hexastock.scheduler;

import cat.gencat.agaur.hexastock.watchlists.application.port.in.MarketSentinelUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketSentinelScheduler {

    private final MarketSentinelUseCase marketSentinelUseCase;

    public MarketSentinelScheduler(MarketSentinelUseCase marketSentinelUseCase) {
        this.marketSentinelUseCase = marketSentinelUseCase;
    }

    @Scheduled(fixedRateString = "${market.sentinel.interval:60000}")
    public void runDetection() {
        marketSentinelUseCase.detectBuySignals();
    }
}

