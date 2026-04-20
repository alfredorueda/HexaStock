package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.TransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoTransactionSpringDataRepository extends MongoRepository<TransactionDocument, String> {

    List<TransactionDocument> findAllByPortfolioIdOrderByCreatedAtAsc(String portfolioId);
}
