package cat.gencat.agaur.hexastock.adapter.in.webmodel;

/**
 * SaleRequestDTO is a Data Transfer Object for stock sale requests in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to receive input data from the external world for the stock sale use case.</p>
 * 
 * <p>This DTO captures the two essential pieces of information needed for a stock sale:</p>
 * <ul>
 *   <li>The ticker symbol of the stock to sell (e.g., "AAPL", "MSFT")</li>
 *   <li>The quantity of shares to sell</li>
 * </ul>
 * 
 * <p>When a sale request is received, the application will:</p>
 * <ol>
 *   <li>Retrieve the current price of the stock</li>
 *   <li>Verify the portfolio has sufficient shares to sell</li>
 *   <li>Apply FIFO accounting to determine which lots to sell from</li>
 *   <li>Calculate proceeds, cost basis, and profit/loss</li>
 *   <li>Execute the sale if all validations pass</li>
 * </ol>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * making it ideal for request DTOs that should not be modified after creation.</p>
 */
public record SaleRequestDTO(String ticker, int quantity) {
}