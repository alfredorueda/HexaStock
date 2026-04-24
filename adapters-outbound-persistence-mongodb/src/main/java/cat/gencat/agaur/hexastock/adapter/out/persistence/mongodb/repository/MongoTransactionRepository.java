package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper.TransactionDocumentMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoTransactionSpringDataRepository;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB implementation of {@link TransactionPort}.
 */
@Component
@Profile("mongodb")
public class MongoTransactionRepository implements TransactionPort {

    private final MongoTransactionSpringDataRepository mongoSpringDataRepository;

    public MongoTransactionRepository(MongoTransactionSpringDataRepository mongoSpringDataRepository) {
        this.mongoSpringDataRepository = mongoSpringDataRepository;
    }

    @Override
    public List<Transaction> getTransactionsByPortfolioId(PortfolioId portfolioId) {
        return mongoSpringDataRepository.findAllByPortfolioIdOrderByCreatedAtAsc(portfolioId.value()).stream()
                .map(TransactionDocumentMapper::toModelEntity)
                .toList();
    }

    @Override
    public void save(Transaction transaction) {
        mongoSpringDataRepository.insert(TransactionDocumentMapper.toDocument(transaction));
    }
}
