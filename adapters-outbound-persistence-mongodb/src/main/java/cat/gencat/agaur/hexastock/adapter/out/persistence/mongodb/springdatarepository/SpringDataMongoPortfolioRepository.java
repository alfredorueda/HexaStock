package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Low-level Spring Data MongoDB repository for {@link PortfolioDocument}.
 *
 * <p>Standard CRUD only. Optimistic-locking semantics are provided automatically by
 * Spring Data MongoDB because {@code PortfolioDocument} declares a {@code @Version}
 * field (see ADR-016); on {@code save()} of an existing document, the update filter
 * includes the current version and a mismatch raises
 * {@link org.springframework.dao.OptimisticLockingFailureException}.</p>
 */
@Repository
public interface SpringDataMongoPortfolioRepository extends MongoRepository<PortfolioDocument, String> {
}
