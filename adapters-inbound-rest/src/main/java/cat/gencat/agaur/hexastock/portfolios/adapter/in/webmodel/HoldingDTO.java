package cat.gencat.agaur.hexastock.portfolios.adapter.in.webmodel;

import java.math.BigDecimal;

public record HoldingDTO(String ticker, BigDecimal quantity, BigDecimal remaining, BigDecimal averagePurchasePrice, BigDecimal currentPrice, BigDecimal unrealizedGain, BigDecimal realizedGain) {}

