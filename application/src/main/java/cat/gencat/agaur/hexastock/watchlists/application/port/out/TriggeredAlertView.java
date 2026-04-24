package cat.gencat.agaur.hexastock.watchlists.application.port.out;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

/**
 * Read-side projection for Market Sentinel evaluation (CQRS query model).
 *
 * <p>Carries only watchlist business identity. Notification routing data is intentionally
 * absent — it now belongs to the Notifications bounded context.</p>
 */
public record TriggeredAlertView(
        String watchlistId,
        String ownerName,
        String listName,
        Ticker ticker,
        Money thresholdPrice
) {}
