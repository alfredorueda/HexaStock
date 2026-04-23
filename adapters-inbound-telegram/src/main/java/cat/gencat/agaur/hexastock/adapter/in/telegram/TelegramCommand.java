package cat.gencat.agaur.hexastock.adapter.in.telegram;

public sealed interface TelegramCommand permits TelegramCommand.CreateWatchlist,
        TelegramCommand.DeleteWatchlist,
        TelegramCommand.ActivateWatchlist,
        TelegramCommand.DeactivateWatchlist,
        TelegramCommand.AddAlert,
        TelegramCommand.RemoveAlert,
        TelegramCommand.RemoveAllAlerts {

    record CreateWatchlist(String listName) implements TelegramCommand {}
    record DeleteWatchlist(String watchlistId) implements TelegramCommand {}
    record ActivateWatchlist(String watchlistId) implements TelegramCommand {}
    record DeactivateWatchlist(String watchlistId) implements TelegramCommand {}
    record AddAlert(String watchlistId, String ticker, String thresholdPrice) implements TelegramCommand {}
    record RemoveAlert(String watchlistId, String ticker, String thresholdPrice) implements TelegramCommand {}
    record RemoveAllAlerts(String watchlistId, String ticker) implements TelegramCommand {}
}

