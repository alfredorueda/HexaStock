package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper.PortfolioMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springDataRepository.JpaPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.in.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.model.Portfolio;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public class JpaPortfolioRepository implements PortfolioPort {

    private final JpaPortfolioSpringDataRepository jpaSpringDataRepository;

    public JpaPortfolioRepository(JpaPortfolioSpringDataRepository jpaSpringDataRepository) {
        this.jpaSpringDataRepository = jpaSpringDataRepository;
    }

    @Override
    public Portfolio getPortfolioById(String id) {
        PortfolioJpaEntity jpaEntity = jpaSpringDataRepository.findByIdForUpdate(id).orElseThrow(() -> new PortfolioNotFoundException(id));
        return PortfolioMapper.toModelEntity(jpaEntity);
    }

    @Override
    public void createPortfolio(Portfolio portfolio) {
        PortfolioJpaEntity jpaEntity = PortfolioMapper.toJpaEntity(portfolio);
        jpaSpringDataRepository.save(jpaEntity);
    }

    @Override
    public void savePortfolio(Portfolio portfolio) {
        PortfolioJpaEntity jpaEntity = PortfolioMapper.toJpaEntity(portfolio);
        jpaSpringDataRepository.save(jpaEntity);
    }
}