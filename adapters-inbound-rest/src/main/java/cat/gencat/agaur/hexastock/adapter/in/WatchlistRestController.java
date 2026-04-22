package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.application.port.in.WatchlistUseCase;
import cat.gencat.agaur.hexastock.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.model.watchlist.WatchlistId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static cat.gencat.agaur.hexastock.adapter.in.webmodel.WatchlistDTOs.*;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistRestController {

    private final WatchlistUseCase watchlistUseCase;

    public WatchlistRestController(WatchlistUseCase watchlistUseCase) {
        this.watchlistUseCase = watchlistUseCase;
    }

    @PostMapping
    public ResponseEntity<WatchlistResponseDTO> create(@RequestBody CreateWatchlistRequestDTO request) {
        Watchlist created = watchlistUseCase.createWatchlist(
                request.ownerName(),
                request.listName(),
                request.telegramChatId()
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId().value())
                .toUri();
        return ResponseEntity.created(location).body(WatchlistResponseDTO.from(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        watchlistUseCase.deleteWatchlist(WatchlistId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/alerts")
    public ResponseEntity<WatchlistResponseDTO> addAlert(@PathVariable String id, @RequestBody AlertEntryRequestDTO request) {
        Watchlist updated = watchlistUseCase.addAlertEntry(
                WatchlistId.of(id),
                toTicker(request.ticker()),
                toMoney(request.thresholdPrice())
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(updated));
    }

    @DeleteMapping("/{id}/alerts")
    public ResponseEntity<WatchlistResponseDTO> removeAlert(@PathVariable String id,
                                                            @RequestParam String ticker,
                                                            @RequestParam String thresholdPrice) {
        Watchlist updated = watchlistUseCase.removeAlertEntry(
                WatchlistId.of(id),
                toTicker(ticker),
                toMoney(thresholdPrice)
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(updated));
    }

    @DeleteMapping("/{id}/alerts/{ticker}")
    public ResponseEntity<WatchlistResponseDTO> removeAllAlertsForTicker(@PathVariable String id, @PathVariable String ticker) {
        Watchlist updated = watchlistUseCase.removeAllAlertsForTicker(
                WatchlistId.of(id),
                toTicker(ticker)
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(updated));
    }

    @PostMapping("/{id}/activation")
    public ResponseEntity<WatchlistResponseDTO> activate(@PathVariable String id) {
        Watchlist updated = watchlistUseCase.activate(WatchlistId.of(id));
        return ResponseEntity.ok(WatchlistResponseDTO.from(updated));
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<WatchlistResponseDTO> deactivate(@PathVariable String id) {
        Watchlist updated = watchlistUseCase.deactivate(WatchlistId.of(id));
        return ResponseEntity.ok(WatchlistResponseDTO.from(updated));
    }
}

