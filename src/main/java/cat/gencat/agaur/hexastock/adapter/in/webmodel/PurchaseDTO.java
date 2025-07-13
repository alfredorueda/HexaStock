package cat.gencat.agaur.hexastock.adapter.in.webmodel;

/**
 * PurchaseDTO is a Data Transfer Object for stock purchase requests in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to receive input data from the external world for the stock purchase use case.</p>
 * 
 * <p>This DTO captures the two essential pieces of information needed for a stock purchase:</p>
 * <ul>
 *   <li>The ticker symbol of the stock to purchase (e.g., "AAPL", "MSFT")</li>
 *   <li>The quantity of shares to purchase</li>
 * </ul>
 * 
 * <p>When a purchase request is received, the application will:</p>
 * <ol>
 *   <li>Retrieve the current price of the stock</li>
 *   <li>Calculate the total cost (price × quantity)</li>
 *   <li>Verify the portfolio has sufficient funds</li>
 *   <li>Execute the purchase if all validations pass</li>
 * </ol>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * making it ideal for request DTOs that should not be modified after creation.</p>
 */
public record PurchaseDTO(String ticker, int quantity) {
}