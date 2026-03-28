package cat.gencat.agaur.hexastock.model.portfolio;

import java.math.BigDecimal;

/**
 * Domain value object representing the computed performance metrics
 * for a single holding (ticker) in a portfolio.
 *
 * <p>This record lives in the domain layer so that domain services
 * can produce holding performance results without depending on
 * adapter-level DTOs.</p>
 */
public record HoldingPerformance(
        String ticker,
        BigDecimal quantity,
        BigDecimal remaining,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal unrealizedGain,
        BigDecimal realizedGain
) {}
