package cat.gencat.agaur.hexastock.watchlists.application.port.in;

/**
 * Primary port for Market Sentinel detection (CQRS read side).
 */
public interface MarketSentinelUseCase {
    void detectBuySignals();
}

