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
        return switch (d.type()) {
            case DEPOSIT -> new DepositTransaction(
                    TransactionId.of(d.id()),
                    PortfolioId.of(d.portfolioId()),
                    Money.of(d.totalAmount()),
                    d.createdAt());
            case WITHDRAWAL -> new WithdrawalTransaction(
                    TransactionId.of(d.id()),
                    PortfolioId.of(d.portfolioId()),
                    Money.of(d.totalAmount()),
                    d.createdAt());
            case PURCHASE -> new PurchaseTransaction(
                    TransactionId.of(d.id()),
                    PortfolioId.of(d.portfolioId()),
                    Ticker.of(d.ticker()),
                    ShareQuantity.of(d.quantity()),
                    Price.of(d.unitPrice()),
                    Money.of(d.totalAmount()),
                    d.createdAt());
            case SALE -> new SaleTransaction(
                    TransactionId.of(d.id()),
                    PortfolioId.of(d.portfolioId()),
                    Ticker.of(d.ticker()),
                    ShareQuantity.of(d.quantity()),
                    Price.of(d.unitPrice()),
                    Money.of(d.totalAmount()),
                    Money.of(d.profit()),
                    d.createdAt());
        };
    }

    public static TransactionDocument toDocument(Transaction t) {
        return new TransactionDocument(
                t.id().value(),
                t.portfolioId().value(),
                t.type(),
                t.ticker() != null ? t.ticker().value() : null,
                t.quantity().value(),
                t.unitPrice() != null ? t.unitPrice().value() : null,
                t.totalAmount().amount(),
                t.profit().amount(),
                t.createdAt());
    }
}
