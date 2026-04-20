package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoPortfolioSpringDataRepository extends MongoRepository<PortfolioDocument, String> {
}
