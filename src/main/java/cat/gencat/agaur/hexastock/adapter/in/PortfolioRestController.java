package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.*;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioManagementUseCase;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.in.ReportingUseCase;
import cat.gencat.agaur.hexastock.application.port.in.TransactionUseCase;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.SellResult;
import cat.gencat.agaur.hexastock.model.Ticker;
import cat.gencat.agaur.hexastock.model.exception.ConflictQuantityException;
import cat.gencat.agaur.hexastock.model.exception.InvalidAmountException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * PortfolioRestController exposes portfolio management operations as REST endpoints.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>primary adapter</strong> (driving adapter)
 * that adapts HTTP requests to calls on the application's primary ports. It provides a REST API
 * for interacting with the core application functionality.</p>
 * 
 * <p>This controller handles the following operations:</p>
 * <ul>
 *   <li>Portfolio creation and retrieval</li>
 *   <li>Cash deposits and withdrawals</li>
 *   <li>Stock purchases and sales</li>
 *   <li>Transaction history retrieval</li>
 * </ul>
 * 
 * <p>It demonstrates the separation of concerns in hexagonal architecture by:
 * <ul>
 *   <li>Depending only on application ports (interfaces), not on their implementations</li>
 *   <li>Converting between API DTOs and domain objects</li>
 *   <li>Handling HTTP-specific concerns (status codes, headers, etc.)</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioRestController {
    
    private final PortfolioManagementUseCase portfolioManagementUseCase;
    private final ReportingUseCase reportingUseCase;
    private final PortfolioStockOperationsUseCase portfolioStockOperationsUseCase;
    private final TransactionUseCase transactionUseCase;

    /**
     * Constructs a new PortfolioRestController with the required application ports.
     * 
     * @param portfolioManagementUseCase Port for portfolio and cash management
     * @param portfolioStockOperationsUseCase Port for stock operations
     * @param transactionUseCase Port for transaction history
     */
    public PortfolioRestController(PortfolioManagementUseCase portfolioManagementUseCase, PortfolioStockOperationsUseCase portfolioStockOperationsUseCase,
                                   TransactionUseCase transactionUseCase, ReportingUseCase reportingUseCase) {
        this.portfolioManagementUseCase = portfolioManagementUseCase;
        this.portfolioStockOperationsUseCase = portfolioStockOperationsUseCase;
        this.transactionUseCase = transactionUseCase;
        this.reportingUseCase = reportingUseCase;
    }
    
    /**
     * Creates a new portfolio.
     * 
     * <p>POST /api/portfolios</p>
     * 
     * @param request DTO containing the owner name
     * @return The newly created portfolio with HTTP 201 Created status
     */
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody CreatePortfolioDTO request) {
        Portfolio portfolio = portfolioManagementUseCase.createPortfolio(request.ownerName());
        return new ResponseEntity<>(portfolio, HttpStatus.CREATED);
    }
    
    /**
     * Retrieves a portfolio by its ID.
     * 
     * <p>GET /api/portfolios/{id}</p>
     * 
     * @param id The unique identifier of the portfolio
     * @return The requested portfolio with HTTP 200 OK status
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) {
        Portfolio portfolio = portfolioManagementUseCase.getPortfolio(id);
        return ResponseEntity.ok(portfolio);
    }

    /**
     * Deposits funds into a portfolio.
     * 
     * <p>POST /api/portfolios/{id}/deposits</p>
     * 
     * @param id The ID of the portfolio to deposit into
     * @param request DTO containing the deposit amount
     * @return HTTP 200 OK with no content
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if the deposit amount is not positive
     */
    @PostMapping("/{id}/deposits")
    public ResponseEntity<Void> deposit(@PathVariable String id, @RequestBody DepositRequestDTO request) {
        portfolioManagementUseCase.deposit(id, Money.of(Currency.getInstance("USD"), request.amount()));
        return ResponseEntity.ok().build();
    }

    /**
     * Withdraws funds from a portfolio.
     * 
     * <p>POST /api/portfolios/{id}/withdrawals</p>
     * 
     * @param id The ID of the portfolio to withdraw from
     * @param request DTO containing the withdrawal amount
     * @return HTTP 200 OK with no content
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidAmountException if the withdrawal amount is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds
     */
    @PostMapping("/{id}/withdrawals")
    public ResponseEntity<Void> withdraw(@PathVariable String id, @RequestBody WithdrawalRequestDTO request) {
        portfolioManagementUseCase.withdraw(id, Money.of(Currency.getInstance("USD"), request.amount()));
        return ResponseEntity.ok().build();
    }
    
    /**
     * Buys shares of a stock for a portfolio.
     * 
     * <p>POST /api/portfolios/{id}/purchase</p>
     * 
     * <p>This endpoint:</p>
     * <ol>
     *   <li>Retrieves the current market price</li>
     *   <li>Executes the purchase if sufficient funds are available</li>
     *   <li>Updates the portfolio's holdings and balance</li>
     *   <li>Records the transaction</li>
     * </ol>
     * 
     * @param id The ID of the portfolio to buy stock for
     * @param request DTO containing the ticker symbol and quantity
     * @return HTTP 200 OK with no content
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException if there are insufficient funds
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<Void> buyStock(@PathVariable String id, @RequestBody PurchaseDTO request) {
        portfolioStockOperationsUseCase.buyStock(id, Ticker.of(request.ticker()), request.quantity());
        return ResponseEntity.ok().build();
    }

    /**
     * Sells shares of a stock from a portfolio.
     * 
     * <p>POST /api/portfolios/{id}/sales</p>
     * 
     * <p>This endpoint:</p>
     * <ol>
     *   <li>Retrieves the current market price</li>
     *   <li>Applies FIFO accounting to determine which lots to sell from</li>
     *   <li>Calculates proceeds, cost basis, and profit/loss</li>
     *   <li>Updates the portfolio's holdings and balance</li>
     *   <li>Records the transaction</li>
     * </ol>
     * 
     * @param id The ID of the portfolio to sell stock from
     * @param request DTO containing the ticker symbol and quantity
     * @return The sale result information with HTTP 200 OK status
     * @throws PortfolioNotFoundException if the portfolio is not found
     * @throws InvalidQuantityException if the quantity is not positive
     * @throws cat.gencat.agaur.hexastock.model.exception.DomainException if the ticker is not found in holdings
     * @throws ConflictQuantityException if trying to sell more shares than owned
     */
    @PostMapping("/{id}/sales")
    public ResponseEntity<SaleResponseDTO> sellStock(@PathVariable String id, @RequestBody SaleRequestDTO request) {
        SellResult result = portfolioStockOperationsUseCase.sellStock(id, Ticker.of(request.ticker()), request.quantity());
        return ResponseEntity.ok(new SaleResponseDTO(result));
    }

    /**
     * Retrieves transaction history for a portfolio.
     * 
     * <p>GET /api/portfolios/{id}/transactions?type=TYPE</p>
     * 
     * @param id The ID of the portfolio to get transactions for
     * @param type Optional transaction type to filter by (e.g., "DEPOSIT", "WITHDRAWAL", "PURCHASE", "SALE")
     * @return A list of transactions with HTTP 200 OK status
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @PathVariable String id,
            @RequestParam(required = false) String type
            ) {

        List<TransactionDTO> transactions = transactionUseCase.getTransactions(
                id,
                Optional.ofNullable(type)
        );

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/holdings")
    public ResponseEntity<List<HoldingDTO>> getHoldings(@PathVariable String id) {

        List<HoldingDTO> lHoldings = reportingUseCase.getHoldingsPerfomance(id);

        return ResponseEntity.ok(lHoldings);
    }
}
