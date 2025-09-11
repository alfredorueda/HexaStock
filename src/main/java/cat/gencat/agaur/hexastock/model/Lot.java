package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lot represents a specific purchase of shares for a particular stock.
 * 
 * <p>In DDD terms, this is an <strong>Entity</strong> that belongs to the Holding aggregate.
 * It tracks the details of a single stock purchase transaction, including:</p>
 * <ul>
 *   <li>How many shares were initially purchased (initialStocks)</li>
 *   <li>How many shares remain unsold (remaining)</li>
 *   <li>The price paid per share (unitPrice)</li>
 *   <li>When the purchase was made (purchasedAt)</li>
 * </ul>
 * 
 * <p>Lot objects are essential for implementing the FIFO (First-In-First-Out) accounting
 * method used when selling shares. Each Lot represents a distinct purchase, and
 * when shares are sold, they are drawn from the oldest Lots first.</p>
 * 
 * <p>Think of a Lot as a receipt for a specific stock purchase. If you buy Apple
 * shares three times at different prices and dates, you'll have three separate Lots.
 * When selling shares, the system uses these Lots to calculate your profit/loss
 * based on the actual purchase prices of the specific shares being sold.</p>
 * 
 * <p>This entity enforces business rules such as:</p>
 * <ul>
 *   <li>Ensuring purchase quantities and prices are positive values</li>
 *   <li>Preventing reduction of shares beyond the remaining amount</li>
 *   <li>Tracking the lifecycle of purchased shares from acquisition to sale</li>
 * </ul>
 */
public class Lot {

    /**
     * Unique identifier for the lot.
     */
    private String id;
    
    /**
     * The number of shares initially purchased in this lot.
     * This value never changes after creation.
     */
    private int initialStocks;
    
    /**
     * The number of shares from this lot that remain unsold.
     * This value starts equal to initialStocks and decreases as shares are sold.
     */
    private int remaining;
    
    /**
     * The price paid per share when this lot was purchased.
     * Used to calculate cost basis and profit/loss during sales.
     */
    private BigDecimal unitPrice;
    
    /**
     * The date and time when this lot was purchased.
     * Used for FIFO ordering during sell operations.
     */
    private LocalDateTime purchasedAt;

    protected Lot() {}

    /**
     * Constructs a Lot with the specified attributes.
     * 
     * @param id The unique identifier for the lot
     * @param initialStocks The number of shares initially purchased
     * @param quantity The number of shares currently remaining (usually equals initialStocks for new lots)
     * @param unitPrice The price paid per share
     * @param purchasedAt The date and time of purchase
     * @param validation Whether to perform validation checks on the inputs
     * @throws InvalidQuantityException if validation is true and quantity is not positive
     * @throws InvalidAmountException if validation is true and unitPrice is not positive
     */
    public Lot(String id, int initialStocks, int quantity, BigDecimal unitPrice, LocalDateTime purchasedAt, boolean validation) {

        if(validation) {
            if (quantity <= 0) {
                throw new InvalidQuantityException("Quantity must be positive");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Unit price must be positive");
            }
        }

        
        this.id = id;
        this.initialStocks = initialStocks;
        this.remaining = quantity;
        this.unitPrice = unitPrice;
        this.purchasedAt = purchasedAt;
    }

    /**
     * Reduces the number of remaining shares in this lot, typically when shares are sold.
     * 
     * <p>This method is called during the FIFO selling process when shares from this lot
     * are being sold. It reduces the 'remaining' count by the specified quantity.</p>
     * 
     * @param qty The number of shares to reduce from this lot
     * @throws ConflictQuantityException if attempting to reduce by more shares than remain in the lot
     */
    public void reduce(int qty) {
        if (qty > remaining) {
            throw new ConflictQuantityException("Cannot reduce by more than remaining quantity");
        }
        remaining -= qty;
    }


    /**
     * Gets the unique identifier of this lot.
     * 
     * @return The lot ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the number of shares initially purchased in this lot.
     * 
     * @return The initial quantity of shares
     */
    public int getInitialStocks() { return initialStocks; }
    
    /**
     * Gets the number of shares from this lot that remain unsold.
     * 
     * @return The remaining quantity of shares
     */
    public int getRemaining() {
        return remaining;
    }
    
    /**
     * Gets the price paid per share when this lot was purchased.
     * 
     * @return The unit price
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    /**
     * Gets the date and time when this lot was purchased.
     * 
     * @return The purchase timestamp
     */
    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

}
