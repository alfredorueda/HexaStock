package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper.PortfolioDocumentMapper;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.SpringDataMongoPortfolioRepository;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of {@link PortfolioPort}.
 *
 * <h3>Concurrency model</h3>
 * <p>Unlike the JPA adapter (which uses pessimistic row locks — see ADR-012), this
 * adapter relies on Spring Data MongoDB <strong>optimistic locking</strong> via the
 * {@code @Version} field on {@link PortfolioDocument} — see ADR-016.</p>
 *
 * <p>Because the {@link PortfolioPort} contract exposes only domain {@code Portfolio}
 * objects (which must not know about persistence versions), the adapter stashes the
 * {@code version} observed at read time in a request-scoped
 * {@link ThreadLocal} map keyed by portfolio id. On {@code savePortfolio}, the stashed
 * version is attached to the outgoing document so Spring Data can issue the
 * compare-and-set update. On a successful save, the cache is refreshed with the
 * newly-bumped version so that subsequent saves in the same request thread remain
 * consistent.</p>
 *
 * <p><strong>Limitation:</strong> on version mismatch, Spring Data raises
 * {@link org.springframework.dao.OptimisticLockingFailureException} instead of
 * blocking. The application layer (unchanged) does not retry, so one of two truly
 * concurrent writes on the same portfolio will surface an error to the caller.
 * This is a deliberate, documented trade-off — see ADR-016.</p>
 *
 * <p>Active only under the {@code mongodb} Spring profile.</p>
 */
@Component
@Profile("mongodb")
public class MongoPortfolioRepository implements PortfolioPort {

    /**
     * Request-thread-scoped cache of the last-read optimistic-locking version,
     * keyed by portfolio id. Correct under Spring MVC's one-thread-per-request model.
     */
    private static final ThreadLocal<Map<String, Long>> VERSION_CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private final SpringDataMongoPortfolioRepository repository;

    public MongoPortfolioRepository(SpringDataMongoPortfolioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Portfolio> getPortfolioById(PortfolioId portfolioId) {
        return repository.findById(portfolioId.value())
                .map(doc -> {
                    VERSION_CACHE.get().put(doc.getId(), doc.getVersion());
                    return PortfolioDocumentMapper.toDomain(doc);
                });
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return repository.findAll().stream()
                .map(doc -> {
                    VERSION_CACHE.get().put(doc.getId(), doc.getVersion());
                    return PortfolioDocumentMapper.toDomain(doc);
                })
                .toList();
    }

    @Override
    public void createPortfolio(Portfolio portfolio) {
        // version=null ⇒ Spring Data will insert with version=0.
        PortfolioDocument doc = PortfolioDocumentMapper.toDocument(portfolio, null);
        PortfolioDocument saved = repository.insert(doc);
        VERSION_CACHE.get().put(saved.getId(), saved.getVersion());
    }

    @Override
    public void savePortfolio(Portfolio portfolio) {
        String id = portfolio.getId().value();
        Long expectedVersion = VERSION_CACHE.get().get(id);
        PortfolioDocument doc = PortfolioDocumentMapper.toDocument(portfolio, expectedVersion);
        // save() with a non-null @Version triggers CAS: match {_id, version};
        // mismatch raises OptimisticLockingFailureException (see ADR-016).
        PortfolioDocument saved = repository.save(doc);
        VERSION_CACHE.get().put(id, saved.getVersion());
    }
}
