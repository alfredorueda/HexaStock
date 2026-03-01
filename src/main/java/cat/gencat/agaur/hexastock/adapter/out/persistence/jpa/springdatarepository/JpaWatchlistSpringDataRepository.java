package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.WatchlistJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaWatchlistSpringDataRepository extends JpaRepository<WatchlistJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WatchlistJpaEntity w WHERE w.id = :id")
    Optional<WatchlistJpaEntity> findByIdForUpdate(@Param("id") String id);
}
