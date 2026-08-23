package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.WatchlistDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoWatchlistSpringDataRepository extends MongoRepository<WatchlistDocument, String> {
}

