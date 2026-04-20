package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.TransactionDocument;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.model.transaction.DepositTransaction;
import cat.gencat.agaur.hexastock.model.transaction.PurchaseTransaction;
import cat.gencat.agaur.hexastock.model.transaction.SaleTransaction;
import cat.gencat.agaur.hexastock.model.transaction.Transaction;
import cat.gencat.agaur.hexastock.model.transaction.TransactionId;
import cat.gencat.agaur.hexastock.model.transaction.WithdrawalTransaction;

/**
 * Converts between the sealed {@link Transaction} hierarchy and the
 * {@link TransactionDocument} MongoDB representation.
 */
public final class TransactionDocumentMapper {

    private TransactionDocumentMapper() { }

    public static Transaction toDomain(TransactionDocument d) {
        return switch (d.getType()) {
            case DEPOSIT -> new DepositTransaction(
                    TransactionId.of(d.getId()),
                    PortfolioId.of(d.getPortfolioId()),
                    Money.of(d.getTotalAmount()),
                    d.getCreatedAt());
            case WITHDRAWAL -> new WithdrawalTransaction(
                    TransactionId.of(d.getId()),
                    PortfolioId.of(d.getPortfolioId()),
                    Money.of(d.getTotalAmount()),
                    d.getCreatedAt());
            case PURCHASE -> new PurchaseTransaction(
                    TransactionId.of(d.getId()),
                    PortfolioId.of(d.getPortfolioId()),
                    Ticker.of(d.getTicker()),
                    ShareQuantity.of(d.getQuantity()),
                    Price.of(d.getUnitPrice()),
                    Money.of(d.getTotalAmount()),
                    d.getCreatedAt());
            case SALE -> new SaleTransaction(
                    TransactionId.of(d.getId()),
                    PortfolioId.of(d.getPortfolioId()),
                    Ticker.of(d.getTicker()),
                    ShareQuantity.of(d.getQuantity()),
                    Price.of(d.getUnitPrice()),
                    Money.of(d.getTotalAmount()),
                    Money.of(d.getProfit()),
                    d.getCreatedAt());
        };
    }

    public static TransactionDocument toDocument(Transaction t) {
        return TransactionDocument.builder()
                .id(t.id().value())
                .portfolioId(t.portfolioId().value())
                .type(t.type())
                .ticker(t.ticker() != null ? t.ticker().value() : null)
                .quantity(t.quantity().value())
                .unitPrice(t.unitPrice() != null ? t.unitPrice().value() : null)
                .totalAmount(t.totalAmount().amount())
                .profit(t.profit().amount())
                .createdAt(t.createdAt())
                .build();
    }
}
