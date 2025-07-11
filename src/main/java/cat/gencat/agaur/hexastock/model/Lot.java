package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.application.port.in.ConflictQuantityException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidAmountException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidQuantityException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lot represents a specific purchase of shares for a particular stock.
 * 
 * In DDD terms, this is an Entity that belongs to the Holding aggregate.
 * It tracks the details of a single stock purchase, including:
 * - How many shares were initially purchased
 * - How many shares remain unsold
 * - The price paid per share (unit price)
 * - When the purchase was made
 * 
 * Think of a Lot as a receipt for a specific stock purchase. If you buy Apple
 * shares three times at different prices, you'll have three separate Lots.
 * When selling shares, the system uses these Lots to calculate your profit/loss
 * based on the FIFO (First-In-First-Out) accounting method.
 */

public class Lot {

    private String id;
    private int initialStocks;
    private int remaining;
    private BigDecimal unitPrice;
    private LocalDateTime purchasedAt;

    protected Lot() {}

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

    public void reduce(int qty) {
        if (qty > remaining) {
            throw new ConflictQuantityException("Cannot reduce by more than remaining quantity");
        }
        remaining -= qty;
    }

    /*
    public boolean isEmpty() {
        return remaining <= 0;
    }
    */
    // Getters
    public String getId() {
        return id;
    }

    public int getInitialStocks() { return initialStocks; }
    
    public int getRemaining() {
        return remaining;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

}
