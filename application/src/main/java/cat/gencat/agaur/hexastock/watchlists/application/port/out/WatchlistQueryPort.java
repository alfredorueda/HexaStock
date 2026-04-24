package cat.gencat.agaur.hexastock.watchlists.application.port.out;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.List;
import java.util.Set;

public interface WatchlistQueryPort {
    Set<Ticker> findDistinctTickersInActiveWatchlists();

    List<TriggeredAlertView> findTriggeredAlerts(Ticker ticker, Money currentPrice);
}

