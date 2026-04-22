package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.application.port.out.AbstractWatchlistQueryPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistPort;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@Import({JpaWatchlistRepository.class, JpaWatchlistQueryRepository.class})
@ActiveProfiles("jpa")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaWatchlistQueryRepositoryContractTest extends AbstractWatchlistQueryPortContractTest {

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

    @Autowired
    private JpaWatchlistRepository watchlistRepository;

    @Autowired
    private JpaWatchlistQueryRepository watchlistQueryRepository;

    @Override
    protected WatchlistPort watchlistPort() {
        return watchlistRepository;
    }

    @Override
    protected WatchlistQueryPort queryPort() {
        return watchlistQueryRepository;
    }

    @Override @Test protected void distinctTickers_activeOnly() { super.distinctTickers_activeOnly(); }
    @Override @Test protected void triggeredAlerts_filterByThreshold() { super.triggeredAlerts_filterByThreshold(); }
}

