package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper.TransactionDocumentMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.SpringDataMongoTransactionRepository;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.transaction.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB implementation of {@link TransactionPort}.
 *
 * <p>Transactions are immutable and stored one document per entry, so no
 * optimistic-locking version is needed.</p>
 *
 * <p>Active only under the {@code mongodb} Spring profile.</p>
 */
@Component
@Profile("mongodb")
public class MongoTransactionRepository implements TransactionPort {

    private final SpringDataMongoTransactionRepository repository;

    public MongoTransactionRepository(SpringDataMongoTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Transaction> getTransactionsByPortfolioId(PortfolioId portfolioId) {
        return repository.findByPortfolioId(portfolioId.value()).stream()
                .map(TransactionDocumentMapper::toDomain)
                .toList();
    }

    @Override
    public void save(Transaction transaction) {
        repository.save(TransactionDocumentMapper.toDocument(transaction));
    }
}
