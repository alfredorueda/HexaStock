package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.application.port.out.AbstractWatchlistPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@Import(JpaWatchlistRepository.class)
@ActiveProfiles("jpa")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaWatchlistRepositoryContractTest extends AbstractWatchlistPortContractTest {

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

    @Autowired
    private JpaWatchlistRepository repository;

    @Override
    protected WatchlistPort port() {
        return repository;
    }

    @Override @Test protected void createAndGetById_roundTrip() { super.createAndGetById_roundTrip(); }
    @Override @Test protected void saveWatchlist_persistsAlerts() { super.saveWatchlist_persistsAlerts(); }
    @Override @Test protected void deleteWatchlist_removes() { super.deleteWatchlist_removes(); }
}

