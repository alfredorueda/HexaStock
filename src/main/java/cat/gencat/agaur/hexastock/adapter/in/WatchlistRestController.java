package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.CreateWatchlistDTO;
import cat.gencat.agaur.hexastock.adapter.in.webmodel.WatchlistAlertRequestDTO;
import cat.gencat.agaur.hexastock.adapter.in.webmodel.WatchlistResponseDTO;
import cat.gencat.agaur.hexastock.application.port.in.WatchlistManagementUseCase;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistRestController {

    private final WatchlistManagementUseCase watchlistManagementUseCase;

    public WatchlistRestController(WatchlistManagementUseCase watchlistManagementUseCase) {
        this.watchlistManagementUseCase = watchlistManagementUseCase;
    }

    @PostMapping
    public ResponseEntity<WatchlistResponseDTO> createWatchlist(@RequestBody CreateWatchlistDTO request) {
        Watchlist watchlist = watchlistManagementUseCase.createWatchlist(request.ownerName(), request.listName());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(watchlist.getId().value())
                .toUri();
        return ResponseEntity.created(location).body(WatchlistResponseDTO.from(watchlist));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchlistResponseDTO> getWatchlist(@PathVariable String id) {
        Watchlist watchlist = watchlistManagementUseCase.getWatchlist(WatchlistId.of(id));
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchlist(@PathVariable String id) {
        watchlistManagementUseCase.deleteWatchlist(WatchlistId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/alerts")
    public ResponseEntity<WatchlistResponseDTO> addAlert(@PathVariable String id,
                                                         @RequestBody WatchlistAlertRequestDTO request) {
        Watchlist watchlist = watchlistManagementUseCase.addAlertEntry(
                WatchlistId.of(id),
                Ticker.of(request.ticker()),
                Money.of(request.thresholdPrice())
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }

    @DeleteMapping("/{id}/alerts")
    public ResponseEntity<WatchlistResponseDTO> removeAlert(@PathVariable String id,
                                                            @RequestParam String ticker,
                                                            @RequestParam BigDecimal thresholdPrice) {
        Watchlist watchlist = watchlistManagementUseCase.removeAlertEntry(
                WatchlistId.of(id),
                Ticker.of(ticker),
                Money.of(thresholdPrice)
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }

    @DeleteMapping("/{id}/alerts/{ticker}")
    public ResponseEntity<WatchlistResponseDTO> removeAllAlertsForTicker(@PathVariable String id,
                                                                          @PathVariable String ticker) {
        Watchlist watchlist = watchlistManagementUseCase.removeAllAlertsForTicker(
                WatchlistId.of(id),
                Ticker.of(ticker)
        );
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<WatchlistResponseDTO> activateWatchlist(@PathVariable String id) {
        Watchlist watchlist = watchlistManagementUseCase.activateWatchlist(WatchlistId.of(id));
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<WatchlistResponseDTO> deactivateWatchlist(@PathVariable String id) {
        Watchlist watchlist = watchlistManagementUseCase.deactivateWatchlist(WatchlistId.of(id));
        return ResponseEntity.ok(WatchlistResponseDTO.from(watchlist));
    }
}
