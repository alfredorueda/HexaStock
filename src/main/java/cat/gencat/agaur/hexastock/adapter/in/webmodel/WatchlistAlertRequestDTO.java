package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import java.math.BigDecimal;

public record WatchlistAlertRequestDTO(String ticker, BigDecimal thresholdPrice) {
}
