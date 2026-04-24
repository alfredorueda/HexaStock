package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document.HoldingDocument;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document.LotDocument;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Holding;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.HoldingId;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Lot;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.LotId;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.DepositTransaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.SaleTransaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.TransactionType;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.TransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Persistence mappers – unit tests (MongoDB)")
class MapperTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    @Nested
    @DisplayName("PortfolioDocumentMapper")
    class PortfolioMapperTests {

        @Test
        @DisplayName("toModelEntity maps all scalar fields from Mongo document to domain")
        void toModelEntity_mapsScalarFields() {
            PortfolioDocument doc = new PortfolioDocument("p-1", "Alice",
                    new BigDecimal("1000.00"), NOW, List.of(), 7L);

            Portfolio domain = PortfolioDocumentMapper.toModelEntity(doc);

            assertThat(domain.getId()).isEqualTo(PortfolioId.of("p-1"));
            assertThat(domain.getOwnerName()).isEqualTo("Alice");
            assertThat(domain.getBalance()).isEqualTo(Money.of(1000));
            assertThat(domain.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toDocument maps scalar fields and uses null version by default")
        void toDocument_mapsScalarFieldsWithNullVersionByDefault() {
            Portfolio domain = new Portfolio(PortfolioId.of("p-2"), "Bob", Money.of(500), NOW);

            PortfolioDocument doc = PortfolioDocumentMapper.toDocument(domain);

            assertThat(doc.getId()).isEqualTo("p-2");
            assertThat(doc.getOwnerName()).isEqualTo("Bob");
            assertThat(doc.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(doc.getCreatedAt()).isEqualTo(NOW);
            assertThat(doc.getVersion()).isNull();
        }

        @Test
        @DisplayName("toDocument maps provided optimistic version")
        void toDocument_mapsProvidedVersion() {
            Portfolio domain = new Portfolio(PortfolioId.of("p-3"), "Carol", Money.of(750), NOW);

            PortfolioDocument doc = PortfolioDocumentMapper.toDocument(domain, 3L);

            assertThat(doc.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("toModelEntity reconstructs holdings and lots")
        void toModelEntity_reconstructsHoldingsAndLots() {
            LotDocument lotDoc = new LotDocument("lot-1", 10, 8, new BigDecimal("150.00"), NOW);
            HoldingDocument holdingDoc = new HoldingDocument("h-1", "AAPL", List.of(lotDoc));
            PortfolioDocument doc = new PortfolioDocument("p-4", "Dani",
                    new BigDecimal("900.00"), NOW, List.of(holdingDoc), 0L);

            Portfolio domain = PortfolioDocumentMapper.toModelEntity(doc);

            assertThat(domain.getHoldings()).hasSize(1);
            Holding holding = domain.getHoldings().get(0);
            assertThat(holding.getTicker()).isEqualTo(Ticker.of("AAPL"));
            assertThat(holding.getLots()).hasSize(1);
            assertThat(holding.getLots().get(0).getRemainingShares()).isEqualTo(ShareQuantity.of(8));
        }

        @Test
        @DisplayName("toModelEntity treats null holdings as empty")
        void toModelEntity_nullHoldings_returnsEmptyList() {
            PortfolioDocument doc = new PortfolioDocument("p-5", "Eva",
                    new BigDecimal("100.00"), NOW, null, 0L);

            Portfolio domain = PortfolioDocumentMapper.toModelEntity(doc);

            assertThat(domain.getHoldings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("LotDocumentMapper")
    class LotMapperTests {

        @Test
        @DisplayName("toModelEntity maps lot fields correctly")
        void toModelEntity_mapsLotFields() {
            LotDocument doc = new LotDocument("lot-1", 20, 15, new BigDecimal("100.50"), NOW);

            Lot domain = LotDocumentMapper.toModelEntity(doc);

            assertThat(domain.getId()).isEqualTo(LotId.of("lot-1"));
            assertThat(domain.getInitialShares()).isEqualTo(ShareQuantity.of(20));
            assertThat(domain.getRemainingShares()).isEqualTo(ShareQuantity.of(15));
            assertThat(domain.getUnitPrice()).isEqualTo(Price.of(100.50));
            assertThat(domain.getPurchasedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toDocument maps lot fields correctly")
        void toDocument_mapsLotFields() {
            Lot domain = new Lot(LotId.of("lot-2"), ShareQuantity.of(5),
                    ShareQuantity.of(3), Price.of(200), NOW);

            LotDocument doc = LotDocumentMapper.toDocument(domain);

            assertThat(doc.getId()).isEqualTo("lot-2");
            assertThat(doc.getInitialShares()).isEqualTo(5);
            assertThat(doc.getRemainingShares()).isEqualTo(3);
            assertThat(doc.getUnitPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(doc.getPurchasedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("HoldingDocumentMapper")
    class HoldingMapperTests {

        @Test
        @DisplayName("toModelEntity maps holding with lots")
        void toModelEntity_mapsHoldingWithLots() {
            HoldingDocument doc = new HoldingDocument("h-1", "MSFT",
                    List.of(new LotDocument("lot-1", 10, 10, new BigDecimal("300.00"), NOW)));

            Holding domain = HoldingDocumentMapper.toModelEntity(doc);

            assertThat(domain.getId()).isEqualTo(HoldingId.of("h-1"));
            assertThat(domain.getTicker()).isEqualTo(Ticker.of("MSFT"));
            assertThat(domain.getLots()).hasSize(1);
        }

        @Test
        @DisplayName("toDocument sorts lots by purchase date ascending")
        void toDocument_sortsLotsByPurchaseDate() {
            Holding domain = new Holding(HoldingId.of("h-2"), Ticker.of("GOOG"));
            domain.addLotFromPersistence(new Lot(LotId.of("lot-late"), ShareQuantity.of(8), ShareQuantity.of(8),
                    Price.of(140), NOW.plusDays(1)));
            domain.addLotFromPersistence(new Lot(LotId.of("lot-early"), ShareQuantity.of(5), ShareQuantity.of(5),
                    Price.of(130), NOW.minusDays(1)));

            HoldingDocument doc = HoldingDocumentMapper.toDocument(domain);

            assertThat(doc.getLots()).extracting(LotDocument::getId)
                    .containsExactly("lot-early", "lot-late");
        }

        @Test
        @DisplayName("toModelEntity treats null lots as empty")
        void toModelEntity_nullLots_returnsEmptyList() {
            HoldingDocument doc = new HoldingDocument("h-3", "NVDA", null);

            Holding domain = HoldingDocumentMapper.toModelEntity(doc);

            assertThat(domain.getLots()).isEmpty();
        }
    }

    @Nested
    @DisplayName("TransactionDocumentMapper")
    class TransactionMapperTests {

        @Test
        @DisplayName("deposit round-trip preserves all non-stock fields")
        void depositRoundTrip() {
            DepositTransaction deposit = new DepositTransaction(
                    TransactionId.of("tx-d"), PortfolioId.of("p-1"), Money.of(500), NOW);

            var doc = TransactionDocumentMapper.toDocument(deposit);
            var back = TransactionDocumentMapper.toModelEntity(doc);

            assertThat(back.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(back.totalAmount()).isEqualTo(Money.of(500));
            assertThat(back.id().value()).isEqualTo("tx-d");
            assertThat(back.ticker()).isNull();
        }

        @Test
        @DisplayName("sale round-trip preserves stock fields and profit")
        void saleRoundTrip() {
            SaleTransaction sale = new SaleTransaction(
                    TransactionId.of("tx-s"), PortfolioId.of("p-1"), Ticker.of("MSFT"),
                    ShareQuantity.positive(5), Price.of(300), Money.of(1500), Money.of(250), NOW);

            var doc = TransactionDocumentMapper.toDocument(sale);
            var back = TransactionDocumentMapper.toModelEntity(doc);

            assertThat(back.type()).isEqualTo(TransactionType.SALE);
            assertThat(back.ticker()).isEqualTo(Ticker.of("MSFT"));
            assertThat(back.quantity()).isEqualTo(ShareQuantity.of(5));
            assertThat(back.unitPrice()).isEqualTo(Price.of(300));
            assertThat(back.profit()).isEqualTo(Money.of(250));
        }
    }
}
