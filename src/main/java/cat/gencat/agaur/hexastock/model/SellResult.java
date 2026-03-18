package cat.gencat.agaur.hexastock.model;

/**
 * SellResult represents the financial outcome of selling shares of a stock.
 *
 * <p>In DDD terms, this is a <strong>Value Object</strong> that encapsulates the results of a sell operation.</p>
 *
 * <p>It contains three key financial metrics:</p>
 * <ul>
 *   <li>proceeds: The total money received from the sale (quantity × sell price)</li>
 *   <li>costBasis: The original purchase cost of the sold shares</li>
 *   <li>profit: The difference between proceeds and costBasis (can be positive or negative)</li>
 * </ul>
 *
 * <p>Think of SellResult as a receipt that shows not just how much you received from
 * selling shares, but also whether you made or lost money compared to what you paid.</p>
 */
public record SellResult(Money proceeds, Money costBasis, Money profit, Money fee) {

    /**
     * Creates a SellResult by calculating profit from proceeds and cost basis (no fee).
     * Backward-compatible factory for existing call sites.
     *
     * @param proceeds The total money received from the sale
     * @param costBasis The original purchase cost of the sold shares
     * @return A new SellResult with calculated profit and zero fee
     */
    public static SellResult of(Money proceeds, Money costBasis) {
        Money profit = proceeds.subtract(costBasis);
        return new SellResult(proceeds, costBasis, profit, Money.ZERO);
    }

    /**
     * Creates a SellResult with fee, calculating profit as netProceeds - costBasis.
     *
     * @param proceeds The gross money received from the sale (quantity × price)
     * @param costBasis The original purchase cost of the sold shares
     * @param fee The fee charged for the sell operation
     * @return A new SellResult with fee-adjusted profit
     */
    public static SellResult withFee(Money proceeds, Money costBasis, Money fee) {
        Money netProceeds = proceeds.subtract(fee);
        Money profit = netProceeds.subtract(costBasis);
        return new SellResult(proceeds, costBasis, profit, fee);
    }

    /**
     * Returns the net proceeds after fee deduction.
     *
     * @return proceeds minus fee
     */
    public Money netProceeds() {
        return proceeds.subtract(fee);
    }

    /**
     * Checks if the sale resulted in a profit.
     *
     * @return true if profit is positive
     */
    public boolean isProfitable() {
        return profit.isPositive();
    }

    /**
     * Checks if the sale resulted in a loss.
     *
     * @return true if profit is negative
     */
    public boolean isLoss() {
        return profit.isNegative();
    }
}
