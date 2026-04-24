package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.AbstractTransactionPortContractTest;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.TransactionPort;
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
 * JPA/MySQL concrete implementation of the {@link TransactionPort} contract tests.
 *
 * <p>Runs the technology-agnostic contract defined in
 * {@link AbstractTransactionPortContractTest} against a real MySQL 8
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
@Import(JpaTransactionRepository.class)
@ActiveProfiles("jpa")
@DisplayName("JpaTransactionRepository – TransactionPort contract (Testcontainers MySQL)")
class JpaTransactionRepositoryContractTest extends AbstractTransactionPortContractTest {

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

    @Autowired
    private JpaTransactionRepository repository;

    @Override
    protected TransactionPort port() {
        return repository;
    }

    @Override @Test protected void depositRoundTrip()                  { super.depositRoundTrip(); }
    @Override @Test protected void purchaseRoundTrip()                 { super.purchaseRoundTrip(); }
    @Override @Test protected void saleRoundTrip()                     { super.saleRoundTrip(); }
    @Override @Test protected void unknownPortfolio_returnsEmptyList() { super.unknownPortfolio_returnsEmptyList(); }
}
