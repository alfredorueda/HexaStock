package com.neueda.portfolio.domain.model;

import com.neueda.portfolio.domain.exception.HoldingNotFoundException;
import com.neueda.portfolio.domain.exception.InvalidQuantityException;
import com.neueda.portfolio.domain.vo.Money;
import com.neueda.portfolio.domain.vo.PortfolioId;
import com.neueda.portfolio.domain.vo.Price;
import com.neueda.portfolio.domain.vo.SellResult;
import com.neueda.portfolio.domain.vo.ShareQuantity;
import com.neueda.portfolio.domain.vo.Ticker;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The aggregate root: an owner's cash balance plus their holdings, one per ticker.
 *
 * <p>Every change to a {@link Holding} or a {@link Lot} goes through this class, which is
 * what lets a sale credit the cash balance and mutate the lots as one operation.
 */
public class Portfolio {

    private final PortfolioId id;
    private final String owner;
    private Money balance;
    private final Map<Ticker, Holding> holdings = new LinkedHashMap<>();
    private final LocalDateTime createdAt;

    private Portfolio(PortfolioId id, String owner, Money balance, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "Portfolio id cannot be null");
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
        this.balance = Objects.requireNonNull(balance, "balance cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    /** Creates an empty portfolio with a zero cash balance. */
    public static Portfolio create(String owner) {
        return new Portfolio(PortfolioId.generate(), owner, Money.ZERO, LocalDateTime.now());
    }

    /**
     * Sells {@code quantity} shares of {@code ticker} at {@code price}, applying FIFO,
     * and credits the proceeds to the cash balance (spec AC-08).
     *
     * <p>The quantity is validated before the holding is looked up, so selling zero of a
     * ticker that is not held is an invalid-quantity failure, not a missing-holding one
     * (spec AC-15). Nothing is mutated unless the whole sale succeeds (AC-16, AC-17).
     *
     * <p>A sale that consumes the whole position drops the holding, so the portfolio only
     * ever contains tickers it actually owns shares of (AC-22).
     *
     * @throws InvalidQuantityException  if the quantity is not positive (AC-10, AC-11)
     * @throws HoldingNotFoundException  if the portfolio does not hold the ticker (AC-13)
     */
    public SellResult sell(Ticker ticker, ShareQuantity quantity, Price price) {
        Objects.requireNonNull(quantity, "quantity cannot be null");
        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive: " + quantity.value());
        }

        Holding holding = getHolding(ticker);
        SellResult result = holding.sell(quantity, price);
        balance = balance.add(result.proceeds());

        if (holding.getTotalShares().isZero()) {
            holdings.remove(ticker);
        }
        return result;
    }

    /**
     * Records a purchase as a new lot on the ticker's holding, creating the holding if
     * this is the first purchase of that ticker.
     *
     * <p>Buying is US-06 and out of scope for this kata: this method builds the state a
     * sale operates on and deliberately does <em>not</em> debit the cash balance or check
     * for sufficient funds, so the cash assertions of US-07 measure the sale alone.
     */
    public void buy(Ticker ticker, ShareQuantity quantity, Price price) {
        Objects.requireNonNull(ticker, "ticker cannot be null");
        holdings.computeIfAbsent(ticker, Holding::create).buy(quantity, price);
    }

    /**
     * @throws HoldingNotFoundException if the portfolio does not hold that ticker
     */
    public Holding getHolding(Ticker ticker) {
        Objects.requireNonNull(ticker, "ticker cannot be null");
        Holding holding = holdings.get(ticker);
        if (holding == null) {
            throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker);
        }
        return holding;
    }

    public Money getBalance() {
        return balance;
    }

    public PortfolioId getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
