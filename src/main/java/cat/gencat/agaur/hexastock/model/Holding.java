package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.application.port.in.ConflictQuantityException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.EntityExistsException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Holding represents the ownership of a specific stock within a portfolio.
 * 
 * <p>In DDD terms, this is an <strong>Entity</strong> that belongs to the Portfolio aggregate.
 * It tracks all lots (purchases) of a particular stock and handles the selling
 * process using the FIFO (First-In-First-Out) accounting method.</p>
 * 
 * <p>A Holding encapsulates:</p>
 * <ul>
 *   <li>The ticker symbol identifying the stock</li>
 *   <li>A collection of Lots representing individual purchase transactions</li>
 *   <li>Methods to buy and sell shares</li>
 *   <li>Logic to calculate total shares owned</li>
 * </ul>
 * 
 * <p>Think of a Holding as your collection of shares for a single company, like Apple or Microsoft.
 * Each time you buy shares of this company, a new "Lot" is created to track that specific purchase.
 * When you sell shares, the oldest ones are sold first (FIFO method), which affects cost basis
 * and profit/loss calculations for tax and performance reporting.</p>
 * 
 * <p>The Holding enforces business rules such as:</p>
 * <ul>
 *   <li>Preventing the sale of more shares than are owned</li>
 *   <li>Maintaining the integrity of Lot data</li>
 *   <li>Correctly applying FIFO accounting during sales</li>
 * </ul>
 */
public class Holding {

    /**
     * Unique identifier for the holding.
     */
    private String id;
    
    /**
     * The ticker symbol identifying the stock.
     */
    private Ticker ticker;

    /**
     * List of lots representing the shares purchased for this holding.
     * Each lot contains information about the quantity, unit price, and purchase date.
     * Lots are processed in FIFO order (oldest first) during sell operations.
     */
    private final List<Lot> lots = new ArrayList<>();

    protected Holding() {}
    
    /**
     * Constructs a Holding with the specified attributes.
     * 
     * @param id The unique identifier for the holding
     * @param ticker The ticker symbol identifying the stock
     */
    public Holding(String id, Ticker ticker) {
        this.id = id;
        this.ticker = ticker;
    }

    /**
     * Factory method to create a new Holding instance with a generated ID.
     * 
     * @param ticker The ticker symbol identifying the stock
     * @return A new Holding instance for the specified ticker
     */
    public static Holding create(Ticker ticker) {
        return new Holding(UUID.randomUUID().toString(), ticker);
    }

    /**
     * Buys shares of this stock, creating a new Lot to track the purchase.
     * 
     * <p>When shares are purchased, a new Lot is created to track:</p>
     * <ul>
     *   <li>The quantity of shares purchased</li>
     *   <li>The price paid per share</li>
     *   <li>The date and time of the purchase</li>
     * </ul>
     * 
     * @param quantity The number of shares to buy
     * @param unitPrice The price per share
     */
    public void buy(int quantity, BigDecimal unitPrice) {
        Lot lot = new Lot(UUID.randomUUID().toString(), quantity, quantity, unitPrice, LocalDateTime.now(), true);
        lots.add(lot);
    }

    /**
     * Sells shares of this stock using the FIFO (First-In-First-Out) accounting method.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Verifies there are enough shares to sell</li>
     *   <li>Sells shares from the oldest Lots first (FIFO)</li>
     *   <li>Calculates the cost basis based on the original purchase prices</li>
     *   <li>Calculates the profit or loss from the sale</li>
     * </ol>
     * 
     * @param quantity The number of shares to sell
     * @param sellPrice The current market price per share
     * @return A SellResult containing proceeds, cost basis, and profit information
     * @throws ConflictQuantityException if there are not enough shares to sell
     */
    public SellResult sell(int quantity, BigDecimal sellPrice) {
        if (getTotalShares() < quantity) {
            throw new ConflictQuantityException("Not enough shares to sell. Available: " + getTotalShares() + ", Requested: " + quantity);
        }

        int remainingToSell = quantity;
        BigDecimal costBasis = BigDecimal.ZERO;
        
        for (Lot lot : lots) {
            if (remainingToSell <= 0) break;

            if (lot.getRemaining() == 0) continue;
            
            int sharesSoldFromLot = Math.min(lot.getRemaining(), remainingToSell);
            BigDecimal lotCostBasis = lot.getUnitPrice().multiply(BigDecimal.valueOf(sharesSoldFromLot));
            
            costBasis = costBasis.add(lotCostBasis);
            lot.reduce(sharesSoldFromLot);
            remainingToSell -= sharesSoldFromLot;
        }

        BigDecimal proceeds = sellPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal profit = proceeds.subtract(costBasis);
        
        return new SellResult(proceeds, costBasis, profit);
    }
    
    /**
     * Calculates the total number of shares currently owned in this holding.
     * 
     * @return The total number of shares across all lots
     */
    public int getTotalShares() {
        return lots.stream()
                .mapToInt(Lot::getRemaining)
                .sum();
    }

    /*
    public boolean isEmpty() {
        return lots.isEmpty() || getTotalShares() == 0;
    }
    */
    
    /**
     * Gets the unique identifier of this holding.
     * 
     * @return The holding ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the ticker symbol identifying the stock.
     * 
     * @return The ticker symbol
     */
    public Ticker getTicker() {
        return ticker;
    }
    
    /**
     * Gets an unmodifiable list of all lots in this holding.
     * 
     * @return An unmodifiable list of lots
     */
    public List<Lot> getLots() {
        return List.copyOf(lots);
    }

    // TODO: @Override equals and hashCode methods for proper entity comparison

    /**
     * Adds a pre-existing Lot to this holding.
     * 
     * <p>This method should only be used for reconstituting a Holding from persistence,
     * not for normal application flow where the buy method should be used instead.</p>
     * 
     * @param lot The Lot to add
     * @throws EntityExistsException if a Lot with the same ID already exists
     */
    public void addLot(Lot lot) {
        long count = lots.parallelStream().filter(l -> l.getId().equals(lot.getId())).count();
        if (count > 0)
            throw new EntityExistsException("Lot " + lot.getId() + " already exists");

        lots.add(lot);
    }
}
