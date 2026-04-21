package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoPortfolioSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.AbstractPortfolioPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataMongoTest
@Import(MongoPortfolioRepository.class)
@ActiveProfiles("mongodb")
@DisplayName("MongoPortfolioRepository - PortfolioPort contract (Testcontainers MongoDB)")
class MongoPortfolioRepositoryContractTest extends AbstractPortfolioPortContractTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoPortfolioRepository repository;

    @Autowired
    private MongoPortfolioSpringDataRepository springDataRepository;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Override
    protected PortfolioPort port() {
        return repository;
    }

    @Override @Test protected void createAndGetById_roundTrip()                { super.createAndGetById_roundTrip(); }
    @Override @Test protected void savePortfolio_updatesBalance()              { super.savePortfolio_updatesBalance(); }
    @Override @Test protected void getAllPortfolios_returnsAll()               { super.getAllPortfolios_returnsAll(); }
    @Override @Test protected void getPortfolioById_nonexistent_returnsEmpty() { super.getPortfolioById_nonexistent_returnsEmpty(); }
    @Override @Test protected void portfolioWithHoldingsAndLots_roundTrip()    { super.portfolioWithHoldingsAndLots_roundTrip(); }
}
