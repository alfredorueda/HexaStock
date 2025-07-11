package cat.gencat.agaur.hexastock.application.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.TransactionDTO;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioStockOperationsUseCase;
import cat.gencat.agaur.hexastock.application.port.in.TransactionUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService implements TransactionUseCase {

    private final TransactionPort<Transaction> transactionPort;

    public TransactionService(TransactionPort<Transaction> transactionPort) {
        this.transactionPort = transactionPort;
    }

    @Override
    public List<TransactionDTO> getTransactions(String portfolioId, Optional<String> type) {

        List<Transaction> lTransactions = transactionPort.getTransactionsByPortfolioId(portfolioId);

        return lTransactions.stream().map(TransactionDTO::new).toList();
    }
}
