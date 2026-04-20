package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.HoldingDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.LotDocument;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.model.portfolio.Holding;
import cat.gencat.agaur.hexastock.model.portfolio.HoldingId;
import cat.gencat.agaur.hexastock.model.portfolio.Lot;
import cat.gencat.agaur.hexastock.model.portfolio.LotId;
import cat.gencat.agaur.hexastock.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;

import java.util.List;

/**
 * Converts between the {@link Portfolio} domain aggregate and the
 * {@link PortfolioDocument} MongoDB representation.
 *
 * <p>Adapter-local anti-corruption layer: the domain model is never aware of
 * MongoDB types, and the persistence model is never aware of domain value objects.</p>
 */
public final class PortfolioDocumentMapper {

    private PortfolioDocumentMapper() { }

    public static Portfolio toDomain(PortfolioDocument d) {
        Portfolio portfolio = new Portfolio(
                PortfolioId.of(d.getId()),
                d.getOwnerName(),
                Money.of(d.getBalance()),
                d.getCreatedAt()
        );
        for (HoldingDocument hd : d.getHoldings()) {
            portfolio.addHolding(toDomain(hd));
        }
        return portfolio;
    }

    /**
     * Converts a domain Portfolio to a MongoDB document, preserving the given
     * optimistic-locking version. Pass {@code null} for fresh inserts; pass the
     * last-read version for updates so Spring Data can enforce the CAS.
     */
    public static PortfolioDocument toDocument(Portfolio p, Long version) {
        List<HoldingDocument> holdings = p.getHoldings().stream()
                .map(PortfolioDocumentMapper::toDocument)
                .toList();
        return new PortfolioDocument(
                p.getId().value(),
                p.getOwnerName(),
                p.getBalance().amount(),
                p.getCreatedAt(),
                holdings,
                version
        );
    }

    private static Holding toDomain(HoldingDocument hd) {
        Holding holding = new Holding(HoldingId.of(hd.getId()), Ticker.of(hd.getTicker()));
        for (LotDocument ld : hd.getLots()) {
            holding.addLotFromPersistence(toDomain(ld));
        }
        return holding;
    }

    private static HoldingDocument toDocument(Holding h) {
        List<LotDocument> lots = h.getLots().stream()
                .map(PortfolioDocumentMapper::toDocument)
                .toList();
        return new HoldingDocument(h.getId().value(), h.getTicker().value(), lots);
    }

    private static Lot toDomain(LotDocument ld) {
        return new Lot(
                LotId.of(ld.id()),
                ShareQuantity.of(ld.initialStocks()),
                ShareQuantity.of(ld.remaining()),
                Price.of(ld.unitPrice()),
                ld.purchasedAt()
        );
    }

    private static LotDocument toDocument(Lot lot) {
        return new LotDocument(
                lot.getId().value(),
                lot.getInitialShares().value(),
                lot.getRemainingShares().value(),
                lot.getUnitPrice().value(),
                lot.getPurchasedAt()
        );
    }
}
