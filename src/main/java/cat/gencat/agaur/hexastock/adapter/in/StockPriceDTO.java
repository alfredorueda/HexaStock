package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.model.StockPrice;

import java.time.Instant;

/**
 * StockPriceDTO is a Data Transfer Object for stock price information in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to transfer data between the domain model and the external world. It helps maintain
 * a clean separation between domain models and API representations.</p>
 * 
 * <p>This record encapsulates the essential information about a stock's price:</p>
 * <ul>
 *   <li>The ticker symbol</li>
 *   <li>The current price</li>
 *   <li>The timestamp when the price was recorded</li>
 *   <li>The currency in which the price is denominated</li>
 * </ul>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * which is ideal for DTOs that should not be modified after creation.</p>
 */
public record StockPriceDTO(
        String symbol, double price, Instant time, String currency
) {

    /**
     * Factory method to create a StockPriceDTO from a domain StockPrice object.
     * 
     * <p>This method serves as an anti-corruption layer between the domain model and the API,
     * ensuring that changes to the domain model don't directly impact API consumers.</p>
     * 
     * @param stockPrice The domain StockPrice object to convert
     * @return A new StockPriceDTO with data from the domain object
     */
    public static StockPriceDTO fromDomainModel(StockPrice stockPrice) { 
        return new StockPriceDTO(
            stockPrice.ticker().value(),
            stockPrice.price(),
            stockPrice.time(),
            stockPrice.currency()
        );
    }
}
