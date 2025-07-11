package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.application.port.in.InvalidAmountException;
import cat.gencat.agaur.hexastock.application.port.in.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.DomainException;
import cat.gencat.agaur.hexastock.model.exception.EntityExistsException;
import cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Portfolio represents an investment portfolio owned by an individual.
 * 
 * This is an Aggregate Root in DDD terms, encapsulating a collection of stocks
 * (Holdings) and providing operations to manage investments.
 * 
 * Think of a portfolio as a personal investment account where you can:
 * - Deposit and withdraw money
 * - Buy and sell stocks
 * - Track your holdings (stocks you own)
 * - Manage your cash balance
 * 
 * The Portfolio enforces business rules like preventing purchases with insufficient funds
 * and ensuring all financial operations use valid amounts.
 */
public class Portfolio {

    private String id;
    
    private String ownerName;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    private final Map<Ticker, Holding> holdings = new HashMap<>();

    protected Portfolio() {}
    
    public Portfolio(String id, String ownerName, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public static Portfolio create(String ownerName) {
        return new Portfolio(UUID.randomUUID().toString(), ownerName, BigDecimal.ZERO, LocalDateTime.now());
    }

    public void deposit(Money money) {
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientFundsException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(money.amount());
    }
    
    public void withdraw(Money money) {
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(money.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
        this.balance = this.balance.subtract(money.amount());
    }

    public void buy(Ticker ticker, int quantity, BigDecimal price) {

       if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be positive");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Price must be positive");
        }
        
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(quantity));
        if (balance.compareTo(totalCost) < 0) {
            throw new InsufficientFundsException("Insufficient funds to buy " + quantity + " shares of " + ticker);
        }
        
        Holding holding = findOrCreateHolding(ticker);
        holding.buy(quantity, price);
        balance = balance.subtract(totalCost);
    }

    public SellResult sell(Ticker ticker, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be positive");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Price must be positive");
        }

        if (!holdings.containsKey(ticker)) {
            throw new DomainException("Ticker not found in Holdings: " + ticker);
        }

        Holding holding = holdings.get(ticker);

        SellResult result = holding.sell(quantity, price);
        balance = balance.add(result.proceeds());
        
        return result;
    }

    private Holding findOrCreateHolding(Ticker ticker) {
        return holdings.computeIfAbsent(ticker, Holding::create);
    }

    // Getters
    public String getId() {
        return id;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public List<Holding> getHoldings() {
        return new ArrayList<>(holdings.values());
    }

    // A ver programador lechón. Solo usa esto para el uso de mappers (reconsitución de la entidad del dominio des de bbdd)
    public void addHolding(Holding holding) {
        if(holdings.containsKey(holding.getTicker()))
            throw new EntityExistsException("Holding " + holding.getTicker() + " already exists");
        holdings.put(holding.getTicker(), holding);
    }

}