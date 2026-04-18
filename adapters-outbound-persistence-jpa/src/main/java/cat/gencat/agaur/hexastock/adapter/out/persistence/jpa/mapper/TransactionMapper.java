package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.mapper;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity.TransactionJpaEntity;
import cat.gencat.agaur.hexastock.model.portfolio.*;
import cat.gencat.agaur.hexastock.model.transaction.*;
import cat.gencat.agaur.hexastock.model.market.*;
import cat.gencat.agaur.hexastock.model.money.*;

public final class TransactionMapper {

    private TransactionMapper() {
        // Utility class - prevent instantiation
    }

    public static Transaction toModelEntity(TransactionJpaEntity e) {
        return switch (e.getType()) {
            case DEPOSIT -> new DepositTransaction(
                    TransactionId.of(e.getId()),
                    PortfolioId.of(e.getPortfolioId()),
                    Money.of(e.getTotalAmount()),
                    e.getCreatedAt());

            case WITHDRAWAL -> new WithdrawalTransaction(
                    TransactionId.of(e.getId()),
                    PortfolioId.of(e.getPortfolioId()),
                    Money.of(e.getTotalAmount()),
                    e.getCreatedAt());

            case PURCHASE -> new PurchaseTransaction(
                    TransactionId.of(e.getId()),
                    PortfolioId.of(e.getPortfolioId()),
                    Ticker.of(e.getTicker()),
                    ShareQuantity.of(e.getQuantity()),
                    Price.of(e.getUnitPrice()),
                    Money.of(e.getTotalAmount()),
                    e.getCreatedAt());

            case SALE -> new SaleTransaction(
                    TransactionId.of(e.getId()),
                    PortfolioId.of(e.getPortfolioId()),
                    Ticker.of(e.getTicker()),
                    ShareQuantity.of(e.getQuantity()),
                    Price.of(e.getUnitPrice()),
                    Money.of(e.getTotalAmount()),
                    Money.of(e.getProfit()),
                    e.getCreatedAt());
        };
    }

    public static TransactionJpaEntity toJpaEntity(Transaction tx) {
        return TransactionJpaEntity.builder()
                .id(tx.id().value())
                .portfolioId(tx.portfolioId().value())
                .type(tx.type())
                .ticker(tx.ticker() != null ? tx.ticker().value() : null)
                .quantity(tx.quantity().value())
                .unitPrice(tx.unitPrice() != null ? tx.unitPrice().value() : null)
                .totalAmount(tx.totalAmount().amount())
                .profit(tx.profit().amount())
                .createdAt(tx.createdAt())
                .build();
    }
}