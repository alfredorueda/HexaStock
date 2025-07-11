package cat.gencat.agaur.hexastock.adapter.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.*;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioManagmentUseCase;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.in.TransactionUseCase;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.SellResult;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioRestController {
    
    private final PortfolioManagmentUseCase portfolioManagmentUseCase;
    private final PortfolioStockOperationsUseCase portfolioStockOperationsUseCase;
    private final TransactionUseCase transactionUseCase;

    public PortfolioRestController(PortfolioManagmentUseCase portfolioManagmentUseCase, PortfolioStockOperationsUseCase portfolioStockOperationsUseCase, TransactionUseCase transactionUseCase) {
        this.portfolioManagmentUseCase = portfolioManagmentUseCase;
        this.portfolioStockOperationsUseCase = portfolioStockOperationsUseCase;
        this.transactionUseCase = transactionUseCase;
    }
    
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody CreatePortfolioDTO request) {
        Portfolio portfolio = portfolioManagmentUseCase.createPortfolio(request.ownerName());
        return new ResponseEntity<>(portfolio, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) {
        Portfolio portfolio = portfolioManagmentUseCase.getPortfolio(id);
        return ResponseEntity.ok(portfolio);
    }

    @PostMapping("/{id}/deposits")
    public ResponseEntity<Void> deposit(@PathVariable String id, @RequestBody DepositRequestDTO request) {

        portfolioManagmentUseCase.deposit(id, Money.of(Currency.getInstance("USD"), request.amount()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/withdrawals")
    public ResponseEntity<Void> withdraw(@PathVariable String id, @RequestBody WithdrawalRequestDTO request) {
        portfolioManagmentUseCase.withdraw(id, Money.of(Currency.getInstance("USD"), request.amount()));
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/purchase")
    public ResponseEntity<Void> buyStock(@PathVariable String id, @RequestBody PurchaseDTO request) {
        portfolioStockOperationsUseCase.buyStock(id, Ticker.of(request.ticker()), request.quantity());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/sales")
    public ResponseEntity<SaleResponseDTO> sellStock(@PathVariable String id, @RequestBody SaleRequestDTO request) {
        SellResult result = portfolioStockOperationsUseCase.sellStock(id, Ticker.of(request.ticker()), request.quantity());
        return ResponseEntity.ok(new SaleResponseDTO(result));
    }

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
}
