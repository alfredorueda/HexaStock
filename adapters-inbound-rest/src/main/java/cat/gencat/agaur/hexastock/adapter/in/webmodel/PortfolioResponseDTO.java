package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cat.gencat.agaur.hexastock.model.Portfolio;

public record PortfolioResponseDTO(
    String id,
    String ownerName,
    BigDecimal balance,
    LocalDateTime createdAt
) {
    public static PortfolioResponseDTO from(Portfolio p) {
        return new PortfolioResponseDTO(
            p.getId().value(),
            p.getOwnerName(),
            p.getBalance().amount(),
            p.getCreatedAt()
        );
    }
}
