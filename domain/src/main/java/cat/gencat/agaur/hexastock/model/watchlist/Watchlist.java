package cat.gencat.agaur.hexastock.model.watchlist;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Watchlist is an Aggregate Root containing price-threshold alerts to monitor.
 *
 * <p>Pure domain object. Notification routing (Telegram chat ids, emails, phone numbers, etc.)
 * is intentionally NOT modeled here: it is an infrastructure concern owned by the
 * Notifications bounded context. The Watchlist only knows the business identity of its
 * owner ({@code ownerName}), which is the {@code userId} carried in domain events.</p>
 */
public class Watchlist {

    private final WatchlistId id;
    private final String ownerName;

    private final String listName;
    private boolean active;
    private final List<AlertEntry> alerts = new ArrayList<>();

    private Watchlist(WatchlistId id,
                      String ownerName,
                      String listName,
                      boolean active,
                      List<AlertEntry> alerts) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerName = requireNonBlank(ownerName, "ownerName must not be blank");
        this.listName = requireNonBlank(listName, "listName must not be blank");
        this.active = active;
        this.alerts.addAll(List.copyOf(Objects.requireNonNull(alerts, "alerts must not be null")));
    }

    public static Watchlist create(WatchlistId id, String ownerName, String listName) {
        return new Watchlist(id, ownerName, listName, true, List.of());
    }

    /**
     * Reconstitution factory used by persistence adapters to rebuild an aggregate
     * from its stored state without going through the {@link #create} business rules.
     */
    public static Watchlist rehydrate(WatchlistId id,
                                      String ownerName,
                                      String listName,
                                      boolean active,
                                      List<AlertEntry> alerts) {
        return new Watchlist(id, ownerName, listName, active, alerts);
    }

    public void addAlert(Ticker ticker, Money thresholdPrice) {
        AlertEntry newAlert = new AlertEntry(ticker, thresholdPrice);
        if (alerts.contains(newAlert)) {
            throw new DuplicateAlertException(ticker, thresholdPrice);
        }
        alerts.add(newAlert);
    }

    public void removeAlert(Ticker ticker, Money thresholdPrice) {
        AlertEntry target = new AlertEntry(ticker, thresholdPrice);
        boolean removed = alerts.remove(target);
        if (!removed) {
            throw new AlertNotFoundException(ticker, thresholdPrice);
        }
    }

    public void removeAllAlertsForTicker(Ticker ticker) {
        boolean removedAny = alerts.removeIf(alert -> alert.ticker().equals(ticker));
        if (!removedAny) {
            throw new AlertNotFoundException(ticker);
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public WatchlistId getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getListName() {
        return listName;
    }

    public boolean isActive() {
        return active;
    }

    public List<AlertEntry> getAlerts() {
        return List.copyOf(alerts);
    }

    public List<AlertEntry> getAlertsForTicker(Ticker ticker) {
        return alerts.stream()
                .filter(a -> a.ticker().equals(ticker))
                .toList();
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}

