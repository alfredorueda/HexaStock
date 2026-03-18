package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InsufficientEligibleSharesException;
import cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.HoldingNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PortfolioStockOperationsService implements the core use cases for stock trading operations.
 * 
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link PortfolioStockOperationsUseCase}) to be used by driving adapters</li>
 *   <li>Uses secondary ports ({@link PortfolioPort}, {@link TransactionPort}, and {@link StockPriceProviderPort}) 
 *       to communicate with driven adapters</li>
 * </ul>
 * </p>
 * 
 * <p>This service orchestrates stock trading operations by:
 * <ul>
 *   <li>Retrieving current stock prices from external providers</li>
 *   <li>Executing buy and sell operations on portfolios</li>
 *   <li>Recording transactions for auditing and tracking purposes</li>
 *   <li>Calculating profits/losses for sell operations using FIFO accounting</li>
 * </ul>
 * </p>
 * 
 * <p>The service ensures that all operations are transactional, maintaining data consistency
 * between the portfolio state, stock prices, and transaction records.</p>
 */

@Transactional
public class PortfolioStockOperationsService implements PortfolioStockOperationsUseCase {

    /**
     * The secondary port used to persist and retrieve portfolios.
     */
    private final PortfolioPort portfolioPort;
    
    /**
     * The secondary port used to record financial transactions.
     */
    private final TransactionPort transactionPort;
    
    /**
     * The secondary port used to retrieve current stock prices.
     */
    private final StockPriceProviderPort stockPriceProviderPort;

    /**
     * Constructs a new PortfolioStockOperationsService with the required secondary ports.
     * 
     * @param portfolioPort The port for portfolio persistence operations
     * @param stockPriceProviderPort The port for retrieving stock prices
     * @param transactionPort The port for transaction recording operations
     */
    public PortfolioStockOperationsService(PortfolioPort portfolioPort, StockPriceProviderPort stockPriceProviderPort, TransactionPort transactionPort) {
        this.portfolioPort = portfolioPort;
        this.stockPriceProviderPort = stockPriceProviderPort;
        this.transactionPort = transactionPort;
    }

