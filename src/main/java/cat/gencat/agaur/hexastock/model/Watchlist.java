package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.AlertNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.DuplicateAlertException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Watchlist {

    private final WatchlistId id;
    private final String ownerName;
    private final String listName;
    private boolean active;
    private final List<AlertEntry> alerts = new ArrayList<>();

    public Watchlist(WatchlistId id, String ownerName, String listName, boolean active, List<AlertEntry> alerts) {
        this.id = Objects.requireNonNull(id, "Watchlist id must not be null");
        this.ownerName = validateNotBlank(ownerName, "Owner name must not be blank");
        this.listName = validateNotBlank(listName, "List name must not be blank");
        this.active = active;
        Objects.requireNonNull(alerts, "Alerts must not be null");
        this.alerts.addAll(alerts);
    }

    public static Watchlist create(WatchlistId id, String ownerName, String listName) {
        return new Watchlist(id, ownerName, listName, true, List.of());
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
        if (!alerts.remove(target)) {
            throw new AlertNotFoundException(ticker, thresholdPrice);
        }
    }

    public void removeAllAlertsForTicker(Ticker ticker) {
        boolean removed = alerts.removeIf(alert -> alert.ticker().equals(ticker));
        if (!removed) {
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
        return Collections.unmodifiableList(alerts);
    }

    public List<AlertEntry> getAlertsForTicker(Ticker ticker) {
        return alerts.stream()
                .filter(alert -> alert.ticker().equals(ticker))
                .toList();
    }

    private static String validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
