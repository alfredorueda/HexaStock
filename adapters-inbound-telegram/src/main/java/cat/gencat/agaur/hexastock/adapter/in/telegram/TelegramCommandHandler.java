package cat.gencat.agaur.hexastock.adapter.in.telegram;

import cat.gencat.agaur.hexastock.application.exception.WatchlistNotFoundException;
import cat.gencat.agaur.hexastock.watchlists.application.port.in.WatchlistUseCase;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.DuplicateAlertException;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;

public class TelegramCommandHandler {

    private final WatchlistUseCase watchlistUseCase;

    public TelegramCommandHandler(WatchlistUseCase watchlistUseCase) {
        this.watchlistUseCase = watchlistUseCase;
    }

    public String handle(TelegramCommand command, String ownerName, String chatId) {
        // chatId is intentionally not propagated into the Watchlist domain anymore.
        // Notification routing is now owned by the Notifications module, which resolves
        // the recipient from the business userId (== ownerName). The Telegram chat id is
        // only used by the webhook controller to send back the bot reply.
        try {
            return switch (command) {
                case TelegramCommand.CreateWatchlist(String listName) -> {
                    Watchlist created = watchlistUseCase.createWatchlist(ownerName, listName);
                    yield "Watchlist creada: id=" + created.getId().value() + " name=" + created.getListName();
                }
                case TelegramCommand.DeleteWatchlist(String watchlistId) -> {
                    watchlistUseCase.deleteWatchlist(WatchlistId.of(watchlistId));
                    yield "Watchlist borrada: id=" + watchlistId;
                }
                case TelegramCommand.ActivateWatchlist(String watchlistId) -> {
                    watchlistUseCase.activate(WatchlistId.of(watchlistId));
                    yield "Watchlist activada: id=" + watchlistId;
                }
                case TelegramCommand.DeactivateWatchlist(String watchlistId) -> {
                    watchlistUseCase.deactivate(WatchlistId.of(watchlistId));
                    yield "Watchlist desactivada: id=" + watchlistId;
                }
                case TelegramCommand.AddAlert(String watchlistId, String ticker, String thresholdPrice) -> {
                    Watchlist wl = watchlistUseCase.addAlertEntry(
                            WatchlistId.of(watchlistId),
                            Ticker.of(ticker),
                            Money.of(thresholdPrice)
                    );
                    yield "Alerta añadida. Total alertas: " + wl.getAlerts().size();
                }
                case TelegramCommand.RemoveAlert(String watchlistId, String ticker, String thresholdPrice) -> {
                    Watchlist wl = watchlistUseCase.removeAlertEntry(
                            WatchlistId.of(watchlistId),
                            Ticker.of(ticker),
                            Money.of(thresholdPrice)
                    );
                    yield "Alerta eliminada. Total alertas: " + wl.getAlerts().size();
                }
                case TelegramCommand.RemoveAllAlerts(String watchlistId, String ticker) -> {
                    Watchlist wl = watchlistUseCase.removeAllAlertsForTicker(
                            WatchlistId.of(watchlistId),
                            Ticker.of(ticker)
                    );
                    yield "Alertas eliminadas para " + ticker + ". Total alertas: " + wl.getAlerts().size();
                }
            };
        } catch (WatchlistNotFoundException ex) {
            return "Error: watchlist no encontrada (" + ex.getMessage() + ")";
        } catch (DuplicateAlertException ex) {
            return "Error: alerta duplicada (" + ex.getMessage() + ")";
        } catch (IllegalArgumentException ex) {
            return "Error: parámetros inválidos (" + ex.getMessage() + ")";
        }
    }
}

