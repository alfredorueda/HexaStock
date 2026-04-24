package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.SharedMySQLContainer;
import cat.gencat.agaur.hexastock.application.port.out.AbstractPortfolioPortContractTest;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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

    // ── JPA-specific: multi-holding batch-fetch round-trip ────────────

    @Test
    @DisplayName("round-trip preserves multiple holdings with multiple lots (exercises @BatchSize)")
    void multipleHoldingsAndLots_roundTrip() {
        Portfolio portfolio = new Portfolio(PortfolioId.of("p-batch"), "BatchTest", Money.of(50000), NOW);

        // AAPL: 2 lots
        Holding aapl = new Holding(HoldingId.of("h-aapl"), Ticker.of("AAPL"));
        aapl.addLotFromPersistence(new Lot(LotId.of("lot-a1"), ShareQuantity.of(10), ShareQuantity.of(10), Price.of(150), NOW));
        aapl.addLotFromPersistence(new Lot(LotId.of("lot-a2"), ShareQuantity.of(5), ShareQuantity.of(5), Price.of(155), NOW.plusDays(1)));
        portfolio.addHolding(aapl);

        // GOOG: 2 lots
        Holding goog = new Holding(HoldingId.of("h-goog"), Ticker.of("GOOG"));
        goog.addLotFromPersistence(new Lot(LotId.of("lot-g1"), ShareQuantity.of(20), ShareQuantity.of(20), Price.of(180), NOW));
        goog.addLotFromPersistence(new Lot(LotId.of("lot-g2"), ShareQuantity.of(8), ShareQuantity.of(8), Price.of(185), NOW.plusDays(2)));
        portfolio.addHolding(goog);

        // MSFT: 1 lot
        Holding msft = new Holding(HoldingId.of("h-msft"), Ticker.of("MSFT"));
        msft.addLotFromPersistence(new Lot(LotId.of("lot-m1"), ShareQuantity.of(15), ShareQuantity.of(15), Price.of(420), NOW));
        portfolio.addHolding(msft);

        port().createPortfolio(portfolio);

        Portfolio found = port().getPortfolioById(PortfolioId.of("p-batch")).orElseThrow();
        assertThat(found.getHoldings()).hasSize(3);

        // Verify each holding's lots are fully reconstituted
        Holding foundAapl = found.getHolding(Ticker.of("AAPL"));
        assertThat(foundAapl.getLots()).hasSize(2);
        assertThat(foundAapl.getTotalShares()).isEqualTo(ShareQuantity.of(15));

        Holding foundGoog = found.getHolding(Ticker.of("GOOG"));
        assertThat(foundGoog.getLots()).hasSize(2);
        assertThat(foundGoog.getTotalShares()).isEqualTo(ShareQuantity.of(28));

        Holding foundMsft = found.getHolding(Ticker.of("MSFT"));
        assertThat(foundMsft.getLots()).hasSize(1);
        assertThat(foundMsft.getTotalShares()).isEqualTo(ShareQuantity.of(15));
    }
}
