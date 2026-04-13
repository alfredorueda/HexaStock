package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.HoldingJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.LotJpaEntity;
import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.PortfolioJpaEntity;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.model.portfolio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Persistence mappers – unit tests")
class MapperTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    // ── PortfolioMapper ──────────────────────────────────────────────

    @Nested
    @DisplayName("PortfolioMapper")
    class PortfolioMapperTests {

        @Test
        @DisplayName("toModelEntity maps all scalar fields from JPA entity to domain")
        void toModelEntity_mapsScalarFields() {
            PortfolioJpaEntity jpa = new PortfolioJpaEntity("p-1", "Alice",
                    new BigDecimal("1000.00"), NOW);

            Portfolio domain = PortfolioMapper.toModelEntity(jpa);

            assertThat(domain.getId()).isEqualTo(PortfolioId.of("p-1"));
            assertThat(domain.getOwnerName()).isEqualTo("Alice");
            assertThat(domain.getBalance()).isEqualTo(Money.of(1000));
            assertThat(domain.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toJpaEntity maps all scalar fields from domain to JPA entity")
        void toJpaEntity_mapsScalarFields() {
            Portfolio domain = new Portfolio(PortfolioId.of("p-2"), "Bob", Money.of(500), NOW);

            PortfolioJpaEntity jpa = PortfolioMapper.toJpaEntity(domain);

            assertThat(jpa.getId()).isEqualTo("p-2");
            assertThat(jpa.getOwnerName()).isEqualTo("Bob");
            assertThat(jpa.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(jpa.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toModelEntity reconstructs holdings from JPA entity")
        void toModelEntity_reconstructsHoldings() {
            PortfolioJpaEntity jpa = new PortfolioJpaEntity("p-3", "Carol",
                    BigDecimal.ZERO, NOW);
            HoldingJpaEntity holdingJpa = new HoldingJpaEntity("h-1", "AAPL");
            LotJpaEntity lotJpa = new LotJpaEntity("lot-1", 10, 10,
                    new BigDecimal("150.00"), NOW);
            holdingJpa.setLots(List.of(lotJpa));
            jpa.setHoldings(java.util.Set.of(holdingJpa));

            Portfolio domain = PortfolioMapper.toModelEntity(jpa);

            assertThat(domain.getHoldings()).hasSize(1);
            assertThat(domain.getHoldings().get(0).getTicker()).isEqualTo(Ticker.of("AAPL"));
            assertThat(domain.getHoldings().get(0).getLots()).hasSize(1);
        }
    }

    // ── LotMapper ────────────────────────────────────────────────────

    @Nested
    @DisplayName("LotMapper")
    class LotMapperTests {

        @Test
        @DisplayName("toModelEntity maps lot fields correctly")
        void toModelEntity() {
            LotJpaEntity jpa = new LotJpaEntity("lot-1", 20, 15,
                    new BigDecimal("100.50"), NOW);

            Lot domain = LotMapper.toModelEntity(jpa);

            assertThat(domain.getId()).isEqualTo(LotId.of("lot-1"));
            assertThat(domain.getInitialShares()).isEqualTo(ShareQuantity.of(20));
            assertThat(domain.getRemainingShares()).isEqualTo(ShareQuantity.of(15));
            assertThat(domain.getUnitPrice()).isEqualTo(Price.of(100.50));
            assertThat(domain.getPurchasedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toJpaEntity maps lot fields correctly")
        void toJpaEntity() {
            Lot domain = new Lot(LotId.of("lot-2"), ShareQuantity.of(5),
                    ShareQuantity.of(3), Price.of(200), NOW);

            LotJpaEntity jpa = LotMapper.toJpaEntity(domain);

            assertThat(jpa.getId()).isEqualTo("lot-2");
            assertThat(jpa.getInitialStocks()).isEqualTo(5);
            assertThat(jpa.getRemaining()).isEqualTo(3);
            assertThat(jpa.getUnitPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }

    // ── HoldingMapper ────────────────────────────────────────────────

    @Nested
    @DisplayName("HoldingMapper")
    class HoldingMapperTests {

        @Test
        @DisplayName("toModelEntity maps holding with lots")
        void toModelEntity() {
            HoldingJpaEntity jpa = new HoldingJpaEntity("h-1", "MSFT");
            LotJpaEntity lot = new LotJpaEntity("lot-1", 10, 10,
                    new BigDecimal("300.00"), NOW);
            jpa.setLots(List.of(lot));

            Holding domain = HoldingMapper.toModelEntity(jpa);

            assertThat(domain.getId()).isEqualTo(HoldingId.of("h-1"));
            assertThat(domain.getTicker()).isEqualTo(Ticker.of("MSFT"));
            assertThat(domain.getLots()).hasSize(1);
        }

        @Test
        @DisplayName("toJpaEntity maps holding with lots")
        void toJpaEntity() {
            Holding domain = new Holding(HoldingId.of("h-2"), Ticker.of("GOOG"));
            domain.addLotFromPersistence(new Lot(LotId.of("lot-2"),
                    ShareQuantity.of(8), ShareQuantity.of(8), Price.of(140), NOW));

            HoldingJpaEntity jpa = HoldingMapper.toJpaEntity(domain);

            assertThat(jpa.getId()).isEqualTo("h-2");
            assertThat(jpa.getTicker()).isEqualTo("GOOG");
            assertThat(jpa.getLots()).hasSize(1);
        }
    }

    // ── TransactionMapper ────────────────────────────────────────────

    @Nested
    @DisplayName("TransactionMapper")
    class TransactionMapperTests {

        @Test
        @DisplayName("deposit round-trip through mapper preserves all fields")
        void depositRoundTrip() {
            var deposit = new cat.gencat.agaur.hexastock.model.transaction.DepositTransaction(
                    cat.gencat.agaur.hexastock.model.transaction.TransactionId.of("tx-d"),
                    PortfolioId.of("p-1"), Money.of(500), NOW);

            var jpa = TransactionMapper.toJpaEntity(deposit);
            var back = TransactionMapper.toModelEntity(jpa);

            assertThat(back.type()).isEqualTo(cat.gencat.agaur.hexastock.model.transaction.TransactionType.DEPOSIT);
            assertThat(back.totalAmount()).isEqualTo(Money.of(500));
            assertThat(back.id().value()).isEqualTo("tx-d");
        }

        @Test
        @DisplayName("purchase round-trip through mapper preserves stock fields")
        void purchaseRoundTrip() {
            var purchase = new cat.gencat.agaur.hexastock.model.transaction.PurchaseTransaction(
                    cat.gencat.agaur.hexastock.model.transaction.TransactionId.of("tx-p"),
                    PortfolioId.of("p-1"), Ticker.of("AAPL"),
                    ShareQuantity.positive(10), Price.of(150), Money.of(1500), NOW);

            var jpa = TransactionMapper.toJpaEntity(purchase);
            var back = TransactionMapper.toModelEntity(jpa);

            assertThat(back.type()).isEqualTo(cat.gencat.agaur.hexastock.model.transaction.TransactionType.PURCHASE);
            assertThat(back.ticker()).isEqualTo(Ticker.of("AAPL"));
            assertThat(back.quantity().value()).isEqualTo(10);
            assertThat(back.unitPrice()).isEqualTo(Price.of(150));
        }

        @Test
        @DisplayName("sale round-trip through mapper preserves profit field")
        void saleRoundTrip() {
            var sale = new cat.gencat.agaur.hexastock.model.transaction.SaleTransaction(
                    cat.gencat.agaur.hexastock.model.transaction.TransactionId.of("tx-s"),
                    PortfolioId.of("p-1"), Ticker.of("MSFT"),
                    ShareQuantity.positive(5), Price.of(300),
                    Money.of(1500), Money.of(250), NOW);

            var jpa = TransactionMapper.toJpaEntity(sale);
            var back = TransactionMapper.toModelEntity(jpa);

            assertThat(back.type()).isEqualTo(cat.gencat.agaur.hexastock.model.transaction.TransactionType.SALE);
            assertThat(back.profit()).isEqualTo(Money.of(250));
        }
    }
}
