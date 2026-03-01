package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.Watchlist;

import java.util.List;

public record WatchlistResponseDTO(
        String id,
        String ownerName,
        String listName,
        boolean active,
        List<WatchlistAlertDTO> alerts
) {
    public static WatchlistResponseDTO from(Watchlist watchlist) {
        return new WatchlistResponseDTO(
                watchlist.getId().value(),
                watchlist.getOwnerName(),
                watchlist.getListName(),
                watchlist.isActive(),
                watchlist.getAlerts().stream().map(WatchlistAlertDTO::from).toList()
        );
    }
}
