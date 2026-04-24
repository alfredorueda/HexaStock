package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.document.PortfolioDocument;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;

import java.util.List;
import java.util.Optional;

public final class PortfolioDocumentMapper {

    private PortfolioDocumentMapper() {
    }

    public static Portfolio toModelEntity(PortfolioDocument doc) {
        Portfolio portfolio = new Portfolio(
                PortfolioId.of(doc.getId()),
                doc.getOwnerName(),
                Money.of(doc.getBalance()),
                doc.getCreatedAt()
        );

        Optional.ofNullable(doc.getHoldings())
                .orElseGet(List::of)
                .stream()
                .map(HoldingDocumentMapper::toModelEntity)
                .forEach(portfolio::addHolding);

        return portfolio;
    }

    public static PortfolioDocument toDocument(Portfolio entity) {
        return toDocument(entity, null);
    }

    public static PortfolioDocument toDocument(Portfolio entity, Long version) {
        return new PortfolioDocument(
                entity.getId().value(),
                entity.getOwnerName(),
                entity.getBalance().amount(),
                entity.getCreatedAt(),
                entity.getHoldings().stream().map(HoldingDocumentMapper::toDocument).toList(),
                version
        );
    }
}
