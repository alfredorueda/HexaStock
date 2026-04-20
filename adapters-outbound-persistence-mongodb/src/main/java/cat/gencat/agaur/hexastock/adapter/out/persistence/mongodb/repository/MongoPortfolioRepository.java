package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper.PortfolioDocumentMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of {@link PortfolioPort}.
 */
@Component
@Profile("mongodb")
public class MongoPortfolioRepository implements PortfolioPort {

    private final MongoPortfolioSpringDataRepository mongoSpringDataRepository;

    public MongoPortfolioRepository(MongoPortfolioSpringDataRepository mongoSpringDataRepository) {
        this.mongoSpringDataRepository = mongoSpringDataRepository;
    }

    @Override
    public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
        return mongoSpringDataRepository.findById(portfolioId.value())
                .map(PortfolioDocumentMapper::toModelEntity);
    }

    @Override
    public void createPortfolio(Portfolio portfolio) {
        mongoSpringDataRepository.insert(PortfolioDocumentMapper.toDocument(portfolio));
    }

    @Override
    public void savePortfolio(Portfolio portfolio) {
        mongoSpringDataRepository.save(PortfolioDocumentMapper.toDocument(portfolio));
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return mongoSpringDataRepository.findAll().stream()
                .map(PortfolioDocumentMapper::toModelEntity)
                .toList();
    }
}
