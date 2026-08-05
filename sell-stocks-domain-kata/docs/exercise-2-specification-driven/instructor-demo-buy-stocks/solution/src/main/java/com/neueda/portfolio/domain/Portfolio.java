package com.neueda.portfolio.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Owns the cash balance and the holdings, one per ticker actually owned.
 * All state changes to a Holding or a Lot are meant to pass through here.
 */
public class Portfolio {

    private final PortfolioId id;
    private final String owner;
    private Money balance;
    private final Map<Ticker, Holding> holdings = new LinkedHashMap<>();
    private final LocalDateTime createdAt;

    public Portfolio(String owner) {
        this.id = new PortfolioId(UUID.randomUUID().toString());
        this.owner = Objects.requireNonNull(owner, "Owner cannot be null");
        this.balance = new Money(BigDecimal.ZERO);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Sells shares FIFO and credits the proceeds to the cash balance.
     *
     * <p>The quantity is validated <em>before</em> the holding is looked up, so selling zero
     * shares of a ticker that is not held is an invalid-quantity failure, not a missing-holding
     * one. A sale that consumes the last lot removes the holding from the portfolio.</p>
     */
    public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
        Objects.requireNonNull(ticker, "Ticker cannot be null");
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + quantity.value());
        }

        Holding holding = getHolding(ticker);
        SellResult result = holding.sell(quantity, price);

        if (holding.getTotalShares().isZero()) {
            holdings.remove(ticker);
        }
        this.balance = balance.add(result.proceeds());
        return result;
    }

    /**
     * Buys shares with cash already in the portfolio. The cost is checked against the balance
     * before anything is mutated; insufficient funds leave the portfolio completely untouched.
     * A successful purchase always appends a brand-new lot.
     */
    public void buy(Ticker ticker, ShareQuantity quantity, Price price) {
        Objects.requireNonNull(ticker, "Ticker cannot be null");
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + quantity.value());
        }

        Money cost = price.multiply(quantity);
        if (balance.amount().compareTo(cost.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + balance.amount().toPlainString()
                            + ", Required: " + cost.amount().toPlainString());
        }

        this.balance = balance.subtract(cost);
        holdings.computeIfAbsent(ticker, Holding::new).buy(quantity, price);
    }

    /**
     * Adds cash to the portfolio. Only ever increases the balance, and only for a positive amount.
     */
    public void deposit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.amount().signum() <= 0) {
            throw new InvalidAmountException(
                    "Amount must be positive: " + amount.amount().toPlainString());
        }
        this.balance = balance.add(amount);
    }

    public Holding getHolding(Ticker ticker) {
        Holding holding = holdings.get(ticker);
        if (holding == null) {
            throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker.symbol());
        }
        return holding;
    }

    public Money getBalance() {
        return balance;
    }
}
