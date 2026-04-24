package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.jpa.mapper.WatchlistMapper;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.jpa.springdatarepository.JpaWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.watchlists.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("jpa")
public class JpaWatchlistRepository implements WatchlistPort {

    private final JpaWatchlistSpringDataRepository springDataRepository;

    public JpaWatchlistRepository(JpaWatchlistSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Watchlist> getWatchlistById(WatchlistId id) {
        return springDataRepository.findByIdForUpdate(id.value())
                .map(WatchlistMapper::toModelEntity);
    }

    @Override
    public void createWatchlist(Watchlist watchlist) {
        springDataRepository.save(WatchlistMapper.toJpaEntity(watchlist));
    }

    @Override
    public void saveWatchlist(Watchlist watchlist) {
        springDataRepository.save(WatchlistMapper.toJpaEntity(watchlist));
    }

    @Override
    public void deleteWatchlist(WatchlistId id) {
        springDataRepository.deleteById(id.value());
    }
}

