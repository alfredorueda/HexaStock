package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.AbstractWatchlistQueryPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataMongoTest
@Import({MongoWatchlistRepository.class, MongoWatchlistQueryRepository.class})
@ActiveProfiles("mongodb")
@DisplayName("MongoWatchlistQueryRepository - WatchlistQueryPort contract (Testcontainers MongoDB)")
class MongoWatchlistQueryRepositoryContractTest extends AbstractWatchlistQueryPortContractTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoWatchlistRepository watchlistRepository;

    @Autowired
    private MongoWatchlistQueryRepository queryRepository;

    @Autowired
    private MongoWatchlistSpringDataRepository springDataRepository;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Override
    protected WatchlistPort watchlistPort() {
        return watchlistRepository;
    }

    @Override
    protected WatchlistQueryPort queryPort() {
        return queryRepository;
    }

    @Test
    @Override
    protected void distinctTickers_activeOnly() {
        assertDoesNotThrow(super::distinctTickers_activeOnly);
    }

    @Test
    @Override
    protected void triggeredAlerts_filterByThreshold() {
        assertDoesNotThrow(super::triggeredAlerts_filterByThreshold);
    }
}

