package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.application.service.MarketSentinelService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketSentinelScheduler {

    private final MarketSentinelService marketSentinelService;

    public MarketSentinelScheduler(MarketSentinelService marketSentinelService) {
        this.marketSentinelService = marketSentinelService;
    }

    @Scheduled(fixedRateString = "${market.sentinel.interval:60000}")
    public void runDetection() {
        marketSentinelService.detectBuySignals();
    }
}
