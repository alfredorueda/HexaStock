package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.model.exception.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Portfolio represents an investment portfolio owned by an individual.
 * 
 * <p>In DDD terms, this is an <strong>Aggregate Root</strong> that encapsulates a collection of Holdings
 * and provides operations to manage investments. As an Aggregate Root, Portfolio
 * ensures that all changes to its contained Holdings happen through its methods, maintaining
 * consistency across the entire aggregate.</p>
 * 
 * <p>The Portfolio domain object represents a personal investment account where users can:</p>
 * <ul>
 *   <li>Deposit and withdraw money (cash management)</li>
 *   <li>Buy and sell stocks (investment management)</li>
 *   <li>Track holdings (stocks owned) and their performance</li>
 *   <li>View transaction history</li>
 * </ul>
 * 
 * <p>Portfolio enforces critical business rules including:</p>
 * <ul>
 *   <li>Preventing purchases with insufficient funds</li>
 *   <li>Ensuring all financial operations use valid positive amounts</li>
 *   <li>Maintaining consistency between holdings and available cash</li>
 *   <li>Proper tracking of profits/losses during sell operations</li>
 * </ul>
 * 
 * <p>This entity forms the central component of the investment management domain,
 * connecting users to their financial assets and activities.</p>
 */
public class Portfolio {

    /**
     * Unique identifier for the portfolio.
     */
    private PortfolioId id;
    
    /**
     * Name of the portfolio owner.
     */
    private String ownerName;
    
    /**
     * Current cash balance available for investment or withdrawal.
     */
    private Money balance;
    
    /**
     * Timestamp when the portfolio was created.
     */
    private LocalDateTime createdAt;

    /**
     * Map of all stock holdings in this portfolio, indexed by ticker symbol.
     * Each Holding represents ownership of a particular stock.
     */
    private final Map<Ticker, Holding> holdings = new HashMap<>();

    protected Portfolio() {}
    
    /**
     * Constructs a Portfolio with the specified attributes.
     *
     * @param id The unique identifier for the portfolio
     * @param ownerName The name of the portfolio owner
     * @param balance The initial cash balance
     * @param createdAt The creation timestamp
     */
    public Portfolio(PortfolioId id, String ownerName, Money balance, LocalDateTime createdAt) {
        Objects.requireNonNull(id, "Portfolio id must not be null");
        Objects.requireNonNull(ownerName, "Owner name must not be null");
        Objects.requireNonNull(balance, "Balance must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");

        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    /**
     * Factory method to create a new Portfolio instance with a generated ID.
     * 
     * @param ownerName The name of the portfolio owner
     * @return A new Portfolio instance with zero balance and current timestamp
     */
    public static Portfolio create(String ownerName) {
        return new Portfolio(PortfolioId.generate(), ownerName, Money.ZERO, LocalDateTime.now());
    }

    /**
     * Deposits money into the portfolio's cash balance.
     * 
     * @param money The amount of money to deposit
     * @throws InsufficientFundsException if the deposit amount is not positive
     */
    public void deposit(Money money) {
        if (!money.isPositive()) {
            throw new InvalidAmountException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(money);
    }
    
    /**
     * Withdraws money from the portfolio's cash balance.
     * 
     * @param money The amount of money to withdraw
     * @throws InvalidAmountException if the withdrawal amount is not positive
     * @throws InsufficientFundsException if there are insufficient funds for the withdrawal
     */
    public void withdraw(Money money) {
        if (!money.isPositive()) {
            throw new InvalidAmountException("Withdrawal amount must be positive");
        }
        if (balance.isLessThan(money)) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
        this.balance = this.balance.subtract(money);
    }

    /**
     * Buys shares of a stock for the portfolio.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Validates the quantity and price are positive</li>
     *   <li>Checks if there are sufficient funds for the purchase</li>
     *   <li>Finds or creates a Holding for the ticker</li>
     *   <li>Adds the purchased shares to the Holding</li>
     *   <li>Reduces the cash balance by the purchase amount</li>
     * </ol>
     * 
     * @param ticker The ticker symbol of the stock to buy
     * @param quantity The number of shares to buy
     * @param price The price per share
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws InvalidAmountException if the price is not positive
     * @throws InsufficientFundsException if there are insufficient funds for the purchase
     */
    public void buy(Ticker ticker, ShareQuantity quantity, Price price) {
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }

        Money totalCost = price.multiply(quantity);
        if (balance.isLessThan(totalCost)) {
            throw new InsufficientFundsException("Insufficient funds to buy " + quantity + " shares of " + ticker);
        }

        Holding holding = findOrCreateHolding(ticker);
        holding.buy(quantity, price);
        balance = balance.subtract(totalCost);
    }

    /**
     * Sells shares of a stock from the portfolio.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Validates the quantity and price are positive</li>
     *   <li>Verifies the ticker exists in the portfolio's holdings</li>
     *   <li>Executes the sale on the appropriate Holding</li>
     *   <li>Increases the cash balance by the sale proceeds</li>
     *   <li>Returns details about the sale, including profit/loss</li>
     * </ol>
     * 
     * <p>The sale follows FIFO (First-In-First-Out) accounting principles,
     * selling the oldest shares first to calculate cost basis and profit.</p>
     * 
     * @param ticker The ticker symbol of the stock to sell
     * @param quantity The number of shares to sell
     * @param price The price per share
     * @return A SellResult containing proceeds, cost basis, and profit information
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws InvalidAmountException if the price is not positive
     * @throws DomainException if the ticker is not found in holdings
     */
    public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }
        if (!holdings.containsKey(ticker)) {
            throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker);
        }

        Holding holding = holdings.get(ticker);
        SellResult result = holding.sell(quantity, price);
        balance = balance.add(result.proceeds());

        return result;
    }

    /**
     * Finds an existing Holding for the specified ticker or creates a new one if none exists.
     * 
     * @param ticker The ticker symbol to find or create a Holding for
     * @return The existing or newly created Holding
     */
    private Holding findOrCreateHolding(Ticker ticker) {
        return holdings.computeIfAbsent(ticker, Holding::create);
    }

    /**
     * Gets the unique identifier of this portfolio.
     * 
     * @return The portfolio ID
     */
    public PortfolioId getId() {
        return id;
    }
    
    /**
     * Gets the name of the portfolio owner.
     * 
     * @return The owner's name
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Gets the current cash balance of the portfolio.
     * 
     * @return The cash balance
     */
    public Money getBalance() {
        return balance;
    }
    
    /**
     * Gets the creation timestamp of the portfolio.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets a list of all stock holdings in this portfolio.
     * 
     * @return A list of all Holdings
     *
     *
     */
    public List<Holding> getHoldings() {
        return List.copyOf(holdings.values());
    }

    /**
     * Adds a pre-existing Holding to this portfolio.
     * 
     * <p>This method should only be used for reconstituting a Portfolio from persistence,
     * not for normal application flow.</p>
     * 
     * @param holding The Holding to add
     * @throws EntityExistsException if a Holding for the same ticker already exists
     */
    public void addHolding(Holding holding) {
        if(holdings.containsKey(holding.getTicker()))
            throw new EntityExistsException("Holding " + holding.getTicker() + " already exists");
        holdings.put(holding.getTicker(), holding);
    }

    public Holding getHolding(Ticker key) {
        if(!holdings.containsKey(key))
            throw new HoldingNotFoundException("Holding " + key + " not exists");
        return holdings.get(key);
    }
}