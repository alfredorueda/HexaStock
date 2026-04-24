package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.LotDocument;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Lot;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.LotId;

public final class LotDocumentMapper {

    private LotDocumentMapper() {
    }

    public static Lot toModelEntity(LotDocument doc) {
        return new Lot(
                LotId.of(doc.getId()),
                ShareQuantity.of(doc.getInitialShares()),
                ShareQuantity.of(doc.getRemainingShares()),
                Price.of(doc.getUnitPrice()),
                doc.getPurchasedAt()
        );
    }

    public static LotDocument toDocument(Lot entity) {
        return new LotDocument(
                entity.getId().value(),
                entity.getInitialShares().value(),
                entity.getRemainingShares().value(),
                entity.getUnitPrice().value(),
                entity.getPurchasedAt()
        );
    }
}
