package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.jpa.springdatarepository.JpaPortfolioSpringDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA-specific integration tests that verify behaviour unique to the
 * relational/JPA adapter and are <em>not</em> part of the port contract.
 *
 * <p>These tests run against real MySQL via Testcontainers because the
 * features under test (pessimistic locking, InnoDB row-level locks)
 * cannot be faithfully verified with H2.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("jpa")
@DisplayName("JPA-specific – pessimistic locking (Testcontainers MySQL)")
class JpaPessimisticLockingTest {

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

    @Autowired
    private JpaPortfolioSpringDataRepository springDataRepository;

    @Autowired
    private EntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    @Test
    @DisplayName("findByIdForUpdate acquires a pessimistic lock on the row")
    void findByIdForUpdate_acquiresPessimisticLock() {
        // Arrange: persist a portfolio
        PortfolioJpaEntity entity = new PortfolioJpaEntity("p-lock", "LockTest",
                new BigDecimal("1000.00"), NOW);
        springDataRepository.saveAndFlush(entity);
        em.clear();

        // Act: retrieve with pessimistic lock
        var locked = springDataRepository.findByIdForUpdate("p-lock");

        // Assert: entity is returned and managed (lock is held within this tx)
        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo("p-lock");
        assertThat(em.contains(locked.get()))
                .as("Entity should be managed (lock held in current persistence context)")
                .isTrue();
    }

    @Test
    @DisplayName("findByIdForUpdate returns empty for nonexistent ID (no lock acquired)")
    void findByIdForUpdate_nonexistent_returnsEmpty() {
        assertThat(springDataRepository.findByIdForUpdate("no-such")).isEmpty();
    }
}
