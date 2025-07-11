package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Transaction;

import java.util.List;

public interface TransactionPort <T> {

    List<T> getTransactionsByPortfolioId(String portFolioId);
    void save(Transaction transaction);
}