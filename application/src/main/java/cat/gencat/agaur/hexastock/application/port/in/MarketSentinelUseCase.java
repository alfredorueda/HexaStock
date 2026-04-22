package cat.gencat.agaur.hexastock.application.port.in;

/**
 * Primary port for Market Sentinel detection (CQRS read side).
 */
public interface MarketSentinelUseCase {
    void detectBuySignals();
}

