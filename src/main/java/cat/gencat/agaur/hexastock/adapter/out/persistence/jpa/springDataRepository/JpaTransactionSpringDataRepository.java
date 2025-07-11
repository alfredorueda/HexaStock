package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTransactionSpringDataRepository extends JpaRepository <TransactionJpaEntity, String> {

    List<TransactionJpaEntity> getAllByPortfolioId(String portFolioId);
}