    /**
     * Buys shares of a stock for a portfolio.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Gets the current price of the stock</li>
     *   <li>Executes the buy operation on the portfolio domain object</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a purchase transaction record</li>
     * </ol>
     * </p>
     * 
     * @param portfolioId The ID of the portfolio to buy stock for
     * @param ticker The ticker symbol of the stock to buy
     * @param quantity The number of shares to buy
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds for the purchase
     */
    @Override
    public void buyStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        portfolio.buy(ticker, quantity, price);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createPurchase(portfolioId, ticker, quantity, price);
        transactionPort.save(transaction);
    }

    /**
     * Sells shares of a stock from a portfolio.
     * 
     * <p>This method:
     * <ol>
     *   <li>Retrieves the specified portfolio</li>
     *   <li>Gets the current price of the stock</li>
     *   <li>Executes the sell operation on the portfolio domain object</li>
     *   <li>Saves the updated portfolio</li>
     *   <li>Creates and saves a sale transaction record with profit/loss information</li>
     *   <li>Returns the SellResult with financial details of the sale</li>
     * </ol>
     * </p>
     * 
     * <p>The sale follows FIFO (First-In-First-Out) accounting principles,
     * selling the oldest shares first to calculate cost basis and profit.</p>
     * 
     * @param portfolioId The ID of the portfolio to sell stock from
     * @param ticker The ticker symbol of the stock to sell
     * @param quantity The number of shares to sell
     * @return A SellResult containing proceeds, cost basis, and profit information
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.DomainException if the ticker is not found in holdings
     * @throws ConflictQuantityException if trying to sell more shares than owned
     */
    @Override
    public SellResult sellStock(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        SellResult sellResult = portfolio.sell(ticker, quantity, price);
        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSale(
                portfolioId, ticker, quantity, price, sellResult.proceeds(), sellResult.profit());
        transactionPort.save(transaction);

        return sellResult;
    }

    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    /**
     * Settlement-aware FIFO sell with fees — ANEMIC style.
     *
     * <p>All business logic resides here in the service, NOT in the domain objects.
     * The domain objects (Portfolio, Holding, Lot) are passive data holders.</p>
     */
    @Override
    public SellResult sellStockWithSettlement(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }

        // Find the holding — no domain method, service checks manually
        Holding holding;
        try {
            holding = portfolio.getHolding(ticker);
        } catch (HoldingNotFoundException e) {
            throw new HoldingNotFoundException("Holding not found in portfolio: " + ticker);
        }

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        LocalDateTime asOf = LocalDateTime.now();

        // Calculate eligible shares — logic in service, NOT in domain
        int eligibleShares = 0;
        for (Lot lot : holding.getLots()) {
            if (!lot.isEmpty()
                    && lot.getSettlementDate() != null
                    && !asOf.isBefore(lot.getSettlementDate())
                    && !lot.isReserved()) {
                eligibleShares += lot.getRemainingShares().value();
            }
        }

        if (eligibleShares < quantity.value()) {
            throw new InsufficientEligibleSharesException(
                    "Not enough eligible (settled + unreserved) shares. Available: "
                            + eligibleShares + ", Requested: " + quantity);
        }

        // Calculate fee
        Money grossProceeds = price.multiply(quantity);
        BigDecimal feeAmount = grossProceeds.amount().multiply(FEE_RATE);
        Money fee = Money.of(feeAmount);
        Money netProceeds = grossProceeds.subtract(fee);

        // Check no negative balance
        // Note: balance AFTER adding netProceeds should be >= 0
        // Edge case: if netProceeds is negative (fee > grossProceeds), check balance
        if (netProceeds.isNegative() && portfolio.getBalance().add(netProceeds).isNegative()) {
            throw new InsufficientFundsException("Net proceeds after fee would cause negative balance");
        }

        // FIFO sell — logic in service, NOT in Holding
        ShareQuantity remainingToSell = quantity;
        Money costBasis = Money.ZERO;

        for (Lot lot : holding.getLots()) {
            if (remainingToSell.isZero()) break;

            // Skip unsettled lots
            if (lot.getSettlementDate() == null || asOf.isBefore(lot.getSettlementDate())) continue;
            // Skip reserved lots
            if (lot.isReserved()) continue;
            // Skip empty lots
            if (lot.isEmpty()) continue;

            ShareQuantity sharesSold = lot.getRemainingShares().min(remainingToSell);
            costBasis = costBasis.add(lot.calculateCostBasis(sharesSold));
            lot.reduce(sharesSold);
            remainingToSell = remainingToSell.subtract(sharesSold);
        }

        // Update balance — service does it directly
        portfolio.deposit(netProceeds);

        SellResult result = SellResult.withFee(grossProceeds, costBasis, fee);

        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSaleWithFee(
                portfolioId, ticker, quantity, price,
                result.proceeds(), result.profit(), result.fee());
        transactionPort.save(transaction);

        return result;
    }

    /**
     * Returns the number of shares eligible for settlement-aware selling.
     *
     * <p>This method was added in Sprint 14 to support the eligible-shares
     * query endpoint. It delegates to the domain's {@code Holding.getEligibleShares()}
     * convenience method, which was introduced alongside the settlement feature.</p>
     */
    @Override
    public int getEligibleSharesCount(PortfolioId portfolioId, Ticker ticker) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        Holding holding = portfolio.getHolding(ticker);

        // Delegates to domain method — added later by a different developer.
        // The service's own sellStockWithSettlement() uses inline logic that
        // checks both settlement AND reservation. The domain method may
        // not perform the same checks.
        return holding.getEligibleShares(LocalDateTime.now()).value();
    }

    /**
     * Reserves a specific lot, preventing it from being included in sales.
     */
    @Override
    public void reserveLot(PortfolioId portfolioId, Ticker ticker, LotId lotId) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        portfolio.reserveLot(ticker, lotId);
        portfolioPort.savePortfolio(portfolio);
    }

    /**
     * Settlement-aware sell that delegates to the Portfolio aggregate.
     *
     * <p>Added in Sprint 14 as a "cleaner" alternative that uses domain methods
     * instead of implementing business logic inline. The service still handles
     * infrastructure concerns (price fetch, persistence, transaction recording).</p>
     */
    @Override
    public SellResult sellStockWithSettlementAggregate(PortfolioId portfolioId, Ticker ticker, ShareQuantity quantity) {
        Portfolio portfolio = portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));

        if (!quantity.isPositive()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }

        StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
        Price price = stockPrice.price();

        // Compute fee (same formula as original endpoint)
        Money grossProceeds = price.multiply(quantity);
        BigDecimal feeAmount = grossProceeds.amount().multiply(FEE_RATE);
        Money fee = Money.of(feeAmount);

        // Delegate to domain aggregate — the "proper DDD" approach.
        // This developer believes the domain methods are authoritative,
        // but they were added later and may differ from the service's inline logic.
        SellResult result = portfolio.sellWithSettlement(ticker, quantity, price, fee, LocalDateTime.now());

        portfolioPort.savePortfolio(portfolio);

        Transaction transaction = Transaction.createSaleWithFee(
                portfolioId, ticker, quantity, price,
                result.proceeds(), result.profit(), result.fee());
        transactionPort.save(transaction);

        return result;
    }
}
