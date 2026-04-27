package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.OptimisticVersionContext;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.mapper.PortfolioDocumentMapper;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.springdatarepository.MongoPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
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
    private final OptimisticVersionContext versionContext =
            new OptimisticVersionContext(MongoPortfolioRepository.class);

    public MongoPortfolioRepository(MongoPortfolioSpringDataRepository mongoSpringDataRepository) {
        this.mongoSpringDataRepository = mongoSpringDataRepository;
    }

    @Override
    public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
        return mongoSpringDataRepository.findById(portfolioId.value())
                .map(document -> {
                    versionContext.remember(document.getId(), document.getVersion());
                    return PortfolioDocumentMapper.toModelEntity(document);
                });
    }

    @Override
    public void createPortfolio(Portfolio portfolio) {
        PortfolioDocument created = mongoSpringDataRepository.insert(PortfolioDocumentMapper.toDocument(portfolio));
        versionContext.remember(created.getId(), created.getVersion());
    }

    @Override
    public void savePortfolio(Portfolio portfolio) {
        String id = portfolio.getId().value();
        Long version = versionContext.resolve(id,
                () -> mongoSpringDataRepository.findById(id).map(PortfolioDocument::getVersion));

        PortfolioDocument saved = mongoSpringDataRepository.save(
                PortfolioDocumentMapper.toDocument(portfolio, version));
        versionContext.remember(saved.getId(), saved.getVersion());
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return mongoSpringDataRepository.findAll().stream()
                .map(PortfolioDocumentMapper::toModelEntity)
                .toList();
    }
}
