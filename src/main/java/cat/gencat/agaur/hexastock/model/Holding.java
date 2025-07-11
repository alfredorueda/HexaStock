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
 * In DDD terms, this is an Entity that belongs to the Portfolio aggregate.
 * It tracks all lots (purchases) of a particular stock and handles the selling
 * process using the FIFO (First-In-First-Out) accounting method.
 * 
 * Think of a Holding as your collection of shares for a single company, like Apple or Microsoft.
 * Each time you buy shares of this company, a new "Lot" is created to track that specific purchase.
 * When you sell shares, the oldest ones are sold first (FIFO method).
 * 
 * The Holding enforces business rules such as preventing the sale of more shares than you own.
 */
public class Holding {

    private String id;
    
    private Ticker ticker;

    /**
     * List of lots representing the shares purchased for this holding.
     * Each lot contains information about the quantity, unit price, and purchase date.
     */
    private final List<Lot> lots = new ArrayList<>();

    protected Holding() {}
    
    public Holding(String id, Ticker ticker) {
        this.id = id;
        this.ticker = ticker;
    }

    public static Holding create(Ticker ticker) {
        return new Holding(UUID.randomUUID().toString(), ticker);
    }

    public void buy(int quantity, BigDecimal unitPrice) {
        Lot lot = new Lot(UUID.randomUUID().toString(), quantity, quantity, unitPrice, LocalDateTime.now(), true);
        lots.add(lot);
    }

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
    public String getId() {
        return id;
    }
    
    public Ticker getTicker() {
        return ticker;
    }
    
    public List<Lot> getLots() {
        return List.copyOf(lots);
    }

    // TODO: @Override equals and hashCode methods for proper entity comparison

    // Only use this for mappers (reconstitution of the domain entity from the database)
    public void addLot(Lot lot) {
        long count = lots.parallelStream().filter(l -> l.getId().equals(lot.getId())).count();
        if (count > 0)
            throw new EntityExistsException("Lot " + lot.getId() + " already exists");

        lots.add(lot);
    }
}
