package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPortfolioSpringDataRepository extends JpaRepository <PortfolioJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
    Optional<PortfolioJpaEntity> findByIdForUpdate(@Param("id") String id);

}
