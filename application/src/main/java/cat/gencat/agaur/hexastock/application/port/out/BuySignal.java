package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.Objects;

public final class BuySignal {

    private final String ownerName;
    private final String listName;
    private final String telegramChatId;
    private final Ticker ticker;
    private final Money thresholdPrice;
    private final StockPrice currentPrice;

    private BuySignal(String ownerName,
                      String listName,
                      String telegramChatId,
                      Ticker ticker,
                      Money thresholdPrice,
                      StockPrice currentPrice) {
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName must not be null");
        this.listName = Objects.requireNonNull(listName, "listName must not be null");
        this.telegramChatId = Objects.requireNonNull(telegramChatId, "telegramChatId must not be null");
        this.ticker = Objects.requireNonNull(ticker, "ticker must not be null");
        this.thresholdPrice = Objects.requireNonNull(thresholdPrice, "thresholdPrice must not be null");
        this.currentPrice = Objects.requireNonNull(currentPrice, "currentPrice must not be null");
    }

    public static BuySignal from(String ownerName,
                                 String listName,
                                 String telegramChatId,
                                 Ticker ticker,
                                 Money thresholdPrice,
                                 StockPrice currentPrice) {
        return new BuySignal(ownerName, listName, telegramChatId, ticker, thresholdPrice, currentPrice);
    }

    public String ownerName() {
        return ownerName;
    }

    public String listName() {
        return listName;
    }

    public String telegramChatId() {
        return telegramChatId;
    }

    public Ticker ticker() {
        return ticker;
    }

    public Money thresholdPrice() {
        return thresholdPrice;
    }

    public StockPrice currentPrice() {
        return currentPrice;
    }
}

