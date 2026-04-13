package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.application.port.out.AbstractPortfolioPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * JPA/MySQL concrete implementation of the {@link PortfolioPort} contract tests.
 *
 * <p>Runs the technology-agnostic contract defined in
 * {@link AbstractPortfolioPortContractTest} against a real MySQL 8
 * instance managed by Testcontainers.</p>
 *
 * <p>Each test method delegates to the inherited contract assertion.
 * The override is necessary so that Spring's
 * {@code TransactionalTestExecutionListener} resolves {@code @Transactional}
 * from this class (via {@code @DataJpaTest}) rather than from the
 * framework-agnostic abstract superclass which has no Spring annotations.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaPortfolioRepository.class)
@ActiveProfiles("jpa")
@DisplayName("JpaPortfolioRepository – PortfolioPort contract (Testcontainers MySQL)")
class JpaPortfolioRepositoryContractTest extends AbstractPortfolioPortContractTest {

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

    @Autowired
    private JpaPortfolioRepository repository;

    @Override
    protected PortfolioPort port() {
        return repository;
    }

    @Override @Test protected void createAndGetById_roundTrip()           { super.createAndGetById_roundTrip(); }
    @Override @Test protected void savePortfolio_updatesBalance()         { super.savePortfolio_updatesBalance(); }
    @Override @Test protected void getAllPortfolios_returnsAll()          { super.getAllPortfolios_returnsAll(); }
    @Override @Test protected void getPortfolioById_nonexistent_returnsEmpty() { super.getPortfolioById_nonexistent_returnsEmpty(); }
    @Override @Test protected void portfolioWithHoldingsAndLots_roundTrip()    { super.portfolioWithHoldingsAndLots_roundTrip(); }
}
