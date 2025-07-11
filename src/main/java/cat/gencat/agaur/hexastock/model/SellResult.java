package cat.gencat.agaur.hexastock.model;

import java.math.BigDecimal;

/**
 * SellResult represents the financial outcome of selling shares of a stock.
 * 
 * In DDD terms, this is a Value Object that encapsulates the results of a sell operation.
 * 
 * It contains three key financial metrics:
 * - proceeds: The total money received from the sale (quantity × sell price)
 * - costBasis: The original purchase cost of the sold shares
 * - profit: The difference between proceeds and costBasis (can be positive or negative)
 * 
 * Think of SellResult as a receipt that shows not just how much you received from
 * selling shares, but also whether you made or lost money compared to what you paid.
 */
public record SellResult(BigDecimal proceeds, BigDecimal costBasis, BigDecimal profit) {
}
