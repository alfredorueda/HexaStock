package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoContainer;
import cat.gencat.agaur.hexastock.application.port.out.AbstractPortfolioPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * MongoDB concrete implementation of the {@link PortfolioPort} contract tests.
 *
 * <p>Runs the technology-agnostic contract defined in
 * {@link AbstractPortfolioPortContractTest} against a real MongoDB 7
 * instance managed by Testcontainers.</p>
 *
 * <p>Because {@code @DataMongoTest} does not wrap tests in a transaction
 * (MongoDB has no single-node multi-document transactions), each test cleans
 * both collections in {@link #cleanDatabase()} to guarantee isolation.</p>
 */
@DataMongoTest
@Import(MongoPortfolioRepository.class)
@ActiveProfiles("mongodb")
@DisplayName("MongoPortfolioRepository – PortfolioPort contract (Testcontainers MongoDB)")
class MongoPortfolioRepositoryContractTest extends AbstractPortfolioPortContractTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", SharedMongoContainer.INSTANCE::getReplicaSetUrl);
    }

    @Autowired
    private MongoPortfolioRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.getDb().drop();
    }

    @Override
    protected PortfolioPort port() {
        return repository;
    }

    @Override @Test protected void createAndGetById_roundTrip()               { super.createAndGetById_roundTrip(); }
    @Override @Test protected void savePortfolio_updatesBalance()             { super.savePortfolio_updatesBalance(); }
    @Override @Test protected void getAllPortfolios_returnsAll()              { super.getAllPortfolios_returnsAll(); }
    @Override @Test protected void getPortfolioById_nonexistent_returnsEmpty(){ super.getPortfolioById_nonexistent_returnsEmpty(); }
    @Override @Test protected void portfolioWithHoldingsAndLots_roundTrip()   { super.portfolioWithHoldingsAndLots_roundTrip(); }
}
