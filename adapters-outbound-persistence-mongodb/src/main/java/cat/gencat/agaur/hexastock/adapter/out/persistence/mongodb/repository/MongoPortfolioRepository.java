package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper.PortfolioDocumentMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of {@link PortfolioPort}.
 */
@Component
@Profile("mongodb")
public class MongoPortfolioRepository implements PortfolioPort {

    private static final String VERSION_CONTEXT_RESOURCE_KEY =
            MongoPortfolioRepository.class.getName() + ".version-context";

    private final MongoPortfolioSpringDataRepository mongoSpringDataRepository;

    public MongoPortfolioRepository(MongoPortfolioSpringDataRepository mongoSpringDataRepository) {
        this.mongoSpringDataRepository = mongoSpringDataRepository;
    }

    @Override
    public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
        return mongoSpringDataRepository.findById(portfolioId.value())
                .map(document -> {
                    rememberVersion(document.getId(), document.getVersion());
                    return PortfolioDocumentMapper.toModelEntity(document);
                });
    }

    @Override
    public void createPortfolio(Portfolio portfolio) {
        PortfolioDocument created = mongoSpringDataRepository.insert(PortfolioDocumentMapper.toDocument(portfolio));
        rememberVersion(created.getId(), created.getVersion());
    }

    @Override
    public void savePortfolio(Portfolio portfolio) {
        String portfolioId = portfolio.getId().value();
        Long expectedVersion = resolveExpectedVersion(portfolioId);

        PortfolioDocument saved = mongoSpringDataRepository.save(
                PortfolioDocumentMapper.toDocument(portfolio, expectedVersion)
        );
        rememberVersion(saved.getId(), saved.getVersion());
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return mongoSpringDataRepository.findAll().stream()
                .map(PortfolioDocumentMapper::toModelEntity)
                .toList();
    }

    private Long resolveExpectedVersion(String portfolioId) {
        return getVersionFromContext(portfolioId)
                .or(() -> mongoSpringDataRepository.findById(portfolioId).map(PortfolioDocument::getVersion))
                .orElse(null);
    }

    private Optional<Long> getVersionFromContext(String portfolioId) {
        if (!isTransactionContextAvailable()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOrCreateVersionContext().get(portfolioId));
    }

    private void rememberVersion(String portfolioId, Long version) {
        if (!isTransactionContextAvailable()) {
            return;
        }
        getOrCreateVersionContext().put(portfolioId, version);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getOrCreateVersionContext() {
        Map<String, Long> context =
                (Map<String, Long>) TransactionSynchronizationManager.getResource(VERSION_CONTEXT_RESOURCE_KEY);
        if (context != null) {
            return context;
        }

        Map<String, Long> newContext = new HashMap<>();
        TransactionSynchronizationManager.bindResource(VERSION_CONTEXT_RESOURCE_KEY, newContext);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(VERSION_CONTEXT_RESOURCE_KEY)) {
                    TransactionSynchronizationManager.unbindResource(VERSION_CONTEXT_RESOURCE_KEY);
                }
            }
        });
        return newContext;
    }

    private boolean isTransactionContextAvailable() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }
}
