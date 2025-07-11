package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.PortfolioMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.TransactionMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository.JpaPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository.JpaTransactionSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("jpa")
public class JpaTransactionRepository implements TransactionPort {

    private final JpaTransactionSpringDataRepository jpaSpringDataRepository;

    public JpaTransactionRepository(JpaTransactionSpringDataRepository jpaSpringDataRepository) {
        this.jpaSpringDataRepository = jpaSpringDataRepository;
    }


    @Override
    public List<Transaction> getTransactionsByPortfolioId(String portFolioId) {
        List<TransactionJpaEntity> lTransactions = jpaSpringDataRepository.getAllByPortfolioId(portFolioId);
        return lTransactions.stream().map(TransactionMapper::toModelEntity).toList();
    }

    @Override
    public void save(Transaction transaction) {
        TransactionJpaEntity jpaEntity = TransactionMapper.toJpaEntity(transaction);
        jpaSpringDataRepository.save(jpaEntity);
    }
}