package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.mapper.WatchlistDocumentMapper;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.springdatarepository.MongoWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.watchlists.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("mongodb")
public class MongoWatchlistRepository implements WatchlistPort {

    private final MongoWatchlistSpringDataRepository springDataRepository;

    public MongoWatchlistRepository(MongoWatchlistSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Watchlist> getWatchlistById(WatchlistId id) {
        return springDataRepository.findById(id.value())
                .map(WatchlistDocumentMapper::toModelEntity);
    }

    @Override
    public void createWatchlist(Watchlist watchlist) {
        springDataRepository.insert(WatchlistDocumentMapper.toDocument(watchlist));
    }

    @Override
    public void saveWatchlist(Watchlist watchlist) {
        springDataRepository.save(WatchlistDocumentMapper.toDocument(watchlist));
    }

    @Override
    public void deleteWatchlist(WatchlistId id) {
        springDataRepository.deleteById(id.value());
    }
}

