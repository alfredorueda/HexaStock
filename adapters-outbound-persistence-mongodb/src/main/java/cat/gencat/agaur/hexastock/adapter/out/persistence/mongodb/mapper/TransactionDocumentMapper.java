package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document.TransactionDocument;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import cat.gencat.agaur.hexastock.model.money.ShareQuantity;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.DepositTransaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.PurchaseTransaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.SaleTransaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.Transaction;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.TransactionId;
import cat.gencat.agaur.hexastock.portfolios.model.transaction.WithdrawalTransaction;

public final class TransactionDocumentMapper {

    private TransactionDocumentMapper() {
    }

    public static Transaction toModelEntity(TransactionDocument doc) {
        return switch (doc.type()) {
            case DEPOSIT -> new DepositTransaction(
                    TransactionId.of(doc.id()),
                    PortfolioId.of(doc.portfolioId()),
                    Money.of(doc.totalAmount()),
                    doc.createdAt());

            case WITHDRAWAL -> new WithdrawalTransaction(
                    TransactionId.of(doc.id()),
                    PortfolioId.of(doc.portfolioId()),
                    Money.of(doc.totalAmount()),
                    doc.createdAt());

            case PURCHASE -> new PurchaseTransaction(
                    TransactionId.of(doc.id()),
                    PortfolioId.of(doc.portfolioId()),
                    Ticker.of(doc.ticker()),
                    ShareQuantity.of(doc.quantity()),
                    Price.of(doc.unitPrice()),
                    Money.of(doc.totalAmount()),
                    doc.createdAt());

            case SALE -> new SaleTransaction(
                    TransactionId.of(doc.id()),
                    PortfolioId.of(doc.portfolioId()),
                    Ticker.of(doc.ticker()),
                    ShareQuantity.of(doc.quantity()),
                    Price.of(doc.unitPrice()),
                    Money.of(doc.totalAmount()),
                    Money.of(doc.profit()),
                    doc.createdAt());
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
