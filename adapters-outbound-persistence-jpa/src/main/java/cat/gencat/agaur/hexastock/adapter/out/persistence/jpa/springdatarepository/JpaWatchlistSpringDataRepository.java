package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.WatchlistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface JpaWatchlistSpringDataRepository extends JpaRepository<WatchlistJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WatchlistJpaEntity w left join fetch w.alerts where w.id = :id")
    Optional<WatchlistJpaEntity> findByIdForUpdate(@Param("id") String id);
}

