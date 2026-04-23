package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Objects;

public record BuySignal(
        String ownerName,
        String listName,
        String userNotificationId,
        Ticker ticker,
        Money thresholdPrice,
        StockPrice currentPrice
) {
    public BuySignal {
        Objects.requireNonNull(ownerName, "ownerName is required");
        Objects.requireNonNull(listName, "listName is required");
        Objects.requireNonNull(userNotificationId, "userNotificationId is required");
        Objects.requireNonNull(ticker, "ticker is required");
        Objects.requireNonNull(thresholdPrice, "thresholdPrice is required");
        Objects.requireNonNull(currentPrice, "currentPrice is required");
    }

    public static BuySignal from(TriggeredAlertView view, StockPrice currentPrice) {
        Objects.requireNonNull(view, "view is required");
        Objects.requireNonNull(currentPrice, "currentPrice is required");

        if (!view.ticker().equals(currentPrice.ticker())) {
            throw new IllegalArgumentException(
                    "Ticker mismatch: alert view %s vs price %s"
                            .formatted(view.ticker(), currentPrice.ticker())
            );
        }

        return new BuySignal(
                view.ownerName(),
                view.listName(),
                view.userNotificationId(),
                view.ticker(),
                view.thresholdPrice(),
                currentPrice
        );
    }
}

