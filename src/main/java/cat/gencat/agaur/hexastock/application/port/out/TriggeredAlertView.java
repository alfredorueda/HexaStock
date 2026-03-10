package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;

public record TriggeredAlertView(
        String ownerName,
        String listName,
        Ticker ticker,
        Money thresholdPrice
) {
}
