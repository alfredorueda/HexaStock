package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.Portfolio;

import java.math.BigDecimal;

/**
 * Response DTO for portfolio creation operations.
 * 
 * <p>This DTO decouples the REST API from the domain model, providing a stable
 * API contract for created portfolios. Translation from domain to API representation
 * happens in the adapter layer, ensuring that changes to the domain model do not
 * directly impact API consumers.</p>
 * 
 * <p>The Money value object from the domain is flattened into separate
 * {@code cashBalance} and {@code currency} fields for simpler JSON representation.</p>
 *
 * @param id          The unique identifier of the created portfolio
 * @param ownerName   The name of the portfolio owner
 * @param cashBalance The current cash balance available in the portfolio
 * @param currency    The ISO 4217 currency code (e.g., "USD")
 */
public record CreatePortfolioResponseDTO(
    String id,
    String ownerName,
    BigDecimal cashBalance,
    String currency
) {
    /**
     * Factory method to create a DTO from a Portfolio domain object.
     * 
     * @param portfolio The domain Portfolio to convert
     * @return A new CreatePortfolioResponseDTO representing the portfolio
     */
    public static CreatePortfolioResponseDTO from(Portfolio portfolio) {
        return new CreatePortfolioResponseDTO(
            portfolio.getId(),
            portfolio.getOwnerName(),
            portfolio.getBalance(),
            "USD" // Default currency as Portfolio uses BigDecimal directly
        );
    }
}
