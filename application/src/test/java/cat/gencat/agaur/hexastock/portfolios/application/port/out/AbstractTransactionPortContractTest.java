package cat.gencat.agaur.hexastock.portfolios.application.port.out;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technology-agnostic contract tests for {@link TransactionPort}.
 *
 * <p>Every persistence adapter that implements {@code TransactionPort}
 * must extend this class and supply a concrete {@link #port()} instance.
 * The exact same assertions run against JPA/MySQL, MongoDB, etc.</p>
 */
public abstract class AbstractTransactionPortContractTest {

    protected static final PortfolioId PID = PortfolioId.of("p-1");
    protected static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    /** Subclasses provide the implementation under test. */
    protected abstract TransactionPort port();

    // ── Deposit round-trip ───────────────────────────────────────────

    @Test
    @SpecificationRef(value = "US-08.AC-1", level = TestLevel.INTEGRATION,
            feature = "get-transaction-history.feature")
    @DisplayName("save and retrieve deposit transaction preserves all fields")
    protected void depositRoundTrip() {
        DepositTransaction tx = new DepositTransaction(
                TransactionId.of("tx-d"), PID, Money.of(500), NOW);
        port().save(tx);

        List<Transaction> txs = port().getTransactionsByPortfolioId(PID);
        assertThat(txs).hasSize(1);

        Transaction found = txs.get(0);
        assertThat(found.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(found.totalAmount()).isEqualTo(Money.of(500));
        assertThat(found.ticker()).isNull();
    }

    // ── Purchase round-trip ──────────────────────────────────────────

    @Test
    @DisplayName("save and retrieve purchase transaction preserves stock fields")
    protected void purchaseRoundTrip() {
        PurchaseTransaction tx = new PurchaseTransaction(
                TransactionId.of("tx-p"), PID,
                Ticker.of("AAPL"), ShareQuantity.positive(10),
                Price.of(150), Money.of(1500), NOW);
        port().save(tx);

        List<Transaction> txs = port().getTransactionsByPortfolioId(PID);
        assertThat(txs).hasSize(1);

        Transaction found = txs.get(0);
        assertThat(found.type()).isEqualTo(TransactionType.PURCHASE);
        assertThat(found.ticker()).isEqualTo(Ticker.of("AAPL"));
        assertThat(found.quantity()).isEqualTo(ShareQuantity.of(10));
        assertThat(found.unitPrice()).isEqualTo(Price.of(150));
        assertThat(found.totalAmount()).isEqualTo(Money.of(1500));
    }

    // ── Sale round-trip ──────────────────────────────────────────────

    @Test
    @SpecificationRef(value = "US-07.FIFO-1", level = TestLevel.INTEGRATION,
            feature = "sell-stocks.feature")
    @DisplayName("save and retrieve sale transaction preserves profit")
    protected void saleRoundTrip() {
        SaleTransaction tx = new SaleTransaction(
                TransactionId.of("tx-s"), PID,
                Ticker.of("MSFT"), ShareQuantity.positive(5),
                Price.of(300), Money.of(1500), Money.of(250), NOW);
        port().save(tx);

        List<Transaction> txs = port().getTransactionsByPortfolioId(PID);
        assertThat(txs).hasSize(1);

        Transaction found = txs.get(0);
        assertThat(found.type()).isEqualTo(TransactionType.SALE);
        assertThat(found.profit()).isEqualTo(Money.of(250));
    }

    // ── Empty list for unknown portfolio ────────────────────────────

    @Test
    @DisplayName("getTransactionsByPortfolioId returns empty list for unknown portfolio")
    protected void unknownPortfolio_returnsEmptyList() {
        assertThat(port().getTransactionsByPortfolioId(PortfolioId.of("unknown")))
                .isEmpty();
    }
}
