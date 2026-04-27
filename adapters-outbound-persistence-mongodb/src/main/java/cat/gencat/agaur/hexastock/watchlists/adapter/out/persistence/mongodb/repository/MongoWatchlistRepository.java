package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.OptimisticVersionContext;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.document.WatchlistDocument;
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
    private final OptimisticVersionContext versionContext =
            new OptimisticVersionContext(MongoWatchlistRepository.class);

    public MongoWatchlistRepository(MongoWatchlistSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Watchlist> getWatchlistById(WatchlistId id) {
        return springDataRepository.findById(id.value())
                .map(doc -> {
                    versionContext.remember(doc.getId(), doc.getVersion());
                    return WatchlistDocumentMapper.toModelEntity(doc);
                });
    }

    @Override
    public void createWatchlist(Watchlist watchlist) {
        WatchlistDocument created = springDataRepository.insert(WatchlistDocumentMapper.toDocument(watchlist));
        versionContext.remember(created.getId(), created.getVersion());
    }

    @Override
    public void saveWatchlist(Watchlist watchlist) {
        String id = watchlist.getId().value();
        Long version = versionContext.resolve(id,
                () -> springDataRepository.findById(id).map(WatchlistDocument::getVersion));

        WatchlistDocument saved = springDataRepository.save(WatchlistDocumentMapper.toDocument(watchlist, version));
        versionContext.remember(saved.getId(), saved.getVersion());
    }

    @Override
    public void deleteWatchlist(WatchlistId id) {
        springDataRepository.deleteById(id.value());
    }
}