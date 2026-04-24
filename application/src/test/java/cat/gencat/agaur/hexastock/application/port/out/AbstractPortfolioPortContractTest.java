package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technology-agnostic contract tests for {@link PortfolioPort}.
 *
 * <p>Every persistence adapter that implements {@code PortfolioPort} (JPA/MySQL,
 * JPA/PostgreSQL, MongoDB, …) must extend this class and supply a concrete,
 * fully-wired {@link #port()} instance. The contract guarantees that all
 * adapters behave identically from the application layer's perspective.</p>
 *
 * <p><strong>No Spring, no framework annotations here.</strong> Subclasses own
 * the infrastructure wiring ({@code @DataJpaTest}, Testcontainers, etc.).</p>
 */
public abstract class AbstractPortfolioPortContractTest {

    protected static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    /** Subclasses provide the implementation under test. */
    protected abstract PortfolioPort port();

    // ── Round-trip: create → findById ────────────────────────────────

    @Test
    @SpecificationRef(value = "US-01.AC-1", level = TestLevel.INTEGRATION,
            feature = "create-portfolio.feature")
    @DisplayName("create and retrieve portfolio preserves all scalar fields")
    protected void createAndGetById_roundTrip() {
        Portfolio portfolio = new Portfolio(PortfolioId.of("p-1"), "Alice", Money.of(1000), NOW);
        port().createPortfolio(portfolio);

        Optional<Portfolio> result = port().getPortfolioById(PortfolioId.of("p-1"));

        assertThat(result).isPresent();
        Portfolio found = result.get();
        assertThat(found.getId()).isEqualTo(PortfolioId.of("p-1"));
        assertThat(found.getOwnerName()).isEqualTo("Alice");
        assertThat(found.getBalance()).isEqualTo(Money.of(1000));
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
    }

    // ── Update via savePortfolio ─────────────────────────────────────

    @Test
    @DisplayName("savePortfolio persists updated balance")
    protected void savePortfolio_updatesBalance() {
        Portfolio portfolio = new Portfolio(PortfolioId.of("p-2"), "Bob", Money.ZERO, NOW);
        port().createPortfolio(portfolio);

        portfolio.deposit(Money.of(500));
        port().savePortfolio(portfolio);

        Portfolio found = port().getPortfolioById(PortfolioId.of("p-2")).orElseThrow();
        assertThat(found.getBalance()).isEqualTo(Money.of(500));
    }

    // ── getAllPortfolios ──────────────────────────────────────────────

    @Test
    @SpecificationRef(value = "US-03.AC-1", level = TestLevel.INTEGRATION,
            feature = "list-portfolios.feature")
    @DisplayName("getAllPortfolios returns all saved portfolios")
    protected void getAllPortfolios_returnsAll() {
        port().createPortfolio(new Portfolio(PortfolioId.of("p-a"), "Alice", Money.ZERO, NOW));
        port().createPortfolio(new Portfolio(PortfolioId.of("p-b"), "Bob", Money.ZERO, NOW));

        List<Portfolio> all = port().getAllPortfolios();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(p -> p.getId().value()).containsExactlyInAnyOrder("p-a", "p-b");
    }

    // ── Empty result for nonexistent ID ──────────────────────────────

    @Test
    @SpecificationRef(value = "US-02.AC-2", level = TestLevel.INTEGRATION,
            feature = "get-portfolio.feature")
    @DisplayName("getPortfolioById returns empty for nonexistent portfolio")
    protected void getPortfolioById_nonexistent_returnsEmpty() {
        assertThat(port().getPortfolioById(PortfolioId.of("no-such"))).isEmpty();
    }

    // ── Portfolio with holdings and lots ──────────────────────────────

    @Test
    @SpecificationRef(value = "US-06.AC-1", level = TestLevel.INTEGRATION,
            feature = "buy-stocks.feature")
    @DisplayName("round-trip preserves holdings and lots within a portfolio")
    protected void portfolioWithHoldingsAndLots_roundTrip() {
        Portfolio portfolio = new Portfolio(PortfolioId.of("p-h"), "Carol", Money.of(5000), NOW);
        Holding holding = new Holding(HoldingId.of("h-1"), Ticker.of("AAPL"));
        Lot lot = new Lot(LotId.of("lot-1"), ShareQuantity.of(10), ShareQuantity.of(10),
                Price.of(150), NOW);
        holding.addLotFromPersistence(lot);
        portfolio.addHolding(holding);
        port().createPortfolio(portfolio);

        Portfolio found = port().getPortfolioById(PortfolioId.of("p-h")).orElseThrow();
        assertThat(found.getHoldings()).hasSize(1);

        Holding foundHolding = found.getHoldings().get(0);
        assertThat(foundHolding.getTicker()).isEqualTo(Ticker.of("AAPL"));
        assertThat(foundHolding.getLots()).hasSize(1);

        Lot foundLot = foundHolding.getLots().get(0);
        assertThat(foundLot.getInitialShares()).isEqualTo(ShareQuantity.of(10));
        assertThat(foundLot.getRemainingShares()).isEqualTo(ShareQuantity.of(10));
        assertThat(foundLot.getUnitPrice()).isEqualTo(Price.of(150));
    }
}
