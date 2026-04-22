package cat.gencat.agaur.hexastock.adapter.in.telegram;

import cat.gencat.agaur.hexastock.application.exception.WatchlistNotFoundException;
import cat.gencat.agaur.hexastock.application.port.in.WatchlistUseCase;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.watchlist.DuplicateAlertException;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;

public class TelegramCommandHandler {

    private final WatchlistUseCase watchlistUseCase;

    public TelegramCommandHandler(WatchlistUseCase watchlistUseCase) {
        this.watchlistUseCase = watchlistUseCase;
    }

    public String handle(TelegramCommand command, String ownerName, String chatId) {
        try {
            return switch (command) {
                case TelegramCommand.CreateWatchlist c -> {
                    Watchlist created = watchlistUseCase.createWatchlist(ownerName, c.listName(), chatId);
                    yield "Watchlist creada: id=" + created.getId().value() + " name=" + created.getListName();
                }
                case TelegramCommand.DeleteWatchlist c -> {
                    watchlistUseCase.deleteWatchlist(WatchlistId.of(c.watchlistId()));
                    yield "Watchlist borrada: id=" + c.watchlistId();
                }
                case TelegramCommand.ActivateWatchlist c -> {
                    watchlistUseCase.activate(WatchlistId.of(c.watchlistId()));
                    yield "Watchlist activada: id=" + c.watchlistId();
                }
                case TelegramCommand.DeactivateWatchlist c -> {
                    watchlistUseCase.deactivate(WatchlistId.of(c.watchlistId()));
                    yield "Watchlist desactivada: id=" + c.watchlistId();
                }
                case TelegramCommand.AddAlert c -> {
                    Watchlist wl = watchlistUseCase.addAlertEntry(
                            WatchlistId.of(c.watchlistId()),
                            Ticker.of(c.ticker()),
                            Money.of(c.thresholdPrice())
                    );
                    yield "Alerta añadida. Total alertas: " + wl.getAlerts().size();
                }
                case TelegramCommand.RemoveAlert c -> {
                    Watchlist wl = watchlistUseCase.removeAlertEntry(
                            WatchlistId.of(c.watchlistId()),
                            Ticker.of(c.ticker()),
                            Money.of(c.thresholdPrice())
                    );
                    yield "Alerta eliminada. Total alertas: " + wl.getAlerts().size();
                }
                case TelegramCommand.RemoveAllAlerts c -> {
                    Watchlist wl = watchlistUseCase.removeAllAlertsForTicker(
                            WatchlistId.of(c.watchlistId()),
                            Ticker.of(c.ticker())
                    );
                    yield "Alertas eliminadas para " + c.ticker() + ". Total alertas: " + wl.getAlerts().size();
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

