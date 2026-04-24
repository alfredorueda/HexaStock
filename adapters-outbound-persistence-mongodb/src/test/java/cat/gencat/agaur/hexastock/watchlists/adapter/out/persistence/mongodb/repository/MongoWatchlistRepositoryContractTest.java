package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.springdatarepository.MongoWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.watchlists.application.port.out.AbstractWatchlistPortContractTest;
import cat.gencat.agaur.hexastock.watchlists.application.port.out.WatchlistPort;
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
@Import(MongoWatchlistRepository.class)
@ActiveProfiles("mongodb")
@DisplayName("MongoWatchlistRepository - WatchlistPort contract (Testcontainers MongoDB)")
class MongoWatchlistRepositoryContractTest extends AbstractWatchlistPortContractTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoWatchlistRepository repository;

    @Autowired
    private MongoWatchlistSpringDataRepository springDataRepository;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Override
    protected WatchlistPort port() {
        return repository;
    }

    @Test
    @Override
    protected void createAndGetById_roundTrip() {
        assertDoesNotThrow(super::createAndGetById_roundTrip);
    }

    @Test
    @Override
    protected void saveWatchlist_persistsAlerts() {
        assertDoesNotThrow(super::saveWatchlist_persistsAlerts);
    }

    @Test
    @Override
    protected void deleteWatchlist_removes() {
        assertDoesNotThrow(super::deleteWatchlist_removes);
    }
}

