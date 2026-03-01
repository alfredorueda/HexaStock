package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.WatchlistMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository.JpaWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.model.Watchlist;
import cat.gencat.agaur.hexastock.model.WatchlistId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("jpa")
public class JpaWatchlistRepository implements WatchlistPort {

    private final JpaWatchlistSpringDataRepository jpaWatchlistSpringDataRepository;

    public JpaWatchlistRepository(JpaWatchlistSpringDataRepository jpaWatchlistSpringDataRepository) {
        this.jpaWatchlistSpringDataRepository = jpaWatchlistSpringDataRepository;
    }

    @Override
    public Optional<Watchlist> getWatchlistById(WatchlistId watchlistId) {
        return jpaWatchlistSpringDataRepository.findByIdForUpdate(watchlistId.value())
                .map(WatchlistMapper::toModelEntity);
    }

    @Override
    public void createWatchlist(Watchlist watchlist) {
        jpaWatchlistSpringDataRepository.save(WatchlistMapper.toJpaEntity(watchlist));
    }

    @Override
    public void saveWatchlist(Watchlist watchlist) {
        jpaWatchlistSpringDataRepository.save(WatchlistMapper.toJpaEntity(watchlist));
    }

    @Override
    public void deleteWatchlist(WatchlistId watchlistId) {
        jpaWatchlistSpringDataRepository.deleteById(watchlistId.value());
    }
}
