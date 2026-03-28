package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.SellResult;
import java.math.BigDecimal;

/**
 * SaleResponseDTO is a Data Transfer Object for returning stock sale results in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to transfer data between the domain model and the external world. It wraps the domain
 * SellResult object to expose it to API consumers.</p>
 * 
 * <p>This DTO encapsulates information about a stock sale, including:</p>
 * <ul>
 *   <li>The proceeds from the sale (total money received)</li>
 *   <li>The cost basis (original purchase cost of the sold shares)</li>
 *   <li>The profit or loss from the sale</li>
 *   <li>The portfolio ID associated with the sale</li>
 *   <li>The ticker symbol of the stock</li>
 *   <li>The quantity of shares sold</li>
 * </ul>
 * 
 * <p>This information is crucial for users to understand the financial impact of their stock sales,
 * particularly for calculating taxes and tracking investment performance.</p>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * which is ideal for DTOs that should not be modified after creation.</p>
 */
public record SaleResponseDTO(
    String portfolioId,
    String ticker,
    int quantity,
    BigDecimal proceeds,
    BigDecimal costBasis,
    BigDecimal profit
) {
    public SaleResponseDTO(String portfolioId, String ticker, int quantity, SellResult sellResult) {
        this(portfolioId, ticker, quantity,
             sellResult.proceeds().amount(),
             sellResult.costBasis().amount(),
             sellResult.profit().amount());
    }

    public static SaleResponseDTO from(String portfolioId, String ticker, int quantity, SellResult sellResult) {
        return new SaleResponseDTO(portfolioId, ticker, quantity, sellResult);
    }
}