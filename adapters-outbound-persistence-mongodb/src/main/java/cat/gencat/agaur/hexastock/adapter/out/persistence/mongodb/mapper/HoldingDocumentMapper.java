package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.HoldingDocument;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.portfolio.Holding;
import cat.gencat.agaur.hexastock.model.portfolio.HoldingId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HoldingDocumentMapper {

    private HoldingDocumentMapper() {
    }

    public static Holding toModelEntity(HoldingDocument doc) {
        Holding holding = new Holding(HoldingId.of(doc.getId()), Ticker.of(doc.getTicker()));

        Optional.ofNullable(doc.getLots())
                .orElseGet(List::of)
                .stream()
                .map(LotDocumentMapper::toModelEntity)
                .forEach(holding::addLotFromPersistence);

        return holding;
    }

    public static HoldingDocument toDocument(Holding entity) {
        var lots = entity.getLots().stream()
                .sorted(Comparator.comparing(lot -> lot.getPurchasedAt()))
                .map(LotDocumentMapper::toDocument)
                .toList();

        return new HoldingDocument(entity.getId().value(), entity.getTicker().value(), lots);
    }
}
