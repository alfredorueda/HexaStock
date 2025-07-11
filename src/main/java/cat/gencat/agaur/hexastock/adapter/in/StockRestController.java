package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockRestController {

    private final GetStockPriceUseCase getStockPriceUseCase;

    public StockRestController(GetStockPriceUseCase getStockPriceUseCase) {
        this.getStockPriceUseCase = getStockPriceUseCase;
    }

    @GetMapping("/{symbol}")
    public StockPriceDTO getStockPrice(@PathVariable String symbol) {

        StockPrice sp = getStockPriceUseCase.getPrice(Ticker.of(symbol));

        return StockPriceDTO.fromDomainModel(sp);
    }
}
