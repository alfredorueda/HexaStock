package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

/**
 * Read-side projection for Market Sentinel evaluation (CQRS query model).
 */
public record TriggeredAlertView(
        String ownerName,
        String listName,
        String userNotificationId,
        Ticker ticker,
        Money thresholdPrice
) {}

