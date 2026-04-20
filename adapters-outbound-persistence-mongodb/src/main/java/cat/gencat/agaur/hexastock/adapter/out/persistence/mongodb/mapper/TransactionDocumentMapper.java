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

public final class TransactionDocumentMapper {

    private TransactionDocumentMapper() {
    }

    public static Transaction toModelEntity(TransactionDocument doc) {
        return switch (doc.getType()) {
            case DEPOSIT -> new DepositTransaction(
                    TransactionId.of(doc.getId()),
                    PortfolioId.of(doc.getPortfolioId()),
                    Money.of(doc.getTotalAmount()),
                    doc.getCreatedAt());

            case WITHDRAWAL -> new WithdrawalTransaction(
                    TransactionId.of(doc.getId()),
                    PortfolioId.of(doc.getPortfolioId()),
                    Money.of(doc.getTotalAmount()),
                    doc.getCreatedAt());

            case PURCHASE -> new PurchaseTransaction(
                    TransactionId.of(doc.getId()),
                    PortfolioId.of(doc.getPortfolioId()),
                    Ticker.of(doc.getTicker()),
                    ShareQuantity.of(doc.getQuantity()),
                    Price.of(doc.getUnitPrice()),
                    Money.of(doc.getTotalAmount()),
                    doc.getCreatedAt());

            case SALE -> new SaleTransaction(
                    TransactionId.of(doc.getId()),
                    PortfolioId.of(doc.getPortfolioId()),
                    Ticker.of(doc.getTicker()),
                    ShareQuantity.of(doc.getQuantity()),
                    Price.of(doc.getUnitPrice()),
                    Money.of(doc.getTotalAmount()),
                    Money.of(doc.getProfit()),
                    doc.getCreatedAt());
        };
    }

    public static TransactionDocument toDocument(Transaction tx) {
        return new TransactionDocument(
                tx.id().value(),
                tx.portfolioId().value(),
                tx.type(),
                tx.ticker() != null ? tx.ticker().value() : null,
                tx.quantity().value(),
                tx.unitPrice() != null ? tx.unitPrice().value() : null,
                tx.totalAmount().amount(),
                tx.profit().amount(),
                tx.createdAt()
        );
    }
}
