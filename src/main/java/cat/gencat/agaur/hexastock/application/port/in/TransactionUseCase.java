package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.TransactionDTO;

import java.util.List;
import java.util.Optional;

public interface TransactionUseCase {

    List<TransactionDTO> getTransactions(String portfolioId, Optional<String> type);


}
