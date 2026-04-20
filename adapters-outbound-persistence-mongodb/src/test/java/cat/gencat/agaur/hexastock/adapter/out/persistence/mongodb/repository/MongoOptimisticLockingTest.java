package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoContainer;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.SpringDataMongoPortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mongo-specific tests that verify behaviour unique to the MongoDB adapter and are
 * <em>not</em> part of the {@code PortfolioPort} contract.
 *
 * <p>Mirrors the role of {@code JpaPessimisticLockingTest} in the JPA module: both
 * exist to document and assert the concurrency semantics of the respective adapter
 * (see ADR-012 for JPA, ADR-016 for MongoDB).</p>
 */
@DataMongoTest
@ActiveProfiles("mongodb")
@DisplayName("MongoDB-specific – optimistic locking via @Version (Testcontainers MongoDB)")
class MongoOptimisticLockingTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", SharedMongoContainer.INSTANCE::getReplicaSetUrl);
    }

    @Autowired
    private SpringDataMongoPortfolioRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.getDb().drop();
    }

    @Test
    @DisplayName("save bumps the @Version field on each update")
    void save_incrementsVersion() {
        PortfolioDocument doc = new PortfolioDocument(
                "p-v", "VersionTest", new BigDecimal("1000.00"),
                NOW, List.of(), null);
        PortfolioDocument v0 = repository.save(doc);
        assertThat(v0.getVersion()).isEqualTo(0L);

        PortfolioDocument v1 = repository.save(new PortfolioDocument(
                "p-v", "VersionTest", new BigDecimal("1500.00"),
                NOW, List.of(), v0.getVersion()));
        assertThat(v1.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("saving with a stale version raises OptimisticLockingFailureException")
    void staleVersion_raisesOptimisticLockingFailure() {
        PortfolioDocument initial = repository.save(new PortfolioDocument(
                "p-stale", "StaleTest", new BigDecimal("100.00"),
                NOW, List.of(), null));
        Long staleVersion = initial.getVersion();

        // Winner commits: version goes from 0 → 1 in the database.
        repository.save(new PortfolioDocument(
                "p-stale", "StaleTest", new BigDecimal("200.00"),
                NOW, List.of(), staleVersion));

        // Loser attempts to save using the now-stale version and must fail.
        assertThatThrownBy(() -> repository.save(new PortfolioDocument(
                "p-stale", "StaleTest", new BigDecimal("999.00"),
                NOW, List.of(), staleVersion)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // Database still reflects the winner's write, not the loser's.
        assertThat(repository.findById("p-stale")).isPresent()
                .get()
                .extracting(PortfolioDocument::getBalance)
                .isEqualTo(new BigDecimal("200.00"));
    }
}
