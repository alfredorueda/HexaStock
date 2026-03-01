package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.AlertEntry;

import java.math.BigDecimal;

public record WatchlistAlertDTO(String ticker, BigDecimal thresholdPrice) {
    public static WatchlistAlertDTO from(AlertEntry alertEntry) {
        return new WatchlistAlertDTO(alertEntry.ticker().value(), alertEntry.thresholdPrice().amount());
    }
}
