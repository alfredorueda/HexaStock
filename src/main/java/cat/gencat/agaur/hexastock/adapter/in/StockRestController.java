package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.application.port.in.GetStockPriceUseCase;
import cat.gencat.agaur.hexastock.model.StockPrice;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.web.bind.annotation.*;

/**
 * StockRestController exposes stock price retrieval operations as REST endpoints.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary adapter</strong> (driving adapter)
 * that adapts HTTP requests to calls on the application's primary ports. It provides a REST API
 * for retrieving stock price information.</p>
 * 
 * <p>This controller demonstrates the separation of concerns in hexagonal architecture by:</p>
 * <ul>
 *   <li>Depending only on the GetStockPriceUseCase port, not on its implementation</li>
 *   <li>Converting between domain models and DTOs for the API response</li>
 *   <li>Handling HTTP-specific concerns</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/stocks")
public class StockRestController {

    /**
     * Port for retrieving stock price information.
     */
    private final GetStockPriceUseCase getStockPriceUseCase;

    /**
     * Constructs a new StockRestController with the required application port.
     * 
     * @param getStockPriceUseCase The port for retrieving stock prices
     */
    public StockRestController(GetStockPriceUseCase getStockPriceUseCase) {
        this.getStockPriceUseCase = getStockPriceUseCase;
    }

    /**
     * Retrieves the current price for a stock by its ticker symbol.
     * 
     * <p>GET /api/stocks/{symbol}</p>
     * 
     * <p>This endpoint:
     * <ol>
     *   <li>Converts the symbol path variable to a domain Ticker object</li>
     *   <li>Retrieves the current price through the GetStockPriceUseCase port</li>
     *   <li>Converts the domain StockPrice to a DTO for the API response</li>
     * </ol>
     * </p>
     * 
     * @param symbol The ticker symbol of the stock (e.g., "AAPL", "MSFT")
     * @return A DTO containing the current price information
     * @throws IllegalArgumentException if the ticker symbol is invalid
     * @throws RuntimeException if the price cannot be retrieved
     */
    @GetMapping("/{symbol}")
    public StockPriceDTO getStockPrice(@PathVariable String symbol) {
        StockPrice sp = getStockPriceUseCase.getPrice(Ticker.of(symbol));
        return StockPriceDTO.fromDomainModel(sp);
    }
}
