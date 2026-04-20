package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.TransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Low-level Spring Data MongoDB repository for {@link TransactionDocument}.
 */
@Repository
public interface SpringDataMongoTransactionRepository extends MongoRepository<TransactionDocument, String> {

    /** Returns every transaction that belongs to the given portfolio. */
    List<TransactionDocument> findByPortfolioId(String portfolioId);
}
